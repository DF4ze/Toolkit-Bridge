#!/bin/bash

# Maven test script
set -e

echo "Running Maven tests..."
mvn clean test

echo "Tests completed successfully!"
