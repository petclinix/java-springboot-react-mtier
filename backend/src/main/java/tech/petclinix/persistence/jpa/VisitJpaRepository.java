package tech.petclinix.persistence.jpa;

import org.springframework.data.jpa.repository.JpaRepository;
import tech.petclinix.persistence.entity.AppointmentEntity;
import tech.petclinix.persistence.entity.PetEntity;
import tech.petclinix.persistence.entity.VisitEntity;

import java.util.List;
import java.util.Optional;

public interface VisitJpaRepository extends JpaRepository<VisitEntity, Long> {

    Optional<VisitEntity> findByAppointment(AppointmentEntity appointment);

    List<VisitEntity> findAllByAppointment_Pet(PetEntity pet);
}
