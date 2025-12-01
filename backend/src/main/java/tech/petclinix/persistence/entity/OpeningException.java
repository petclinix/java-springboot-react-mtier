package tech.petclinix.persistence.entity;

import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
@Table(
        name = "opening_exception",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"location_id", "date"})
        }
)
public class OpeningException {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private LocalDate date;

    /**
     * If true → the entire day is closed.
     * If false → periods define special hours for that day.
     */
    @Column(nullable = false)
    private boolean closed = false;

    private String note;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "location_id")
    private LocationEntity location;

    public OpeningException() {
        // JPA requires a no-arg constructor
    }

    public OpeningException(LocationEntity location, LocalDate date, boolean closed, String note) {
        this.location = location;
        this.date = date;
        this.closed = closed;
        this.note = note;
    }

    // Getters and setters
    public Long getId() { return id; }
    public LocalDate getDate() { return date; }
    public void setDate(LocalDate date) { this.date = date; }
    public boolean isClosed() { return closed; }
    public void setClosed(boolean closed) { this.closed = closed; }
    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }
    public LocationEntity getLocation() { return location; }
    public void setLocation(LocationEntity location) { this.location = location; }


}
