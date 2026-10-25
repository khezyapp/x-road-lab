package kh.gov.smecu.portal.application;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

/**
 * Provider side of the reverse X-Road flow: MOC submits an application to SME-CU.
 *
 * <p>The X-Road service code is {@code applicationSubmit}; the SS forwards
 * {@code /r1/DEV/BIZ/SME-CU/PORTAL/applicationSubmit/applications} here as
 * {@code POST /api/applications}.
 *
 * <p>Storage is in-memory on purpose: the lab is disposable and this endpoint exists to
 * prove the reverse direction, not to model a real registry.
 */
@RestController
@RequestMapping("/api/applications")
public class ApplicationController {

    private final Map<String, ApplicationReceipt> receipts = new ConcurrentHashMap<>();

    @PostMapping
    public ResponseEntity<ApplicationReceipt> submit(@RequestBody(required = false) ApplicationRequest request) {
        if (request == null || isBlank(request.businessId()) || isBlank(request.applicantName())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "businessId and applicantName are required");
        }

        String id = "APP-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        ApplicationReceipt receipt = new ApplicationReceipt(
                id,
                request.businessId(),
                request.applicantName(),
                request.notes(),
                "RECEIVED",
                Instant.now().toString());

        receipts.put(id, receipt);
        return ResponseEntity.status(HttpStatus.CREATED).body(receipt);
    }

    @GetMapping
    public List<ApplicationReceipt> list() {
        return List.copyOf(receipts.values());
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
