#!/bin/bash

# Maven install script
set -e

echo "Running Maven install..."
./mvnw clean install

if [ $? -eq 0 ]; then
    echo "Install completed successfully!"
else
    echo "Install failed with exit code $?"
    exit 1
fi
