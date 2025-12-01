package tech.petclinix.logic.service;

import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import tech.petclinix.persistence.entity.VetEntity;
import tech.petclinix.persistence.jpa.VetJpaRepository.Specifications;
import tech.petclinix.persistence.jpa.VetJpaRepository;

import java.util.Optional;

@Service
public class VetService {

    private final VetJpaRepository repository;

    public VetService(VetJpaRepository repository) {
        this.repository = repository;
    }

    public VetEntity retrieveByUsername(String username) {
        return findByUsername(username)
                .orElseThrow(() -> new EntityNotFoundException("Vet not found: " + username));
    }

    public Optional<VetEntity> findByUsername(String username) {
        return repository.findOne(Specifications.byUsername(username));
    }

}
