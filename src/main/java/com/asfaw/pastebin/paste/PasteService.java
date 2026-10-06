package com.asfaw.pastebin.paste;

import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class PasteService {

    private static final int MAX_ID_ATTEMPTS = 5;

    private final PasteRepository repository;
    private final IdGenerator idGenerator;
    private final Clock clock;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public Paste create(String title, String content, Duration ttl, boolean burnAfterRead, String rawPassword) {
        Instant now = clock.instant();
        Paste paste = new Paste();
        paste.setId(nextFreeId());
        paste.setTitle(title);
        paste.setContent(content);
        paste.setCreatedAt(now);
        paste.setExpiresAt(ttl == null ? null : now.plus(ttl));
        paste.setBurnAfterRead(burnAfterRead);
        if (rawPassword != null && !rawPassword.isBlank()) {
            paste.setPasswordHash(passwordEncoder.encode(rawPassword));
        }
        return repository.save(paste);
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
            return new ViewOutcome.Viewed(locked, true);
        }
        repository.incrementViews(id);
        return new ViewOutcome.Viewed(paste, false);
    }

    @Transactional(readOnly = true)
    public Optional<Paste> find(String id) {
        return repository.findById(id).filter(p -> !isExpired(p));
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
