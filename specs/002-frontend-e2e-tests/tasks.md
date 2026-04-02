# Tasks: Frontend End-to-End Test Coverage

## Phase 1 - Track The Work
- [x] T0201 Create a dedicated feature branch for `002-frontend-e2e-tests`. *(Completed 2026-04-02: created branch `feature/002-frontend-e2e-tests` from `main`.)*
- [x] T0202 Create the Speckit scaffold under `specs/002-frontend-e2e-tests/`. *(Completed 2026-04-02: added `spec.md`, `plan.md`, and `tasks.md` for the frontend E2E testing feature.)*

## Phase 2 - Foundation
- [x] T0210 Select and document the frontend/browser testing framework for this repo. *(Completed 2026-04-02: selected Playwright for browser-level testing of the Thymeleaf/JavaScript frontend and documented the choice in the spec and test README.)*
- [x] T0211 Add the initial Playwright project scaffolding and root-level test commands. *(Completed 2026-04-02: added `package.json`, `package-lock.json`, `playwright.config.js`, `tests/e2e/README.md`, and root `npm` scripts.)*
- [x] T0212 Define how the Spring Boot app is started and configured for end-to-end tests. *(Completed 2026-04-02: added `scripts/run-e2e-server.sh` to start the app on port `18080` with required datasource env vars for Playwright `webServer`.)*

## Phase 3 - User Story 1: Core Page And Form Flow Coverage (P1)
**Goal**: The main browser-visible workflows are validated end-to-end.

**Independent Test**: Run the browser suite and verify the homepage, organization creation, and person creation flows in a real browser.

- [x] T0220 [US1] Add an end-to-end test for the homepage load flow. *(Completed 2026-04-02: added a Playwright test that verifies the homepage title, heading, and primary navigation links.)*
- [x] T0221 [US1] Add an end-to-end test for organization creation success. *(Completed 2026-04-02: added a Playwright browser flow that creates a unique organization and verifies the success redirect and confirmation message.)*
- [x] T0222 [US1] Add an end-to-end test for person creation success when the organization exists. *(Completed 2026-04-02: added a Playwright flow that creates an organization, creates a person in that domain, and verifies the success redirect and confirmation message.)*
- [x] T0223 [US1] Add an end-to-end test for person creation failure when the organization does not exist. *(Completed 2026-04-02: added a Playwright flow that submits a person for a missing organization and verifies the browser-visible error response.)*

## Phase 4 - User Story 2: Org Chart State Coverage (P1)
**Goal**: The org chart browser behavior is covered for the states users actually encounter.

**Independent Test**: Run the browser suite and verify the UI behavior for missing organization, empty organization, and populated organization states.

- [x] T0230 [US2] Add an end-to-end test for the missing-organization org chart state. *(Completed 2026-04-02: added a Playwright test that verifies the org chart page shows an error for an unknown domain.)*
- [x] T0231 [US2] Add an end-to-end test for the empty-organization org chart state. *(Completed 2026-04-02: added a Playwright test that verifies the org chart page shows the empty-state message for an organization with no people.)*
- [x] T0232 [US2] Add an end-to-end test for the populated-organization org chart state. *(Completed 2026-04-02: added a Playwright test that verifies a created person renders in the org chart tree for the organization.)*

## Phase 5 - Validate And Record
- [x] T0240 Run the frontend/browser suite locally. *(Completed 2026-04-02: installed Chromium for Playwright and ran `npm run test:e2e` successfully with all seven browser tests passing.)*
- [x] T0241 Run any necessary backend validation that supports the browser suite. *(Completed 2026-04-02: ran `mvn test -q` after the E2E additions to confirm backend support code and existing tests still pass.)*
- [x] T0242 Record framework choice, commands, and validation results in `progress.ai`. *(Completed 2026-04-02: recorded the Playwright framework choice, setup commands, execution details, fixes, and validation results in the progress log.)*

## Phase 6 - Review Handoff
- [ ] T0250 Commit the completed E2E testing work with the GitHub App identity.
- [ ] T0251 Push the feature branch to `origin`.
- [ ] T0252 Open a PR for review.
