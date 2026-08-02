package tech.petclinix.persistence.jpa;

import org.springframework.data.jpa.repository.JpaRepository;
import tech.petclinix.persistence.entity.ActivityLogEntity;

public interface ActivityLogJpaRepository extends JpaRepository<ActivityLogEntity, Long> {
}
