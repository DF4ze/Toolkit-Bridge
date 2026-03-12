#!/bin/bash

# Script to delete a local branch

set -e

# Check if branch name is provided
if [ -z "$1" ]; then
    echo "Usage: $0 <branchName>"
    echo "Example: $0 feature-branch"
    exit 1
fi

BRANCH_NAME="$1"

echo "Deleting local branch '$BRANCH_NAME'..."

# Check if the branch exists locally
if ! git branch --list | grep -q "^*.*$BRANCH_NAME$"; then
    echo "Branch '$BRANCH_NAME' not found locally."
    exit 1
fi

# Delete the local branch
git branch -d "$BRANCH_NAME"

echo "Successfully deleted local branch '$BRANCH_NAME'"
