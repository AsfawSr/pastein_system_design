package com.asfaw.pastebin.paste;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PasteServiceTest {

    private static final Instant NOW = Instant.parse("2026-01-15T12:00:00Z");

    @Mock
    private PasteRepository repository;

    @Mock
    private IdGenerator idGenerator;

    private PasteService service;

    @BeforeEach
    void setUp() {
        service = new PasteService(repository, idGenerator, Clock.fixed(NOW, ZoneOffset.UTC));
    }

    @Test
    void createAssignsIdTitleContentAndTimestamp() {
        when(idGenerator.generate()).thenReturn("abc12345");
        when(repository.existsById("abc12345")).thenReturn(false);
        when(repository.save(any(Paste.class))).thenAnswer(inv -> inv.getArgument(0));

        Paste paste = service.create("my title", "hello world", null);

        assertThat(paste.getId()).isEqualTo("abc12345");
        assertThat(paste.getTitle()).isEqualTo("my title");
        assertThat(paste.getContent()).isEqualTo("hello world");
        assertThat(paste.getCreatedAt()).isEqualTo(NOW);
        assertThat(paste.getExpiresAt()).isNull();
    }

    @Test
    void createComputesExpiresAtFromTtl() {
        when(idGenerator.generate()).thenReturn("abc12345");
        when(repository.existsById("abc12345")).thenReturn(false);
        when(repository.save(any(Paste.class))).thenAnswer(inv -> inv.getArgument(0));

        Paste paste = service.create(null, "content", Duration.ofHours(1));

        assertThat(paste.getExpiresAt()).isEqualTo(NOW.plus(Duration.ofHours(1)));
    }

    @Test
    void createRetriesWhenGeneratedIdCollides() {
        when(idGenerator.generate()).thenReturn("taken123", "free4567");
        when(repository.existsById("taken123")).thenReturn(true);
        when(repository.existsById("free4567")).thenReturn(false);
        when(repository.save(any(Paste.class))).thenAnswer(inv -> inv.getArgument(0));

        Paste paste = service.create(null, "content", null);

        assertThat(paste.getId()).isEqualTo("free4567");
    }

    @Test
    void createFailsAfterTooManyCollisions() {
        when(idGenerator.generate()).thenReturn("same1234");
        when(repository.existsById("same1234")).thenReturn(true);

        assertThatThrownBy(() -> service.create(null, "content", null))
                .isInstanceOf(IllegalStateException.class);
        verify(repository, never()).save(any());
    }

    @Test
    void findReturnsPasteThatNeverExpires() {
        Paste stored = pasteExpiringAt(null);
        when(repository.findById("abc12345")).thenReturn(Optional.of(stored));

        assertThat(service.find("abc12345")).containsSame(stored);
    }

    @Test
    void findReturnsPasteBeforeExpiry() {
        Paste stored = pasteExpiringAt(NOW.plusSeconds(60));
        when(repository.findById("abc12345")).thenReturn(Optional.of(stored));

        assertThat(service.find("abc12345")).containsSame(stored);
    }

    @Test
    void findHidesExpiredPaste() {
        Paste stored = pasteExpiringAt(NOW.minusSeconds(1));
        when(repository.findById("abc12345")).thenReturn(Optional.of(stored));

        assertThat(service.find("abc12345")).isEmpty();
    }

    @Test
    void findHidesPasteExpiringExactlyNow() {
        Paste stored = pasteExpiringAt(NOW);
        when(repository.findById("abc12345")).thenReturn(Optional.of(stored));

        assertThat(service.find("abc12345")).isEmpty();
    }

    private Paste pasteExpiringAt(Instant expiresAt) {
        Paste paste = new Paste();
        paste.setId("abc12345");
        paste.setContent("content");
        paste.setCreatedAt(NOW.minusSeconds(3600));
        paste.setExpiresAt(expiresAt);
        return paste;
    }
}
