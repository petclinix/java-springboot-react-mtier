package tech.petclinix.persistence.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;

import static java.util.Objects.requireNonNull;

@Entity
@Table(name = "appointments")
public class AppointmentEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    private VetEntity vet;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    private PetEntity pet;

    private LocalDateTime startAt;

    protected AppointmentEntity() {
        // JPA requires a no-arg constructor
    }

    public AppointmentEntity(VetEntity vet, PetEntity pet,LocalDateTime startAt) {
        this.vet = requireNonNull(vet, "vet must not be null");
        this.pet = requireNonNull(pet, "pet must not be null");
        this.startAt = requireNonNull(startAt, "startAt must not be null");
    }

    public Long getId() {
        return id;
    }

    public VetEntity getVet() {
        return vet;
    }

    public PetEntity getPet() {
        return pet;
    }

    public LocalDateTime getStartAt() {
        return startAt;
    }
}
