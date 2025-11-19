package tech.petclinix.logic.service;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tech.petclinix.persistence.entity.UserEntity;
import tech.petclinix.persistence.jpa.UserJpaRepository;
import tech.petclinix.persistence.mapper.UserMapper;

import java.util.Optional;

@Service
public class UserService {

    private final UserJpaRepository repository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserJpaRepository repository, PasswordEncoder passwordEncoder) {
        this.repository = repository;
        this.passwordEncoder = passwordEncoder;
    }

    public Optional<DomainUser> findByUsername(String username) {
        return repository.findByUsername(username).map(UserMapper::toDomain);
    }

    public boolean existsByUsername(String username) {
        return repository.findByUsername(username).isPresent();
    }


    @Transactional
    public DomainUser register(String username, String rawPassword, UserType userType) {
        var hashed = passwordEncoder.encode(rawPassword);

        var entity = new UserEntity(username, hashed, userType);
        var saved = repository.save(entity);
        return UserMapper.toDomain(saved);
    }

    public boolean authenticate(String username, String rawPassword) {
        return repository.findByUsername(username)
                .map(e -> passwordEncoder.matches(rawPassword, e.getPasswordHash()))
                .orElse(false);
    }
}
