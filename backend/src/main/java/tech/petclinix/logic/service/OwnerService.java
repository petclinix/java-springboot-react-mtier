package tech.petclinix.logic.service;

import tech.petclinix.logic.domain.exception.NotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
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

    @Transactional(readOnly = true)
    public OwnerEntity retrieveByUsername(Username username) {
        return findByUsername(username)
                .orElseThrow(() -> new NotFoundException("Owner not found: " + username.value()));
    }

    @Transactional(readOnly = true)
    public Optional<OwnerEntity> findByUsername(Username username) {
        return repository.findOne(Specifications.byUsername(username));
    }

}
