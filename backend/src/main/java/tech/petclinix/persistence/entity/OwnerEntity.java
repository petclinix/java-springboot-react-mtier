package tech.petclinix.persistence.entity;

import jakarta.persistence.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Entity
@DiscriminatorValue("O")   // type = "O"
public class OwnerEntity extends UserEntity {

    @OneToMany(mappedBy = "owner", cascade = CascadeType.ALL)
    private List<PetEntity> pets = new ArrayList<>();

    protected OwnerEntity() {
        // JPA requires a no-arg constructor
    }

    public OwnerEntity(String username, String passwordHash) {
        super(username, passwordHash);
    }

    public List<PetEntity> getPets() {
        return Collections.unmodifiableList(pets);
    }

    public void addPet(PetEntity pet) {
        if (!pets.contains(pet)) {
            pets.add(pet);
        }
        pet.setOwner(this);
    }

    @Override
    public <T> T accept(UserVisitor<T> visitor) {
        return visitor.visitOwner(this);
    }
}
