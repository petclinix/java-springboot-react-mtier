package tech.petclinix.persistence.jpa;

import jakarta.persistence.criteria.Path;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import tech.petclinix.persistence.entity.*;

import java.util.List;
import java.util.Optional;

public interface AppointmentJpaRepository extends JpaRepository<AppointmentEntity, Long>, JpaSpecificationExecutor<AppointmentEntity> {

    @Query("SELECT a.vet.username, COUNT(a) FROM AppointmentEntity a GROUP BY a.vet.username ORDER BY COUNT(a) DESC")
    List<Object[]> countByVetUsername();

    public static class Specifications {
        public static Specification<AppointmentEntity> byVet(VetEntity vet) {
            return (root, query, cb) ->
                    cb.equal(root.get(AppointmentEntity_.vet), vet);
        }

        public static Specification<AppointmentEntity> byPet(PetEntity pet) {
            return (root, query, cb) ->
                    cb.equal(root.get(AppointmentEntity_.pet), pet);
        }

        public static Specification<AppointmentEntity> byOwnerUsername(String ownerUsername) {
            return (root, query, cb) -> {
                Path<PetEntity> petPath = root.get(AppointmentEntity_.pet);
                Path<OwnerEntity> ownerPath = petPath.get(PetEntity_.owner);
                return cb.equal(ownerPath.get(OwnerEntity_.username), ownerUsername);
            };
        }

        public static Specification<AppointmentEntity> byVetUsername(String vetUsername) {
            return (root, query, cb) -> {
                Path<VetEntity> vetPath = root.get(AppointmentEntity_.vet);
                return cb.equal(vetPath.get(VetEntity_.username), vetUsername);
            };
        }

        public static Specification<AppointmentEntity> byId(Long id) {
            return (root, query, cb) ->
                    cb.equal(root.get(AppointmentEntity_.id), id);
        }
    }
}
