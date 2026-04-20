# Festiva AI Context Pack

This pack is meant to be unpacked into the **root of `Sunagatov/Festiva`**.

It is optimized for Claude-style coding agents and other repo-aware CLI assistants.  
The goal is simple:

- reduce repeated repo scanning
- reduce token usage
- improve first-pass accuracy
- keep edits inside the right files
- preserve Festiva-specific rules and flows

## What to do

1. Unzip this pack into the repo root.
2. Keep `CLAUDE.md` at the root.
3. Keep `AGENTS.md` at the root for broader agent compatibility.
4. Keep `.claude/commands/*` if your tool supports custom commands.
5. Use `./scripts/ai/print-hotspots.sh <topic>` before broad repo searches.
6. Use `./scripts/ai/build-cloudy-context.sh` to generate one compact context file.

## Highest-value files

- `CLAUDE.md` — main repo instructions for the coding agent
- `docs/ai/REPO_MAP.md` — where to look first
- `docs/ai/CHANGE_IMPACT_GUIDE.md` — which files change together
- `docs/ai/DOMAIN_AND_STATE.md` — core invariants and state machine
- `docs/ai/TOKEN_EFFICIENCY.md` — rules that keep token use low

## Why this helps

Without a repo-specific context pack, an agent tends to:

- re-read the same files
- search too broadly
- miss hidden invariants
- forget linked files such as i18n, callbacks, and tests
- waste tokens on low-value docs and generated content

This pack pushes the agent toward **small targeted reads first**.
