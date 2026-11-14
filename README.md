# 🏦 Fintech Wallet API

![Java](https://img.shields.io/badge/Java-21-orange)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-4.1.1-brightgreen)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-16-blue)
![Docker](https://img.shields.io/badge/Docker-Compose-2496ED)
[![CI](https://github.com/rogerioduenas/wallet-api/actions/workflows/ci.yml/badge.svg)](https://github.com/rogerioduenas/wallet-api/actions/workflows/ci.yml)

A backend RESTful API built with Java 21 and Spring Boot for managing digital wallets, user accounts, role-based access control, and transactional operations.

---

## 📖 About the Repository

This repository implements a financial wallet service focused on data integrity, database concurrency control, stateless authentication, and automated multi-layered testing. It brings together core backend practices: schema versioning, pessimistic locking, containerized execution, and continuous integration.

### 🎯 Core Capabilities

- **User & Role Management:** Customer self-registration, admin-only user provisioning, paginated user listing, and account status management (`ACTIVE` / `BLOCKED`).
- **Stateless Security:** JWT-based authentication (`jjwt`) with custom security filters, `BCrypt` password hashing, and Role-Based Access Control (`ROLE_USER` vs. `ROLE_ADMIN`).
- **Transactional Operations:** Deposits, withdrawals, and peer-to-peer wallet transfers with balance validation, active-status verification, and self-transfer prevention.
- **Dynamic Statement Filtering:** Paginated transaction history for customers with dynamic JPA `Specification` filters by date range (`Instant`, UTC) and transaction type (`DEPOSIT`, `WITHDRAW`, `TRANSFER`), plus a paginated audit statement for admins.

---

## 🏗️ Architecture & Engineering Decisions

The project follows a layered architecture that separates HTTP routing, business logic, persistence, security, and error handling:

```text
src/main/java/com/rogerio/wallet_api/
├── config/ # SecurityFilterChain, JWT filter, delegated auth handlers, UTC Clock & web config
├── domain/ # JPA entities (User, Wallet, Transaction), enums, repositories & JPA Specifications
├── dto/ # Immutable request & response records with Bean Validation
├── exception/ # Domain-specific business exceptions
├── handler/ # GlobalExceptionHandler returning RFC 9457 ProblemDetail responses
├── infra/config/ # MasterAdminSeeder (CommandLineRunner for initial admin bootstrap)
├── mapper/ # Explicit domain-to-DTO mappers
├── service/ # Transactional business rules, JWT generation, and wallet locking
└── web/ # Versioned REST controllers (/api/v1/...) & URI builders
```

### 🔹 Key Technical Highlights

**Data integrity & concurrency**
- **Money handled safely:** `BigDecimal` mapped to `NUMERIC(19, 2)`. Deposit, withdraw, and transfer take a pessimistic write lock on the wallet to prevent lost updates, while reads (balance, statements) use a separate lock-free lookup in read-only transactions.
- **Versioned schema:** Flyway migrations (V1 to V6) with Hibernate `ddl-auto: validate` and `open-in-view` disabled.

**Security**
- **Stateless JWT + RBAC:** custom authentication filter. The user's role and status are loaded from the database on every request, so a blocked user loses access immediately, even with a still-valid token.
- **Login hardening:** BCrypt hashing and the same error response for unknown email and wrong password.

**API design**
- **Standardized errors:** a single `@RestControllerAdvice` returns RFC 9457 `ProblemDetail` for validation, domain, and Spring Security (401/403) errors.
- **Dynamic, paginated statements:** JPA `Specification` filters (date range, type) with `Pageable` and DTO-based page serialization, plus a paginated admin audit view.
- **Clean layering:** controllers, services, repositories, immutable `record` DTOs, explicit mappers, and domain-specific exceptions.

**Quality & delivery**
- **Multi-layer tests:** 100+ tests across unit, `@WebMvcTest` slices, and E2E with Testcontainers on a real PostgreSQL. An injected UTC `Clock` keeps time deterministic.
- **CI & workflow:** GitHub Actions on every pull request. Changes follow issue → PR → CI → merge, never straight to `main`.
- **Containerized:** multi-stage Dockerfile and Docker Compose with a database healthcheck.

---

## 🔌 API Endpoints Overview

All endpoints are prefixed with `/api/v1`.

### Public Endpoints

| Method | Endpoint | Description |
| :--- | :--- | :--- |
| `POST` | `/customers/register` | Register a new customer and initialize a zero-balance wallet |
| `POST` | `/auth/login` | Authenticate credentials and receive a Bearer JWT |

### Customer Endpoints (`ROLE_USER`)

| Method | Endpoint | Description |
| :--- | :--- | :--- |
| `GET` | `/wallets/balance` | Retrieve the authenticated user's current balance |
| `POST` | `/wallets/deposit` | Deposit funds into the authenticated user's wallet |
| `POST` | `/wallets/withdraw` | Withdraw funds (validates sufficient balance) |
| `POST` | `/wallets/transfer` | Transfer funds to another active user's wallet |
| `GET` | `/wallets/statement` | Paginated transaction statement with optional `startDate`, `endDate` and `type` filters |

### Admin Endpoints (`ROLE_ADMIN`)

| Method | Endpoint | Description |
| :--- | :--- | :--- |
| `POST` | `/admins/register` | Register a new administrator account |
| `GET` | `/admins/users` | List registered users with pagination and sorting |
| `PATCH` | `/admins/users/{id}/status` | Update a user's account status (`ACTIVE` or `BLOCKED`) |
| `GET` | `/admins/wallets/{userId}/statement` | Audit the paginated transaction statement of a specific user |

---

## 🚀 How to Run

### Option 1: Docker Compose (recommended)

Runs both the PostgreSQL 16 database and the Spring Boot API in isolated containers.

1. Clone the repository:

```bash
git clone https://github.com/rogerioduenas/wallet-api.git
cd wallet-api
```

2. Configure the environment variables:

```bash
cp .env.example .env
```

The defaults are meant for local development only. Set your own secrets (JWT key and master admin credentials) in any shared or deployed environment.

3. Build and start the containers:

```bash
docker compose up --build
```

The API will be available at `http://localhost:8080`. A Master Admin user is seeded on startup using the configured `ADMIN_EMAIL` and `ADMIN_PASSWORD`.

### Option 2: Run locally with Maven

Requires JDK 21 and a running PostgreSQL instance (or start only the database container with `docker compose up db-container -d`):

```bash
./mvnw spring-boot:run
```

### Run the test suite

Requires Docker running locally for the Testcontainers-based E2E tests:

```bash
./mvnw clean test
```

---

## 🧪 Quick Start

```bash
# 1. Register a customer
curl -X POST http://localhost:8080/api/v1/customers/register \
-H "Content-Type: application/json" \
-d '{"fullName":"John Doe","email":"john@email.com","password":"password123"}'

# 2. Log in and copy the token from the response
curl -X POST http://localhost:8080/api/v1/auth/login \
-H "Content-Type: application/json" \
-d '{"email":"john@email.com","password":"password123"}'

TOKEN="paste-the-jwt-here"

# 3. Deposit funds
curl -X POST http://localhost:8080/api/v1/wallets/deposit \
-H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" \
-d '{"amount": 100.00}'

# 4. Check the balance
curl http://localhost:8080/api/v1/wallets/balance -H "Authorization: Bearer $TOKEN"

# 5. Query the statement (paginated, filterable)
curl "http://localhost:8080/api/v1/wallets/statement?type=DEPOSIT&page=0&size=10" \
-H "Authorization: Bearer $TOKEN"
```

---

## 🧪 Testing & Continuous Integration

The repository has 100+ automated tests across three levels, executed on every push and pull request through GitHub Actions (`.github/workflows/ci.yml`):

- **Unit Tests** (`service/`, `config/security/`): Isolated tests with JUnit 5, Mockito, and AssertJ covering business rules, token validation, and security filters.
- **Web Slice Tests** (`web/`): Controller and security rule verification using `@WebMvcTest`, `MockMvc`, and `@MockitoBean`.
- **End-to-End & Integration Tests** (`e2e/`, `infra/config/`): Full application context tests (`@SpringBootTest`) backed by Testcontainers (`postgres:16-alpine`), validating HTTP flows, Flyway migrations, security filters, and persistence against a real PostgreSQL instance.

---

## 🔄 Development Workflow

Every change follows the same flow: **issue → branch → pull request → CI checks → merge**. Nothing is committed directly to `main`.

- **Issues and pull requests** use templates (context, technical scope, acceptance criteria) and are linked with `Closes #N`.
- **Commits** follow the Conventional Commits style (`feat(...)`, `fix(...)`, `docs(...)`).
- **CI** runs the full test suite on every pull request, and a change is merged only when it passes.

The issues, pull requests, and commit history are public. They show how the project evolved, including the fixes already merged, and the pending ones are tracked in the roadmap below.

---

## 🗺️ Known Limitations & Roadmap

This project is under active improvement. The items below are tracked as [issues](https://github.com/rogerioduenas/wallet-api/issues):

- **Concurrency:** Transfers lock the sender wallet and then the recipient wallet. Locking in a consistent order (by ID) will prevent potential deadlocks between simultaneous opposite transfers, with a dedicated concurrency test.
- **Statements:** Statements currently list operations initiated by the wallet owner. Incoming transfers and a direction indicator (`IN` / `OUT`) are planned, along with a default sort order and a whitelist of sortable fields.
- **Validation:** Stricter input rules (monetary scale, email normalization, field size limits) and database-level `CHECK` constraints.
- **Security hardening:** Removal of development defaults for secrets, rate limiting for login and registration, and extra safeguards for admin status changes.
- **Testing:** Broader E2E coverage for every route (money flows, statements, admin operations, 401/403) and concurrency tests.
- **Delivery:** OpenAPI/Swagger documentation and a public demo deployment.

---

## 🛠️ Technologies Used

- **Language & Runtime:** Java 21 (Eclipse Temurin)
- **Framework:** Spring Boot 4.1.1 (Spring Web, Spring Security, Spring Data JPA, Bean Validation)
- **Authentication:** JSON Web Tokens (jjwt 0.12.6) and BCrypt
- **Database & Migrations:** PostgreSQL 16, Hibernate, Flyway
- **Testing:** JUnit 5, Mockito, AssertJ, Spring MockMvc, Testcontainers
- **Infrastructure & CI:** Docker, Docker Compose, Maven Wrapper, GitHub Actions

---

## 👨‍💻 Author

**Rogerio Duenas**, Java backend developer.

- GitHub: [github.com/rogerioduenas](https://github.com/rogerioduenas)
- LinkedIn: [linkedin.com/in/rogerio-duenas-3428441b1](https://www.linkedin.com/in/rogerio-duenas-3428441b1/)
- Email: [rogerioduenasdev@gmail.com](mailto:rogerioduenasdev@gmail.com)