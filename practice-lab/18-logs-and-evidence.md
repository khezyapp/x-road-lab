# 18 — Logs and Evidence

**Status: Verified 2026-10-25** (message-log query, request-id correlation across both
Security Servers, and an error `detail` UUID found in `proxy.log`).

**Goal:** find a call in the message log on both Security Servers, correlate it with
`X-Road-Request-Id`, and locate a failed call's `detail` UUID.

**Time:** 10 minutes.

## Three identifiers, three jobs

| Value | Where it comes from | What it identifies |
| --- | --- | --- |
| `X-Road-Id` | response header | one message (request or response) |
| `X-Road-Request-Id` | response header | the whole request/response pair, **same on both SSes** |
| `detail` (UUID in an error body) | error response | one failure, for the SS logs |

`X-Road-Request-Id` is the one you use to join the two Security Servers.

## 1. Make a call and capture the ID

```bash
curl -i -sS \
  "http://localhost:4210/r1/DEV/GOV/MOC/REGISTRY/businessLookup/businesses" \
  -H "X-Road-Client: DEV/BIZ/SME-CU/PORTAL" \
  | grep -iE 'x-road-(id|request-id|service)'
```

```text
x-road-id: DEV-<uuid>
x-road-request-id: <request-uuid>
x-road-service: DEV/GOV/MOC/REGISTRY/businessLookup
```

## 2. Read the message log on the consumer SS

The message log is a PostgreSQL database inside each Security Server:
`messagelog` → schema `messagelog` → table `logrecord`.

```bash
docker exec -u postgres ss-smecu psql -d messagelog -c "
SELECT id, xrequestid, memberclass, membercode, subsystemcode,
       response, archived, to_timestamp(time/1000) AS at
FROM messagelog.logrecord
WHERE discriminator = 'm'
ORDER BY id DESC
LIMIT 4;"
```

```text
 id |              xrequestid              | memberclass | membercode | subsystemcode | response | archived |           at
----+--------------------------------------+-------------+------------+---------------+----------+----------+------------------------
 70 | 2bd1c64e-...                         | BIZ         | SME-CU     | PORTAL        | f        | f        | 2026-10-25 10:34:56+00
 68 | c41b3494-...                         | BIZ         | SME-CU     | PORTAL        | t        | f        | 2026-10-25 10:33:20+00
 67 | c41b3494-...                         | BIZ         | SME-CU     | PORTAL        | f        | f        | 2026-10-25 10:33:20+00
```

Two rows per exchange: `response = f` is the request, `response = t` is the response.
They share `xrequestid` and have different `id` values.

Now query by the header value:

```bash
docker exec -u postgres ss-smecu psql -d messagelog -c "
SELECT id, xrequestid, response, membercode, subsystemcode
FROM messagelog.logrecord
WHERE xrequestid = '<request-uuid>'
ORDER BY id;"
```

## 3. Do the same on the provider SS

```bash
docker exec -u postgres ss-moc psql -d messagelog -c "
SELECT id, xrequestid, response, membercode, subsystemcode
FROM messagelog.logrecord
WHERE xrequestid = '<request-uuid>'
ORDER BY id;"
```

**Expected:** the same `xrequestid`, two more rows, with `membercode = MOC` /
`subsystemcode = REGISTRY`. That is the proof that this one call was recorded on both
sides of the trust boundary.

## 4. Find a failed call

Reproduce a controlled failure (step 17, case A):

```bash
curl -sS \
  "http://localhost:4210/r1/DEV/GOV/MOC/REGISTRY/businessLookup/businesses" \
  -H "X-Road-Client: DEV/BIZ/SME-CU/NOPE"
```

```json
{"type":"server.clientproxy.unknown_member","message":"...","detail":"<detail-uuid>"}
```

Search the SS that produced the error (`clientproxy` → consumer, `serverproxy` →
provider):

```bash
docker exec ss-smecu sh -c 'grep -rn "<detail-uuid>" /var/log/xroad/ | head'
```

**Expected** on the consumer:

```text
/var/log/xroad/proxy.log:... ERROR ... Request processing error (<detail-uuid>)
```

Rejections that happen before message logging (unknown client, ACL denial) are **not** in
the `messagelog` table — the `detail` UUID plus `proxy.log` is the evidence for those.

## 5. The other logs

| File (inside the SS) | Use |
| --- | --- |
| `clientproxy_access.log` | one line per request arriving from an information system |
| `serverproxy_access.log` | one line per request forwarded to the provider app |
| `proxy.log` | errors, including the `detail` UUID |
| `signer.log` | token, key, certificate, OCSP, signing errors |
| `configuration_client.log` | global-configuration download problems |
| `messagelog-archiver.log` | archiving of old records |

```bash
docker exec ss-smecu sh -c 'tail -5 /var/log/xroad/clientproxy_access.log'
```

## 6. Retention and archiving

Records stay in the table until the archiver moves them: `keep-records-for=30` days,
archive path `/var/lib/xroad/`. A row with `archived = t` is no longer in the online
table; the evidence is in the archive file.

```bash
docker exec ss-smecu sh -c 'ls -lh /var/lib/xroad/ | head'
```

## If it fails

| Symptom | Meaning | Fix |
| --- | --- | --- |
| `psql: FATAL: role "root" does not exist` | You connected as the container's default user. | Use `docker exec -u postgres ...`. |
| `relation "logrecord" does not exist` | You are in the wrong schema. | Qualify it: `messagelog.logrecord`. |
| Query returns no rows | The call was rejected before logging, or the ID is from another request. | Try the newest rows; check `proxy.log` for the `detail`. |
| `detail` UUID not found in logs | It may be on the other SS. | `clientproxy` → consumer, `serverproxy` → provider. |
| Rows show `archived = t` | The record was archived. | Look in `/var/lib/xroad/` archive files. |

## Why this step exists

X-Road's value is not just routing; it is **evidence**. The message log records who called
what, when, with which hash, signed and timestamped. When a partner says "we sent it and
you rejected it", the `X-Road-Request-Id` and the two message-log rows settle it.

The `detail` UUID is the fast path: paste it into the SS logs instead of guessing from the
timestamp.

Next: [19-reset.md](19-reset.md)
