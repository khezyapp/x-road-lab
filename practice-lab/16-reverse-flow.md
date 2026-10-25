# 16 — Reverse Flow

**Status: Verified 2026-10-25** — reverse call returns 201 with an application receipt.

**Goal:** publish `applicationSubmit` on `ss-smecu`, grant `DEV/GOV/MOC/REGISTRY` access,
and submit an application through `ss-moc`. This completes the two-way lab.

**Time:** 10 minutes.

## What already exists

The portal now exposes the provider side:

```text
POST /api/applications   → 201 ApplicationReceipt
GET  /api/applications   → list of receipts (in-memory, lab only)
```

Verify it directly before involving X-Road:

```bash
curl -i -sS -X POST http://localhost:4710/api/applications \
  -H 'Content-Type: application/json' \
  -d '{"businessId":"B-1001","applicantName":"Sok Dara","notes":"lab test"}'
```

**Expected:** `201` and

```json
{"id":"APP-...","businessId":"B-1001","applicantName":"Sok Dara",
 "notes":"lab test","status":"RECEIVED","receivedAt":"..."}
```

## 1. Prove the SS can reach the portal

Calls are made by `ss-smecu`, not by your browser:

```bash
docker exec ss-smecu sh -c "curl -s -o /dev/null -w '%{http_code}\n' http://sme-cu-portal:8080/api/applications"
```

**Expected:** `200`.

## 2. Publish `applicationSubmit` on ss-smecu

`https://localhost:4200` → **Clients** → `DEV/BIZ/SME-CU/PORTAL` → **Services** →
**Add REST**.

| Field | Value |
| --- | --- |
| **Service Type** | `REST API base path` |
| **URL** | `http://sme-cu-portal:8080/api` |
| **Service Code** | `applicationSubmit` |

Then **enable** the description.

**Expected:** the service ID is `DEV/BIZ/SME-CU/PORTAL/applicationSubmit`.
A call to `/r1/DEV/BIZ/SME-CU/PORTAL/applicationSubmit/applications` is forwarded to
`http://sme-cu-portal:8080/api/applications`.

## 3. Grant MOC access

Still on `ss-smecu`: open the service `applicationSubmit` → **Parameters** →
**Access Rights** → **Add subjects**.

| Field | Value |
| --- | --- |
| Name | *(empty)* |
| Instance | `DEV` |
| Member Class | `GOV` |
| Member Code | `MOC` |
| Subsystem Code | `REGISTRY` |
| Subject type | `Subsystem` |

**Search** → tick `DEV/GOV/MOC/REGISTRY` → **Save**.

## 4. Make REGISTRY usable over HTTP

`https://localhost:4300` → **Clients** → `DEV/GOV/MOC/REGISTRY` → **Internal servers** →
**Connection type** = `HTTP`.

Without this, the call fails with `clientproxy.ssl_authentication_failed` (same as step
14, but on the MOC side now).

## 5. Submit through X-Road from the MOC side

```bash
curl -i -sS -X POST \
  "http://localhost:4310/r1/DEV/BIZ/SME-CU/PORTAL/applicationSubmit/applications" \
  -H "X-Road-Client: DEV/GOV/MOC/REGISTRY" \
  -H 'Content-Type: application/json' \
  -d '{"businessId":"B-1001","applicantName":"Sok Dara","notes":"via MOC"}'
```

**Expected**

```http
HTTP/1.1 201 Created
X-Road-Client: DEV/GOV/MOC/REGISTRY
X-Road-Service: DEV/BIZ/SME-CU/PORTAL/applicationSubmit
X-Road-Id: <uuid>
X-Road-Request-Id: <uuid>
X-Road-Request-Hash: <base64>
```

Body: an `ApplicationReceipt` with `"status":"RECEIVED"`.

Confirm the portal actually stored it:

```bash
curl -sS http://localhost:4710/api/applications
```

**Expected:** the receipt with `businessId: B-1001`.

## If it fails

| Symptom | Meaning | Fix |
| --- | --- | --- |
| 500 / `access_denied` | ACL missing for `DEV/GOV/MOC/REGISTRY`. | Section 3. |
| 404 / `unknown_service` | Service code typo, or description disabled. | Section 2; enable it. |
| `clientproxy.ssl_authentication_failed` | REGISTRY connection type is HTTPS. | Section 4. |
| 400 with a Spring error body | The provider rejected the payload. | Send both `businessId` and `applicantName`. |
| 500 `NetworkError` | `ss-smecu` cannot reach `sme-cu-portal`. | Section 1 check. |
| 502 `X-Road error` | The SS rejected the message; read the body `detail`. | Use the UUID in the SS logs. |
| Receipt not in `GET /api/applications` | The SS called a different app or port. | Check the description URL: `http://sme-cu-portal:8080/api`. |

## Why this step exists

The first direction proved SME-CU can consume. This one proves MOC can consume **SME-CU**
— which means SME-CU is now a provider too: it needed a registered subsystem, a published
service, an ACL, and a reachable app.

A one-way lab hides exactly these mistakes: a provider that was never registered, an ACL
pointing at the wrong subject, or an app bound to the wrong port. The reverse call
exercises all of them in one request.

The MOC consumer is `curl` here. A `moc-registry` client endpoint would be the symmetric
version of step 15; the lab does not require it.

Next: [17-negative-tests.md](17-negative-tests.md)
