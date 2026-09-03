#!/bin/bash

kubectl config use-context dev-gcp
kubectl config set-context --current --namespace=etterlatte

function get_secrets() {
  local repo=$1
  kubectl -n etterlatte get secret ${repo} -o json | jq '.data | map_values(@base64d)'
}

GJENLEVENDE_BS_SAK_FRONTEND_LOKAL_SECRETS=$(get_secrets azuread-gjenlevende-bs-sak-frontend-lokal)

GJENLEVENDE_BS_SAK_FRONTEND_CLIENT_ID=$(echo "$GJENLEVENDE_BS_SAK_FRONTEND_LOKAL_SECRETS" | jq -r '.AZURE_APP_CLIENT_ID')
GJENLEVENDE_BS_SAK_FRONTEND_CLIENT_SECRET=$(echo "$GJENLEVENDE_BS_SAK_FRONTEND_LOKAL_SECRETS" | jq -r '.AZURE_APP_CLIENT_SECRET')

# Generate random 32 character strings for the cookie and session keys
SESSION_SECRET=$(openssl rand -hex 16)

if [ -z "$GJENLEVENDE_BS_SAK_FRONTEND_CLIENT_ID" ]
then
      echo "Klarte ikke å hente miljøvariabler. Er du pålogget Naisdevice og google?"
      return 1
fi

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
CLIENT_ID='$GJENLEVENDE_BS_SAK_FRONTEND_CLIENT_ID'
CLIENT_SECRET='$GJENLEVENDE_BS_SAK_FRONTEND_CLIENT_SECRET'
PORT=8080

# Lokalt mot lokal-backend
# ENV=lokalt
# ACCESS_TOKEN_LOKALT=$ACCESS_TOKEN_LOKALT

# Lokalt mot preprod
ENV=lokalt-mot-preprod
GJENLEVENDE_BS_SAK_SCOPE=api://dev-gcp.etterlatte.gjenlevende-bs-sak/.default

APP_VERSION=0.0.1
EOF

echo ".env-fil er opprettet med miljøvariabler fra dev-gcp"
