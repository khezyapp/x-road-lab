package kh.gov.smecu.portal.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "portal")
public record PortalProperties(Provider provider, XRoad xroad) {

    public PortalProperties {
        if (provider == null) {
            provider = new Provider("http://localhost:8081/api");
        }
        if (xroad == null) {
            xroad = new XRoad(false,
                    "http://localhost:4310",
                    "DEV/BIZ/SME-CU/PORTAL",
                    "/r1/DEV/GOV/MOC/REGISTRY/businessLookup");
        }
    }

    public record Provider(String baseUrl) {
    }

    public record XRoad(
            boolean enabled,
            String securityServerUrl,
            String clientId,
            String servicePath
    ) {
    }
}
