#!/bin/bash
set -e

echo "Starter mock-miljo for grunn-og-hjelpestonad..."

# Start services med mock-profil
docker compose --profile mock up -d

echo "Venter på PostgreSQL..."
until docker exec grunn-og-hjelpestonad-postgres-1 pg_isready -U postgres > /dev/null 2>&1; do
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
echo "Frontendens lokale Authorization Code Flow fullføres automatisk mot mock-serveren."
echo ""
echo "For å stoppe: docker compose --profile mock down"
echo "For å slette data: docker compose --profile mock down -v"
