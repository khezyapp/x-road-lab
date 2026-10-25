# 07 — Start the Security Servers

**Status: Verified 2026-10-25** (both containers healthy, both confirmed uninitialized
through the SS admin API).

**Goal:** start `ss-smecu` and `ss-moc` and confirm they are healthy and uninitialized —
no configuration anchor, no clients.

**Time:** 1–2 minutes on the first start.

## Run

The Central Server and test CA must already be running:

```bash
cd /home/khezy/Documents/learning/x-road-lab
./practice-lab/scripts/up.sh ss-smecu ss-moc
```

This starts only the two Security Servers. They join the `xroad-network` created by the
previous step and reach the CS as `cs:4000`.

## Expected

```bash
cd infra/v7
docker compose -f compose.yaml -f compose.dev.yaml --env-file .env ps --format '{{.Name}}\t{{.Status}}'
```

```text
cs        Up ... (healthy)
ss-moc    Up ... (healthy)
ss-smecu  Up ... (healthy)
testca    Up ... (healthy)
```

On the first start they show `(health: starting)` for up to a minute, then `(healthy)`.
`ss-moc` uses a custom entrypoint that initializes its SoftHSM token before starting the
SS services, so it can take a few seconds longer.

Both UIs:

- `https://localhost:4200` — `ss-smecu`
- `https://localhost:4300` — `ss-moc`

Log in with `xrd` / `secret`. An uninitialized Security Server lands on the
initialization view, whose first section is **Configuration Anchor**
(*"Import the configuration anchor provided by the Central Server's administrator."*).

**Do not upload an anchor yet.** That is step 08.

## Prove they are uninitialized

```bash
cd /tmp
for h in 4200 4300; do
  rm -f ss$h.cookies
  curl -sk -c ss$h.cookies -X POST https://localhost:$h/login \
       -d 'username=xrd&password=secret' -o /dev/null
  X=$(awk '$6=="XSRF-TOKEN"{print $7}' ss$h.cookies)
  echo "== $h =="
  curl -sk -b ss$h.cookies -H "X-XSRF-TOKEN: $X" \
       https://localhost:$h/api/v1/tokens \
    | python3 -c 'import json,sys; t=json.load(sys.stdin)[0]; print("keys:", t["keys"], "logged_in:", t["logged_in"])'
  echo -n "anchor: "
  curl -sk -b ss$h.cookies -H "X-XSRF-TOKEN: $X" -o /dev/null -w '%{http_code}\n' \
       https://localhost:$h/api/v1/configuration-anchor
done
```

**Expected**

```text
== 4200 ==
keys: [] logged_in: false
anchor: 404
== 4300 ==
keys: [] logged_in: false
anchor: 404
```

Three facts in one output: the software token exists, it holds no keys, and no anchor has
been imported.

A fresh SS also answers `GET /api/v1/clients` with a **500**
`core.malformed_server_conf` — *"Server conf is not initialized!"*. That is expected at
this stage, not a broken server. After step 08 the same call returns an empty list.

## If it fails

| Symptom | Meaning | Fix |
| --- | --- | --- |
| `no such network: xroad-network` | The CS was never started, so the network does not exist. | Run step 03 first, or `./practice-lab/scripts/up.sh cs testca`. |
| `port is already allocated` | A demo stack still owns 4200/4210/4300/4310. | `docker ps`; stop the old containers. |
| `ss-smecu` is `(unhealthy)` after 3 minutes | The SS cannot start its services. | `docker logs --tail=100 ss-smecu`; check `proxy`, `signer`, `confclient`. |
| `ss-moc` restarts in a loop | The SoftHSM entrypoint failed, usually a permission or token-init problem. | `docker logs ss-moc`; the first error, not the last restart. |
| UI loads but login fails | The SS database is from another run with a different admin password. | `docker compose ... down` (state is not in a volume) and start again. |
| You see `ss0` / `ss1` in `docker ps` | You are running the upstream compose file, not `infra/v7`. | Check the `-f` paths. |

## Before you continue

Neither Security Server has persistent volumes. `docker compose down` (with or without
`-v`) deletes their databases, keys, and initialization. If you stop for the day, use
`stop`, not `down`, or accept that step 08 starts over.

## Why this step exists

Starting the Security Servers before initializing them separates two failure classes:
"the container does not run" and "the X-Road configuration is wrong". Step 08 changes
X-Road state; if it fails, you already know the containers are healthy.

It also establishes the baseline: two servers that exist, can log in, and trust nothing
yet — no anchor, no keys, no clients.

Next: [08-init-ss-smecu.md](08-init-ss-smecu.md)
