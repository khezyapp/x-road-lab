# X-Road Foundations

Status: **Verified** from `X-Road/doc/Architecture/arc-g_x-road_arhitecture.md` (v1.17, 2026-10-22) and `arc-sec_x_road_security_architecture.md`, plus the CamDX public site.

## What X-Road is

X-Road is a decentralized data-exchange layer between organizations. It is not a central database and it does not transform payloads. It provides:

- **Addressing** — organizations and services have globally unique identifiers.
- **Security** — mutual TLS between Security Servers, signed messages, OCSP validity checks, and time-stamping.
- **Evidence** — signed, time-stamped message logs that can be used for non-repudiation.
- **Access control** — the service provider decides which consumer subsystems may call which service.
- **Federation** — members of different X-Road instances can exchange messages when the instances trust each other.

Two design goals matter for integration work:

1. Data flows directly between the members' Security Servers; the Central Server is not in the message path.
2. X-Road handles organization-level authentication. The information system still handles end-user authentication and application authorization.

## Components

| Component | Role | Lab equivalent |
| --- | --- | --- |
| **Central Server (CS)** | Holds the member/server registry and the instance security policy (trusted CAs, TSAs, parameters). Publishes signed **global configuration**. Provides management services. | One container, `cs`. |
| **Security Server (SS)** | The member's entry point. Terminates mutual TLS, verifies signatures and OCSP, applies ACLs, forwards to the information system, and writes the message log. | One container per simulated organization. |
| **Information System (IS)** | The organization's application. Consumes services, provides services, or both. | Spring Boot 4 app. |
| **Certification Authority (CA)** | Issues authentication certificates to Security Servers and signing certificates to members. Must be approved in the CS. | Test CA in the dev stack. |
| **Time-Stamping Authority (TSA)** | Provides RFC 3161 timestamps for the message log. Must be approved in the CS. | Test TSA in the dev stack. |
| **OCSP responder** | Reports certificate validity. Security Servers cache responses and exchange them during transport. | Test OCSP in the dev stack. |
| **Configuration Proxy (CP)** | Optional mirror that distributes global configuration when the CS must not be exposed directly. | Not required for the first lab. |
| **Monitoring components** | Operational and environmental monitoring. Optional. | Not required for the first lab. |

A Security Server is **multi-tenant**: one server can host several members and subsystems. For learning, use one Security Server per simulated organization so the boundaries are visible.

## Trust chain

```text
Central Server
  ├─ signs global configuration with its configuration-signing key
  ├─ lists approved CAs and TSAs
  └─ registers members, subsystems, Security Servers, and auth certificates
          │
          ▼
Security Server (member A)
  ├─ downloads global configuration using the configuration anchor
  ├─ verifies the CS signature
  ├─ owns an authentication key/cert (server identity)
  └─ hosts signing key/cert for member A (message signatures)
          │
          ▼  mutual TLS on 5500 + signed transport message
Security Server (member B)
  ├─ verifies A's auth certificate against approved CA + OCSP
  ├─ verifies the message signature
  └─ applies the service ACL before forwarding to B's IS
```

Key consequences:

- A Security Server must be **initialized** with the configuration anchor and **registered** in the CS before it can exchange messages.
- A member's subsystems are **not** registered automatically when the member or its Security Server is registered. Each subsystem used as a client must be registered separately.
- Global configuration propagation is asynchronous. A change can be correct in the CS but not yet effective in an SS.
- If the CS, OCSP, or TSA is temporarily unavailable, an already-running SS can continue within its configured grace period. This is why "it worked yesterday" and "the CS is down" can both be true.

## Message path (REST example)

```text
Consumer IS
  │  GET /r1/DEV/GOV/MOC/REGISTRY/businessLookup/...
  │  X-Road-Client: DEV/BIZ/SME-CU/PORTAL
  ▼
Consumer Security Server
  │  checks local client, service, ACL, global config
  │  signs the message, opens mutual TLS to the provider SS
  ▼
Provider Security Server  ── 5500/tcp, mutual TLS
  │  verifies auth cert + OCSP + signature
  │  applies the provider's service ACL
  ▼
Provider IS (Spring Boot)
  │  normal HTTP request; X-Road headers are preserved
  ▼
Response follows the reverse path, with a request hash for binding
```

The consumer never connects directly to the provider IS. It only knows the provider's service ID and the address of its own Security Server.

## What X-Road does not do

- It does not store business data centrally.
- It does not convert REST to SOAP or vice versa. Both ends must use the same message protocol.
- It does not authenticate citizens or end users. The consumer IS must do that and may pass `userId`/`X-Road-UserId`.
- It does not replace application-level authorization. The SS ACL decides whether the consumer subsystem may call; the provider app still enforces business rules.

## CamDX mapping

CamDX is Cambodia's X-Road-based data-exchange platform. The public site (<https://camdx.gov.kh/>) describes the same architecture:

| X-Road concept | CamDX equivalent |
| --- | --- |
| X-Road instance / operator | CamDX ecosystem, operated by Techo Startup Center. |
| Central Server + trust services | CamDX core components and CamDX PKI. |
| Member | A CamDX member organization. |
| Security Server | Required on the member's infrastructure, with a certificate signed by CamDX PKI. |
| Service catalog | CamDX API catalogue; a provider lists APIs, a consumer selects the APIs it needs. |
| Onboarding | Application, technical/testing requirements, Security Server installation, API catalogue, security checklist, SIT/UAT, contract. |

CamDX states a preference for **REST APIs**, but the underlying X-Road instance still supports SOAP. A lab built on a local test instance teaches the same mechanics; it does **not** make the lab trusted by the real CamDX instance. Real membership requires CamDX-issued certificates and their onboarding process.

See [05-versions-and-camdx.md](05-versions-and-camdx.md) for the exact CamDX version evidence, and [11-camdx-onboarding.md](11-camdx-onboarding.md) for the membership mapping.
