# Fintech API — CI/CD with Jenkins

A production-ready Fintech REST API built with Spring Boot, featuring JWT authentication, account management, and transactions. This repository includes a full CI/CD pipeline using Jenkins, Docker, Docker Compose, and SonarQube.

---

## Project Structure

```
ci-with-jenkins/
├── Dockerfile                  ← Multi-stage build (Maven build + JRE runtime)
├── Jenkinsfile                 ← Declarative CI/CD pipeline
├── docker-compose.yml          ← Orchestrates app + PostgreSQL
├── pom.xml                     ← Maven build config
├── settings.xml                ← Maven proxy/mirror config (gitignored)
├── .env                        ← Runtime secrets for docker-compose (gitignored)
└── src/
    └── main/
        ├── java/com/fintech/
        │   ├── FintechApplication.java
        │   ├── config/         ← Security, UserDetailsService, DataSeeder
        │   ├── controller/     ← Auth, Account, Transaction endpoints
        │   ├── service/        ← Business logic
        │   ├── entity/         ← JPA entities (User, Account, Transaction)
        │   ├── repository/     ← Spring Data repositories
        │   ├── dto/            ← Request/response DTOs
        │   ├── security/       ← JwtUtils, JwtAuthFilter
        │   └── exception/      ← GlobalExceptionHandler
        └── resources/
            └── application.properties
```

---

## Tech Stack

- **Java 17** / **Spring Boot 3.2**
- **PostgreSQL 16** (via Docker Compose)
- **JWT** (`jjwt 0.11.5`)
- **Maven 3.9** (runs inside Docker container during CI)
- **Docker** / **Docker Compose**
- **Jenkins** (Declarative Pipeline)
- **SonarQube** (code quality gate)

---

## Running Locally with Docker Compose

```bash
# Copy and populate the env file
cp .env.example .env   # or create manually (see .env Keys below)

# Start database + app
docker compose up -d
```

App: `http://localhost:8081`
PostgreSQL: `localhost:5432`

---

## Demo Credentials (auto-seeded)

| Role  | Email             | Password   |
|-------|-------------------|------------|
| ADMIN | admin@fintech.com | Admin@1234 |
| USER  | jane@fintech.com  | Jane@1234  |

---

## API Endpoints

### Auth

| Method | Endpoint             | Auth   | Description    |
|--------|----------------------|--------|----------------|
| POST   | /api/auth/register   | None   | Register user  |
| POST   | /api/auth/login      | None   | Login, get JWT |

### Accounts

| Method | Endpoint                       | Auth   | Description          |
|--------|--------------------------------|--------|----------------------|
| POST   | /api/accounts                  | Bearer | Create account       |
| GET    | /api/accounts                  | Bearer | List my accounts     |
| GET    | /api/accounts/{accountNumber}  | Bearer | Get specific account |

### Transactions

| Method | Endpoint                                   | Auth   | Description         |
|--------|--------------------------------------------|--------|---------------------|
| POST   | /api/transactions/deposit                  | Bearer | Deposit funds       |
| POST   | /api/transactions/withdraw                 | Bearer | Withdraw funds      |
| POST   | /api/transactions/transfer                 | Bearer | Transfer funds      |
| GET    | /api/transactions/history/{accountNumber}  | Bearer | Transaction history |

### Sample Requests

```json
// Register
POST /api/auth/register
{ "fullName": "John Smith", "email": "john@example.com", "password": "Pass@1234", "phoneNumber": "+19876543210" }

// Login
POST /api/auth/login
{ "email": "john@example.com", "password": "Pass@1234" }

// Create Account (Bearer token required)
POST /api/accounts
{ "accountType": "SAVINGS", "currency": "USD" }

// Transfer
POST /api/transactions/transfer
{ "fromAccountNumber": "FT0000000001", "toAccountNumber": "FT0000000002", "amount": 100.00, "description": "Rent payment" }
```

---

## CI/CD Pipeline (Jenkins)

### Pipeline Stages

```
Checkout
Prepare Environment
├── CI
│   ├── Build          (mvn clean compile)
│   ├── Unit Test      (mvn test + JUnit results)
│   └── Package        (mvn package -DskipTests)
├── Code Quality
│   ├── SonarQube Analysis
│   └── Quality Gate
├── Docker
│   └── Docker Build   (docker compose build)
└── Release
    ├── Deploy         (docker compose up)
    ├── Smoke Test     (app reachability check)
    ├── UAT            (credentialed login test)
    └── Push to Docker Hub
```

Each Maven stage runs inside a `maven:3.9-eclipse-temurin-17` container with the `settings.xml` injected from Jenkins Config File Provider (proxy, mirror config).

### Jenkins Credentials Required

| Credential ID              | Type            | Used for                          |
|----------------------------|-----------------|-----------------------------------|
| `fintech-env-file`         | Secret file     | `.env` with DB credentials        |
| `dockerhub-credentials`    | Username/Password | Docker Hub login                |
| `fintech-uat-credentials`  | Username/Password | UAT login test (`/api/auth/login`) |

> The Config File Provider plugin must be installed. The `settings.xml` is stored as a Managed File with ID `11e2101e-5b3d-4afa-894f-834c2cfacd33`.

### `.env` File (for docker-compose — DB credentials only)

```env
DB_NAME=fintech_db
DB_USER=postgres
DB_PASSWORD=your_password
APP_PORT=8081
```

> `.env` and `settings.xml` are gitignored and must be provided separately on the Jenkins agent.

### Docker Images Published

Each successful build pushes two tags to Docker Hub:

- `francdomain/fnctech-api:<build-number>` — versioned
- `francdomain/fnctech-api:latest` — always latest

---

## Dockerfile

Multi-stage build:

1. **Build stage** — `maven:3.9.9-eclipse-temurin-17`: copies `settings.xml`, runs `dependency:go-offline`, then `clean package`
2. **Runtime stage** — `eclipse-temurin:17-jre-jammy`: copies the JAR, exposes port 8080
