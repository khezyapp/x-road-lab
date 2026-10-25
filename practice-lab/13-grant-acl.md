# 13 — Grant Access

**Status: Verified 2026-10-25** — `DEV/BIZ/SME-CU/PORTAL` has access to `businessLookup`.

**Goal:** add `DEV/BIZ/SME-CU/PORTAL` as an ACL subject on `businessLookup`. Without this,
every call is denied, even though the service is published and enabled.

**Time:** 3 minutes.

## Preconditions

- Step 12 done: `businessLookup` is enabled on `ss-moc`.
- `DEV/BIZ/SME-CU/PORTAL` is a **Registered** client of `ss-smecu`.

## 1. Open the service

`https://localhost:4300` → **Clients** → `DEV/GOV/MOC/REGISTRY` → **Services** →
click the service `businessLookup` (not the description row).

Open its **Parameters** tab and scroll to the **Access Rights** section.

## 2. Add the subject

1. Click **Add subjects**.
2. In the **Add Subjects** dialog, fill the search:

| Field | Value |
| --- | --- |
| **Name** | leave **empty** (it filters the member's display name, not the subsystem) |
| **Instance** | `DEV` |
| **Member Class** | `BIZ` |
| **Member Code** | `SME-CU` |
| **Subsystem Code** | `PORTAL` |
| **Subject type** | `Subsystem` (if the field is shown) |

If you prefer to use **Name**, enter `SME-CU` — the member's name. It is not the
subsystem name (`Portal`); the field maps to `member_name_group_description`.

3. Click **Search**.
4. Tick the row `DEV/BIZ/SME-CU/PORTAL`.
5. Click **Save**.

**Expected:** *"Access rights added successfully"*, and the subject appears in the Access
Rights list with **Access Rights given**.

## 3. Verify

```bash
cd /tmp && rm -f moc.cookies
curl -sk -c moc.cookies -X POST https://localhost:4300/login \
     -d 'username=xrd&password=secret' -o /dev/null
X=$(awk '$6=="XSRF-TOKEN"{print $7}' moc.cookies)

curl -sk -b moc.cookies -H "X-XSRF-TOKEN: $X" \
  'https://localhost:4300/api/v1/services/DEV%3AGOV%3AMOC%3AREGISTRY%3AbusinessLookup/service-clients'
```

**Expected:** a list containing `DEV:BIZ:SME-CU:PORTAL`.

```bash
curl -sk -b moc.cookies -H "X-XSRF-TOKEN: $X" \
  'https://localhost:4300/api/v1/clients/DEV%3AGOV%3AMOC%3AREGISTRY/service-clients/DEV%3ABIZ%3ASME-CU%3APORTAL/access-rights'
```

**Expected:** one access right for `businessLookup`.

## Alternative path

The same ACL can be created from the client side: **Clients → REGISTRY → Service clients
→ Add subject**, pick `security…`/`DEV/BIZ/SME-CU/PORTAL`, then select the service
`businessLookup` in the wizard. Both write the same access-right record.

## If it fails

| Symptom | Meaning | Fix |
| --- | --- | --- |
| Search returns nothing | The subsystem does not exist in the CS, or a filter is wrong. | Check `DEV/BIZ/SME-CU/PORTAL` exists and is `REGISTERED` on `ss-smecu`. |
| Subject added, call still fails with `access_denied` | The ACL was created at the wrong level, or for a different service. | Check the service's Access Rights list; the ACL is per service code. |
| `Access rights given` but empty list | You added the subject without ticking the service. | Re-add and select `businessLookup`. |
| Duplicate subject | Already present. | Remove and re-add only if the rights look wrong. |
| Call returns 404 instead of `access_denied` | The service code or path is wrong — this is not an ACL problem. | Compare with step 12 (`businessLookup`). |

## Why this step exists

Access is **denied by default**. Registering a client and enabling a service are only two
of the three conditions; the third is an ACL entry naming the exact consumer subsystem.

This is the single most common cause of "everything looks registered but I get `access_denied`". The
subject must match the subsystem that sends `X-Road-Client`, and the ACL matches the
**service code**, not a version.

Next: [14-first-call.md](14-first-call.md)
