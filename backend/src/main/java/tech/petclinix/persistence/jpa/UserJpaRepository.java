package tech.petclinix.persistence.jpa;

import org.springframework.data.jpa.repository.JpaRepository;
import tech.petclinix.persistence.entity.UserEntity;
import java.util.Optional;

public interface UserJpaRepository extends JpaRepository<UserEntity, Long> {
    Optional<UserEntity> findByUsername(String username);
}
