package com.asfaw.pastebin.user;

import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository repository;
    private final PasswordEncoder passwordEncoder;
    private final Clock clock;

    @Transactional
    public User register(String username, String rawPassword) {
        if (repository.existsByUsernameIgnoreCase(username)) {
            throw new UsernameTakenException(username);
        }
        User user = new User();
        user.setUsername(username);
        user.setPasswordHash(passwordEncoder.encode(rawPassword));
        user.setCreatedAt(clock.instant());
        return repository.save(user);
    }
}
