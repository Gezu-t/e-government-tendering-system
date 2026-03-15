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

### Prerequisites
- Java 17+, Maven 3.8+, Docker & Docker Compose, Node.js 18+

### One-Command Startup

```bash
./scripts/start.sh              # Full stack (infra + build + services + frontend)
./scripts/start.sh infra        # MySQL, Kafka, Redis via Docker only
./scripts/start.sh services     # Spring Boot services only
./scripts/start.sh frontend     # React dev server only
./scripts/start.sh status       # Show status of all components
./scripts/start.sh stop         # Stop everything
```

### Access Points

| Component | URL |
|-----------|-----|
| Frontend | http://localhost:3000 |
| Gateway API | http://localhost:8080 |
| Eureka Dashboard | http://localhost:8761 |
| Kafka UI | http://localhost:8090 |

## User Roles

| Role | Capabilities |
|------|-------------|
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

8 test files across 4 core services (~80 tests):

```bash
mvn test    # Run all tests
```

| Service | Tests |
|---------|-------|
| user-service | UserServiceImpl, VendorQualificationServiceImpl |
| tender-service | TenderServiceImpl, TenderController |
| bidding-service | BidSealingServiceImpl, AntiCollusionServiceImpl |
| evaluation-service | EvaluationServiceImpl, MultiCriteriaEvaluationServiceImpl |

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
