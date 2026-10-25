# X-Road Manual Lab Guide

This guide is a sequence of small, copy/paste steps for running the upstream **X-Road 7.8.3 Docker dev stack** by hand. Each step is its own file so you can read one screen at a time.

The goal is not just to make it work. Each step tells you:

- what to run,
- what success looks like,
- what a missing result usually means,
- why the step exists.

## What you will have at the end

A local X-Road instance with:

- a Central Server (`cs`),
- two Security Servers (`ss0`, `ss1`),
- a test CA/OCSP/TSA (`testca`),
- a SOAP provider (`issoap`),
- a REST provider (`isopenapi` and `isrest`),
- demo members and services already registered by an initialization script.

You will send a REST call and a SOAP call through the consumer Security Server, inspect the X-Road headers, deliberately break things, and read the logs.

This is **not** the three-organization lab yet. It is the smoke test that teaches the mechanics first. The three-org lab is the next step.

## Status

- The commands and file paths are verified against upstream tag `7.8.3`.
- The Compose configuration was validated with `docker compose config`.
- The full stack has **not yet been executed end-to-end on this machine**. If a result differs, record it here; do not pretend it worked.

## How to use this guide

1. Run the files in order. Do not skip the failure lab.
2. Every command block starts with a `cd` or is run from the stated directory.
3. Compare your output with **Expected**. If it differs, read **If it fails** before changing anything.
4. Keep the terminal output; it is your evidence.
5. When you change a fact, update the page.

## Steps

| Step | File | What it answers |
| --- | --- | --- |
| 01 | [01-prerequisites.md](01-prerequisites.md) | Are Docker, Compose, CPU architecture, and resources ready? |
| 02 | [02-get-source.md](02-get-source.md) | How do I get X-Road 7.8.3 without using the v8 `develop` tree? |
| 03 | [03-start-stack.md](03-start-stack.md) | How do I start the containers and check what is running? |
| 04 | [04-initialize.md](04-initialize.md) | How do I create the instance, members, servers, certificates, services, and ACLs? |
| 05 | [05-open-admin-ui.md](05-open-admin-ui.md) | How do I log in to the Central Server and Security Servers? |
| 06 | [06-inspect-state.md](06-inspect-state.md) | What did initialization actually create? |
| 07 | [07-first-rest-call.md](07-first-rest-call.md) | How do I make the first successful REST call? |
| 08 | [08-read-response-headers.md](08-read-response-headers.md) | What do the X-Road response headers prove? |
| 09 | [09-first-soap-call.md](09-first-soap-call.md) | How does the same flow work with SOAP? |
| 10 | [10-failure-lab.md](10-failure-lab.md) | What do wrong service, wrong client, ACL, and provider-down errors look like? |
| 11 | [11-logs-and-evidence.md](11-logs-and-evidence.md) | Where do I find the request ID, error UUID, and cause? |
| 12 | [12-reset.md](12-reset.md) | How do I wipe the lab and start again? |
| 13 | [13-next-steps.md](13-next-steps.md) | How do I move from the demo to the SME-CU/MOC/CAMD-X lab? |

## Safety and scope

- Everything runs on localhost with test credentials (`xrd` / `secret`, token PIN `Secret1234`).
- The test CA is not trusted by CamDX. Do not use these certificates or keys outside the lab.
- Do not expose the admin UI or proxy ports to the public internet.
- The stack is disposable. `down -v` is expected during learning.
- Never edit `/mnt/data/opensource/x-roads/X-Road`; this guide extracts a copy under `/tmp`.

## If you only have five minutes

Run steps 01–04, then step 07. If the REST call returns `"Hello, world from POST service!"`, the core works. Come back for the failure lab and logs.
