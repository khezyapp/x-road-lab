package kh.gov.moc.registry.business;

import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/businesses")
public class BusinessController {

    private static final Logger log = LoggerFactory.getLogger(BusinessController.class);

    private final BusinessRegistry registry;

    public BusinessController(BusinessRegistry registry) {
        this.registry = registry;
    }

    @GetMapping
    public List<Business> list(
            @RequestHeader(value = "X-Road-Client", required = false) String xroadClient) {
        log.info("business list requested by xroadClient={}", xroadClient);
        return registry.findAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Business> get(
            @PathVariable String id,
            @RequestHeader(value = "X-Road-Client", required = false) String xroadClient) {
        log.info("business lookup id={} requested by xroadClient={}", id, xroadClient);
        return registry.findById(id)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }
}
