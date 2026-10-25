# 06 — Add the CA, OCSP and TSA

**Status: Verified 2026-10-25** — CA approved; OCSP responder and TSA registered.

**Goal:** approve the test CA in the Central Server, register its OCSP responder, and
register the timestamping authority. Without these, no certificate can be validated and
no message can be signed.

**Time:** 5 minutes.

## 0. Get the three certificates

The test CA keeps its certificates in a Docker volume, not on the host. Copy them out:

```bash
cd /home/khezy/Documents/learning/x-road-lab
./practice-lab/scripts/ca-certs.sh
```

**Expected**

```text
-rw-r--r-- ... ca.pem
-rw-r--r-- ... ocsp.pem
-rw-r--r-- ... tsa.pem
```

They land in `./ca-certs/`. Treat them as lab-only material; never commit them.

## 1. Add the certification service

Sidebar → **Trust Services** → **Certification Services** → **+ Add certification
service** (the dialog title is *Add certification service*).

1. Upload `ca-certs/ca.pem` (*"Upload the certificate file from your computer"*) and
   confirm with **Upload**.
2. A second dialog, **CA Settings**, opens. Set:

| Field | Value |
| --- | --- |
| *This CA can only be used for TLS authentication (mark if true)* | **unchecked** |
| **Certificate profile info** | `ee.ria.xroad.common.certificateprofile.impl.FiVRKCertificateProfileInfoProvider` |
| **Default CSR format** | `DER` |
| ACME fields | leave off |

**Certificate profile info** is required and has no default. The value is the fully
qualified class name that implements
`ee.ria.xroad.common.certificateprofile.CertificateProfileInfoProvider`. The dev stack
uses the FI/VRK profile; it is the only one shipped with this build.

**Default CSR format** also matters later: it becomes the format Security Servers use
when they create keys, and it **locks** the CSR-format selector in the SS key wizard.
Leave it at `DER` — the test CA detects and signs both DER and PEM, so this is not a
limitation.

Click **Save**.

**Expected:** the table lists one row named **Test CA** (taken from the certificate's
CN/O), with no red state.

## 2. Add the OCSP responder

Open the **Test CA** row → **OCSP Responders** tab → **+ Add OCSP Responder**.

| Field | Value |
| --- | --- |
| **URL** | `http://testca:8888` |
| Certificate | `ca-certs/ocsp.pem` |
| Cost type | **Free** |

**Expected:** one responder with URL `http://testca:8888`.

The URL is the **container name**, not `localhost`: the CS validates it from inside the
CS container, where `localhost` is the CS itself.

## 3. Add the timestamping service

Sidebar → **Trust Services** → **Timestamping Services** → **+ Add timestamping
service**.

| Field | Value |
| --- | --- |
| **URL** | `http://testca:8899` |
| Certificate | `ca-certs/tsa.pem` |
| Cost type | **Free** |

**Expected:** one row with URL `http://testca:8899`.

The TSA certificate must carry the non-repudiation key usage. The test CA's
`tsa.cert.pem` does; a certificate without it is rejected.

## 4. Verify

In the UI, all three lists have exactly one entry.

From the API:

```bash
cd /tmp && rm -f cs.cookies
curl -sk -c cs.cookies -X POST https://localhost:4100/login \
     -d 'username=xrd&password=secret'
XSRF=$(awk '$6=="XSRF-TOKEN"{print $7}' cs.cookies)
H=(-sk -b cs.cookies -H "X-XSRF-TOKEN: $XSRF")

curl "${H[@]}" https://localhost:4100/api/v1/certification-services
curl "${H[@]}" https://localhost:4100/api/v1/certification-services/1/ocsp-responders
curl "${H[@]}" https://localhost:4100/api/v1/timestamping-services | head -c 200
```

**Expected** (IDs may differ; URLs and counts must match)

```json
[{"id":1,"name":"Test CA", ...}]
[{"url":"http://testca:8888","cost_type":"FREE","id":1,"has_certificate":true}]
[{"url":"http://testca:8899","cost_type":"FREE", ...}]
```

## If it fails

| Symptom | Meaning | Fix |
| --- | --- | --- |
| Upload says the certificate is invalid | Wrong file or a key instead of a certificate. | Use `ca.pem` from `scripts/ca-certs.sh`; check `openssl x509 -in ca.pem -noout -subject`. |
| **Save** is disabled in CA Settings | **Certificate profile info** is empty (it is required). | Paste the FiVRK class name above. |
| `Certification service ... already exists` | You uploaded the same CA twice. | Keep the existing row. |
| OCSP responder rejected / `Certification service not found` | Wrong certification service, or the URL is not an HTTP URL. | Use the Test CA row and `http://testca:8888`. |
| TSA rejected | The certificate is not a timestamping certificate. | Use `tsa.pem`; verify with `openssl x509 -in tsa.pem -noout -text | grep -A1 "Key Usage"`. |
| CS logs show connection errors to `testca` | The CS cannot resolve the container name. | Both containers must be on `xroad-network`; check `docker exec cs curl -s -o /dev/null -w '%{http_code}\n' http://testca:8888/testca/` (expects `200`). |

## Why this step exists

Certificates are only as good as the entities that vouch for them. The CS must know:

- **which CA** may issue member and Security Server certificates,
- **how to ask** whether a certificate is revoked (OCSP),
- **who** timestamps messages so the message log is legally meaningful.

Skip any of the three and step 08 fails at certificate registration, with an error that
looks like a certificate problem rather than missing configuration.

Next: [07-start-sses.md](07-start-sses.md)
