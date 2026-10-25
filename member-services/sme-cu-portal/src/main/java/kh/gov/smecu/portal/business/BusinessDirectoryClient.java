package kh.gov.smecu.portal.business;

import java.util.List;
import kh.gov.smecu.portal.config.PortalProperties;
import kh.gov.smecu.portal.error.BusinessNotFoundException;
import kh.gov.smecu.portal.error.ProviderUnavailableException;
import kh.gov.smecu.portal.error.XRoadCallException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

/**
 * Calls the MOC registry either directly (development) or through the SME-CU
 * Security Server (X-Road mode).
 */
@Component
public class BusinessDirectoryClient {

    private final RestClient client;

    @Autowired
    public BusinessDirectoryClient(PortalProperties properties) {
        this(RestClient.builder(), properties);
    }

    BusinessDirectoryClient(RestClient.Builder builder, PortalProperties properties) {
        PortalProperties.XRoad xroad = properties.xroad();

        String baseUrl = xroad.enabled()
                ? xroad.securityServerUrl() + xroad.servicePath()
                : properties.provider().baseUrl();

        RestClient.Builder configured = builder.baseUrl(baseUrl);
        if (xroad.enabled()) {
            configured.defaultHeader("X-Road-Client", xroad.clientId());
        }
        this.client = configured.build();
    }

    public List<Business> findAll() {
        try {
            List<Business> businesses = client.get()
                    .uri("/businesses")
                    .retrieve()
                    .body(new ParameterizedTypeReference<List<Business>>() {
                    });
            return businesses == null ? List.of() : businesses;
        } catch (RestClientResponseException e) {
            throw translate(e, null);
        } catch (RestClientException e) {
            throw new ProviderUnavailableException("MOC registry is unavailable", e);
        }
    }

    public Business findById(String id) {
        try {
            Business business = client.get()
                    .uri("/businesses/{id}", id)
                    .retrieve()
                    .body(Business.class);
            if (business == null) {
                throw new BusinessNotFoundException(id, null);
            }
            return business;
        } catch (RestClientResponseException e) {
            throw translate(e, id);
        } catch (RestClientException e) {
            throw new ProviderUnavailableException("MOC registry is unavailable", e);
        }
    }

    private RuntimeException translate(RestClientResponseException e, String businessId) {
        String xroadError = e.getResponseHeaders() == null
                ? null
                : e.getResponseHeaders().getFirst("X-Road-Error");

        if (xroadError != null) {
            String requestId = e.getResponseHeaders() == null
                    ? null
                    : e.getResponseHeaders().getFirst("X-Road-Request-Id");
            return new XRoadCallException(xroadError, requestId, e.getResponseBodyAsString(), e);
        }
        if (businessId != null && e.getStatusCode().value() == 404) {
            return new BusinessNotFoundException(businessId, e);
        }
        return e;
    }
}
