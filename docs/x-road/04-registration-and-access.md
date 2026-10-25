# Registration and Access Control

Status: **Verified** from the v7.8.3 Central Server and Security Server user guides and the upstream `development/hurl/scenarios/setup.hurl` automation.

This is the checklist that turns "two Spring Boot apps and a Security Server" into a working X-Road service. Almost every integration failure is a skipped step here.

## Mental model

```text
Central Server registry
  member (organization)
    └─ subsystem (client identity)
         └─ service (provider identity)
              └─ ACL: which client subsystems/groups may call it

Security Server (per organization)
  owner member + server code
    ├─ authentication key/cert  → identifies the server to other servers
    ├─ signing key/cert         → signs messages for the member
    ├─ client subsystem(s)      → registered association with this SS
    └─ service description(s)   → WSDL / REST base path / OpenAPI

Information system
  consumer: sends X-Road-Client + service ID to its own SS
  provider: receives a normal HTTP/SOAP request from its SS
```

The CS does **not** forward traffic. It distributes configuration and approves registrations. The SS enforces access.

## Preconditions

Before registering anything, verify:

- The CS is initialized (instance identifier, admin user, software token).
- Required **member classes** exist (for example `GOV`, `BIZ`, `ORG`).
- An approved **CA** and **OCSP responder** are configured in the CS.
- An approved **TSA** is configured in the CS.
- The SS has the **configuration anchor** and can download global configuration.
- The SS and CS clocks are synchronized (NTP); certificate and OCSP checks are time-sensitive.
- The CS has an **internal** and an **external configuration signing key**. The
  initialization wizard creates the software token but not these keys; without them the
  CS reports `active key missing` and cannot publish global configuration.
- The CS has a **management service provider** configured (a registered subsystem). Until
  it is set, configuration generation fails on
  `managementService ... managementRequestServiceProviderId is expected`, because the
  private parameters must name the subsystem that serves management requests.

The upstream Docker dev stack performs all of these steps in `setup.hurl`. Use it as a working reference, not as a production procedure.

## Commissioning sequence

### 1. Register the member in the Central Server

CS UI → **Members** → **Add member**:

- member class
- member code
- member name

The member now exists in the registry, but it is not yet a Security Server client and has no services.

### 2. Create the subsystem

CS UI → member detail → **Subsystems** → **Add new subsystem**:

- subsystem code (for example `PORTAL`)

Alternatively, the SS registration request can introduce the subsystem. Explicit creation in the CS is easier to audit and is the recommended learning path.

### 3. Initialize the Security Server

SS UI → initialization:

- owner member class + code
- Security Server code (for example `SS-SMECU`)
- software token PIN

Then:

1. Import the **configuration anchor** from the CS.
2. Create an **authentication** key and CSR for the server.
3. Sign the CSR at the approved CA.
4. Import the authentication certificate.
5. Register the authentication certificate with the CS.
6. Approve the registration request in the CS (unless auto-approval is enabled).

The SS cannot participate in SS-to-SS TLS until its authentication certificate is registered and approved.

### 4. Register the subsystem as a Security Server client

SS UI → **Clients**:

1. Add the member/subsystem as a client.
2. Select or create the **signing** key and certificate for the member. Subsystems of the same member on the same SS share the member's signing certificate.
3. Submit the client registration request.
4. Approve the request in the CS.

A subsystem can be in these states:

| State | Meaning |
| --- | --- |
| `Saved` | Entered locally; association not registered in the CS. |
| `Registration in progress` | Request submitted; waiting for CS approval. |
| `Registered` | Approved; the subsystem can consume/provide subject to services and ACLs. |
| `Disabled` | Temporarily disabled by request; not usable. |
| `Global error` | The CS revoked the association; investigate in the CS. |
| `Deletion in progress` | Removal requested. |

**Trap:** rejection is not pushed to the SS. A rejected request can sit in "Registration in progress" indefinitely. Check the CS management requests view, not only the SS.

### 5. Publish the provider service

On the **provider's** SS, open the provider subsystem → **Services**:

- **SOAP:** add the WSDL URL.
- **REST base path:** add the base URL + service code.
- **OpenAPI 3:** add the OpenAPI document URL + service code. Endpoints are parsed automatically.

Then:

1. Set the **service URL** that the SS will call (the provider IS address, reachable from the SS).
2. Set the timeout.
3. Decide whether to verify the provider IS TLS certificate.
4. **Enable** the service description. New descriptions are disabled by default.
5. If using OpenAPI, confirm the parsed endpoint list is what you expect.

The service description URL and the service URL are different things:

- description URL → where the SS reads WSDL/OpenAPI metadata
- service URL → where the SS forwards actual calls

A service can be registered and enabled but still return "unknown service" if the caller's service code does not match the description.

### 6. Grant access

On the provider's SS, open the service or service description → **Access Rights** (or use **Service Clients**):

- add the consumer **subsystem** (for example `DEV/BIZ/SME-CU/PORTAL`), or a global/local group
- for REST, optionally grant access at **endpoint** level instead of service level

ACL rules:

- Access is denied by default. No subject means no callers.
- ACLs match the **service code**, not the service version.
- Endpoint-level ACLs are service-based only; they are not managed from the Service Clients view.
- Removing service-level access also removes endpoint-level access for that service.
- A local group belongs to one SS client; a global group is defined in the CS.

### 7. Verify

From the consumer network:

```bash
curl -i -sS \
  "http://<consumer-ss>:8080/r1/DEV/GOV/MOC/REGISTRY/businessLookup/businesses/123" \
  -H "X-Road-Client: DEV/BIZ/SME-CU/PORTAL"
```

Check:

- HTTP status and body from the provider.
- `X-Road-Id`, `X-Road-Service`, `X-Road-Request-Id`, `X-Road-Request-Hash` response headers.
- No `X-Road-Error` header.
- Provider SS service description is enabled.
- Consumer SS client state is `Registered`.
- Provider SS access-rights table contains the exact consumer subsystem.
- Message log on both SSs contains the request.

## Automation

The upstream dev stack drives the whole flow through the management REST APIs with Hurl. Useful patterns:

- Log in with `POST /login`, keep the `XSRF-TOKEN` cookie, send it as `X-XSRF-TOKEN`.
- CS API: `/api/v1/members`, `/api/v1/subsystems`, `/api/v1/management-requests/{id}/approval`, `/api/v1/certification-services`, `/api/v1/timestamping-services`.
- SS API: `/api/v1/clients`, `/api/v1/clients/{id}/service-descriptions`, `/api/v1/services/{id}`, `/api/v1/clients/{id}/service-clients/{clientId}/access-rights`, `/api/v1/service-descriptions/{id}/enable`.
- Registration is asynchronous: poll global configuration/state rather than assuming immediate success.

Read the exact sequence at tag `7.8.3`:

```bash
git -C /mnt/data/opensource/x-roads/X-Road show \
  7.8.3:development/hurl/scenarios/setup.hurl
```

## Checklist summary

- [ ] CS initialized; member classes, CA, OCSP, TSA configured.
- [ ] Member exists in the CS.
- [ ] Subsystem exists in the CS.
- [ ] SS initialized and auth certificate registered/approved.
- [ ] Subsystem added as SS client; signing cert present; client registration approved.
- [ ] Provider service description added, URL set, description enabled.
- [ ] Consumer subsystem granted access to the service/endpoint.
- [ ] Global configuration propagated to both SSs.
- [ ] End-to-end call succeeds with X-Road response headers.
