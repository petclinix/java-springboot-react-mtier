package tech.petclinix.persistence.entity;

import jakarta.persistence.*;

import static java.util.Objects.requireNonNull;

@Entity
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "type")
@Table(name = "users")
public abstract class UserEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String username;

    @Column(nullable = false)
    private String passwordHash;

    @Column(nullable = false)
    private boolean active = true;

    protected UserEntity() {
        // JPA requires a no-arg constructor
    }

    protected UserEntity(String username, String passwordHash) {
        this.username = requireNonNull(username, "username must not be null");
        this.passwordHash = requireNonNull(passwordHash, "passwordHash must not be null");
    }

    public Long getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public abstract <T> T accept(UserVisitor<T> visitor);

    public static interface UserVisitor<T> {
        T visitOwner(OwnerEntity owner);

        T visitVet(VetEntity vet);

        T visitAdmin(AdminEntity admin);
    }
}
