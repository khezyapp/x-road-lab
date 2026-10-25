# 02 — Own the Stack

**Goal:** understand exactly what was copied from upstream, what was changed, and why.
**Time:** 10 minutes; no containers need to start.

## What exists

```bash
cd /home/khezy/Documents/learning/x-road-lab

find infra/v7 -maxdepth 2 -type f | sort
```

```text
infra/v7/.env                                  # image pins, project name
infra/v7/.env.example                          # same, safe to commit
infra/v7/compose.yaml                          # base: images, healthchecks, volumes
infra/v7/compose.dev.yaml                      # overlay: container names, host ports
infra/v7/ss-moc/init-token-and-run-entrypoint.sh
infra/v7/testmail/...                          # mailpit fixtures (kept)
infra/v7/wiremock_mappings/...                 # isrest stubs (kept)
```

`tmp/xroad-7.8.3/` is the extracted upstream tag. Treat it as read-only reference; the
lab runs from `infra/v7/`.

> **Image version check:** the published `xrddev-*:latest` images currently ship
> **7.8.1**, not 7.8.3 (`docker exec cs dpkg-query -W -f '${Version}\n' xroad-centralserver`).
> The source tree and the manuals are 7.8.3. This is expected; the mechanics used in this
> lab are the same. Record the version you actually run.

## What was changed from upstream

See it yourself:

```bash
diff -u tmp/xroad-7.8.3/Docker/xrd-dev-stack/compose.yaml infra/v7/compose.yaml
```

| Change | Why |
| --- | --- |
| `ss0` → `ss-smecu`, `ss1` → `ss-moc` | The container name is part of how you reason about the lab. `ss0` hides which organization it belongs to. |
| `hurl` service removed | Its build context is `../../development/hurl`, which does not exist under `infra/`. It also exists to run `setup.hurl`, which creates the demo members we are replacing. |
| `local-dev-run.sh` not copied | Same reason: no `--initialize` in this lab. |
| `mailpit` and `isrest` images moved into `.env` | So every image, including the demo ones, is pinned in one place. |
| `name: xrd-lab` added | Deterministic project and volume names, whatever directory you run from. |
| demo IS containers kept | They are useful to compare against a known-good provider. They have no members in this Central Server, so they are inert until registered. |

The `ss-moc` entrypoint script is the upstream `ss1` script, unchanged. It initializes
the SoftHSM token on every start, which is why `ss-moc` needs `XROAD_TOKEN_0_PIN` and
`XROAD_TOKEN_31_PIN`.

## Verify the configuration

```bash
cd /home/khezy/Documents/learning/x-road-lab
docker compose -f infra/v7/compose.yaml -f infra/v7/compose.dev.yaml \
               --env-file infra/v7/.env config --services
```

**Expected**

```text
cs
issoap
isopenapi
isrest
mailpit
ss-moc
ss-smecu
testca
```

And a full validation:

```bash
docker compose -f infra/v7/compose.yaml -f infra/v7/compose.dev.yaml \
               --env-file infra/v7/.env config >/dev/null && echo "config OK"
```

**Expected:** `config OK`.

## How the two compose files fit together

- `compose.yaml` is the base: images, memory limits, healthchecks, the `ca-volume`.
- `compose.dev.yaml` is an overlay: container names, host port mappings, the `ss-moc`
  entrypoint mount, and the `xroad-network` definition.

Both must always be passed, in that order:

```bash
docker compose -f compose.yaml -f compose.dev.yaml --env-file .env <command>
```

That is what `practice-lab/scripts/up.sh` does for you.

## Starting a subset

Steps 03 and 07 bring the stack up in two phases on purpose:

```bash
cd /home/khezy/Documents/learning/x-road-lab

./practice-lab/scripts/up.sh cs testca     # step 03
./practice-lab/scripts/up.sh ss-smecu ss-moc   # step 07
```

With no arguments it starts everything, including the demo information systems:

```bash
./practice-lab/scripts/up.sh
```

## Stopping and wiping

```bash
cd /home/khezy/Documents/learning/x-road-lab

# stop, keep state
docker compose -f infra/v7/compose.yaml -f infra/v7/compose.dev.yaml --env-file infra/v7/.env stop

# remove containers, keep volumes (Central Server database, CA keys)
docker compose -f infra/v7/compose.yaml -f infra/v7/compose.dev.yaml --env-file infra/v7/.env down

# remove everything, including the test CA and the CS database
docker compose -f infra/v7/compose.yaml -f infra/v7/compose.dev.yaml --env-file infra/v7/.env down -v
```

`down -v` is a full reset: every member, certificate and registration is gone. That is
expected in a lab, but do not run it in the middle of a debugging session. The full
procedure is in [19-reset.md](19-reset.md).

## If it fails

| Symptom | Meaning | Fix |
| --- | --- | --- |
| `env file .../.env not found` | You ran compose without `--env-file`, from the wrong directory. | `cd infra/v7` or use the absolute paths above. |
| `service "ss0" has neither an image nor a build context` | You are using the upstream compose file, not `infra/v7`. | Check the `-f` paths. |
| `invalid reference format` on an image line | `.env` was edited or the digest is malformed. | Restore from `.env.example`. |
| `no such network: xroad-network` | Compose tried to start before the network existed. | Start `cs` first; it creates the network. |

## Why this step exists

The demo stack was something you ran. This is a stack you own: its names, its pins, its
reset procedure. Every later step edits state inside these containers, so knowing which
file controls which container saves hours when something looks wrong.

Next: [03-start-cs.md](03-start-cs.md)
