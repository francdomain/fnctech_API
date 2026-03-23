# Fintech App — CI/CD with Jenkins

A production-ready full-stack Fintech application featuring a Spring Boot REST API backend and a React frontend, served through Nginx. The project includes a complete CI/CD pipeline using Jenkins, Docker, Docker Compose, SonarQube, and Trivy.

---

## Project Structure

```
ci-with-jenkins/
├── Jenkinsfile                        ← Declarative CI/CD pipeline (frontend + backend)
├── docker-compose.yml                 ← Orchestrates frontend, backend, and PostgreSQL
├── .env                               ← Runtime secrets (gitignored)
│
├── frontend/
│   ├── Dockerfile                     ← Multi-stage: React build + Nginx serve
│   ├── nginx.conf                     ← Nginx config: static files + /api proxy
│   ├── package.json
│   └── src/
│
└── backend/
    ├── Dockerfile                     ← Multi-stage: Maven build + JRE runtime
    ├── Jenkinsfile                    ← Legacy (backend-only, superseded by root Jenkinsfile)
    ├── settings.xml                   ← Maven proxy config (gitignored)
    ├── pom.xml
    └── src/
        └── main/
            ├── java/com/fintech/
            │   ├── FintechApplication.java
            │   ├── config/            ← SecurityConfig, DataSeeder, UserDetailsService
            │   ├── controller/        ← Auth, Account, Transaction endpoints
            │   ├── service/           ← Business logic
            │   ├── entity/            ← JPA entities: User, Account, Transaction
            │   ├── repository/        ← Spring Data JPA repositories
            │   ├── dto/               ← Request/response DTOs
            │   ├── security/          ← JwtUtils, JwtAuthFilter
            │   └── exception/         ← GlobalExceptionHandler, FintechException
            └── resources/
                └── application.properties
```

---

## Tech Stack

### Backend
- **Java 17** / **Spring Boot 3.4**
- **Spring Security** with stateless JWT authentication
- **Spring Data JPA** / **Hibernate**
- **PostgreSQL 16** (production) / **H2** (tests)
- **Lombok**, **Bean Validation**
- **jjwt 0.11.5** for JWT
- **JaCoCo** for test coverage
- **Maven 3.9**

### Frontend
- **React** (Create React App)
- **Nginx Alpine** (production static file server + API reverse proxy)

### Infrastructure & CI/CD
- **Docker** / **Docker Compose**
- **Jenkins** (Declarative Pipeline)
- **SonarQube** (code quality and coverage gate)
- **Trivy** (container image vulnerability scanning)
- **Docker Hub** (image registry)

---

## Architecture

```
Browser
   │
   ▼
Nginx (port 80)
   ├── /           → serves React static build
   └── /api/*      → proxies to Spring Boot backend (internal, port 8080)
                            │
                            ▼
                     PostgreSQL (internal, port 5432)
```

The backend is never directly exposed to the host. All traffic flows through Nginx, which acts as the single entry point.

---

## Running Locally with Docker Compose

### Prerequisites
- Docker and Docker Compose installed
- A `.env` file at the project root (see below)

### Start all services

```bash
cd ci-with-jenkins
docker compose up --build
```

| Service   | URL                        |
|-----------|----------------------------|
| Frontend  | http://localhost            |
| API       | http://localhost/api        |
| Database  | internal only (port 5433 on host for tooling) |

### Environment Variables (`.env`)

```env
# Database
DB_HOST=db
DB_NAME=<db_name>
DB_USER=<db_user>
DB_PASSWORD=<your_db_password>
HOST_DB_PORT=5433

# Ports
HOST_FRONTEND_PORT=80

# Corporate proxy (if applicable)
PROXY_HOST=your.proxy.host
PROXY_PORT=proxy.port
NO_PROXY=localhost,127.0.0.1
```

---

## Demo Credentials (auto-seeded on startup)

| Role  | Email               | Password     |
|-------|---------------------|--------------|
| ADMIN | admin@fintech.com   | Admin@1234   |
| USER  | jane@fintech.com    | Jane@1234    |

---

## API Endpoints

### Authentication

| Method | Endpoint              | Auth   | Description         |
|--------|-----------------------|--------|---------------------|
| POST   | /api/auth/register    | None   | Register a new user |
| POST   | /api/auth/login       | None   | Login, receive JWT  |

### Accounts

| Method | Endpoint                          | Auth   | Description           |
|--------|-----------------------------------|--------|-----------------------|
| POST   | /api/accounts                     | Bearer | Create a bank account |
| GET    | /api/accounts                     | Bearer | List my accounts      |
| GET    | /api/accounts/{accountNumber}     | Bearer | Get a specific account|

### Transactions

| Method | Endpoint                                    | Auth   | Description          |
|--------|---------------------------------------------|--------|----------------------|
| POST   | /api/transactions/deposit                   | Bearer | Deposit funds        |
| POST   | /api/transactions/withdraw                  | Bearer | Withdraw funds       |
| POST   | /api/transactions/transfer                  | Bearer | Transfer between accounts |
| GET    | /api/transactions/history/{accountNumber}   | Bearer | Transaction history  |

### Sample Requests

```json
// Register
POST /api/auth/register
{
  "fullName": "John Smith",
  "email": "john@example.com",
  "password": "Pass@1234",
  "phoneNumber": "+2348012345678"
}

// Login
POST /api/auth/login
{ "email": "john@example.com", "password": "Pass@1234" }

// Create Account  (Authorization: Bearer <token>)
POST /api/accounts
{ "accountType": "SAVINGS", "currency": "NGN" }

// Transfer  (Authorization: Bearer <token>)
POST /api/transactions/transfer
{
  "fromAccountNumber": "FT0000000001",
  "toAccountNumber": "FT0000000002",
  "amount": 5000.00,
  "description": "Rent payment"
}
```

---

## CI/CD Pipeline (Jenkins)

The `Jenkinsfile` at the project root manages the full pipeline for both frontend and backend.

### Pipeline Stages

```
Initial Cleanup
Checkout
├── Backend
│   ├── Build Jar          (mvn clean package)
│   └── SonarQube Analysis (quality gate + coverage)
├── Build & Start Services
│   ├── docker compose build backend frontend
│   └── docker compose up db backend frontend
├── Security Scanning
│   ├── Trivy Scan — Backend image
│   └── Trivy Scan — Frontend image
└── Push Images
    ├── Push fnctech-api:<git-sha> to Docker Hub
    └── Push fnctech-frontend:<git-sha> to Docker Hub
```

### Jenkins Credentials Required

| Credential ID             | Type              | Purpose                          |
|---------------------------|-------------------|----------------------------------|
| `fintech-env-file`        | Secret file       | `.env` with DB and app secrets   |
| `dockerhub-credentials`   | Username/Password | Docker Hub login for push/pull   |
| `sonarqube-token`         | Secret text       | SonarQube authentication         |

> The **Config File Provider** plugin must be installed. The `settings.xml` (Maven proxy config) is stored as a Managed File with ID `1cf7f93c-2f77-4b22-8fbc-6422ea025ca5`.

### Docker Images Published

Each successful build pushes two images to Docker Hub, tagged by Git SHA:

```
<dockerhub-user>/fnctech-api:<git-sha>
<dockerhub-user>/fnctech-frontend:<git-sha>
```

### Jenkins Pipeline Configuration

In your Jenkins pipeline job, set:
- **Script Path**: `Jenkinsfile` (project root, not `backend/Jenkinsfile`)

---

## Dockerfiles

### Backend (`backend/Dockerfile`)

Multi-stage build:
1. **Build stage** — `maven:3.9-eclipse-temurin-17`: injects `settings.xml`, resolves dependencies offline, runs `mvn clean package`
2. **Runtime stage** — `eclipse-temurin:17-jre-alpine`: copies the JAR, applies Alpine security patches, exposes port 8080

### Frontend (`frontend/Dockerfile`)

Multi-stage build:
1. **Build stage** — `node:24-alpine`: installs dependencies with `npm ci`, runs `npm run build`
2. **Serve stage** — `nginx:alpine`: copies the React `build/` output and a custom `nginx.conf`, exposes port 80

---

## Security Notes

- JWT tokens expire after 24 hours (`app.jwt.expiration-ms=86400000`)
- The JWT secret should be overridden via environment variable in production
- The backend is not directly exposed to the host — only reachable through Nginx
- Trivy scans run on both images and fail the pipeline on `HIGH` or `CRITICAL` CVEs
- CORS is configured in `SecurityConfig` and handled at the Nginx layer in production
- Seed passwords (`SEED_ADMIN_PASSWORD`, `SEED_DEMO_PASSWORD`) should be overridden via environment variables in production
