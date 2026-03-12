#!/bin/bash

# Maven install script
set -e

echo "Running Maven install..."
./mvnw clean install

echo "Install completed successfully!"
