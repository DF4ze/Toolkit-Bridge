#!/bin/bash

# Maven test script
set -e

echo "Running Maven tests..."
./mvnw clean test

if [ $? -eq 0 ]; then
    echo "Tests completed successfully!"
else
    echo "Tests failed with exit code $?"
    exit 1
fi
