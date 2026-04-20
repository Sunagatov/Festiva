#!/usr/bin/env bash
set -euo pipefail

OUT_DIR=".ai/generated"
OUT_FILE="${OUT_DIR}/cloudy-context.md"

mkdir -p "${OUT_DIR}"

append_file() {
  local file="$1"
  if [[ -f "$file" ]]; then
    {
      echo
      echo "---"
      echo
      echo "## FILE: ${file}"
      echo
      cat "$file"
      echo
    } >> "${OUT_FILE}"
  fi
}

append_snippet() {
  local file="$1"
  local max_lines="${2:-220}"
  if [[ -f "$file" ]]; then
    {
      echo
      echo "---"
      echo
      echo "## SNIPPET: ${file} (first ${max_lines} lines)"
      echo
      sed -n "1,${max_lines}p" "$file"
      echo
    } >> "${OUT_FILE}"
  fi
}

: > "${OUT_FILE}"

{
  echo "# Festiva compact agent context"
  echo
  echo "Generated on: $(date -u +"%Y-%m-%d %H:%M:%S UTC")"
  echo
  echo "Purpose: small, high-signal context for Claude/Cloudy-style repo work."
  echo
} >> "${OUT_FILE}"

append_file "CLAUDE.md"
append_file "docs/ai/PROJECT_OVERVIEW.md"
append_file "docs/ai/REPO_MAP.md"
append_file "docs/ai/DOMAIN_AND_STATE.md"
append_file "docs/ai/CHANGE_IMPACT_GUIDE.md"
append_file "docs/ai/TOKEN_EFFICIENCY.md"
append_file "docs/ai/VERIFICATION.md"

append_snippet "src/main/resources/application.yml" 200
append_snippet "src/main/java/com/festiva/FestivaApplication.java" 120
append_snippet "src/main/java/com/festiva/bot/BirthdayBot.java" 240
append_snippet "src/main/java/com/festiva/command/CommandRouter.java" 220
append_snippet "src/main/java/com/festiva/bot/CallbackQueryHandler.java" 320
append_snippet "src/main/java/com/festiva/friend/entity/Friend.java" 240
append_snippet "src/main/java/com/festiva/friend/api/FriendService.java" 260
append_snippet "src/main/java/com/festiva/state/BotState.java" 120
append_snippet "src/main/java/com/festiva/state/UserStateService.java" 260
append_snippet "src/main/java/com/festiva/user/UserPreference.java" 120
append_snippet "src/main/java/com/festiva/notification/BirthdayReminder.java" 260
append_snippet "src/main/java/com/festiva/command/handler/ImportIcsCommandHandler.java" 320
append_snippet "src/main/java/com/festiva/ai/IcsNameExtractorService.java" 120

echo "Wrote ${OUT_FILE}"
