# CamDX Onboarding and SME-CU

Status: **Verified** from the CamDX public site (<https://camdx.gov.kh/>) and the CamDX X-Road fork. This page maps the organizational process to the lab; it is not a substitute for CamDX's official instructions.

## What CamDX expects from a member

CamDX describes a five-step membership path:

1. Submit the online application form.
2. Wait for review and approval by Techo Startup Center (the operator).
3. Fulfill the technical and testing requirements:
   - complete the Security Server installation;
   - prepare the API catalogue for listing (provider) or the preferred API list (consumer);
   - submit the Information Security Guideline checklist;
   - submit the SIT and UAT documents.
4. Sign the contract after the requirements are met.
5. Receive the official membership announcement.

CamDX members connect through a **Security Server signed to CamDX's PKI**. CamDX core components provide registry, monitoring, transaction logging, and proxying; they do not own the exchanged data.

## Mapping to the lab

| CamDX requirement | Lab artifact | What it teaches |
| --- | --- | --- |
| Membership application / approval | None | Organizational process; cannot be simulated. |
| Security Server installation | `SS-SMECU` (and one SS per partner) | Initialization, auth certificate, CS registration, global configuration. |
| CamDX PKI certificate | Test CA certificate | Certificate request/import/registration; **not** CamDX-trusted. |
| API catalogue (provider) | Service descriptions: OpenAPI 3 or REST base path | Service code, endpoints, service URL, metadata. |
| Preferred API list (consumer) | ACL subjects + consumer config | Which services SME-CU may call and with which subsystem identity. |
| Security checklist | TLS, ACLs, message log, patching, backups | The operational controls around the integration. |
| SIT / UAT | Positive and negative end-to-end tests | Registration, ACL, error handling, reverse direction. |
| Contract / announcement | None | Organizational process. |

## What SME-CU should prepare

Technical:

- Member class and member code assigned/agreed with CamDX.
- Subsystem code for the SME-CU portal (`PORTAL` in this lab).
- An OpenAPI 3 description for every service SME-CU provides, or a base URL + service code.
- The list of CamDX services SME-CU will consume, with service codes and versions.
- A Security Server hostname, public address, and certificate signing request.
- Network plan: inbound 5500/5577, outbound to the CamDX CS and partner SSs, admin UI restricted.
- Test cases for both directions, including failure cases.

Organizational:

- Named technical and security contacts.
- Information Security Guideline checklist responses.
- SIT/UAT plan and evidence.
- Data-processing/legal basis for each exchanged data field.

## Lab vs real CamDX

| Aspect | Lab | Real CamDX |
| --- | --- | --- |
| Instance | `DEV` | CamDX instance (identifier provided by the operator). |
| Trust | Local test CA | CamDX PKI; certificates issued through the CamDX process. |
| Central Server | Local container | CamDX-operated core. |
| Membership | Local member records | Approved CamDX member. |
| API catalogue | Local service descriptions | CamDX API catalogue entry. |
| Security review | Self-check | CamDX checklist + SIT/UAT review. |
| Data | Mock | Real, legally authorized data. |

A successful lab call proves the mechanics, not CamDX approval. Do not send real personal or business data through the lab.

## Recommended lab sequence for SME-CU

1. Run the upstream 7.8.3 demo and make one REST call.
2. Build the three-org topology: SME-CU, MOC, CAMD-X.
3. Implement SME-CU as a consumer of `businessLookup` and `catalogSearch`.
4. Implement SME-CU as a provider of `applicationSubmit`; have MOC consume it.
5. Add negative tests: missing ACL, wrong subsystem, disabled service, provider down.
6. Write the SME-CU runbook: IDs, endpoints, certificates, owners, and reset steps.
7. Only then map the runbook to CamDX's real onboarding checklist.

## Questions to confirm with CamDX

These cannot be answered from the public site or the lab:

- Which CamDX environment (test/production) and X-Road version SME-CU must match.
- The exact member class and member code SME-CU will receive.
- CamDX PKI certificate profiles, CSR format, and whether ACME is supported.
- Whether CamDX requires a specific Security Server variant or hardening baseline.
- The API catalogue format and review workflow.
- Data-protection and audit requirements for SME-CU's specific services.

Record the answers in this page when verified; do not guess them in code or configuration.
