# 10 — Register the Clients

**Status: Verified 2026-10-25** — `PORTAL` and `REGISTRY` registered; consumer connection types set to HTTP.

**Goal:** register `DEV/BIZ/SME-CU/PORTAL` on `ss-smecu` and `DEV/GOV/MOC/REGISTRY` on
`ss-moc`, and approve both in the CS.

**Time:** 10 minutes.

## Preconditions

- Step 09 done: `ss-moc` auth certificate **Registered**, `Sign key` certificate
  **Saved**.
- Step 08b done: management services working, `ss-smecu` client **Registered**.
- Both Security Servers show their owner member as a `REGISTERED` client.

## No new keys

A **signing key belongs to the member**, not to the subsystem. SME-CU already has its
`Sign key` (registered in 08b); MOC has its `Sign key` from step 09. Adding `PORTAL` and
`REGISTRY` therefore asks for **no key** — they reuse the member certificate.

MOC's sign certificate is still `SAVED`; it becomes `REGISTERED` when `REGISTRY` is
registered, exactly as SME-CU's did when `MANAGEMENT` was registered.

## 1. Add PORTAL on ss-smecu

`https://localhost:4200` → **Clients** → **+ Add client**.

**Do not use Select Client** — `PORTAL` does not exist in the CS yet, so it will not appear
in the global list. Enter it by hand:

| Field | Value |
| --- | --- |
| **Member Class** | `BIZ` |
| **Member Code** | `SME-CU` |
| **Subsystem Code** | `PORTAL` |
| **Subsystem Name** | `Portal` |

**Next** → finish page → tick **Register client** → **Submit**.

**Expected:** the client appears with status **Registration in progress**.

> Alternative, also valid: create the subsystem in the CS first (Members → SME-CU →
> Subsystems → Add subsystem), then use **Select Client** on the SS. This is what the
> [registration checklist](../docs/x-road/04-registration-and-access.md) recommends for
> auditability. Typing it on the SS is the path the demo automation uses.

## 2. Approve PORTAL in the CS

CS UI → **Management Requests** → approve the client registration for
`DEV/BIZ/SME-CU/PORTAL`.

**Expected:** the SS client status becomes **Registered**. The inbox may look empty if
the request was already approved — verify the client status, not the inbox.

## 2b. Set the connection type to HTTP

The UI creates clients with **HTTPS**, which tells the proxy to demand a TLS client
certificate from the information system. `curl` and the Spring Boot apps do not present
one, so the first call fails with:

```text
clientproxy.ssl_authentication_failed: Client (...) specifies HTTPS but did not supply TLS certificate
```

Fix it on the consumer:

**Clients** → `DEV/BIZ/SME-CU/PORTAL` → **Internal servers** tab → **Connection type** =
`HTTP`.

**Expected:** *"Connection type updated"*. The select saves on change.

The demo automation creates clients with `"connection_type": "HTTP"` for exactly this
reason. Set the same for `REGISTRY` if it will act as a consumer in step 16.

## 3. Add REGISTRY on ss-moc

`https://localhost:4300` → **Clients** → **+ Add client**.

| Field | Value |
| --- | --- |
| **Member Class** | `GOV` |
| **Member Code** | `MOC` |
| **Subsystem Code** | `REGISTRY` |
| **Subsystem Name** | `Registry` |

**Next** → tick **Register client** → **Submit**.

Then approve it in the CS.

**Expected:**

- `DEV:GOV:MOC:REGISTRY` is **Registered** on `ss-moc`.
- MOC's `Sign key` certificate changes from **Saved** to **Registered**.

## 4. Verify

```bash
cd /tmp
for p in 4200 4300; do
  rm -f c$p.cookies
  curl -sk -c c$p.cookies -X POST https://localhost:$p/login \
       -d 'username=xrd&password=secret' -o /dev/null
  X=$(awk '$6=="XSRF-TOKEN"{print $7}' c$p.cookies)
  echo "== $p =="
  curl -sk -b c$p.cookies -H "X-XSRF-TOKEN: $X" https://localhost:$p/api/v1/clients \
    | python3 -c 'import json,sys; d=json.load(sys.stdin); [print(c["id"], c["status"]) for c in d]'
done
```

**Expected**

```text
== 4200 ==
DEV:BIZ:SME-CU REGISTERED
DEV:BIZ:SME-CU:MANAGEMENT REGISTERED
DEV:BIZ:SME-CU:PORTAL REGISTERED
== 4300 ==
DEV:GOV:MOC REGISTERED
DEV:GOV:MOC:REGISTRY REGISTERED
```

And in the CS:

```bash
cd /tmp && rm -f cs.cookies
curl -sk -c cs.cookies -X POST https://localhost:4100/login \
     -d 'username=xrd&password=secret' -o /dev/null
X=$(awk '$6=="XSRF-TOKEN"{print $7}' cs.cookies)
curl -sk -b cs.cookies -H "X-XSRF-TOKEN: $X" \
     'https://localhost:4100/api/v1/clients/?page=0&limit=20'
```

**Expected:** five clients: the two members, `MANAGEMENT`, `PORTAL`, `REGISTRY`.

## If it fails

| Symptom | Meaning | Fix |
| --- | --- | --- |
| `Select Client` does not list PORTAL / REGISTRY | They are not in the CS registry yet. | Type the IDs by hand, or create the subsystems in the CS first. |
| `Subsystem already exists` | It was created in the CS already. | Use **Select Client** instead of typing. |
| Client stays **Saved** | You did not tick **Register client**, or the wizard's Submit did not register. | Open the client → **Register**. |
| Register fails `tr.signer.unknown_member` | The member has no signing certificate in the token. | Import the `Sign key` certificate (08 for SME-CU, 09 for MOC). |
| Register fails with `ssl_authentication_failed` / *Security server has no valid authentication certificate* | The SS's **own** auth certificate is registered but **not active**. | Open the AUTH certificate → Certificate details → **Activate**, then retry. Do this on both servers. |
| Register fails with `no_timestamping_provider_found` / *Cannot time-stamp messages: no timestamping services configured* | The SS has not selected a TSA from global configuration. | Settings → System Parameters → Timestamping Services → **Add** → `Test TSA` (`http://testca:8899`). Do this on both servers. |
| Register fails with `unknown_host` and `INSERT_MANAGEMENT_SERVICE_ADDRESS_HERE` | The management WSDL was imported with its placeholder service URL and never corrected. | Open the WSDL description → a service → **Parameters** → Service URL `https://cs:4002/managementservice/manage/` → tick **Apply to all** → save. |
| Stuck in **Registration in progress** | Not approved, or rejected silently. | Approve in the CS Management Requests (check the status filter). |
| MOC sign cert stays **Saved** after approval | The client registration did not complete. | Verify the client is `REGISTERED`; refresh the token view. |
| Wrong member class (`COM` instead of `BIZ`/`GOV`) | Typed from the demo guide. | Delete the client and re-add with the right class. |

## Why this step exists

Up to now the servers were registered but had no *identities* to act with. A subsystem is
that identity: `PORTAL` is what will put `X-Road-Client` on requests, `REGISTRY` is what
will own the service `MOC` publishes.

This is also where the two directions of the lab become concrete: SME-CU now has a
consumer subsystem, MOC has a provider subsystem. Neither can do anything with the other
yet — that is steps 12–14 (service description, ACL, first call).

Next: [11-deploy-apps.md](11-deploy-apps.md)
