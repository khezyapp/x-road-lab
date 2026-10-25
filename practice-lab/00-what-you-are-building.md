# 00 — What You Are Building

**Goal:** know every identifier before you type one.
**Time:** 10 minutes reading.

## The target

Two simulated organizations, each with its own Security Server, talking through a
Central Server run by a simulated CamDX. Both information systems are the Spring Boot
services in `member-services/`.

```text
                     ┌──────────────────────────────┐
                     │ Central Server (CamDX)       │
                     │ instance DEV                 │
                     │ UI https://localhost:4100    │
                     │ test CA / OCSP / TSA         │
                     └───────────┬──────────────────┘
                        global configuration (no traffic)
            ┌────────────────────┴────────────────────┐
            │                                         │
  ┌─────────▼──────────┐                   ┌──────────▼─────────┐
  │ SS-SMECU           │◄── mTLS :5500 ───►│ SS-MOC             │
  │ UI  :4200          │                   │ UI  :4300          │
  │ proxy :4210        │                   │ proxy :4310        │
  │ DEV/BIZ/SME-CU     │                   │ DEV/GOV/MOC        │
  │ subsystem PORTAL   │                   │ subsystem REGISTRY │
  └─────────┬──────────┘                   └──────────┬─────────┘
            │ HTTP (lab only)                         │ HTTP (lab only)
  ┌─────────▼──────────┐                   ┌──────────▼─────────┐
  │ sme-cu-portal      │                   │ moc-registry       │
  │ :4710 on the host  │                   │ :4711 on the host  │
  └────────────────────┘                   └────────────────────┘
```

The flow you build first:

```text
sme-cu-portal → ss-smecu → (mTLS) → ss-moc → moc-registry
```

The Central Server never forwards a message. It distributes configuration and approves
registrations. The Security Servers move and sign the traffic.

## Who is who

| Organization | Role in this lab | Member class | Member code | Subsystem | Security Server |
| --- | --- | --- | --- | --- | --- |
| SME-CU | Consumer (then provider) | `BIZ` | `SME-CU` | `PORTAL` | `SS-SMECU` |
| Ministry of Commerce | Provider (then consumer) | `GOV` | `MOC` | `REGISTRY` | `SS-MOC` |
| CamDX | Operates the Central Server | — | — | — | — |

The CamDX platform does not need a member in this first pass. It only operates the CS.
A `CAMD-X` member and a third Security Server come later, after this flow works.

## Identifiers you will type

| Thing | Value | Notes |
| --- | --- | --- |
| Instance | `DEV` | Not the real CamDX instance identifier |
| Consumer client ID | `DEV/BIZ/SME-CU/PORTAL` | Goes in `X-Road-Client` |
| Provider subsystem | `DEV/GOV/MOC/REGISTRY` | Owns the service |
| Service ID | `DEV/GOV/MOC/REGISTRY/businessLookup` | Service code only, no version |
| Security Server codes | `SS-SMECU`, `SS-MOC` | Set during SS initialization |
| Provider app URL | `http://moc-registry:8080/api` | Reachable from **inside** the SS container |
| Consumer SS URL (host) | `http://localhost:4210` | Proxy of `ss-smecu` |
| Consumer SS URL (container) | `http://ss-smecu:8080` | Same server, internal port |

Member and subsystem codes are case-sensitive and cannot be changed without
re-registration. The allowed character set is in
[02-identifiers.md](../docs/x-road/02-identifiers.md).

## Objects you will create, and where

This is the whole lab in one table. Each row is one step.

| Object | Created in | Step |
| --- | --- | --- |
| Instance identifier `DEV`, software token PIN | Central Server UI | 04 |
| Internal and external configuration signing keys | Global Configuration → Signing Keys | 04 |
| Member classes `BIZ` and `GOV` | Central Server UI | 04 |
| Member `DEV/BIZ/SME-CU` | Central Server UI | 05 |
| Member `DEV/GOV/MOC` | Central Server UI | 05 |
| Subsystem `DEV/BIZ/SME-CU/MANAGEMENT` | Central Server UI | 05 |
| Management service provider setting | Settings → System Settings → Management Services | 05 |
| CA, OCSP responder, TSA approved | Central Server UI | 06 |
| `ss-smecu`: anchor, auth cert, registration | SS UI + test CA form + CS approval | 08 |
| Management Services' Security Server (`SS-SMECU`) | Management Services card | 08 |
| `ss-moc`: same | SS UI + test CA form + CS approval | 09 |
| Client `DEV/BIZ/SME-CU/PORTAL` on `ss-smecu` | SS UI + CS approval | 10 |
| Client `DEV/GOV/MOC/REGISTRY` on `ss-moc` | SS UI + CS approval | 10 |
| Service description `businessLookup` | SS-MOC UI | 12 |
| ACL subject `DEV/BIZ/SME-CU/PORTAL` | SS-MOC UI | 13 |

## The seven invariants

By step 14 you should be able to explain these without notes. Each one is established
by a specific step, not by reading.

1. **Member vs subsystem vs service.** A member is an organization. A subsystem is a
   client identity inside that organization. A service is something a subsystem
   publishes. `DEV/BIZ/SME-CU` is a member; `PORTAL` is a subsystem of it.
2. **`X-Road-Client` is the consumer subsystem.** It identifies who is calling, not what
   is called. The called service is in the URL.
3. **Service description URL vs service URL.** The description URL is what the SS reads
   metadata from (for REST, the base path). The service URL is what a real call is
   forwarded to. Both live on the provider SS.
4. **Registered + enabled is not allowed.** Access is denied by default. Without an ACL
   entry for the exact consumer subsystem, a fully registered client still gets `access_denied`.
5. **No `X-Road-Error` for provider errors.** `X-Road-Error` means X-Road itself failed.
   If the provider application returns 500, you get a 500 without that header.
6. **The provider SS creates the evidence headers.** `X-Road-Id`, `X-Road-Request-Id`,
   and `X-Road-Request-Hash` are added by the Security Server, not by the application.
7. **Global configuration takes time.** A correct change on the provider SS can still
   fail for a few seconds while the consumer SS downloads the new configuration.

## Ports

| Service | Host UI | Host proxy | Internal |
| --- | --- | --- | --- |
| `cs` | 4100 | — | 4000 |
| `ss-smecu` | 4200 | 4210 | 4000 / 8080 |
| `ss-moc` | 4300 | 4310 | 4000 / 8080 |
| `testca` | 4400 (`/testca/sign`) | — | 8888 |
| `sme-cu-portal` | — | 4710 | 8080 |
| `moc-registry` | — | 4711 | 8080 |
| demo `isopenapi` (kept) | — | 4700 | 8080 |
| demo `isrest` (kept) | — | 4500 | 8080 |
| demo `issoap` (kept) | — | 4600 | 8080 |

The demo information systems stay in the stack for comparison but have no members in
this Central Server, so they cannot be called until you register them.

## What you are not building yet

- The `CAMD-X` member and third Security Server.
- The reverse flow (`applicationSubmit`, MOC as consumer) — step 16.
- SOAP. REST only until step 14 works.
- Anything X-Road 8.

Next: [01-prerequisites.md](01-prerequisites.md)
