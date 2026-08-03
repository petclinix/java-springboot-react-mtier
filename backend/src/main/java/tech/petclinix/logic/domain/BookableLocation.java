package tech.petclinix.logic.domain;

/**
 * Read-only view of a {@link tech.petclinix.persistence.entity.LocationEntity} exposed to
 * owners when choosing where to book an appointment.
 *
 * <p>Deliberately separate from {@link Location} (the vet's own write contract for managing
 * their locations) so that {@code vetUsername} does not leak into that path.
 */
public record BookableLocation(
        Long id,
        String name,
        String vetUsername,
        String zoneId,
        String street,
        String postalCode,
        String city,
        String country
) {
}
