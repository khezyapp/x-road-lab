# 08b — Management Services

**Status: Verified 2026-10-25** — provider registered, `MANAGEMENT` client registered, WSDL enabled with service URL overridden, ACL granted.

**Goal:** make `ss-smecu` the Security Server that provides management services, register
the `MANAGEMENT` client, publish the management WSDL, and grant the
`security-server-owners` group access to it.

**Time:** 10–15 minutes.

## Preconditions

- Step 08 done: auth certificate **Registered**, `Sign key` certificate imported
  (**Saved**), token logged in.
- Step 05 done: `DEV/BIZ/SME-CU/MANAGEMENT` exists in the CS and is set as the management
  service provider. The *Management Services' Security Server* row is still empty.

## 1. Register this server as the management service provider (CS)

CS UI → **Settings** → **System Settings** → **Management Services** → row *Management
Services' Security Server* → **Edit** → dialog **Select Security Server** → `SS-SMECU`.

**Expected:** the row shows the server with owner `DEV/BIZ/SME-CU`.

This is the `register-provider` call; the CS stores
`security_server_id = DEV:BIZ:SME-CU:SS-SMECU`.

```bash
cd /tmp && rm -f cs.cookies
curl -sk -c cs.cookies -X POST https://localhost:4100/login \
     -d 'username=xrd&password=secret' -o /dev/null
X=$(awk '$6=="XSRF-TOKEN"{print $7}' cs.cookies)
curl -sk -b cs.cookies -H "X-XSRF-TOKEN: $X" \
     https://localhost:4100/api/v1/management-services-configuration
```

**Expected:** `service_provider_id` is the MANAGEMENT subsystem, `security_server_id` is
no longer empty.

## 2. Add the MANAGEMENT client (SS)

The subsystem already exists in the CS, so select it instead of retyping it.

1. **Clients** → **+ Add client**.
2. Click **Select Client** (the page says *"If the Client is already existing, you can
   select it from the Global list"*).
3. Choose `DEV/BIZ/SME-CU/MANAGEMENT`.
4. **Next** → finish page.
5. Tick **Register client**.
6. **Submit**.

If you prefer to type it: Member Class `BIZ`, Member Code `SME-CU`, Subsystem Code
`MANAGEMENT`, Subsystem Name `Management`.

**There is no signing-key field in this wizard.** The subsystem reuses the member's
signing certificate — the `Sign key` you imported in step 08. The finish page's text about
"a new key and CSR" applies only when you add a **new member**, not a subsystem of an
existing one.

**Expected:** the client appears with status **Registration in progress**.

> If the `Sign key` certificate is missing from the token, the client is still added, but
> Register fails with `tr.signer.unknown_member`. That is why step 08 creates it first.

## 3. Approve the client registration (CS)

CS UI → **Management Requests** → approve the client registration request for
`DEV/BIZ/SME-CU/MANAGEMENT`.

**Expected:** the SS client status becomes **Registered**, and the `Sign key`
certificate in the token also becomes **Registered** (it is registered as part of the
member's first client).

> **The page only lists requests that are still waiting.** Once a request is approved it
> leaves the default view, so an empty page can mean "already approved", not "never
> arrived". Confirm the outcome instead of the inbox:

```bash
# SS: client and key state
curl -sk -b q.cookies -H "X-XSRF-TOKEN: $X" https://localhost:4200/api/v1/clients
```

**Expected:** `DEV:BIZ:SME-CU:MANAGEMENT` with `"status":"REGISTERED"`.

If you want to see the history in the UI, change the status filter on the Management
Requests page (or call `/api/v1/management-requests?sort=id&desc=true`); approved requests
appear there with `"status":"APPROVED"`.

## 4. Publish the management WSDL (SS)

1. **Clients** → `DEV/BIZ/SME-CU/MANAGEMENT` → **Services**.
2. **Add WSDL**.
3. **URL** = `http://cs/managementservices.wsdl` — copy it from the **WSDL Address** row
   on the CS Management Services card if in doubt.
4. Add, then **Enable** the description (new descriptions are disabled by default).

**Expected:** the description is enabled and lists the management operations
(`clientReg`, `clientDeletion`, `authCertDeletion`, and so on).

### Override the service URL — required

The WSDL shipped with the CS contains a literal placeholder:

```xml
<soap:address location="http://INSERT_MANAGEMENT_SERVICE_ADDRESS_HERE"/>
```

Every imported service therefore has that useless URL, and the first management request
fails with `unknown_host`. The upstream script fixes it with a `url` + `url_all` PATCH
immediately after import.

In the UI:

1. Still under **Clients → MANAGEMENT → Services**, click the WSDL description to expand
   it, then click any service (for example `clientReg`).
2. Open the **Parameters** tab.
3. **Service URL** = `https://cs:4002/managementservice/manage/` — this is the
   **Management Services Address** row on the CS Management Services card.
4. Tick **Apply to all**.
5. Save.

Verify every service now points at the real address:

```bash
curl -sk -b ss.cookies -H "X-XSRF-TOKEN: $X" \
  'https://localhost:4200/api/v1/clients/DEV%3ABIZ%3ASME-CU%3AMANAGEMENT/service-descriptions' \
  | python3 -c 'import json,sys; d=json.load(sys.stdin); print(sorted({s["url"] for s in d[0]["services"]}))'
```

**Expected:** `['https://cs:4002/managementservice/manage/']` — no
`INSERT_MANAGEMENT_SERVICE_ADDRESS_HERE`.

The URL must be reachable *from the SS container*, not from your browser:

```bash
docker exec ss-smecu sh -c "curl -s -o /dev/null -w '%{http_code}\n' http://cs/managementservices.wsdl"
```

**Expected:** `200`.

## 5. Grant access to the security server owners (SS)

1. **Clients** → `DEV/BIZ/SME-CU/MANAGEMENT` → **Service clients**.
2. **Add subject**.
3. Step **Member / Group** → global group `security-server-owners` → **Next**.
4. Step **Servicecode** → tick the management operations:

```text
clientReg            clientDeletion       authCertDeletion
ownerChange          clientEnable         clientDisable
addressChange        clientRename         maintenanceModeEnable
maintenanceModeDisable
```

5. **Finish**.

**Expected:** `security-server-owners` appears in the service-clients list with access
rights given.

## 6. Verify

- CS Management Services: provider = `DEV/BIZ/SME-CU/MANAGEMENT`, server = `SS-SMECU`.
- SS Clients: `DEV/BIZ/SME-CU/MANAGEMENT` is **Registered**.
- SS Services: the management WSDL description is **Enabled**.
- SS Service clients: `security-server-owners` has the ten operations.
- SS token: SIGN certificate now **Registered**.

## 7. Deleting a client you added by mistake

This is normal while learning; it is local to the SS.

1. **Clients** → click the client (`DEV/BIZ/SME-CU/MANAGEMENT`).
2. Use the button in the header:
   - Status **Saved** → **Delete** → confirm *"Do you want to delete this client?"*
   - Status **Registered**, **Registration in progress**, **Disabled**, or **…in
     progress** → **Delete is hidden**. Click **Unregister** first, confirm, wait for the
     CS to approve the unregistration, then **Delete**.
3. If a dialog asks *"Signature key and certificate associated with the deleted client
   have no users. Delete key and certificate?"* → click **No**. That key is the member's
   `Sign key`; deleting it would force you to redo step 08.

Deleting the SS client does **not** remove `DEV/BIZ/SME-CU/MANAGEMENT` from the CS
registry — that entry was created in step 05. Re-adding the client later selects the same
subsystem again.

## If it fails

| Symptom | Meaning | Fix |
| --- | --- | --- |
| **Select Client** list is empty or lacks MANAGEMENT | The subsystem is not in the CS registry. | Create it in the CS (Members → SME-CU → Subsystems), then refresh. |
| Register fails with `tr.signer.unknown_member` | The member has no signing certificate in the token. | Import the `Sign key` certificate (step 08, section 5) and retry. |
| Client stays **Registration in progress** | The CS has not approved, or rejected silently. | Check **Management Requests** in the CS, including the status filter for approved/declined requests. |
| **Management Requests** looks empty | Pending requests are shown by default; yours may already be approved. | Verify the client status on the SS (`REGISTERED`) rather than relying on the inbox. |
| **Add WSDL** rejects the URL | The SS cannot fetch it, or the URL is wrong. | Use `http://cs/managementservices.wsdl`; test with `docker exec ss-smecu curl`. |
| WSDL added but services are "unknown" | The description is disabled. | Enable it. |
| `security-server-owners` is not selectable | The global group is defined in the CS and may need a refresh. | Reload the view; confirm the group exists in the CS. |
| Delete button is missing | The client is registered or in progress. | Unregister first, then delete. |
| Orphan dialog appeared after delete | The sign key has no other clients. | Click **No**; keep the key. |

## Why this step exists

Security Servers exchange management requests through a **management service**: a
subsystem hosted on one SS that proxies to the CS. The CS records which subsystem and
which server provide it; the SS publishes the WSDL and grants the
`security-server-owners` group the right to call it.

Get the order wrong — provider not set, client not registered, WSDL not enabled, ACL
missing — and later client registrations can hang in *Registration in progress* with no
error on the SS. That is the failure this step prevents.

Previous: [08-init-ss-smecu.md](08-init-ss-smecu.md)

Next: [09-init-ss-moc.md](09-init-ss-moc.md)
