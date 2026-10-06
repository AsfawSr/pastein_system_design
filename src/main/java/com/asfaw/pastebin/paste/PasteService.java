package com.asfaw.pastebin.paste;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class PasteService {

    private static final int MAX_ID_ATTEMPTS = 5;

    private final PasteRepository repository;
    private final IdGenerator idGenerator;

    @Transactional
    public Paste create(String title, String content) {
        Paste paste = new Paste();
        paste.setId(nextFreeId());
        paste.setTitle(title);
        paste.setContent(content);
        paste.setCreatedAt(Instant.now());
        return repository.save(paste);
    }

    @Transactional(readOnly = true)
    public Optional<Paste> find(String id) {
        return repository.findById(id);
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
