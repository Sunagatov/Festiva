#!/usr/bin/env bash
set -euo pipefail

REPO_ROOT="${1:-.}"
cd "$REPO_ROOT"

if [[ ! -f "pom.xml" ]]; then
  echo "pom.xml not found. Run this from the Festiva repository root or pass the repo path as the first argument."
  exit 1
fi

echo "Java version:"
java -version || true
echo

echo "Maven version:"
mvn -version || true
echo

echo "Compiling tests without running them..."
mvn -q -DskipTests test-compile

echo
echo "Running full test suite..."
mvn test

echo
echo "Verification finished successfully."
