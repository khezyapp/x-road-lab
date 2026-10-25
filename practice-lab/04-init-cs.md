# 04 — Initialize the Central Server

**Status: Verified 2026-10-25** — instance `DEV`, both configuration signing keys active, member classes present, generation alerts clear.

**Goal:** create instance `DEV`, set the software token PIN, generate the two configuration
signing keys, and add the member classes `BIZ` and `GOV`.

**Time:** 3 minutes.

## 1. Initialize

Open **`https://localhost:4100`** and log in with `xrd` / `secret`.

An uninitialized Central Server lands on **Initial Configuration**. Fill it exactly:

| Field | Value | Why |
| --- | --- | --- |
| **Instance Identifier** | `DEV` | The lab instance. Never the real CamDX identifier. |
| **Central Server Address** | `cs` | The address Security Servers use to reach the CS. The container name works inside `xroad-network`; `localhost` would not. |
| **PIN** | `Secret1234` | Software token PIN. Dev-only value. |
| **Repeat PIN** | `Secret1234` | Must match. |

Click **Submit**. Initialization takes a few seconds.

**Expected:** a notification reading *"The Central Server initialized! Now continue with
the full configuration."*

If you instead land on **Members**, the CS was already initialized (a previous run's
container survived). Check **Global Configuration → Signing Keys** and **Settings →
System Settings → Management Services** before continuing; if either is empty, continue
with sections 2–4 on the existing instance, or reset with
[19-reset.md](19-reset.md) and start clean.

## 2. Generate the configuration signing keys

Initialization creates the software token but **not** the keys that sign global
configuration. Until they exist, the dashboard shows:

```text
Signing of external configuration failed - active key missing
Signing of internal configuration failed - active key missing
Global configuration generation failing since <timestamp>
```

Create both keys:

1. Sidebar → **Global Configuration** → tab **Internal Configuration**.
2. Section **Signing Keys** → expand **softToken-0** → **+ Add key**.
3. **Key friendly name** = `Internal signing key` → save.
4. Switch to the **External Configuration** tab → same steps → `External signing key`.

Both keys are generated inside the software token and self-signed by the Central Server.
No CA is involved, and the token is already logged in (the container sets the PIN at
start).

**Expected:** within a minute, both *"active key missing"* messages disappear.

The dashboard may still say **Global configuration generation failing**. That is a
different, later problem — the management service provider — and it is fixed in
[step 05](05-create-members.md). Do not stop here trying to fix it.

## 3. Add the member classes

The lab uses `BIZ` for SME-CU and `GOV` for MOC. The demo stack only creates `COM`, `GOV`
and `ORG`, so `BIZ` must be added by hand.

Go to **Settings** → **System Settings** → the **Member Classes** section.

For each class click **+ Add**, which opens **Add member class**:

| Code | Description |
| --- | --- |
| `BIZ` | Business |
| `GOV` | Government |

The dialog has exactly two fields: **Code** and **Description**. Click **Save** in the
dialog.

**Expected:** the Member Classes table lists `BIZ` and `GOV`.

## 4. Verify

In the UI: the two classes above, **Members** is empty, and **Global Configuration** →
**Internal Configuration** / **External Configuration** each list one signing key.

From the API:

```bash
cd /tmp && rm -f cs.cookies
curl -sk -c cs.cookies -X POST https://localhost:4100/login \
     -d 'username=xrd&password=secret'
XSRF=$(awk '$6=="XSRF-TOKEN"{print $7}' cs.cookies)
H=(-sk -b cs.cookies -H "X-XSRF-TOKEN: $XSRF")

curl "${H[@]}" https://localhost:4100/api/v1/system/status
curl "${H[@]}" https://localhost:4100/api/v1/member-classes
curl "${H[@]}" https://localhost:4100/api/v1/tokens \
  | python3 -c 'import json,sys; t=json.load(sys.stdin)[0]; print([k["label"]["label"] for k in t["configuration_signing_keys"]])'
```

**Expected**

```json
{"initialization_status":{"instance_identifier":"DEV",
 "central_server_address":"cs","software_token_init_status":"INITIALIZED"}}
[{"description":"Business","code":"BIZ"},{"description":"Government","code":"GOV"}]
['Internal signing key', 'External signing key']
```

(The first object is trimmed: the real response also contains `high_availability_status`.)

## If it fails

| Symptom | Meaning | Fix |
| --- | --- | --- |
| `Already fully initialized` | The CS database survived a previous run. | Keep it and continue, or `down -v` (step 19) for a clean start. |
| `Token pin policy failure` | PIN rejected by the signer. | Use `Secret1234` or a longer mixed PIN. |
| `Instance identifier should not contain ...` | `:`, `;`, `/`, `\`, `%` are forbidden. | Use `DEV`. |
| `Instance identifier` shows `Dev` / `dev` | Identifiers are case-sensitive; every later ID assumes `DEV`. | Cannot be edited — `down -v` and redo this page with `DEV`. |
| `Signing of internal/external configuration failed - active key missing` | No configuration signing keys exist. | Section 2: add one key per configuration source. |
| `Global configuration generation failing since ...` persists after the keys | The management service provider is not set. | Expected here; fix it in [step 05](05-create-members.md). |
| `Member class with the same code already exists` | You added it twice, or it survived a reset. | Skip it; duplicates are harmless. |
| Initialization spins then fails | The signer inside the CS cannot initialize its token. | `docker logs --tail=100 cs`; reset if the token is half-initialized. |

## Why this step exists

The instance identifier prefixes **every** X-Road identifier you will type later
(`DEV/BIZ/SME-CU/PORTAL`) and cannot be changed. The member classes are the namespace
above the member code, so they must exist before step 05 can create members.

The signing keys are easy to miss and expensive to skip: an initialized CS without them
is initialized but mute — it cannot publish any configuration, so no Security Server can
download global configuration. Initialization does not create them; the demo automation
did, which is why this page used to be wrong.

Next: [05-create-members.md](05-create-members.md)
