# 08 — Read the Response Headers

**Goal:** learn to distinguish an X-Road response from a direct provider response.
**Time:** 3 minutes.

## Capture the headers and body separately

```bash
curl -sS -D /tmp/xroad-headers.txt -o /tmp/xroad-body.json -X POST \
  "http://localhost:4310/r1/DEV/COM/1234/TestService/mock1" \
  -H "X-Road-Client: DEV/COM/4321/TestClient" \
  -H "Content-Type: application/json" \
  -d '{"data":1.0,"service":"random"}'

cat /tmp/xroad-headers.txt
cat /tmp/xroad-body.json
```

## What the headers mean

| Header | What it proves |
| --- | --- |
| `X-Road-Client` | The consumer subsystem that the SS accepted. |
| `X-Road-Service` | The provider service ID that was routed to. |
| `X-Road-Id` | Unique ID of this X-Road message. |
| `X-Road-Request-Id` | ID to search for in the SS logs. |
| `X-Road-Request-Hash` | Hash binding this response to the request (evidence/tamper detection). |
| `X-Road-Error` | If present, X-Road/SS produced the error; if absent, the provider app did. |

The provider's own headers are separate. In this demo, WireMock sets `mock-response: true`; that header is not an X-Road header.

## Compare with a direct provider call

Call the provider IS directly, bypassing X-Road:

```bash
curl -i -sS -X POST "http://localhost:4500/integration/mock_1" \
  -H "Content-Type: application/json" \
  -d '{"data":1.0,"service":"random"}'
```

Expected:

- HTTP `200`
- `mock-response: true`
- **no** `X-Road-Client`, `X-Road-Service`, `X-Road-Id`, `X-Road-Request-Id`, or `X-Road-Request-Hash`

This is the key lesson: the provider app does not create the X-Road evidence headers. The Security Servers do. A direct call proves the app works but proves nothing about X-Road.

## If it fails

| Symptom | Meaning | Check |
| --- | --- | --- |
| Direct call fails but X-Road call works | You called the wrong host port, or the host port is not published. | The SS uses the container network (`isrest:8080`); host port 4500 is only for debugging. |
| X-Road call works but direct call returns 404 | You used the wrong method/path. | The mapping expects `POST /integration/mock_1`. |
| No X-Road headers on the X-Road call | You called the provider port, not the SS proxy. | Use `localhost:4310`, not `localhost:4500`. |
| `X-Road-Error` present | The error is in X-Road, not the provider. | Read the body `type`; use step 11 to find the detail UUID. |

## Why this step exists

A response body tells you the app worked. The X-Road headers tell you **which identities and service** the system actually used, and whether the error came from X-Road or the app. This distinction is the difference between debugging the right layer and guessing.

## Next

[09-first-soap-call.md](09-first-soap-call.md)
