package tech.petclinix.persistence.jpa;

import jakarta.persistence.criteria.Path;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import tech.petclinix.logic.domain.Username;
import tech.petclinix.persistence.entity.*;

import java.util.List;
import java.util.Optional;

public interface VisitJpaRepository extends JpaRepository<VisitEntity, Long>, JpaSpecificationExecutor<VisitEntity> {


    public static class Specifications {

        public static Specification<VisitEntity> byAppointment(AppointmentEntity appointment) {
            return (root, query, cb) ->
                    cb.equal(root.get(VisitEntity_.appointment), appointment);
        }

        public static Specification<VisitEntity> byPet(PetEntity pet) {
            return (root, query, cb) -> {
                Path<AppointmentEntity> appointmentPath = root.get(VisitEntity_.appointment);
                return cb.equal(appointmentPath.get(AppointmentEntity_.pet), pet);
            };
        }

        private Specifications() {
        }


    }
}
