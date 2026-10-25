# 05 — Create the Members

**Status: Verified 2026-10-25** — both members created, `MANAGEMENT` subsystem exists, management service provider set.

**Goal:** create the two organizations in the Central Server, then make the CS able to
publish configuration by giving it a management service provider.

**Time:** 5 minutes.

## Run

1. Sidebar → **Members**.
2. Click **+ Add member**.
3. Fill the dialog (fields appear in this order: **Member name**, **Member class**,
   **Member code**) and click **Add**.

Create both:

| Member name | Member class | Member code |
| --- | --- | --- |
| `SME-CU` | `BIZ` | `SME-CU` |
| `Ministry of Commerce` | `GOV` | `MOC` |

**Member code** is the identifier that appears in every X-Road ID
(`DEV/BIZ/SME-CU`). **Member name** is display text only. Do not swap them.

## Expected

The Members table lists two rows. Open each one: the encoded ID is

```text
DEV:BIZ:SME-CU
DEV:GOV:MOC
```

and the **Subsystems** tab of both is empty.

## 3. Create the MANAGEMENT subsystem

Global configuration cannot be generated until the CS knows **which subsystem provides
management services**. That subsystem must exist in the registry before you can select it.

Members → open **SME-CU** → **Subsystems** tab → **+ Add subsystem**:

| Field | Value |
| --- | --- |
| **Subsystem code** | `MANAGEMENT` |
| **Subsystem name** | `Management` |

**Expected:** SME-CU now has one subsystem, `DEV/BIZ/SME-CU/MANAGEMENT`.

This is only a registry entry. The subsystem becomes an actual client of a Security
Server in step 08, when it will host the management service.

## 4. Set the management service provider

Settings → **System Settings** → card **Management Services**.

On the row **Service Provider Identifier**, click **Edit** → dialog **Select
Subsystem** → choose `DEV/BIZ/SME-CU/MANAGEMENT`.

**Expected:** the row reads `SUBSYSTEM:DEV/BIZ/SME-CU/MANAGEMENT`, the *Service Provider
Name* is `SME-CU`, and within about two minutes the *Global configuration generation
failing* message disappears from the dashboard.

The **Management Services' Security Server** row stays empty until step 08 — the server
must exist before it can be selected.

> **Lab shortcut:** the real topology gives management services to the CamDX operator.
> This first pass has no CamDX Security Server yet, so SME-CU hosts them. The topology
> page records the difference; do not present this as the production arrangement.

## Verify from the API

```bash
cd /tmp && rm -f cs.cookies
curl -sk -c cs.cookies -X POST https://localhost:4100/login \
     -d 'username=xrd&password=secret'
XSRF=$(awk '$6=="XSRF-TOKEN"{print $7}' cs.cookies)
curl -sk -b cs.cookies -H "X-XSRF-TOKEN: $XSRF" \
     'https://localhost:4100/api/v1/clients/?page=0&limit=20'
```

**Expected**

```json
{"clients":[
  {"member_name":"SME-CU","client_id":{"member_class":"BIZ","member_code":"SME-CU","encoded_id":"DEV:BIZ:SME-CU"}},
  {"member_name":"Ministry of Commerce","client_id":{"member_class":"GOV","member_code":"MOC","encoded_id":"DEV:GOV:MOC"}}
],"paging_metadata":{"total_items":2,"items":2,"limit":20,"offset":0}}
```

That listing counts members only. Prove the management service provider separately:

```bash
curl "${H[@]}" https://localhost:4100/api/v1/management-services-configuration
curl "${H[@]}" https://localhost:4100/api/v1/notifications/alerts
```

**Expected**

```json
{"service_provider_id":"SUBSYSTEM:DEV:BIZ:SME-CU:MANAGEMENT","security_server_id":"", ...}
[]
```

An empty alerts list is the proof that global configuration generation recovered. If it
is still non-empty, wait two minutes and repeat; generation runs on a timer.

(`clients` is the unified member + subsystem listing; a bare member is a client of type
`MEMBER`. The response also contains `type` and `instance_id` fields, trimmed above.)

## Deliberately not done yet

The subsystems `PORTAL` and `REGISTRY` are created in **step 10**, when each Security
Server submits its client registration and the CS approves it. That is the flow the demo
automation uses.

Creating them here instead is also valid — and
[the registration checklist](../docs/x-road/04-registration-and-access.md) recommends it
for auditability. Waiting until step 10 means you see the *approval* half of
registration, which is where the "stuck in Registration in progress" trap lives.

The `MANAGEMENT` subsystem is the exception: its registry entry has to exist **now**,
because global configuration generation needs a management service provider. It becomes
a Security Server client in step 08, and the server is then selected on this same
Management Services card.

## If it fails

| Symptom | Meaning | Fix |
| --- | --- | --- |
| The **Member class** dropdown is empty | Step 04's member classes were not saved. | Go back to Settings → System Settings → Member Classes. |
| `Member class ... not found` | You typed a class that does not exist, or used lowercase. | Codes are case-sensitive: `BIZ`, `GOV`. |
| `Member with the same id already exists` | Duplicate entry, or state survived a reset. | Check the table; skip if the row is already correct. |
| Row appears but the ID has the wrong member code | You filled **Member name** and **Member code** in the wrong order. | Delete the member and recreate it. The code is what matters. |
| **Select Subsystem** dialog is empty | The `MANAGEMENT` subsystem was not created. | Section 3: add it under SME-CU. |
| Generation still failing after selecting the provider | The selection was not saved, or generation has not run yet. | Re-open the card; check `service_provider_id` via the API; wait two minutes. |
| `Management service provider not set` | Same as above, but surfaced by an API call. | Select `DEV/BIZ/SME-CU/MANAGEMENT` in Management Services. |

## Why this step exists

A member is only a registry entry: it cannot send or receive anything until it has a
Security Server client and a subsystem. Creating the member separately makes it obvious
that "registered organization" and "registered client" are different states.

The management subsystem is a second lesson: the CS itself is a participant in the
X-Road fabric. It needs a registered subsystem to provide management services, and it
cannot publish signed configuration until that provider is named. This is the piece the
demo script hid, and the reason the dashboard kept complaining after initialization.

Next: [06-add-ca-ocsp-tsa.md](06-add-ca-ocsp-tsa.md)
