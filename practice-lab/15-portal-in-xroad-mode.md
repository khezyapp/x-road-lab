# 15 — Portal in X-Road Mode

**Status: Verified 2026-10-25** (portal returns data with X-Road enabled, and with the
direct provider URL pointed at a dead address).

**Goal:** switch `sme-cu-portal` from direct HTTP to X-Road, and prove the JSON it serves
now arrives through the Security Servers.

**Time:** 2 minutes.

## Preconditions

- Step 14 green: the `curl` call to `businessLookup` returns 200.
- `PORTAL` connection type is `HTTP` (step 10, section 2b).

## 1. Enable X-Road mode

```bash
cd /home/khezy/Documents/learning/x-road-lab/member-services
PORTAL_XROAD_ENABLED=true docker compose up -d --force-recreate sme-cu-portal
```

The variable is interpolated into the container environment. Nothing else changes: the
portal already points at `http://ss-smecu:8080` (container port, not the host's 4210).

```bash
docker exec sme-cu-portal env | grep -E 'PORTAL_XROAD|PORTAL_PROVIDER'
```

**Expected**

```text
PORTAL_XROAD_ENABLED=true
PORTAL_XROAD_SECURITY_SERVER_URL=http://ss-smecu:8080
PORTAL_XROAD_CLIENT_ID=DEV/BIZ/SME-CU/PORTAL
PORTAL_XROAD_SERVICE_PATH=/r1/DEV/GOV/MOC/REGISTRY/businessLookup
PORTAL_PROVIDER_BASE_URL=http://moc-registry:8080/api
```

`PORTAL_PROVIDER_BASE_URL` is still set but **ignored** while X-Road mode is on.

## 2. Call the portal

```bash
curl -sS http://localhost:4710/api/portal/businesses | head -c 200
curl -sS http://localhost:4710/api/portal/businesses/B-1002
```

**Expected:** the same business JSON as before, e.g.
`[{"id":"B-1001","registrationNumber":"KH-2024-001001",...`.

The portal appends `/businesses` to `PORTAL_XROAD_SERVICE_PATH` and sends
`X-Road-Client: DEV/BIZ/SME-CU/PORTAL`.

## 3. Prove it is not direct mode

Point the direct provider URL at a dead address, keep X-Road enabled, and recreate:

```bash
PORTAL_XROAD_ENABLED=true PORTAL_PROVIDER_BASE_URL=http://127.0.0.1:9/api \
  docker compose up -d --force-recreate sme-cu-portal
sleep 15
curl -sS http://localhost:4710/api/portal/businesses | head -c 120
```

**Expected:** the data still arrives. If direct mode were used, the portal would return
`503 Provider unavailable`.

Restore the normal configuration:

```bash
PORTAL_XROAD_ENABLED=true docker compose up -d --force-recreate sme-cu-portal
```

## If it fails

| Symptom | Meaning | Fix |
| --- | --- | --- |
| `503 Provider unavailable` | The portal cannot reach the SS, or the SS refused the request. | `docker exec sme-cu-portal curl -s -o /dev/null -w '%{http_code}\n' http://ss-smecu:8080/` |
| `502 X-Road error` | The SS returned an `X-Road-Error`. | The response body carries the code and a `detail` UUID; use it in the SS logs. |
| `access_denied` through the portal but 200 with curl | Different client or connection type. | Check `PORTAL_XROAD_CLIENT_ID` and the client's connection type. |
| `Connection refused` to `localhost:4210` | The container cannot use host ports. | Use `http://ss-smecu:8080` inside the network. |
| Still works after `PORTAL_XROAD_ENABLED=false` | The container was not recreated, so the old env is still active. | `--force-recreate`. |
| Data arrives but the direct URL is alive | You did not actually switch modes. | `docker exec sme-cu-portal env | grep XROAD`; recreate. |

## Why this step exists

Until now the app called the provider directly. This is the moment the **application**
becomes an X-Road consumer: it sends `X-Road-Client`, the SS adds identity and evidence,
and the app never learns that a Security Server is in the path.

Both modes remain useful:

- **direct** for local development and tests (fast, no X-Road dependency);
- **X-Road** for the real integration contract.

Next: [16-reverse-flow.md](16-reverse-flow.md)
