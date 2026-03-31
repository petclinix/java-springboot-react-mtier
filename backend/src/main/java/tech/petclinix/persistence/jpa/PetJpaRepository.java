package tech.petclinix.persistence.jpa;

import jakarta.persistence.criteria.Path;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import tech.petclinix.logic.domain.Username;
import tech.petclinix.persistence.entity.OwnerEntity;
import tech.petclinix.persistence.entity.OwnerEntity_;
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

        public static Specification<PetEntity> byOwnerUsername(Username ownerUsername) {
            return (root, query, cb) -> {
                Path<OwnerEntity> ownerPath = root.get(PetEntity_.owner);
                return cb.equal(ownerPath.get(OwnerEntity_.username), ownerUsername.value());
            };
        }

        public static Specification<PetEntity> byId(Long id) {
            return (root, query, cb) ->
                    cb.equal(root.get(PetEntity_.id), id);
        }
    }
}
