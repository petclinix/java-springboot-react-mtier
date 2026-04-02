package tech.petclinix.logic.service;

import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import tech.petclinix.logic.domain.Username;
import tech.petclinix.logic.domain.Vet;
import tech.petclinix.logic.service.mapper.EntityMapper;
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

    public List<Vet> findAll() {
        return repository.findAll().stream()
                .map(EntityMapper::toVet)
                .toList();
    }

    public VetEntity retrieveById(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Vet not found: " + id));
    }

    public VetEntity retrieveByUsername(Username vetUsername) {
        return findByUsername(vetUsername)
                .orElseThrow(() -> new EntityNotFoundException("Vet not found: " + vetUsername.value()));
    }

    public Optional<VetEntity> findByUsername(Username vetUsername) {
        return repository.findOne(Specifications.byUsername(vetUsername));
    }

}
