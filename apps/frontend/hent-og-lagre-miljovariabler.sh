#!/bin/bash

MOCK_OAUTH2_SERVER_URL=http://localhost:8089/default

# Generate random 32 character strings for the cookie and session keys
SESSION_SECRET=$(openssl rand -hex 16)

cat << EOF > .env
# Denne filen er generert automatisk ved å kjøre \`hent-og-lagre-miljovariabler.sh\`

SESSION_SECRET='$SESSION_SECRET'
CLIENT_ID='test'
CLIENT_SECRET='test'
PORT=8080

ENV=lokalt
GRUNN_OG_HJELPESTONAD_SCOPE=default
OAUTH2_AUTHORIZATION_ENDPOINT=$MOCK_OAUTH2_SERVER_URL/authorize
OAUTH2_TOKEN_ENDPOINT=$MOCK_OAUTH2_SERVER_URL/token

APP_VERSION=0.0.1
EOF

echo ".env-fil er opprettet."
