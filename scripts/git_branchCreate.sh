#!/bin/bash

# Git feature branch script
set -e

# Check if both prefix and feature name parameters are provided
if [ $# -lt 2 ]; then
    echo "Error: Feature prefix and feature name are required."
    echo "Usage: $0 <prefix> <feature>"
    exit 1
fi

PREFIX="$1"
FEATURE_NAME="$2"

echo "Pulling latest changes..."
git pull --quiet

echo "Creating and switching to branch: ${PREFIX}-${FEATURE_NAME}..."
git checkout -b "${PREFIX}-${FEATURE_NAME}"

echo "Pushing to remote origin..."
git push -u origin "${PREFIX}-${FEATURE_NAME}"

echo "Success: Feature branch '${PREFIX}-${FEATURE_NAME}' created and pushed successfully!"
