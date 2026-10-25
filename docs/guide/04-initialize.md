# 04 — Initialize X-Road

**Goal:** create the instance, members, Security Servers, certificates, services, and ACLs.
**Time:** 3–10 minutes; retries are normal.

## Run

```bash
cd /tmp/xroad-7.8.3/Docker/xrd-dev-stack
./local-dev-run.sh --initialize
```

What it does:

1. Starts the stack again (idempotent).
2. Runs the Hurl scenario `development/hurl/scenarios/setup.hurl` inside the `hurl` container.
3. Retries failed HTTP calls while the CS/SS start and global configuration propagates.

Do not abort on the first HTTP error in the output. The script is designed to retry.

## Expected

The command ends without a Hurl `FAILURE`, and the last request succeeds. You may see intermediate connection errors or 5xx responses during the run; those are expected while components start.

Check the containers again:

```bash
cd /tmp/xroad-7.8.3/Docker/xrd-dev-stack
docker compose -f compose.yaml -f compose.dev.yaml --env-file .env ps --all
```

The CS and both SS containers should be running/healthy. `hurl` should be gone or exited.

## What was created

- Instance `DEV`.
- Member classes `COM`, `GOV`, `ORG`.
- Member `DEV/COM/1234` (provider) and `DEV/COM/4321` (consumer).
- Management subsystem on `1234`, registered to SS0.
- Test CA, OCSP responder, and TSA approved in the CS.
- SS0 and SS1 initialized; authentication and signing certificates issued and registered.
- `TestService` registered on SS0 and `TestClient` on SS1.
- SOAP service `getRandom/v1` and other adapter operations.
- REST base-path service `mock1`.
- OpenAPI service `restapi` with an endpoint-level ACL.
- Access for `TestClient` to the demo services.

You will inspect these in [06-inspect-state.md](06-inspect-state.md).

## If it fails

| Symptom | Meaning | Fix |
| --- | --- | --- |
| Hurl ends with `FAILURE` on the first CS call | CS is not healthy yet or the port is wrong. | Check `docker compose ... ps` and `logs cs`; wait, then rerun. |
| `Connection refused` for a long time | A container is not running or the host port is not published. | Inspect `ps --all` and `logs <service>`; fix the container before rerunning. |
| Failure while adding a CA or TSA | Test CA is not healthy or the CA URL is wrong. | `docker compose ... logs testca`; check `http://localhost:4400/testca/certs`. |
| Failure registering an SS certificate | SS cannot reach the CS, or the CS has not approved the request. | Check `logs ss0`, `logs cs`, and the CS **Management Requests** page. |
| Failure adding a service | Provider IS (WSDL/OpenAPI URL) is not reachable from inside the SS container. | Check `docker exec ss0 curl -v <provider-url>`; fix the service URL. |
| Hurl succeeds but a later call fails | Initialization is done; the problem is now routing/ACL/protocol. | Continue to step 07, then use the failure lab. |
| The script is interrupted | State is partial. | Do not assume it is safe to continue. Check `ps`, then rerun `--initialize`; if a rerun fails on an already-created object, reset instead of guessing. |

If a rerun is not enough, reset with [12-reset.md](12-reset.md) and start from step 03. A clean reset is cheaper than guessing.

## Why this step exists

X-Road has no single "start" button. Initialization is a sequence of trust operations: approve a CA, issue certificates, register servers, register clients, publish services, grant access. Doing it manually teaches the sequence; the Hurl script is a working reference for the order.

After this step, the system is **configured**, but you have not yet proven that a message can flow.

## Next

[05-open-admin-ui.md](05-open-admin-ui.md)
