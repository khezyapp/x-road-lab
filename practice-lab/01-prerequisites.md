# 01 — Prerequisites

**Goal:** prove the host can run the lab before you change any X-Road state.
**Time:** 2 minutes (plus image pulls on a first run).

## Run

```bash
cd /home/khezy/Documents/learning/x-road-lab

docker version --format '{{.Server.Version}}'
docker compose version
java -version 2>&1 | head -1

ls tmp/xroad-7.8.3/Docker/xrd-dev-stack/compose.yaml
```

Check the ports you will need:

```bash
for p in 4100 4200 4210 4300 4310 4400 4710 4711; do
  if ss -ltn "sport = :$p" | grep -q LISTEN; then echo "$p BUSY"; else echo "$p free"; fi
done
```

Check the clock (X-Road certificates are time-sensitive):

```bash
timedatectl | grep -E 'System clock|synchronized'
```

## Expected

```text
29.x            (Docker server version — 20.10+ is fine)
Docker Compose version v2.x
openjdk version "25..." or Oracle GraalVM 25...
tmp/xroad-7.8.3/Docker/xrd-dev-stack/compose.yaml
4100 free
4200 free
4210 free
4300 free
4310 free
4400 free
4710 free
4711 free
System clock synchronized: yes
```

If you already ran the demo guide, some containers may still be using these ports.
Stop them first:

```bash
docker ps --format '{{.Names}}\t{{.Ports}}' | grep -E '^(cs|ss0|ss1|testca|isrest|isopenapi|issoap)\b'
docker compose -f tmp/xroad-7.8.3/Docker/xrd-dev-stack/compose.yaml \
               -f tmp/xroad-7.8.3/Docker/xrd-dev-stack/compose.dev.yaml \
               --env-file tmp/xroad-7.8.3/Docker/xrd-dev-stack/.env stop
```

## If it fails

| Symptom | Meaning | Fix |
| --- | --- | --- |
| `Cannot connect to the Docker daemon` | Docker is not running or your user is not in the `docker` group. | `sudo systemctl start docker`; or run `sudo usermod -aG docker $USER` and log in again. |
| `docker compose` not found | Compose v1 or no plugin. | Install the Compose v2 plugin; this lab uses `docker compose`, not `docker-compose`. |
| `java -version` shows 21 or lower | The services target Java 25. | Install JDK 25 or set `JAVA_HOME`. Images still build fine; host `./gradlew` will not. |
| A port is `BUSY` | Something else owns it. | Stop the demo stack, or `ss -ltnp "sport = :<port>"` to find the process. |
| `tmp/xroad-7.8.3/...` missing | The tag was not extracted into the repo. | Re-extract tag `7.8.3` as described in [docs/guide/02-get-source.md](../docs/guide/02-get-source.md). |
| Clock not synchronized | TLS, OCSP and timestamps will fail later with confusing errors. | `sudo timedatectl set-ntp true`; wait for `synchronized: yes`. |

## Why this step exists

Each later failure mode has a cheap check here. A busy port looks like "the Security
Server will not start". A skewed clock looks like "the certificate is invalid". Running
these first means every later error is about X-Road, not the host.

## Notes

- Images are pinned by digest in `infra/v7/.env`. The first `up` may still need to pull
  `axllent/mailpit` and the base image for the Spring Boot services.
- 4100–4400 and 4700s are host ports only. Inside the Docker network every service uses
  its internal port, and Security Servers talk over 5500.
- Do not start X-Road 8 images on these ports. See
  [docs/x-road/05-versions-and-camdx.md](../docs/x-road/05-versions-and-camdx.md).

Next: [02-own-the-stack.md](02-own-the-stack.md)
