# 06 — Inspect What Initialization Created

**Goal:** connect the X-Road concepts to the actual objects in the CS/SS UIs.
**Time:** 5 minutes.

Do this before sending a request. If an object is missing or in the wrong state, the call will fail for a reason you can predict.

## Central Server — <https://localhost:4100/>

Check:

| Where | Expected |
| --- | --- |
| **Members** | `DEV/COM/1234` ("Test member") and `DEV/COM/4321` ("Test client"). |
| **Member classes** | `COM`, `GOV`, `ORG`. |
| **Security Servers** | `SS0` owned by `1234`, `SS1` owned by `4321`. |
| **Management Requests** | The demo requests should be approved, not pending. |
| **Configuration / trust services** | The test CA, OCSP responder, and TSA are present. |

Open member `1234` → **Subsystems**:

- `MANAGEMENT`
- `TestService`
- `TestSaved` (created by the setup's registration tests)

Open member `4321` → **Subsystems**:

- `TestClient`
- `test-consumer` (test fixture)

## Security Server SS0 — <https://localhost:4200/>

SS0 is the **provider** and management server.

Check **Clients**:

| Client | Expected state | Role |
| --- | --- | --- |
| `DEV/COM/1234` (owner) | Registered | Server owner |
| `DEV/COM/1234/MANAGEMENT` | Registered | Management services |
| `DEV/COM/1234/TestService` | Registered | Service provider |
| `DEV/COM/1234/TestSaved` | Saved or Registered | Registration test fixture |

Open `TestService` → **Services**. You should see:

- a WSDL description with `getRandom`, `helloService`, `listPeople`, `personDetails`;
- a REST description with service code `mock1`;
- an OpenAPI description with service code `restapi`.

Each description should be **enabled**. A description that is present but disabled returns an error to callers even though it looks configured.

Open `mock1` → **Access Rights**. `DEV/COM/4321/TestClient` should be listed.

## Security Server SS1 — <https://localhost:4300/>

SS1 is the **consumer**.

Check **Clients**:

| Client | Expected state |
| --- | --- |
| `DEV/COM/4321` (owner) | Registered |
| `DEV/COM/4321/TestClient` | Registered |
| `DEV/COM/4321/test-consumer` | Registered or test-fixture state |

`TestClient` is the subsystem that the REST/SOAP calls will use in `X-Road-Client` / the SOAP `client` header.

## Record your state

Fill this in while the environment is fresh. It makes later failures much easier to diagnose.

```text
CS members:                 ______________________________
SS0 owner / server code:    ______________________________
SS1 owner / server code:    ______________________________
Provider subsystem:         ______________________________
Consumer subsystem:         ______________________________
REST service code:          ______________________________
SOAP service code/version:  ______________________________
ACL subject on mock1:       ______________________________
```

## If something is missing

| Missing / wrong | Likely cause | What to do |
| --- | --- | --- |
| Member absent in CS | Initialization stopped before adding members. | Re-run step 04; check CS logs. |
| SS absent in CS | Authentication certificate was never registered/approved. | SS → Keys and Certificates; CS → Management Requests. |
| Client state `Saved` | Client was added but not registered. | SS → Clients → Register; approve in CS. |
| Client state `Registration in progress` | Request is pending or was rejected without notification. | CS → Management Requests. |
| Service description disabled | Default state after adding; never enabled. | SS → Services → enable it. |
| ACL empty | No consumer was granted access. | SS → service → Access Rights. |
| Service URL points at `localhost` | Inside the SS container, `localhost` is the SS. | Use the provider container name and internal port. |

## Why this step exists

The rest of the guide assumes this state. If you skip it, a later "404" or "access denied" looks like a protocol bug when it is really a missing registration or a disabled description.

You are also learning the difference between:

- **registered** (the association exists in the CS),
- **enabled** (the service description is active),
- **allowed** (the ACL grants the consumer access).

All three are required.

For the manual registration sequence behind this state, see [04-registration-and-access.md](../x-road/04-registration-and-access.md). The demo's Hurl script is the automated version of that sequence.

## Next

[07-first-rest-call.md](07-first-rest-call.md)
