Investigate and fix this Festiva bug: $ARGUMENTS

Use this workflow:

1. Read `CLAUDE.md`.
2. Read `docs/ai/CHANGE_IMPACT_GUIDE.md`.
3. Read only the smallest relevant hotspot files.
4. State:
   - root cause
   - exact impacted files
   - minimal patch plan
5. Implement the fix.
6. Add or update regression tests.
7. Show the exact verification command to run.

Rules:
- preserve EN/RU behavior
- preserve callback safety
- preserve ownership validation
- preserve unknown-year birthday support
- keep the patch focused
