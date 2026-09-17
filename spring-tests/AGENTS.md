# Instructions for all agents working on spring-tests

These rules apply to every agent and every change within this module.

## Keep tests and their descriptions synchronized

- The test catalogue is `src/test/tests.md`, relative to this module. Do not create a second catalogue elsewhere.
- Whenever you add, modify, rename, move or remove a test, review the corresponding catalogue entries and update them in the same change to match the actual implementation.
- Also review affected entries when changing shared test helpers, parameter sources, fixtures, database profiles or mappings that alter what the tests verify.
- Never finish a change with missing tests, obsolete entries, incorrect class or method names, or descriptions that disagree with the implemented assertions and scenarios.
- Document each test method once under its declaring class. Do not duplicate entries for inherited methods or individual parameterized invocations.

## Preserve accurate descriptions

- Do not rewrite an existing title or description unless you have identified a concrete mismatch with the actual test implementation.
- A mismatch introduced by a test change must be corrected immediately in the same change. Add entries for new tests and remove entries for deleted tests.
- Make only the edits needed to correct the mismatch. Do not polish, rephrase, expand or reorganize descriptions that are already accurate.
- If a code-only refactoring leaves the documented behavior and method reference unchanged, verify the entry and leave its wording unchanged. Synchronization does not require artificial documentation edits.

## Language and format

- All content in `src/test/tests.md` must be in English.
- Keep each entry in this three-line format, with a blank line between entries:

  ```text
  Short description
  ClassName.testMethodName
  Detailed description, no longer than 300 characters.
  ```

- Test methods must use camelCase names beginning with `test` followed by an uppercase letter, for example `testFoo` or `testDurationInfiniteWriteRead`.
- Do not use sentence-style backtick names, spaces or underscores in test method names.

## Before finishing

- Verify that the catalogue covers every test method and that every referenced class and method exists.
- Compare descriptions with the actual assertions, parameter values, column mappings and database-specific behavior.
- Check that all catalogue text is English and every detailed description is at most 200 characters.
