# Build & Test Commands

## Backend (Java/Spring Boot)

```bash
cd backend

# Compile
gradle compileJava

# Run tests (H2 in-memory DB, no Docker needed)
gradle test

# Build JAR
gradle bootJar

# Run locally (requires PostgreSQL on :5433)
ASOCKS_API_KEY="your-key" java -jar build/libs/clicker-0.1.0.jar
```

## Frontend (React/Vite/TypeScript)

```bash
cd frontend

# Install dependencies
npm install

# Type check
npx tsc --noEmit

# Run unit tests (vitest + testing-library)
npm test

# Run unit tests with coverage
npm run test:coverage

# Start dev server
npm run dev

# Production build
npm run build
```

## E2E Tests (Playwright)

```bash
cd e2e

# Install dependencies (first time)
npm install

# Run all E2E tests (starts backend + frontend automatically)
npx playwright test

# Run specific test file
npx playwright test specs/campaigns.spec.ts

# Run with browser visible
npx playwright test --headed
```

## Docker (PostgreSQL)

```bash
# Start PostgreSQL
docker compose up -d

# Stop
docker compose down
```

## Test Accounts

E2E tests create their own users via registration flow.
Unit tests use H2 in-memory DB (no external dependencies).
