package com.asfaw.pastebin.apikey;

import com.asfaw.pastebin.user.User;
import com.asfaw.pastebin.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;

import java.time.Clock;

@Service
@RequiredArgsConstructor
public class ApiKeyService {

    private static final String ALPHABET = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";

    private final ApiKeyRepository repository;
    private final UserRepository userRepository;
    private final Clock clock;
    private final SecureRandom random = new SecureRandom();

    public record IssuedKey(Long id, String label, String plaintextKey) {
    }

    @Transactional
    public IssuedKey issue(String username, String label) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalStateException("Unknown user: " + username));
        String plaintext = "pb_" + randomToken(32);
        ApiKey key = new ApiKey();
        key.setUser(user);
        key.setKeyHash(sha256Hex(plaintext));
        key.setLabel(label);
        key.setCreatedAt(clock.instant());
        repository.save(key);
        // the plaintext is returned exactly once and never stored
        return new IssuedKey(key.getId(), label, plaintext);
    }

    @Transactional(readOnly = true)
    public Optional<ApiKeyAuth> authenticate(String plaintextKey) {
        return repository.findByKeyHash(sha256Hex(plaintextKey))
                .map(key -> new ApiKeyAuth(key.getId(), key.getUser().getUsername(), key.getRateLimitPerMinute()));
    }

    @Transactional(readOnly = true)
    public List<ApiKey> listFor(String username) {
        return repository.findByUserUsernameOrderByCreatedAtDesc(username);
    }

    @Transactional
    public void revoke(Long id, String username) {
        ApiKey key = repository.findById(id)
                .orElseThrow(() -> new ApiKeyNotFoundException(id));
        if (!key.getUser().getUsername().equals(username)) {
            throw new AccessDeniedException("Not your API key");
        }
        repository.delete(key);
    }

    private String randomToken(int length) {
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(ALPHABET.charAt(random.nextInt(ALPHABET.length())));
        }
        return sb.toString();
    }

    static String sha256Hex(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 unavailable", e);
        }
    }
}
