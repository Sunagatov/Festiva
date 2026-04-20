#!/usr/bin/env bash
set -euo pipefail

PROJECT_DIR="${1:-.}"
cd "$PROJECT_DIR"

DRY_RUN="${DRY_RUN:-0}"
CLEAN_DOCS="${CLEAN_DOCS:-1}"
CLEAN_SELF_SCRIPT="${CLEAN_SELF_SCRIPT:-0}"

echo "Strict cleanup in: $(pwd)"
echo "Options: DRY_RUN=${DRY_RUN} CLEAN_DOCS=${CLEAN_DOCS} CLEAN_SELF_SCRIPT=${CLEAN_SELF_SCRIPT}"
echo

run_cmd() {
  if [[ "$DRY_RUN" == "1" ]]; then
    echo "would run: $*"
  else
    eval "$@"
  fi
}

remove_if_exists() {
  local path="$1"
  if [[ -e "$path" || -L "$path" ]]; then
    if [[ "$DRY_RUN" == "1" ]]; then
      echo "would remove: $path"
    else
      rm -rf -- "$path"
      echo "removed: $path"
    fi
  fi
}

# Remove the docs tree robustly if requested
if [[ "$CLEAN_DOCS" == "1" ]]; then
  remove_if_exists "./docs"
  # second pass in case of odd recreated directories / shell state
  if [[ -d "./docs" ]]; then
    run_cmd "find ./docs -mindepth 1 -maxdepth 100 -exec rm -rf {} +"
    remove_if_exists "./docs"
  fi
fi

# Remove the helper cleanup script itself only if explicitly requested
if [[ "$CLEAN_SELF_SCRIPT" == "1" ]]; then
  remove_if_exists "./festiva_cleanup_all_unneeded_local_files.sh"
  remove_if_exists "./festiva_cleanup_strict_docs_and_helpers.sh"
fi

echo
echo "Verification:"
if [[ "$DRY_RUN" == "1" ]]; then
  echo "would run: test -d ./docs && echo 'docs still exists' || echo 'docs removed'"
else
  if [[ -d "./docs" ]]; then
    echo "docs still exists"
  else
    echo "docs removed"
  fi
fi

echo
echo "Recommended next commands:"
echo "  git status"
echo "  mvn test"
