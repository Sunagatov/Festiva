#!/usr/bin/env bash
set -euo pipefail

REPO_DIR="${1:-.}"
cd "$REPO_DIR"

echo "Java version:"
java -version || true
echo

echo "Maven version:"
mvn -version || true
echo

if command -v docker >/dev/null 2>&1; then
  echo "Docker CLI detected."
  if docker info >/dev/null 2>&1; then
    echo "Docker daemon is reachable."
  else
    echo "Docker daemon is NOT reachable. Docker-backed integration tests should now be skipped gracefully."
  fi
else
  echo "Docker CLI not found. Docker-backed integration tests should now be skipped gracefully."
fi
echo

echo "Running full test suite..."
mvn test
