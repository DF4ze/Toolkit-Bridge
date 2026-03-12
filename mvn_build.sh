#!/bin/bash

# Maven compile script
set -e

echo "Running Maven compile..."
./mvnw clean compile

echo "Compile completed successfully!"
