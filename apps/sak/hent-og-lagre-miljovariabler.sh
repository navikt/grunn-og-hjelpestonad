#!/bin/bash
set -e

echo "Bytter til dev-gcp kontekst..."
kubectl config use-context dev-gcp

# Verifiser at vi er på dev-gcp
CURRENT_CONTEXT=$(kubectl config current-context)
if [ "$CURRENT_CONTEXT" != "dev-gcp" ]; then
    echo "   Du må være på dev-gcp kontekst! ❌"
    echo "   Nåværende kontekst: $CURRENT_CONTEXT"
    echo "   Dette scriptet fungerer IKKE med prod-gcp!"
    exit 1
fi
echo "Kontekst: dev-gcp ✅ "

kubectl config set-context --current --namespace=etterlatte
echo "Namespace: etterlatte ✅ "

function get_secrets() {
  local repo=$1
  kubectl -n etterlatte get secret ${repo} -o json | jq '.data | map_values(@base64d)'
}

GJENLEVENDE_BS_SAK_LOKAL_SECRETS=$(get_secrets TEST_SECRET)

GJENLEVENDE_BS_SAK_CLIENT_ID=$(echo "$GJENLEVENDE_BS_SAK_LOKAL_SECRETS" | jq -r '.AZURE_APP_CLIENT_ID')
GJENLEVENDE_BS_SAK_CLIENT_SECRET=$(echo "$GJENLEVENDE_BS_SAK_LOKAL_SECRETS" | jq -r '.AZURE_APP_CLIENT_SECRET')
GJENLEVENDE_BS_SAK_TENANT_ID=$(echo "$GJENLEVENDE_BS_SAK_LOKAL_SECRETS" | jq -r '.AZURE_APP_TENANT_ID')
GJENLEVENDE_BS_SAK_JWK=$(echo "$GJENLEVENDE_BS_SAK_LOKAL_SECRETS" | jq -r '.AZURE_APP_JWK')

if [ -z "$GJENLEVENDE_BS_SAK_CLIENT_ID" ]
then
      echo "Klarte ikke å hente miljøvariabler. Er du pålogget Naisdevice og google?"
      return 1
fi

# Write the variables to a file that can be sourced
cat << EOF > .env.local
# Denne filen er generert automatisk ved å kjøre \`hent-og-lagre-miljovariabler.sh\`

export AZURE_APP_CLIENT_ID='$GJENLEVENDE_BS_SAK_CLIENT_ID'
export AZURE_APP_CLIENT_SECRET='$GJENLEVENDE_BS_SAK_CLIENT_SECRET'
export AZURE_APP_TENANT_ID='$GJENLEVENDE_BS_SAK_TENANT_ID'
export AZURE_APP_JWK='$GJENLEVENDE_BS_SAK_JWK'

# Database konfigurasjon for lokal kjøring
export DB_JDBC_URL='jdbc:postgresql://localhost:5432/gjenlevende-bs-sak'

# Azure AD konfigurasjon
export AZURE_OPENID_CONFIG_ISSUER='https://login.microsoftonline.com/$GJENLEVENDE_BS_SAK_TENANT_ID/v2.0'
export AZUREAD_TOKEN_ENDPOINT_URL='https://login.microsoftonline.com/$GJENLEVENDE_BS_SAK_TENANT_ID/oauth2/v2.0/token'
export AUTHORIZATION_URL='https://login.microsoftonline.com/$GJENLEVENDE_BS_SAK_TENANT_ID/oauth2/v2.0/authorize'
export API_SCOPE='api://$GJENLEVENDE_BS_SAK_CLIENT_ID/.default'

# Scope for tjenester
export GJENLEVENDE_BS_INFOTRYGD_SCOPE='api://dev-fss.etterlatte.gjenlevende-bs-infotrygd/.default'

EOF

echo ".env.local fil er opprettet med miljøvariabler fra dev-gcp"
echo "Kjør 'source .env.local' for å laste inn variablene i din shell"
