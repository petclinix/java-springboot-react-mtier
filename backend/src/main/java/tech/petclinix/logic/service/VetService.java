package tech.petclinix.logic.service;

import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import tech.petclinix.persistence.entity.VetEntity;
import tech.petclinix.persistence.jpa.VetJpaRepository.Specifications;
import tech.petclinix.persistence.jpa.VetJpaRepository;

import java.util.List;
import java.util.Optional;

@Service
public class VetService {

    private final VetJpaRepository repository;

    public VetService(VetJpaRepository repository) {
        this.repository = repository;
    }

    public List<VetEntity> findAll() {
        return repository.findAll();
    }

    public VetEntity retrieveById(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Vet not found: " + id));
    }

    public VetEntity retrieveByUsername(String username) {
        return findByUsername(username)
                .orElseThrow(() -> new EntityNotFoundException("Vet not found: " + username));
    }

    public Optional<VetEntity> findByUsername(String username) {
        return repository.findOne(Specifications.byUsername(username));
    }

}
