# Lab Topology

Status: **Partially deployed (2026-10-25).** The two-organization flows are implemented and
verified in `practice-lab/`: SME-CU and MOC, each with a Security Server, consuming each
other's service. The CamDX member, the third Security Server, and the catalogue flow are
still **Planned**. The source tree is 7.8.3, but the published dev images run 7.8.1.

## Goal

Simulate a small CamDX-style ecosystem in which:

- CamDX operates the Central Server and the API catalogue.
- SME-CU consumes a partner's registry service and provides its own application-submission service.
- A partner agency (Ministry of Commerce placeholder) provides a business registry service and consumes SME-CU's service.

This exercises both directions: SME-CU as consumer and as provider.

## Topology

```text
                         ┌─────────────────────────────┐
                         │ Central Server (CamDX)      │
                         │ instance: DEV               │
                         │ UI :4100                    │
                         │ test CA / TSA / OCSP        │
                         └─────────────┬───────────────┘
                                       │ global configuration
          ┌────────────────────────────┼────────────────────────────┐
          │                            │                            │
┌─────────▼─────────┐        ┌─────────▼─────────┐        ┌─────────▼─────────┐
│ SS-SMECU          │        │ SS-MOC            │        │ SS-CAMDX          │
│ UI :4200          │        │ UI :4300          │        │ UI :4400          │
│ proxy :4210       │        │ proxy :4310       │        │ proxy :4410       │
│ member SME-CU     │        │ member MOC        │        │ member CAMD-X     │
│ subsystem PORTAL  │        │ subsystem REGISTRY│        │ subsystem CATALOG │
└─────────┬─────────┘        └─────────┬─────────┘        └─────────┬─────────┘
          │ HTTP (lab)                  │ HTTP (lab)                  │ HTTP (lab)
┌─────────▼─────────┐        ┌─────────▼─────────┐        ┌─────────▼─────────┐
│ sme-cu-portal     │        │ moc-registry      │        │ camdx-catalog     │
│ Spring Boot 4     │        │ Spring Boot 4     │        │ Spring Boot 4     │
│ :8080             │        │ :8080             │        │ :8080             │
└───────────────────┘        └───────────────────┘        └───────────────────┘

All containers share the Docker network `xroad-network`.
SS-to-SS traffic uses mutual TLS on 5500/tcp and OCSP on 5577/tcp.
```

## Members and identifiers

| Organization | Role | Member class | Member code | Subsystem | Security Server |
| --- | --- | --- | --- | --- | --- |
| SME-CU | Consumer + provider | `BIZ` | `SME-CU` | `PORTAL` | `SS-SMECU` |
| Partner agency (MoC placeholder) | Provider + consumer | `GOV` | `MOC` | `REGISTRY` | `SS-MOC` |
| CamDX platform | Catalogue provider + operator | `GOV` | `CAMD-X` | `CATALOG` | `SS-CAMDX` |

Instance: `DEV`. The real CamDX instance identifier is not used in the lab; using `DEV` prevents accidental confusion with production.

## Service flows

| # | Flow | X-Road service | Protocol | ACL subject |
| --- | --- | --- | --- | --- |
| 1 | SME-CU portal → partner registry | `DEV/GOV/MOC/REGISTRY/businessLookup` | REST `GET` | `DEV/BIZ/SME-CU/PORTAL` |
| 2 | SME-CU portal → CamDX catalogue | `DEV/GOV/CAMD-X/CATALOG/catalogSearch` | REST `GET` | `DEV/BIZ/SME-CU/PORTAL` |
| 3 | Partner registry → SME-CU portal | `DEV/BIZ/SME-CU/PORTAL/applicationSubmit` | REST `POST` | `DEV/GOV/MOC/REGISTRY` |

Flow 3 is what makes this a two-way lab rather than a single consumer demo. It also forces the team to register SME-CU as a provider and MOC as a consumer, which is where most real onboarding mistakes appear.

## Suggested application endpoints

The provider IS exposes ordinary HTTP endpoints behind its SS:

| App | Internal endpoint | X-Road service code | Notes |
| --- | --- | --- | --- |
| `moc-registry` | `GET /api/businesses/{id}` | `businessLookup` | Returns a mock registration record. |
| `camdx-catalog` | `GET /api/catalog?q=` | `catalogSearch` | Returns mock service metadata. |
| `sme-cu-portal` | `POST /api/applications` | `applicationSubmit` | Accepts an application payload; provider side of flow 3. |

The **consumer** side of flow 3 lives in `moc-registry` or a small test client; it calls its own SS with `X-Road-Client: DEV/GOV/MOC/REGISTRY`.

## Port plan (host)

| Service | UI / admin | Proxy / app |
| --- | --- | --- |
| Central Server | `4100` | — |
| SS-SMECU | `4200` | `4210` |
| SS-MOC | `4300` | `4310` |
| SS-CAMDX | `4400` | `4410` |
| Test CA | `4500` (cert API), `4501` (ACME) | — |
| `sme-cu-portal` | — | `4710` |
| `moc-registry` | — | `4711` |
| `camdx-catalog` | — | `4712` |
| Mailpit (optional) | `8025` | `1025` (SMTP) |

Inside the Docker network, every app still listens on `8080`; host ports are only for debugging. The SS service URL must use the **container name and internal port**, for example `http://moc-registry:8080/api` (the extra `/api` is part of the provider base path, so the caller's `/businesses/123` arrives as `/api/businesses/123`).

## Trust layout

- One **test CA** issues all authentication and signing certificates.
- Each Security Server has an authentication key/certificate and registers it in the CS.
- Each member has a signing key/certificate; all subsystems of that member on the same SS share it.
- The test CA's root and OCSP responder are approved in the CS.
- The test TSA is approved in the CS.
- The test CA is not trusted by CamDX. This lab cannot join the real CamDX instance.

## What this topology teaches

- Member vs subsystem vs service identifiers.
- Security Server registration and subsystem registration, including approval states.
- Service description vs service URL.
- Service-level ACLs in both directions.
- REST consumer/provider integration in Spring Boot 4.
- Global configuration propagation and message logs.
- The difference between "my app works" and "the X-Road service is callable".

## Out of scope for the first iteration

- Federation with a second X-Road instance.
- Configuration Proxy, HA, monitoring, and operational monitoring.
- Real CamDX PKI, production certificates, or CamDX approval.
- SOAP (add it only after the REST flows work, using the upstream example adapter).
- X-Road 8 deployment.

## Relationship to the upstream demo stack

The upstream 7.8.3 Docker stack is a **smoke test**, not this topology:

- it has one CS, two SSs, and demo members `DEV/COM/1234` / `DEV/COM/4321`;
- it already initializes registration, services, and ACLs;
- it is the fastest way to see a working call before building the three-org lab.

Run it first using [07-runbook-v7-docker.md](07-runbook-v7-docker.md), then build the topology above in `infra/` with its own compose file.

## Management services in the first pass

The CS needs a registered subsystem to serve management requests, and the private
parameters name it. The demo gives that role to `DEV/COM/1234/MANAGEMENT` on `ss0`.

In the `practice-lab/` first pass, `SS-SMECU` hosts management services
(`DEV/BIZ/SME-CU/MANAGEMENT`) because no CamDX Security Server exists yet. The eventual
three-organization lab gives that role to `SS-CAMD-X`; until then, say "lab shortcut",
not "the design".
