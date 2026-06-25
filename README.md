# Clicker — Traffic Simulation Platform

Traffic simulation engine that generates realistic organic visits to target websites from diverse IPs and countries via [asocks.com](https://asocks.com) proxies.

## Architecture

```
clicker/
├── backend/                          # Spring Boot 3 + Java 21
│   ├── src/main/java/com/clicker/
│   │   ├── config/                   # Security (JWT), WebSocket (STOMP), AppProperties
│   │   ├── controller/               # REST endpoints + GlobalExceptionHandler
│   │   ├── domain/                   # JPA entities: Campaign, Scenario, Site, User, etc.
│   │   ├── dto/                      # Request/response records with validation
│   │   ├── repository/               # Spring Data JPA repositories
│   │   ├── service/
│   │   │   ├── AsocksService.java          # Proxy acquisition, pool management, IP rotation
│   │   │   ├── AuthService.java            # Register/login with BCrypt + JWT
│   │   │   ├── BrowserSimulationWorker.java # Playwright-based full browser simulation
│   │   │   ├── CampaignService.java        # Campaign CRUD + lifecycle control
│   │   │   ├── CookieSession.java          # Per-session OkHttp CookieJar
│   │   │   ├── HttpSimulationWorker.java   # OkHttp-based HTTP visits (Levels 1 & 2)
│   │   │   ├── JwtService.java             # JWT token generation and validation
│   │   │   ├── OrganicProfile.java         # 11 realistic browser/OS profiles
│   │   │   ├── ScenarioService.java        # Scenario CRUD with ordered steps
│   │   │   ├── SimulationEngine.java       # Campaign scheduler, visit dispatch, duration enforcement
│   │   │   ├── SiteService.java            # Target site CRUD
│   │   │   ├── UserAgentService.java       # User-Agent rotation pool
│   │   │   └── WebSocketPublisher.java     # Real-time STOMP events to dashboard
│   │   └── ClickerApplication.java   # Entry point, graceful shutdown
│   └── src/main/resources/
│       ├── application.yml           # DB, JWT, asocks, Playwright config
│       └── db/migration/             # Flyway migrations (PostgreSQL)
├── frontend/                         # React + Vite + TypeScript + Tailwind
│   └── src/
│       ├── api/                      # Typed API client + JWT auto-attach
│       ├── context/AuthContext.tsx    # Auth state management
│       └── pages/                    # Login, Register, Dashboard, Campaigns, Scenarios, Sites
├── e2e/                              # Playwright E2E test suite (51 tests)
└── docker-compose.yml                # PostgreSQL 16
```

## Database Schema

```
users ──< sites ──< campaigns ──< campaign_runs ──< visit_events
                     │
                     ├──< campaign_scenarios >── scenarios ──< scenario_steps
                     │
           [JSONB columns: geo_distribution, device_profile, user_agent_config, proxy_config]
```

- **Users** — email, password (bcrypt), multi-tenant isolation
- **Sites** — target website base URLs
- **Campaigns** — simulation config (level, traffic pattern, geo distribution, device mix)
- **Scenarios** — reusable step sequences (LOAD, CLICK, SCROLL, WAIT, HOVER, TYPE, etc.)
- **CampaignRuns** — execution history with visit statistics
- **VisitEvents** — per-request telemetry (status, response time, proxy used)

## Simulation Tiers

### Level 1 — HTTP Only
Simple page loads via OkHttp with rotating asocks proxies and organic browser headers. Each visit acquires a fresh proxy — unique IP per request.

```
OrganicProfile (Chrome/Firefox/Safari) → OkHttp + CookieJar → asocks proxy → target site
```

### Level 2 — Browser Navigation
Multi-path browsing with realistic delays. Navigates between pages (`/`, `/about`, `/pricing`, `/blog`, etc.) with proper `Referer` chains and per-session cookie persistence.

```
Profile → OkHttp + CookieJar + Referer chain → asocks proxy → page 1 → pause → page 2 → pause → page 3
```

### Level 3 — Full Browser
Preheated Playwright Chromium pool. Page load, natural scroll (eased, variable speed), mouse movement simulation, random link clicks, stealth scripts to hide automation. Each visit opens a fresh browser context through a unique proxy.

```
Playwright pool → browser context → stealth JS → page load → organic scroll → mouse move → click link → exit
```

## IP Rotation

```
Campaign start → asocks API: search N proxies per country → cache in pool
    ↓
For each visit:  dequeue one proxy → use → discard (consumed, unique IP)
    ↓              pool low? → background refill via asocks API
Campaign end → pool drained
```

Every single visit gets a unique IP. No IP reuse within a campaign.

## Organic Browser Profiles

11 presets in `OrganicProfile.java` covering real-world browser distribution:

| Profile | Device | Browser | Viewport |
|---------|--------|---------|----------|
| chrome-win | Desktop | Chrome 131 | 1920×1080 |
| chrome-mac | Desktop | Chrome 131 | 1680×1050@2x |
| chrome-linux | Desktop | Chrome 131 | 1920×1080 |
| firefox-win | Desktop | Firefox 133 | 1920×1080 |
| firefox-mac | Desktop | Firefox 133 | 1680×1050@2x |
| safari-mac | Desktop | Safari 18.2 | 1680×1050@2x |
| edge-win | Desktop | Edge 131 | 1920×1080 |
| chrome-android | Mobile | Chrome 131 | 412×915@3x |
| safari-ios | Mobile | Safari 18.2 | 393×852@3x |
| samsung-android | Mobile | Samsung 24 | 384×854@3x |

Each profile includes: exact User-Agent, Accept headers, Sec-Ch-Ua hints, Sec-Fetch-* headers, viewport, device pixel ratio, platform, timezone, language, fonts, WebGL fingerprint.

## Campaign Config Model

```
Campaign
├── siteId              # Target website
├── simulationLevel      # HTTP_ONLY | BROWSER_NAVIGATION | FULL_BROWSER
├── trafficPattern       # CONSTANT | RAMP_UP | PULSE | REALISTIC_WAVE
├── visitsPerHour        # Traffic rate
├── durationMinutes      # Auto-stop after this duration
├── geoDistribution[]    # [{countryCode: "US", weight: 60}, {countryCode: "GB", weight: 40}]
├── deviceProfile[]      # [{device: "desktop", os: "Windows 10", browser: "Chrome", weight: 70}]
├── userAgentConfig      # {rotation: "RANDOM" | "WEIGHTED" | "SESSION_STICKY"}
├── proxyConfig          # {provider: "ASOCKS"}
└── scenarios[]          # Reusable behavior patterns (Levels 2 & 3)
    └── Scenario
        ├── name, description
        └── steps[]
            ├── actionType: LOAD | CLICK | SCROLL | WAIT | HOVER | TYPE | EXTRACT_TEXT | SCREENSHOT | CUSTOM_JS
            ├── selector       # CSS/XPath
            ├── value          # Text to type
            ├── delayBeforeMs, delayAfterMs
            └── probability    # 0.0–1.0 chance of executing this step
```

## API Endpoints

### Auth
| Method | Path | Description |
|--------|------|-------------|
| POST | `/api/auth/register` | Register new user |
| POST | `/api/auth/login` | Login, returns JWT |

### Sites
| Method | Path | Description |
|--------|------|-------------|
| GET, POST | `/api/sites` | List / Create |
| GET, PUT, DELETE | `/api/sites/{id}` | Read / Update / Delete |

### Scenarios
| Method | Path | Description |
|--------|------|-------------|
| GET, POST | `/api/scenarios` | List / Create |
| GET, PUT, DELETE | `/api/scenarios/{id}` | Read / Update / Delete |

### Campaigns
| Method | Path | Description |
|--------|------|-------------|
| GET, POST | `/api/campaigns` | List / Create |
| GET, PUT, DELETE | `/api/campaigns/{id}` | Read / Update / Delete |
| POST | `/api/campaigns/{id}/start` | Start simulation |
| POST | `/api/campaigns/{id}/stop` | Stop simulation |
| POST | `/api/campaigns/{id}/pause` | Pause simulation |
| POST | `/api/campaigns/{id}/resume` | Resume simulation |
| GET | `/api/campaigns/{id}/runs` | Run history |
| GET | `/api/campaigns/{id}/stats` | Live statistics |

### WebSocket
| Topic | Description |
|-------|-------------|
| `/topic/visits/{runId}` | Real-time visit events (path, status, response time, proxy) |
| `/topic/stats/{runId}` | Periodic statistics snapshots |
| `/topic/status/{runId}` | Campaign status changes (RUNNING, PAUSED, COMPLETED) |

## Running Locally

### Prerequisites
- Java 21+
- Node.js 22+
- Docker (for PostgreSQL)

### Setup

```bash
# 1. Start PostgreSQL
docker compose up -d

# 2. Set asocks API key (get yours at https://asocks.com)
export ASOCKS_API_KEY=your_key_here

# 3. Start backend (port 8080)
cd backend
./gradlew bootRun

# 4. Start frontend (port 3000, separate terminal)
cd frontend
npm install
npm run dev
```

Open `http://localhost:3000` — register an account and start creating campaigns.

### Running Tests

```bash
# Backend tests (50 tests, uses H2 in-memory DB)
cd backend && ./gradlew test

# E2E tests (51 Playwright tests, requires backend + frontend running)
cd e2e && npm install && npx playwright install chromium && npx playwright test
```

### Production Build

```bash
cd backend
./gradlew bootJar
docker build -t clicker .

docker run \
  -e JWT_SECRET=your-256-bit-secret \
  -e ASOCKS_API_KEY=your_key \
  -e SPRING_DATASOURCE_URL=jdbc:postgresql://your-db:5432/clicker \
  -e SPRING_DATASOURCE_USERNAME=clicker \
  -e SPRING_DATASOURCE_PASSWORD=clicker \
  -p 8080:8080 \
  clicker
```

## Environment Variables

| Variable | Required | Default | Description |
|----------|----------|---------|-------------|
| `ASOCKS_API_KEY` | Yes | — | asocks.com API key for proxy acquisition |
| `JWT_SECRET` | Yes (prod) | `local-dev-secret...` | JWT signing key (min 256 bits for HS256) |
| `SPRING_DATASOURCE_URL` | No | `jdbc:postgresql://localhost:5433/clicker` | PostgreSQL JDBC URL |
| `SPRING_DATASOURCE_USERNAME` | No | `clicker` | DB username |
| `SPRING_DATASOURCE_PASSWORD` | No | `clicker` | DB password |

## Tech Stack

| Layer | Technology |
|-------|-----------|
| Runtime | Java 21, Node.js 22 |
| Framework | Spring Boot 3.4, React 19 |
| Database | PostgreSQL 16, Flyway, H2 (tests) |
| HTTP Client | OkHttp 4.12 |
| Browser Automation | Playwright 1.49 (Java bindings) |
| Auth | BCrypt + JWT (jjwt) |
| Real-time | STOMP over WebSocket (SockJS) |
| Frontend | React, Vite, TypeScript, Tailwind CSS, React Router |
| E2E Testing | Playwright (TypeScript) |
| Containerization | Docker, docker-compose |

## Test Coverage

- **Backend**: 50 tests — auth, CRUD, simulation engine (start/stop/pause/resume/auto-stop), proxy rotation, security
- **E2E**: 51 tests covering every UI flow — register, login, logout, validation errors, sites CRUD, scenarios (all 9 action types, move/delete steps, delays, probability), campaigns (all 3 levels, 4 traffic patterns, geo distribution, detail page, delete), dashboard, empty states
