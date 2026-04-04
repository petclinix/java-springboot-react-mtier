package tech.petclinix.logic.service;

import tech.petclinix.logic.domain.exception.InvalidCredentialsException;
import tech.petclinix.logic.domain.exception.NotFoundException;
import tech.petclinix.logic.domain.exception.UsernameAlreadyTakenException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tech.petclinix.logic.domain.DomainUser;
import tech.petclinix.logic.domain.UserType;
import tech.petclinix.logic.domain.Username;
import tech.petclinix.logic.service.mapper.UserMapper;
import tech.petclinix.persistence.entity.AdminEntity;
import tech.petclinix.persistence.entity.OwnerEntity;
import tech.petclinix.persistence.entity.UserEntity;
import tech.petclinix.persistence.entity.VetEntity;
import tech.petclinix.persistence.jpa.UserJpaRepository;
import tech.petclinix.persistence.jpa.UserJpaRepository.Specifications;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.List;
import java.util.Optional;

@Service
public class UserService {

    private static final Logger LOGGER = LoggerFactory.getLogger(UserService.class);

    private final UserJpaRepository repository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserJpaRepository repository, PasswordEncoder passwordEncoder) {
        this.repository = repository;
        this.passwordEncoder = passwordEncoder;
    }

    public Optional<DomainUser> findByUsername(Username username) {
        return repository.findOne(Specifications.byUsername(username))
                .map(UserMapper::toDomain);
    }

    @Transactional
    public DomainUser register(Username username, String rawPassword, UserType userType) {
        var hashed = passwordEncoder.encode(rawPassword);
        var user = switch (userType) {
            case OWNER -> new OwnerEntity(username.value(), hashed);
            case VET -> new VetEntity(username.value(), hashed);
            case ADMIN -> new AdminEntity(username.value(), hashed);
        };
        try {
            var saved = UserMapper.toDomain(repository.save(user));
            LOGGER.info("User registered: {} ({})", username.value(), userType);
            return saved;
        } catch (DataIntegrityViolationException e) {
            throw new UsernameAlreadyTakenException(username.value());
        }
    }

    @Transactional(readOnly = true)
    public DomainUser authenticate(Username username, String rawPassword) {
        return repository.findOne(Specifications.byUsername(username))
                .filter(e -> passwordEncoder.matches(rawPassword, e.getPasswordHash()))
                .filter(UserEntity::isActive)
                .map(UserMapper::toDomain)
                .orElseThrow(() -> {
                    LOGGER.warn("Authentication failed for username: {}", username.value());
                    return new InvalidCredentialsException();
                });
    }

    @Transactional(readOnly = true)
    public List<DomainUser> findAll() {
        return repository.findAll().stream()
                .map(UserMapper::toDomain)
                .toList();
    }

    @Transactional
    public DomainUser deactivate(Long id) {
        var entity = repository.findById(id)
                .orElseThrow(() -> new NotFoundException("User not found: " + id));
        entity.setActive(false);
        var saved = UserMapper.toDomain(repository.save(entity));
        LOGGER.info("User {} deactivated", entity.getUsername());
        return saved;
    }

    @Transactional
    public DomainUser activate(Long id) {
        var user = repository.findById(id)
                .orElseThrow(() -> new NotFoundException("User not found: " + id));
        user.setActive(true);
        var saved = UserMapper.toDomain(repository.save(user));
        LOGGER.info("User {} activated", user.getUsername());
        return saved;
    }
}
