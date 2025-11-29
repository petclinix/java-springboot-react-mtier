package tech.petclinix.persistence.entity;

import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Entity
@DiscriminatorValue("O")   // userType = "O"
public class OwnerEntity extends UserEntity {

    @OneToMany(mappedBy = "owner", cascade = CascadeType.ALL)
    private List<PetEntity> pets = new ArrayList<>();

    public List<PetEntity> getPets() {
        return Collections.unmodifiableList(pets);
    }

    public void addPet(PetEntity pet) {
        if(!pets.contains(pet)) {
            pets.add(pet);
        }
        pet.setOwner(this);
    }


}
