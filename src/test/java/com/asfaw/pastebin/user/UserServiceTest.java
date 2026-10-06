package com.asfaw.pastebin.user;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    private static final Instant NOW = Instant.parse("2026-01-15T12:00:00Z");

    @Mock
    private UserRepository repository;

    @Mock
    private PasswordEncoder passwordEncoder;

    private UserService service;

    @BeforeEach
    void setUp() {
        service = new UserService(repository, passwordEncoder, Clock.fixed(NOW, ZoneOffset.UTC));
    }

    @Test
    void registerHashesPasswordAndStampsCreation() {
        when(repository.existsByUsernameIgnoreCase("asfaw")).thenReturn(false);
        when(passwordEncoder.encode("longenoughpw")).thenReturn("$2a$hash");
        when(repository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        User user = service.register("asfaw", "longenoughpw");

        assertThat(user.getUsername()).isEqualTo("asfaw");
        assertThat(user.getPasswordHash()).isEqualTo("$2a$hash");
        assertThat(user.getCreatedAt()).isEqualTo(NOW);
    }

    @Test
    void registerRejectsTakenUsernameCaseInsensitively() {
        when(repository.existsByUsernameIgnoreCase("Asfaw")).thenReturn(true);

        assertThatThrownBy(() -> service.register("Asfaw", "longenoughpw"))
                .isInstanceOf(UsernameTakenException.class);
        verify(repository, never()).save(any());
    }
}
