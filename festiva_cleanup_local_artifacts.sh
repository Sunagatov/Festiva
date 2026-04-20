#!/usr/bin/env bash
set -euo pipefail

REPO_DIR="${1:-.}"
DRY_RUN="${DRY_RUN:-0}"
CLEAN_IDE="${CLEAN_IDE:-1}"
CLEAN_AMAZONQ="${CLEAN_AMAZONQ:-0}"

cd "$REPO_DIR"

if [[ ! -f pom.xml ]]; then
  echo "Error: pom.xml not found in $REPO_DIR" >&2
  exit 1
fi

removed_any=0

say() {
  printf '%s\n' "$*"
}

remove_path() {
  local path="$1"
  if [[ -e "$path" || -L "$path" ]]; then
    removed_any=1
    if [[ "$DRY_RUN" == "1" ]]; then
      say "[dry-run] rm -rf $path"
    else
      rm -rf -- "$path"
      say "removed: $path"
    fi
  fi
}

remove_glob_matches() {
  local pattern="$1"
  shopt -s nullglob dotglob
  local matches=( $pattern )
  shopt -u nullglob dotglob
  for m in "${matches[@]:-}"; do
    [[ -n "$m" ]] && remove_path "$m"
  done
}

remove_find_matches() {
  local description="$1"
  shift
  local found=0
  while IFS= read -r -d '' path; do
    found=1
    remove_path "$path"
  done < <(find . "$@" -print0 2>/dev/null)
  if [[ "$found" == "0" && "$DRY_RUN" == "1" ]]; then
    :
  fi
}

say "Cleaning local/generated artifacts in: $(pwd)"
say "Options: DRY_RUN=$DRY_RUN CLEAN_IDE=$CLEAN_IDE CLEAN_AMAZONQ=$CLEAN_AMAZONQ"
say

# Build/test output
remove_path "target"
remove_glob_matches "./surefire-reports"
remove_glob_matches "./*.dump"
remove_glob_matches "./*.dumpstream"
remove_glob_matches "./hs_err_pid*"
remove_glob_matches "./replay_pid*"

# One-off backup folders created during bugfix/test stabilization work
remove_glob_matches "./festiva_bugfix_backup_*"
remove_glob_matches "./festiva_testcontainers_fix_backup_*"
remove_glob_matches "./festiva_test_singleton_mongo_fix_backup_*"

# One-off local helper scripts created during the repair process
remove_glob_matches "./festiva_backend_bugfix_apply.sh"
remove_glob_matches "./festiva_backend_bugfix_verify.sh"
remove_glob_matches "./festiva_testcontainers_macos_fix.sh"
remove_glob_matches "./festiva_testcontainers_macos_verify.sh"
remove_glob_matches "./festiva_test_singleton_mongo_fix.sh"
remove_glob_matches "./festiva_test_singleton_mongo_verify.sh"

# Common local junk
remove_find_matches "macOS junk" -type f \( -name '.DS_Store' -o -name '*.log' -o -name '*.tmp' \)

# IDE artifacts (safe for project, local to your machine)
if [[ "$CLEAN_IDE" == "1" ]]; then
  remove_path ".idea"
  remove_find_matches "IntelliJ files" -type f \( -name '*.iml' -o -name '*.ipr' -o -name '*.iws' \)
fi

# Optional Amazon Q local workspace cleanup
if [[ "$CLEAN_AMAZONQ" == "1" ]]; then
  remove_path ".amazonq"
fi

say
if [[ "$removed_any" == "0" ]]; then
  say "Nothing matched the cleanup rules."
else
  if [[ "$DRY_RUN" == "1" ]]; then
    say "Dry run completed. Nothing was deleted."
  else
    say "Cleanup completed."
  fi
fi

say
say "Suggested follow-up commands:"
say "  git status"
say "  mvn test"
