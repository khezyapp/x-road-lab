# Spring Boot 4 Integration

Status: **Verified** patterns from the X-Road REST/SOAP protocols and the compiled `member-services` example on this host (Spring Boot 4.1.1, Java 25).

**Implemented example:** [`13-member-services.md`](13-member-services.md) describes the compiled SME-CU consumer portal and MOC provider in `member-services/`.

## Choose the integration style

| Situation | Use |
| --- | --- |
| New service, CamDX-style API | **REST** — normal Spring MVC controller + `RestClient` consumer. |
| Provider publishes a WSDL | **SOAP** — `xrd4j` adapter or CXF, with X-Road headers. |
| Legacy SOAP provider, new consumer | SOAP client; do not try to convert REST↔SOAP in the SS. |

Start with REST. It removes SOAP header plumbing and matches CamDX's stated preference.

## REST consumer

The consumer calls **its own Security Server**, not the provider. The service ID is in the path; the client identity is a header.

```java
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
public class BusinessLookupClient {

    private final RestClient http;

    public BusinessLookupClient(BusinessLookupProperties props) {
        this.http = RestClient.builder()
            .baseUrl(props.securityServerUrl() + props.businessLookupPath())
            .defaultHeader("X-Road-Client", props.clientId())
            .build();
    }

    public BusinessRecord lookup(String businessId) {
        return http.get()
            .uri("/businesses/{id}", businessId)
            .retrieve()
            .body(BusinessRecord.class);
    }
}
```

`spring-boot-starter-webmvc` in Boot 4.1.1 does not provide an injectable `RestClient.Builder` bean in this setup. Build the client with `RestClient.builder()`, or define your own builder bean.

Configuration-friendly version:

```yaml
portal:
  xroad:
    security-server-url: http://ss-smecu:8080
    client-id: DEV/BIZ/SME-CU/PORTAL
    business-lookup-path: /r1/DEV/GOV/MOC/REGISTRY/businessLookup
```

```java
this.http = RestClient.builder()
    .baseUrl(props.securityServerUrl() + props.businessLookupPath())
    .defaultHeader("X-Road-Client", props.clientId())
    .build();
```

Notes:

- `baseUrl` points at the **consumer SS proxy port** (8080 inside the Docker network).
- Do not put the provider IS URL here. The SS resolves the service ID to the provider URL.
- `X-Road-Client` must be a **subsystem** ID, not just a member ID.
- Add `X-Road-UserId` when a real end user initiated the action; it is optional metadata, not authentication.
- Set connect/read timeouts. The SS has its own service timeout; a hung consumer should fail before the UI does.

### Error handling

`RestClient` throws by default on 4xx/5xx. Distinguish X-Road errors from provider errors by the `X-Road-Error` header:

```java
try {
    return http.get().uri("/businesses/{id}", id)
        .retrieve()
        .body(BusinessRecord.class);
} catch (RestClientResponseException e) {
    String xroadError = e.getResponseHeaders() == null
        ? null
        : e.getResponseHeaders().getFirst("X-Road-Error");
    if (xroadError != null) {
        // X-Road/SS failure; log type + X-Road-Request-Id + body
        throw new XRoadCallException(xroadError, e.getResponseBodyAsString(), e);
    }
    // Provider application failure; preserve its status/body semantics
    throw e;
}
```

Log `X-Road-Request-Id` and the error `detail` UUID; they are the keys to the SS logs.

## REST provider

The provider is an ordinary Spring MVC controller. The SS forwards a normal HTTP request and expects a normal HTTP response.

```java
@RestController
@RequestMapping("/api")
public class RegistryController {

    @GetMapping("/businesses/{id}")
    public ResponseEntity<BusinessRecord> get(
            @PathVariable String id,
            @RequestHeader(value = "X-Road-Client", required = false) String clientId,
            @RequestHeader(value = "X-Road-UserId", required = false) String userId) {

        BusinessRecord record = service.find(id);
        if (record == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(record);
    }
}
```

Rules:

- Do **not** set `X-Road-*` response headers. The provider SS overwrites them. Return normal headers only.
- Do not attempt to verify the X-Road signature in the app. The SS already did that.
- Use normal HTTP status codes. A provider 404/500 is passed back as the provider's response, without `X-Road-Error`.
- If you need the caller identity for business authorization, read `X-Road-Client`; but the SS ACL is the primary access control. Do not trust a header that a caller could spoof outside X-Road.
- The provider must be reachable from the **SS container** at the configured service URL. Use the container name, not `localhost`.

## Internal TLS

For the first lab, set the SS client connection type to **HTTP** (private Docker network). This avoids a second certificate workflow.

For a realistic setup:

- **Consumer IS → SS:** SS connection type **HTTPS** (client auth). The Spring app needs a client certificate/keystore and a truststore containing the SS internal TLS certificate. Upload the app's certificate to the SS under the client's **Internal Servers** tab.
- **SS → provider IS:** use an `https://` service URL. If **Verify TLS certificate** is enabled in the SS service settings, the SS must trust the provider certificate; upload it to the SS internal TLS certificate list.
- **HTTPS NOAUTH** is a middle ground: encryption without client authentication. It still requires the IS to trust the SS certificate.
- The SS's own internal TLS certificate can be exported from its UI and imported into the app truststore.

Do not use `curl -k` equivalents in application code. Disabling verification in the lab hides the exact certificate mistakes you need to learn.

## SOAP provider with xrd4j

`xrd4j` is the NIIS Java library for X-Road SOAP adapters and clients. Its README says it is **compatible with X-Road 7**, requires Java 21+, and the current source uses **Jakarta Servlet 6.1** (so it can sit in a Spring Boot 3/4 servlet container).

The upstream example adapter is a useful reference:

- `/mnt/data/opensource/x-roads/xrd4j/example-adapter`
- Spring Boot **3.5.16**, Java **21**, CXF JAX-WS, `xrd4j-server` / `xrd4j-common`
- servlet base class: `org.niis.xrd4j.server.AbstractAdapterServlet`
- WSDL: `example-adapter.wsdl`, serves at `/example-adapter/Endpoint?wsdl`
- services: `getRandom`, `helloService`, `listPeople`, `personDetails`, `storeAttachments`, `getAttachments`

For Spring Boot 4.1.x:

- Keep the Jakarta imports; no `javax.servlet`.
- Bump the Spring Boot plugin/BOM to 4.1.x and use Java 25.
- Keep `xrd4j` at its current version; verify compatibility with the X-Road instance you run.
- Re-test WSDL generation and SOAP header copying; these are the fragile parts.

The adapter must:

1. Parse the X-Road request headers.
2. Process the body.
3. Copy **all** request headers into the response in the same order and values.
4. Let the SS add `requestHash` and transport security.

`AbstractAdapterServlet` handles the header copying for you; hand-written servlets usually get it wrong.

## SOAP consumer

Use `xrd4j`'s `SOAPClientImpl` (or a generated CXF client plus manual X-Road headers). The request must include `client`, `service`, `id`, and `protocolVersion=4.0`. The SS URL is the consumer SS proxy; the service ID selects the provider.

Do not build a SOAP client that posts directly to the provider SS with a made-up URL. The X-Road service ID, not the provider address, is the routing key.

## Spring Boot 4 notes

- Use **Java 25** and **Spring Boot 4.1.x** to match the host and the upstream v8 stack. Spring Boot 4 uses Jakarta EE APIs; any `javax.*` dependency is a red flag.
- `RestClient` is the simplest synchronous client. `WebClient` is fine if the app is already reactive.
- Keep X-Road IDs in configuration, not hard-coded in controllers. The same app should be runnable as SME-CU or MOC by changing environment variables.
- Package as a normal executable JAR and run it as a container on `xroad-network`.
- If you use Spring Security, remember that X-Road already authenticated the **organization**; your app still needs end-user authentication and business authorization.

## Docker networking

```yaml
services:
  sme-cu-portal:
    build: ./member-services/sme-cu-portal
    container_name: sme-cu-portal
    networks: [xroad-network]
    environment:
      PORTAL_XROAD_ENABLED: "true"
      PORTAL_XROAD_SECURITY_SERVER_URL: http://ss-smecu:8080
      PORTAL_XROAD_CLIENT_ID: DEV/BIZ/SME-CU/PORTAL
      PORTAL_XROAD_SERVICE_PATH: /r1/DEV/GOV/MOC/REGISTRY/businessLookup
```

The provider SS service URL must be `http://moc-registry:8080/api`, not `http://localhost:4701`. `localhost` inside the SS container is the SS itself, and the `/api` base path must match the controller.

## Test strategy

1. **Provider direct:** call the Spring controller on its container/host port. This tests business logic without X-Road.
2. **Through X-Road:** call the consumer SS proxy with `X-Road-Client`. This tests registration, ACL, service URL, and protocol.
3. **Assert headers:** require `X-Road-Id` and `X-Road-Request-Hash`; assert no `X-Road-Error`.
4. **Negative tests:** wrong client ID → ACL failure; wrong service code → unknown service; disabled description → SS error; provider down → `Server.ServerProxy.NetworkError`.
5. **Reverse direction:** repeat for MOC → SME-CU. A one-way test hides half the registration work.

A `curl` that works is stronger evidence than a unit test that mocks the SS. Keep both.
