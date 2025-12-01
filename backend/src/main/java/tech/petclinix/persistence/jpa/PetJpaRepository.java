package tech.petclinix.persistence.jpa;

import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import tech.petclinix.persistence.entity.OwnerEntity;
import tech.petclinix.persistence.entity.PetEntity;
import tech.petclinix.persistence.entity.PetEntity_;

import java.util.Optional;

public interface PetJpaRepository extends JpaRepository<PetEntity, Long>, JpaSpecificationExecutor<PetEntity> {
    Optional<PetEntity> findByName(String name);

    public static class Specifications {
        public static Specification<PetEntity> byOwner(OwnerEntity owner) {
            return (root, query, cb) ->
                    cb.equal(root.get(PetEntity_.owner), owner);
        }
    }
}
