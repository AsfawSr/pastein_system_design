package com.asfaw.pastebin.paste;

import lombok.RequiredArgsConstructor;
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

    @Transactional
    public Paste create(String title, String content, Duration ttl) {
        Instant now = clock.instant();
        Paste paste = new Paste();
        paste.setId(nextFreeId());
        paste.setTitle(title);
        paste.setContent(content);
        paste.setCreatedAt(now);
        paste.setExpiresAt(ttl == null ? null : now.plus(ttl));
        return repository.save(paste);
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
