# Troubleshooting

Status: **Verified failure modes** from the X-Road protocols/manuals and the upstream dev stack. Exact log text varies by version; use the checks, not a string match.

## Triage order

Work from the outside in. Do not change application code until steps 1–5 pass.

1. **Identifiers** — instance, member, subsystem, service code, version, path.
2. **Client registration** — is the consumer subsystem `Registered` on its SS?
3. **Service** — is the description enabled, and is the service code present?
4. **ACL** — is the exact consumer subsystem granted access?
5. **Transport** — can the SS reach the provider IS? Are certs/OCSP/time valid?
6. **Application** — only now inspect the Spring controller/service.
7. **Global configuration** — has the change propagated to both SSs?

## Symptom matrix

| Symptom | Most likely cause | Check |
| --- | --- | --- |
| `404` / "unknown service" / "service not found" | Wrong service code, wrong provider subsystem, or disabled service description. | SS → provider subsystem → Services; compare the caller path component-by-component. |
| `500` with `X-Road-Error: ...access_denied` (occasionally `403`) | Consumer subsystem missing from the service ACL, or wrong `X-Road-Client`. | Provider SS → service → Access Rights; compare exact subsystem ID. |
| `500` with `Server.ServerProxy.NetworkError` | Provider SS cannot reach the provider IS, or the connection timed out. | From the SS container: `curl -v <service-url>`. Check the service URL and Docker DNS. |
| `500` with `Server.ServerProxy.ServiceFailed` | Provider returned an error that the SS classified as a service failure. | Provider app logs; call the provider directly. |
| `500` with `Server.ServerProxy.DatabaseError` | SS database problem. | SS logs; container health; disk. |
| `503` / `Server.ClientProxy.OutdatedGlobalConf` | Global configuration not current. | SS Diagnostics; `configuration_client.log`; wait for propagation. |
| TLS handshake / certificate error | Auth certificate not registered/approved, CA not trusted, wrong hostname, or expired cert. | SS auth cert state; CS approved CA; cert SAN vs address; `openssl s_client`. |
| OCSP error | OCSP responder unreachable or response expired; SS clock wrong. | SS Diagnostics; `signer.log`; `timedatectl`; test CA health. |
| SOAP fault about headers/body | Provider did not copy request headers, or wrapper element does not match service code. | Provider SOAP response; WSDL; xrd4j adapter logs. |
| `405` / `415` on REST | Method/path not in the service description, or content type not accepted. | Service endpoints; provider controller mapping. |
| Call works from host but not from SS | Service URL uses `localhost` or a host port that is not reachable inside the SS container. | Use the container name + internal port (`http://moc-registry:8080/api`). |
| Registration stuck in "Registration in progress" | CS rejected the request without notifying the SS, or approval is still pending. | CS → Management Requests; look for `Rejected`/`Pending`. |
| CS/SS UI unreachable | Container not healthy, port not published, or wrong scheme (`https`, not `http`). | `docker compose ps`; `curl -k https://localhost:<port>/`. |
| Everything worked, then stopped | Global configuration refresh, expired OCSP, clock drift, or a certificate rotation. | SS Diagnostics and logs; compare timestamps. |

## Error taxonomy (REST)

The `X-Road-Error` response header is the fastest discriminator:

- **Present** → X-Road produced the error. Read the JSON `type` and `detail`.
  - `Client.*` — request/consumer input.
  - `Server.ClientProxy.*` — consumer SS.
  - `Server.ServerProxy.*` — provider SS.
- **Absent** → the provider application produced the status and body. Debug the provider.

Always capture:

```text
X-Road-Error
X-Road-Id
X-Road-Request-Id
X-Road-Service
response body (contains detail UUID for X-Road errors)
```

Search the SS logs for the detail UUID.

## Commands

### Container and UI health

```bash
cd /tmp/xroad-7.8.3/Docker/xrd-dev-stack
docker compose -f compose.yaml -f compose.dev.yaml ps
docker compose -f compose.yaml -f compose.dev.yaml logs --tail=200 ss0
docker compose -f compose.yaml -f compose.dev.yaml logs --tail=200 cs
```

The admin UI uses HTTPS:

```bash
curl -k -I https://localhost:4200/
curl -k -I https://localhost:4100/
```

### X-Road logs

```bash
docker exec ss0 sh -c 'ls -l /var/log/xroad'
docker exec ss0 sh -c 'tail -n 200 /var/log/xroad/proxy.log'
docker exec ss0 sh -c 'tail -n 200 /var/log/xroad/configuration_client.log'
docker exec ss0 sh -c 'tail -n 200 /var/log/xroad/signer.log'
```

Typical locations:

- `proxy.log` — message routing, ACL, provider connection errors.
- `configuration_client.log` — global configuration download/verification.
- `signer.log` — keys, certificates, OCSP, signing.
- `clientproxy_access.log` / `serverproxy_access.log` — request-level access logs.
- `messagelog` — database/archive issues (may be separate).

### Reachability from the Security Server

Do not test from the host. Test from the container that must make the call:

```bash
docker exec ss0 sh -c 'curl -v --max-time 5 http://isrest:8080/integration/mock_1'
docker exec ss0 sh -c 'getent hosts isrest'
```

For the lab, replace `isrest` with `moc-registry` and the path with the provider endpoint.

### Time

X-Road certificates and OCSP responses are time-sensitive:

```bash
timedatectl
docker exec ss0 date -u
```

Fix host NTP before debugging certificate errors.

### CS management requests

If a client or auth certificate is stuck:

1. Open the CS UI → **Management Requests**.
2. Check for `Pending` or `Rejected`.
3. Approve a pending request, or fix and resubmit after a rejection.
4. Remember that the SS is not notified of rejection automatically.

## Failure patterns worth memorizing

### "It works with curl directly to the provider, but not through X-Road"

The provider URL is reachable from your laptop, not from the SS. Use the container name and internal port. Check the SS service URL, not the host port.

### "The service is enabled, but the call says unknown service"

The path uses a different service code than the description. The service code is in the URL (`/r1/.../<serviceCode>/...`), not the OpenAPI operation ID. Compare the SS service list with the curl path.

### "ACL looks right, but access is denied"

The ACL subject must match the **full subsystem ID** (`DEV/BIZ/SME-CU/PORTAL`), including the instance. A member ID is not a subsystem ID. Also check whether the grant is service-level vs endpoint-level for REST.

### "Registration is green in the SS but calls fail"

The SS client state can be `Registered` while global configuration is stale, or while the provider service description is disabled. Check both sides, not just the consumer.

### "SOAP works for one operation and fails for another"

The WSDL may define multiple operations with different wrapper element names. The request wrapper must match the `serviceCode`; the response wrapper is normally `<serviceCode>Response`. A hand-written adapter often copies headers but not the wrapper.

### "Everything fails after a restart"

The dev stack has no persistent volumes by default. A restart may recreate containers with fresh databases and tokens. Re-run initialization, or add volumes deliberately.

## Evidence to collect before asking for help

- Exact request: method, URL, `X-Road-Client`, body, content type.
- Response: status, `X-Road-Error`, body, request IDs.
- Consumer SS client state and provider SS service/ACL state.
- `proxy.log` / `configuration_client.log` / `signer.log` snippets with the detail UUID.
- Whether the CS and both SSs have the same global configuration version.
- `timedatectl` output and certificate validity dates.

This turns "X-Road is broken" into a specific layer.
