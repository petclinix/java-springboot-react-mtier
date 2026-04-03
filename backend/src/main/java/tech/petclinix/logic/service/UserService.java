package tech.petclinix.logic.service;

import tech.petclinix.logic.domain.exception.NotFoundException;
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

import java.util.List;
import java.util.Optional;

@Service
public class UserService {

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
        return UserMapper.toDomain(repository.save(user));
    }

    public Optional<DomainUser> authenticate(Username username, String rawPassword) {
        return repository.findOne(Specifications.byUsername(username))
                .filter(e -> passwordEncoder.matches(rawPassword, e.getPasswordHash()))
                .filter(UserEntity::isActive)
                .map(UserMapper::toDomain);
    }

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
        return UserMapper.toDomain(repository.save(entity));
    }

    @Transactional
    public DomainUser activate(Long id) {
        var user = repository.findById(id)
                .orElseThrow(() -> new NotFoundException("User not found: " + id));
        user.setActive(true);
        return UserMapper.toDomain(repository.save(user));
    }
}
