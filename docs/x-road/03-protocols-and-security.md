# Protocols and Security

Status: **Verified** from `pr-rest_x-road_message_protocol_for_rest.md` (v1.0.4), `pr-mess_x-road_message_protocol.md` (v4.0.25), `pr-messtransp_x-road_message_transport_protocol.md` (v2.x), and `arc-sec_x_road_security_architecture.md`.

## Choose one protocol per service

X-Road supports REST and SOAP side by side, but **a single call cannot switch protocols**. If the consumer IS speaks REST to its Security Server, the provider IS must also speak REST. The Security Server does not convert REST to SOAP or vice versa.

- **REST** — preferred for new work and for CamDX. Simpler to build in Spring Boot, and CamDX states a preference for REST APIs.
- **SOAP** — required only when the provider exposes a WSDL/SOAP service. The message protocol is SOAP 4.0 with X-Road headers.

## REST contract

### Request

```http
{method} /r1/{instance}/{memberClass}/{memberCode}/{subsystemCode}/{serviceCode}[/path][?query]
X-Road-Client: {instance}/{memberClass}/{memberCode}/{subsystemCode}
```

Example:

```bash
curl -sS -X POST \
  "http://localhost:4310/r1/DEV/COM/1234/TestService/mock1" \
  -H "X-Road-Client: DEV/COM/4321/TestClient" \
  -H "Content-Type: application/json" \
  -d '{"data":1.0,"service":"random"}'
```

Notes:

- `X-Road-Client` is the **only mandatory X-Road header** in the request.
- The `r1` path segment is the protocol major version, not the service version.
- The service code is part of the URL; the path after it is forwarded to the provider.
- Each identifier component is percent-encoded separately; `/` separators stay literal.
- Optional X-Road headers: `X-Road-UserId`, `X-Road-Issue`, `X-Road-Represented-Party`, and `X-Road-Security-Server` (monitoring use).
- `Content-Type` and `Accept` are transported unmodified. HTTP redirects are not followed by default.

### Response

The provider SS adds:

| Header | Meaning |
| --- | --- |
| `X-Road-Client` | Client that initiated the call. |
| `X-Road-Service` | Service ID that was invoked. |
| `X-Road-Id` | Unique message ID. |
| `X-Road-Request-Id` | Unique request ID. |
| `X-Road-Request-Hash` | Base64 SHA-512 binding the response to the request. |
| `X-Road-Error` | Present only when X-Road itself produced the error. |

**Error triage:** if `X-Road-Error` is present, the failure happened in X-Road. If it is absent, the provider application produced the status/body. X-Road error bodies look like:

```json
{
  "type": "Server.ServerProxy.NetworkError",
  "message": "Connect to 10.0.0.5:8080 failed: Connection refused",
  "detail": "<uuid>"
}
```

The `type` prefix narrows the location:

- `Client.*` — malformed request or client-side input.
- `Server.ClientProxy.*` — the consumer Security Server.
- `Server.ServerProxy.*` — the provider Security Server.

The `detail` UUID appears in the SS logs; search for it.

## SOAP contract

A request envelope carries the X-Road headers in the SOAP header and the application payload in the body:

```xml
<soapenv:Envelope
    xmlns:soapenv="http://schemas.xmlsoap.org/soap/envelope/"
    xmlns:xro="http://x-road.eu/xsd/xroad.xsd"
    xmlns:iden="http://x-road.eu/xsd/identifiers">
  <soapenv:Header>
    <xro:client iden:objectType="SUBSYSTEM">
      <iden:xRoadInstance>DEV</iden:xRoadInstance>
      <iden:memberClass>COM</iden:memberClass>
      <iden:memberCode>4321</iden:memberCode>
      <iden:subsystemCode>TestClient</iden:subsystemCode>
    </xro:client>
    <xro:service iden:objectType="SERVICE">
      <iden:xRoadInstance>DEV</iden:xRoadInstance>
      <iden:memberClass>COM</iden:memberClass>
      <iden:memberCode>1234</iden:memberCode>
      <iden:subsystemCode>TestService</iden:subsystemCode>
      <iden:serviceCode>getRandom</iden:serviceCode>
      <iden:serviceVersion>v1</iden:serviceVersion>
    </xro:service>
    <xro:id>ID11234</xro:id>
    <xro:userId>EE1234567890</xro:userId>
    <xro:protocolVersion>4.0</xro:protocolVersion>
  </soapenv:Header>
  <soapenv:Body>
    <prod:getRandom xmlns:prod="http://test.x-road.fi/producer">
      <prod:request/>
    </prod:getRandom>
  </soapenv:Body>
</soapenv:Envelope>
```

Rules:

- `client` and `protocolVersion` are mandatory; `protocolVersion` must be `4.0`.
- `service` is mandatory on requests; `id` is mandatory (UUID recommended).
- The provider **must copy every request header into the response, in the same order and with the same values**. Do not invent a new client/service header in the response.
- The body uses document/literal-wrapped encoding. The request wrapper element name must match `serviceCode`; the response wrapper is normally `<serviceCode>Response`.
- `Content-Type` and `SOAPAction` HTTP headers are preserved. All other HTTP headers are dropped.
- Attachments use MTOM or swaRef and trigger batch signatures. Do not build them by hand unless the provider contract requires it.

## Transport and evidence

```text
Consumer IS ──HTTP/HTTPS──► Consumer SS
Consumer SS ──mutual TLS 5500──► Provider SS
Provider SS ──HTTP/HTTPS──► Provider IS
```

- **SS-to-SS transport** is HTTP over mutually authenticated TLS on **5500/tcp**. Each side presents its authentication certificate; the other side checks that it was issued by an approved CA, is registered in the global configuration, and has a valid OCSP response.
- **OCSP retrieval** between Security Servers uses **5577/tcp**. A client SS downloads missing OCSP responses from the provider SS before it accepts the provider's certificate chain.
- **Message signatures** are created by the member's signing key on the transport message, not by the application. The provider SS verifies the signature before forwarding.
- **Time-stamping** is asynchronous. The SS logs messages and periodically obtains RFC 3161 timestamps; a short TSA outage does not immediately stop message exchange. Wire format and Java snippets: [RFC 3161 time-stamping cheatsheet](12-rfc3161-timestamping.md).
- **Message log** stores request/response metadata and, by default, bodies. It is the evidence trail. Bodies can be disabled or encrypted, and archive retention is configurable.
- **Global configuration** is signed by the CS and verified by each SS. The configuration anchor is the initial trust input.

### Ports to remember (v7)

| Port | Direction | Purpose |
| --- | --- | --- |
| `5500/tcp` | SS ↔ SS | Message transport (mutual TLS). |
| `5577/tcp` | SS → SS | OCSP response retrieval. |
| `4000/tcp` | admin → CS/SS | Admin UI / management REST API; bind to a trusted network only. |
| `4001/tcp` | SS → CS | Authentication certificate registration. |
| `80/443/tcp` | SS → CS | Global configuration download. |
| `4002/tcp` | management SS → CS | Management services. |
| `8080`, `8443/tcp` | IS → SS | Information-system access point. |
| `5500/5577` | monitoring → SS | Monitoring access when enabled. |

X-Road 8 adds internal gRPC and OpenBao ports; see [05-versions-and-camdx.md](05-versions-and-camdx.md).

## Service descriptions

A service must be described before it can be called:

- **REST base path** — provide the provider base URL and a service code. Fine-grained endpoints can be added manually for endpoint-level ACLs.
- **OpenAPI 3** — provide the OpenAPI document URL and a service code. Endpoints are parsed automatically; endpoint-level ACLs are available.
- **WSDL** — provide the WSDL URL. Service code and version come from the WSDL (`binding/operation/@name`, `xrd:version`).

The Security Server fetches the description; the description URL must be reachable **from the SS container**, not from your laptop. A description can be valid but disabled; a disabled description returns an SS-generated error to callers.

## Internal information-system connection

For the IS-to-SS hop, the SS client has a configurable connection type:

- **HTTP** — only for an isolated private network.
- **HTTPS NOAUTH** — TLS without client certificates; the IS must trust the SS certificate.
- **HTTPS (client auth)** — default for new clients; the IS presents a certificate that is uploaded to the SS.

For the first local lab, HTTP inside the Docker network is acceptable. For anything resembling production, use HTTPS with client authentication and manage the internal TLS certificates explicitly.

## Protocol-related mistakes to avoid

- Sending `X-Road-Client` with a member ID instead of a subsystem ID.
- Calling the provider IS URL directly and expecting X-Road headers or ACLs to apply.
- Mixing REST on one side and SOAP on the other.
- Forgetting to copy SOAP headers in the provider response.
- Assuming `Content-Type` or `Accept` is optional on a body-carrying REST call; it is recommended and is preserved when present.
- Treating a provider application 4xx/5xx as an X-Road failure. Check `X-Road-Error` first.
