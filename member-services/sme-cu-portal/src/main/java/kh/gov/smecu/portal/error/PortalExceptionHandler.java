package kh.gov.smecu.portal.error;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class PortalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(PortalExceptionHandler.class);

    @ExceptionHandler(BusinessNotFoundException.class)
    public ResponseEntity<ProblemDetail> businessNotFound(BusinessNotFoundException e) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.NOT_FOUND, "No business found for id " + e.businessId());
        problem.setTitle("Business not found");
        problem.setProperty("businessId", e.businessId());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(problem);
    }

    @ExceptionHandler(XRoadCallException.class)
    public ResponseEntity<ProblemDetail> xroadFailure(XRoadCallException e) {
        log.warn("X-Road call failed: {}", e.xroadError());
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_GATEWAY, "The X-Road request failed");
        problem.setTitle("X-Road error");
        problem.setProperty("xroadError", e.xroadError());
        if (e.requestId() != null) {
            problem.setProperty("requestId", e.requestId());
        }
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(problem);
    }

    @ExceptionHandler(ProviderUnavailableException.class)
    public ResponseEntity<ProblemDetail> providerUnavailable(ProviderUnavailableException e) {
        log.warn("Provider unavailable: {}", e.getMessage());
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.SERVICE_UNAVAILABLE, "The MOC registry is unavailable");
        problem.setTitle("Provider unavailable");
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(problem);
    }
}
