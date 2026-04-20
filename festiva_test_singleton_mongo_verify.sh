#!/usr/bin/env bash
set -euo pipefail

REPO_DIR="${1:-.}"
cd "$REPO_DIR"

echo "Running focused integration tests first..."
mvn -q -Dtest=FriendServiceIntegrationTest,FriendCommandTest,BirthdayReminderTest test

echo
echo "Running full test suite..."
mvn test
