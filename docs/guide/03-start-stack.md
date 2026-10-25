# 03 — Start the Containers

**Goal:** pull the images, build the Hurl helper, and start the stack **without** initializing X-Road yet.
**Time:** 5–15 minutes on the first run (image download); seconds afterward.

Starting first and initializing second lets you see which layer failed.

## Run

```bash
cd /tmp/xroad-7.8.3/Docker/xrd-dev-stack
./local-dev-run.sh
```

What this does:

- starts the services in `compose.yaml` + `compose.dev.yaml`;
- builds the `hurl` image from `../../development/hurl`;
- starts the containers in the background.

Check status:

```bash
cd /tmp/xroad-7.8.3/Docker/xrd-dev-stack
docker compose -f compose.yaml -f compose.dev.yaml --env-file .env ps --all
```

## Expected

You should see containers named:

```text
cs
ss0
ss1
testca
issoap
isopenapi
isrest
mailpit
```

`hurl` may appear as `exited (0)`; it is a one-shot helper, not a long-running service. The X-Road containers may show `starting` for a while, then `healthy`. The important ones (`cs`, `ss0`, `ss1`) must become healthy before initialization.

A healthy CS/SS exposes HTTPS on port 4000 inside the container; the dev overrides publish:

| Container | Host UI | Host proxy |
| --- | --- | --- |
| `cs` | `https://localhost:4100` | — |
| `ss0` | `https://localhost:4200` | `http://localhost:4210` |
| `ss1` | `https://localhost:4300` | `http://localhost:4310` |

## If it fails

| Symptom | Meaning | Fix |
| --- | --- | --- |
| `port is already allocated` | Another process/container uses 4100, 4200, 4210, 4300, 4310, 4400, 4500, 4600, 4700, or 8025. | Stop the conflicting process or edit `compose.dev.yaml` host ports. |
| `pull access denied` / `manifest unknown` | The image name/tag is unavailable or the registry is blocked. | Check network/proxy access to `ghcr.io` and Docker Hub; retry. |
| Containers restart in a loop | The image cannot start on this host, or a dependency is unhealthy. | `docker compose ... logs <service>` and read the first error, not the last restart. |
| `hurl` build fails | The build context `../../development/hurl` is missing. | You extracted only part of the tag. Re-extract the whole `7.8.3` tree. |
| `docker compose` warns about `version` | Your Compose version ignores the obsolete `version:` key. | Warning only; continue if the stack starts. |
| Everything is `created` but not running | Compose was interrupted. | `docker compose ... up -d` and check logs. |

Useful checks:

```bash
docker compose -f compose.yaml -f compose.dev.yaml --env-file .env logs --tail=100 cs
docker compose -f compose.yaml -f compose.dev.yaml --env-file .env logs --tail=100 ss0
```

## Why this step exists

The X-Road initialization is a chain: CS healthy → SS healthy → certificates issued → global configuration downloaded. If you initialize immediately and it fails, you cannot tell whether the problem is a container, a port, a certificate, or an X-Road configuration step.

Starting first gives you a clean baseline: **containers up, X-Road not yet initialized**.

The stack uses test credentials and a test CA. It is not production software.

## Next

[04-initialize.md](04-initialize.md)
