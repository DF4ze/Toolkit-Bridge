#!/bin/bash

# Maven install script
set -e

echo "Running Maven install..."
mvn clean install

echo "Install completed successfully!"
