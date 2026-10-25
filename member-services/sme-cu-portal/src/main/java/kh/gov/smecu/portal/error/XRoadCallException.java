package kh.gov.smecu.portal.error;

public class XRoadCallException extends RuntimeException {

    private final String xroadError;
    private final String requestId;

    public XRoadCallException(String xroadError, String requestId, String responseBody, Throwable cause) {
        super("X-Road error " + xroadError + ": " + responseBody, cause);
        this.xroadError = xroadError;
        this.requestId = requestId;
    }

    public String xroadError() {
        return xroadError;
    }

    public String requestId() {
        return requestId;
    }
}
