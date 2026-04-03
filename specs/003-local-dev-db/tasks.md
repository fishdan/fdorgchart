# Tasks: Development Uses a Local Database

## Phase 1 - Track The Work
- [x] T0301 Create a dedicated feature branch for `003-local-dev-db`. *(Completed 2026-04-03: created branch `feature/003-local-dev-db` from `main` while preserving unrelated local worktree changes.)*
- [x] T0302 Create the Speckit scaffold under `specs/003-local-dev-db/`. *(Completed 2026-04-03: added `spec.md`, `plan.md`, and `tasks.md` for the development-local-database feature.)*

## Phase 2 - Design The Configuration Boundary
- [x] T0310 Choose the development-only isolation mechanism. *(Completed 2026-04-03: selected an explicit Spring `dev` profile backed by `application-dev.properties`.)*
- [x] T0311 Define the production-safety rule in the base configuration. *(Completed 2026-04-03: base `application.properties` now requires `SPRING_DATASOURCE_URL` instead of defaulting to localhost.)*
- [x] T0312 Define how local developer credentials are sourced without committing secrets. *(Completed 2026-04-03: documented local development to use environment-provided username/password while the `dev` profile supplies only the localhost URL default.)*

## Phase 3 - User Story 1: Developer Starts Against Local DB (P1)
**Goal**: A developer can intentionally run the app against a local database during development.

**Independent Test**: Start the app with the development-only configuration and verify the resolved datasource points at the local MariaDB target.

- [x] T0320 [US1] Add the development-only datasource configuration. *(Completed 2026-04-03: added `src/main/resources/application-dev.properties` with a localhost MariaDB URL default behind the explicit `dev` Spring profile.)*
- [x] T0321 [US1] Add or update documentation for the local startup command and prerequisites. *(Completed 2026-04-03: updated `README.md` with the `SPRING_PROFILES_ACTIVE=dev` local startup workflow, local credential expectations, and the `scripts/run-dev-server.sh` helper script.)*
- [x] T0322 [US1] Add a verification step that proves the development path resolves to the local database target. *(Completed 2026-04-03: added `DatabaseConfigurationTest` to verify the `dev` profile config defaults to `jdbc:mariadb://localhost:3306/myorgchart`.)*

## Phase 4 - User Story 2: Production Behavior Stays Unchanged (P1)
**Goal**: Production and deployed environments are unaffected by the development-local-database change.

**Independent Test**: Start or inspect the app without the development-only configuration and verify datasource resolution still follows the existing environment-based production path.

- [x] T0330 [US2] Update the base configuration so non-development runtime remains production-safe. *(Completed 2026-04-03: removed the localhost fallback from base `application.properties` so non-dev runtime requires an explicit datasource URL.)*
- [x] T0331 [US2] Add a verification step that proves the default/deployed path does not opt into the local database configuration. *(Completed 2026-04-03: added `DatabaseConfigurationTest` to verify base config requires `SPRING_DATASOURCE_URL` instead of defaulting to localhost.)*
- [x] T0332 [US2] Review deployment-facing docs/config comments for any ambiguous wording that could imply localhost in production. *(Completed 2026-04-03: updated `README.md` and config comments to state that deployed environments must not activate the `dev` profile.)*

## Phase 5 - Validate And Record
- [x] T0340 Run the chosen verification commands for development and non-development configuration paths. *(Completed 2026-04-03: ran `bash ./mvnw -q -Dtest=DatabaseConfigurationTest test` and `bash ./mvnw -q test` successfully.)*
- [x] T0341 Record the isolation approach, commands, and validation results in `progress.ai`. *(Completed 2026-04-03: recorded the `dev` profile approach, affected files, verification commands, and the `mvnw` permission workaround in the progress log.)*
- [x] T0342 Add dev-only cleanup support for test data and cover a real org hierarchy browser flow. *(Completed 2026-04-03: added a `dev`-profile cleanup endpoint, updated the E2E server to run under `dev`, and added a Playwright test that creates an organization plus a 3-person hierarchy, verifies the org chart nesting, and deletes the test data afterward.)*

## Phase 6 - Review Handoff
- [ ] T0350 Commit the completed local-dev-db work with the GitHub App identity.
- [ ] T0351 Push the feature branch to `origin`.
- [ ] T0352 Open a PR for review.
