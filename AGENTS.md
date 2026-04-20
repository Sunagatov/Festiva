# Festiva agent guide

Canonical repo instructions live in `CLAUDE.md`.

## Minimal rule set

- Prefer targeted reads over broad scans.
- Start with `docs/ai/REPO_MAP.md` and `docs/ai/CHANGE_IMPACT_GUIDE.md`.
- Keep Festiva-specific invariants intact:
  - bilingual EN/RU behavior
  - Telegram callback contracts
  - per-user ownership validation
  - birthdays may be with or without a year
  - reminder logic depends on timezone + notify hour + lastNotifiedDate
- When changing persistence or validation, inspect both:
  - `friend/entity/Friend.java`
  - `friend/api/FriendService.java`
- When changing user flows, check:
  - routing
  - state transitions
  - callback handlers
  - messages
  - regression tests
- Avoid reading unrelated docs or generated files.

## Best first files

- `CLAUDE.md`
- `docs/ai/PROJECT_OVERVIEW.md`
- `docs/ai/REPO_MAP.md`
- `docs/ai/DOMAIN_AND_STATE.md`
- `docs/ai/CHANGE_IMPACT_GUIDE.md`
