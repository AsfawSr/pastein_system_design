package com.asfaw.pastebin.paste;

import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;

// separate bean so @Cacheable goes through the proxy (self-invocation inside PasteService would bypass it)
@Component
@RequiredArgsConstructor
public class PasteFinder {

    private final PasteRepository repository;

    @Cacheable(cacheNames = "pastes", unless = "#result == null || #result.burnAfterRead")
    public Paste findById(String id) {
        return repository.findWithOwnerById(id).orElse(null);
    }

    @CacheEvict(cacheNames = "pastes")
    public void evict(String id) {
    }
}
