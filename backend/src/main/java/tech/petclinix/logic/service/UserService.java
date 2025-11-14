package tech.petclinix.logic.service;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class UserService {

    private final Map<String, String> users = new ConcurrentHashMap<>();
    private final PasswordEncoder passwordEncoder;

    public UserService(PasswordEncoder passwordEncoder) {
        this.passwordEncoder = passwordEncoder;
    }

    @PostConstruct
    public void init() {
        // Seed users
        users.put("user", passwordEncoder.encode("password"));
        users.put("alice", passwordEncoder.encode("alicepass"));
    }

    public boolean validateCredentials(String username, String rawPassword) {
        var hashed = users.get(username);
        if (hashed == null) return false;
        return passwordEncoder.matches(rawPassword, hashed);
    }

    public Optional<User> findByUsername(String username) {
        return users.containsKey(username) ? Optional.of(new User(username)) : Optional.empty();
    }

    public record User(String username) {
    }
}
