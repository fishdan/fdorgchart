# Spec: Private Accounts, Domain Ownership, And Org Privacy

## Context
The current application treats people and organizations as open records that can be added through shared forms. There is no concept of a user-owned identity, verified email ownership, private membership control, domain ownership verification, admin-managed approvals, or private org chart visibility.

The next major feature set introduces account ownership and organizational control:

- a user can create a private account tied to their email address
- email ownership is verified with a code sent through the SES setup documented in `../fishdan-terraform/aws/ses.tf`
- verified users can prevent others from adding their email to organizations
- verified users can manage their own org membership details
- verified users can verify domain ownership through DNS TXT records
- official or claimed domains gain admin control and approval workflows
- org charts can become private

Because this is a large cross-cutting change, the work must be broken into phases with explicit dependencies and clear rules around ownership, rate limits, approvals, and visibility.

## Scope
- Add user accounts identified by email address and secured by password login.
- Add email verification with SES-backed code sending and rate limits.
- Add private-account protections that prevent others from adding a verified user’s email to an org.
- Add a user settings area for self-service profile and membership management.
- Add domain verification and claiming via DNS TXT records.
- Add org admin roles, delegated admin management, and provisional membership approval for official domains.
- Add org chart visibility controls for public vs private organizations.

## Out of Scope
- Single sign-on, OAuth, social login, or passwordless login.
- Broad redesign of the entire frontend or navigation beyond the pages required for this feature set.
- Fine-grained RBAC beyond the user/admin rules required here.
- Multi-tenant billing, invitations, or enterprise seat management.
- Replacing SES infrastructure outside the application’s integration needs.
- Removing or login-gating the existing free self-entry flow for unreserved emails in non-owned organizations.

## Core Product Rules
- The existing free self-entry model remains a first-class product mode.
- A manager must still be able to invite an organization to populate its chart by telling people to visit the site and enter themselves without logging in.
- If an email address is not reserved by a verified private account, it may still be added through the public flow.
- If an organization is not owned, claimed, or official-domain-controlled, public self-entry must remain available.
- Browser authentication for private-account features uses server-side session auth in v1 rather than JWT.
- Organizations have an ownership boundary:
- `OPEN` organizations keep the free public self-entry model
- `OFFICIAL` organizations are verified/claimed domains that can enforce admin and approval controls
- A private account is created with email address plus password.
- Email address is the account username.
- A private account becomes verified only after the user correctly provides the email verification code that was sent to that address.
- Verification-code sending is rate limited to:
- at most once every 5 minutes per email address
- at most 5 sends per email address per rolling day or calendar day, to be finalized during implementation design
- Once an email address belongs to a verified private account, no one except that account owner may add that email address to an organization.
- If another user attempts to add that email address to an org, the UI must show:
- `"<email address> has a private account. Please send an email to <email address> asking them to add themselves to <organization>"`
- A verified private user can:
- change their password
- see the organizations they belong to
- edit their own supervisor and department within those organizations
- add themselves to new organizations
- Any verified user can attempt to make a domain official by completing DNS TXT verification.
- The application must tell the user the required TXT record value.
- Domain verification attempts may be checked no more than once every 10 minutes per domain verification flow.
- The account that successfully verifies a domain becomes that organization’s first admin.
- A verified user can also claim an existing domain through the same DNS TXT verification method.
- In v1, claiming an existing domain means claiming an already-existing `OPEN` organization; it does not permit DNS takeover of an already `OFFICIAL` organization.
- For an official domain, self-added users are provisional until an admin approves them.
- Provisional users are members of the organization record but do not appear on the org chart until approved.
- An admin can grant or revoke admin status for other people in the same organization tree, subject to implementation-defined safety rules for the last admin.
- An admin can configure whether an org chart is public or private.
- If a chart is private, entering the domain on the org chart page must show a message that the organization’s chart is private instead of exposing the tree.
- Private verification codes are single-use, expire after 15 minutes, and a newer send invalidates older pending codes for that account/email.
- Private org charts are visible only to approved members of that organization.

## Scope Breakdown By Phase

### Phase A: Accounts And Email Verification
- Account creation
- password-based login
- SES-backed verification code send/confirm flow
- verification send throttling and abuse controls

### Phase B: Private Membership Ownership
- Reserve only verified email addresses for self-managed org membership while preserving the public flow for unreserved emails
- block non-owner additions for private-account emails
- user settings page for password change and self-managed org profile edits

### Phase C: Official Domains And Claims
- Domain verification request flow
- DNS TXT instructions and retry pacing
- official-domain creation
- claim flow for existing domains
- first-admin assignment

### Phase D: Admin Controls And Approval Workflow
- Admin role management
- provisional membership for official domains
- admin approval/rejection flow
- admin-facing membership review controls

### Phase E: Org Chart Privacy
- Public/private org chart setting
- org chart access restrictions for private organizations
- user-facing private-chart message

## Requirements
- R1: The system must support account registration using email address and password.
- R2: The system must support password-based login for existing accounts.
- R2a: Browser login state for private-account features must use server-side session authentication in v1.
- R3: The system must integrate with the SES-backed email sending approach documented in `../fishdan-terraform/aws/ses.tf` for verification emails.
- R4: The system must support verification-code confirmation and persist verified-account state.
- R5: Verification-code sending must be rate limited to once every 5 minutes per email address.
- R6: Verification-code sending must be capped at 5 sends per email address per day.
- R7: A verified private account email address must be self-owned for organization membership changes.
- R7a: If an email address is not reserved by a verified private account, the public add-person flow must continue to allow that address to be added to a non-owned organization without login.
- R8: Non-owners attempting to add a verified private-account email to an org must receive the specified user-facing message naming the email and organization.
- R9: Verified users must have a settings area for password change, organization membership listing, department editing, and supervisor editing.
- R10: Verified users must be able to add themselves to organizations from the authenticated account flow.
- R11: Verified users must be able to start domain officialization or claim flows by receiving a required TXT record value to publish in DNS.
- R12: DNS verification checks must be limited to at most one verification attempt every 10 minutes per active domain verification flow.
- R13: The account that successfully verifies a domain must become the first admin for that organization.
- R14: Existing `OPEN` domains must be claimable through the DNS verification flow.
- R15: For official domains, self-added users must be stored as provisional until approved by an admin.
- R15a: The provisional-membership rule applies only to official or claimed domains and must not block the public self-entry flow for non-owned organizations.
- R16: Provisional users must not appear on the org chart until approved.
- R17: Org admins must be able to grant and revoke admin status for other users in the organization according to documented safety rules.
- R18: Org admins must be able to mark an org chart public or private.
- R19: Private org charts must return or render a private-organization message instead of exposing org structure.
- R19a: Private org charts must be viewable only by approved members of that organization.
- R20: The phased implementation plan must identify safe delivery slices so this feature can be built incrementally.
- R21: `progress.ai` must record the chosen phase breakdown, notable rule decisions, referenced infrastructure dependencies, and validation results as implementation proceeds.

## Success Criteria
- S1: A user can register, receive a verification code by email, confirm it, and become a verified private account holder.
- S2: A verified private-account email cannot be added to an organization by anyone else.
- S2a: An unreserved email address can still be added through the public flow for a non-owned organization without account creation.
- S3: A verified user can manage their own org membership profile through settings.
- S4: A verified user can verify or claim a domain with DNS TXT verification and become the initial org admin.
- S5: Official-domain joins are provisional until admin approval and do not appear on the org chart before approval.
- S6: Admins can manage admin privileges and org chart visibility.
- S7: When an org chart is private, unauthorized public lookup reveals only the private-chart message and not the organization structure.
- S8: The work is decomposed into implementation phases and task groups that can be delivered in sequence.
