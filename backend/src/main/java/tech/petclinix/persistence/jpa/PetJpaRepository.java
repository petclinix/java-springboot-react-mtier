package tech.petclinix.persistence.jpa;

import org.springframework.data.jpa.repository.JpaRepository;
import tech.petclinix.persistence.entity.PetEntity;
import tech.petclinix.persistence.entity.UserEntity;

import java.util.Optional;

public interface PetJpaRepository extends JpaRepository<PetEntity, Long> {
    Optional<PetEntity> findByName(String name);
}
