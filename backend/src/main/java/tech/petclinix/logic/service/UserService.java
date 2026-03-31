package tech.petclinix.logic.service;

import jakarta.persistence.EntityNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tech.petclinix.logic.domain.DomainUser;
import tech.petclinix.logic.domain.UserType;
import tech.petclinix.logic.service.mapper.UserMapper;
import tech.petclinix.persistence.entity.AdminEntity;
import tech.petclinix.persistence.entity.OwnerEntity;
import tech.petclinix.persistence.entity.UserEntity;
import tech.petclinix.persistence.entity.VetEntity;
import tech.petclinix.persistence.jpa.UserJpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

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
        UserEntity saved;
        if(UserType.OWNER == userType) {
            var entity = new OwnerEntity(username, hashed);
            saved = repository.save(entity);
        }else if(UserType.VET == userType) {
            var entity = new VetEntity(username, hashed);
            saved = repository.save(entity);
        }else if(UserType.ADMIN == userType) {
            var entity = new AdminEntity(username, hashed);
            saved = repository.save(entity);
        }else throw new IllegalArgumentException("Invalid user type");
        return UserMapper.toDomain(saved);
    }

    public Optional<UserEntity> authenticate(String username, String rawPassword) {
        return repository.findByUsername(username)
                .filter(e -> passwordEncoder.matches(rawPassword, e.getPasswordHash()))
                .filter(UserEntity::isActive);
    }

    public List<DomainUser> findAll() {
        return repository.findAll().stream()
                .map(UserMapper::toDomain)
                .toList();
    }

    @Transactional
    public DomainUser deactivate(Long id) {
        var entity = repository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("User not found: " + id));
        entity.setActive(false);
        return UserMapper.toDomain(repository.save(entity));
    }

    @Transactional
    public DomainUser activate(Long id) {
        var user = repository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("User not found: " + id));
        user.setActive(true);
        return UserMapper.toDomain(repository.save(user));
    }
}
