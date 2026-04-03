# Plan: Development Uses a Local Database

## Goal
Separate local development datasource behavior from production behavior so development intentionally uses a local database without creating a production regression risk.

## Constraints
- Keep the change aligned with `.specify/memory/constitution.md`.
- Do not alter deployed production behavior as part of this work.
- Do not commit secrets or require production credentials in the repository.
- Keep the design simple and easy to audit.

## Proposed Approach
1. Introduce an explicit development-only configuration boundary
- Add a development-specific Spring configuration path, most likely a `dev` profile file such as `application-dev.properties`.
- Keep `application.properties` production-safe and environment-driven.

2. Route development to the local datasource
- Configure the development path to use the local MariaDB connection values appropriate for a local machine.
- Decide whether local credentials should come from non-secret defaults, local environment variables, or a documented developer override file ignored by git.

3. Preserve production behavior by default
- Ensure production/deployed startup continues to use the existing environment-based datasource settings when no development-only profile is active.
- Avoid any default that would cause deployed code to resolve to localhost unexpectedly.

4. Document and verify
- Update the README with the development startup command and production-safety explanation.
- Validate both the explicit development path and the default non-development path.
- Record the decision and results in `progress.ai`.

## Likely Affected Paths
- `src/main/resources/application.properties`
- `src/main/resources/application-dev.properties` or equivalent profile-specific config
- `README.md`
- `progress.ai`

## Verification
- Automated or executable check that the development profile resolves datasource settings to the local database target
- Automated or executable check that the default profile still resolves through environment-based production/deployment settings
- Manual review that no committed secret values are introduced

## Open Questions
- Whether local developer credentials should remain environment-driven inside the `dev` profile or use safe local defaults for username/password.
- Whether any helper scripts or `spring-boot` command aliases should be added for local startup.
