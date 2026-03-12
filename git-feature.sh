#!/bin/bash

# Git feature branch script
set -e

# Check if feature name parameter is provided
if [ -z "$1" ]; then
    echo "Error: Feature name is required."
    echo "Usage: $0 <feature>"
    exit 1
fi

FEATURE_NAME="$1"

echo "Pulling latest changes..."
git pull --quiet

echo "Creating and switching to branch: oklm_bot-${FEATURE_NAME}..."
git checkout -b "oklm_bot-${FEATURE_NAME}"

echo "Pushing to remote origin..."
git push -u origin "oklm_bot-${FEATURE_NAME}"

echo "Success: Feature branch 'oklm_bot-${FEATURE_NAME}' created and pushed successfully!"
