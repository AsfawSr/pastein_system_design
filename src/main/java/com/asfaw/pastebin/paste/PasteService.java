package com.asfaw.pastebin.paste;

import com.asfaw.pastebin.user.UserRepository;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class PasteService {

    private static final int MAX_ID_ATTEMPTS = 5;

    private final PasteRepository repository;
    private final PasteFinder finder;
    private final IdGenerator idGenerator;
    private final Clock clock;
    private final PasswordEncoder passwordEncoder;
    private final MeterRegistry meterRegistry;
    private final UserRepository userRepository;

    @Transactional
    public Paste create(CreatePasteCommand command, String ownerUsername) {
        Instant now = clock.instant();
        Paste paste = new Paste();
        paste.setId(nextFreeId());
        paste.setTitle(command.title());
        paste.setContent(command.content());
        paste.setCreatedAt(now);
        paste.setExpiresAt(command.ttl() == null ? null : now.plus(command.ttl()));
        paste.setBurnAfterRead(command.burnAfterRead());
        paste.setVisibility(command.visibility());
        paste.setLanguage(command.language() == null || command.language().isBlank()
                ? "plaintext" : command.language());
        if (command.password() != null && !command.password().isBlank()) {
            paste.setPasswordHash(passwordEncoder.encode(command.password()));
        }
        if (ownerUsername != null) {
            userRepository.findByUsername(ownerUsername).ifPresent(paste::setOwner);
        }
        Paste saved = repository.save(paste);
        meterRegistry.counter("pastebin.pastes.created", "visibility", saved.getVisibility().name()).increment();
        return saved;
    }

    @Transactional(readOnly = true)
    public Page<Paste> listPublic(int page, int size) {
        return repository.findVisible(PasteVisibility.PUBLIC, clock.instant(), PageRequest.of(page, size));
    }

    @Transactional(readOnly = true)
    public Page<Paste> listOwnedBy(String username, int page, int size) {
        return repository.findByOwnerUsername(username, PageRequest.of(page, size));
    }

    @Transactional(readOnly = true)
    public Page<Paste> searchPublic(String query, int page, int size) {
        return repository.searchPublic(query, clock.instant(), PageRequest.of(page, size));
    }

    @Transactional
    public ViewOutcome view(String id, String rawPassword) {
        Paste paste = find(id).orElseThrow(() -> new PasteNotFoundException(id));
        if (paste.getPasswordHash() != null) {
            if (rawPassword == null || rawPassword.isBlank()) {
                return new ViewOutcome.PasswordRequired(false);
            }
            if (!passwordEncoder.matches(rawPassword, paste.getPasswordHash())) {
                return new ViewOutcome.PasswordRequired(true);
            }
        }
        if (paste.isBurnAfterRead()) {
            // re-fetch under row lock so concurrent readers can't both burn it
            Paste locked = repository.findByIdForUpdate(id)
                    .orElseThrow(() -> new PasteNotFoundException(id));
            repository.delete(locked);
            finder.evict(id);
            meterRegistry.counter("pastebin.pastes.burned").increment();
            return new ViewOutcome.Viewed(locked, true, ownerUsername(locked));
        }
        repository.incrementViews(id);
        return new ViewOutcome.Viewed(paste, false, ownerUsername(paste));
    }

    @Transactional
    public Paste updateOwned(String id, String username, String title, String content, String language) {
        Paste paste = requireOwned(id, username);
        paste.setTitle(title);
        paste.setContent(content);
        paste.setLanguage(language == null || language.isBlank() ? "plaintext" : language);
        finder.evict(id);
        return paste;
    }

    @Transactional
    public void deleteOwned(String id, String username) {
        Paste paste = requireOwned(id, username);
        repository.delete(paste);
        finder.evict(id);
    }

    @Transactional(readOnly = true)
    public Paste getOwned(String id, String username) {
        return requireOwned(id, username);
    }

    private Paste requireOwned(String id, String username) {
        Paste paste = repository.findById(id).orElseThrow(() -> new PasteNotFoundException(id));
        String owner = ownerUsername(paste);
        if (owner == null || !owner.equals(username)) {
            throw new AccessDeniedException("You are not the owner of this paste");
        }
        return paste;
    }

    private String ownerUsername(Paste paste) {
        return paste.getOwner() == null ? null : paste.getOwner().getUsername();
    }

    @Transactional(readOnly = true)
    public Optional<Paste> find(String id) {
        return Optional.ofNullable(finder.findById(id)).filter(p -> !isExpired(p));
    }

    private boolean isExpired(Paste paste) {
        return paste.getExpiresAt() != null && !clock.instant().isBefore(paste.getExpiresAt());
    }

    private String nextFreeId() {
        for (int attempt = 0; attempt < MAX_ID_ATTEMPTS; attempt++) {
            String id = idGenerator.generate();
            if (!repository.existsById(id)) {
                return id;
            }
        }
        throw new IllegalStateException("No unique paste id after " + MAX_ID_ATTEMPTS + " attempts");
    }
}
