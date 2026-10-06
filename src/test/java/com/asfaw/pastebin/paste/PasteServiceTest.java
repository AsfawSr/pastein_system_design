package com.asfaw.pastebin.paste;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

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

    @Mock
    private PasswordEncoder passwordEncoder;

    private PasteService service;

    @BeforeEach
    void setUp() {
        service = new PasteService(repository, idGenerator, Clock.fixed(NOW, ZoneOffset.UTC), passwordEncoder);
    }

    @Test
    void createAssignsIdTitleContentAndTimestamp() {
        stubFreeId("abc12345");

        Paste paste = service.create(new CreatePasteCommand("my title", "hello world", null, false, null, PasteVisibility.UNLISTED, "java"));

        assertThat(paste.getId()).isEqualTo("abc12345");
        assertThat(paste.getTitle()).isEqualTo("my title");
        assertThat(paste.getContent()).isEqualTo("hello world");
        assertThat(paste.getCreatedAt()).isEqualTo(NOW);
        assertThat(paste.getExpiresAt()).isNull();
        assertThat(paste.getPasswordHash()).isNull();
        assertThat(paste.getVisibility()).isEqualTo(PasteVisibility.UNLISTED);
        assertThat(paste.getLanguage()).isEqualTo("java");
    }

    @Test
    void createDefaultsLanguageToPlaintext() {
        stubFreeId("abc12345");

        Paste paste = service.create(new CreatePasteCommand(null, "content", null, false, null, PasteVisibility.UNLISTED, null));

        assertThat(paste.getLanguage()).isEqualTo("plaintext");
    }

    @Test
    void createComputesExpiresAtFromTtl() {
        stubFreeId("abc12345");

        Paste paste = service.create(new CreatePasteCommand(null, "content", Duration.ofHours(1), false, null, PasteVisibility.UNLISTED, null));

        assertThat(paste.getExpiresAt()).isEqualTo(NOW.plus(Duration.ofHours(1)));
    }

    @Test
    void createHashesPasswordInsteadOfStoringPlaintext() {
        stubFreeId("abc12345");
        when(passwordEncoder.encode("s3cret")).thenReturn("$2a$hash");

        Paste paste = service.create(new CreatePasteCommand(null, "content", null, false, "s3cret", PasteVisibility.UNLISTED, null));

        assertThat(paste.getPasswordHash()).isEqualTo("$2a$hash");
    }

    @Test
    void createTreatsBlankPasswordAsNoPassword() {
        stubFreeId("abc12345");

        Paste paste = service.create(new CreatePasteCommand(null, "content", null, false, "   ", PasteVisibility.UNLISTED, null));

        assertThat(paste.getPasswordHash()).isNull();
        verify(passwordEncoder, never()).encode(any());
    }

    @Test
    void createRetriesWhenGeneratedIdCollides() {
        when(idGenerator.generate()).thenReturn("taken123", "free4567");
        when(repository.existsById("taken123")).thenReturn(true);
        when(repository.existsById("free4567")).thenReturn(false);
        when(repository.save(any(Paste.class))).thenAnswer(inv -> inv.getArgument(0));

        Paste paste = service.create(new CreatePasteCommand(null, "content", null, false, null, PasteVisibility.PUBLIC, null));

        assertThat(paste.getId()).isEqualTo("free4567");
        assertThat(paste.getVisibility()).isEqualTo(PasteVisibility.PUBLIC);
    }

    @Test
    void createFailsAfterTooManyCollisions() {
        when(idGenerator.generate()).thenReturn("same1234");
        when(repository.existsById("same1234")).thenReturn(true);

        assertThatThrownBy(() -> service.create(new CreatePasteCommand(null, "content", null, false, null, PasteVisibility.UNLISTED, null)))
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

    @Test
    void viewOfNormalPasteDoesNotDelete() {
        Paste stored = pasteExpiringAt(null);
        when(repository.findById("abc12345")).thenReturn(Optional.of(stored));

        ViewOutcome outcome = service.view("abc12345", null);

        assertThat(outcome).isEqualTo(new ViewOutcome.Viewed(stored, false));
        verify(repository, never()).delete(any());
    }

    @Test
    void viewIncrementsCounterAtomicallyInDatabase() {
        Paste stored = pasteExpiringAt(null);
        when(repository.findById("abc12345")).thenReturn(Optional.of(stored));

        service.view("abc12345", null);

        verify(repository).incrementViews("abc12345");
    }

    @Test
    void viewOfBurnPasteDeletesUnderLockAndReportsBurned() {
        Paste stored = pasteExpiringAt(null);
        stored.setBurnAfterRead(true);
        when(repository.findById("abc12345")).thenReturn(Optional.of(stored));
        when(repository.findByIdForUpdate("abc12345")).thenReturn(Optional.of(stored));

        ViewOutcome outcome = service.view("abc12345", null);

        assertThat(outcome).isEqualTo(new ViewOutcome.Viewed(stored, true));
        verify(repository).delete(stored);
    }

    @Test
    void viewDoesNotCountBurnReads() {
        Paste stored = pasteExpiringAt(null);
        stored.setBurnAfterRead(true);
        when(repository.findById("abc12345")).thenReturn(Optional.of(stored));
        when(repository.findByIdForUpdate("abc12345")).thenReturn(Optional.of(stored));

        service.view("abc12345", null);

        verify(repository, never()).incrementViews(any());
    }

    @Test
    void viewThrowsWhenConcurrentReaderAlreadyBurnedIt() {
        Paste stored = pasteExpiringAt(null);
        stored.setBurnAfterRead(true);
        when(repository.findById("abc12345")).thenReturn(Optional.of(stored));
        when(repository.findByIdForUpdate("abc12345")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.view("abc12345", null))
                .isInstanceOf(PasteNotFoundException.class);
        verify(repository, never()).delete(any());
    }

    @Test
    void viewThrowsForMissingPaste() {
        when(repository.findById("missing1")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.view("missing1", null))
                .isInstanceOf(PasteNotFoundException.class);
    }

    @Test
    void viewAsksForPasswordWhenProtectedAndNoneGiven() {
        Paste stored = protectedPaste();
        when(repository.findById("abc12345")).thenReturn(Optional.of(stored));

        ViewOutcome outcome = service.view("abc12345", null);

        assertThat(outcome).isEqualTo(new ViewOutcome.PasswordRequired(false));
        verify(repository, never()).incrementViews(any());
    }

    @Test
    void viewFlagsWrongPasswordAttempt() {
        Paste stored = protectedPaste();
        when(repository.findById("abc12345")).thenReturn(Optional.of(stored));
        when(passwordEncoder.matches("wrong", "$2a$hash")).thenReturn(false);

        ViewOutcome outcome = service.view("abc12345", "wrong");

        assertThat(outcome).isEqualTo(new ViewOutcome.PasswordRequired(true));
    }

    @Test
    void viewUnlocksWithCorrectPassword() {
        Paste stored = protectedPaste();
        when(repository.findById("abc12345")).thenReturn(Optional.of(stored));
        when(passwordEncoder.matches("s3cret", "$2a$hash")).thenReturn(true);

        ViewOutcome outcome = service.view("abc12345", "s3cret");

        assertThat(outcome).isEqualTo(new ViewOutcome.Viewed(stored, false));
        verify(repository).incrementViews("abc12345");
    }

    private void stubFreeId(String id) {
        when(idGenerator.generate()).thenReturn(id);
        when(repository.existsById(id)).thenReturn(false);
        when(repository.save(any(Paste.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    private Paste pasteExpiringAt(Instant expiresAt) {
        Paste paste = new Paste();
        paste.setId("abc12345");
        paste.setContent("content");
        paste.setCreatedAt(NOW.minusSeconds(3600));
        paste.setExpiresAt(expiresAt);
        return paste;
    }

    private Paste protectedPaste() {
        Paste paste = pasteExpiringAt(null);
        paste.setPasswordHash("$2a$hash");
        return paste;
    }
}
