#!/bin/bash

# Maven compile script
set -e

echo "Running Maven compile..."
./mvnw clean compile

if [ $? -eq 0 ]; then
    echo "Compile completed successfully!"
else
    echo "Compile failed with exit code $?"
    exit 1
fi
