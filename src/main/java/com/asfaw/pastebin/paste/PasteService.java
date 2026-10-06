package com.asfaw.pastebin.paste;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
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

    @Transactional
    public Paste create(CreatePasteCommand command) {
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
        return repository.save(paste);
    }

    @Transactional(readOnly = true)
    public Page<Paste> listPublic(int page, int size) {
        return repository.findVisible(PasteVisibility.PUBLIC, clock.instant(), PageRequest.of(page, size));
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
            return new ViewOutcome.Viewed(locked, true);
        }
        repository.incrementViews(id);
        return new ViewOutcome.Viewed(paste, false);
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
