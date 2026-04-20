#!/usr/bin/env bash
set -euo pipefail

PROJECT_DIR="${1:-.}"
cd "$PROJECT_DIR"

DRY_RUN="${DRY_RUN:-0}"
CLEAN_IDE="${CLEAN_IDE:-1}"
CLEAN_AMAZONQ="${CLEAN_AMAZONQ:-0}"
CLEAN_DOCS="${CLEAN_DOCS:-0}"
CLEAN_LOCAL_FIX_ARTIFACTS="${CLEAN_LOCAL_FIX_ARTIFACTS:-1}"

echo "Cleaning project folder: $(pwd)"
echo "Options:"
echo "  DRY_RUN=${DRY_RUN}"
echo "  CLEAN_IDE=${CLEAN_IDE}"
echo "  CLEAN_AMAZONQ=${CLEAN_AMAZONQ}"
echo "  CLEAN_DOCS=${CLEAN_DOCS}"
echo "  CLEAN_LOCAL_FIX_ARTIFACTS=${CLEAN_LOCAL_FIX_ARTIFACTS}"
echo

remove_path() {
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

remove_glob_matches() {
  local pattern="$1"
  shopt -s nullglob dotglob
  local matches=( $pattern )
  shopt -u dotglob
  if (( ${#matches[@]} > 0 )); then
    for item in "${matches[@]}"; do
      remove_path "$item"
    done
  fi
  shopt -u nullglob
}

# Build / runtime junk
remove_path "target"
remove_glob_matches "*.log"
remove_glob_matches "*.tmp"
remove_glob_matches "*.out"
remove_glob_matches "*.pid"
remove_glob_matches "*.dump"
remove_glob_matches "*.dumpstream"
remove_glob_matches "hs_err_pid*"
remove_glob_matches "replay_pid*"
remove_glob_matches ".DS_Store"
remove_glob_matches "**/.DS_Store"

# IDE junk
if [[ "$CLEAN_IDE" == "1" ]]; then
  remove_path ".idea"
  remove_glob_matches "*.iml"
  remove_glob_matches "*.ipr"
  remove_glob_matches "*.iws"
fi

# Our temporary/local repair artifacts from this chat
if [[ "$CLEAN_LOCAL_FIX_ARTIFACTS" == "1" ]]; then
  remove_path "festiva_backend_bugfix_apply.sh"
  remove_path "festiva_backend_bugfix_verify.sh"
  remove_path "festiva_cleanup_local_artifacts.sh"
  remove_path "festiva_testcontainers_macos_fix.sh"
  remove_path "festiva_testcontainers_macos_verify.sh"
  remove_path "festiva_test_singleton_mongo_fix.sh"
  remove_path "festiva_test_singleton_mongo_verify.sh"

  remove_glob_matches "festiva_bugfix_backup_*"
  remove_glob_matches "festiva_testcontainers_fix_backup_*"
  remove_glob_matches "festiva_test_singleton_mongo_fix_backup_*"
fi

# Local assistant workspace, optional
if [[ "$CLEAN_AMAZONQ" == "1" ]]; then
  remove_path ".amazonq"
fi

# Project docs are optional. Keep by default.
if [[ "$CLEAN_DOCS" == "1" ]]; then
  remove_path "docs"
fi

echo
echo "Cleanup finished."
echo
echo "Recommended follow-up:"
echo "  git status"
echo "  mvn test"
