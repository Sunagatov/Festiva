Implement this Festiva feature: $ARGUMENTS

Workflow:

1. Read `CLAUDE.md`.
2. Read `docs/ai/PROJECT_OVERVIEW.md`.
3. Read `docs/ai/CHANGE_IMPACT_GUIDE.md`.
4. Identify:
   - entrypoint
   - domain changes
   - state changes
   - i18n changes
   - tests
5. Propose a narrow implementation plan.
6. Implement in small, consistent steps.
7. Verify with focused tests first, then broader tests if needed.

Rules:
- no unrelated refactors
- keep callback/data contracts explicit
- update tests in the same change
- keep user-visible text bilingual when applicable
