package tech.petclinix.logic.service;

import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import tech.petclinix.persistence.entity.OwnerEntity;
import tech.petclinix.persistence.jpa.OwnerJpaRepository;
import tech.petclinix.persistence.jpa.OwnerJpaRepository.Specifications;
import java.util.Optional;

@Service
public class OwnerService {

    private final OwnerJpaRepository repository;

    public OwnerService(OwnerJpaRepository repository) {
        this.repository = repository;
    }

    public OwnerEntity retrieveByUsername(String username) {
        return findByUsername(username)
                .orElseThrow(() -> new EntityNotFoundException("Owner not found: " + username));
    }


    public Optional<OwnerEntity> findByUsername(String username) {
        return repository.findOne(Specifications.byUsername(username));
    }

}
