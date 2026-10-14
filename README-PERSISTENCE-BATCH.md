# API Dependency Radar — Persistence and REST API batch

This batch adds the first end-to-end backend workflow on top of the existing OpenAPI detector and fetcher.

## Start local PostgreSQL

From the project root:

```powershell
docker compose up -d
docker compose ps
```

The database defaults to `localhost:5432`, database `api_dependency_radar`, username `postgres`, and password `postgres`. Override the password with `DB_PASSWORD` when needed. These defaults are for local development only.

## Run tests

```powershell
mvn clean test
```

The Maven test JVM is configured to use `Asia/Kolkata` so the local JVM/PostgreSQL timezone issue does not depend on a temporary `JAVA_TOOL_OPTIONS` value.

## Run the backend

```powershell
mvn spring-boot:run
```

The API listens on `http://localhost:8080` by default. Hibernate currently creates/updates local tables using `spring.jpa.hibernate.ddl-auto=update`; use versioned database migrations before production.

## REST endpoints

### Register a dependency

`POST /api/dependencies`

```json
{
  "name": "Example API",
  "providerName": "Example Provider",
  "sourceUrl": "https://api.example.com/openapi.yaml",
  "description": "API used by my application"
}
```

The server validates/fetches the URL, parses the OpenAPI document, and saves the document as the initial baseline. Returns `201 Created` and a `Location` header.

### List dependencies

`GET /api/dependencies`

### Get one dependency

`GET /api/dependencies/{id}`

### Check for changes now

`POST /api/dependencies/{id}/check`

Fetches the current OpenAPI document, compares it to the saved baseline, stores newly detected changes, and advances the baseline after a successful comparison. Returns the changes detected in that run.

### View change history

`GET /api/dependencies/{id}/changes`

### Delete a dependency

`DELETE /api/dependencies/{id}`

Deletes its recorded changes and dependency in one transaction.

## Notes

- V1 currently supports OpenAPI URLs only (`sourceType: OPENAPI`).
- Detection is deterministic; this batch does not add scheduled monitoring or notifications.
- Endpoints are not authenticated yet. Keep the service local/private until the authentication and authorization phase is implemented.
- The URL validator blocks HTTP and local/private addresses by default. DNS-rebinding and connection-time SSRF hardening remain production-security work.
- The old `api-radar-postgres-old` container, if still present, is intentionally not deleted by this batch.
