package tech.petclinix.persistence.jpa;

import jakarta.persistence.criteria.Path;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import tech.petclinix.logic.domain.AppointmentStatus;
import tech.petclinix.logic.domain.Username;
import tech.petclinix.persistence.entity.*;

import java.time.LocalDate;

public interface AppointmentJpaRepository
        extends JpaRepository<AppointmentEntity, Long>,
                JpaSpecificationExecutor<AppointmentEntity>,
                AppointmentRepositoryCustom {

    public static class Specifications {
        public static Specification<AppointmentEntity> byVet(VetEntity vet) {
            return (root, query, cb) ->
                    cb.equal(root.get(AppointmentEntity_.vet), vet);
        }

        public static Specification<AppointmentEntity> byPet(PetEntity pet) {
            return (root, query, cb) ->
                    cb.equal(root.get(AppointmentEntity_.pet), pet);
        }

        public static Specification<AppointmentEntity> byOwnerUsername(Username ownerUsername) {
            return (root, query, cb) -> {
                Path<PetEntity> petPath = root.get(AppointmentEntity_.pet);
                Path<OwnerEntity> ownerPath = petPath.get(PetEntity_.owner);
                return cb.equal(ownerPath.get(OwnerEntity_.username), ownerUsername.value());
            };
        }

        public static Specification<AppointmentEntity> byVetUsername(Username vetUsername) {
            return (root, query, cb) -> {
                Path<VetEntity> vetPath = root.get(AppointmentEntity_.vet);
                return cb.equal(vetPath.get(VetEntity_.username), vetUsername.value());
            };
        }

        public static Specification<AppointmentEntity> byId(Long id) {
            return (root, query, cb) ->
                    cb.equal(root.get(AppointmentEntity_.id), id);
        }

        /** Appointments for the given vet that start on the given calendar date. */
        public static Specification<AppointmentEntity> byVetOnDate(VetEntity vet, LocalDate date) {
            return (root, query, cb) -> {
                var startOfDay = date.atStartOfDay();
                var startOfNextDay = date.plusDays(1).atStartOfDay();
                return cb.and(
                        cb.equal(root.get(AppointmentEntity_.vet), vet),
                        cb.greaterThanOrEqualTo(root.get(AppointmentEntity_.startAt), startOfDay),
                        cb.lessThan(root.get(AppointmentEntity_.startAt), startOfNextDay)
                );
            };
        }

        /** Appointments that have not yet reached a terminal state (BOOKED or CONFIRMED). */
        public static Specification<AppointmentEntity> active() {
            return (root, query, cb) ->
                    root.get(AppointmentEntity_.status).in(AppointmentStatus.BOOKED, AppointmentStatus.CONFIRMED);
        }

        private Specifications() {
        }
    }
}
