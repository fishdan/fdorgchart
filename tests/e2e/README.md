# End-to-End Tests

This directory will hold Playwright browser tests for the Thymeleaf frontend flows.

## Planned initial coverage
- homepage render
- organization creation success
- person creation success
- person creation failure for missing organization
- org chart missing/empty/populated states

## Local usage
1. Export the datasource settings used by the Spring Boot app:
   - `SPRING_DATASOURCE_USERNAME`
   - `SPRING_DATASOURCE_PASSWORD`
   - Optional: `SPRING_DATASOURCE_URL` if you want the `dev` profile to use a non-default local database URL
2. Install browser test dependencies:
   - `npm install`
   - `npm run test:e2e:install`
3. Run the E2E suite:
   - `npm run test:e2e`

The Playwright config starts the Spring Boot app on port `18080` through `scripts/run-e2e-server.sh`.
That script activates the `dev` profile by default so browser tests run against the development-local database path.
