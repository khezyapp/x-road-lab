package kh.gov.smecu.portal.error;

public class BusinessNotFoundException extends RuntimeException {

    private final String businessId;

    public BusinessNotFoundException(String businessId, Throwable cause) {
        super("Business not found: " + businessId, cause);
        this.businessId = businessId;
    }

    public String businessId() {
        return businessId;
    }
}
