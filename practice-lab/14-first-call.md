# 14 — First X-Road Call

**Status: Verified 2026-10-25** — call returns 200 with the X-Road evidence headers.

**Goal:** call `businessLookup` through the `ss-smecu` proxy with `curl`, and read the
X-Road response headers. This is the milestone: one consumer, one provider, one ACL, one
successful call.

**Time:** 1 minute.

## Preconditions

- Step 12 done: `businessLookup` enabled on `ss-moc`.
- Step 13 done: `DEV/BIZ/SME-CU/PORTAL` has access rights.
- Step 10: `PORTAL` connection type is **HTTP** (Internal servers tab). If it is HTTPS,
  see the first failure row.

## The call

```bash
curl -i -sS \
  "http://localhost:4210/r1/DEV/GOV/MOC/REGISTRY/businessLookup/businesses" \
  -H "X-Road-Client: DEV/BIZ/SME-CU/PORTAL"
```

Read the URL right to left:

```text
http://localhost:4210                   consumer Security Server (ss-smecu proxy)
/r1                                     REST message protocol version
/DEV/GOV/MOC/REGISTRY                   provider subsystem
/businessLookup                         provider service code
/businesses                             path forwarded to the provider app
```

`X-Road-Client` is the **consumer subsystem**. It is not the provider and not a member.

## Expected

```http
HTTP/1.1 200 OK
X-Road-Client: DEV/BIZ/SME-CU/PORTAL
X-Road-Service: DEV/GOV/MOC/REGISTRY/businessLookup
X-Road-Id: <uuid>
X-Road-Request-Id: <uuid>
X-Road-Request-Hash: <base64>
Content-Type: application/json
```

Body: the business records, starting with `[{"id":"B-1001",...`.

There must be **no** `X-Road-Error` header.

Single record:

```bash
curl -sS \
  "http://localhost:4210/r1/DEV/GOV/MOC/REGISTRY/businessLookup/businesses/B-1001" \
  -H "X-Road-Client: DEV/BIZ/SME-CU/PORTAL"
```

## If it fails

| Symptom | Meaning | Fix |
| --- | --- | --- |
| `clientproxy.ssl_authentication_failed` / *specifies HTTPS but did not supply TLS certificate* | The consumer client's connection type is `HTTPS`, so the proxy demands a client certificate. | Clients → `PORTAL` → **Internal servers** → **Connection type** = `HTTP`, then retry. |
| 500 / `access_denied` | ACL missing, or it names a different subject. | Step 13: add `DEV/BIZ/SME-CU/PORTAL` to `businessLookup`. |
| 404 / `unknown_service` | Service code typo, or the description is disabled. | Compare with step 12; confirm it is enabled. |
| 500 `NetworkError` | `ss-moc` cannot reach `moc-registry`. | `docker exec ss-moc curl -s http://moc-registry:8080/api/businesses`. |
| 500 `OutdatedGlobalConf` | The consumer SS has stale configuration. | Wait; check SS **Diagnostics**. |
| 500 `no_timestamping_provider_found` | The SS has no TSA selected. | Settings → System Parameters → Timestamping Services. |
| TLS/certificate error between SSes | Auth certificate, SAN, or OCSP problem. | Check both SSes' **Diagnostics** and `signer.log`. |

## What this proves

One request exercises every object built so far:

- `ss-smecu` accepted the client and the service ID;
- global configuration told it that `ss-moc` hosts the service;
- the two SSes authenticated each other with mTLS;
- `ss-moc` found the description **enabled** and the ACL **allowed** `PORTAL`;
- `ss-moc` reached `http://moc-registry:8080/api/businesses`;
- the message was signed and timestamped, and the evidence headers were added by the SS.

Keep the `X-Road-Request-Id` and the body `detail` UUID; they are how you find this call in
the message logs later.

Next: [15-portal-in-xroad-mode.md](15-portal-in-xroad-mode.md)
