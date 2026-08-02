package tech.petclinix.persistence.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;

import static java.util.Objects.requireNonNull;

@Entity
@Table(name = "activity_log")
public class ActivityLogEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String username;

    @Column(nullable = false)
    private String action;

    @Column(nullable = false)
    private LocalDateTime timestamp;

    protected ActivityLogEntity() {
        // JPA requires a no-arg constructor
    }

    public ActivityLogEntity(String username, String action) {
        this.username = requireNonNull(username, "username must not be null");
        this.action = requireNonNull(action, "action must not be null");
        this.timestamp = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public String getAction() {
        return action;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }
}
