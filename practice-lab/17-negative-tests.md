# 17 — Negative Tests

**Status: Verified 2026-10-25** for the unknown client, unknown service, ACL and
provider-down cases. The disabled-description case is documented with the observed
behaviour, which differs from the manual on this 7.8.1 image.

**Goal:** break five things deliberately, record the exact responses, and restore each
one. The point is to learn the signature of each failure, not to keep the lab broken.

**Time:** 15 minutes.

## Read this first

X-Road reports most failures as **HTTP 500 with an `X-Road-Error` header**. The header is
the diagnosis; the status code alone is not. Errors *without* `X-Road-Error` come from the
provider application.

Keep the `detail` UUID from the body — it matches the SS message log.

## Baseline

```bash
curl -sS -o /dev/null -w '%{http_code}\n' \
  "http://localhost:4210/r1/DEV/GOV/MOC/REGISTRY/businessLookup/businesses" \
  -H "X-Road-Client: DEV/BIZ/SME-CU/PORTAL"
```

**Expected:** `200`. If not, fix that before testing failures.

## A. Unknown client

```bash
curl -i -sS \
  "http://localhost:4210/r1/DEV/GOV/MOC/REGISTRY/businessLookup/businesses" \
  -H "X-Road-Client: DEV/BIZ/SME-CU/NOPE"
```

**Expected**

```http
HTTP/1.1 500 Server Error
X-Road-Error: server.clientproxy.unknown_member
```

```json
{"type":"server.clientproxy.unknown_member",
 "message":"Client 'SUBSYSTEM:DEV/BIZ/SME-CU/NOPE' not found","detail":"<uuid>"}
```

**Meaning:** the **consumer** SS does not know this subsystem. Nothing was forwarded.

## B. Wrong service code

```bash
curl -i -sS \
  "http://localhost:4210/r1/DEV/GOV/MOC/REGISTRY/businessLookupX/businesses" \
  -H "X-Road-Client: DEV/BIZ/SME-CU/PORTAL"
```

**Expected**

```http
HTTP/1.1 500 Server Error
X-Road-Error: server.serverproxy.unknown_service
```

```json
{"type":"server.serverproxy.unknown_service",
 "message":"Unknown service: SERVICE:DEV/GOV/MOC/REGISTRY/businessLookupX","detail":"<uuid>"}
```

**Meaning:** this time the error comes from the **provider** SS. The request was routed,
but no such service exists.

## C. Remove the ACL

On `https://localhost:4300`: Clients → `REGISTRY` → Services → `businessLookup` →
Parameters → **Access Rights** → remove `DEV/BIZ/SME-CU/PORTAL`.

Wait a few seconds, then:

```bash
curl -i -sS \
  "http://localhost:4210/r1/DEV/GOV/MOC/REGISTRY/businessLookup/businesses" \
  -H "X-Road-Client: DEV/BIZ/SME-CU/PORTAL"
```

**Expected**

```http
HTTP/1.1 500 Server Error
X-Road-Error: server.serverproxy.access_denied
```

```json
{"type":"server.serverproxy.access_denied",
 "message":"Request is not allowed: SERVICE:DEV/GOV/MOC/REGISTRY/businessLookup","detail":"<uuid>"}
```

**Meaning:** service exists, provider is reachable, but the consumer is not allowed. This
is the **ACL** failure.

**Restore:** same dialog → **Add subjects** → `DEV/BIZ/SME-CU/PORTAL` → Save → retry until
`200`.

## D. Provider down

```bash
cd /home/khezy/Documents/learning/x-road-lab
docker compose -f member-services/docker-compose.yml stop moc-registry
sleep 3

curl -i -sS \
  "http://localhost:4210/r1/DEV/GOV/MOC/REGISTRY/businessLookup/businesses" \
  -H "X-Road-Client: DEV/BIZ/SME-CU/PORTAL"

docker compose -f member-services/docker-compose.yml start moc-registry
```

**Expected**

```http
HTTP/1.1 500 Server Error
X-Road-Error: server.serverproxy.unknown_host
```

```json
{"type":"server.serverproxy.unknown_host","message":"moc-registry: Name or service not known","detail":"<uuid>"}
```

**Meaning:** the container is gone, so Docker DNS no longer resolves the name. If the app
were running but returning errors, you would get a **500 without `X-Road-Error`** — that
is an application failure, not an X-Road failure.

**Restore:** the `start` above; wait until `(healthy)`.

## E. Consumer connection type

On `https://localhost:4200`: Clients → `PORTAL` → Internal servers → Connection type =
`HTTPS`, then call the baseline again.

**Expected**

```http
HTTP/1.1 500 Server Error
X-Road-Error: server.clientproxy.ssl_authentication_failed
```

```json
{"type":"server.clientproxy.ssl_authentication_failed",
 "message":"Client (SUBSYSTEM:DEV/BIZ/SME-CU/PORTAL) specifies HTTPS but did not supply TLS certificate", ...}
```

**Restore:** set the connection type back to `HTTP`.

## F. Disabled description — observed quirk

The manual says a disabled service description makes its services inaccessible. On this
7.8.1 image, disabling `businessLookup` through the API (`PUT
/service-descriptions/{id}/disable`) returned 200 and the API showed `disabled: true`, but
calls kept returning **200**, even after restarting `ss-moc`.

Record what you see. If your run blocks the call, the expected response is a 500 whose
message contains the **disable notice** you typed. If it does not, treat it as an
environment quirk and do not rely on "disable" as an access-control mechanism in this
lab.

**Restore:** re-enable the description.

## Summary

| Test | `X-Road-Error` | Which SS |
| --- | --- | --- |
| Unknown client | `server.clientproxy.unknown_member` | consumer |
| Wrong service | `server.serverproxy.unknown_service` | provider |
| ACL removed | `server.serverproxy.access_denied` | provider |
| Provider down | `server.serverproxy.unknown_host` | provider |
| Client HTTPS without cert | `server.clientproxy.ssl_authentication_failed` | consumer |

Notice the prefix: `clientproxy` means the problem is on the caller's side; `serverproxy`
means the provider side. That half of the diagnosis is free.

## Why this step exists

A working lab teaches one path. These five failures teach you to read the other paths, and
they are the ones you will actually meet in an integration: a typo in the client ID, a
typo in the service code, a missing ACL, a stopped provider, and a connection type that
does not match how the information system connects.

Next: [18-logs-and-evidence.md](18-logs-and-evidence.md)
