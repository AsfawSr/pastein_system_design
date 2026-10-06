package com.asfaw.pastebin.paste;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PasteServiceTest {

    @Mock
    private PasteRepository repository;

    @Mock
    private IdGenerator idGenerator;

    @InjectMocks
    private PasteService service;

    @Test
    void createAssignsIdTitleContentAndTimestamp() {
        when(idGenerator.generate()).thenReturn("abc12345");
        when(repository.existsById("abc12345")).thenReturn(false);
        when(repository.save(any(Paste.class))).thenAnswer(inv -> inv.getArgument(0));

        Paste paste = service.create("my title", "hello world");

        assertThat(paste.getId()).isEqualTo("abc12345");
        assertThat(paste.getTitle()).isEqualTo("my title");
        assertThat(paste.getContent()).isEqualTo("hello world");
        assertThat(paste.getCreatedAt()).isNotNull();
    }

    @Test
    void createRetriesWhenGeneratedIdCollides() {
        when(idGenerator.generate()).thenReturn("taken123", "free4567");
        when(repository.existsById("taken123")).thenReturn(true);
        when(repository.existsById("free4567")).thenReturn(false);
        when(repository.save(any(Paste.class))).thenAnswer(inv -> inv.getArgument(0));

        Paste paste = service.create(null, "content");

        assertThat(paste.getId()).isEqualTo("free4567");
    }

    @Test
    void createFailsAfterTooManyCollisions() {
        when(idGenerator.generate()).thenReturn("same1234");
        when(repository.existsById("same1234")).thenReturn(true);

        assertThatThrownBy(() -> service.create(null, "content"))
                .isInstanceOf(IllegalStateException.class);
        verify(repository, never()).save(any());
    }

    @Test
    void findDelegatesToRepository() {
        Paste stored = new Paste();
        when(repository.findById("abc12345")).thenReturn(Optional.of(stored));

        assertThat(service.find("abc12345")).containsSame(stored);
    }
}
