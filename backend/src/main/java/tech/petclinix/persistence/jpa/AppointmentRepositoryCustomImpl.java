package tech.petclinix.persistence.jpa;

import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import tech.petclinix.logic.domain.AppointmentStatus;
import tech.petclinix.logic.domain.StatsData.VetAppointmentCount;
import tech.petclinix.persistence.entity.AppointmentEntity;
import tech.petclinix.persistence.entity.AppointmentEntity_;
import tech.petclinix.persistence.entity.VetEntity;
import tech.petclinix.persistence.entity.VetEntity_;

import java.time.LocalDateTime;
import java.util.List;

public class AppointmentRepositoryCustomImpl implements AppointmentRepositoryCustom {

    private final EntityManager entityManager;

    public AppointmentRepositoryCustomImpl(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Override
    public List<VetAppointmentCount> countPerVet() {
        var cb = entityManager.getCriteriaBuilder();
        var cq = cb.createQuery(VetAppointmentCount.class);
        var root = cq.from(AppointmentEntity.class);
        var vetUsername = root.get(AppointmentEntity_.vet).get(VetEntity_.username);
        var count = cb.count(root);
        cq.select(cb.construct(VetAppointmentCount.class, vetUsername, count))
          .groupBy(vetUsername)
          .orderBy(cb.desc(count));
        return entityManager.createQuery(cq).getResultList();
    }

    @Override
    public List<AppointmentEntity> findOverlappingForUpdate(VetEntity vet, LocalDateTime startAt, LocalDateTime endsAt) {
        var cb = entityManager.getCriteriaBuilder();
        var cq = cb.createQuery(AppointmentEntity.class);
        var root = cq.from(AppointmentEntity.class);
        cq.select(root).where(
                cb.equal(root.get(AppointmentEntity_.vet), vet),
                root.get(AppointmentEntity_.status).in(AppointmentStatus.BOOKED, AppointmentStatus.CONFIRMED),
                cb.lessThan(root.get(AppointmentEntity_.startAt), endsAt),
                cb.greaterThan(root.get(AppointmentEntity_.endsAt), startAt)
        );
        return entityManager.createQuery(cq)
                .setLockMode(LockModeType.PESSIMISTIC_WRITE)
                .getResultList();
    }
}
