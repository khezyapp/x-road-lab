# Member Services — SME-CU Portal + MOC Registry

Two small Spring Boot 4 services used to practise the **integration**, not application development.

- **MOC registry** (`moc-registry`, port `8081`) — provider. Returns mock business-registration records.
- **SME-CU portal** (`sme-cu-portal`, port `8080`) — consumer. Exposes a portal API and calls the MOC registry.

Status: **built and tested on this host** (Spring Boot 4.1.1, Java 25, Gradle 9.7.1). Direct mode works; X-Road mode is wired but needs the three-organization lab's Security Servers, members, and ACLs.

## Composite build layout

`member-services/` is a Gradle **composite build**. Each service is its own independent Gradle build:

```text
member-services/
├── settings.gradle              # root: includeBuild(...)
├── build.gradle                 # buildAll / testAll aggregators
├── gradlew
├── moc-registry/                # independent build
│   ├── settings.gradle
│   ├── build.gradle
│   └── src/...
└── sme-cu-portal/               # independent build
    ├── settings.gradle
    ├── build.gradle
    └── src/...
```

The root does not compile code. It aggregates the two builds:

```bash
cd member-services
./gradlew buildAll    # both services, including tests
./gradlew testAll     # tests only
```

Run one service from its own build:

```bash
./gradlew -p moc-registry bootRun
./gradlew -p sme-cu-portal bootRun
```

## Prerequisites

- JDK 25 (the builds use a Java 25 toolchain).
- The Gradle wrapper downloads Gradle 9.7.1 on first use.
- No Docker or X-Road stack is needed for **direct mode**.

## Direct mode (start here)

This tests the app contract without X-Road. Run each command in its own terminal:

```bash
cd member-services
./gradlew -p moc-registry bootRun
```

```bash
cd member-services
./gradlew -p sme-cu-portal bootRun
```

The portal defaults to `portal.xroad.enabled=false` and calls `http://localhost:8081/api` directly.

### Call the provider

```bash
curl -sS http://localhost:8081/api/businesses | jq
curl -sS http://localhost:8081/api/businesses/B-1001 | jq
```

### Call the consumer portal

```bash
curl -sS http://localhost:8080/api/portal/businesses | jq
curl -sS http://localhost:8080/api/portal/businesses/B-1001 | jq
```

Expected: the portal returns the same records as the provider, because it calls the provider on your behalf.

### Submit an application (reverse flow provider)

The portal is also the provider of `applicationSubmit`:

```bash
curl -i -sS -X POST http://localhost:8080/api/applications \
  -H 'Content-Type: application/json' \
  -d '{"businessId":"B-1001","applicantName":"Sok Dara"}'

curl -sS http://localhost:8080/api/applications | jq
```

Expected: HTTP `201` with an `ApplicationReceipt` (`"status":"RECEIVED"`), then the receipt
in the list. Storage is in-memory; restarting the service clears it.

### Unknown id

```bash
curl -i -sS http://localhost:8080/api/portal/businesses/UNKNOWN
```

Expected: HTTP `404` with a `ProblemDetail` body containing `"businessId":"UNKNOWN"`.

## Mock data

| id | registration number | name | status |
| --- | --- | --- | --- |
| `B-1001` | `KH-2024-001001` | Angkor Coffee Co., Ltd. | `ACTIVE` |
| `B-1002` | `KH-2024-001002` | Mekong Logistics Co., Ltd. | `ACTIVE` |
| `B-1003` | `KH-2025-001003` | Tonle Craft Studio | `PENDING` |

## X-Road mode

In X-Road mode the portal calls the **SME-CU Security Server**, not the MOC app:

```text
sme-cu-portal
  GET http://<ss-smecu>:8080/r1/DEV/GOV/MOC/REGISTRY/businessLookup/businesses
  X-Road-Client: DEV/BIZ/SME-CU/PORTAL
        │
        ▼
SS-SMECU ──mutual TLS──► SS-MOC ──HTTP──► moc-registry:8080/api/businesses
```

Enable it with environment variables:

```bash
cd member-services
PORTAL_XROAD_ENABLED=true \
PORTAL_XROAD_SECURITY_SERVER_URL=http://localhost:4310 \
PORTAL_XROAD_CLIENT_ID=DEV/BIZ/SME-CU/PORTAL \
PORTAL_XROAD_SERVICE_PATH=/r1/DEV/GOV/MOC/REGISTRY/businessLookup \
./gradlew -p sme-cu-portal bootRun
```

Or pass Spring arguments:

```bash
./gradlew -p sme-cu-portal bootRun --args='--portal.xroad.enabled=true'
```

The defaults in `sme-cu-portal/src/main/resources/application.yml` already contain the planned lab IDs. `PORTAL_PROVIDER_BASE_URL` is ignored while X-Road mode is enabled.

This assumes the services run on the same Docker network as the Security Servers, where the provider is reachable as `moc-registry:8080`. If the apps run on the host, the SS service URL must use an address reachable from the SS container; update it in the service description.

### What must exist in X-Road

The default IDs match the planned three-organization topology, **not** the upstream `DEV/COM/1234` demo:

- Member `DEV/GOV/MOC`, subsystem `REGISTRY`, registered on `SS-MOC`.
- Member `DEV/BIZ/SME-CU`, subsystem `PORTAL`, registered on `SS-SMECU`.
- MOC's Security Server has a REST service description:
  - type: REST base path
  - URL: `http://moc-registry:8080/api`
  - service code: `businessLookup`
  - description enabled
- The MOC service ACL includes `DEV/BIZ/SME-CU/PORTAL`.
- Both Security Servers trust the test CA; global configuration is current.

The provider app does not need to know it is behind X-Road. It receives a normal HTTP request and returns normal HTTP.

### Why the upstream demo cannot be used as-is

The 7.8.3 demo exposes `mock1` at `http://isrest:8080/integration/mock_1`. That endpoint returns a test message, not the business JSON this consumer expects. Pointing the portal at `mock1` will fail deserialization. Use the three-org lab or register `moc-registry` as the provider service.

## Failure meanings

| Portal response | Meaning | Check |
| --- | --- | --- |
| `503 Provider unavailable` | The portal could not reach the MOC app (direct mode) or the SS. | Is `moc-registry` running? Is `PORTAL_PROVIDER_BASE_URL` correct? |
| `502 X-Road error` | The SS returned `X-Road-Error`; the failure is in X-Road. | Read the error type; check registration/ACL/service description. |
| `404 Business not found` | X-Road/provider worked, but no record has that id. | `GET /api/businesses` on the provider. |
| `500` with JSON parse error | The portal reached a provider that does not return the expected business contract. | Check the service URL; do not point it at the demo `mock1`. |
| `Connection refused` | Nothing is listening on 8080/8081/4310. | Start the app or the Security Server. |

The `X-Road error` response includes the `xroadError` type (for example `Server.ServerProxy.NetworkError`). Use `X-Road-Request-Id` and the body `detail` UUID from the SS logs; see `docs/x-road/09-troubleshooting.md`.

## Spring Boot 4 notes

- `spring-boot-starter-webmvc` does **not** auto-configure a `RestClient.Builder` bean in this setup. The consumer creates one with `RestClient.builder()`; the package-private constructor accepts a builder for tests.
- The apps use Java 25 and Spring Boot 4.1.1, matching the host and upstream X-Road 8's stack.
- `Business` is duplicated in both services on purpose: each member owns its contract. A real deployment would use a shared schema/versioned API.
- The provider logs `X-Road-Client` when the header is present, which helps confirm the SS forwarded the consumer subsystem.

## Next

1. Run direct mode and capture the two responses.
2. Register `moc-registry` in the three-org lab and enable X-Road mode.
3. Compare the portal response with the `X-Road-*` headers on the SS call.
4. Add the reverse flow: MOC consumes SME-CU's `applicationSubmit`.
