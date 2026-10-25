package kh.gov.moc.registry.business;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class BusinessRegistryTest {

    private final BusinessRegistry registry = new BusinessRegistry();

    @Test
    void returnsSeededBusinesses() {
        assertThat(registry.findAll()).hasSize(3);
    }

    @Test
    void findsKnownBusiness() {
        assertThat(registry.findById("B-1001"))
                .get()
                .extracting(Business::name)
                .isEqualTo("Angkor Coffee Co., Ltd.");
    }

    @Test
    void returnsEmptyForUnknownBusiness() {
        assertThat(registry.findById("missing")).isEmpty();
    }
}
