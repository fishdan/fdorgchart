# Spec: Frontend End-to-End Test Coverage

## Context
This application is inherited and currently has only backend-side automated coverage. The current test suite validates controller and service behavior, but it does not validate the browser-visible behavior of the Thymeleaf pages and their JavaScript form flows.

The primary concern is making sure the frontend actually works from a user perspective:

- pages load successfully
- forms submit correctly in the browser
- success and error states render as expected
- org chart viewing behavior matches the current UI contract

## Scope
- Introduce a browser-level frontend testing framework suitable for this server-rendered Spring Boot application.
- Add initial end-to-end coverage for the highest-value user-facing flows.
- Define a repeatable local test workflow for running the frontend/browser checks against the application.

## Out of Scope
- Replacing the current frontend stack.
- Converting the app to a SPA or adding a frontend component test runner first.
- Broad backend refactors unrelated to making the browser test path reliable.
- Large authentication or authorization changes.

## Requirements
- R1: The project must adopt a browser automation framework appropriate for end-to-end testing of a Spring Boot + Thymeleaf app.
- R2: The initial frontend test suite must cover the core user journeys:
- homepage loads
- organization creation happy path
- person creation happy path for an existing organization
- person creation failure path for a missing organization
- org chart viewing states for missing org, empty org, and populated org
- R3: The test workflow must be runnable locally with documented setup steps.
- R4: The test suite must avoid depending on fragile manual data assumptions wherever practical.
- R5: `progress.ai` must record the framework choice, test scope, commands used, and validation results.

## Success Criteria
- S1: A developer can run the frontend/browser test suite locally with a documented command.
- S2: The suite verifies the main user-visible flows end-to-end in a real browser.
- S3: The initial suite is stable enough to catch regressions in form submission, redirects, and UI error/success states.
- S4: The new tests are tracked in Spec Kit and integrated into the repository workflow.
