package tech.petclinix.logic.service;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tech.petclinix.persistence.entity.PetEntity;
import tech.petclinix.persistence.entity.UserEntity;
import tech.petclinix.persistence.jpa.PetJpaRepository;
import tech.petclinix.persistence.jpa.UserJpaRepository;
import tech.petclinix.persistence.mapper.PetMapper;
import tech.petclinix.persistence.mapper.UserMapper;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class PetService {

    private final PetJpaRepository repository;

    public PetService(PetJpaRepository repository) {
        this.repository = repository;
    }

    public List<DomainPet> findAll() {
        return repository.findAll().stream()
                .map(PetMapper::toDomain)
                .toList();
    }

    public Optional<DomainPet> findByName(String name) {
        return repository.findByName(name).map(PetMapper::toDomain);
    }


    @Transactional
    public DomainPet persist(String name) {
        var entity = new PetEntity(name);
        var saved = repository.save(entity);
        return PetMapper.toDomain(saved);
    }


}
