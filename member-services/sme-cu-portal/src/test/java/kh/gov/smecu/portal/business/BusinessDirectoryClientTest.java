package kh.gov.smecu.portal.business;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import kh.gov.smecu.portal.config.PortalProperties;
import kh.gov.smecu.portal.error.XRoadCallException;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class BusinessDirectoryClientTest {

    private static final String BUSINESS_JSON = """
            [{
              "id": "B-1001",
              "registrationNumber": "KH-2024-001001",
              "name": "Angkor Coffee Co., Ltd.",
              "status": "ACTIVE",
              "registeredAt": "2024-03-12",
              "owner": "Sok Dara"
            }]
            """;

    @Test
    void usesDirectProviderUrlWhenXroadIsDisabled() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        PortalProperties properties = new PortalProperties(
                new PortalProperties.Provider("http://localhost:8081/api"),
                new PortalProperties.XRoad(false, "", "", ""));

        BusinessDirectoryClient client = new BusinessDirectoryClient(builder, properties);

        server.expect(requestTo("http://localhost:8081/api/businesses"))
                .andRespond(withSuccess(BUSINESS_JSON, MediaType.APPLICATION_JSON));

        assertThat(client.findAll()).singleElement()
                .extracting(Business::id)
                .isEqualTo("B-1001");
        server.verify();
    }

    @Test
    void callsSecurityServerAndSendsXroadClientHeaderWhenEnabled() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        PortalProperties properties = new PortalProperties(
                new PortalProperties.Provider("http://localhost:8081/api"),
                new PortalProperties.XRoad(
                        true,
                        "http://localhost:4310",
                        "DEV/BIZ/SME-CU/PORTAL",
                        "/r1/DEV/GOV/MOC/REGISTRY/businessLookup"));

        BusinessDirectoryClient client = new BusinessDirectoryClient(builder, properties);

        server.expect(requestTo(
                        "http://localhost:4310/r1/DEV/GOV/MOC/REGISTRY/businessLookup/businesses"))
                .andExpect(header("X-Road-Client", "DEV/BIZ/SME-CU/PORTAL"))
                .andRespond(withSuccess(BUSINESS_JSON, MediaType.APPLICATION_JSON));

        assertThat(client.findAll()).singleElement()
                .extracting(Business::registrationNumber)
                .isEqualTo("KH-2024-001001");
        server.verify();
    }

    @Test
    void translatesXroadErrorHeader() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        PortalProperties properties = new PortalProperties(
                new PortalProperties.Provider("http://localhost:8081/api"),
                new PortalProperties.XRoad(true, "http://localhost:4310",
                        "DEV/BIZ/SME-CU/PORTAL", "/r1/DEV/GOV/MOC/REGISTRY/businessLookup"));

        BusinessDirectoryClient client = new BusinessDirectoryClient(builder, properties);

        server.expect(requestTo(
                        "http://localhost:4310/r1/DEV/GOV/MOC/REGISTRY/businessLookup/businesses"))
                .andRespond(withStatus(HttpStatus.BAD_GATEWAY)
                        .header("X-Road-Error", "Server.ServerProxy.NetworkError")
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("{}"));

        assertThatThrownBy(client::findAll)
                .isInstanceOf(XRoadCallException.class)
                .hasMessageContaining("Server.ServerProxy.NetworkError");
        server.verify();
    }
}
