# Tasks: Security Hardening Baseline

## Phase 1 - Track The Work
- [x] T0101 Create a dedicated feature branch for `001-security-hardening` before implementation begins. *(Completed 2026-04-02: created branch `feature/001-security-hardening` from `main`.)*
- [x] T0102 Create the Speckit scaffold under `specs/001-security-hardening/`. *(Completed 2026-04-02: added `spec.md`, `plan.md`, and `tasks.md` using the `../iby` repo structure.)*
- [x] T0103 Mirror the active constitution into `.specify/memory/constitution.md`. *(Completed 2026-04-02: added the Spec Kit memory constitution so this repo now matches the `../iby` structure.)*

## Phase 2 - Foundational Security Cleanup
- [x] T0110 Remove committed plaintext datasource credentials from `src/main/resources/application.properties` and replace them with safe externalized configuration. *(Completed 2026-04-02: replaced committed datasource values with environment-backed properties and safe inline documentation.)*
- [x] T0111 Remove datasource password logging from `src/main/java/com/fishdan/myorgchart/MyOrgChartApplication.java`. *(Completed 2026-04-02: removed the startup password print from the application entrypoint.)*
- [x] T0112 Review the organization creation path in `src/main/java/com/fishdan/myorgchart/organization/OrganizationController.java` and `src/main/java/com/fishdan/myorgchart/organization/OrganizationService.java` to confirm the correct service-layer insertion point for password hashing. *(Completed 2026-04-02: confirmed password hashing belongs in `OrganizationService#createOrganization` so request handling remains unchanged in the controller.)*

## Phase 3 - User Story 1: Safe Organization Password Storage (P1)
**Goal**: New organizations can still be created, but raw passwords are never persisted.

**Independent Test**: Submit the organization creation flow and verify persisted organization data does not contain the submitted raw password.

- [x] T0120 [US1] Add failing automated coverage for the organization creation path in `src/test/java/com/fishdan/myorgchart/organization/`. *(Completed 2026-04-02: added `OrganizationServiceTest` and `OrganizationControllerTest` covering password hashing and request-path behavior.)*
- [x] T0121 [US1] Implement password hashing in `src/main/java/com/fishdan/myorgchart/organization/OrganizationService.java`. *(Completed 2026-04-02: added `PasswordEncoder`-backed hashing before repository save.)*
- [x] T0122 [US1] Update `src/main/java/com/fishdan/myorgchart/organization/Organization.java` only as needed to support hashed password storage safely. *(Completed 2026-04-02: marked the password field write-only for JSON serialization so stored password hashes are not returned in API responses.)*
- [x] T0123 [US1] Verify the controller path in `src/main/java/com/fishdan/myorgchart/organization/OrganizationController.java` preserves current request handling while delegating secure storage to the service layer. *(Completed 2026-04-02: controller continues accepting JSON organization creation requests while hashing occurs only in the service.)*

## Phase 4 - User Story 2: Safe Runtime Configuration (P1)
**Goal**: The app can start without committed secrets, and no raw secrets appear in startup output.

**Independent Test**: Start the app with externally supplied datasource credentials and verify startup succeeds without printing secrets.

- [x] T0130 [US2] Add or update configuration handling in `src/main/resources/application.properties` for environment-backed datasource settings. *(Completed 2026-04-02: datasource URL, username, and password now resolve from environment variables.)*
- [x] T0131 [US2] Remove secret-printing behavior from `src/main/java/com/fishdan/myorgchart/MyOrgChartApplication.java`. *(Completed 2026-04-02: startup no longer prints the datasource password.)*
- [x] T0132 [US2] Document any required local/runtime env vars in a safe repo-visible location if the change would otherwise be ambiguous. *(Completed 2026-04-02: documented `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME`, and `SPRING_DATASOURCE_PASSWORD` in `src/main/resources/application.properties` comments.)*

## Phase 5 - Validate And Record
- [x] T0140 Run backend validation for `001-security-hardening` with `mvn test -q`. *(Completed 2026-04-02: backend test suite passed after the hardening changes.)*
- [x] T0141 Perform manual verification that the app boots with externalized datasource configuration and no secret logging. *(Completed 2026-04-02: started successfully with datasource env vars and `--server.port=0`; no startup password print remained.)*
- [x] T0142 Record implementation decisions, commands, and results in `progress.ai`. *(Completed 2026-04-02: progress log updated with branch creation, security changes, and validation evidence.)*

## Phase 6 - Review Handoff
- [x] T0150 Commit the completed hardening work with the GitHub App identity. *(Completed 2026-04-02: commit `5390f23` (`Harden secrets handling and add Speckit workflow`) authored as `ai-codex-dan[bot]`.)*
- [x] T0151 Push the feature branch to `origin`. *(Completed 2026-04-02: pushed `feature/001-security-hardening` to `origin`.)*
- [x] T0152 Open a PR for review. *(Completed 2026-04-02: opened PR `#2`, `Harden secrets handling and add Speckit workflow` -> `https://github.com/fishdan/fdorgchart/pull/2`.)*
