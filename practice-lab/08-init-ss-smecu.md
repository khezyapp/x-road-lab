# 08 — Initialize ss-smecu

**Status: Verified 2026-10-25** — auth and signing certificates imported, auth certificate registered and active, TSA selected.

**Goal:** trust the CS (configuration anchor), initialize the server as owner SME-CU,
log in the software token, add the authentication key **and** the member signing key
(both signed at the test CA), then register the authentication certificate and get it
approved.

**Time:** 15–25 minutes.

> The Management Services provisioning (provider registration, `MANAGEMENT` client,
> management WSDL, ACL) is the next page: [08b-management-services.md](08b-management-services.md).
> Do it before step 10, not before this one.

## 1. Download the configuration anchor from the CS

The Security Server trusts nothing until it has the CS's anchor.

In the CS UI (`https://localhost:4100`): **Global Configuration** → tab **Internal
Configuration** → section **Anchor** → **Download**.

Save the file as `anchor.xml` somewhere you can find it (for example `~/Downloads`).

**Expected:** a small XML file whose root is `configurationAnchor` and whose
`instanceIdentifier` is `DEV`.

```bash
head -c 300 ~/Downloads/anchor.xml
```

## 2. Run the initialization wizard

Open **`https://localhost:4200`**, log in with `xrd` / `secret`. An uninitialized server
shows **Initial configuration** with three steps.

### Step 1 — Configuration Anchor

Click **Upload**, choose `anchor.xml`. The page shows the anchor **hash** and **created**
time.

**Expected:** hash and date appear. If the file is rejected, stop here — the rest cannot
work.

### Step 2 — Owner Member

| Field | Value |
| --- | --- |
| **Member Class** | `BIZ` |
| **Member Code** | `SME-CU` |
| **Security Server Code** | `SS-SMECU` |

The member name fills in from the CS registry. A warning that the member is unregistered
is expected at this point; accept it.

**Expected:** the owner is `DEV/BIZ/SME-CU` and the server code is `SS-SMECU`.

### Step 3 — Token PIN

| Field | Value |
| --- | --- |
| **PIN** | `Secret1234` |
| **Confirm PIN** | `Secret1234` |

Finish the wizard.

**Expected:** the Security Server dashboard loads, and the software token is logged in
(no "Log in" button on the token).

## 3. Add the authentication key

The server cannot talk to other servers until an authentication certificate is
registered.

1. **Keys and certificates** → **Sign and Auth Keys**.
2. Expand **softToken-0** → **+ Add key**.
3. Step **Key details**: **Key Label** = `Auth key`.
4. Step **CSR details**:
   - **Usage** = `AUTHENTICATION`
   - **Certification Service** = `Test CA`
   - **CSR Format** is locked to `DER`. That is expected — see below.
5. Step **Generate CSR** shows the certificate subject fields. Fill them exactly:

| Field | Value | Why |
| --- | --- | --- |
| **Server DNS Name (CN)** | `ss-smecu` | Must be the name other Security Servers use to reach this one. |
| **Organization Name (O)** | `SME-CU` | Display/organizational only; the test CA does not validate it. |
| **Subject Alternative Name (SAN)** | `ss-smecu` | Must match the CN, or SS-to-SS TLS verification fails later. |

These three are what the test CA's profile asks for. If your form also shows **Country
Code (C)** or **Serial Number**, use `KH` and `DEV/SS-SMECU/BIZ` respectively.

6. Click **Generate CSR**. A `.der` CSR file is downloaded.

**Why the format selector is locked:** the Security Server disables it whenever the
selected CA declares a default CSR format. In step 06 you set `Default CSR format = DER`
on the Test CA, so the wizard always produces DER and the field cannot be changed here.

That is fine. The test CA detects PEM vs DER from the upload and signs either. (The demo
automation requests PEM explicitly through the API, which makes the field look editable
in the reference material.)

**Why CN and SAN must be `ss-smecu`:** the Security Server authenticates itself to other
servers with this certificate over mutual TLS. The peer checks that the hostname it
dialled (`ss-smecu`, as registered in the CS) appears in the certificate. A wrong CN/SAN
produces a TLS error on the *other* server, not on this one, which is why it is painful
to diagnose later.

## 4. Sign and import the authentication certificate

Open **`http://localhost:4400/testca/sign`**.

1. **CSR** → choose the downloaded CSR file.
2. **Type** → select **Auth**.
3. Click **Sign**. The browser downloads the signed certificate.

The file is a PEM certificate. You do not need to inspect it, but this checks it is not
an error page:

```bash
openssl x509 -in ~/Downloads/cert.pem -noout -subject -issuer
```

Replace `cert.pem` with the file the browser actually saved.

**Expected:** `subject=...CN=ss-smecu...` and `issuer=...CN=Test CA, O=Test`.

Then import it:

1. **Keys and certificates** → token **softToken-0** → **Import cert.**
2. Choose the signed certificate.

**Expected:** the AUTH table lists the key and the certificate with status **Saved**.

**Do not click Register yet.** The registration request is a message signed by the
member, so the Security Server needs the member's **signing** certificate first. Without
it, Register fails with `tr.signer.unknown_member` / *"Could not find any certificates
for member 'MEMBER:DEV/BIZ/SME-CU'"*.

## 5. Add the signing key

This is the key the member uses to sign outgoing messages — including the registration
request in the next section. Its certificate is **not** registered on its own; it becomes
registered when the member's first client is registered in 08b.

1. Same token → **+ Add key**.
2. **Key Label** = `Sign key`.
3. **CSR details**:
   - **Usage** = `SIGNING`
   - **Client** = `DEV/BIZ/SME-CU`
   - **Certification Service** = `Test CA`
   - **CSR Format** stays `DER` and is locked, same as in section 3.
4. **Generate CSR** → subject fields. For a signing certificate the CN means *member*,
   not server:

| Field | Value |
| --- | --- |
| **Member Code (CN)** | `SME-CU` |
| **Organization Name (O)** | `SME-CU` |
| **Subject Alternative Name (SAN)** | `ss-smecu` |

Fill the fields your form actually shows; if the CN label is still *Server DNS Name*,
use `ss-smecu` there.

5. Click **Generate CSR** → download.
6. Sign it at `http://localhost:4400/testca/sign` with type **Sign**.
7. Token → **Import cert.** → upload the signed certificate.

**Expected:** the SIGN table lists the key and the certificate shows **Saved**.

## 6. Register the authentication certificate

Now that a signing certificate for `DEV/BIZ/SME-CU` exists in the token:

1. **Keys and certificates** → AUTH table → **Register** on the auth certificate.
2. **Registration request** dialog → **Security server DNS name or IP address** =
   `ss-smecu`. Confirm.

**Expected:** the certificate status becomes **Registration in progress**.

If you click Register before section 5, you get `tr.signer.unknown_member`. Importing the
signing certificate afterwards is enough; you do not need to recreate the auth key.

## 7. Approve the registration in the CS

CS UI (`https://localhost:4100`) → **Management Requests**.

Find the authentication certificate registration request for `SS-SMECU` and **Approve**.

**Expected:** the request disappears from the pending list, and the SS certificate status
becomes **Registered** within a minute.

### Activate the certificate

Registration and activation are **separate states**. Registration tells the CS the
certificate exists; activation tells the local signer to use it for TLS. If the
certificate shows `DISABLED` / inactive and offers an **Activate** action, it cannot be
used yet — and every outbound X-Road call fails with `ssl_authentication_failed`.

1. **Keys and certificates** → AUTH table → **click the certificate row**.
2. In **Certificate details**, click **Activate**.
3. **Expected:** *"Certificate has been activated"* and the OCSP status becomes
   `OCSP_RESPONSE_GOOD`.

```bash
curl -sk -b ss.cookies -H "X-XSRF-TOKEN: $X" https://localhost:4200/api/v1/tokens \
  | python3 -c 'import json,sys; t=json.load(sys.stdin)[0]; [print(k["usage"], [(c["status"], c["active"], c["ocsp_status"]) for c in k.get("certificates",[])]) for k in t["keys"]]'
```

**Expected:** `('AUTHENTICATION', [('REGISTERED', True, 'OCSP_RESPONSE_GOOD')])`.

Verify from the API on the SS:

```bash
cd /tmp && rm -f ss.cookies
curl -sk -c ss.cookies -X POST https://localhost:4200/login \
     -d 'username=xrd&password=secret' -o /dev/null
X=$(awk '$6=="XSRF-TOKEN"{print $7}' ss.cookies)
curl -sk -b ss.cookies -H "X-XSRF-TOKEN: $X" https://localhost:4200/api/v1/tokens \
  | python3 -c 'import json,sys; t=json.load(sys.stdin)[0]; print("logged_in:", t["logged_in"]); [print(k["usage"], k["label"], [c["status"] for c in k.get("certificates",[])]) for k in t["keys"]]'
```

**Expected:** `logged_in: True`, one `AUTHENTICATION` key with a `REGISTERED` certificate,
and one `SIGNING` key with a `SAVED` certificate.

## 8. Select the timestamping service

Global configuration *offers* the TSA (the CS's list), but each Security Server must
**select** which one it uses to timestamp messages. Nothing is selected by default.

SS UI → **Settings** → **System Parameters** → section **Timestamping Services** →
**Add** → select `Test TSA` (`http://testca:8899`) → save.

**Expected:** the table lists `Test TSA` / `http://testca:8899` / `Free`.

```bash
curl -sk -b ss.cookies -H "X-XSRF-TOKEN: $X" https://localhost:4200/api/v1/system/timestamping-services
```

**Expected**

```json
[{"name":"Test TSA","url":"http://testca:8899","cost_type":"FREE"}]
```

Without this, the server cannot send **any** X-Road message: the proxy refuses with
`no_timestamping_provider_found`, because every message log record must be timestamped.
This is per-server local configuration, not something the CS can do for you.

## 9. Verify

In the SS UI:

- Token `softToken-0`: logged in.
- AUTH: one key, certificate `Registered`.
- SIGN: one key, certificate `Saved`.
- **Clients**: empty.
- **Diagnostics** → global configuration: no errors.

From the API:

```bash
cd /tmp && rm -f ss.cookies
curl -sk -c ss.cookies -X POST https://localhost:4200/login \
     -d 'username=xrd&password=secret' -o /dev/null
X=$(awk '$6=="XSRF-TOKEN"{print $7}' ss.cookies)

# auth/sign key state
curl -sk -b ss.cookies -H "X-XSRF-TOKEN: $X" https://localhost:4200/api/v1/tokens \
  | python3 -c 'import json,sys; t=json.load(sys.stdin)[0]; print([(k["usage"], k["label"]) for k in t["keys"]])'

# clients are still empty on this server
curl -sk -b ss.cookies -H "X-XSRF-TOKEN: $X" https://localhost:4200/api/v1/clients
```

**Expected**

```text
[('AUTHENTICATION', 'Auth key'), ('SIGNING', 'Sign key')]
[]
```

The CS side should show no pending management requests, and the SS should appear under
**Security Servers** with its authentication certificate approved.

## If it fails

| Symptom | Meaning | Fix |
| --- | --- | --- |
| Anchor rejected as *invalid* or *unreachable* | The anchor is for another instance, or the SS cannot reach the CS. | Download it again from the CS; check `docker exec ss-smecu curl -s -o /dev/null -w '%{http_code}\n' http://cs/`. |
| *Member is unregistered* warning | Expected: SME-CU has no registered clients yet. | Accept it and continue. |
| PIN rejected / token locked | The PIN does not match the token. | Use `Secret1234`. If locked, remove the container (`docker compose ... rm -f ss-smecu`) and start over. |
| `Generate CSR` is disabled | Usage is `SIGNING` but no Client is selected. | Select `DEV/BIZ/SME-CU`. |
| Test CA returns an HTML error page | Wrong **Type** selected, or the upload failed. | Choose **Auth** for the auth key and **Sign** for the signing key. DER is accepted. |
| `Import cert.` rejects the file | Not a PEM/DER certificate, or the key does not match. | Re-download the signed cert from the test CA; sign the CSR that belongs to this key. |
| Register fails with `tr.signer.unknown_member` and *Could not find any certificates for member 'MEMBER:DEV/BIZ/SME-CU'* | The member has no signing certificate in the token, so the registration request cannot be signed. | Add the `Sign key` and import its certificate (section 5), then click Register again. |
| Status stays **Registration in progress** | The CS has not approved, or rejected silently. | Check **Management Requests** in the CS, not only the SS. |
| Auth certificate never becomes **Registered** | Approve did not reach the SS, or global configuration is stale. | Wait a minute; check SS **Diagnostics** and `docker logs ss-smecu`. |
| `SS-SMECU` already in use | The owner already has a server with that code. | Delete the old SS in the CS, or pick a different code (and update later pages). |

## Why this step exists

This is where the trust chain becomes real:

- the **anchor** tells the SS which CS to trust and where to download configuration;
- the **authentication certificate** identifies this server to other servers over mTLS;
- the **signing certificate** identifies the member on messages the server sends.

None of the three is optional, and each has its own failure mode. Doing them in this
order means a failure is attributable to exactly one of them.

The order is **not** cosmetic: registering the authentication certificate sends a request
signed by the member's signing key. If that key does not exist yet, the signer refuses
with `tr.signer.unknown_member`. The upstream `setup.hurl` creates the signing key before
it calls `Register` for the same reason.

Note that the signing certificate is imported but not registered here. That looks wrong
until 08b: it becomes registered as part of the first client registration, because it
belongs to the member, not to the server.

Next: [08b-management-services.md](08b-management-services.md)
