package tech.petclinix.persistence.entity;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalTime;

@Entity
@Table(
        name = "opening_exception",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"location_id", "date"})
        }
)
public class OpeningOverrideEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private LocalDate date;

    private LocalTime openTime;
    private LocalTime closeTime;

    /**
     * If true → the entire day is closed (openTime/closeTime are null).
     * If false → openTime/closeTime define special hours for that day.
     */
    @Column(nullable = false)
    private boolean closed = false;

    private String reason;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "location_id")
    private LocationEntity location;

    public OpeningOverrideEntity() {
        // JPA requires a no-arg constructor
    }

    public OpeningOverrideEntity(LocationEntity location, LocalDate date, LocalTime openTime, LocalTime closeTime, boolean closed, String reason) {
        this.location = location;
        this.date = date;
        this.openTime = openTime;
        this.closeTime = closeTime;
        this.closed = closed;
        this.reason = reason;
    }

    // Getters and setters
    public Long getId() { return id; }
    public LocalDate getDate() { return date; }
    public void setDate(LocalDate date) { this.date = date; }
    public LocalTime getOpenTime() { return openTime; }
    public void setOpenTime(LocalTime openTime) { this.openTime = openTime; }
    public LocalTime getCloseTime() { return closeTime; }
    public void setCloseTime(LocalTime closeTime) { this.closeTime = closeTime; }
    public boolean isClosed() { return closed; }
    public void setClosed(boolean closed) { this.closed = closed; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
    public LocationEntity getLocation() { return location; }
    public void setLocation(LocationEntity location) { this.location = location; }

}
