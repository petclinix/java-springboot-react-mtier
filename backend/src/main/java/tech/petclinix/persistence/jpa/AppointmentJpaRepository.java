package tech.petclinix.persistence.jpa;

import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import tech.petclinix.persistence.entity.*;

import java.util.Optional;

public interface AppointmentJpaRepository extends JpaRepository<AppointmentEntity, Long>, JpaSpecificationExecutor<AppointmentEntity> {

    public static class Specifications {
        public static Specification<AppointmentEntity> byVet(VetEntity vet) {
            return (root, query, cb) ->
                    cb.equal(root.get(AppointmentEntity_.vet), vet);
        }

        public static Specification<AppointmentEntity> byPet(PetEntity pet) {
            return (root, query, cb) ->
                    cb.equal(root.get(AppointmentEntity_.pet), pet);
        }
    }
}
