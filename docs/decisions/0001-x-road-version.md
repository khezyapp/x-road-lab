# 0001 — Target X-Road 7.8.x for the Lab

- **Status:** Accepted for the learning lab
- **Date:** 2026-10-23
- **Deciders:** Lab owner (SME-CU preparation)
- **Related:** [../x-road/05-versions-and-camdx.md](../x-road/05-versions-and-camdx.md)

## Context

The lab must teach how SME-CU will integrate through CamDX. It needs a working local X-Road ecosystem and two or three Spring Boot 4 information systems.

Three version signals conflict:

1. The public X-Road documentation is **7.8.3**.
2. The local upstream clone is **8.0.0-beta2** (`develop`), unreleased, with a first production target of December 2026.
3. The CamDX fork (`Techo-Startup-Center/CamDX`) is on **7.8.2**, with recent Cambodia PKI/branding and RHEL 9 work.

X-Road 8 changes deployment substantially: Java 25, Spring Boot 4.1.1, OpenBao, gRPC, split Security Server containers, and a 7.8.x-only upgrade path. Those are real topics, but they are not the integration mechanics SME-CU needs first.

## Decision

Use **X-Road 7.8.x** (prefer 7.8.3 upstream, matching CamDX's 7.8.2 where relevant) for the lab.

- Use the upstream 7.8.3 Docker dev stack for the first smoke test.
- Build the three-organization lab on a v7.8.x baseline.
- Keep X-Road 8 as a separate, optional experiment.
- Pin source tags and image versions; never use `develop` source for v7 behavior.

## Consequences

**Positive**

- Matches the CamDX fork's version and the relevant onboarding behavior.
- Stable manuals, `xrd4j` support for X-Road 7, and a ready-made Docker Compose stack.
- Less operational noise: no OpenBao/gRPC/split-container learning curve.
- Spring Boot 4 can still be used for the information systems; the X-Road core version is independent of the app framework.

**Negative / trade-offs**

- The lab does not exercise X-Road 8 features or the v7→v8 upgrade.
- The local clone's working tree is v8, so v7 source must be read via `git show 7.8.3:...` or an extracted tag.
- Some upstream v7 artifacts use `:latest` image tags; reproducibility requires digest pinning.

## Alternatives considered

| Alternative | Why not now |
| --- | --- |
| X-Road 8 beta | Unreleased, beta 2, different deployment, OpenBao/gRPC overhead, and not what CamDX currently tracks. |
| Both v7 and v8 labs in parallel | Doubles infrastructure and splits attention; add v8 later if needed. |
| X-Road 6.x (CamDX's older tag) | Older protocol/tooling; 7.8.x is the current CamDX-aligned baseline. |

## Revisit triggers

Revisit this decision when any of the following becomes true:

- CamDX moves its fork/production to X-Road 8.
- The learning goal shifts to X-Road 8 development, upgrade, or operations.
- X-Road 8 reaches a production release and the CamDX operator publishes an upgrade path.
- A required CamDX integration only works on v8 (verify with the operator before assuming).

## Notes

This ADR decides the **X-Road core** version, not the Spring Boot version. The information systems can and should use Spring Boot 4.x (Java 25) regardless of whether the core is v7 or v8. The protocol between the app and the SS remains REST or SOAP 4.0.
