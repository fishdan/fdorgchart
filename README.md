# fdorgchart

`fdorgchart` is a Spring Boot web app for building and viewing organization charts.

It now supports two product modes:

- a simple free mode where people can still add themselves to open organizations without logging in
- an account-managed mode with verified private identities, official-domain administration, approvals, and private org charts

## Product modes

### Free open-org mode

This mode remains a first-class path.

- create an organization identified by a domain such as `example.com`
- tell coworkers to visit the site and add themselves
- no login is required
- unreserved email addresses can still be added to `OPEN` organizations

This is the workflow that must stay simple:

1. Create an organization
2. Add people to that organization
3. View the org chart

### Private-account and official-org mode

This mode adds ownership and admin controls:

- private accounts with email + password
- SES-backed email verification codes
- verified-email ownership protection
- official-domain claims through DNS TXT verification
- admin approval for official-domain self-joins
- org chart public/private visibility controls

## Main routes

Browser pages:

- `/` - home page
- `/create-organization` - create an organization
- `/create-person` - add a person through the public flow
- `/orgchart` - enter a domain and render the org chart
- `/register` - create a private account
- `/login` - sign in
- `/verify-account` - confirm the email verification code
- `/settings` - self-service account, membership, domain, admin, and privacy controls

Application endpoints:

- `POST /api/organizations` - create an organization
- `GET /api/organizations` - list organizations
- `POST /api/people` - create a person
- `GET /api/orgchart?domain=example.com` - return org chart JSON for one domain

Development-only test helpers under the `dev` profile:

- `GET /api/dev/test-data/verification-code?email=...`
- `POST /api/dev/test-data/dns-txt?domain=...&value=...`
- `DELETE /api/dev/test-data/dns-txt?domain=...`
- `DELETE /api/dev/test-data/account?email=...`
- `DELETE /api/dev/test-data/organization?domain=...`

## Core rules

- `OPEN` organizations preserve the simple public no-login self-entry flow.
- If an email is not reserved by a verified private account, it can still be added to an open organization publicly.
- A verified private-account email can only be added or managed by that account owner.
- `OFFICIAL` organizations are admin-managed domains created or claimed through DNS TXT verification.
- Self-joins to official organizations are provisional until approved by an admin.
- Provisional members do not appear on the org chart.
- Official organizations cannot be left with zero admins.
- Official org charts can be public or private.
- Private org charts are visible only to org admins and approved members of that organization.

## Data model

### Organization

An organization has:

- `name`
- `domain`
- `email`
- `password`
- `ownershipType` as `OPEN` or `OFFICIAL`
- `chartVisibility` as `PUBLIC` or `PRIVATE`

### Person

A person has:

- `fullName`
- `email`
- `domain`
- `department`
- `supervisorEmail` (optional)
- `organization`
- `approvalStatus` as `APPROVED`, `PROVISIONAL`, or `REJECTED`

### Account and domain-control entities

The private-account and official-domain features add:

- `Account`
- `EmailVerificationCode`
- `DomainVerificationChallenge`
- `OrganizationAdmin`

## Running locally

### Requirements

- Java 17
- MariaDB
- a database the app can connect to

### 1. Create a MariaDB database

Example:

```sql
CREATE DATABASE myorgchart;
```

### 2. Set environment variables

Production and non-development runs read their database connection from environment variables:

```bash
export SPRING_DATASOURCE_URL=jdbc:mariadb://localhost:3306/myorgchart
export SPRING_DATASOURCE_USERNAME=your_db_user
export SPRING_DATASOURCE_PASSWORD=your_db_password
```

### 3. Start the app for local development

For local development, activate the `dev` Spring profile.

```bash
export SPRING_PROFILES_ACTIVE=dev
export SPRING_DATASOURCE_USERNAME=your_local_db_user
export SPRING_DATASOURCE_PASSWORD=your_local_db_password
bash ./scripts/run-dev-server.sh
```

When the `dev` profile is active and `SPRING_DATASOURCE_URL` is not set, the app defaults to:

```text
jdbc:mariadb://localhost:3306/myorgchart
```

### 4. Email verification configuration

Non-dev runtimes require SES sender configuration:

```bash
export APP_EMAIL_FROM_ADDRESS=verified-sender@example.com
export APP_EMAIL_CONFIGURATION_SET=optional-configuration-set
```

`APP_EMAIL_CONFIGURATION_SET` is optional.

### 5. DNS verification behavior

Production-style DNS verification uses public TXT lookup.

Under the `dev` profile, browser tests and local development can simulate TXT records through the dev-only endpoints instead of editing real DNS.

### 6. Start the app directly

```bash
export SPRING_PROFILES_ACTIVE=dev
bash ./mvnw spring-boot:run
```

The app listens on port `8080` by default.

## How to use it

### Free open-org flow

1. Open `/create-organization` and create an organization.
2. Open `/create-person` and add people using the same domain.
3. Leave `Supervisor Email` blank for top-level leaders.
4. Open `/orgchart`, enter the domain, and view the chart.

### Private-account flow

1. Open `/register` and create a private account.
2. Verify the email with the code sent through SES.
3. Open `/settings` to:
   - add yourself to organizations
   - update your own department and supervisor
   - start official-domain verification

### Official-domain admin flow

1. Start a domain verification challenge from `/settings`.
2. Publish the required TXT record.
3. Re-check verification from `/settings`.
4. After success, the organization becomes `OFFICIAL` and you become the first admin.
5. Approve or reject provisional members from `/settings`.
6. Grant or revoke admin status for approved verified members.
7. Toggle chart visibility between public and private.

## Validation

Backend validation:

```bash
bash ./mvnw -q test
```

Browser validation:

```bash
SPRING_DATASOURCE_USERNAME=root SPRING_DATASOURCE_PASSWORD=mysql npm run test:e2e
```

As of the completed `004-private-accounts` implementation, the Playwright suite passes with `13 passed`.

## Hosting notes

You can host this app anywhere that supports:

- Java 17
- MariaDB
- environment-variable configuration

For deployed environments:

- do not activate the `dev` profile
- provide datasource environment variables explicitly
- provide SES sender configuration if email verification is enabled

Build and run:

```bash
./mvnw clean package
java -jar MyOrgChart-0.0.1-SNAPSHOT.jar
```
