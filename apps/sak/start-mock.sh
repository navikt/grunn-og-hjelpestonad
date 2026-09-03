#!/bin/bash
set -e

echo "Starter mock-miljo for gjenlevende-bs-sak..."

# Start services med mock-profil
docker compose --profile mock up -d

echo "Venter på PostgreSQL..."
until docker exec gjenlevende-bs-sak-postgres-1 pg_isready -U postgres > /dev/null 2>&1; do
    sleep 1
done
echo "PostgreSQL er ferdig"

echo "Venter på mock-oauth2-server..."
until curl -s http://localhost:8089/default/.well-known/openid-configuration > /dev/null 2>&1; do
    sleep 1
done
echo "mock-oauth2-server er ferdig"

# Vent pa WireMock
echo "Venter på WireMock..."
until curl -s http://localhost:8090/__admin/mappings > /dev/null 2>&1; do
    sleep 1
done
echo "WireMock er ferdig"

echo ""
echo "Mock miljø er klart!"
echo ""
echo "Kjør ApplicationLocalMock i IntelliJ"
echo ""
echo "For å teste med mock-token:"
echo "TOKEN=\$(curl -s -X POST http://localhost:8089/default/token -d 'grant_type=client_credentials&client_id=test&client_secret=test' | jq -r '.access_token')"
echo "curl -H \"Authorization: Bearer \$TOKEN\" http://localhost:8082/internal/health"
echo ""
echo "For å stoppe: docker compose --profile mock down"
echo "For å slette data: docker compose --profile mock down -v"
