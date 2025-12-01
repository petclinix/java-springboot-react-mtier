package tech.petclinix.persistence.jpa;

import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import tech.petclinix.persistence.entity.OwnerEntity;
import tech.petclinix.persistence.entity.OwnerEntity_;

public interface OwnerJpaRepository extends JpaRepository<OwnerEntity, Long>, JpaSpecificationExecutor<OwnerEntity> {

    public static class Specifications {

        public static Specification<OwnerEntity> byUsername(String username) {
            return (root, query, cb) ->
                    cb.equal(root.get(OwnerEntity_.username), username);
        }
    }
}
