# 09 — Initialize ss-moc

**Status: Verified 2026-10-25** — `ss-moc` initialized, auth certificate registered and active, TSA selected.

**Goal:** repeat the Security Server initialization for owner MOC: anchor, owner member,
token PIN, authentication key and certificate, member signing key, then register the
authentication certificate and approve it.

**Time:** 10–15 minutes if step 08 went smoothly.

## What changes from step 08

| Thing | `ss-smecu` (step 08) | `ss-moc` (this step) |
| --- | --- | --- |
| Owner member | `DEV/BIZ/SME-CU` | `DEV/GOV/MOC` |
| Member class | `BIZ` | `GOV` |
| Member code | `SME-CU` | `MOC` |
| Server code | `SS-SMECU` | `SS-MOC` |
| Auth CN / SAN | `ss-smecu` | `ss-moc` |
| Sign CN | `SME-CU` | `MOC` |
| Registration address | `ss-smecu` | `ss-moc` |
| SS UI / proxy | 4200 / 4210 | **4300 / 4310** |
| Management services | hosted here (08b) | not hosted here |

Everything else — the order, the DER lock, the two keys before Register — is identical.

## 1. Configuration anchor

Reuse the `anchor.xml` you downloaded in step 08. If you no longer have it, download it
again from the CS: **Global Configuration** → **Internal Configuration** → **Anchor** →
**Download**.

## 2. Initialization wizard

Open **`https://localhost:4300`**, log in with `xrd` / `secret`.

| Step | Value |
| --- | --- |
| **Configuration Anchor** | upload `anchor.xml` |
| **Owner Member — Member Class** | `GOV` |
| **Owner Member — Member Code** | `MOC` |
| **Owner Member — Security Server Code** | `SS-MOC` |
| **Token PIN / Confirm PIN** | `Secret1234` |

**Expected:** the dashboard loads; token `softToken-0` is logged in.

## 3. Authentication key and certificate

1. **Keys and certificates** → **Sign and Auth Keys** → token → **+ Add key**.
2. **Key Label** = `Auth key`.
3. **CSR details**: Usage `AUTHENTICATION`, Certification Service `Test CA`, format locked
   to `DER`.
4. **Generate CSR** subject fields:

| Field | Value |
| --- | --- |
| **Server DNS Name (CN)** | `ss-moc` |
| **Organization Name (O)** | `MOC` |
| **Subject Alternative Name (SAN)** | `ss-moc` |

5. **Generate CSR** → sign at `http://localhost:4400/testca/sign` with type **Auth**.
6. Back in the token → **Import cert.** → certificate shows **Saved**.

**Do not click Register yet** — the member's signing certificate does not exist.

## 4. Signing key and certificate

1. Token → **+ Add key** → **Key Label** = `Sign key`.
2. **CSR details**: Usage `SIGNING`, Client `DEV/GOV/MOC`, Certification Service `Test CA`,
   format locked to `DER`.
3. **Generate CSR** subject fields:

| Field | Value |
| --- | --- |
| **Member Code (CN)** | `MOC` |
| **Organization Name (O)** | `MOC` |
| **Subject Alternative Name (SAN)** | `ss-moc` |

4. Generate → sign at the test CA with type **Sign** → **Import cert.**

**Expected:** SIGN table shows the key, certificate **Saved**.

## 5. Register the authentication certificate

1. AUTH certificate row → **Register**.
2. **Security server DNS name or IP address** = `ss-moc`. Confirm.
3. CS UI → **Management Requests** → approve the request for `SS-MOC`.

The inbox only shows waiting requests; if it looks empty, the request may already be
approved. Check the outcome on the SS instead.

**Expected:** auth certificate becomes **Registered** within a minute.

Then **activate** it: open the certificate row → **Certificate details** → **Activate**.
A registered but inactive certificate cannot be used for TLS, and client registration
fails with `ssl_authentication_failed`.

**Expected:** *"Certificate has been activated"*, OCSP status `OCSP_RESPONSE_GOOD`.

## 6. Select the timestamping service

SS UI → **Settings** → **System Parameters** → **Timestamping Services** → **Add** →
`Test TSA` (`http://testca:8899`) → save.

**Expected:** the table lists `Test TSA` / `http://testca:8899` / `Free`. Without it, the
first X-Road message fails with `no_timestamping_provider_found`.

## 7. Verify

SS UI (`https://localhost:4300`):

- Token logged in.
- AUTH: one key, certificate `Registered`.
- SIGN: one key, certificate `Saved`.
- **Clients**: empty.

From the API:

```bash
cd /tmp && rm -f moc.cookies
curl -sk -c moc.cookies -X POST https://localhost:4300/login \
     -d 'username=xrd&password=secret' -o /dev/null
X=$(awk '$6=="XSRF-TOKEN"{print $7}' moc.cookies)

curl -sk -b moc.cookies -H "X-XSRF-TOKEN: $X" https://localhost:4300/api/v1/tokens \
  | python3 -c 'import json,sys; t=json.load(sys.stdin)[0]; print("logged_in:",t["logged_in"]); [print(k["usage"], k["label"], [c["status"] for c in k.get("certificates",[])]) for k in t["keys"]]'

curl -sk -b moc.cookies -H "X-XSRF-TOKEN: $X" https://localhost:4300/api/v1/clients
```

**Expected**

```text
logged_in: True
AUTHENTICATION Auth key ['REGISTERED']
SIGNING Sign key ['SAVED']
[]
```

The sign certificate becomes `REGISTERED` in step 10, when MOC's first client
(`REGISTRY`) is registered.

## If it fails

| Symptom | Meaning | Fix |
| --- | --- | --- |
| Member `GOV/MOC` not found | The member was not created in the CS, or the class is wrong. | Step 05: member `MOC` with class `GOV`. |
| Anchor rejected | Wrong instance or unreachable CS. | `docker exec ss-moc sh -c "curl -s -o /dev/null -w '%{http_code}\n' http://cs/"` → expect `200`. |
| PIN rejected | Token PIN mismatch. | `Secret1234`; if locked, recreate the container. |
| Register fails `tr.signer.unknown_member` | Sign key missing when you clicked Register. | Import the `Sign key` certificate (section 4), then Register again. |
| Auth cert stays `Registration in progress` | CS approval missing or rejected. | Check Management Requests including the status filter. |
| `SS-MOC` already in use | Old server with the same code. | Delete the old SS in the CS or choose another code (and update steps 10/12). |

## Why this step exists

The second Security Server is where the sequence stops being a recipe. You repeat the
same trust operations with a different identity, and the differences are entirely in
**identifiers**: member class/code, server code, CN/SAN.

Those identifiers are not cosmetic. `ss-moc` is the name `ss-smecu` will dial, the SAN in
its certificate, and the host in the provider URL that step 12 publishes. Get one wrong
and the first cross-server call fails at TLS, long after this step looked successful.

Next: [10-register-clients.md](10-register-clients.md)
