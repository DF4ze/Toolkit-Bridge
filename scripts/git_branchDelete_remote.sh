#!/bin/bash

# Script to delete a remote branch with interactive validation

set -e

# Check if branch name is provided
if [ -z "$1" ]; then
    echo "Usage: $0 <branchName>"
    echo "Example: $0 feature-branch"
    exit 1
fi

BRANCH_NAME="$1"

echo "Deleting remote branch '$BRANCH_NAME'..."

# Check if the branch exists on remote
if ! git ls-remote --heads origin "$BRANCH_NAME" > /dev/null 2>&1; then
    echo "Branch '$BRANCH_NAME' not found on remote repository."
    exit 1
fi

# Interactive confirmation
read -p "Are you sure you want to delete the remote branch? (Y/N) " CONFIRMATION

if [ "$CONFIRMATION" != "Y" ] && [ "$CONFIRMATION" != "y" ]; then
    echo "Deletion cancelled by user."
    exit 0
fi

# Delete the remote branch
git push origin --delete "$BRANCH_NAME"

echo "Successfully deleted remote branch '$BRANCH_NAME'"
