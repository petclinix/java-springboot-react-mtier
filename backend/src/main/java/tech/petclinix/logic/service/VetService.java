package tech.petclinix.logic.service;

import tech.petclinix.logic.domain.exception.NotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
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

    @Transactional(readOnly = true)
    public List<Vet> findAll() {
        return repository.findAll().stream()
                .map(EntityMapper::toVet)
                .toList();
    }

    /* default */ VetEntity retrieveByUsername(Username vetUsername) {
        return findByUsername(vetUsername)
                .orElseThrow(() -> new NotFoundException("Vet not found: " + vetUsername.value()));
    }

    /* default */ Optional<VetEntity> findByUsername(Username vetUsername) {
        return repository.findOne(Specifications.byUsername(vetUsername));
    }

}
