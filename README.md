# 💳 Fintech API — Spring Boot + Maven

A simple, production-ready Fintech REST API with JWT authentication, account management, and transactions.

---

## 📁 Project Structure

```
fintech-api/                                      ← ROOT DIRECTORY
│
├── pom.xml                                       ← Maven build config + dependencies
├── Dockerfile                                    ← Multi-stage Docker build (Java 17)
├── Jenkinsfile                                   ← CI/CD pipeline (build, test, sonar, docker, smoke, uat, push)
├── .env                                          ← Pipeline runtime config (local/Jenkins workspace)
├── .gitignore                                    ← Ignore secrets/artifacts (includes .env)
│
├── src/
│   ├── main/
│   │   ├── java/com/fintech/
│   │   │   │
│   │   │   ├── FintechApplication.java           ← Spring Boot entry point
│   │   │   │
│   │   │   ├── config/
│   │   │   │   ├── SecurityConfig.java           ← Spring Security + JWT filter chain
│   │   │   │   ├── UserDetailsServiceImpl.java   ← Loads user from DB for auth
│   │   │   │   └── DataSeeder.java               ← Seeds demo users on startup
│   │   │   │
│   │   │   ├── controller/
│   │   │   │   ├── AuthController.java           ← POST /api/auth/register, /login
│   │   │   │   ├── AccountController.java        ← GET/POST /api/accounts
│   │   │   │   └── TransactionController.java    ← POST /api/transactions/*
│   │   │   │
│   │   │   ├── service/
│   │   │   │   ├── AuthService.java              ← Register & login logic
│   │   │   │   ├── AccountService.java           ← Create & fetch accounts
│   │   │   │   └── TransactionService.java       ← Deposit, withdraw, transfer
│   │   │   │
│   │   │   ├── entity/
│   │   │   │   ├── User.java                     ← User JPA entity
│   │   │   │   ├── Account.java                  ← Account JPA entity
│   │   │   │   └── Transaction.java              ← Transaction JPA entity
│   │   │   │
│   │   │   ├── repository/
│   │   │   │   ├── UserRepository.java           ← User DB queries
│   │   │   │   ├── AccountRepository.java        ← Account DB queries
│   │   │   │   └── TransactionRepository.java    ← Transaction DB queries
│   │   │   │
│   │   │   ├── dto/
│   │   │   │   └── Dto.java                      ← All request/response DTOs + ApiResponse<T>
│   │   │   │
│   │   │   ├── security/
│   │   │   │   ├── JwtUtils.java                 ← Generate & validate JWT tokens
│   │   │   │   └── JwtAuthFilter.java            ← Intercepts requests, sets auth context
│   │   │   │
│   │   │   └── exception/
│   │   │       ├── FintechException.java         ← Custom runtime exception
│   │   │       └── GlobalExceptionHandler.java   ← Maps exceptions to HTTP responses
│   │   │
│   │   └── resources/
│   │       └── application.properties            ← DB, JWT, server config
│   │
│   └── test/
│       └── java/com/fintech/                     ← Test directory
```

---

## 🚀 Getting Started

### Prerequisites

- Java 17+
- Maven 3.8+

### Run the app

```bash
cd fintech-api
mvn spring-boot:run
```

Server starts on: `http://localhost:8080`
H2 Console: `http://localhost:8080/h2-console` (JDBC URL: `jdbc:h2:mem:fintechdb`)

### Run with Docker (Port 8081)

```bash
docker build -t fintech-api .
docker run --rm -p 8081:8081 fintech-api
```

Container app URL: `http://localhost:8081`

---

## 🔐 Demo Credentials (auto-seeded)

| Role  | Email             | Password   |
| ----- | ----------------- | ---------- |
| ADMIN | admin@fintech.com | Admin@1234 |
| USER  | jane@fintech.com  | Jane@1234  |

---

## 📡 API Endpoints

### Auth

| Method | Endpoint           | Auth | Description       |
| ------ | ------------------ | ---- | ----------------- |
| POST   | /api/auth/register | None | Register new user |
| POST   | /api/auth/login    | None | Login, get JWT    |

### Accounts

| Method | Endpoint                      | Auth   | Description          |
| ------ | ----------------------------- | ------ | -------------------- |
| POST   | /api/accounts                 | Bearer | Create account       |
| GET    | /api/accounts                 | Bearer | List my accounts     |
| GET    | /api/accounts/{accountNumber} | Bearer | Get specific account |

### Transactions

| Method | Endpoint                                  | Auth   | Description         |
| ------ | ----------------------------------------- | ------ | ------------------- |
| POST   | /api/transactions/deposit                 | Bearer | Deposit funds       |
| POST   | /api/transactions/withdraw                | Bearer | Withdraw funds      |
| POST   | /api/transactions/transfer                | Bearer | Transfer funds      |
| GET    | /api/transactions/history/{accountNumber} | Bearer | Transaction history |

---

## 📦 Sample Requests

### Register

```json
POST /api/auth/register
{
  "fullName": "John Smith",
  "email": "john@example.com",
  "password": "Pass@1234",
  "phoneNumber": "+19876543210"
}
```

### Login → get token

```json
POST /api/auth/login
{
  "email": "john@example.com",
  "password": "Pass@1234"
}
```

### Create Account (use Bearer token)

```json
POST /api/accounts
Authorization: Bearer <token>
{
  "accountType": "SAVINGS",
  "currency": "USD"
}
```

### Transfer

```json
POST /api/transactions/transfer
Authorization: Bearer <token>
{
  "fromAccountNumber": "FT0000000001",
  "toAccountNumber": "FT0000000002",
  "amount": 100.00,
  "description": "Rent payment"
}
```

---

## 🛠 Tech Stack

- **Java 17**
- **Spring Boot 3.2** (Web, Security, Data JPA, Validation)
- **H2** in-memory database (swap for PostgreSQL/MySQL easily)
- **JWT** via `jjwt 0.11.5`
- **Lombok**
- **BCrypt** password hashing

---

## ⚙️ Jenkins CI/CD

The repository includes a declarative `Jenkinsfile` with these stages:

1. Checkout
2. Load Environment Config (`.env`)
3. Build (`mvn clean compile`)
4. Unit Test (`mvn test`)
5. Package (`mvn package -DskipTests`)
6. SonarQube Analysis
7. Quality Gate
8. Docker Build
9. Run Container
10. Smoke Test (app reachability)
11. UAT (credentialed login)
12. Push Image to Docker Hub

### Required `.env` Keys

```env
APP_NAME=fintech-api
APP_PORT=8081
CONTAINER_NAME=fintech-api-ci

DOCKERHUB_REPO=your-dockerhub-username/fintech-api
DOCKER_CREDENTIALS_ID=dockerhub-credentials
SMOKE_TEST_CREDENTIALS_ID=fintech-smoke-login-credentials

SONARQUBE_SERVER=sonarqube-server
SONAR_PROJECT_KEY=fintech-api
```

> Note: `.env` is ignored by git. Ensure it exists in the Jenkins workspace/agent before running the pipeline.

### Jenkins Credentials Needed

- `dockerhub-credentials` → **Username with password** for Docker Hub
- `fintech-smoke-login-credentials` → **Username with password** used for UAT login (`/api/auth/login`)
