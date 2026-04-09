# Plan: Private Accounts, Domain Ownership, And Org Privacy

## Goal
Introduce verified private user accounts and official-domain administration without trying to land every behavior in one unsafe change set.

## Constraints
- Keep the work aligned with `.specify/memory/constitution.md`.
- Preserve the existing free org/person self-entry flow as a permanent product mode, not just a temporary migration state.
- Do not commit email or infrastructure secrets.
- Reuse the SES infrastructure approach documented in `../fishdan-terraform/aws/ses.tf` instead of inventing a separate mail path.
- Treat authentication, verification, domain ownership, and visibility as high-scrutiny security areas.

## Proposed Delivery Phases
1. Account and verification foundation
- Introduce account storage, password hashing, login flow, verification-code generation, SES delivery integration, and send-rate enforcement.
- Establish the authenticated session model needed for later phases.

2. Private-account enforcement and self-service
- Reserve only verified email identities for self-managed membership and leave unreserved emails on the existing public flow.
- Add the user settings surface for password changes, organization list, supervisor/department edits, and self-add to organizations.

3. Official domains and claim flow
- Add domain-verification records, TXT challenge generation, DNS verification checks, retry pacing, and first-admin assignment.
- Support both brand-new official domains and claims for preexisting domains.

4. Admin workflow and provisional members
- Add per-organization admin status, provisional self-joins for official domains, approval actions, and admin-to-admin delegation controls without applying those controls to non-owned organizations.

5. Org chart privacy
- Add org-level public/private visibility settings.
- Update org chart endpoints and pages to suppress private org trees from public viewers.

## Architectural Expectations
- Introduce explicit domain models for:
- user accounts
- verification codes or verification challenges
- domain verification challenges
- org membership state, including provisional/approved status
- org-level admin permissions
- Prefer straightforward server-rendered flows unless a richer client interaction is clearly required.
- Passwords must be hashed; verification codes should be stored in a form and lifecycle that limits abuse and replay.
- The design should prevent production abuse from repeated email sends or DNS rechecks.

## Likely Affected Paths
- `src/main/java/com/fishdan/myorgchart/...`
- `src/main/resources/templates/...`
- `src/main/resources/application*.properties`
- `src/test/java/com/fishdan/myorgchart/...`
- `tests/e2e/...`
- `README.md`
- `progress.ai`

## Verification Strategy
- Automated tests for account creation, login, verification throttling, ownership enforcement, provisional membership rules, admin actions, and org chart privacy.
- End-to-end tests for the highest-risk user flows:
- register and verify
- blocked add for private-account email
- public self-entry for an unreserved email in a non-owned organization without login
- self-add to org
- official-domain join pending approval
- private org chart access denial
- Manual or integration verification for SES and DNS-dependent flows where full automated end-to-end verification is not practical in local development.

## Key Design Decisions To Finalize During Implementation
- Session mechanism: Spring Security-backed session auth vs lighter custom session handling.
- Verification-code lifetime and whether codes are single-use with rolling invalidation.
- Whether the 5-per-day email cap should be rolling 24 hours or calendar-day based.
- Whether claiming an existing domain transfers admin control immediately or preserves current admins until explicit review.
- Safety rule for revoking admin status when an org has only one admin left.
- Whether private org charts are hidden from everyone except approved members/admins or simply from unauthenticated/public viewers.
- How to express the owned-versus-non-owned organization boundary cleanly in the data model without regressing the free flow.

## Phase 2 Decisions
### T0410 Authentication And Session Approach
- Use server-side session authentication for browser flows in v1.
- Store authenticated account state in the standard servlet `HttpSession` behind the normal `JSESSIONID` cookie.
- Do not use JWT for the initial implementation.
- Scope authentication only to private-account, settings, domain-claim, admin, and private-chart access flows.
- Keep the public create-organization, create-person, and public org-chart entry flows available without login where the product rules allow.

### T0411 Persistence Model
- Keep `Person` as the org-chart row model so the free self-entry flow remains intact.
- Keep `Organization` as the organization record, but add an ownership boundary so each org is either:
- `OPEN` for the current free/public model
- `OFFICIAL` for a claimed or verified domain with admin controls
- Add `Account`:
- `id`
- `email` unique
- `passwordHash`
- `emailVerifiedAt` nullable
- `createdAt`
- `lastLoginAt` nullable
- Add `EmailVerificationCode`:
- `id`
- `accountId`
- `email`
- `codeHash`
- `status` (`PENDING`, `CONSUMED`, `EXPIRED`, `INVALIDATED`)
- `createdAt`
- `expiresAt`
- `consumedAt` nullable
- Add `OrganizationMembership`:
- `id`
- `organizationId`
- `accountId`
- `personId` nullable until the user creates or links their chart row
- `role` (`MEMBER`, `ADMIN`)
- `approvalStatus` (`APPROVED`, `PROVISIONAL`, `REJECTED`)
- `createdAt`
- `approvedAt` nullable
- This membership table carries ownership, admin, and approval semantics without forcing every public `Person` row to become an authenticated record.
- Add `DomainVerificationChallenge`:
- `id`
- `organizationId`
- `domain`
- `requestedByAccountId`
- `challengeToken`
- `status` (`PENDING`, `VERIFIED`, `FAILED`, `SUPERSEDED`)
- `createdAt`
- `lastCheckedAt` nullable
- `verifiedAt` nullable
- Owned-versus-non-owned boundary:
- an organization is `OPEN` until a verified domain challenge succeeds
- when it becomes `OFFICIAL`, admin and approval rules activate
- open organizations keep the public self-entry path

### T0412 Verification-Code Abuse Controls
- Verification codes are single-use.
- A newly sent code invalidates any older pending code for the same account/email.
- Code lifetime is 15 minutes from send time.
- Resend is blocked until 5 minutes have passed since the most recent send for that email/account.
- Daily cap means no more than 5 sends in the trailing 24 hours for that email/account.
- The cap is enforced by counting `EmailVerificationCode` send records in the last 24 hours, which avoids timezone ambiguity.

### T0413 Domain Claim Control And Last-Admin Safety Rule
- The first successful verification for an `OPEN` organization changes it to `OFFICIAL`.
- The verifying account becomes an approved admin membership for that organization.
- This phase does not support hostile takeover of an already `OFFICIAL` organization through DNS re-claim.
- "Existing domain claim" in v1 means claiming an already-existing org record that is still `OPEN`.
- Last-admin safety rule:
- the system must reject any action that would leave an `OFFICIAL` organization with zero approved admins
- admin revocation, membership removal, or role downgrade must preserve at least one approved admin
- explicit admin transfer can be supported later, but zero-admin state is never allowed

### T0414 Private Org-Chart Authorization
- Private org charts are visible only to authenticated users who are approved members of that organization.
- Approved admins are included because they are approved members with the `ADMIN` role.
- Provisional members, rejected members, unauthenticated visitors, and users from other organizations cannot view a private chart.
- Public charts remain visible to everyone.

### T0415 Free-Flow Non-Regression Rule
- The public self-entry flow remains the default path for `OPEN` organizations.
- If an email address is not reserved by a verified private account, anyone can add that email to an `OPEN` organization without login.
- If an email address is reserved by a verified private account, only that account owner can create or manage that identity in org membership.
- `OFFICIAL`-organization controls must not spill back into `OPEN` organizations.
- Regression tests must prove that a manager can still tell coworkers to visit the site and add themselves without account creation for an `OPEN` organization.

## External Dependencies
- Amazon SES configuration documented in `../fishdan-terraform/aws/ses.tf`
- DNS TXT verification against public DNS for official-domain and claim flows

## Non-Goals For The First Implementation Slice
- Password reset by email
- Multi-factor authentication
- Invitation workflows
- Cross-org global admin tooling
