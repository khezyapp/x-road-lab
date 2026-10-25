# 13 — Next Steps: From Demo to the SME-CU Lab

**Goal:** turn the working demo into the three-organization lab without losing the lessons.
**Time:** planning; implementation is the next project.

## What you should be able to explain now

Before building the lab, check that you can answer these without notes:

- [ ] Difference between a member, a subsystem, and a service.
- [ ] Why `X-Road-Client` is the consumer subsystem, not the provider.
- [ ] Difference between a service description URL and a service URL.
- [ ] Why a registered client and an enabled service are not enough; the ACL must also allow the call.
- [ ] Why `X-Road-Error` is absent for a provider application error.
- [ ] Why the provider SS, not the app, creates the evidence headers.
- [ ] Why global configuration propagation can make a correct change appear broken.

If any answer is unclear, revisit [01-foundations.md](../x-road/01-foundations.md), [02-identifiers.md](../x-road/02-identifiers.md), or [04-registration-and-access.md](../x-road/04-registration-and-access.md).

## Current implementation

The first REST consumer/provider pair is implemented in **`member-services/`**:

- `moc-registry` — provider on `8081` (`GET /api/businesses`, `GET /api/businesses/{id}`)
- `sme-cu-portal` — consumer on `8080` (`GET /api/portal/businesses`, `GET /api/portal/businesses/{id}`)

Direct mode works, and **both X-Road directions are now registered and verified** on this
host (2026-10-25):

- SME-CU → MOC `businessLookup` returns `200` through `ss-smecu` and `ss-moc`;
- MOC → SME-CU `applicationSubmit` returns `201` through the same pair.

See [13-member-services.md](../x-road/13-member-services.md) for the endpoints, and
`practice-lab/` for the step-by-step build that produced them.

Both services are packaged as layered container images with an `/actuator/health`
endpoint; `member-services/docker-compose.yml` runs them on the X-Road network's host
ports `4710` (portal) and `4711` (registry).

> **Executable version of this page:** `practice-lab/` at the repository root rebuilds
> this lab from scratch, one UI screen per page — Central Server initialization, both
> Security Servers, client registration, service description, ACL, first call. It is a
> working notebook and is deliberately outside this VitePress site.

## Build order

1. **Copy the v7 stack into `infra/v7/`.** Do not edit `/tmp` or the upstream clone. Keep a lab-owned `.env`, pin image digests, and document the reset command.
2. **Keep the test CA.** Replace the demo members/services with `SME-CU`, `MOC`, and `CAMD-X` (see [06-lab-topology.md](../x-road/06-lab-topology.md)).
3. **Add the third Security Server.** One SS per organization makes the trust boundaries visible.
4. **Replace the demo IS containers with Spring Boot 4 services.** Start with one REST consumer (`sme-cu-portal` → `moc-registry`).
5. **Register both directions.** SME-CU consumes MOC; MOC consumes SME-CU's `applicationSubmit`. A one-way lab hides half the registration work.
6. **Add negative tests.** Wrong client, wrong service, disabled description, ACL removed, provider down.
7. **Write the lab runbook for humans.** One step per file, like this guide, with expected output and failure explanations.
8. **Update statuses.** Change `Planned` to `Verified` only after the step runs on this host.

## Mapping from demo to lab

| Demo | Lab |
| --- | --- |
| `DEV/COM/1234/TestService` | `DEV/BIZ/SME-CU/PORTAL` (and MOC/CAMD-X providers) |
| `DEV/COM/4321/TestClient` | `DEV/BIZ/SME-CU/PORTAL` as consumer; `DEV/GOV/MOC/REGISTRY` for the reverse flow |
| `isrest` WireMock | `moc-registry` Spring Boot provider |
| `isopenapi` | `camdx-catalog` Spring Boot provider |
| `issoap` | Add only after REST works, or if a partner requires WSDL |
| Test CA | Same test CA; never CamDX PKI |

## First implementation target

Build this single flow end-to-end:

```text
sme-cu-portal  --X-Road-->  ss-smecu  --mTLS-->  ss-moc  --HTTP-->  moc-registry
     |                                                                    |
     +---------------------- businessLookup --------------------------+
```

Do not start with three services and two directions. Get one provider, one consumer, one ACL, and one successful call. Then add the second flow.

**Status:** the forward flow is verified (`200`, step 14) and the reverse flow is verified
(`201`, step 16) in `practice-lab/`. The three-organization topology above is still the
next project.

## Reference pages

- [06-lab-topology.md](../x-road/06-lab-topology.md) — organizations, IDs, ports, trust layout.
- [08-spring-boot-integration.md](../x-road/08-spring-boot-integration.md) — Spring Boot consumer/provider patterns.
- [04-registration-and-access.md](../x-road/04-registration-and-access.md) — the registration/ACL checklist.
- [09-troubleshooting.md](../x-road/09-troubleshooting.md) — symptom matrix and logs.
- [11-camdx-onboarding.md](../x-road/11-camdx-onboarding.md) — how the lab maps to CamDX membership.

## A note on production

The lab teaches mechanics. It does not make SME-CU a CamDX member, does not use CamDX PKI, and does not satisfy CamDX security review. Those are separate organizational and security workstreams.
