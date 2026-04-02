# Plan: Frontend End-to-End Test Coverage

## Goal
Add a maintainable browser-level safety net around the existing Thymeleaf frontend so inherited UI flows can be validated automatically.

## Constraints
- Keep the work aligned with `.specify/memory/constitution.md`.
- Focus on end-to-end coverage, not a wholesale frontend testing platform redesign.
- Preserve existing application behavior while making it testable.
- Prefer a small, high-value initial suite over broad but flaky coverage.

## Proposed Approach
1. Select the browser test framework
- Use Playwright as the default browser automation framework.
- Keep backend tests on JUnit/Spring Boot; do not replace them.

2. Establish the test harness
- Add the minimal Node/Playwright scaffolding needed to run browser tests.
- Define how the Spring Boot app is started for tests and how test data is isolated.

3. Cover the highest-value flows first
- Homepage render
- Create organization success flow
- Create person success flow
- Create person invalid-domain error flow
- Org chart missing/empty/populated states

4. Document and validate
- Provide a local command path for installing and running the browser tests.
- Record the framework choice and validation results in `progress.ai`.

## Likely Affected Paths
- `package.json` and Playwright config files at repo root
- `tests/e2e/` or similar browser test directory
- `src/main/resources/templates/`
- `src/main/java/com/fishdan/myorgchart/...` only if small testability fixes are required
- `progress.ai`

## Verification
- Automated: backend app starts for the test harness
- Automated: Playwright end-to-end suite passes locally
- Manual: confirm the documented commands are enough for another developer to rerun the suite

## Open Questions
- Whether to use the live MariaDB-backed app for E2E tests or introduce a dedicated test database path first.
- Whether some browser flows need lightweight test hooks or seed endpoints for reliable setup.
