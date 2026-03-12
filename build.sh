#!/bin/bash

# Maven compile script
set -e

echo "Running Maven compile..."
mvn clean compile

echo "Compile completed successfully!"
