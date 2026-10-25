package kh.gov.smecu.portal.business;

import java.time.LocalDate;

/**
 * Business-registration record as exposed by the SME-CU portal.
 * Mirrors the MOC registry contract; a real system would use a shared schema.
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
