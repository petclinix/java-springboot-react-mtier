package tech.petclinix.logic.service;

import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import tech.petclinix.logic.domain.Username;
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

    public OwnerEntity retrieveByUsername(Username username) {
        return findByUsername(username)
                .orElseThrow(() -> new EntityNotFoundException("Owner not found: " + username.value()));
    }


    public Optional<OwnerEntity> findByUsername(Username username) {
        return repository.findOne(Specifications.byUsername(username));
    }

}
