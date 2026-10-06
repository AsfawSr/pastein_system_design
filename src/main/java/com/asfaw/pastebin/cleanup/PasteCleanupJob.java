package com.asfaw.pastebin.cleanup;

import com.asfaw.pastebin.paste.PasteRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;

@Component
@RequiredArgsConstructor
@Slf4j
public class PasteCleanupJob {

    private final PasteRepository repository;
    private final Clock clock;

    @Scheduled(fixedDelayString = "${pastebin.cleanup.interval:PT10M}")
    @Transactional
    public void purgeExpiredPastes() {
        int deleted = repository.deleteExpired(clock.instant());
        if (deleted > 0) {
            log.info("Purged {} expired paste(s)", deleted);
        }
    }
}
