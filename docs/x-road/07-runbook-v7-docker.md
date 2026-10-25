# Runbook: Upstream X-Road 7.8.3 Docker Dev Stack

Status: **Verified from source; `docker compose config` validated; full stack not yet executed on this host.**

The detailed, copy/paste runbook is split into small steps under [`docs/guide/`](../guide/index.md). Start there:

| Step | File |
| --- | --- |
| 01 | [Prerequisites](../guide/01-prerequisites.md) |
| 02 | [Get X-Road 7.8.3 source](../guide/02-get-source.md) |
| 03 | [Start the containers](../guide/03-start-stack.md) |
| 04 | [Initialize X-Road](../guide/04-initialize.md) |
| 05 | [Open the admin UIs](../guide/05-open-admin-ui.md) |
| 06 | [Inspect registration state](../guide/06-inspect-state.md) |
| 07 | [First REST call](../guide/07-first-rest-call.md) |
| 08 | [Read response headers](../guide/08-read-response-headers.md) |
| 09 | [First SOAP call](../guide/09-first-soap-call.md) |
| 10 | [Failure lab](../guide/10-failure-lab.md) |
| 11 | [Logs and evidence](../guide/11-logs-and-evidence.md) |
| 12 | [Reset](../guide/12-reset.md) |
| 13 | [Next steps](../guide/13-next-steps.md) |

## Why a demo before the lab

The upstream stack gives you a working CS, two Security Servers, a test CA, and demo SOAP/REST providers with initialization automation. It is the fastest way to learn the difference between:

- registered client,
- enabled service description,
- granted ACL,
- provider application error versus X-Road error.

Once that is clear, build the three-organization topology in [`06-lab-topology.md`](06-lab-topology.md) with Spring Boot 4 services.

## Key facts

- Instance: `DEV`
- Demo provider: `DEV/COM/1234/TestService`
- Demo consumer: `DEV/COM/4321/TestClient`
- UI login: `xrd` / `secret`
- Token PIN: `Secret1234`
- CS UI: <https://localhost:4100/>
- SS0 UI / proxy: <https://localhost:4200/> / `http://localhost:4210`
- SS1 UI / proxy: <https://localhost:4300/> / `http://localhost:4310`

Do not use the demo IDs or test certificates in the SME-CU lab or production.
