#!/bin/bash



# Orchestration script for feature development workflow

set -e



# Check if all 3 parameters are provided

if [ $# -lt 3 ]; then

    echo "Error: Usage requires 3 parameters."

    echo "Usage: $0 <prefix> <feature_name> <description>"

    echo "Example: $0 feature login-page 'Add user login page with authentication'"

    exit 1

fi



PREFIX="$1"

FEATURE_NAME="$2"

DESCRIPTION="$3"



BRANCH_NAME="${PREFIX}-${FEATURE_NAME}"



echo "=========================================="

echo "Starting feature development workflow..."

echo "Branch: $BRANCH_NAME"

echo "Description: $DESCRIPTION"

echo "=========================================="



# Step 0: Switch to master and pull latest changes

echo ""

echo "[Step 0/6] Ensuring we're on master branch and up to date..."

if [ "$(git branch --show-current)" != "master" ]; then

    echo "Switching to master branch..."

    git checkout master

fi

echo "Pulling latest changes from remote..."

git pull origin master

echo "Master branch is now up to date."



# Step 1: Create or switch to existing branch (local or remote)

echo ""

echo "[Step 1/6] Checking for existing branch (local or remote)..."

# Check if branch exists locally
if git branch --list | grep -q "^*.*$BRANCH_NAME$"; then

    echo "Branch '$BRANCH_NAME' already exists locally, switching to it..."

    git checkout "$BRANCH_NAME"

elif git ls-remote --heads origin "$BRANCH_NAME" > /dev/null 2>&1; then

    echo "Branch '$BRANCH_NAME' exists on remote, fetching and switching to it..."

    git fetch origin "$BRANCH_NAME"

    git checkout "$BRANCH_NAME"

else

    echo "Branch '$BRANCH_NAME' does not exist locally or remotely, creating it..."

    if ! ./git_branchCreate.sh "$PREFIX" "$FEATURE_NAME"; then

        echo "Error: Failed to create branch '$BRANCH_NAME'"

        exit 1

    fi

fi



# Step 2: Use aider with ollama model (non-interactive)

echo ""

echo "[Step 2/6] Generating code with Aider..."

AIDER_OUTPUT=$(aider --model ollama/qwen3.5:9b --no-confirm "$DESCRIPTION" 2>&1 || true)

AIDER_EXIT_CODE=$?



if [ $AIDER_EXIT_CODE -ne 0 ]; then

    echo "Warning: Aider encountered issues, but continuing with workflow..."

fi



# Step 3: Build the project

echo ""

echo "[Step 3/6] Building project (compile)..."

BUILD_ATTEMPTS=0

MAX_BUILD_ATTEMPTS=3



while [ $BUILD_ATTEMPTS -lt $MAX_BUILD_ATTEMPTS ]; do

    BUILD_ATTEMPTS=$((BUILD_ATTEMPTS + 1))

    echo "Build attempt $BUILD_ATTEMPTS/$MAX_BUILD_ATTEMPTS..."



    if ./mvn_build.sh; then

        echo "Build successful on attempt $BUILD_ATTEMPTS!"

        break

    else

        ERROR_MSG=$(./mvnw clean compile 2>&1 | tail -20)

        echo "Build failed with error:"

        echo "$ERROR_MSG"



        if [ $BUILD_ATTEMPTS -lt $MAX_BUILD_ATTEMPTS ]; then

            echo "Sending build error to Aider for correction..."

            echo "$ERROR_MSG" | aider --model ollama/qwen3.5:9b --no-confirm 2>&1 || true

        else

            echo "=========================================="

            echo "ERROR: Build failed after $MAX_BUILD_ATTEMPTS attempts!"

            echo "=========================================="

            exit 1

        fi

    fi

done



# Step 4: Run tests

echo ""

echo "[Step 4/6] Running tests..."

TEST_ATTEMPTS=0

MAX_TEST_ATTEMPTS=3



while [ $TEST_ATTEMPTS -lt $MAX_TEST_ATTEMPTS ]; do

    TEST_ATTEMPTS=$((TEST_ATTEMPTS + 1))

    echo "Test attempt $TEST_ATTEMPTS/$MAX_TEST_ATTEMPTS..."



    if ./mvn_test.sh; then

        echo "Tests passed on attempt $TEST_ATTEMPTS!"

        break

    else

        ERROR_MSG=$(./mvnw clean test 2>&1 | tail -20)

        echo "Tests failed with error:"

        echo "$ERROR_MSG"



        if [ $TEST_ATTEMPTS -lt $MAX_TEST_ATTEMPTS ]; then

            echo "Sending test error to Aider for correction..."

            echo "$ERROR_MSG" | aider --model ollama/qwen3.5:9b --no-confirm 2>&1 || true

        else

            echo "=========================================="

            echo "ERROR: Tests failed after $MAX_TEST_ATTEMPTS attempts!"

            echo "=========================================="

            exit 1

        fi

    fi

done



# Step 5: Install dependencies

echo ""

echo "[Step 5/6] Installing dependencies..."

if ! ./mvn_install.sh; then

    echo "Error: Failed to install dependencies"

    exit 1

fi



# Step 6: Commit changes

echo ""

echo "[Step 6/6] Committing changes..."

COMMIT_MSG="feat: $FEATURE_NAME - $DESCRIPTION"



if git add -A && git commit -m "$COMMIT_MSG"; then

    echo "=========================================="

    echo "SUCCESS: Feature workflow completed!"

    echo "Branch: $BRANCH_NAME"

    echo "Commit message: $COMMIT_MSG"

    echo "=========================================="

else

    echo "Warning: Could not commit changes automatically."

    echo "Please commit manually with:"

    echo "  git add -A && git commit -m '$COMMIT_MSG'"

fi



echo ""

echo "Workflow completed!"
