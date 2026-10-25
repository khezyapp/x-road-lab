# 05 — Open the Admin UIs

**Goal:** confirm the CS and both Security Servers are serving their UIs, and log in.
**Time:** 2 minutes.

## Run

Check the HTTP endpoints first:

```bash
curl -k -I https://localhost:4100/
curl -k -I https://localhost:4200/
curl -k -I https://localhost:4300/
```

`-k` is required because the dev stack uses self-signed certificates.

Open in a browser:

| Service | URL | Login |
| --- | --- | --- |
| Central Server | <https://localhost:4100/> | `xrd` / `secret` |
| Security Server SS0 | <https://localhost:4200/> | `xrd` / `secret` |
| Security Server SS1 | <https://localhost:4300/> | `xrd` / `secret` |

Accept the browser certificate warning. The token PIN used by the stack is `Secret1234`; you normally do not need to type it after initialization.

## Expected

- `curl` returns a `200`, `302`, or `401` response, not `Connection refused`.
- The browser shows an X-Road login page.
- After login:
  - CS: **Members**, **Security Servers**, **Management Requests**, **Configuration**.
  - SS0: **Clients** with `TestService`, **Services**, **Keys and Certificates**.
  - SS1: **Clients** with `TestClient`.

## If it fails

| Symptom | Meaning | Fix |
| --- | --- | --- |
| `Connection refused` | Container not running, or the port is not published. | `docker compose ... ps --all`; check `logs cs`, `logs ss0`, `logs ss1`. |
| `502 Bad Gateway` | The backend is still starting. | Wait 30–60 seconds and retry. |
| Browser warns about the certificate | Expected: self-signed dev certificate. | Proceed only for localhost. |
| Login fails with `xrd` / `secret` | Initialization did not complete, or the admin password differs. | Check the initialization output and CS logs; reset if needed. |
| Logging into SS0 breaks the SS1 session | Both UIs are on the same `localhost` cookie domain. | Use separate browser profiles/incognito windows, or open one UI at a time. |

The cookie/session collision is an upstream dev-stack limitation, not an X-Road configuration error. It is caused by running multiple admin UIs on `localhost` with different ports.

## Why this step exists

The UIs are the fastest way to inspect state that is otherwise spread across APIs and databases:

- CS: which members, subsystems, CAs, TSAs, and registration requests exist.
- SS: client state (`Saved`, `Registration in progress`, `Registered`), services, ACLs, keys, and diagnostics.

Before sending a request, know what "registered" and "enabled" look like in the UI.

## Next

[06-inspect-state.md](06-inspect-state.md)
