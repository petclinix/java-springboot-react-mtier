package tech.petclinix.persistence.entity;

import jakarta.persistence.*;
import tech.petclinix.logic.domain.Gender;
import tech.petclinix.logic.domain.Species;

import java.time.LocalDate;

import static java.util.Objects.requireNonNull;

@Entity
@Table(name = "pets")
public class PetEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    private Species species;

    @Enumerated(EnumType.STRING)
    private Gender gender;

    private LocalDate birthDate;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    private OwnerEntity owner;

    protected PetEntity() {
        // JPA requires a no-arg constructor
    }

    public PetEntity(String name, OwnerEntity owner) {
        this.name = requireNonNull(name, "name must not be null");
        this.owner = requireNonNull(owner, "owner must not be null");
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public Species getSpecies() {
        return species;
    }

    public Gender getGender() {
        return gender;
    }

    public LocalDate getBirthDate() {
        return birthDate;
    }

    public OwnerEntity getOwner() {
        return owner;
    }

    // PetEntity — correct
    public void setOwner(OwnerEntity newOwner) {
        if (this.owner == newOwner) return;
        this.owner = newOwner;
        newOwner.addPet(this);
    }
}
