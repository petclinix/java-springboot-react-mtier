package tech.petclinix.persistence.entity;

import jakarta.persistence.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Entity
@DiscriminatorValue("V")   // type = "V"
public class VetEntity extends UserEntity {

    @OneToMany(mappedBy = "vet", cascade = CascadeType.ALL)
    private List<LocationEntity> locations = new ArrayList<>();

    protected VetEntity() {
        // JPA requires a no-arg constructor
    }

    public VetEntity(String username, String passwordHash) {
        super(username, passwordHash);
    }

    public List<LocationEntity> getLocations() {
        return Collections.unmodifiableList(locations);
    }

    @Override
    public <T> T accept(UserVisitor<T> visitor) {
        return visitor.visitVet(this);
    }
}
