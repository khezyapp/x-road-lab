# Practice Lab — build the SME-CU / MOC X-Road lab from scratch

This is the executable version of [docs/guide/13-next-steps.md](../docs/guide/13-next-steps.md).
The `docs/guide/` series runs the upstream demo, where a script creates everything for
you. This series does the opposite: **you create the members, certificates, services and
ACLs yourself**, in the UI, one screen at a time.

Read a page, run what it says, compare with **Expected**, then move on. Nothing here is
verified until it has run on this host — see the status column.

## Status

| Step | Page | Establishes | Status |
| --- | --- | --- | --- |
| 00 | [00-what-you-are-building.md](00-what-you-are-building.md) | The target topology and every identifier you will type | Verified (design) |
| 01 | [01-prerequisites.md](01-prerequisites.md) | Host, ports, source tree, clock | Verified 2026-10-25 |
| 02 | [02-own-the-stack.md](02-own-the-stack.md) | `infra/v7/` stack, renamed SSes, pinned images | Verified 2026-10-25 |
| 03 | [03-start-cs.md](03-start-cs.md) | CS + test CA running, nothing initialized | Verified 2026-10-25 |
| 04 | [04-init-cs.md](04-init-cs.md) | Instance `DEV`, token, signing keys, member classes | Verified 2026-10-25 |
| 05 | [05-create-members.md](05-create-members.md) | Members `SME-CU` and `MOC`; management provider | Verified 2026-10-25 |
| 06 | [06-add-ca-ocsp-tsa.md](06-add-ca-ocsp-tsa.md) | Trust services approved in the CS | Verified 2026-10-25 |
| 07 | [07-start-sses.md](07-start-sses.md) | Two uninitialized Security Servers | Verified 2026-10-25 |
| 08 | [08-init-ss-smecu.md](08-init-ss-smecu.md) | `ss-smecu` initialized, auth active, signing key added | Verified 2026-10-25 |
| 08b | [08b-management-services.md](08b-management-services.md) | Management service provider, WSDL, ACL | Verified 2026-10-25 |
| 09 | [09-init-ss-moc.md](09-init-ss-moc.md) | `ss-moc` registered with the CS | Verified 2026-10-25 |
| 10 | [10-register-clients.md](10-register-clients.md) | `PORTAL` and `REGISTRY` clients `Registered` | Verified 2026-10-25 |
| 11 | [11-deploy-apps.md](11-deploy-apps.md) | Both Spring Boot services healthy | Verified 2026-10-25 |
| 12 | [12-publish-service.md](12-publish-service.md) | `businessLookup` description enabled | Verified 2026-10-25 |
| 13 | [13-grant-acl.md](13-grant-acl.md) | SME-CU allowed to call MOC | Verified 2026-10-25 |
| 14 | [14-first-call.md](14-first-call.md) | **First successful X-Road call** | Verified 2026-10-25 |
| 15 | [15-portal-in-xroad-mode.md](15-portal-in-xroad-mode.md) | The app consumes through X-Road | Verified 2026-10-25 |
| 16 | [16-reverse-flow.md](16-reverse-flow.md) | MOC consumes SME-CU's `applicationSubmit` | Verified 2026-10-25 |
| 17 | [17-negative-tests.md](17-negative-tests.md) | What the five failures look like | Verified 2026-10-25 (4/5) |
| 18 | [18-logs-and-evidence.md](18-logs-and-evidence.md) | Request id, error UUID, message log | Verified 2026-10-25 |
| 19 | [19-reset.md](19-reset.md) | A clean wipe and rebuild | Verified 2026-10-25 |

**All steps 00–19 are verified on this host as of 2026-10-25.** The lab was reset at the
end, so the stack is currently down; rebuild with `scripts/up.sh cs testca` and start at
[step 03](03-start-cs.md).

## Prerequisites in one line

Docker + Compose v2, JDK 25, the extracted tag at `tmp/xroad-7.8.3/`, and ports
4100/4200/4210/4300/4310/4400/4700/4710/4711 free.

## Rules for this lab

- One step per page. Do not skip ahead; every step depends on the previous state.
- Never run `local-dev-run.sh --initialize`. It does not exist in `infra/v7/` on purpose:
  it would create the demo members we are replacing.
- The test CA is disposable and untrusted. Never reuse these keys outside the lab.
- If a step fails, read **If it fails** before changing anything, and record what you saw.
- Only mark a page `Verified` after it ran on this host.

## Known host facts

- The published dev images ship **X-Road 7.8.1**, even though the source tree is 7.8.3.
  Check with `docker exec cs dpkg-query -W -f '${Version}\n' xroad-centralserver`.
  The commissioning mechanics are identical; note differences if a screen misbehaves.
- Status `Draft (API-verified)` means the page's resulting state was confirmed through
  the same management API the UI calls, but the UI itself has not been clicked through.
- **Global configuration generation needs two things after initialization**: one internal
  and one external signing key (step 04), and a management service provider (step 05).
  Missing keys produce `active key missing`; a missing provider produces
  `managementService ... managementRequestServiceProviderId is expected` in the CS log.
- **The CA's Default CSR format locks the SS key wizard.** With `DER` set in step 06, the
  CSR-format selector is read-only during key creation. The test CA detects and signs
  both DER and PEM, so no conversion is needed.

## Where the rest of the knowledge lives

- [docs/x-road/02-identifiers.md](../docs/x-road/02-identifiers.md) — ID grammar.
- [docs/x-road/04-registration-and-access.md](../docs/x-road/04-registration-and-access.md) — the checklist these steps implement.
- [docs/x-road/06-lab-topology.md](../docs/x-road/06-lab-topology.md) — topology and ports.
- [docs/x-road/09-troubleshooting.md](../docs/x-road/09-troubleshooting.md) — symptom matrix.
- [docs/x-road/13-member-services.md](../docs/x-road/13-member-services.md) — the two Spring Boot apps.

## Scripts

- `scripts/up.sh [service...]` — start the stack, or a subset (`scripts/up.sh cs testca`).
- `scripts/ca-certs.sh [dir]` — copy the CA/OCSP/TSA certificates out of `testca` for step 06.
