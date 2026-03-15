# E-Government Tendering System

A comprehensive microservices-based platform for managing government procurement and tendering processes.
# Note: This project is under development there is a lot of work to be done.
## Overview

The E-Government Tendering System is designed to streamline government procurement processes through a modern, secure, and transparent web-based solution. It allows government agencies to publish tenders, vendors to submit bids, and officials to evaluate and award contracts in a consistent and auditable manner.

The platform is built using a microservices architecture to ensure scalability, maintainability, and the ability to evolve individual components independently.

![System Architecture](docs/images/architecture.png)

## Microservices

The system consists of the following microservices:

### Core Services

| Service | Description | Port |
|---------|-------------|------|
| **Discovery Service** | Service registry and discovery using Netflix Eureka | 8761 |
| **Config Service** | Centralized configuration server using Spring Cloud Config | 8888 |
| **Gateway Service** | API gateway using Spring Cloud Gateway | 8080 |
| **Auth Service** | Authentication and authorization service using OAuth2/JWT | 9000 |

### Business Services

| Service | Description | Port |
|---------|-------------|------|
| **User Service** | Manages users, roles, and permissions | 8081 |
| **Tender Service** | Handles tender creation, publication, and management | 8082 |
| **Bidding Service** | Manages the bid submission and tracking process | 8083 |
| **Contract Service** | Handles contract creation and management after bid selection | 8084 |
| **Document Service** | Manages document uploads, storage, and retrieval | 8085 |
| **Notification Service** | Handles system notifications through various channels | 8086 |
| **Evaluation Service** | Supports bid evaluation processes and decision-making | 8087 |
| **Audit Service** | Tracks and logs all critical system actions | 8088 |

## Technical Stack

- **Language**: Java 17
- **Framework**: Spring Boot 3.2.x, Spring Cloud 2023.0.x
- **Build Tool**: Maven
- **Database**: MySQL 8.0
- **Messaging**: Apache Kafka
- **Service Discovery**: Netflix Eureka
- **API Gateway**: Spring Cloud Gateway
- **Authentication**: OAuth 2.0 with JWT
- **Documentation**: OpenAPI 3 (Swagger)
- **Containerization**: Docker, Docker Compose
- **Testing**: JUnit 5, Mockito, Testcontainers
- **CI/CD**: GitHub Actions (or your preferred CI/CD tool)

## System Requirements

- Java 17 or higher
- Maven 3.8.x or higher
- Docker and Docker Compose (for containerized deployment)
- MySQL 8.0
- Kafka 3.x

## Project Structure

```
e-government-tendering-system/
├── common-util/                  # Shared libraries and utilities
├── config-service/              # Centralized configuration service
├── discovery-service/           # Service registry and discovery
├── gateway-service/             # API gateway
├── user-service/                # User management service
├── tender-service/              # Tender management service
├── bidding-service/             # Bid management service
├── contract-service/            # Contract management service
├── document-service/            # Document management service
├── notification-service/        # Notification service
├── audit-service/               # Audit logging service
├── evaluation-service/          # Bid evaluation service
├── docker-compose.yml           # Docker Compose for local deployment
└── pom.xml                      # Parent POM file
```

## Getting Started

### Prerequisites

- Java 17 or higher
- Maven 3.8.x or higher
- Docker and Docker Compose (for containerized deployment)

### Building the Project

```bash
# Clone the repository
git clone https://github.com/your-org/e-government-tendering-system.git
cd e-government-tendering-system

# Build the entire project
mvn clean install
```

### Running Locally with Docker Compose // we will include this part in the feature

```bash
# Start all services
docker-compose up -d

# View logs
docker-compose logs -f [service-name]

# Stop all services
docker-compose down
```

### Running Individual Services

```bash
cd service-name
mvn spring-boot:run
```

Note: For local development, you need to start the Core Services (Discovery, Config, Gateway) before starting the Business Services.

## Development Workflow

1. **Setup**: Clone the repository and build the project
2. **Core Services**: Start the Discovery, Config, and Gateway services
3. **Database**: Ensure MySQL is running and the required databases are created
4. **Kafka**: Start Kafka for event processing
5. **Business Services**: Start the required business services for your development task
6. **Testing**: Use Swagger UI for API testing or write automated tests

## API Documentation

Each service exposes its API documentation via Swagger UI at:
```
http://localhost:<service-port>/swagger-ui.html
```

To view documentation for all services through the Gateway:
```
http://localhost:8080/swagger-ui.html
```

## Configuration

The configuration for all services is centralized in the Config Service. The configuration files are stored in:
```
config-service/src/main/resources/config
```

Each service has its own configuration file named `<service-name>.yml`. Common configuration is defined in `application.yml`.

## Security

The system uses OAuth 2.0 with JWT for authentication and authorization. The Auth Service is responsible for issuing and validating JWT tokens.

User roles:
- **ADMIN**: System administrators with full access
- **TENDEREE**: Government officials who create and manage tenders
- **EVALUATOR**: Officials who evaluate bids
- **COMMITTEE**: Evaluation committee members who review and approve evaluations
- **TENDERER**: Organizations that submit bids

## Paper-Aligned Security Features

The following features are implemented based on the Fong & Yan paper on "Design of a Web-based Tendering System for e-Government Procurement":

### 1. Sealed Bidding (Bid Sealing/Encryption)
Bids are cryptographically sealed upon submission using AES-256-GCM encryption with SHA-256 integrity hashing. Sealed bids cannot be viewed by anyone (including administrators) until the tender's submission deadline passes. The system supports:
- Automatic sealing on bid submission
- Scheduled unsealing after the tender deadline
- Manual bid opening ceremony (unseal all bids for a tender)
- Tamper detection via integrity hash verification
- **API**: `POST /api/v1/bids/{bidId}/seal`, `POST /api/v1/bids/tender/{tenderId}/unseal-all`

### 2. Digital Signatures (Non-Repudiation)
All bids and contracts can be digitally signed using SHA256withRSA signatures. This provides non-repudiation, ensuring that bidders cannot deny having submitted a bid and contract parties cannot deny agreement. The system supports:
- Entity signing (bids, contracts)
- Signature verification and rejection
- Content hash comparison to detect post-signing modifications
- **API**: `POST /api/v1/signatures/{entityType}/{entityId}/sign`, `POST /api/v1/signatures/{signatureId}/verify`

### 3. Anti-Collusion Measures
The system tracks bid submission metadata (IP address, device fingerprint, user agent, timing) and provides automated collusion detection analysis:
- **Same IP detection**: Flags bids from different tenderers submitted from the same IP
- **Same device detection**: Flags bids from different tenderers using the same device
- **Pricing pattern analysis**: Detects suspiciously similar bid prices (within 2% threshold)
- **Timing anomaly detection**: Flags bids submitted within 60 seconds of each other
- **API**: `GET /api/v1/anti-collusion/tender/{tenderId}/analyze`

### 4. Tender Amendment Workflow
Published tenders can be amended with full audit trail. Amendments are versioned, track the previous and new values, and trigger Kafka events to notify all registered bidders:
- Amendment numbering and history
- Deadline extension support
- Mandatory notification to all bidders via Kafka events
- **API**: `POST /api/tenders/{tenderId}/amend`, `GET /api/tenders/{tenderId}/amendments`

### 5. Vendor Pre-Qualification
Vendors must be pre-qualified before participating in tenders. The qualification process validates business licenses, tax registration, financial capability, experience, and certifications:
- Multi-category qualification support
- Qualification scoring (0-100)
- Time-limited qualifications with automatic expiry
- Status workflow: PENDING -> UNDER_REVIEW -> QUALIFIED/DISQUALIFIED
- **API**: `POST /api/vendor-qualifications`, `PUT /api/vendor-qualifications/{id}/review`

### 6. Multi-Criteria Evaluation Scoring
Bid evaluation uses a configurable weighted scoring model with category-level breakdown:
- **Score categories**: Technical, Financial, Compliance, Experience, Quality
- Configurable weights per category (must sum to 100%)
- Mandatory pass thresholds per category
- Category-level pass/fail determination
- Overall qualification assessment
- **API**: `POST /api/multi-criteria/tenders/{tenderId}/categories`, `GET /api/multi-criteria/tenders/{tenderId}/results`

## Event-Driven Architecture

The system uses Kafka for event-driven communication between services. Key events include:
- Tender created/updated/published/amended
- Bid submitted/updated/withdrawn/sealed/unsealed
- Contract awarded/signed
- Document uploaded/verified
- Evaluation completed with multi-criteria breakdown
- Notification events

## Database Schema

Each service manages its own database schema. The database migrations are handled using Flyway and are located in:
```
<service-name>/src/main/resources/db/migration
```

## Monitoring and Observability

The system exposes metrics via Spring Boot Actuator and can be integrated with:
- Prometheus for metrics collection
- Grafana for dashboards
- ELK Stack for log aggregation

## Deployment

### Docker Deployment // we will include this part in the feature

The `docker-compose.yml` file in the root directory can be used for local deployment. For production deployment, consider using Kubernetes.

### Kubernetes Deployment // we will include this part in the feature

Kubernetes manifests are provided in the `k8s/` directory for deploying the system on a Kubernetes cluster.

## Implementation Phases

For a detailed breakdown of all 16 implementation phases aligned with the Fong & Yan paper, see [docs/IMPLEMENTATION_PHASES.md](docs/IMPLEMENTATION_PHASES.md).

## Contributing

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'Add some amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## Tendering Workflow (End-to-End)

```
1. Vendor Registration & Pre-Qualification
   └─ Vendor registers → Submits qualification → Reviewed & approved

2. Tender Creation & Publication
   └─ Tenderee creates DRAFT tender → Configures evaluation categories → Publishes tender
   └─ Tender can be AMENDED after publication (notifies all bidders)

3. Bid Submission (Sealed)
   └─ Tenderer creates DRAFT bid → Submits bid → Bid is SEALED (encrypted)
   └─ Anti-collusion metadata is recorded (IP, device, timing)

4. Bid Opening Ceremony
   └─ After submission deadline → All sealed bids are UNSEALED → Integrity verified

5. Bid Evaluation (Multi-Criteria)
   └─ Evaluators score bids per criteria → Category breakdown computed
   └─ Technical/Financial/Compliance scores calculated → Rankings generated

6. Committee Review & Award
   └─ Committee reviews evaluation → Approves results → Contract awarded
   └─ Digital signatures applied to bids and contracts

7. Contract Management
   └─ Contract created → Milestones tracked → Contract lifecycle managed

8. Full Audit Trail
   └─ Every action across all services is captured in the audit service
```

## Acknowledgments

- Based on research by Simon Fong and Zhuang Yan on "Design of a Web-based Tendering System for e-Government Procurement"
- Implements sealed bidding, digital signatures, anti-collusion, vendor pre-qualification, tender amendments, and multi-criteria evaluation as recommended by the paper
- Inspired by best practices in e-Government systems worldwide

## Contact

For questions or support, please contact me through my GitHub: [https://github.com/GezahegnTsegaye/e-government-tendering-system](https://github.com/GezahegnTsegaye/e-government-tendering-system)