package tech.petclinix.persistence.entity;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;

@Entity
@DiscriminatorValue("V")   // type = "V"
public class VetEntity extends UserEntity {

    protected VetEntity() {
        // JPA requires a no-arg constructor
    }

    public VetEntity(String username, String passwordHash) {
        super(username, passwordHash);
    }

    @Override
    public <T> T accept(UserVisitor<T> visitor) {
        return visitor.visitVet(this);
    }
}
