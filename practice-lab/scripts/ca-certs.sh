#!/usr/bin/env bash
# Copy the test CA, OCSP and TSA certificates out of the running `testca`
# container so they can be uploaded in the Central Server UI (step 06).
#
#   ./scripts/ca-certs.sh              # -> ./ca-certs
#   ./scripts/ca-certs.sh /tmp/labcerts
set -euo pipefail

out="${1:-$PWD/ca-certs}"
mkdir -p "$out"

for name in ca ocsp tsa; do
  docker cp "testca:/home/ca/certs/${name}.pem" "$out/${name}.pem"
done

ls -l "$out"
