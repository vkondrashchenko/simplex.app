#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
ENV_FILE="$SCRIPT_DIR/.env.deploy"

if [[ ! -f "$ENV_FILE" ]]; then
  echo "Error: $ENV_FILE not found."
  echo "Copy deploy/.env.deploy.example to deploy/.env.deploy and set IMAGE_REPOSITORY."
  exit 1
fi

# shellcheck source=.env.deploy
source "$ENV_FILE"

if [[ -z "${IMAGE_REPOSITORY:-}" ]]; then
  echo "Error: IMAGE_REPOSITORY is not set in $ENV_FILE."
  exit 1
fi

cd "$SCRIPT_DIR"

echo "Building image..."
# DOCKER_BUILDKIT=0: BuildKit produces OCI manifests which Lambda does not support.
# The legacy builder produces the required Docker v2 manifest format.
DOCKER_BUILDKIT=0 sam build

echo "Deploying to AWS (image_repository: $IMAGE_REPOSITORY)..."
sam deploy --image-repository "$IMAGE_REPOSITORY" "$@"
