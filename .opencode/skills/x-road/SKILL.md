---
name: X-Road Lab
description: Answer questions and do tasks about X-Road and CamDX in this repository, including architecture, identifiers, Central Server, Security Server, registration, ACLs, REST/SOAP protocols, message flow, troubleshooting, Docker lab setup, and Spring Boot 4 integration. Use whenever the user mentions X-Road, CamDX, SME-CU integration, Security Server, X-Road-Client, or an X-Road service call.
---

# X-Road Lab Skill

Use this skill for any X-Road question or task in this repo. The knowledge base is the repository's `docs/` directory (three levels up from this skill); do not answer from memory when a page exists.

## Ground rules

1. **Version first.** This lab targets **X-Road 7.8.x**. CamDX's fork is on **7.8.2**; upstream stable/docs are **7.8.3**. The local clone at `/mnt/data/opensource/x-roads/X-Road` is on **8.0.0-beta2** (`develop`) — never read v7 behavior from its working tree. Use `git show 7.8.3:<path>` or extract tag `7.8.3`.
2. **Upstream is read-only.** Do not edit `/mnt/data/opensource/x-roads/**`. Copy into this repo.
3. **Separate fact from plan.** Docs mark `Verified` vs `Planned`. Do not present a planned lab step as executed. If you verify something new, update the relevant docs page and date.
4. **REST first.** CamDX prefers REST. Use SOAP only when a provider exposes a WSDL or the user asks.
5. **Do not mix v7 and v8 artifacts, ports, images, or manuals.** X-Road 8 uses OpenBao/gRPC/split containers and is beta.
6. **The lab is not production.** Test CA, `xrd/secret`, token PIN `Secret1234`. Never reuse its certs/keys.

## Route the question

| User asks about | Read |
| --- | --- |
| Concepts, components, trust, CamDX model | `../../../docs/x-road/01-foundations.md` |
| IDs, allowed characters, URL/SOAP identifier form | `../../../docs/x-road/02-identifiers.md` |
| REST/SOAP contract, headers, transport, signing, OCSP, ports | `../../../docs/x-road/03-protocols-and-security.md` |
| Registering members/subsystems/servers, service descriptions, ACLs | `../../../docs/x-road/04-registration-and-access.md` |
| v7 vs v8, CamDX version evidence, version-safe commands | `../../../docs/x-road/05-versions-and-camdx.md` |
| Lab orgs, service flows, ports, trust layout | `../../../docs/x-road/06-lab-topology.md` |
| Starting the upstream 7.8.3 Docker stack, demo credentials | `../../../docs/guide/index.md` |
| Spring Boot 4 consumer/provider, REST/SOAP code, TLS | `../../../docs/x-road/08-spring-boot-integration.md` |
| A call fails, error headers, logs, registration stuck | `../../../docs/x-road/09-troubleshooting.md` |
| A term is unclear | `../../../docs/x-road/10-glossary.md` |
| CamDX membership/onboarding mapping | `../../../docs/x-road/11-camdx-onboarding.md` |
| Implemented SME-CU/MOC Spring Boot services | `../../../docs/x-road/13-member-services.md` and `../../../member-services/README.md` |
| Why the lab chose v7 | `../../../docs/decisions/0001-x-road-version.md` |

For exhaustive upstream detail, follow the links in those pages to `https://docs.x-road.global/` (7.8.3) or the tagged source.

## Workflow: answer a concept question

1. Read the routed page, then the relevant upstream section if the answer needs normative detail.
2. State the version you are answering for if it matters.
3. Give the concrete identifier/header/port, not a generic description.
4. Link the repo page (and upstream source) in the answer.
5. If the question exposes a gap or a wrong doc statement, fix the doc in the same task.

## Workflow: debug a failed X-Road call

Do not start with the application. Work in this order and report which step failed:

1. **Parse the identifiers** in the request: instance / member class / member code / subsystem / service code / version. Check allowed characters and `X-Road-Client` is a subsystem, not a member.
2. **Consumer SS client state** — must be `Registered`, not `Saved`/`Registration in progress`/`Disabled`/`Global error`.
3. **Provider service description** — present, URL set, description **enabled**.
4. **ACL** — provider service/endpoint has the exact consumer subsystem (or a group containing it).
5. **Reachability from the SS container**, not the host: `docker exec <ss> curl -v <service-url>`. Use container name + internal port.
6. **Transport** — auth cert registered/approved, CA trusted, OCSP valid, clocks synced.
7. **Response taxonomy** — if `X-Road-Error` is present it is an X-Road failure; `Client.*` vs `Server.ClientProxy.*` vs `Server.ServerProxy.*`. Capture `X-Road-Request-Id` and the body `detail` UUID.
8. **Application** — provider status/body without `X-Road-Error` means the provider app failed.

Use `../../../docs/x-road/09-troubleshooting.md` for the symptom matrix and log commands. Do not "fix" an ACL by granting a broad group until you have identified the missing exact subject.

## Workflow: implement or change a Spring Boot integration

1. Confirm the intended direction: consumer, provider, or both.
2. Confirm the protocol (default REST) and the exact IDs from `../../../docs/x-road/02-identifiers.md`.
3. Consumer: call the **consumer SS** with `RestClient`/`WebClient`, base path `/r1/<serviceId>`, default `X-Road-Client` header. Never call the provider IS directly.
4. Provider: normal Spring MVC controller; do not set `X-Road-*` response headers; stay reachable from the SS at the configured service URL.
5. Keep IDs in config/env, not hard-coded in controllers.
6. For SOAP: use `xrd4j`/CXF, Jakarta imports, copy all request headers into the response, and verify the WSDL wrapper names. `xrd4j` is X-Road 7; do not claim v8 support.
7. Test in this order: provider direct → consumer SS with curl → reverse direction → negative cases (wrong client, wrong service, disabled description, provider down).
8. Report the exact commands and the observed `X-Road-*` headers. "It compiles" is not verification.

## Member services (implemented)

The runnable code is in `../../../member-services/`:

- `moc-registry` — provider on `8081`; `GET /api/businesses`, `GET /api/businesses/{id}`.
- `sme-cu-portal` — consumer on `8080`; `GET /api/portal/businesses`, `GET /api/portal/businesses/{id}`.
- Composite build: `./gradlew buildAll` / `testAll`; run with `./gradlew -p <service> bootRun`.
- Direct mode is the default. X-Road mode: `PORTAL_XROAD_ENABLED=true` plus `PORTAL_XROAD_*` settings; service path `/r1/DEV/GOV/MOC/REGISTRY/businessLookup/v1`, client `DEV/BIZ/SME-CU/PORTAL`.
- Do not point it at the demo `mock1`; that returns test text, not the business JSON contract.
- Spring Boot 4.1.1 does not auto-provide `RestClient.Builder` here; use `RestClient.builder()`.

## Workflow: lab/infra changes

1. Read `../../../docs/x-road/06-lab-topology.md` and `../../../docs/guide/index.md`.
2. Keep `infra/` and `services/` separate from the upstream clone.
3. Use one Docker network (`xroad-network`) and container names for service URLs.
4. Preserve the port plan or update the topology table and compose file together.
5. Keep test certs/keys/.env out of git; commit only examples.
6. After a successful bring-up, update the runbook status from `Planned` to `Verified` with the commands and versions actually used.

## Quick reference

```text
Instance:            DEV
SME-CU client:       DEV/BIZ/SME-CU/PORTAL
Partner registry:    DEV/GOV/MOC/REGISTRY
CamDX catalogue:     DEV/GOV/CAMD-X/CATALOG

REST request:        {method} /r1/{serviceId}[/path]
Mandatory header:    X-Road-Client: {instance}/{class}/{member}/{subsystem}
SS-to-SS:            5500/tcp (mTLS) + 5577/tcp (OCSP)
Admin UI:            4000/tcp (https), bind to trusted network
IS access:           8080/8443/tcp

Upstream demo:       DEV/COM/1234/TestService + DEV/COM/4321/TestClient
Demo REST:           POST http://localhost:4310/r1/DEV/COM/1234/TestService/mock1
Demo SOAP:           POST http://localhost:4310/ (getRandom/v1)
Demo UI creds:       xrd / secret; token PIN Secret1234
```

## Before saying a task is done

- [ ] Version is explicit (v7.8.x unless the user asked for v8).
- [ ] Identifiers match across CS, SS, config, and test command.
- [ ] Registration states are `Registered`, descriptions enabled, ACLs exact.
- [ ] The call was made through the **consumer SS**, not directly to the provider.
- [ ] Response has `X-Road-Id`/`X-Road-Request-Hash` and no `X-Road-Error`.
- [ ] Reverse direction tested if both orgs are producers.
- [ ] For code changes: `cd member-services && ./gradlew testAll` passes.
- [ ] Docs updated if a fact/decision changed; no secrets/certs committed.
- [ ] Upstream clone untouched.

## When information is missing

Ask one focused question only if the repo/docs cannot answer it. Good candidates:

- Which CamDX environment/version the user must match (test vs production).
- Whether a real CamDX certificate/onboarding is in scope.
- Whether to build the three-org lab or only the upstream demo first.

Do not ask about things the KB already states, such as the default lab version, IDs, ports, or REST-first preference.

## Maintaining this skill

When the lab version changes, update in one pass:

1. `../../../docs/x-road/05-versions-and-camdx.md` and the ADR.
2. `../../../docs/x-road/02-identifiers.md` + `06-lab-topology.md` if IDs change.
3. This skill's ground rules and quick reference.
4. `../../../AGENTS.md` if the workflow or target version changes.
