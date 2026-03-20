# E-Government Tendering System

A comprehensive microservices-based platform for managing government procurement and tendering processes, built on research by Simon Fong and Zhuang Yan.

## Overview

The E-Government Tendering System digitizes the full government procurement lifecycle — from vendor registration and tender publication through sealed bid submission, multi-criteria evaluation, contract award, and post-award performance monitoring. The platform achieves transparency, accountability, and efficiency through event-driven architecture, cryptographic bid sealing, digital signatures, and automated anti-collusion detection.

The system targets the **56% efficiency improvement** and **42% cycle time reduction** demonstrated in the Fong & Yan paper by automating manual backend processes and providing decision support for optimal bidder allocation.

## System Architecture

```
                                    ┌─────────────────────┐
                                    │   React Frontend     │
                                    │   (Port 3000)        │
                                    └──────────┬──────────┘
                                               │
                                    ┌──────────▼──────────┐
                                    │   API Gateway        │
                                    │   Spring Cloud GW    │
                                    │   (Port 8080)        │
                                    │   + Rate Limiting     │
                                    │   + JWT Validation    │
                                    │   + Circuit Breaker   │
                                    └──────────┬──────────┘
                                               │
                 ┌─────────────────────────────┼─────────────────────────────┐
                 │                             │                             │
    ┌────────────▼───────────┐   ┌────────────▼───────────┐   ┌────────────▼───────────┐
    │  Discovery Service     │   │  Config Service         │   │  Redis                  │
    │  Netflix Eureka        │   │  Spring Cloud Config    │   │  Rate Limiting Cache    │
    │  (Port 8761)           │   │  (Port 8888)            │   │  (Port 6379)            │
    └────────────────────────┘   └─────────────────────────┘   └─────────────────────────┘

    ┌──────────────────────────────────────────────────────────────────────────────────┐
    │                           BUSINESS SERVICES                                       │
    │                                                                                   │
    │  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐          │
    │  │ User Service  │  │Tender Service│  │Bidding Svc   │  │ Evaluation   │          │
    │  │ (8081)        │  │ (8082)       │  │ (8083)       │  │ Svc (8087)   │          │
    │  │              │  │              │  │              │  │              │          │
    │  │ - Auth/JWT   │  │ - CRUD       │  │ - Sealed Bids│  │ - Scoring    │          │
    │  │ - Roles      │  │ - Amendments │  │ - Digital Sig│  │ - Ranking    │          │
    │  │ - Vendor     │  │ - Pre-bid Q&A│  │ - Anti-collu │  │ - Multi-crit │          │
    │  │   Qualific.  │  │ - Categories │  │ - Envelopes  │  │ - CoI Decl.  │          │
    │  │ - Org Blackl.│  │              │  │              │  │ - Committee  │          │
    │  └──────┬───────┘  └──────┬───────┘  └──────┬───────┘  └──────┬───────┘          │
    │         │                 │                 │                 │                    │
    │  ┌──────▼───────┐  ┌──────▼───────┐  ┌──────▼───────┐  ┌──────▼───────┐          │
    │  │Contract Svc  │  │Document Svc  │  │Notification  │  │ Audit Service│          │
    │  │ (8084)       │  │ (8085)       │  │Svc (8086)    │  │ (8088)       │          │
    │  │              │  │              │  │              │  │              │          │
    │  │ - Milestones │  │ - Upload     │  │ - Email      │  │ - Event Log  │          │
    │  │ - Amendments │  │ - Access Ctrl│  │ - SMS/Push   │  │ - Reports    │          │
    │  │ - Vendor Perf│  │ - Metadata   │  │ - Templates  │  │ - Statistics │          │
    │  └──────────────┘  └──────────────┘  └──────────────┘  └──────────────┘          │
    └──────────────────────────────────────────────────────────────────────────────────┘

    ┌──────────────────────────────────────────────────────────────────────────────────┐
    │                           INFRASTRUCTURE                                          │
    │  ┌──────────────────┐  ┌──────────────────┐  ┌──────────────────┐                │
    │  │   MySQL 8.0       │  │  Apache Kafka     │  │  Zookeeper       │                │
    │  │   (Port 3306)     │  │  (Port 9092)      │  │  (Port 2181)     │                │
    │  │   8 databases     │  │  Event streaming  │  │                  │                │
    │  └──────────────────┘  └──────────────────┘  └──────────────────┘                │
    └──────────────────────────────────────────────────────────────────────────────────┘
```

## Quick Start

### What You Need

- Java 21+
- Maven 3.8+
- Node.js 22.12+ recommended for the frontend toolchain
- MySQL running locally or via Docker
- Kafka and Redis optional for partial local development, but recommended for the full event-driven flow

### Supported Local Modes

There are two realistic ways to run the project locally:

1. Full local stack
   MySQL, Kafka, Redis, backend services, and frontend are all available.

2. Degraded local stack
   MySQL is available, but Kafka and/or Redis are missing.
   In this mode the core HTTP application can still start, but event-driven features, Kafka consumers, and some health checks are reduced or unavailable.

### Environment Setup

Copy `.env.example` to `.env` and fill in real values:

```bash
cp .env.example .env
```

Minimum required variables:

| Variable | Required | Purpose | Notes |
|----------|----------|---------|-------|
| `DB_PASSWORD` | Yes | MySQL password for backend services | Used by startup script and Spring datasource config |
| `JWT_SECRET` | Yes | Shared HS512 signing/verification secret | Must be the same across `user-service`, gateway, and resource services |
| `SEALING_MASTER_KEY` | Yes | Base64 AES master key for bid sealing | Must be a base64-encoded 32-byte key |
| `SMTP_HOST` | No | Outbound SMTP host | Used by notification-service |
| `SMTP_PORT` | No | Outbound SMTP port | Defaults to `587` in service config |
| `SMTP_USERNAME` | No | SMTP username | Leave blank if not testing email |
| `SMTP_PASSWORD` | No | SMTP password | Leave blank if not testing email |
| `MYSQL_ROOT_PASSWORD` | Docker only | MySQL container password | Usually set to `${DB_PASSWORD}` |

Current `.env.example` conventions:

```bash
# Database
DB_PASSWORD=change-me-before-running

# Shared JWT secret for HS512
JWT_SECRET=change-me-to-a-64-plus-character-random-string-before-running-ok

# Base64-encoded 32-byte AES key
SEALING_MASTER_KEY=change-me-base64-32-bytes=
```

### Startup Commands

```bash
./scripts/start.sh              # Full stack: infra check + DB setup + services + frontend
./scripts/start.sh infra        # Infrastructure only
./scripts/start.sh services     # Spring Boot services only
./scripts/start.sh frontend     # React dev server only
./scripts/start.sh build        # Build everything, then start
./scripts/start.sh status       # Status table for services and frontend
./scripts/start.sh logs         # Follow current log files
./scripts/start.sh stop         # Stop services and docker infra
```

### Recommended Local Flows

Full stack with Docker:

```bash
./scripts/start.sh
```

Backend only when infra is already running:

```bash
./scripts/start.sh services
```

Frontend only:

```bash
./scripts/start.sh frontend
```

Rebuild after backend code changes:

```bash
./scripts/start.sh build
```

### How `start.sh` Actually Behaves

- It loads `.env` automatically if the file exists.
- It checks Java, Maven, MySQL client, and Node availability.
- It creates the service databases if MySQL is reachable.
- It starts services in dependency order:
  - discovery-service
  - config-service
  - gateway-service + user-service
  - business services
- It can build missing service JARs automatically.
- It writes runtime logs to `logs/`.
- It writes PID files to `.pids/`.

### Kafka / Redis Degraded Mode

If Docker is not available, or Kafka is not reachable on `localhost:9092`, `start.sh` assumes you are in a degraded local environment.

For newly started JVMs, the script now:

- checks Kafka reachability before launching services
- disables Kafka listener startup where supported
- disables Kafka health checks where supported

Important: these flags only apply to newly started processes.

If services are already running and Kafka availability changes, restart them:

```bash
./scripts/start.sh stop
./scripts/start.sh services
```

If you skip the restart, old JVMs will continue trying to connect to Kafka and you will keep seeing log noise like:

- `Connection to node -1 (localhost:9092) could not be established`
- `Topic user-events not present in metadata`

### Authentication Contract

The application currently uses shared-secret JWTs:

- `user-service` issues HS512 tokens
- gateway validates the same HS512 tokens
- resource services validate the same HS512 tokens

This means one thing must stay consistent everywhere:

- `JWT_SECRET` must be identical across all running services

If one service starts with a different secret, login may succeed but the next protected API call will fail with `401`, which the frontend may interpret as “log in again”.

### Access Points

| Component | URL |
|-----------|-----|
| Frontend | http://localhost:3000 |
| Gateway API | http://localhost:8080 |
| Eureka Dashboard | http://localhost:8761 |
| Kafka UI | http://localhost:8090 |

### Service Ports

These are the effective ports used by the startup script:

| Service | Port |
|---------|------|
| `discovery-service` | `8761` |
| `config-service` | `8888` |
| `gateway-service` | `8080` |
| `user-service` | `8081` |
| `tender-service` | `8082` |
| `bidding-service` | `8083` |
| `contract-service` | `8084` |
| `document-service` | `8085` |
| `notification-service` | `8086` |
| `evaluation-service` | `8087` |
| `audit-service` | `8088` via script startup |
| `frontend` | `3000` |

### Demo Users

Default seeded users from Flyway:

| Username | Password | Role |
|----------|----------|------|
| `admin` | `admin123` | `ADMIN` |
| `tenderee` | `tenderee123` | `TENDEREE` |
| `tenderer` | `tenderer123` | `TENDERER` |
| `evaluator` | `evaluator123` | `EVALUATOR` |
| `committee` | `committee123` | `COMMITTEE` |

If you log in with `admin`, the frontend now has explicit `ADMIN` role support. Earlier local builds could fall into the wrong dashboard path and appear to “loop” back to login.

## User Roles

| Role | Capabilities |
|------|-------------|
| **ADMIN** | Cross-service administration, audit access, reporting, privileged operational actions |
| **TENDEREE** | Create/publish/amend tenders, manage evaluations, award contracts, view reports |
| **TENDERER** | Browse tenders, submit pre-qualification, submit sealed bids, track contracts |
| **EVALUATOR** | Score bids against criteria, declare conflicts of interest |
| **COMMITTEE** | Review evaluations, approve/reject committee decisions |

## Paper-Aligned Features

Implementing the key requirements from Fong & Yan's "Design of a Web-based Tendering System for e-Government Procurement" (ICEGOV 2009):

| # | Feature | Implementation |
|---|---------|---------------|
| 1 | **Sealed Bidding** | AES-256-GCM encryption + SHA-256 hashing; bids unsealed only after deadline |
| 2 | **Digital Signatures** | SHA256withRSA for non-repudiation of bids and contracts |
| 3 | **Anti-Collusion** | IP/device/pricing/timing analysis with automated flagging |
| 4 | **Vendor Pre-Qualification** | Scored qualification with time-limited validity and auto-expiry |
| 5 | **Organization Blacklist** | Debarment/suspension management with automatic expiry |
| 6 | **Multi-Criteria Evaluation** | Technical/Financial/Compliance/Quality weighted scoring |
| 7 | **Conflict of Interest** | Mandatory evaluator declarations with review workflow |
| 8 | **Tender Amendments** | Versioned amendments with Kafka notification to all bidders |
| 9 | **Two-Envelope Bidding** | Separate technical and financial envelopes with independent sealing |
| 10 | **Pre-Bid Clarifications** | Public Q&A between vendors and tenderee for transparency |
| 11 | **Contract Amendments** | Formal amendment requests with approval workflow |
| 12 | **Vendor Performance** | Post-award scorecards (quality, timeliness, compliance, communication) |
| 13 | **Procurement Reporting** | Audit-based summaries with date-range filtering |

## Event-Driven Architecture

| Kafka Topic | Producer | Consumers |
|-------------|----------|-----------|
| `tender-events` | Tender Service | Bidding, Audit, Notification |
| `bid-events` | Bidding Service | Evaluation, Contract, Audit, Notification |
| `evaluation-events` | Evaluation Service | Bidding, Contract, Audit |
| `contract-events` | Contract Service | Bidding, Audit, Notification |
| `user-events` | User Service | Notification, Audit |
| `notification-events` | Notification Service | Audit |

## Tendering Workflow

```
1. Vendor Registration & Pre-Qualification
   └─ Register → Submit qualification → Review & approve
   └─ Blacklisted organizations prevented from bidding

2. Tender Creation & Publication
   └─ Create DRAFT → Configure criteria & categories → Publish
   └─ Amend after publication (notifies all bidders)
   └─ Pre-bid Q&A: Vendors ask, tenderee answers publicly

3. Bid Submission (Sealed, Two-Envelope)
   └─ Create DRAFT → Add items per criteria → Upload documents
   └─ Submit → Bid SEALED (AES-256-GCM) → Envelopes separated
   └─ Anti-collusion metadata recorded (IP, device, timing)

4. Bid Opening Ceremony
   └─ After deadline → All bids UNSEALED → Integrity verified
   └─ Tamper detection flags modified bids

5. Bid Evaluation (Multi-Criteria)
   └─ Evaluators declare conflicts of interest
   └─ Score per criteria → Weighted category scores → Rankings
   └─ Digital signatures applied for non-repudiation

6. Committee Review & Award
   └─ Committee reviews → Approves/rejects
   └─ Contract awarded based on allocation strategy

7. Contract Management & Performance
   └─ Contract created → Milestones tracked → Amendments managed
   └─ Vendor performance scored per review period

8. Full Audit Trail
   └─ Every action captured → Procurement reports generated
```

## Database

8 databases auto-created on startup via Flyway migrations:

| Database | Key Tables |
|----------|-----------|
| `user_service` | users, organizations, vendor_qualifications, organization_blacklist |
| `tender_service` | tenders, tender_criteria, tender_items, tender_amendments, pre_bid_clarifications |
| `bidding_service` | bids, bid_seals, digital_signatures, bid_submission_metadata, bid_envelopes |
| `evaluation_service` | evaluations, criteria_scores, evaluation_category_configs, conflict_of_interest_declarations |
| `contract_service` | contracts, contract_milestones, contract_amendments, vendor_performances |
| `document_service` | documents |
| `notification_service` | notifications, notification_audit |
| `audit_service` | audit_logs |

## Testing

Default test path excludes Docker-dependent integration tests:

```bash
mvn test                # Unit/default test path
mvn test -Pintegration  # Integration tests, requires Docker/Testcontainers
```

Notes:

- `mvn test` is intended to pass in a standard developer environment without Docker.
- The integration profile runs `*IntegrationTest.java` classes explicitly.
- Frontend production builds require a modern Node runtime compatible with Vite in this repo.

### Frontend Tooling Notes

The frontend build currently expects a modern Node runtime. If you see errors like:

- `Vite requires Node.js version 20.19+ or 22.12+`
- `node:util does not provide an export named styleText`

your shell is using an older Node version than the one shown elsewhere on your machine. Fix the active shell runtime first, then rerun frontend commands.

### Troubleshooting

#### Login redirects back to login repeatedly

Common causes:

1. `JWT_SECRET` does not match across running services.
2. You logged in as a role the frontend did not support in the current build.
3. The first protected API call returned `401`, and the frontend interceptor forced a logout.

Checks:

```bash
./scripts/start.sh status
tail -f logs/gateway-service.log logs/user-service.log
```

Look for:

- successful `POST /api/auth/login`
- immediate `401` on the next protected request
- JWT validation/signature errors in gateway or resource-service logs

#### Kafka connection warnings spam the logs

This means Kafka is not reachable but one or more services were started without degraded-mode flags.

Fix:

```bash
./scripts/start.sh stop
./scripts/start.sh services
```

Or start Kafka properly and then restart the stack.

#### Notification or audit features do not react to events

That is expected if Kafka is down. The HTTP APIs can still work partially, but event-driven flows such as notifications, audit ingestion, and asynchronous domain reactions will not behave normally.

#### Frontend build fails even though `node --version` looked fine earlier

Your terminal session may still be using another Node binary. Recheck in the same shell:

```bash
node --version
which node
```

#### `start.sh services` says a service is already running

That process keeps its original JVM flags. If you changed `.env`, Kafka availability, or code, restart the stack:

```bash
./scripts/start.sh stop
./scripts/start.sh services
```

## Technical Stack

| Layer | Technology |
|-------|-----------|
| **Backend** | Java 17, Spring Boot 3.2.2, Spring Cloud 2023.0.0 |
| **Frontend** | React 19, TypeScript, Vite, Ant Design, Zustand |
| **Database** | MySQL 8.0 (per-service), Flyway migrations |
| **Messaging** | Apache Kafka (event-driven communication) |
| **Caching** | Redis (gateway rate limiting) |
| **Security** | JWT (HS512), AES-256-GCM, SHA256withRSA |
| **Discovery** | Netflix Eureka |
| **Gateway** | Spring Cloud Gateway + Resilience4j |
| **CI/CD** | GitHub Actions |
| **Containers** | Docker, Docker Compose |
| **Monitoring** | Spring Boot Actuator, Prometheus |

## Implementation Phases

See [docs/IMPLEMENTATION_PHASES.md](docs/IMPLEMENTATION_PHASES.md) for the full 16-phase roadmap.

## Acknowledgments

- Based on: Fong, S. & Yan, Z. (2009). "Design of a Web-based Tendering System for e-Government Procurement." ICEGOV 2009.
- Inspired by OECD Digital Government and World Bank procurement frameworks.

## Contact

Gezahegn Tsegaye — [GitHub](https://github.com/GezahegnTsegaye/e-government-tendering-system)

For support and feedback, please contact Gezahegn Tsegaye.
