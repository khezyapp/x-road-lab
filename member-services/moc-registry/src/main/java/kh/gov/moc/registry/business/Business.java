package kh.gov.moc.registry.business;

import java.time.LocalDate;

/**
 * Simple business-registration record returned by the MOC registry.
 * This is mock data for the X-Road interaction lab, not a real registry model.
 */
public record Business(
        String id,
        String registrationNumber,
        String name,
        String status,
        LocalDate registeredAt,
        String owner
) {
}
