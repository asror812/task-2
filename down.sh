#!/bin/bash
# Start the application

docker compose -f docker-compose.yml down
docker compose -f paylite/src/main/docker/services.yml down
docker compose -f paynet/src/main/docker/services.yml down