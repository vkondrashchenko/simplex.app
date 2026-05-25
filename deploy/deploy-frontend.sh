#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
ENV_FILE="$SCRIPT_DIR/.env.deploy"

if [[ ! -f "$ENV_FILE" ]]; then
  echo "Error: $ENV_FILE not found."
  echo "Copy deploy/.env.deploy.example to deploy/.env.deploy and fill in all values."
  exit 1
fi

# shellcheck source=.env.deploy
source "$ENV_FILE"

if [[ -z "${AMPLIFY_APP_ID:-}" ]]; then
  echo "Error: AMPLIFY_APP_ID is not set in $ENV_FILE."
  exit 1
fi

if [[ -z "${VITE_API_URL:-}" ]]; then
  echo "Error: VITE_API_URL is not set in $ENV_FILE."
  exit 1
fi

FRONTEND_DIR="$(dirname "$SCRIPT_DIR")/frontend"
BRANCH="main"
ZIP_FILE="/tmp/symplex-frontend.zip"
DEPLOY_JSON="/tmp/symplex-deploy-response.json"

echo "Building frontend (VITE_API_URL=$VITE_API_URL)..."
cd "$FRONTEND_DIR"
npm install --silent
VITE_API_URL="$VITE_API_URL" npm run build

echo "Packaging dist/..."
rm -f "$ZIP_FILE"
(cd dist && zip -r "$ZIP_FILE" .)

echo "Creating Amplify deployment..."
aws amplify create-deployment \
  --app-id "$AMPLIFY_APP_ID" \
  --branch-name "$BRANCH" \
  --output json > "$DEPLOY_JSON"

JOB_ID=$(python3 -c "import json; d=json.load(open('$DEPLOY_JSON')); print(d['jobId'])")
ZIP_URL=$(python3 -c "import json; d=json.load(open('$DEPLOY_JSON')); print(d['zipUploadUrl'])")

echo "Uploading frontend artifact (job: $JOB_ID)..."
curl --silent --show-error --fail -T "$ZIP_FILE" "$ZIP_URL"

echo "Starting deployment..."
aws amplify start-deployment \
  --app-id "$AMPLIFY_APP_ID" \
  --branch-name "$BRANCH" \
  --job-id "$JOB_ID"

DOMAIN=$(aws amplify get-app --app-id "$AMPLIFY_APP_ID" --query 'app.defaultDomain' --output text)

echo ""
echo "Deployment started. Live URL (ready in ~30 s):"
echo "  https://${BRANCH}.${DOMAIN}"
echo ""
echo "Track progress:"
echo "  aws amplify get-job --app-id $AMPLIFY_APP_ID --branch-name $BRANCH --job-id $JOB_ID"
