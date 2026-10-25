---
layout: home

hero:
  name: X-Road Lab
  text: SME-CU / CamDX learning lab
  tagline: A disposable X-Road 7.8.x ecosystem for simulated organizations, with the knowledge base that explains every step.
  actions:
    - theme: brand
      text: Start the lab guide
      link: /guide/
    - theme: alt
      text: Read the foundations
      link: /x-road/01-foundations

features:
  - title: Manual lab guide
    details: Thirteen copy/paste steps for the upstream X-Road 7.8.3 Docker dev stack — each with expected output and what a failure means.
    link: /guide/
  - title: Knowledge base
    details: Foundations, identifiers, protocols, registration, Spring Boot integration, troubleshooting, and a glossary.
    link: /x-road/01-foundations
  - title: Version-safe
    details: Pinned to X-Road 7.8.x. The 8.0 beta stays a separate experiment and never shares images, ports, or manuals with this lab.
    link: /x-road/05-versions-and-camdx
---

# X-Road Knowledge Base

This directory is the reference for the SME-CU / CamDX X-Road lab. It is written for both humans and coding agents: each page answers a specific class of question and points to the upstream source it was derived from.

## Read this first

- **Lab target: X-Road 7.8.x.** The CamDX fork tracks **7.8.2**, upstream stable is **7.8.3**, and the public docs site serves **7.8.3**.
- **X-Road 8 is beta.** The local upstream clone is `8.0.0-beta2` on `develop`; do not mix v7 and v8 instructions.
- **The lab is not production.** It uses a test CA, test credentials, and disposable containers. Never reuse its keys, certificates, or configuration in a real CamDX environment.
- **REST first.** CamDX prefers REST APIs. SOAP is supported and documented, but only use it when a provider exposes a WSDL.

## Sources and trust order

When sources disagree, trust them in this order:

1. Executable lab configuration (`infra/`, `services/`) once it exists.
2. Upstream source/tag at `/mnt/data/opensource/x-roads/X-Road` (pin the tag or commit).
3. Public manual for the same version: <https://docs.x-road.global/> (currently 7.8.3).
4. X-Road 8 beta manuals inside the local clone (`doc/Manuals/*v8*`) — only for v8 work.

The upstream checkout is **read-only reference**. Copy what you need into this repo; do not edit it.

## Index

| Page | Use it for |
| --- | --- |
| [01-foundations.md](x-road/01-foundations.md) | What X-Road is, the components, the trust model, and how CamDX maps onto it. |
| [02-identifiers.md](x-road/02-identifiers.md) | Instance/member/subsystem/service IDs, allowed characters, and the lab's ID scheme. |
| [03-protocols-and-security.md](x-road/03-protocols-and-security.md) | REST and SOAP contracts, transport, signing, OCSP, timestamps, message log, errors. |
| [04-registration-and-access.md](x-road/04-registration-and-access.md) | End-to-end commissioning checklist, client states, service descriptions, ACLs. |
| [05-versions-and-camdx.md](x-road/05-versions-and-camdx.md) | v7 vs v8, what the CamDX fork actually runs, and version-safe rules. |
| [06-lab-topology.md](x-road/06-lab-topology.md) | Proposed three-organization topology, service flows, and trust layout. |
| [07-runbook-v7-docker.md](x-road/07-runbook-v7-docker.md) | Runbook index; the copy/paste steps are in `docs/guide/`. |
| [guide/index.md](guide/index.md) | Step-by-step manual lab: one small file per step, with expected results and failure explanations. |
| [08-spring-boot-integration.md](x-road/08-spring-boot-integration.md) | Spring Boot 4 consumer/provider patterns, REST and SOAP, TLS, Docker networking. |
| [09-troubleshooting.md](x-road/09-troubleshooting.md) | Symptom-to-cause triage for registration, ACL, proxy, TLS, and configuration failures. |
| [10-glossary.md](x-road/10-glossary.md) | Short definitions of X-Road terms used across the pages. |
| [11-camdx-onboarding.md](x-road/11-camdx-onboarding.md) | CamDX membership requirements and how they map to the lab. |
| [12-rfc3161-timestamping.md](x-road/12-rfc3161-timestamping.md) | RFC 3161 cheatsheet: ASN.1, OIDs, status codes, and Java request/verify snippets. |
| [13-member-services.md](x-road/13-member-services.md) | Implemented SME-CU consumer and MOC provider (composite Gradle build). |
| `practice-lab/` (outside this site) | Executable from-scratch build of the lab: UI steps for the CS, both Security Servers, clients, service description, ACL, and first call. |
| [decisions/0001-x-road-version.md](decisions/0001-x-road-version.md) | Why the lab targets 7.8.x and keeps v8 as a separate experiment. |

## Conventions

- **Status labels:** `Verified` means checked against source/docs on the date shown. `Planned` means a lab decision that has not been executed yet. Do not silently promote `Planned` to `Verified`.
- **Version pinning:** commands that use the upstream clone must name a tag (for example `7.8.3`), not `develop`.
- **One source of truth for IDs:** the planned IDs live in [02-identifiers.md](x-road/02-identifiers.md) and [06-lab-topology.md](x-road/06-lab-topology.md). If you change one, change the other and the lab config in the same change.
- **Keep pages short:** link to the upstream manual for exhaustive detail; this KB records the decisions, the traps, and the working sequence.
- **This is also a website:** the pages are plain Markdown under `docs/`; run `npm run docs:dev` to read them in VitePress. New pages must also be added to the sidebar in `docs/.vitepress/config.mts`.

## Attribution

The summaries here are derived from the X-Road documentation (CC BY-SA 3.0), the X-Road source at tag `7.8.3`, and the CamDX fork. Upstream documents remain authoritative; links are provided on each page.

_Last verified: 2026-10-23._
