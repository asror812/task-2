#!/bin/bash
# Start the application

docker compose -f docker-compose.yml up -d
docker compose -f paylite/src/main/docker/services.yml up -d
docker compose -f paynet/src/main/docker/services.yml up -d