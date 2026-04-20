#!/usr/bin/env bash
set -euo pipefail

topic="${1:-}"

if [[ -z "${topic}" ]]; then
  cat <<'EOF'
Usage:
  ./scripts/ai/print-hotspots.sh <topic>

Topics:
  bot
  callback
  friend
  reminder
  ics
  settings
  i18n
  metrics
  tests
EOF
  exit 1
fi

case "${topic}" in
  bot)
    cat <<'EOF'
src/main/java/com/festiva/bot/BirthdayBot.java
src/main/java/com/festiva/command/CommandRouter.java
src/main/java/com/festiva/bot/CallbackQueryHandler.java
src/main/resources/application.yml
EOF
    ;;
  callback)
    cat <<'EOF'
src/main/java/com/festiva/bot/CallbackQueryHandler.java
src/main/java/com/festiva/bot/DatePickerCallbackHandler.java
src/main/java/com/festiva/bot/EditCallbackHandler.java
src/main/java/com/festiva/command/DatePickerKeyboard.java
src/main/java/com/festiva/command/handler/SettingsCommandHandler.java
src/main/java/com/festiva/command/handler/ListCommandHandler.java
src/main/java/com/festiva/command/handler/UpcomingBirthdaysCommandHandler.java
EOF
    ;;
  friend)
    cat <<'EOF'
src/main/java/com/festiva/friend/entity/Friend.java
src/main/java/com/festiva/friend/entity/Relationship.java
src/main/java/com/festiva/friend/api/FriendService.java
src/main/java/com/festiva/friend/repository/FriendMongoRepository.java
src/main/java/com/festiva/command/handler/
EOF
    ;;
  reminder)
    cat <<'EOF'
src/main/java/com/festiva/notification/BirthdayReminder.java
src/main/java/com/festiva/notification/NotificationSender.java
src/main/java/com/festiva/user/UserPreference.java
src/main/java/com/festiva/state/UserStateService.java
src/main/java/com/festiva/friend/entity/Friend.java
src/main/java/com/festiva/i18n/Messages.java
EOF
    ;;
  ics)
    cat <<'EOF'
src/main/java/com/festiva/command/handler/ImportIcsCommandHandler.java
src/main/java/com/festiva/ai/IcsNameExtractorService.java
src/main/java/com/festiva/state/UserStateService.java
src/main/java/com/festiva/friend/entity/Friend.java
src/main/java/com/festiva/friend/api/FriendService.java
src/main/java/com/festiva/i18n/Messages.java
EOF
    ;;
  settings)
    cat <<'EOF'
src/main/java/com/festiva/command/handler/SettingsCommandHandler.java
src/main/java/com/festiva/bot/CallbackQueryHandler.java
src/main/java/com/festiva/state/UserStateService.java
src/main/java/com/festiva/user/UserPreference.java
src/main/java/com/festiva/bot/BotCommandsService.java
src/main/java/com/festiva/i18n/Messages.java
EOF
    ;;
  i18n)
    cat <<'EOF'
src/main/java/com/festiva/i18n/Lang.java
src/main/java/com/festiva/i18n/Messages.java
src/main/java/com/festiva/command/handler/
src/main/java/com/festiva/bot/
EOF
    ;;
  metrics)
    cat <<'EOF'
src/main/java/com/festiva/metrics/
src/main/java/com/festiva/bot/BirthdayBot.java
src/main/resources/application.yml
EOF
    ;;
  tests)
    cat <<'EOF'
src/test/java/com/festiva/
docs/ai/VERIFICATION.md
EOF
    ;;
  *)
    echo "Unknown topic: ${topic}" >&2
    exit 1
    ;;
esac
