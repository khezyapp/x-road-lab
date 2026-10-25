package kh.gov.smecu.portal.application;

/** Request body accepted by the applicationSubmit provider endpoint. */
public record ApplicationRequest(String businessId, String applicantName, String notes) {
}
