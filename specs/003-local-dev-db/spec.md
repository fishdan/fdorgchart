# Spec: Development Uses a Local Database

## Context
The repository currently relies on the shared datasource properties in `application.properties`, with behavior driven by environment variables and a localhost fallback. That makes development behavior implicit instead of intentionally separated from production behavior.

The next change should make local development point to a local database through an explicit development-only configuration path. Production and deployed environments must continue using their existing deployment configuration and must not silently switch to local database settings.

## Scope
- Define an explicit development-only datasource configuration path for local work.
- Make it straightforward for a developer to start the app against a local MariaDB instance.
- Preserve current production and deployment behavior unless a production environment explicitly opts into a different setting.
- Document the local startup workflow and the production safety boundaries.

## Out of Scope
- Changing production datasource hosts, credentials, or deployment secrets.
- Introducing a new production profile strategy beyond what is required to isolate development behavior.
- Database schema redesign or migration-tool adoption.
- Reworking unrelated runtime configuration.

## Requirements
- R1: Local development must have an explicit, documented configuration path that points to a local database instead of relying on accidental fallbacks.
- R2: Production and deployed environments must keep their current datasource resolution unless they intentionally activate a development-only profile or override.
- R3: The implementation must not require committed secrets for local or production database access.
- R4: The development-only configuration must be easy to activate locally with a clear command or environment variable.
- R5: The repository documentation must explain how development uses the local database and why production remains unchanged.
- R6: Verification must include a check that the development path resolves to the local datasource and a separate check that the default production/deployed path still resolves through the existing environment-based configuration.
- R7: `progress.ai` must record the chosen isolation mechanism, affected files, commands used, and validation results.

## Success Criteria
- S1: A developer can intentionally start the app in development mode against a local MariaDB instance using documented steps.
- S2: The default runtime configuration for deployed environments remains environment-driven and does not point at localhost unless explicitly configured to do so.
- S3: Reviewers can inspect the configuration files and documentation and clearly see the boundary between development-only and production behavior.
- S4: The work is tracked in Spec Kit with a plan and task breakdown before implementation proceeds.
