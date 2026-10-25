package kh.gov.moc.registry.business;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Service;

/**
 * In-memory mock registry. Replace with a database when the lab moves beyond
 * the service-interaction phase.
 */
@Service
public class BusinessRegistry {

    private final Map<String, Business> businesses = new LinkedHashMap<>();

    public BusinessRegistry() {
        add(new Business("B-1001", "KH-2024-001001", "Angkor Coffee Co., Ltd.",
                "ACTIVE", LocalDate.of(2024, 3, 12), "Sok Dara"));
        add(new Business("B-1002", "KH-2024-001002", "Mekong Logistics Co., Ltd.",
                "ACTIVE", LocalDate.of(2024, 6, 4), "Lim Sreyneang"));
        add(new Business("B-1003", "KH-2025-001003", "Tonle Craft Studio",
                "PENDING", LocalDate.of(2025, 1, 20), "Chan Vibol"));
    }

    private void add(Business business) {
        businesses.put(business.id(), business);
    }

    public List<Business> findAll() {
        return List.copyOf(businesses.values());
    }

    public Optional<Business> findById(String id) {
        return Optional.ofNullable(businesses.get(id));
    }
}
