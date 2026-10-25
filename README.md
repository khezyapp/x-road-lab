# X-Road Lab — SME-CU / CamDX

A hands-on lab for learning X-Road and simulating SME-CU's integration with CamDX-style partners using Spring Boot 4.

**Status:** knowledge base, agent skill, manual lab guide, and the `member-services` Spring Boot pair are ready. The X-Road lab infrastructure is not built yet.

## Start here

- **[docs/index.md](docs/index.md)** — knowledge-base index.
- **[docs/x-road/01-foundations.md](docs/x-road/01-foundations.md)** — concepts and architecture.
- **[docs/x-road/07-runbook-v7-docker.md](docs/x-road/07-runbook-v7-docker.md)** — runbook index for the upstream X-Road 7.8.3 Docker stack.
- **[docs/guide/index.md](docs/guide/index.md)** — the same runbook split into small, copy/paste steps with failure explanations.
- **[member-services/README.md](member-services/README.md)** — SME-CU portal + MOC registry (Spring Boot 4, composite Gradle build).
- **[.opencode/skills/x-road/SKILL.md](.opencode/skills/x-road/SKILL.md)** — the skill coding agents load for X-Road work.

## Docs site

The `docs/` folder is a VitePress site rooted at `docs/`:

```bash
npm install          # once
npm run docs:dev     # http://localhost:5173
npm run docs:build   # static site in docs/.vitepress/dist
npm run docs:preview # serve the built site
```

Add new pages to the `sidebar` in `docs/.vitepress/config.mts`.

## Version

The lab targets **X-Road 7.8.x**:

- CamDX's fork is on **7.8.2**.
- Upstream stable and public docs are **7.8.3**.
- X-Road 8 is **beta** and kept as a separate experiment.

The upstream clone at `/mnt/data/opensource/x-roads/X-Road` is on `develop` (8.0.0-beta2). Read v7 behavior from tag `7.8.3`, not the working tree. See [docs/x-road/05-versions-and-camdx.md](docs/x-road/05-versions-and-camdx.md).

## Layout

```text
docs/                  knowledge base, manual guide, VitePress site
.opencode/skills/      agent skill
member-services/       implemented Spring Boot 4 services (composite build)
infra/                 X-Road compose, test CA, seed/reset scripts (not built yet)
```

## Member services

```bash
cd member-services
./gradlew buildAll
./gradlew testAll
```

Run the provider and consumer in separate terminals:

```bash
cd member-services && ./gradlew -p moc-registry bootRun
cd member-services && ./gradlew -p sme-cu-portal bootRun
```

Then:

```bash
curl http://localhost:8081/api/businesses
curl http://localhost:8080/api/portal/businesses
```

## Goal

Simulate three organizations:

- **SME-CU** — consumer and provider
- **MOC** — partner agency, provider and consumer
- **CAMD-X** — CamDX catalogue/operator member

Each organization has its own Security Server and Spring Boot information system. The lab covers both directions: SME-CU consuming a partner service, and the partner consuming an SME-CU service.

The lab is **not production** and cannot join the real CamDX instance. Real onboarding needs CamDX PKI certificates, an API catalogue, a security checklist, and SIT/UAT.
