#!/usr/bin/env bash
# Start the X-Road lab stack.
#
#   ./scripts/up.sh                 # everything
#   ./scripts/up.sh cs testca       # only the Central Server and the test CA
#
# Passing service names is how steps 03 and 07 start the stack in two phases.
set -euo pipefail

here="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
stack="$here/../../infra/v7"

cd "$stack"
exec docker compose -f compose.yaml -f compose.dev.yaml --env-file .env up -d "$@"
