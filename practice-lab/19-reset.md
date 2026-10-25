# 19 — Reset

**Status: Verified 2026-10-25** — executed on this host: containers, `xrd-lab_ca-volume`, and
`xroad-network` are gone. The optional image and `ca-certs/` cleanup was not performed.

**Goal:** wipe containers, volumes, and CA state, then rebuild from step 03 without
guessing.

> **This deletes the whole lab.** Every member, certificate, key, service description,
> ACL, and message-log record goes away. That is the point: the lab is disposable.

## What dies and what survives

| Dies | Survives |
| --- | --- |
| CS database: instance, members, member classes, CA/OCSP/TSA config, signing keys, management provider | `tmp/xroad-7.8.3/` (the extracted upstream source) |
| Both SS databases: clients, keys, certificates, token state, TSA selection, service descriptions, ACLs | `infra/v7/` files (compose, `.env`) |
| `xrd-lab_ca-volume`: CA key/cert, OCSP and TSA certificates | `member-services/` source, `build/`, built jars |
| `xroad-network` | Docker images, unless you remove them |
| Member-service containers (in-memory applications) | `docs/`, `practice-lab/` |
| Files you downloaded: `anchor.xml`, CSRs, signed certs, `ca-certs/` (manual) | `~/Downloads` files, unless you delete them |

Only **one** thing lives in a named volume: `ca-volume`. The CS and SS databases live
inside their containers, so `down` alone already destroys them. `-v` exists to delete the
CA as well.

## 1. Stop the member services first

```bash
cd /home/khezy/Documents/learning/x-road-lab
docker compose -f member-services/docker-compose.yml down
```

If you skip this, the infra `down -v` cannot remove `xroad-network` because the member
containers are still attached to it.

## 2. Reset the X-Road stack

```bash
docker compose -f infra/v7/compose.yaml -f infra/v7/compose.dev.yaml \
               --env-file infra/v7/.env down -v
```

**Expected:** containers, the network, and `xrd-lab_ca-volume` removed.

## 3. Optional: remove the built images and local certificate copies

```bash
docker rmi xroad-lab/moc-registry:0.1.0 xroad-lab/sme-cu-portal:0.1.0
rm -rf ca-certs
```

Also delete the anchor, CSRs, and signed certificates you saved in `~/Downloads`; they
belong to the old CA and will be rejected by the new one.

## 4. Verify the wipe

```bash
docker ps -a --format '{{.Names}}' | grep -E '^(cs|ss-smecu|ss-moc|testca|moc-registry|sme-cu-portal)$' || echo "no lab containers"
docker volume ls --format '{{.Name}}' | grep xrd-lab || echo "no lab volumes"
docker network ls --format '{{.Name}}' | grep -x xroad-network || echo "no lab network"
```

**Expected**

```text
no lab containers
no lab volumes
no lab network
```

## 5. Rebuild

```bash
./practice-lab/scripts/up.sh cs testca
```

Then follow [03-start-cs.md](03-start-cs.md) onward. A few things are new again:

- the **test CA is regenerated**, so `scripts/ca-certs.sh` returns different certificates;
- **signing keys must be recreated** (step 04) — they lived in the CS container;
- the **management service provider** must be set again (step 05);
- both SSes must be re-initialized (steps 08–09), including **activating** the auth
  certificates and **selecting the TSA**;
- the management WSDL must be re-imported and its **service URL overridden** (08b).

## The half-reset trap

`docker compose down` **without** `-v` removes the containers and their databases but
keeps `xrd-lab_ca-volume`. On the next start you have a fresh CS and a CA that still
exists from the old run.

That is not a clean state: the CA is unconfigured in the new CS, but the certificates you
already downloaded are still from the same CA. It looks "almost right" and fails in
confusing ways.

Use `down -v` unless you deliberately want to keep the CA.

## If it fails

| Symptom | Meaning | Fix |
| --- | --- | --- |
| `network xroad-network has active endpoints` | Member-service containers are still attached. | `docker compose -f member-services/docker-compose.yml down` first. |
| A volume is "in use" | A container is still running. | `docker ps`; remove it, then `down -v` again. |
| Containers come back after the reset | Something else is running Compose for this project. | Check `docker ps -a` and `docker compose ls`. |
| Ports still busy after the reset | A non-Compose process owns them. | `ss -ltnp "sport = :4100"` (and the other ports). |
| The new CA does not match your old certs | You kept the old downloads. | Re-run `scripts/ca-certs.sh` after starting `testca`. |

## Why this step exists

A lab you cannot reset is a lab you are afraid to touch. Resetting is the fastest way back
to a known state, and doing it deliberately — in the right order — is part of the skill.

It also makes the cost of each step explicit: everything above is recreated by steps
03–18, which is exactly why those steps are worth automating later with a script of your
own.

This is the end of the practice lab. The remaining work is turning the manual sequence
into a repeatable setup script, and then building the three-organization topology in
`docs/x-road/06-lab-topology.md`.
