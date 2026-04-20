# Token efficiency rules for Festiva

These rules are specifically meant to make agent runs cheaper and faster.

## Core principle

Do not pay tokens to rediscover stable repo knowledge.

Use the context docs first, then read only the smallest set of source files needed.

## Reading policy

### Good
- read 3 to 6 targeted files first
- follow symbol references only when needed
- inspect direct neighbors of the changed file
- use repo map + impact guide to narrow scope

### Bad
- reading every handler for a small bug
- opening full feature docs before code
- scanning all tests before finding the touched flow
- repeating repo summaries in every run

## Default file budget by task

### Small bug
Target: 4 to 8 files

Suggested order:
1. entrypoint / router
2. target handler or service
3. one domain file
4. one test
5. one helper if required

### Medium feature
Target: 8 to 14 files

Suggested order:
1. repo map
2. impact guide
3. primary handler/service files
4. domain/state/i18n
5. direct tests

### Architecture review
Read wider only after building a hotspot list.

## Output policy

Keep outputs compact:

- root cause
- impacted files
- minimal patch plan
- regression tests
- verification command

Do not dump long file summaries unless explicitly requested.

## Search policy

Search by:
- class name
- method name
- callback prefix
- command name
- exact message key
- domain field name

Avoid fuzzy repo-wide exploration first.

## When to read docs/features

Read `docs/features/*` only when you need:
- acceptance criteria
- expected UX copy
- edge-case product behavior

Do not use feature docs as the first stop for implementation bugs.

## Best practical workflow

1. run `./scripts/ai/print-hotspots.sh <topic>`
2. read `CLAUDE.md`
3. read the 2–5 primary files
4. form a hypothesis
5. read only missing dependencies
6. patch
7. run the smallest useful verification
