package tech.petclinix.persistence.entity;

import jakarta.persistence.*;

import java.time.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents a physical location (clinic, branch, shop) that has
 * opening hours (weekly recurring + overrides).
 */
@Entity
@Table(name = "location")
public class LocationEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(nullable = false, updatable = false)
    private VetEntity vet;

    /**
     * Display username of the location (e.g. “PetClinix Vienna”)
     */
    @Column(nullable = false)
    private String name;

    /**
     * IANA timezone string, e.g. "Europe/Vienna", required for correct
     * opening-hours / “is open now?” calculations.
     */
    @Column(nullable = false)
    private String zoneId;

    // Optional address information
    private String street;
    private String postalCode;
    private String city;
    private String country;

    @OneToMany(
            mappedBy = "location",
            cascade = CascadeType.ALL,
            orphanRemoval = true
    )
    @OrderBy("dayOfWeek asc, sortOrder asc")
    private List<OpeningPeriodEntity> weeklyPeriods = new ArrayList<>();

    @OneToMany(
            mappedBy = "location",
            cascade = CascadeType.ALL,
            orphanRemoval = true
    )
    @OrderBy("date asc")
    private List<OpeningOverrideEntity> overrides = new ArrayList<>();

    public LocationEntity() {
        // JPA requires a no-arg constructor
    }

    public LocationEntity(VetEntity vet, String name, ZoneId zoneId) {
        this(vet, name, zoneId.getId());
    }

    public LocationEntity(VetEntity vet, String name, String zoneId) {
        this.vet = vet;
        this.name = name;
        this.zoneId = zoneId;
    }

    public Long getId() {
        return id;
    }

    public VetEntity getVet() {
        return vet;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getZoneId() {
        return zoneId;
    }

    public void setZoneId(String zoneId) {
        this.zoneId = zoneId;
    }

    public String getStreet() {
        return street;
    }

    public void setStreet(String street) {
        this.street = street;
    }

    public String getPostalCode() {
        return postalCode;
    }

    public void setPostalCode(String postalCode) {
        this.postalCode = postalCode;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public String getCountry() {
        return country;
    }

    public void setCountry(String country) {
        this.country = country;
    }

    public List<OpeningPeriodEntity> getWeeklyPeriods() {
        return weeklyPeriods;
    }

    public List<OpeningOverrideEntity> getOverrides() {
        return overrides;
    }

    public boolean isOpenAt(Instant instantUtc) {
        ZoneId zone = ZoneId.of(this.zoneId);
        ZonedDateTime zdt = instantUtc.atZone(zone);
        LocalDate localDate = zdt.toLocalDate();
        DayOfWeek dow = zdt.getDayOfWeek();
        LocalTime time = zdt.toLocalTime();

        // 1) Check exception first
        OpeningOverrideEntity ex = overrides.stream()
                .filter(e -> e.getDate().equals(localDate))
                .findFirst()
                .orElse(null);

        if (ex != null) {
            if (ex.isClosed()) return false;
            if (ex.getOpenTime() == null || ex.getCloseTime() == null) return false;
            return isTimeInPeriod(time, ex.getOpenTime(), ex.getCloseTime());
        }

        // 2) check weekly periods for this day
        int dowVal = dow.getValue(); // 1..7
        return weeklyPeriods.stream()
                .filter(p -> p.getDayOfWeek() == dowVal)
                .anyMatch(p -> isTimeInPeriod(time, p.getStartTime(), p.getEndTime()));
    }

    private boolean isTimeInPeriod(LocalTime t, LocalTime start, LocalTime end) {
        // Normal case start < end
        return (!t.isBefore(start)) && t.isBefore(end);
    }


}
