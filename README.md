# fdorgchart

`fdorgchart` is a small Spring Boot web app for building and viewing a simple organization chart.

It lets you:

- create an organization identified by a domain such as `example.com`
- add people to that organization
- assign each person an optional supervisor email
- render the reporting structure as a tree in the browser

The project uses:

- Java 17
- Spring Boot
- Thymeleaf server-rendered templates
- Spring Data JPA
- MariaDB
- Playwright for browser-level end-to-end tests

## What the app does

The app has three main flows:

1. Create an organization
2. Add people to that organization
3. View the org chart for that organization domain

The org chart is built from `supervisorEmail` relationships:

- people without a supervisor are treated as top-level nodes
- people with a `supervisorEmail` are shown under that supervisor
- the chart is filtered by organization domain

This is intentionally a lightweight org-chart manager, not a full identity or HR system.

## Main routes

Browser pages:

- `/` - home page
- `/create-organization` - create an organization
- `/create-person` - add a person
- `/orgchart` - enter a domain and render the org chart

Application endpoints:

- `POST /api/organizations` - create an organization
- `GET /api/organizations` - list organizations
- `POST /api/people` - create a person
- `GET /api/orgchart?domain=example.com` - return org chart JSON for one domain

## Data model

### Organization

An organization has:

- `name`
- `domain`
- `email`
- `password`

Notes:

- organization domains must be unique
- organization passwords are hashed before being stored

### Person

A person has:

- `fullName`
- `email`
- `domain`
- `department`
- `supervisorEmail` (optional)
- `organization`

Notes:

- a person can only be created if the organization domain already exists
- person records are unique by `email + domain`
- person domains are normalized to lowercase before saving

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

For local development, activate the `dev` Spring profile. That profile intentionally points the app at a local MariaDB instance and keeps this localhost behavior out of the default production-safe configuration.

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

You can still override the local dev URL explicitly by setting `SPRING_DATASOURCE_URL`.

If you prefer not to use the helper script, this is the equivalent direct command:

```bash
export SPRING_PROFILES_ACTIVE=dev
bash ./mvnw spring-boot:run
```

### 4. Start the app for production or other deployed environments

Production and deployed environments should not activate the `dev` profile. They should continue providing all datasource settings explicitly:

```bash
export SPRING_DATASOURCE_URL=jdbc:mariadb://your-db-host:3306/myorgchart
export SPRING_DATASOURCE_USERNAME=your_db_user
export SPRING_DATASOURCE_PASSWORD=your_db_password
./mvnw spring-boot:run
```

### 5. Build and run the jar

With the Maven wrapper:

```bash
./mvnw spring-boot:run
```

Or build a jar and run it:

```bash
./mvnw clean package
java -jar MyOrgChart-0.0.1-SNAPSHOT.jar
```

The app listens on port `8080` by default.

Open:

```text
http://localhost:8080
```

## How to use it

1. Open `/create-organization` and create an organization using the domain you want to manage.
2. Open `/create-person` and add people using that exact same domain.
3. Leave `Supervisor Email` blank for top-level leaders.
4. Set `Supervisor Email` for everyone else to build the reporting tree.
5. Open `/orgchart`, enter the domain, and view the chart.

Example flow:

- Create organization with domain `example.com`
- Add `ceo@example.com` with no supervisor
- Add `manager@example.com` with supervisor `ceo@example.com`
- Add `dev@example.com` with supervisor `manager@example.com`

The chart will render as CEO -> Manager -> Dev.

## Hosting your own version

You can host this app anywhere that supports a Java process plus a MariaDB database.

Minimum requirements:

- Java 17 runtime
- a MariaDB instance
- environment variables for the datasource
- a way to run `java -jar MyOrgChart-0.0.1-SNAPSHOT.jar`

### Basic deployment process

1. Provision a MariaDB database.
2. Create the three datasource environment variables:
   - `SPRING_DATASOURCE_URL`
   - `SPRING_DATASOURCE_USERNAME`
   - `SPRING_DATASOURCE_PASSWORD`
3. Do not activate the `dev` profile in deployed environments; it exists only to make local development point at `localhost`.
4. Build the application:

```bash
./mvnw clean package
```

5. Deploy the generated jar:

```bash
java -jar MyOrgChart-0.0.1-SNAPSHOT.jar
```

### Platforms this should fit easily

- a small VM running systemd
- Docker or another container platform
- Heroku-style platforms that run a `Procfile`
- Render, Railway, Fly.io, or similar services that support Java apps and external databases

This repository already includes a `Procfile`:

```text
web: java -jar MyOrgChart-0.0.1-SNAPSHOT.jar
```

That makes Procfile-based hosting straightforward after the jar is built.

### Self-hosting on a Linux VM

Typical pattern:

1. Install Java 17.
2. Install or connect to MariaDB.
3. Clone the repo.
4. Export the datasource env vars.
5. Ensure `SPRING_PROFILES_ACTIVE` is unset or set to a non-`dev` profile for the deployed service.
6. Run `./mvnw clean package`.
7. Start `java -jar MyOrgChart-0.0.1-SNAPSHOT.jar`.
8. Put Nginx or Caddy in front of it if you want TLS and a public domain.

### Running behind a reverse proxy

If you expose the app publicly, put it behind a reverse proxy such as Nginx or Caddy and forward traffic to port `8080`.

That gives you:

- HTTPS termination
- domain routing
- easier restarts and process supervision

## Database behavior

JPA is configured with:

```properties
spring.jpa.hibernate.ddl-auto=update
```

That means the app will try to update the schema automatically at startup.

This is convenient for development and simple deployments, but for stricter production environments you may eventually want explicit migrations.

## Testing

Run backend tests:

```bash
./mvnw test
```

Run browser tests:

```bash
npm install
npm run test:e2e:install
npm run test:e2e
```

The Playwright setup starts the Spring Boot app on port `18080` through `scripts/run-e2e-server.sh`.

## Current limitations

- no authentication flow for logging into an organization
- no edit or delete UI for organizations or people
- org chart relationships are based only on supervisor email strings
- no import/export workflow
- no production migration tooling yet

## Project structure

Key files:

- `src/main/java/com/fishdan/myorgchart` - Spring Boot application and controllers
- `src/main/resources/templates` - Thymeleaf templates
- `src/main/resources/application.properties` - runtime configuration
- `src/test/java` - backend tests
- `tests/e2e` - Playwright tests
