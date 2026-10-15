#!/bin/bash
# Start the application

set -euo pipefail

# Local Consul must listen on the container interface so published ports work.
# consul.yml rejects this wildcard bind in every non-local environment.
export CONSUL_DEPLOY_ENV="${CONSUL_DEPLOY_ENV:-development}"
export CONSUL_CLIENT_BIND="${CONSUL_CLIENT_BIND:-0.0.0.0}"
export CONSUL_HOST_BIND="${CONSUL_HOST_BIND:-127.0.0.1}"

docker compose -f docker-compose.yml up -d
docker compose -f paylite/src/main/docker/services.yml up -d
docker compose -f paynet/src/main/docker/services.yml up -d
