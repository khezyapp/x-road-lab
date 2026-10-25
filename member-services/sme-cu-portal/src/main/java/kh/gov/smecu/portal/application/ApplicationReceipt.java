package kh.gov.smecu.portal.application;

/** Stored result of an application submission. */
public record ApplicationReceipt(
        String id,
        String businessId,
        String applicantName,
        String notes,
        String status,
        String receivedAt) {
}
