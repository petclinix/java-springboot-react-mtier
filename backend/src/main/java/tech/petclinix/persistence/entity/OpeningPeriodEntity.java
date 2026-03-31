package tech.petclinix.persistence.entity;

import jakarta.persistence.*;

import java.time.LocalTime;

@Entity
@Table(
        name = "opening_period",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"location_id", "day_of_week", "sort_order"})
        }
)
public class OpeningPeriodEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 1 = Monday ... 7 = Sunday
     */
    @Column(name = "day_of_week", nullable = false)
    private int dayOfWeek;

    @Column(nullable = false)
    private LocalTime startTime;

    @Column(nullable = false)
    private LocalTime endTime;

    /**
     * Multiple shifts per day. E.g.:
     * sortOrder = 0 → morning
     * sortOrder = 1 → afternoon
     */
    @Column(nullable = false)
    private int sortOrder;

    @ManyToOne(cascade = {CascadeType.MERGE, CascadeType.PERSIST, CascadeType.REFRESH}, fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "location_id")
    private LocationEntity location;

    public OpeningPeriodEntity() {
        // JPA requires a no-arg constructor
    }

    public OpeningPeriodEntity(LocationEntity location, int dayOfWeek, LocalTime startTime, LocalTime endTime, int sortOrder) {
        this.location = location;
        this.dayOfWeek = dayOfWeek;
        this.startTime = startTime;
        this.endTime = endTime;
        this.sortOrder = sortOrder;
    }

    // Getters and setters
    public Long getId() {
        return id;
    }

    public int getDayOfWeek() {
        return dayOfWeek;
    }

    public void setDayOfWeek(int dayOfWeek) {
        this.dayOfWeek = dayOfWeek;
    }

    public LocalTime getStartTime() {
        return startTime;
    }

    public void setStartTime(LocalTime startTime) {
        this.startTime = startTime;
    }

    public LocalTime getEndTime() {
        return endTime;
    }

    public void setEndTime(LocalTime endTime) {
        this.endTime = endTime;
    }

    public int getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(int sortOrder) {
        this.sortOrder = sortOrder;
    }

    public LocationEntity getLocation() {
        return location;
    }

    public void setLocation(LocationEntity location) {
        this.location = location;
    }


}
