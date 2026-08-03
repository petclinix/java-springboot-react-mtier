package tech.petclinix.persistence.entity;

import jakarta.persistence.*;
import tech.petclinix.logic.domain.AppointmentStatus;

import java.time.LocalDateTime;

import static java.util.Objects.requireNonNull;

@Entity
@Table(name = "appointments", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"vet_id", "start_at"})
})
public class AppointmentEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    private VetEntity vet;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    private LocationEntity location;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    private PetEntity pet;

    private LocalDateTime startAt;

    private LocalDateTime endsAt;

    @Enumerated(EnumType.STRING)
    private AppointmentStatus status;

    protected AppointmentEntity() {
        // JPA requires a no-arg constructor
    }

    public AppointmentEntity(LocationEntity location, PetEntity pet, LocalDateTime startAt, LocalDateTime endsAt) {
        this.location = requireNonNull(location, "location must not be null");
        this.vet = requireNonNull(location.getVet(), "location.vet must not be null");
        this.pet = requireNonNull(pet, "pet must not be null");
        this.startAt = requireNonNull(startAt, "startAt must not be null");
        this.endsAt = requireNonNull(endsAt, "endsAt must not be null");
        this.status = AppointmentStatus.BOOKED;
    }

    public void cancel() {
        this.status = AppointmentStatus.CANCELLED;
    }

    public Long getId() {
        return id;
    }

    public VetEntity getVet() {
        return vet;
    }

    public LocationEntity getLocation() {
        return location;
    }

    public PetEntity getPet() {
        return pet;
    }

    public LocalDateTime getStartAt() {
        return startAt;
    }

    public LocalDateTime getEndsAt() {
        return endsAt;
    }

    public AppointmentStatus getStatus() {
        return status;
    }
}
