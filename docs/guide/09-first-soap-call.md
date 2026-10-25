# 09 — First SOAP Call

**Goal:** send a SOAP 4.0 request through the consumer Security Server.
**Time:** 3 minutes.

REST and SOAP use the same transport and the same Security Servers, but the consumer/provider must both use the same message protocol. The demo SOAP provider is `issoap`; the service is `getRandom/v1`.

## Run

```bash
curl -i -sS -X POST "http://localhost:4310/" \
  -H "Content-Type: text/xml" \
  --data-binary @- <<XML
<soapenv:Envelope
    xmlns:soapenv="http://schemas.xmlsoap.org/soap/envelope/"
    xmlns:xro="http://x-road.eu/xsd/xroad.xsd"
    xmlns:iden="http://x-road.eu/xsd/identifiers"
    xmlns:prod="http://test.x-road.fi/producer">
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
    <xro:id>ID-soap-$(date +%s)</xro:id>
    <xro:userId>EE1234567890</xro:userId>
    <xro:protocolVersion>4.0</xro:protocolVersion>
  </soapenv:Header>
  <soapenv:Body>
    <prod:getRandom>
      <prod:request/>
    </prod:getRandom>
  </soapenv:Body>
</soapenv:Envelope>
XML
```

## Expected

- HTTP `200`
- A SOAP envelope with a `<data>` element containing a number
- No `<soap:Fault>` / `<Fault>`
- X-Road response headers may be present, but SOAP evidence is primarily in the signed transport/message log

The response wrapper should correspond to `getRandomResponse` (document/literal-wrapped), not a hand-invented element.

## If it fails

| Symptom | Meaning | Check |
| --- | --- | --- |
| `404` / SOAP fault about service | Wrong service code/version, or the WSDL description is disabled. | SS0 → TestService → Services; service code `getRandom`, version `v1`. |
| Fault about the `client` header | Missing/invalid client identifier, or the subsystem is not registered. | `X-Road-Client` equivalent: `DEV/COM/4321/TestClient`; SS1 client state. |
| Fault about `service` | Missing service header or wrong provider subsystem. | Provider subsystem `DEV/COM/1234/TestService`. |
| Fault about body/wrapper | The body element does not match the operation. | Use `<prod:getRandom>` as above; check the WSDL. |
| `415 Unsupported Media Type` | Missing/incorrect `Content-Type`. | Use `text/xml` (the demo does). |
| `500` with `Server.ServerProxy.NetworkError` | SS0 cannot reach the SOAP adapter. | `docker exec ss0 curl -v "http://issoap:8080/example-adapter/Endpoint?wsdl"` |
| OK from direct adapter but not X-Road | You bypassed the SS or used the wrong IDs. | Call the SS proxy on 4310 with the SOAP headers. |

## Why this step exists

SOAP looks different but follows the same routing and trust model:

- The SS routes by the identifiers in the SOAP header.
- The body wrapper must match the WSDL operation.
- The provider must copy the X-Road headers into the response.
- The SS, not the app, signs and logs the message.

If REST works and SOAP fails, the problem is almost always the SOAP contract (service/version/body/header copying), not the network.

## Next

[10-failure-lab.md](10-failure-lab.md)
