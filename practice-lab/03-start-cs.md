# 03 — Start the Central Server

**Status: Verified 2026-10-25** (containers healthy, UI reachable, CS confirmed uninitialized).

**Goal:** start only the Central Server and the test CA, and confirm both are healthy before any state exists.

**Time:** 1 minute after the first pull.

## Run

```bash
cd /home/khezy/Documents/learning/x-road-lab
./practice-lab/scripts/up.sh cs testca
```

First start takes 30–60 seconds. Check:

```bash
cd infra/v7
docker compose -f compose.yaml -f compose.dev.yaml --env-file .env ps
```

## Expected

```text
NAME      ...   STATUS
cs        ...   Up ... (healthy)
testca    ...   Up ... (healthy)
```

Only these two. `ss-smecu` and `ss-moc` must **not** be running yet — they come in step 07.

Check the two endpoints:

```bash
curl -sk -o /dev/null -w 'CS UI: %{http_code}\n' https://localhost:4100/
curl -s -o /dev/null -w 'Test CA form: %{http_code}\n' http://localhost:4400/testca/sign
```

**Expected**

```text
CS UI: 200
Test CA form: 200
```

Then in a browser:

- **`https://localhost:4100`** — accept the self-signed certificate warning. The login form appears.
- **`http://localhost:4400/testca/sign`** — a page titled **Test CA: CSR signing**. You will use it in steps 08 and 09. (`http://localhost:4400/` returns 403 by design; the form lives under `/testca/`.)

Do **not** initialize the Central Server yet.

## Prove it from the API (optional)

The UI is a client of the same management API. This is the machine version of "the CS exists but has no configuration":

```bash
cd /tmp && rm -f cs.cookies
curl -sk -c cs.cookies -X POST https://localhost:4100/login \
     -d 'username=xrd&password=secret'
XSRF=$(awk '$6=="XSRF-TOKEN"{print $7}' cs.cookies)
curl -sk -b cs.cookies -H "X-XSRF-TOKEN: $XSRF" \
     https://localhost:4100/api/v1/system/status
```

**Expected** — `software_token_init_status` is `NOT_INITIALIZED` and the identifiers are empty:

```json
{"high_availability_status":{"is_ha_configured":false,"node_name":"node_0"},
 "initialization_status":{"instance_identifier":"",
 "central_server_address":"","software_token_init_status":"NOT_INITIALIZED"}}
```

After step 04 the same call returns `"instance_identifier":"DEV"` and `INITIALIZED`. That contrast is the cheapest proof that step 04 really happened.

## What version is actually running?

```bash
docker exec cs dpkg-query -W -f '${Package} ${Version}\n' xroad-centralserver
```

**Expected on this host:** `xroad-centralserver 7.8.1-0.ubuntu22.04`.

Record what you get. The upstream *source* tree is 7.8.3, but the published
`ghcr.io/nordic-institute/xrddev-central-server:latest` image currently ships **7.8.1**.
The commissioning mechanics are the same; the difference matters only if a screen or
API call behaves differently from the 7.8.3 docs. Note it, don't fight it.

## If it fails

| Symptom | Meaning | Fix |
| --- | --- | --- |
| `port is already allocated` | Another stack owns 4100 or 4400. | Check `docker ps`; stop the demo stack or the old containers. |
| `cs` stays `starting` for more than 2 minutes | The container is unhealthy. | `docker logs --tail=100 cs`; read the first error, not the last. |
| `CS UI: 000` | Nothing is listening on 4100. | `docker compose ... ps`; the container is not running. |
| Browser shows `NET::ERR_CERT_AUTHORITY_INVALID` | Expected: the CS uses a self-signed certificate. | Accept the warning. Only on localhost. |
| `testca` form does not load | Port 4400 is mapped to `testca:8888`. | `docker logs testca`; check the nginx process inside. |
| `login` returns 401 | The demo stack's credentials are different, or the CS database is from another run. | Use `xrd` / `secret`; if it still fails, reset with step 19. |

## Why this step exists

Two of the three trust anchors of the lab (the CS and the CA) must be healthy before
anything else. Starting the Security Servers at the same time would hide which layer
failed: a crashed CS and a crashed SS look identical from a failing API call.

Next: [04-init-cs.md](04-init-cs.md)
