# Versions and CamDX

Status: **Verified 2026-10-23** from the upstream clone, `CHANGELOG.md`, the public docs site, and the CamDX GitHub fork.

## Version matrix

| Thing | Version | Evidence |
| --- | --- | --- |
| Public X-Road docs | **7.8.3** | `https://docs.x-road.global/` version selector. |
| Upstream stable release | **7.8.3** (2026-09-07) | `CHANGELOG.md`, tag `7.8.3`. |
| Local upstream clone | **8.0.0-beta2** on `develop` | `src/gradle.properties`, `git log`. |
| X-Road 8 production target | **December 2026** (first production release) | Upstream README, "X-Road 8 Spaceship". |
| CamDX X-Road fork | **7.8.2** | `Techo-Startup-Center/CamDX`, tag `7.8.2` and `src/gradle.properties` on `master`. |

The mismatch is the single biggest source of wrong answers in this repo: the working tree contains **v8 beta** source, while the docs site and CamDX are on **7.8.x**.

## What is different in X-Road 8

X-Road 8 is a major platform change, not just a protocol bump:

- Java **25** and Spring Boot **4.1.1** (the v8 codebase itself uses these).
- Gradle **9** (wrapper 9.7.1 in the clone at the time of writing).
- **OpenBao** local secret store replaces the old secret storage approach.
- Internal components communicate over **gRPC** (signer, proxy, configuration client, auxiliary service, monitoring).
- The Security Server is split into separately deployable components and container images.
- Upgrade path is **7.8.x → 8**; older versions must first reach 7.8.x.
- As of this writing, X-Road 8 is **beta 2** and not production-ready. The upstream upgrade guides explicitly say they apply to beta 2 and may change.

The core concepts (members, subsystems, services, Security Servers, global configuration, ACLs, REST/SOAP protocols) remain, but deployment, ports, certificates, and operations differ. Do not apply a v8 command to a v7 lab or vice versa.

## What CamDX actually runs

The CamDX GitHub fork (`Techo-Startup-Center/CamDX`) is a fork of `nordic-institute/X-Road`:

- `src/gradle.properties` sets `xroadVersion=7.8.2`.
- The `7.8.2` tag exists and is the current upstream-aligned tag.
- Recent commits add **Cambodia PKI and branding support** and **RHEL 9 packaging** for 7.8.2.
- The fork was active in September 2026.

This is evidence about the fork, not a guarantee about every CamDX production node. Confirm the target environment's version before exchanging certificates or building adapters.

CamDX's public onboarding requirements add process on top of the software:

- install a Security Server with a certificate signed by CamDX PKI;
- prepare an API catalogue (provider) or a preferred API list (consumer);
- submit the information-security checklist and SIT/UAT documents;
- sign the membership contract.

A local test instance cannot substitute for CamDX's PKI or approval process. It teaches the mechanics; real onboarding is an organizational process. See [11-camdx-onboarding.md](11-camdx-onboarding.md).

## Decision for this lab

**Target X-Road 7.8.x for the lab.** Reasons:

1. **CamDX alignment** — the CamDX fork is on 7.8.2, so v7 behavior is the relevant behavior for SME-CU.
2. **Stable docs and clients** — 7.8.3 is the current stable release; `xrd4j` explicitly targets X-Road 7.
3. **Working dev stack** — upstream 7.8.3 ships a Docker Compose environment with CS, two Security Servers, a test CA, SOAP/REST example information systems, and Hurl initialization.
4. **Less incidental complexity** — v8's OpenBao, gRPC, and split containers are operational topics that distract from learning integration and CamDX onboarding.
5. **No production need for v8 yet** — v8 is beta and its first production release is scheduled for December 2026.

Keep X-Road 8 as a **separate experiment** if the goal becomes contributing to v8, testing the upgrade path, or matching a future CamDX upgrade. Do not mix v7 and v8 artifacts in one lab.

See [../decisions/0001-x-road-version.md](../decisions/0001-x-road-version.md) for the decision record and revisit triggers.

## Version-safe working rules

- **Read v7 from the tag, not the working tree.** The clone is on `develop` (v8). Use:
  ```bash
  git -C /mnt/data/opensource/x-roads/X-Road show 7.8.3:doc/Manuals/ug-ss_x-road_6_security_server_user_guide.md
  ```
  or extract a tag:
  ```bash
  mkdir -p /tmp/xroad-7.8.3
  git -C /mnt/data/opensource/x-roads/X-Road archive 7.8.3 | tar -x -C /tmp/xroad-7.8.3
  ```
- **Use the matching manual.** v8 manuals are named `*v8*` / `*8_central*`; v7 manuals use `v6` in the filename even though they describe 7.8.x. That naming is historical, not a version claim.
- **Check `xroadVersion` before running a build or copying an image tag.**
- **Do not use v8 images in a v7 lab.** v7 uses `ghcr.io/nordic-institute/xrddev-*`; v8 uses the split `ss-*` images built into a local registry.
- **Pin what you run.** The v7 dev stack uses `:latest` image tags. Record the resolved digest if you need reproducibility.
- **CamDX-specific questions** — start from the CamDX fork, not upstream `develop`. If the two disagree, the fork is the better signal for CamDX, but confirm with the operator before making production claims.
