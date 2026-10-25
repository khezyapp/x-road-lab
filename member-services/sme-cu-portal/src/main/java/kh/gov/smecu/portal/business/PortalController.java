package kh.gov.smecu.portal.business;

import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * SME-CU portal API. It exposes a stable portal contract and delegates the
 * actual lookup to the MOC registry through {@link BusinessDirectoryClient}.
 */
@RestController
@RequestMapping("/api/portal/businesses")
public class PortalController {

    private final BusinessDirectoryClient directory;

    public PortalController(BusinessDirectoryClient directory) {
        this.directory = directory;
    }

    @GetMapping
    public List<Business> list() {
        return directory.findAll();
    }

    @GetMapping("/{id}")
    public Business get(@PathVariable String id) {
        return directory.findById(id);
    }
}
