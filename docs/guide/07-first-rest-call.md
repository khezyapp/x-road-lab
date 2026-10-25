# 07 — First REST Call

**Goal:** send a request through the consumer Security Server and get a response from the provider information system.
**Time:** 1 minute.

## The call

```bash
curl -i -sS -X POST \
  "http://localhost:4310/r1/DEV/COM/1234/TestService/mock1" \
  -H "X-Road-Client: DEV/COM/4321/TestClient" \
  -H "Content-Type: application/json" \
  -d '{"data":1.0,"service":"random"}'
```

Read the URL from right to left:

```text
http://localhost:4310              consumer Security Server (SS1 proxy)
/r1                                REST message protocol version
/DEV/COM/1234/TestService          provider subsystem
/mock1                             provider service code
```

`X-Road-Client` is the **consumer subsystem** (`DEV/COM/4321/TestClient`). It is not the provider ID and not a member ID.

## Expected

HTTP `200` and a body containing:

```json
{"message":"Hello, world from POST service!"}
```

The response should also include headers like:

```http
X-Road-Client: DEV/COM/4321/TestClient
X-Road-Service: DEV/COM/1234/TestService/mock1
X-Road-Id: <uuid>
X-Road-Request-Id: <uuid>
X-Road-Request-Hash: <base64>
```

There should be **no** `X-Road-Error` header.

## If it fails

| Symptom | Meaning | Check |
| --- | --- | --- |
| `Connection refused` | SS1 is not running or 4310 is not published. | `docker compose ... ps --all`; `logs ss1`. |
| `404` / unknown service | Service code/path does not match the provider description, or the description is disabled. | SS0 → TestService → Services; compare `mock1` and its state. |
| `500` with `X-Road-Error: ...access_denied` | `TestClient` is not in the ACL, or the client is not registered. | SS0 → mock1 → Access Rights; SS1 → TestClient state. |
| `500` with `X-Road-Error: Server.ServerProxy.NetworkError` | SS0 cannot reach `http://isrest:8080/integration/mock_1`. | `docker exec ss0 curl -v http://isrest:8080/integration/mock_1`; `logs isrest`. |
| `500` with no `X-Road-Error` | The provider application (WireMock) returned an error. | `logs isrest`; call the provider directly. |
| `X-Road-Error: Server.ClientProxy.OutdatedGlobalConf` | SS1 has not received current global configuration. | Wait; check SS1 **Diagnostics** and `configuration_client.log`. |
| TLS/certificate error | A certificate, OCSP, or clock problem. | SS **Diagnostics**; `signer.log`; `timedatectl`. |

Use [09-troubleshooting.md](../x-road/09-troubleshooting.md) for the full matrix. Do not change the Spring Boot app yet — there is no Spring Boot app in this demo.

## Why this step exists

This is the first proof that all of these are true at once:

- SS1 is a registered client.
- The service ID resolves to SS0.
- SS0 has the `mock1` description enabled.
- The ACL allows `TestClient`.
- SS0 can reach the provider URL.
- SS0 and SS1 trust each other's certificates.

If this call works, the X-Road plumbing is correct. Later application failures are then clearly application-level.

## Next

[08-read-response-headers.md](08-read-response-headers.md)
