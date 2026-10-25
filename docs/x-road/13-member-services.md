# Member Services (Spring Boot 4)

Status: **Verified** — built and tested on this host with Spring Boot 4.1.1, Java 25, and Gradle 9.7.1.

The runnable code lives in **`member-services/`** at the repository root. It is a Gradle **composite build**: each service is an independent build, included by the root.

```text
member-services/
├── settings.gradle          # includeBuild("moc-registry"), includeBuild("sme-cu-portal")
├── build.gradle             # buildAll / testAll aggregators
├── moc-registry/            # provider, port 8081
└── sme-cu-portal/           # consumer, port 8080
```

## Services

| Service | Role | Endpoint |
| --- | --- | --- |
| `moc-registry` | Mock Ministry of Commerce provider | `GET /api/businesses`, `GET /api/businesses/{id}` |
| `sme-cu-portal` | SME-CU single portal / consumer | `GET /api/portal/businesses`, `GET /api/portal/businesses/{id}` |
| `sme-cu-portal` | Provider of `applicationSubmit` (reverse flow) | `POST /api/applications`, `GET /api/applications` |

The portal delegates to the provider. Direct mode calls `http://localhost:8081/api`; X-Road mode calls the SME-CU Security Server with `X-Road-Client: DEV/BIZ/SME-CU/PORTAL`.

## Build and test

```bash
cd member-services
./gradlew buildAll
./gradlew testAll
```

Run one service from its own build:

```bash
cd member-services
./gradlew -p moc-registry bootRun
```

```bash
cd member-services
./gradlew -p sme-cu-portal bootRun
```

## Direct mode

The portal defaults to direct mode (`portal.xroad.enabled=false`).

```bash
curl -sS http://localhost:8081/api/businesses
curl -sS http://localhost:8080/api/portal/businesses
curl -sS http://localhost:8080/api/portal/businesses/B-1001
```

Mock records: `B-1001` Angkor Coffee, `B-1002` Mekong Logistics, `B-1003` Tonle Craft Studio.

## Docker packaging

Both services have a layered Dockerfile and a compose file. Build the jars first, then
the images:

```bash
cd member-services
./gradlew clean buildAll
docker compose build
docker compose up -d
```

- Images: `xroad-lab/moc-registry:0.1.0`, `xroad-lab/sme-cu-portal:0.1.0`.
- Host ports: `4711` → registry, `4710` → portal. Inside the network both listen on `8080`.
  The registry's `8081` default is a host-only choice; compose sets `SERVER_PORT=8080`.
- Both images define `HEALTHCHECK CMD curl -fsS http://localhost:8080/actuator/health`,
  so `docker compose ps` shows `(healthy)` and `depends_on: service_healthy` works.
- The Dockerfiles use `-Djarmode=tools` (Spring Boot 4 has no `layertools`) and require
  `jar` to be disabled in `build.gradle`, otherwise `build/libs/` contains two jars.
- The portal compose file joins the external `xroad-network`, so the X-Road stack in
  `infra/v7/` must be running first.

Health check directly:

```bash
curl -sS http://localhost:4711/actuator/health
curl -sS http://localhost:4710/actuator/health
```

## X-Road mode

```bash
cd member-services
PORTAL_XROAD_ENABLED=true \
PORTAL_XROAD_SECURITY_SERVER_URL=http://localhost:4210 \
PORTAL_XROAD_CLIENT_ID=DEV/BIZ/SME-CU/PORTAL \
PORTAL_XROAD_SERVICE_PATH=/r1/DEV/GOV/MOC/REGISTRY/businessLookup \
./gradlew -p sme-cu-portal bootRun
```

`4210` is the **consumer** Security Server (`ss-smecu`). `4310` is `ss-moc`, the
provider's server, and is not used by the consumer.

The portal then requests:

```text
http://localhost:4210/r1/DEV/GOV/MOC/REGISTRY/businessLookup/businesses
X-Road-Client: DEV/BIZ/SME-CU/PORTAL
```

This assumes the services run on the same Docker network as the Security Servers, where the provider is reachable as `moc-registry:8080`. If the apps run on the host instead, the SS service URL must use an address reachable from the SS container; update it in the service description.

Required X-Road state (planned three-org topology):

- `DEV/GOV/MOC/REGISTRY` registered on `SS-MOC`.
- `DEV/BIZ/SME-CU/PORTAL` registered on `SS-SMECU`.
- REST service description on MOC: URL `http://moc-registry:8080/api`, service code `businessLookup`, enabled.
- ACL subject `DEV/BIZ/SME-CU/PORTAL` on that service.

The upstream `DEV/COM/1234` demo's `mock1` is **not** compatible: it returns a test message, not the business JSON contract.

## Failure meanings

| Response | Meaning |
| --- | --- |
| `503 Provider unavailable` | The portal cannot reach the MOC app or the SS. |
| `502 X-Road error` | The SS returned `X-Road-Error`; check registration, service description, ACL. |
| `404 Business not found` | X-Road/provider worked; the id does not exist. |
| JSON parse error | The URL points at a provider with a different contract (for example the demo `mock1`). |

## Spring Boot 4 note

`spring-boot-starter-webmvc` 4.1.1 does not provide an injectable `RestClient.Builder` bean in this setup. The consumer constructs one with `RestClient.builder()`; its package-private overload accepts a builder for tests.

See [08-spring-boot-integration.md](08-spring-boot-integration.md) for the general patterns.
