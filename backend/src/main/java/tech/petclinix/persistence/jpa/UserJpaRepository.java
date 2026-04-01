package tech.petclinix.persistence.jpa;

import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import tech.petclinix.logic.domain.Username;
import tech.petclinix.persistence.entity.*;

public interface UserJpaRepository extends JpaRepository<UserEntity, Long>, JpaSpecificationExecutor<UserEntity> {
    public static class Specifications {

        public static Specification<UserEntity> byUsername(Username username) {
            return (root, query, cb) ->
                    cb.equal(root.get(UserEntity_.username), username.value());
        }
        private Specifications() {
        }


    }
}
