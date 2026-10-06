package com.asfaw.pastebin.apikey;

import com.asfaw.pastebin.user.User;
import com.asfaw.pastebin.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
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
class ApiKeyServiceTest {

    private static final Instant NOW = Instant.parse("2026-01-15T12:00:00Z");

    @Mock
    private ApiKeyRepository repository;

    @Mock
    private UserRepository userRepository;

    private ApiKeyService service;

    @BeforeEach
    void setUp() {
        service = new ApiKeyService(repository, userRepository, Clock.fixed(NOW, ZoneOffset.UTC));
    }

    @Test
    void issueStoresOnlyTheHashOfTheKey() {
        User user = new User();
        user.setUsername("asfaw");
        when(userRepository.findByUsername("asfaw")).thenReturn(Optional.of(user));

        ApiKeyService.IssuedKey issued = service.issue("asfaw", "ci");

        ArgumentCaptor<ApiKey> captor = ArgumentCaptor.forClass(ApiKey.class);
        verify(repository).save(captor.capture());
        ApiKey saved = captor.getValue();

        assertThat(issued.plaintextKey()).startsWith("pb_");
        assertThat(saved.getKeyHash()).isNotEqualTo(issued.plaintextKey());
        assertThat(saved.getKeyHash()).isEqualTo(ApiKeyService.sha256Hex(issued.plaintextKey()));
        assertThat(saved.getCreatedAt()).isEqualTo(NOW);
    }

    @Test
    void authenticateMatchesByHash() {
        User user = new User();
        user.setUsername("asfaw");
        ApiKey key = new ApiKey();
        key.setId(7L);
        key.setUser(user);
        key.setRateLimitPerMinute(42);
        when(repository.findByKeyHash(ApiKeyService.sha256Hex("pb_secret"))).thenReturn(Optional.of(key));

        assertThat(service.authenticate("pb_secret"))
                .contains(new ApiKeyAuth(7L, "asfaw", 42));
        assertThat(service.authenticate("pb_wrong")).isEmpty();
    }

    @Test
    void revokeRejectsNonOwner() {
        User user = new User();
        user.setUsername("asfaw");
        ApiKey key = new ApiKey();
        key.setUser(user);
        when(repository.findById(7L)).thenReturn(Optional.of(key));

        assertThatThrownBy(() -> service.revoke(7L, "intruder"))
                .isInstanceOf(org.springframework.security.access.AccessDeniedException.class);
        verify(repository, never()).delete(any());
    }
}
