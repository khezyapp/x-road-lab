# AGENTS.md

## What this is
- A learning lab for X-Road, not a production deployment. End goal: a local X-Road ecosystem plus 2–3 simulated organizations (SME-CU and the CamDX-facing entities) whose information systems are Spring Boot 4 apps.
- Current state: README, knowledge base, manual lab guide, skill, and the `member-services` composite build are in place; no `.git` yet. The services build and test on this host.

## Read first
- [`docs/index.md`](docs/index.md) is the knowledge-base index (also the VitePress home page). Read the page that matches the question before answering from memory.
- [`.opencode/skills/x-road/SKILL.md`](.opencode/skills/x-road/SKILL.md) is the X-Road skill. Load it for X-Road/CamDX questions, integration work, lab setup, or debugging.
- [`docs/x-road/13-member-services.md`](docs/x-road/13-member-services.md) describes the implemented Spring Boot consumer/provider pair.
- Key decisions live in [`docs/decisions/0001-x-road-version.md`](docs/decisions/0001-x-road-version.md).

## Docs site (VitePress)
- The site root is `docs/`; config is `docs/.vitepress/config.mts`. Package scripts live in the root `package.json`.
- `npm install` once, then `npm run docs:dev` (default http://localhost:5173), `npm run docs:build`, `npm run docs:preview`.
- Routes come from the file tree: `docs/index.md` → `/`, `docs/guide/index.md` → `/guide/`. VitePress does **not** map `README.md` to a directory index, so index pages must be named `index.md`.
- Adding a page means adding it to the `sidebar` in `docs/.vitepress/config.mts` as well.
- The build fails on dead links. Lab endpoints (`http://localhost:4100` etc.) are exempt via `ignoreDeadLinks`; do not widen that exemption.

## Version policy
- **Lab target: X-Road 7.8.x.** CamDX's fork is on **7.8.2**; upstream stable and public docs are **7.8.3**.
- The local upstream clone `/mnt/data/opensource/x-roads/X-Road` is on `develop` = **8.0.0-beta2**, unreleased. Its working tree is **not** v7 source. Read v7 with `git show 7.8.3:<path>` or extract tag `7.8.3`.
- X-Road 8 uses Java 25, Spring Boot 4.1.1, OpenBao, and gRPC; keep it as a separate experiment. Do not mix v7/v8 images, ports, or manuals.
- Prefer **REST** for new integrations; use SOAP only for WSDL providers.

## Reference material (read-only)
- Live docs: <https://docs.x-road.global/> (7.8.3).
- Upstream checkout: `/mnt/data/opensource/x-roads/` (`X-Road/` is the main repo; siblings are separate projects). Treat it as read-only; copy needed config into this repo.
- v7 Docker dev stack: `X-Road/Docker/xrd-dev-stack` at tag `7.8.3` (not in the `develop` tree). Step-by-step manual guide: `docs/guide/index.md`.
- Upstream commissioning automation (the best executable spec): `git -C /mnt/data/opensource/x-roads/X-Road show 7.8.3:development/hurl/scenarios/setup.hurl`.
- X-Road 8 dev infrastructure (only for v8 work): `X-Road/development/docker/` and `X-Road/scripts/env-lxd/`.

## Host environment (verified 2026-10-23)
- Ubuntu 26.04, JDK 25.0.2, Maven 3.9.12, Gradle 9.2.1, Docker 29.x + Compose v5, 61 GiB RAM.
- LXD and Ansible are **not installed**; the upstream `env-lxd` path needs both.
- The X-Road CS/SS containers are Ubuntu 24.04-based, which is fine on this newer host. The SS compose uses `localhost:5555/...` images only for v8, not the v7 dev stack.

## Member services (implemented)
- `member-services/` is a Gradle **composite build** (`settings.gradle` uses `includeBuild`), containing independent builds `moc-registry` (provider, port 8081) and `sme-cu-portal` (consumer, port 8080).
- Commands: `cd member-services && ./gradlew buildAll` and `./gradlew testAll`. Run one service with `./gradlew -p moc-registry bootRun` or `./gradlew -p sme-cu-portal bootRun`.
- Direct mode is the default (`portal.xroad.enabled=false`) and calls `http://localhost:8081/api`. X-Road mode uses `PORTAL_XROAD_ENABLED=true` plus the `PORTAL_XROAD_*` settings in `sme-cu-portal/src/main/resources/application.yml`.
- Spring Boot 4.1.1 + Java 25. `spring-boot-starter-webmvc` does not auto-configure an injectable `RestClient.Builder` here; the consumer builds one with `RestClient.builder()`.

## Lab layout (planned)
- `infra/` — compose files, CA/certs, seed and reset scripts, `.env.example`.
- `member-services/` — implemented Spring Boot 4 information systems: `moc-registry` provider and `sme-cu-portal` consumer.
- `docs/` — knowledge base, topology, runbook, per-step notes, ADRs; also the VitePress site root (`.vitepress/` config, `index.md` route indexes).
- Keep member codes, subsystem codes, service codes, and Security Server mappings in `docs/x-road/02-identifiers.md` and `docs/x-road/06-lab-topology.md`; update both together.

## Working rules
- Never modify `/mnt/data/opensource/x-roads/**`.
- Use tag-pinned upstream paths. Do not run `develop` scripts for a v7 lab.
- Keep test certs, keys, `.env`, and message-log data out of git; commit `.env.example` only.
- The lab is disposable: use named volumes and document reset commands.
- When a fact or decision changes, update the matching docs page and its status (`Verified` vs `Planned`), then the skill if the workflow changed.

## X-Road gotchas
- A working HTTP endpoint is not enough. The member/subsystem must be registered in the CS, the SS registered, the service description enabled, and the ACL granted. No ACL means no access.
- Registration rejection is not pushed to the SS; a request can sit in "Registration in progress". Check CS management requests.
- Global configuration propagation, OCSP, and clock sync break calls in ways that look like application errors. Check `proxy.log`, `configuration_client.log`, `signer.log`, and SS Diagnostics first.
- The provider service URL must be reachable from the **SS container**. Use container names and internal ports, not `localhost` or host ports.
- REST errors with `X-Road-Error` come from X-Road; errors without it come from the provider app. Capture `X-Road-Request-Id` and the body `detail` UUID.
- `xrd4j` and the upstream example adapter target X-Road 7 (Jakarta Servlet, Java 21+). Copy patterns, verify versions; do not claim v8 support.
