package tech.petclinix.persistence.entity;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;

@Entity
@DiscriminatorValue("A")   // type = "A"
public class AdminEntity extends UserEntity {

    protected AdminEntity() {
        // JPA requires a no-arg constructor
    }

    public AdminEntity(String username, String passwordHash) {
        super(username, passwordHash);
    }

    @Override
    public <T> T accept(UserVisitor<T> visitor) {
        return visitor.visitAdmin(this);
    }
}
