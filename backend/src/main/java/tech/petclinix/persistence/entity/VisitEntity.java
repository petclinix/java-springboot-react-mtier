package tech.petclinix.persistence.entity;

import jakarta.persistence.*;

import static java.util.Objects.requireNonNull;

@Entity
@Table(name = "VISIT")
public class VisitEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(nullable = false, updatable = false)
    private AppointmentEntity appointment;

    private String vetSummary;

    private String ownerSummary;

    private String vaccination;

    protected VisitEntity() {
        // JPA requires a no-arg constructor
    }

    public VisitEntity(AppointmentEntity appointment) {
        this.appointment = requireNonNull(appointment, "appointment must not be null");
    }

    public Long getId() {
        return id;
    }

    public AppointmentEntity getAppointment() {
        return appointment;
    }

    public String getVetSummary() {
        return vetSummary;
    }

    public void setVetSummary(String vetSummary) {
        this.vetSummary = vetSummary;
    }

    public String getOwnerSummary() {
        return ownerSummary;
    }

    public void setOwnerSummary(String ownerSummary) {
        this.ownerSummary = ownerSummary;
    }

    public String getVaccination() {
        return vaccination;
    }

    public void setVaccination(String vaccination) {
        this.vaccination = vaccination;
    }
}
