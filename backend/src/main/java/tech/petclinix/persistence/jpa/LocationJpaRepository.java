package tech.petclinix.persistence.jpa;

import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import tech.petclinix.persistence.entity.*;

import java.util.Optional;

public interface LocationJpaRepository extends JpaRepository<LocationEntity, Long>, JpaSpecificationExecutor<LocationEntity> {
    Optional<LocationEntity> findByName(String name);

    Optional<LocationEntity> findByVetAndName(VetEntity vet, String name);

    public static class Specifications {
        public static Specification<LocationEntity> byVet(VetEntity vet) {
            return (root, query, cb) ->
                    cb.equal(root.get(LocationEntity_.vet), vet);
        }
    }
}
