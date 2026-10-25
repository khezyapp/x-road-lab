# 02 — Get X-Road 7.8.3 Source

**Goal:** extract the **v7** tag into a scratch directory without touching the `develop` (v8) working tree.
**Time:** 1–3 minutes.

## Why not use the existing clone directly?

The clone at `/mnt/data/opensource/x-roads/X-Road` is on `develop` = **8.0.0-beta2**. The dev stack for v7 lives at `Docker/xrd-dev-stack` on tag `7.8.3`, not in the v8 tree. Mixing them is the fastest way to get a confusing failure.

Extract the tag to `/tmp`; leave the upstream clone untouched.

## Run

```bash
git -C /mnt/data/opensource/x-roads/X-Road tag -l 7.8.3
```

Expected: it prints `7.8.3`.

Then:

```bash
rm -rf /tmp/xroad-7.8.3
mkdir -p /tmp/xroad-7.8.3
git -C /mnt/data/opensource/x-roads/X-Road archive 7.8.3 | tar -x -C /tmp/xroad-7.8.3
```

Verify the version and the stack files:

```bash
grep '^xroadVersion=' /tmp/xroad-7.8.3/src/gradle.properties
ls /tmp/xroad-7.8.3/Docker/xrd-dev-stack
```

## Expected

```text
xroadVersion=7.8.3
```

The directory listing should include:

```text
.env
compose.yaml
compose.dev.yaml
local-dev-run.sh
local-dev-prepare.sh
ss1/
testmail/
wiremock_mappings/
```

You can also check:

```bash
du -sh /tmp/xroad-7.8.3
```

Expected: roughly 180 MB.

## If it fails

| Symptom | Meaning | Fix |
| --- | --- | --- |
| `tag '7.8.3' not found` | The clone has no tags, or only a shallow fetch. | Do not modify the upstream clone. Clone a separate copy with tags, or download the GitHub tag archive. |
| `path ... does not exist` | You used `develop` or only extracted part of the tag. | Re-run the `git archive` command against the whole tag. |
| `xroadVersion=8...` | You extracted the wrong tree. | Delete `/tmp/xroad-7.8.3` and extract tag `7.8.3` again. |
| `No such file or directory: Docker/xrd-dev-stack` | The tag is not 7.8.x, or the extract is incomplete. | Confirm `git tag -l`; confirm `Docker/xrd-dev-stack` exists in the extracted tree. |
| `tar: This does not look like a tar archive` | The `git archive` output was empty because the ref was wrong. | Run `git -C ... tag -l 7.8.3` first. |

## Why this step exists

In X-Road, **the version is part of the behavior**. A v8 README, v8 compose file, or v8 manual will send you down a different path (OpenBao, gRPC, split containers). Pinning the tag makes every later command reproducible.

The extracted copy is safe to delete. The original clone remains read-only reference material.

## Next

[03-start-stack.md](03-start-stack.md)
