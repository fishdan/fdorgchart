# Plan: Security Hardening Baseline

## Goal
Close the currently known high-risk secret-handling gaps without expanding scope beyond the baseline hardening already identified in `progress.ai`.

## Constraints
- Must honor the Speckit constitution in `.specify/memory/constitution.md`.
- Must stay narrowly scoped to configuration secret handling and organization password storage.
- Must avoid introducing unnecessary DTOs or broad architectural churn.
- Must verify security-sensitive behavior with automated checks where practical.

## Proposed Approach
1. Externalize datasource secrets
- Replace committed values in `src/main/resources/application.properties` with environment-backed placeholders.
- If needed, provide a safe example or fallback pattern that documents required env vars without committing secrets.

2. Remove password disclosure from startup
- Delete the startup `System.out.println` in `src/main/java/com/fishdan/myorgchart/MyOrgChartApplication.java`.
- Confirm no remaining code logs raw datasource or organization passwords.

3. Hash organization passwords before persistence
- Introduce a focused password-hashing path in the organization service layer rather than controller code.
- Preserve the existing organization creation request shape while storing only a hashed password in the entity.
- Determine whether a schema change is required after inspecting current column constraints and data usage.

4. Add regression coverage
- Add unit and/or MVC/service tests around organization creation so the hardening behavior is enforced by tests.
- Validate with `mvn test -q` and any additional targeted checks needed for config handling.

## Affected Paths
- `src/main/resources/application.properties`
- `src/main/java/com/fishdan/myorgchart/MyOrgChartApplication.java`
- `src/main/java/com/fishdan/myorgchart/organization/Organization.java`
- `src/main/java/com/fishdan/myorgchart/organization/OrganizationController.java`
- `src/main/java/com/fishdan/myorgchart/organization/OrganizationService.java`
- `src/test/java/com/fishdan/myorgchart/organization/` (new tests expected)
- `progress.ai`

## Verification
- Automated: `mvn test -q`
- Automated: targeted assertions that raw passwords are not persisted or logged by the updated code path
- Manual: confirm local startup still works when datasource secrets are provided externally

## Open Questions
- Whether there are existing organization records whose raw passwords require one-time migration or backfill.
- Whether any runtime environment currently depends on committed `application.properties` secrets and needs explicit deployment coordination.
