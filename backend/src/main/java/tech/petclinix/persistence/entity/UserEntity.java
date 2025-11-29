package tech.petclinix.persistence.entity;

import jakarta.persistence.*;
import tech.petclinix.logic.service.UserType;
import tech.petclinix.persistence.jpa.UserTypeConverter;

import static java.util.Objects.requireNonNull;

@Entity
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "type")
@Table(name = "users")
public class UserEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String username;

    @Column(nullable = false)
    private String passwordHash;

    @Convert(converter = UserTypeConverter.class)
    @Column(nullable = false, updatable = false)
    private UserType userType;

    protected UserEntity() {
        // JPA requires a no-arg constructor
    }

    public UserEntity(String username, String passwordHash, UserType userType) {
        this.username = requireNonNull(username, "username must not be null");
        this.passwordHash = requireNonNull(passwordHash, "passwordHash must not be null");
        this.userType = requireNonNull(userType, "userType must not be null");
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

    public UserType getUserType() {
        return userType;
    }
}
