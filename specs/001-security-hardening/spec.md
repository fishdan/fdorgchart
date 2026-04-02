# Spec: Security Hardening Baseline

## Context
Repository review and the 2026-04-02 monthly security audit identified basic security issues that should be addressed before additional feature work:

- plaintext database credentials are committed in `src/main/resources/application.properties`
- `MyOrgChartApplication` logs the datasource password at startup
- `Organization` stores organization passwords as plain text

These issues are already documented in `progress.ai`, but they are not yet tracked as a formal Speckit feature.

## Scope
- Remove committed plaintext runtime secrets from the application configuration path.
- Stop exposing datasource credentials in startup output or logs.
- Replace plain-text organization password storage with a one-way hashed representation.
- Preserve the existing organization creation flow while hardening password handling.
- Add verification coverage for the changed security-sensitive behavior.

## Out of Scope
- Building a full user authentication or session-management system.
- Password reset flows or account recovery.
- Broad infrastructure redesign beyond app configuration and persistence updates needed for this hardening task.
- Unrelated UI/UX changes.

## Requirements
- R1: Application database credentials must be sourced from environment variables or equivalent external configuration, not committed plaintext values in `src/main/resources/application.properties`.
- R2: Application startup and normal runtime logs must not print datasource passwords or organization passwords.
- R3: Newly created organizations must not persist raw passwords in the database.
- R4: Existing organization creation behavior must continue to accept a user-entered password while converting it into a safe stored form before persistence.
- R5: The password-hardening implementation must be covered by automated verification close to the affected backend code.
- R6: `progress.ai` must record the implementation decisions, migration approach, and validation results.

## Success Criteria
- S1: The tracked repository no longer contains committed plaintext datasource credentials in `src/main/resources/application.properties`.
- S2: `MyOrgChartApplication` no longer logs any datasource secret at startup.
- S3: Organization records created after the change store hashed passwords instead of raw password text.
- S4: Relevant backend tests and validation commands pass after the hardening changes.
