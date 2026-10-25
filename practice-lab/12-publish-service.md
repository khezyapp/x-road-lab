# 12 — Publish the Service

**Status: Verified 2026-10-25** — `businessLookup` description enabled and reachable from `ss-moc`.

**Goal:** on `ss-moc`, add a REST base-path description for `moc-registry`, confirm the
Security Server can reach the provider, and enable the description.

**Time:** 5 minutes.

## Preconditions

- Step 11 done: `moc-registry` is running and healthy on port 4711 (container port 8080).
- `DEV/GOV/MOC/REGISTRY` is a **Registered** client of `ss-moc`.

## 1. Prove the SS can reach the provider

The call is made by `ss-moc`, not by your browser or the portal:

```bash
docker exec ss-moc sh -c "curl -s -o /dev/null -w '%{http_code}\n' http://moc-registry:8080/api/businesses"
```

**Expected:** `200`.

If this fails, nothing later can work. Use the **container name and internal port** —
`localhost:8081` and `localhost:4711` are host-only addresses.

## 2. Add the REST description

`https://localhost:4300` → **Clients** → `DEV/GOV/MOC/REGISTRY` → **Services** →
**Add REST**.

| Field | Value |
| --- | --- |
| **Service Type** | `REST API base path` |
| **URL** | `http://moc-registry:8080/api` |
| **Service Code** | `businessLookup` |

There is **no version field**. The resulting service ID is
`DEV/GOV/MOC/REGISTRY/businessLookup`, and a call to
`/r1/DEV/GOV/MOC/REGISTRY/businessLookup/businesses` is forwarded to
`http://moc-registry:8080/api/businesses`.

## 3. Enable the description

New descriptions are **disabled** by default. On the service description row, enable it.

**Expected:** the description shows as enabled and lists one service, `businessLookup`.

```bash
cd /tmp && rm -f moc.cookies
curl -sk -c moc.cookies -X POST https://localhost:4300/login \
     -d 'username=xrd&password=secret' -o /dev/null
X=$(awk '$6=="XSRF-TOKEN"{print $7}' moc.cookies)
curl -sk -b moc.cookies -H "X-XSRF-TOKEN: $X" \
  'https://localhost:4300/api/v1/clients/DEV%3AGOV%3AMOC%3AREGISTRY/service-descriptions'
```

**Expected**

```json
[{"url":"http://moc-registry:8080/api","type":"REST","disabled":false,
  "services":[{"id":"DEV:GOV:MOC:REGISTRY:businessLookup","service_code":"businessLookup",
  "url":"http://moc-registry:8080/api"}]}]
```

The important fields: `"disabled": false`, and a service whose full ID ends in
`:businessLookup`.

## 4. What is not done yet

The service is published but **nobody may call it**. Access is denied by default; step 13
adds `DEV/BIZ/SME-CU/PORTAL` to the ACL. A call now returns 500 with `access_denied` from the provider SS.

## If it fails

| Symptom | Meaning | Fix |
| --- | --- | --- |
| `Add REST` rejects the URL | Not an HTTP(S) URL. | Use `http://moc-registry:8080/api`. |
| The description stays disabled | You added it but did not enable it. | Enable it on the description row. |
| Later call returns `unknown service` | Service code mismatch, or description disabled. | Compare the call path with `businessLookup` exactly. |
| SS logs show `NetworkError` reaching the provider | The SS cannot resolve or reach the app. | Run the `docker exec ss-moc curl` check above. |
| URL accidentally includes `/businesses` | The base path must not include the operation path. | Use `http://moc-registry:8080/api`; the remainder comes from the call. |
| `Service code already exists` | The description was added twice. | Keep one; delete the duplicate. |

## Why this step exists

This is the boundary between "an HTTP app" and "an X-Road service":

- the **URL** tells the SS where to forward calls;
- the **service code** is what callers put in the X-Road URL;
- **enable** publishes the description into global configuration and makes it routable.

The provider app never learns that X-Road exists. It keeps serving
`/api/businesses`; the SS adds the identity, signing, and evidence headers.

Next: [13-grant-acl.md](13-grant-acl.md)
