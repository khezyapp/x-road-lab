# 11 — Deploy the Applications

**Status: Verified 2026-10-25** (built, imaged, started, healthy, direct mode returns data).

**Goal:** build both jars, build the layered images, start them on `xroad-network`, and
confirm health plus direct mode before any X-Road traffic.

**Time:** 2–3 minutes after the first image build.

## Preconditions

- The X-Road stack is running (`cs`, `testca`, `ss-smecu`, `ss-moc`).
- Step 10 done: `PORTAL` and `REGISTRY` are **Registered**.

## 1. Build the jars

```bash
cd /home/khezy/Documents/learning/x-road-lab/member-services
./gradlew cleanAll buildAll
```

`cleanAll` matters: it removes the old `-plain.jar` files. The Dockerfiles assume exactly
one jar in each `build/libs/`.

**Expected**

```text
BUILD SUCCESSFUL
```

```bash
ls -1 moc-registry/build/libs sme-cu-portal/build/libs
```

```text
moc-registry/build/libs:
moc-registry-0.1.0.jar
sme-cu-portal/build/libs:
sme-cu-portal-0.1.0.jar
```

## 2. Build the images

```bash
docker compose build
```

**Expected:** `Image xroad-lab/moc-registry:0.1.0 Built`, same for `sme-cu-portal`.

The Dockerfiles use Spring Boot's layered extraction (`-Djarmode=tools`). The dependency
layer is ~21 MB and changes rarely; the application layer is ~16 kB and changes on every
code change. Confirm:

```bash
docker history --format '{{.CreatedBy}} | {{.Size}}' xroad-lab/moc-registry:0.1.0 | head -6
```

## 3. Start them

```bash
docker compose up -d
```

**Expected**

```text
Container moc-registry Started
Container moc-registry Healthy
Container sme-cu-portal Started
```

`moc-registry` is started first; the portal waits for it through
`depends_on: condition: service_healthy`.

## 4. Verify

```bash
docker compose ps --format '{{.Name}}\t{{.Status}}\t{{.Ports}}'
```

**Expected**

```text
moc-registry   Up ... (healthy)   0.0.0.0:4711->8080/tcp
sme-cu-portal  Up ... (healthy)   0.0.0.0:4710->8080/tcp
```

Health:

```bash
curl -sS http://localhost:4711/actuator/health
curl -sS http://localhost:4710/actuator/health
```

**Expected**

```json
{"groups":["liveness","readiness"],"status":"UP"}
{"groups":["liveness","readiness"],"status":"UP"}
```

Direct mode (no X-Road):

```bash
curl -sS http://localhost:4711/api/businesses | head -c 120
curl -sS http://localhost:4710/api/portal/businesses | head -c 120
```

**Expected:** both return the business JSON, starting with
`[{"id":"B-1001","registrationNumber":"KH-2024-001001",...`.

The portal is in direct mode because compose defaults `PORTAL_XROAD_ENABLED` to `false`
and points `PORTAL_PROVIDER_BASE_URL` at `http://moc-registry:8080/api`. Step 15 flips it.

## If it fails

| Symptom | Meaning | Fix |
| --- | --- | --- |
| `no such network: xroad-network` | The X-Road stack is not running. | Start `cs` first (step 03). |
| `COPY failed: ... multiple files` during build | `build/libs/` contains both the boot jar and a stale `-plain.jar`. | `./gradlew cleanAll buildAll`, then rebuild. |
| `moc-registry` is `(unhealthy)` | The process is up but `/actuator/health` fails, or it listens elsewhere. | `docker logs moc-registry`; compose sets `SERVER_PORT=8080` (the app's default is 8081 on the host). |
| Portal health is `UP` but `/api/portal/businesses` returns 503 | Direct mode cannot reach the provider. | Check `PORTAL_PROVIDER_BASE_URL`; test `docker exec sme-cu-portal curl -s http://moc-registry:8080/api/businesses`. |
| `port is already allocated` | Something owns 4710/4711. | `docker ps`; stop the conflicting container. |
| Build is extremely slow | First build pulls the Liberica JRE image and dependencies. | Wait; later builds use cache. |

## Why this step exists

The application and the X-Road layer fail independently. Deploying the apps **without**
X-Road first proves:

- the jar runs on Java 25;
- the provider returns its contract;
- the consumer can parse it;
- health checks and ports are right.

Only then does step 14 add the X-Road hop. When something breaks after that, you already
know it is not the application itself.

Next: [12-publish-service.md](12-publish-service.md)
