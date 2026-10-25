# 01 — Prerequisites

**Goal:** prove the host can run the X-Road Docker dev stack.
**Time:** 2 minutes.

## Run

```bash
docker version
docker compose version
uname -m
free -h
df -h /tmp
```

## Expected

- Docker Engine **24 or newer** and Docker Compose **v2 or newer**.
- `uname -m` prints `x86_64`.
- At least **8 GB RAM** available.
- At least **20 GB free** on `/tmp` (images plus containers; the source extract is about 180 MB).
- Your user can run Docker without `sudo`.

Example shape (versions will differ):

```text
Client: Docker Engine 29.x
Server: Docker Engine 29.x

Docker Compose version v5.x

x86_64

               total        used        free
Mem:            61Gi        ...
/dev/...        337G        ...        179G ...
```

## If it fails

| Symptom | Meaning | Fix |
| --- | --- | --- |
| `docker: command not found` | Docker is not installed. | Install Docker Engine for your distribution. |
| `permission denied ... docker.sock` | Your user is not in the `docker` group. | Add the user to `docker`, log out/in, or use a rootless Docker setup. |
| `docker compose` is not a command | Compose v2 plugin is missing. | Install `docker-compose-plugin` (package name varies). |
| `uname -m` prints `aarch64` | The published images are amd64. | Use an amd64 host/VM, or build images locally (out of scope for this guide). |
| Less than 8 GB RAM | Containers may be OOM-killed during initialization. | Stop other workloads or use a larger host. |
| Less than 20 GB disk | Image pulls or databases may fail. | Free space before starting; `docker system df` shows usage. |

## Why this step exists

The upstream stack is a set of Linux containers. Docker is the only runtime you need; you do **not** need Java, Gradle, LXD, or Ansible for this guide.

The architecture matters because the published X-Road dev images are built for `amd64`. On ARM you would need a different image build, which is a separate exercise.

## Next

[02-get-source.md](02-get-source.md)
