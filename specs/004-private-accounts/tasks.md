# Tasks: Private Accounts, Domain Ownership, And Org Privacy

## Phase 1 - Track The Work
- [x] T0401 Create a dedicated feature branch for `004-private-accounts`. *(Completed 2026-04-03: created branch `feature/004-private-accounts` from updated `main` after merging spec `003-local-dev-db`.)*
- [x] T0402 Create the Speckit scaffold under `specs/004-private-accounts/`. *(Completed 2026-04-03: added `spec.md`, `plan.md`, and `tasks.md` for the private-accounts feature set.)*

## Phase 2 - Design Decisions Before Implementation
- [x] T0410 Choose the authentication/session approach. *(Completed 2026-04-09: selected server-side `HttpSession` authentication for private-account browser flows and explicitly rejected JWT for v1.)*
- [x] T0411 Define the persistence model for user accounts, verification codes, domain verification challenges, membership approval state, and org admin state. *(Completed 2026-04-09: documented a hybrid model that preserves `Person` for the free flow and adds `Account`, `EmailVerificationCode`, `OrganizationMembership`, and `DomainVerificationChallenge`, plus the `OPEN`/`OFFICIAL` organization boundary.)*
- [x] T0412 Finalize abuse-control rules for verification-code expiry, resend behavior, and daily cap accounting. *(Completed 2026-04-09: selected single-use 15-minute codes, 5-minute resend lockout, and a 5-sends-per-trailing-24-hours cap.)*
- [x] T0413 Finalize the domain-claim control model and the last-admin safety rule. *(Completed 2026-04-09: defined claim behavior for existing `OPEN` organizations only and documented the no-zero-admin invariant for `OFFICIAL` organizations.)*
- [x] T0414 Define the authorization rule for private org chart viewing. *(Completed 2026-04-09: private charts are viewable only by approved members of that organization.)*
- [x] T0415 Define the non-regression rule for the free self-entry flow. *(Completed 2026-04-09: documented that unreserved emails remain addable without login in `OPEN` organizations and added a regression-test task to enforce it.)*

## Phase 3 - User Story 1: Accounts And Email Verification (P1)
**Goal**: A person can register an account, log in, and verify ownership of their email address.

**Independent Test**: Create an account, send a verification code through the app, confirm the code, and verify resend limits are enforced.

- [x] T0420 [US1] Add the account domain model, repository, and password-hashing behavior. *(Completed 2026-04-09: added `Account` and `EmailVerificationCode` entities plus repositories, reused `PasswordEncoder` for password hashing, and stored only hashed verification codes.)*
- [x] T0421 [US1] Add account registration and login flows. *(Completed 2026-04-09: added server-rendered register/login pages and `AccountController` flows backed by `HttpSession`.)*
- [x] T0422 [US1] Integrate SES-backed verification email sending using the infrastructure pattern documented in `../fishdan-terraform/aws/ses.tf`. *(Completed 2026-04-09: added `AwsSesVerificationEmailSender` using AWS SDK SES v2 and repo-safe env-backed sender configuration.)*
- [x] T0423 [US1] Add verification-code confirmation and verified-user state. *(Completed 2026-04-09: added verification-code confirmation, verified timestamp persistence, and the verify-account page.)*
- [x] T0424 [US1] Enforce the 5-minute resend limit and 5-per-day send cap. *(Completed 2026-04-09: implemented a 5-minute resend lockout, 5 sends per trailing 24 hours, 15-minute single-use code expiry, and invalidation of older pending codes.)*
- [x] T0425 [US1] Add automated coverage for account creation, login, verification success/failure, and send throttling. *(Completed 2026-04-09: added `AccountServiceTest` and `AccountControllerTest`; also re-ran the existing Playwright suite to confirm the public no-login flow still passes.)*

## Phase 4 - User Story 2: Private Account Ownership And Self-Service (P1)
**Goal**: Verified private accounts control their own email identity inside organizations.

**Independent Test**: Verify that another user cannot add a verified private-account email to an org, while the verified user can add themselves and edit their own org profile data.

- [x] T0430 [US2] Enforce that verified private-account emails can only be added to orgs by the account owner. *(Completed 2026-04-09: `PersonService` now blocks public creation of entries whose email belongs to a verified private account unless the authenticated session email matches that account.)*
- [x] T0431 [US2] Render the specified blocked-add message when another user tries to add a private-account email. *(Completed 2026-04-09: the blocked public-add path now returns the required message telling the user to email the account owner and ask them to add themselves.)*
- [x] T0432 [US2] Add a settings page for password change and organization membership listing. *(Completed 2026-04-09: added `/settings` with account summary, password-change form, and self-managed organization entry listing.)*
- [x] T0433 [US2] Allow verified users to edit their own supervisor and department in joined organizations. *(Completed 2026-04-09: added authenticated self-service membership update flow for department and supervisor email.)*
- [x] T0434 [US2] Allow verified users to add themselves to additional organizations. *(Completed 2026-04-09: added authenticated self-add flow from settings that creates the member's own `Person` entry using their verified account email.)*
- [x] T0435 [US2] Add automated and browser coverage for private-email enforcement and self-service membership flows. *(Completed 2026-04-09: added backend tests plus new Playwright scenarios covering reserved-email blocking and verified-user self-service updates.)*
- [x] T0436 [US2] Add regression coverage proving that an unreserved email can still be added through the public flow for a non-owned organization without login. *(Completed 2026-04-09: kept the existing no-login browser flow passing and revalidated it alongside the new account-protection cases.)*

## Phase 5 - User Story 3: Official Domains And Claims (P1)
**Goal**: Verified users can prove domain ownership and become admins through DNS TXT verification.

**Independent Test**: Start a domain verification flow, receive the TXT record requirement, simulate or verify DNS presence, and confirm the successful user becomes the first admin.

- [x] T0440 [US3] Add domain verification challenge storage and TXT record generation. *(Completed 2026-04-09: added `DomainVerificationChallenge` persistence plus generated `fdorgchart-verification=...` TXT challenge tokens.)*
- [x] T0441 [US3] Add the UI flow for creating or claiming a domain via DNS TXT verification. *(Completed 2026-04-09: added the settings-page flow to start DNS challenges, display TXT instructions, and trigger verification checks.)*
- [x] T0442 [US3] Enforce the once-per-10-minute verification-check cap. *(Completed 2026-04-09: domain verification checks now reject repeated retries inside a 10-minute window per challenge.)*
- [x] T0443 [US3] Make the successful verifier the first admin for the organization. *(Completed 2026-04-09: successful verification now marks the organization `OFFICIAL` and records the verifying account as an org admin.)*
- [x] T0444 [US3] Support claim flow for an existing domain with documented transfer semantics. *(Completed 2026-04-09: existing `OPEN` organizations can now be claimed through DNS TXT verification, while already-`OFFICIAL` organizations reject re-claim attempts.)*
- [x] T0445 [US3] Add automated coverage for challenge generation, verification pacing, success, and claim behavior. *(Completed 2026-04-09: added service/controller tests and a Playwright flow for claiming an existing open organization through the settings UI.)*

## Phase 6 - User Story 4: Admin Controls And Provisional Membership (P1)
**Goal**: Official domains are admin-controlled and self-joins require approval before public chart visibility.

**Independent Test**: Self-add a verified user to an official domain, confirm they are provisional and hidden from the org chart, then approve them as an admin and verify they become visible.

- [x] T0450 [US4] Add org membership approval state for official-domain joins. *(Completed 2026-04-09: added `Person.approvalStatus` with `APPROVED`, `PROVISIONAL`, and `REJECTED` states.)*
- [x] T0451 [US4] Make self-joins to official domains provisional until approved. *(Completed 2026-04-09: verified self-adds to `OFFICIAL` organizations now become `PROVISIONAL` unless the joining account is already an admin; `OPEN` organizations remain immediately approved.)*
- [x] T0452 [US4] Prevent provisional users from appearing on the org chart. *(Completed 2026-04-09: the org-chart endpoint now filters `OFFICIAL` organizations to approved members only.)*
- [x] T0453 [US4] Add admin review actions to approve or reject provisional members. *(Completed 2026-04-09: added settings-page admin review controls for approving or rejecting pending members in official organizations.)*
- [x] T0454 [US4] Add admin grant/revoke controls for other org users. *(Completed 2026-04-09: added admin grant/revoke actions for approved members with verified private accounts.)*
- [x] T0455 [US4] Enforce the safety rule around last-admin removal or transfer. *(Completed 2026-04-09: admin revocation now rejects any action that would leave an official organization with zero admins.)*
- [x] T0456 [US4] Add automated and browser coverage for provisional membership and admin-role management. *(Completed 2026-04-09: added unit/controller tests plus a Playwright flow covering provisional self-join, hidden chart state, approval, and admin promotion.)*

## Phase 7 - User Story 5: Org Chart Privacy (P1)
**Goal**: Admins can control whether an organization chart is public.

**Independent Test**: Mark an org chart private, request it from the public org chart page, and confirm only the private-chart message is shown; then mark it public and confirm the chart is visible again.

- [x] T0460 [US5] Add org-level public/private visibility state. *(Completed 2026-04-09: added `Organization.chartVisibility` with `PUBLIC` and `PRIVATE` states.)*
- [x] T0461 [US5] Add admin controls to manage org chart visibility. *(Completed 2026-04-09: added settings-page controls for org admins to toggle chart visibility between public and private.)*
- [x] T0462 [US5] Update org chart endpoints and templates to hide private org charts from unauthorized viewers. *(Completed 2026-04-09: private-chart requests now require either org-admin access or an approved member row tied to the authenticated account.)*
- [x] T0463 [US5] Render the private-chart message on the org chart page for private organizations. *(Completed 2026-04-09: unauthorized lookups now render the private-chart message instead of exposing the org tree.)*
- [x] T0464 [US5] Add automated and browser coverage for private/public org chart behavior. *(Completed 2026-04-09: added controller/service coverage plus a Playwright scenario for private denial and public re-enable behavior.)*

## Phase 8 - Validation And Documentation
- [x] T0470 Update README and operator/developer docs for account verification, SES dependency, DNS verification, admin approvals, and private org charts. *(Completed 2026-04-09: updated `README.md` to document the free/open flow, private-account flow, SES configuration, DNS verification, admin approvals, and private/public chart behavior.)*
- [x] T0471 Run the relevant backend validation suite for the implemented phases. *(Completed 2026-04-09: re-ran `bash ./mvnw -q test` against the completed `004` feature set.)*
- [x] T0472 Run browser/E2E validation for the implemented phases. *(Completed 2026-04-09: re-ran `SPRING_DATASOURCE_USERNAME=root SPRING_DATASOURCE_PASSWORD=mysql npm run test:e2e` against the completed `004` feature set.)*
- [x] T0473 Record implementation decisions, validation commands, and outcomes in `progress.ai`. *(Completed 2026-04-09: logged phase-by-phase implementation and final validation results in `progress.ai`.)*

## Phase 9 - Review Handoff
- [ ] T0480 Commit the completed private-account work with the GitHub App identity.
- [ ] T0481 Push the feature branch to `origin`.
- [ ] T0482 Open a PR for review.
