#!/bin/bash

# Script to merge a branch into master and push to repository

set -e

# Check if branch name is provided
if [ -z "$1" ]; then
    echo "Usage: $0 <branchName>"
    echo "Example: $0 feature-branch"
    exit 1
fi

BRANCH_NAME="$1"

echo "Merging branch '$BRANCH_NAME' into master..."

# Check if the branch exists locally
if ! git branch --list | grep -q "^*.*$BRANCH_NAME$"; then
    echo "Branch '$BRANCH_NAME' not found locally. Fetching from remote..."
    git fetch origin "$BRANCH_NAME" 2>/dev/null || true
    
    # Check if it exists after fetch
    if ! git branch --list | grep -q "^*.*$BRANCH_NAME$"; then
        echo "Branch '$BRANCH_NAME' not found in local or remote repository."
        exit 1
    fi
fi

# Switch to master branch
git checkout master

# Merge the branch
git merge "$BRANCH_NAME" -m "Merge branch '$BRANCH_NAME'"

echo "Successfully merged '$BRANCH_NAME' into master"

# Push to remote repository
echo "Pushing changes to remote repository..."
git push origin master

echo "Done! Branch '$BRANCH_NAME' has been merged and pushed."
