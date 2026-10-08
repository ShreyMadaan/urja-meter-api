# Urja Meter API

A read-only REST API built using **Java and Spring Boot** to provide structured access to the authenticated legacy **Urja Meter Ops** portal.

Developed as part of the **Flock Energy Backend Engineering Take-Home Assignment**.

The project focuses on reverse-engineering undocumented endpoints, managing session-based authentication, and exposing electricity meter and distribution transformer information through a cleaner REST API.

## Tech Stack

- Java 24
- Spring Boot 4.0.8
- Spring Web and Spring Validation
- Spring RestClient
- Maven
- JUnit / Spring Boot Test

No database or frontend is required.

## Getting Started

### Prerequisites

- Java 21 or higher
- Maven or Maven Wrapper
- Valid Urja Meter Ops portal credentials

### Configuration

Copy `.env.example` to `.env` and configure:

```properties
URJA_EMAIL=your_email
URJA_PASSWORD=your_password
```

Alternatively, configure the values as operating-system environment variables.

The `.env` file is excluded from version control.

**Security:** Never commit portal credentials, session cookies, or signing secrets.

### Running the Application

Clone the repository:

```bash
git clone https://github.com/ShreyMadaan/urja-meter-api.git
cd urja-meter-api
```

Run on Windows:

```powershell
.\mvnw.cmd spring-boot:run
```

Or using Maven:

```bash
mvn spring-boot:run
```

The application starts at:

`http://localhost:8080`

### Running Tests

On Windows:

```powershell
.\mvnw.cmd clean test
```

Or:

```bash
mvn clean test
```

## REST API Endpoints

All endpoints use the `/api/v1` prefix.

| Method | Endpoint | Description |
|---|---|---|
| GET | `/api/v1/meters?q=&page=1` | Search meters with 1-based pagination |
| GET | `/api/v1/meters/{meterId}` | Retrieve meter metadata from structured export |
| GET | `/api/v1/meters/{meterId}/network` | Retrieve meter network hierarchy |
| GET | `/api/v1/meters/{meterId}/location` | Retrieve geographic coordinates |
| GET | `/api/v1/meters/{meterId}/consumption` | Retrieve energy consumption readings |
| GET | `/api/v1/transformers?page=1` | List distribution transformers |

### Example Requests

**List meters**

```bash
curl "http://localhost:8080/api/v1/meters?page=1"
```

**Search meters**

```bash
curl "http://localhost:8080/api/v1/meters?q=J100000&page=1"
```

**Get meter details**

```bash
curl "http://localhost:8080/api/v1/meters/J100000"
```

**Get meter network hierarchy**

```bash
curl "http://localhost:8080/api/v1/meters/J100000/network"
```

**Get meter location**

```bash
curl "http://localhost:8080/api/v1/meters/J100000/location"
```

**Get energy readings**

```bash
curl "http://localhost:8080/api/v1/meters/J100000/consumption"
```

**List transformers**

```bash
curl "http://localhost:8080/api/v1/transformers?page=1"
```

For the complete API specification, see [openapi.json](openapi.json).

## Architecture

The project follows a layered architecture to separate public API behavior from legacy portal integration.

```text
                    REST Client
                         |
                         v
                  API Controllers
                         |
                         v
                    Services
                         |
                         v
                   Portal Client
                         |
                         v
                 Session Manager
                         |
                         v
                Legacy Urja Portal
```

**Controllers:** Expose REST endpoints and validate incoming parameters.

**Services:** Implement meter and transformer retrieval logic.

**Portal Client:** Handles communication with the legacy portal and upstream error responses.

**Session Manager:** Handles login, cookie storage, expiration, and reauthentication.

## Legacy Portal Investigation

The legacy application was investigated using Chrome DevTools and authenticated browser requests.

The following upstream endpoints were identified:

| Method | Endpoint | Purpose |
|---|---|---|
| POST | `/login` | Portal authentication |
| GET | `/portal/meters/search` | Meter search and pagination |
| GET | `/meters/{meterId}` | Server-rendered meter details |
| GET | `/portal/meters/{meterId}/energy` | Energy readings |
| GET | `/portal/meters/{meterId}/geo` | Geographic coordinates |
| GET | `/portal/dts` | Distribution transformer listing |
| GET | `/portal/export` | Structured meter metadata and hierarchy |

The portal uses a session cookie:

`__Secure-better-auth.session_token`

The observed cookie includes an approximately one-hour lifetime.

Detailed findings, response structures, and assumptions are documented in [PROTOCOL.md](PROTOCOL.md).

## Design Decisions and Trade-offs

### 1. In-Memory Session Management

Authentication cookies are extracted from the upstream `Set-Cookie` header and stored in memory.

The implementation includes proactive session refresh and a single reauthentication attempt after an upstream `401 Unauthorized` response.

**Trade-off:** This approach is appropriate for a single-process take-home application but would need reconsideration for distributed production deployments.

### 2. Structured Export Instead of HTML Parsing

Meter metadata and network hierarchy are retrieved from:

`/portal/export?page=1`

This avoids parsing the portal's server-rendered HTML, which could change as its UI evolves.

**Trade-off:** The implementation assumes the observed export contains all relevant meters. If export pagination changes, a meter lookup could incorrectly return `404`.

### 3. Read-Only API

The API does not modify portal records.

No database is introduced; information is fetched directly from the upstream source.

**Trade-off:** This reduces implementation complexity but makes response availability and latency dependent on the legacy portal.

### 4. Preserve Upstream Data Semantics

Energy readings and geographic values are passed through without inventing additional meaning.

The implementation does not assume:

- An unspecified timezone for energy timestamps.
- Whether energy readings are cumulative or interval-based.
- Units or interpretations not confirmed by the upstream contract.

**Trade-off:** Clients receive data close to the original source representation rather than fully normalized domain objects.

### 5. No Cross-Request Export Cache

Export data is fetched on demand.

**Trade-off:** This prioritizes freshness but can result in repeated large upstream transfers.

### 6. Error Handling

The intended error mappings include:

| Condition | HTTP Status |
|---|---|
| Invalid request parameters | `400 Bad Request` |
| Meter not found / upstream not found | `404 Not Found` |
| Upstream authentication or communication failure | `502 Bad Gateway` |
| Missing portal configuration | `503 Service Unavailable` |

## Testing and Current Status

The following checks have been performed:

| Test | Result |
|---|---|
| Maven build | Passed |
| Existing automated tests | Passed |
| Spring Boot application startup | Passed |
| Upstream login integration | Blocked |
| End-to-end API verification | Pending |

### Known Authentication Issue

During live testing, the legacy portal rejected the backend's authentication request:

```text
HTTP 403 Forbidden
Cross-site POST form submissions are forbidden
```

The error originates from the upstream `POST /login` request.

The portal's browser-based login was observed to work, but the programmatic login request was rejected by its cross-site form submission protection.

Consequently, the API endpoints are implemented but have not been successfully verified end-to-end against the live portal.

**This limitation remains unresolved in the submitted implementation.** A supported programmatic authentication flow must be established before the integration can be considered operational.

## Known Limitations and Future Improvements

- Resolve the upstream authentication incompatibility using an authorized login mechanism.
- Add upstream contract tests and mock-server integration tests.
- Validate export pagination and completeness.
- Introduce typed, versioned response DTOs after confirming upstream data semantics.
- Add configurable request timeouts and structured request tracing.
- Consider bounded caching for frequently accessed export data.
- Handle discrepancies between transformer display names using stable transformer codes.
- Improve multi-instance session management if production deployment requires it.
- Expand automated coverage for session expiration, error handling, and malformed responses.

## Reflection

The primary engineering challenge was investigating an undocumented, authenticated legacy application and translating its observed behavior into a maintainable REST API.

The investigation revealed several important integration considerations: session cookies must be managed explicitly, structured export data can be more reliable than parsing server-rendered HTML, and upstream response fields should not be assigned semantics without verification.

Two significant assumptions remain: that the export endpoint contains all relevant meter records and that the session cookie's `Max-Age` accurately represents its usable lifetime.

The authentication failure also demonstrated that a successful browser login does not necessarily imply that the same endpoint permits server-to-server authentication.

Given additional development time, the priorities would be to establish a supported authentication flow, validate the upstream contract with controlled integration tests, and strengthen response typing, resilience, and observability.

## Documentation

- [PROTOCOL.md](PROTOCOL.md) — Reverse-engineered upstream protocol and observations.
- [openapi.json](openapi.json) — REST API specification.

---

**Author:** Shrey Madaan  
**Assignment:** Flock Energy Backend Engineering Take-Home  
**Repository:** https://github.com/ShreyMadaan/urja-meter-api