# 10 — Failure Lab

**Goal:** break the working call on purpose and learn what each failure means.
**Time:** 10 minutes.

Do this while the successful calls still work. The point is to recognize the error shape, not to memorize a status code.

> Exact status codes and `type` strings vary by X-Road version. The invariant is **whether `X-Road-Error` is present**, and **which Security Server logs the detail UUID**.

## Decision tree first

```text
Did you get an HTTP response at all?
  NO  → network/port/container problem (connection refused, timeout)
  YES → read X-Road-Error:
          absent  → the provider application produced the status/body
          present → X-Road produced the error:
                    Client.*             request/consumer input
                    Server.ClientProxy.* consumer Security Server
                    Server.ServerProxy.* provider Security Server
```

A **missing** `X-Road-Error` is information: X-Road did its job and the provider app failed. A **missing response** is also information: you never reached the SS.

## Experiment A — wrong service code

```bash
curl -i -sS -X POST \
  "http://localhost:4310/r1/DEV/COM/1234/TestService/noSuchService" \
  -H "X-Road-Client: DEV/COM/4321/TestClient" \
  -H "Content-Type: application/json" \
  -d '{}'
```

**Expected:** an X-Road error response. `X-Road-Error` is present; the body has a `type`, `message`, and usually a `detail` UUID.

**Why:** the consumer SS may route the provider subsystem, but the provider SS cannot find a service description for `noSuchService`. This is a **service description** problem, not an ACL problem.

**Fix:** compare the path service code with SS0 → TestService → Services. Check that the description is enabled.

## Experiment B — unknown consumer

```bash
curl -i -sS -X POST \
  "http://localhost:4310/r1/DEV/COM/1234/TestService/mock1" \
  -H "X-Road-Client: DEV/COM/9999/NOBODY" \
  -H "Content-Type: application/json" \
  -d '{"data":1.0,"service":"random"}'
```

**Expected:** an X-Road error; `X-Road-Error` is present. The message mentions an unknown client/subsystem or access denial.

**Why:** the consumer SS only accepts clients registered to it. `DEV/COM/9999/NOBODY` is not a client on SS1, so the call never becomes a valid X-Road message.

**Fix:** use the registered consumer `DEV/COM/4321/TestClient`. This is **not** fixed by adding an ACL on SS0; the failure happens before the provider.

## Experiment C — provider down

Stop the REST provider:

```bash
cd /tmp/xroad-7.8.3/Docker/xrd-dev-stack
docker compose -f compose.yaml -f compose.dev.yaml --env-file .env stop isrest
```

Call the same working REST request:

```bash
curl -i -sS -X POST \
  "http://localhost:4310/r1/DEV/COM/1234/TestService/mock1" \
  -H "X-Road-Client: DEV/COM/4321/TestClient" \
  -H "Content-Type: application/json" \
  -d '{"data":1.0,"service":"random"}'
```

**Expected:** an X-Road error, typically `Server.ServerProxy.NetworkError` (or a related provider-side failure). The `detail` UUID points to the SS0 logs.

**Why:** SS1 and SS0 are fine; SS0 cannot connect to `http://isrest:8080/integration/mock_1`. The provider is down, but the failure is reported by X-Road, so `X-Road-Error` is present.

Restore it:

```bash
cd /tmp/xroad-7.8.3/Docker/xrd-dev-stack
docker compose -f compose.yaml -f compose.dev.yaml --env-file .env start isrest
```

Wait a few seconds and retry the call.

## Experiment D — remove the ACL (optional, UI)

This is the cleanest way to see an access-control failure.

1. Open SS0: <https://localhost:4200/>.
2. Go to **Clients** → `DEV/COM/1234/TestService` → **Services**.
3. Expand the `mock1` REST description, click the service code.
4. Open **Access Rights**.
5. Remove `DEV/COM/4321/TestClient`.
6. Run the working REST call again.

**Expected:** an X-Road error indicating access denied/unauthorized. `X-Road-Error` is present.

**Why:** the service exists and SS0 can reach the provider, but the provider SS refuses the consumer subsystem. This is an **ACL** problem, not a network or service-description problem.

Restore the access right:

1. Same **Access Rights** view → **Add Subjects**.
2. Search for `DEV/COM/4321/TestClient`.
3. Add it and save.
4. Retry the call.

If you skip restoring, later steps will fail. Record that you changed it.

## Experiment E — wrong REST method (optional)

Call the `mock1` service with `GET` instead of `POST`:

```bash
curl -i -sS "http://localhost:4310/r1/DEV/COM/1234/TestService/mock1" \
  -H "X-Road-Client: DEV/COM/4321/TestClient"
```

**Expected:** the provider (WireMock) returns a method-related error. `X-Road-Error` is **absent** because X-Road successfully delivered the request; the provider rejected it.

**Why:** this is the counter-example to A–D. It shows what an application error looks like versus an X-Road error.

## Summary table

| Experiment | Error source | `X-Road-Error` | First place to look |
| --- | --- | --- | --- |
| Wrong service code | Provider SS service description | Present | SS0 → Services |
| Unknown consumer | Consumer SS client registry | Present | SS1 → Clients |
| Provider down | Provider SS → provider IS connection | Present | SS0 logs; `isrest` container |
| ACL removed | Provider SS access control | Present | SS0 → service → Access Rights |
| Wrong HTTP method | Provider application | Absent | Provider logs/contract |

## Why this step exists

Production debugging is mostly classification. Once you know whether the failure is network, client registration, service description, ACL, transport, or application, the fix is usually obvious. This lab gives you one example of each shape while the environment is small and disposable.

## Next

[11-logs-and-evidence.md](11-logs-and-evidence.md)
