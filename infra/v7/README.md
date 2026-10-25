# infra/v7 — X-Road 7.8.3 lab stack

A lab-owned copy of the upstream `Docker/xrd-dev-stack` at tag `7.8.3`.
The step-by-step guide is [`practice-lab/`](../../practice-lab/README.md).

## Commands

```bash
# start everything
docker compose -f compose.yaml -f compose.dev.yaml --env-file .env up -d

# start a subset (the guide starts cs+testca first, then the Security Servers)
docker compose -f compose.yaml -f compose.dev.yaml --env-file .env up -d cs testca

# status
docker compose -f compose.yaml -f compose.dev.yaml --env-file .env ps

# reset (destroys members, certificates, and the CS database)
docker compose -f compose.yaml -f compose.dev.yaml --env-file .env down -v
```

Both `-f` files are required. `--env-file .env` is required.

## Services

| Service | Internal | Host | Purpose |
| --- | --- | --- | --- |
| `cs` | 4000 | 4100 | Central Server UI/API |
| `ss-smecu` | 4000 / 8080 | 4200 / 4210 | SME-CU Security Server |
| `ss-moc` | 4000 / 8080 | 4300 / 4310 | MOC Security Server |
| `testca` | 8888 | 4400 | Test CA, OCSP, TSA, CSR signing form |
| `isopenapi` | 8080 | 4700 | Demo OpenAPI provider (kept, unregistered) |
| `isrest` | 8080 | 4500 | Demo WireMock provider (kept, unregistered) |
| `issoap` | 8080 | 4600 | Demo SOAP adapter (kept, unregistered) |
| `mailpit` | 8025 / 1025 | 8025 / 1025 | Mail sink (unused by the lab) |

The lab's Spring Boot services join this stack's `xroad-network` and publish
4710 (`sme-cu-portal`) and 4711 (`moc-registry`).

## Do not

- Do not run `local-dev-run.sh --initialize`. It is not in this directory on purpose; it
  creates the upstream demo members (`DEV/COM/1234`, `DEV/COM/4321`) instead of the lab
  members.
- Do not edit `tmp/xroad-7.8.3/`; it is the reference copy.
- Do not mix X-Road 8 images into this compose file.

## Images

Pinned by digest in `.env` (see `.env.example`). Re-resolve a digest with:

```bash
docker inspect --format '{{index .RepoDigests 0}}' <image>
```
