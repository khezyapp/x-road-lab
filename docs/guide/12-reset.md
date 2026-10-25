# 12 — Reset the Lab

**Goal:** return to a clean state and prove nothing is left running.
**Time:** 1 minute.

## Stop and remove everything

```bash
cd /tmp/xroad-7.8.3/Docker/xrd-dev-stack
docker compose -f compose.yaml -f compose.dev.yaml --env-file .env down --remove-orphans -v
```

`-v` removes the Compose-managed CA volume. The `testmail/` and `wiremock_mappings/` directories are bind mounts in the source tree and are not deleted.

Verify:

```bash
docker ps -a --format '{{.Names}}\t{{.Status}}' | grep -E '^(cs|ss0|ss1|testca|issoap|isopenapi|isrest|mailpit|hurl)\b' || echo "no lab containers"
docker volume ls | grep -i xrd || echo "no lab volumes"
```

Expected:

```text
no lab containers
no lab volumes
```

## If it fails

| Symptom | Meaning | Fix |
| --- | --- | --- |
| Port still in use after `down` | Another container/process owns the port. | `docker ps`; stop the other container or change ports. |
| `network xroad-network has active endpoints` | A container was not removed. | `docker compose ... down --remove-orphans`; then `docker network ls`. |
| Volume remains | It is not managed by this Compose project, or removal was skipped. | `docker volume ls`; remove only the lab volume by name. |
| `no lab containers` but UI still loads | Browser cache or another stack on the same ports. | Check `docker ps` and the actual port owner (`ss -ltnp`). |

## Remove the source extract (optional)

```bash
rm -rf /tmp/xroad-7.8.3
```

You can always re-extract it with step 02. Do not delete the upstream clone.

## Why this step exists

X-Road state is spread across containers, databases, certificates, and global configuration. A partial reset creates "ghost" state: an old certificate, a stale registration, or a service still pointing at a removed container. A full `down -v` is the only reliable way to get back to a known baseline.

Learning is faster when reset is cheap. Do not be afraid to reset instead of debugging a half-initialized environment.

## Next

[13-next-steps.md](13-next-steps.md)
