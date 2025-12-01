package tech.petclinix.persistence.jpa;

import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import tech.petclinix.persistence.entity.OwnerEntity;
import tech.petclinix.persistence.entity.OwnerEntity_;
import tech.petclinix.persistence.entity.VetEntity;
import tech.petclinix.persistence.entity.VetEntity_;

public interface VetJpaRepository extends JpaRepository<VetEntity, Long>, JpaSpecificationExecutor<VetEntity> {

    public static class Specifications {

        public static Specification<VetEntity> byUsername(String username) {
            return (root, query, cb) ->
                    cb.equal(root.get(VetEntity_.username), username);
        }
    }
}
