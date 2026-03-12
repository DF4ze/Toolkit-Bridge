#!/bin/bash

# Maven test script
set -e

echo "Running Maven tests..."
./mvnw clean test

echo "Tests completed successfully!"
