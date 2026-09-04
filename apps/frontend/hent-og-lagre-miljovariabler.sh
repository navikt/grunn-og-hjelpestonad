#!/bin/bash

GRUNN_OG_HJELPESTONAD_FRONTEND_LOKAL_SECRETS=grunn-og-hjelpestonad-client

GRUNN_OG_HJELPESTONAD_FRONTEND_CLIENT_ID=$(echo "$GRUNN_OG_HJELPESTONAD_FRONTEND_LOKAL_SECRETS")
GRUNN_OG_HJELPESTONAD_FRONTEND_CLIENT_SECRET=$(echo "$GRUNN_OG_HJELPESTONAD_FRONTEND_LOKAL_SECRETS")

# Generate random 32 character strings for the cookie and session keys
SESSION_SECRET=$(openssl rand -hex 16)

# Hent token fra mock OAuth-server for lokalt miljø
echo "Henter token fra mock OAuth-server (localhost:8089)"
ACCESS_TOKEN_LOKALT=$(curl -s -X POST http://localhost:8089/default/token \
  -d 'grant_type=client_credentials&client_id=test&client_secret=test' \
  | jq -r '.access_token' 2>/dev/null)

if [ -z "$ACCESS_TOKEN_LOKALT" ] || [ "$ACCESS_TOKEN_LOKALT" = "null" ]; then
  echo "Kunne ikke hente token fra mock OAuth-server. Sørg for at backend kjører på localhost:8089."
  ACCESS_TOKEN_LOKALT=""
else
  echo "Token hentet fra mock OAuth-server"
fi

cat << EOF > .env
# Denne filen er generert automatisk ved å kjøre \`hent-og-lagre-miljovariabler.sh\`

SESSION_SECRET='$SESSION_SECRET'
CLIENT_ID='$GRUNN_OG_HJELPESTONAD_FRONTEND_CLIENT_ID'
CLIENT_SECRET='$GRUNN_OG_HJELPESTONAD_FRONTEND_CLIENT_SECRET'
PORT=8080

ENV=lokalt
ACCESS_TOKEN_LOKALT=$ACCESS_TOKEN_LOKALT

APP_VERSION=0.0.1
EOF

echo ".env-fil er opprettet."
