# Remediation Issues

Issue-ready backlog derived from the repository evaluation on 2026-03-17.
Revised 2026-03-17: applied "no hardcoded / magic values — everything dynamic or DB-driven" architectural principle across all issues.

## Architectural Principle

No magic strings, hardcoded credentials, or static constants that bypass the configuration system.
Every tuneable value must be externalized through one of:

- `@ConfigurationProperties` bound from `application.yml` / config-server
- `@Value("${property.path:safe-default}")` for single values
- Environment variables for secrets (`${ENV_VAR}` with no plaintext default)

## Milestones

### Milestone 1: Auth And Gateway Coherence
- Login tokens are accepted by the gateway and downstream services.
- Role-based authorization works consistently across protected endpoints.
- Active gateway runtime config matches frontend and deployment expectations.

### Milestone 2: Secure Bid Sealing
- Bid sealing no longer stores raw content-encryption keys beside ciphertext.
- Unseal decrypts the sealed payload rather than re-reading mutable database rows.
- Tamper and deadline behavior are covered by tests.

### Milestone 3: Green Developer Quality Gates
- `mvn test` passes in a standard non-Docker developer environment.
- Docker-dependent integration tests run behind an explicit profile.
- `frontend` build and lint both pass.

### Milestone 4: Production Hardening
- Notification configuration is environment-driven.
- Embedded placeholder passwords and secrets are removed from committed runtime config.
- No `public static final` magic strings exist in `@ConfigurationProperties` beans.

## Issue 1: Decide And Document The System JWT Model ✅ DONE

**Priority:** P0
**Estimate:** 1 day
**Depends on:** None
**Blocks:** 2, 3, 4

### Problem
`user-service` currently issues HS512 JWTs, while the gateway and multiple resource services are configured as OIDC resource servers against different issuers. The system has no single, consistent token contract.

### Scope
- Choose one token model for the platform:
  - first-party JWTs issued by `user-service`, or
  - external OIDC/OAuth2 issuer
- Define the canonical claims with their **config-property keys**:
  - `app.security.jwt.claim-roles` → claim carrying authorities (e.g. `roles`)
  - `app.security.jwt.claim-user-id` → claim carrying the numeric user ID (e.g. `userId`)
  - `app.security.jwt.claim-permissions` → claim carrying permissions (e.g. `permissions`)
  - `app.security.jwt.issuer` → token issuer URI
  - `app.security.jwt.expiration` → token lifetime (ms)
- Record the decision in project docs.

### Acceptance Criteria
- A short architecture note exists in `docs/`.
- The note names the issuer, signing strategy, claim schema, and corresponding config-property keys.
- All services read claim names from config — no hardcoded string literals for claim names.

### File Targets
- `user-service/src/main/java/com/egov/tendering/user/security/JwtTokenProvider.java`
- `gateway-service/src/main/resources/application.yml`
- `bidding-service/src/main/resources/application.yaml`
- `evaluation-service/src/main/resources/application.yml`
- `contract-service/src/main/resources/application.yml`

## Issue 2: Implement A Single JWT Validation Path Across Gateway And Services ✅ DONE

**Priority:** P0
**Estimate:** 2-3 days
**Depends on:** 1
**Blocks:** 3

### Problem
The current services do not share a working token-validation strategy. Even successful login does not imply a usable token for protected endpoints.

### Scope
- Remove the incompatible auth path that is not selected in Issue 1.
- Align gateway and all resource services to the same issuer/decoder setup.
- Standardize shared auth config via config-server — avoid per-service drift.

### Acceptance Criteria
- A token obtained from `/api/auth/login` or the chosen external issuer is accepted by the gateway.
- At least one protected endpoint in each of these domains is accessible with a valid token:
  - user, tender, bidding, evaluation, contract
- Invalid or expired tokens are rejected consistently.

### File Targets
- `gateway-service/src/main/resources/application.yml`
- `gateway-service/src/main/java/com/egov/tendering/gateway/config/SecurityConfig.java`
- service `SecurityConfig` classes under backend modules

## Issue 3: Normalize JWT Claims And Externalize Claim-Name Config ✅ DONE

**Priority:** P0
**Estimate:** 1 day
**Depends on:** 1
**Blocks:** None

### Problem
Authorities are encoded as a comma-separated string, while downstream converters are not configured to parse that representation safely. Additionally, the claim names `"roles"`, `"userId"`, and `"permissions"` are hardcoded as Java string literals in every service independently, creating invisible coupling and guaranteed drift.

### Scope
- Change `roles` to a JSON array, or explicitly configure a shared delimiter.
- **Remove all hardcoded claim-name string literals** from `SecurityConfig`, `JwtTokenProvider`, and `JwtUserIdExtractor`.
- Each class reads its claim names from `app.security.jwt.claim-roles`, `app.security.jwt.claim-user-id`, `app.security.jwt.claim-permissions` via `@Value` with safe defaults.
- Config-server provides the single authoritative value shared across all services.

### Acceptance Criteria
- `hasRole` and `hasAuthority` checks succeed for valid tokens.
- Role parsing is covered by unit or integration tests.
- Grep for `"roles"` and `"userId"` finds zero string literals in `SecurityConfig`, `JwtTokenProvider`, or `JwtUserIdExtractor` — only property keys.
- The same token shape works across at least two resource services.

### File Targets
- `user-service/src/main/java/com/egov/tendering/user/security/JwtTokenProvider.java`
- `bidding-service/src/main/java/com/egov/tendering/bidding/config/SecurityConfig.java`
- `bidding-service/src/main/java/com/egov/tendering/bidding/config/JwtUserIdExtractor.java`
- `tender-service/src/main/java/com/egov/tendering/tender/config/SecurityConfig.java`
- `evaluation-service/src/main/java/com/egov/tendering/evaluation/config/SecurityConfig.java`
- `contract-service/src/main/java/com/egov/tendering/contract/config/SecurityConfig.java`
- `document-service/src/main/java/com/egov/tendering/document/config/SecurityConfig.java`
- `notification-service/src/main/java/com/egov/tendering/notification/config/SecurityConfig.java`

## Issue 4: Reconcile Gateway Runtime Config With Frontend And Deployment ✅ DONE

**Priority:** P1
**Estimate:** 1 day
**Depends on:** 1
**Blocks:** None

### Problem
The local gateway config, Docker config, frontend base URL, and config-repo gateway config disagree on active port and route definitions. Public endpoint paths are hardcoded as Java string literals in `SecurityConfig`.

### Scope
- Resolve the `8080` vs `9090` conflict.
- Remove or replace stale routes in `config-repo/config-client-gateway.yml`.
- Ensure the frontend default API base URL matches the active gateway.
- **Externalize the list of publicly accessible paths** from `SecurityConfig` into `application.yml` under `app.gateway.public-paths` and `app.gateway.public-get-paths`. Adding a new public endpoint requires a config change only, not a code change.

### Acceptance Criteria
- One active gateway port is documented and used consistently.
- Gateway routes in runtime config correspond to real services in this repository.
- Frontend requests hit the correct gateway URL without manual mismatch fixes.
- `SecurityConfig` reads public paths from `GatewayProperties` — no path strings in Java source.

### File Targets
- `gateway-service/src/main/resources/application.yml`
- `gateway-service/src/main/java/com/egov/tendering/gateway/config/SecurityConfig.java`
- `gateway-service/src/main/java/com/egov/tendering/gateway/config/GatewayProperties.java` (new)
- `gateway-service/src/main/java/com/egov/tendering/gateway/config/RouteConfig.java`
- `config-repo/config-client-gateway.yml`
- `frontend/src/api/client.ts`

## Issue 5: Redesign Bid Sealing To Use Defensible Key Management ✅ DONE

**Priority:** P0
**Estimate:** 3-5 days
**Depends on:** None
**Blocks:** 6

### Problem
The current sealing flow stores the raw AES content key in the same persistence record as the ciphertext (completely defeating the encryption) and does not actually decrypt sealed payloads during unseal. All cryptographic parameters (`AES/GCM/NoPadding`, `SHA-256`, tag length, IV length) are `private static final` constants with no override path.

### Scope
- Introduce proper key handling:
  - wrapped content key using a master key, or
  - external KMS/secret-store abstraction
- Separate ciphertext from key-encryption material.
- **Replace all crypto `static final` constants** with a `BidSealingProperties` `@ConfigurationProperties` bean bound from `bidding.seal.*`, making algorithm and parameters configurable without redeployment.
- Keep enough metadata for auditable unseal.

### Acceptance Criteria
- Raw content-encryption keys are not stored directly as application data.
- Sealed payloads remain decryptable by authorized application logic only.
- The storage model supports integrity and audit requirements.
- No `private static final` crypto constants remain in `BidSealingServiceImpl`; all read from `BidSealingProperties`.

### File Targets
- `bidding-service/src/main/java/com/egov/tendering/bidding/service/impl/BidSealingServiceImpl.java`
- `bidding-service/src/main/java/com/egov/tendering/bidding/config/BidSealingProperties.java` (new)
- `bidding-service/src/main/resources/application.yaml`
- `bidding-service` persistence model for bid seals

## Issue 6: Make Unseal Operate On The Sealed Artifact And Add Tamper Tests ✅ DONE

**Priority:** P0
**Estimate:** 2 days
**Depends on:** 5
**Blocks:** None

### Problem
Unseal currently verifies integrity by hashing the current mutable `Bid` entity instead of decrypting the sealed payload. This has two defects:
1. The encrypted payload is never read — it is orphaned.
2. Any change to a non-business field on `Bid` (e.g. `updatedAt`, ORM version) after sealing will produce a false `TAMPER_DETECTED` on a legitimate bid.

The `@Scheduled` cron for auto-unseal is hardcoded as `"0 */15 * * * *"` with no override path.

### Scope
- Implement decrypt-on-unseal using `encryptedContent`.
- Compare decrypted payload integrity against stored integrity metadata.
- Fix the false-positive risk: hash the business-data snapshot inside `encryptedContent`, not the live entity.
- **Externalize the scheduled cron** to `${bidding.seal.unseal-check-cron:0 */15 * * * *}`.
- Add deadline, tamper, and scheduled-unseal test coverage.

### Acceptance Criteria
- Unseal returns or reconstructs data from `encryptedContent`.
- Mutating the live bid row after sealing does not produce a false "valid" unseal.
- Mutating `updatedAt` or `version` on `Bid` does not produce a false `TAMPER_DETECTED`.
- Attempting unseal before deadline fails.
- Scheduled unseal behavior is covered by tests.
- The cron expression is read from config, not a string literal.

### File Targets
- `bidding-service/src/main/java/com/egov/tendering/bidding/service/impl/BidSealingServiceImpl.java`
- `bidding-service/src/main/resources/application.yaml`
- `bidding-service/src/test/java/...`

## Issue 7: Split Unit Tests From Docker-Dependent Integration Tests ✅ DONE

**Priority:** P1
**Estimate:** 1 day
**Depends on:** None
**Blocks:** None

### Problem
`mvn test` currently fails in environments without Docker because Testcontainers integration tests are part of the default test path.

### Scope
- Move Testcontainers tests behind an explicit Maven profile or tag.
- Keep unit tests in the default test target.
- Document how to run integration tests locally and in CI.

### Acceptance Criteria
- `mvn test` passes without Docker.
- Integration tests still run through an explicit command such as `mvn test -Pintegration`.
- CI can choose unit-only or full integration execution intentionally.

### File Targets
- root `pom.xml`
- service `pom.xml` files as needed
- `*IntegrationTest.java` classes

## Issue 8: Add End-To-End Auth Smoke Tests Through The Gateway ✅ DONE

**Priority:** P1
**Estimate:** 1-2 days
**Depends on:** 2, 3, 4
**Blocks:** None

### Problem
There is no reliable automated check that login, gateway auth, and role enforcement work together.

### Scope
- Add one or more end-to-end auth smoke tests.
- Cover:
  - login success
  - protected route success with valid token
  - protected route rejection with invalid token
  - one role-based access rule

### Acceptance Criteria
- Test coverage exists for the complete auth path, not only local token creation.
- Failures clearly indicate whether the issue is token issuance, gateway validation, or endpoint authorization.

## Issue 9: Fix Frontend React Hook And Lint Violations ✅ DONE

**Priority:** P2
**Estimate:** 0.5-1 day
**Depends on:** None
**Blocks:** None

### Problem
The production build passes, but `npm run lint` fails due to state updates triggered directly inside effects and missing hook dependencies.

### Scope
- Refactor the affected pages to avoid synchronous state cascades in effects.
- Use stable async loaders or event-oriented patterns.
- Ensure lint passes without suppressing the rules.

### Acceptance Criteria
- `cd frontend && npm run lint` passes.
- Behavior of tender listing, tender detail, and qualification flows remains intact.

### File Targets
- `frontend/src/pages/public/TenderListPage.tsx`
- `frontend/src/pages/tenderee/TenderDetailPage.tsx`
- `frontend/src/pages/tenderer/QualificationPage.tsx`

## Issue 10: Replace Hardcoded Notification Credentials And Non-Prod Stubs ✅ DONE

**Priority:** P2
**Estimate:** 1 day
**Depends on:** None
**Blocks:** None

### Problem
`NotificationConfig.mailSender()` overrides Spring Boot's auto-configuration with hardcoded SMTP host, port, username, and password as Java string literals (the file's own comment acknowledges they should be in `application.yml`). The `application.yml` SMTP block is the only sensitive section **not** using the `${ENV_VAR:default}` pattern used everywhere else in the file. SMS/push senders are always mock implementations with no profile gate.

### Scope
- **Delete `mailSender()`** from `NotificationConfig`. Spring Boot auto-configures `JavaMailSender` from `spring.mail.*` — the custom bean only overrides it with hardcoded values.
- Wrap `spring.mail.host`, `spring.mail.username`, `spring.mail.password` with env-var bindings (`${SMTP_HOST}`, `${SMTP_USERNAME}`, `${SMTP_PASSWORD}`).
- Gate mock SMS/push senders to `dev` or `test` profiles via `@Profile("dev,test")`.
- Document which channels are production-ready and which are intentionally stubbed.

### Acceptance Criteria
- No production runtime credentials are hardcoded in Java config or `application.yml`.
- `NotificationConfig` contains no `mailSender()` bean.
- Mock implementations are profile-gated.
- Notification behavior is explicit by environment.

### File Targets
- `notification-service/src/main/java/com/egov/tendering/notification/config/NotificationConfig.java`
- `notification-service/src/main/resources/application.yml`
- `config-repo/config-client-notification.yml`

## Issue 11: Remove Embedded Default Passwords And Placeholder Secrets ✅ DONE

**Priority:** P1
**Estimate:** 1-2 days
**Depends on:** None
**Blocks:** None

### Problem
Committed runtime config still includes placeholder passwords and insecure default secrets for databases, JWT, and SMTP.

### Scope
- Replace committed defaults with environment variables or documented secret references (`${SECRET_NAME}` — no plaintext fallback for production secrets).
- Keep local development possible through explicit `.env` or documented export steps.
- Review Docker and config-repo files for the same issue.

### Acceptance Criteria
- No committed runtime config contains production-like placeholder secrets as active defaults.
- Local startup instructions clearly state required environment variables.
- Security-sensitive defaults are minimized and non-production-only where unavoidable.

### File Targets
- `docker-compose.yml`
- service `application.yml` and `application.yaml` files
- `config-repo/*.yml`

## Issue 12: Fix Static Constant In KafkaTopics ConfigurationProperties Bean ✅ DONE

**Priority:** P1
**Estimate:** 0.5 days
**Depends on:** None
**Blocks:** None

### Problem
`KafkaTopics` is a `@ConfigurationProperties` bean where all fields are correctly bound from `kafka.topics.*` in `application.yml`. However `NOTIFICATION_SENT` is declared `public static final String` — it bypasses the config system entirely and can never be overridden by environment or config-server. Any consumer of that constant uses a hardcoded value silently.

### Scope
- Convert `NOTIFICATION_SENT` to a regular instance field with a default value.
- Verify all consumers use the injected bean value, not the former constant.

### Acceptance Criteria
- No `static final` String constants remain in any `@ConfigurationProperties` class.
- `NOTIFICATION_SENT` value is bound from `kafka.topics.notification-sent` in config.

### File Targets
- `notification-service/src/main/java/com/egov/tendering/notification/config/KafkaTopics.java`

## Issue 13: Externalize Gateway Public-Path List From Java Source ✅ DONE

**Priority:** P1
**Estimate:** 0.5 days
**Depends on:** None
**Blocks:** None

### Problem
Public endpoint paths in the gateway `SecurityConfig` are hardcoded as Java string literals. Adding a new unauthenticated endpoint requires a code change and redeployment of the gateway, rather than a config-server update.

### Scope
- Create `GatewayProperties` `@ConfigurationProperties` bean (prefix `app.gateway`).
- Add `public-paths` (any-method) and `public-get-paths` (GET-only) lists to `application.yml`.
- `SecurityConfig` builds its permit matchers from the injected `GatewayProperties` lists.
- Keep infrastructure paths (`/actuator/**`, OPTIONS preflight) hardcoded as they are not operational concerns.

### Acceptance Criteria
- Adding a new public endpoint requires only a change to `application.yml`, not to `SecurityConfig.java`.
- All currently public paths are preserved in the config file.
- `SecurityConfig` contains no URL string literals beyond `/actuator/**`.

### File Targets
- `gateway-service/src/main/java/com/egov/tendering/gateway/config/GatewayProperties.java` (new)
- `gateway-service/src/main/java/com/egov/tendering/gateway/config/SecurityConfig.java`
- `gateway-service/src/main/resources/application.yml`

## Suggested Sprint Breakdown

### Sprint 1
- 1. Decide And Document The System JWT Model
- 2. Implement A Single JWT Validation Path Across Gateway And Services
- 3. Normalize JWT Claims And Externalize Claim-Name Config *(includes removing "roles"/"userId" string literals from all SecurityConfig, JwtTokenProvider, JwtUserIdExtractor)*
- 4. Reconcile Gateway Runtime Config With Frontend And Deployment *(includes GatewayProperties and public-path externalization — supersedes Issue 13)*
- 12. Fix Static Constant In KafkaTopics ConfigurationProperties Bean
- 13. Externalize Gateway Public-Path List From Java Source *(merged into Issue 4 scope)*

### Sprint 2
- 5. Redesign Bid Sealing To Use Defensible Key Management *(includes BidSealingProperties)*
- 6. Make Unseal Operate On The Sealed Artifact And Add Tamper Tests *(includes cron externalization and false-positive fix)*
- 7. Split Unit Tests From Docker-Dependent Integration Tests
- 8. Add End-To-End Auth Smoke Tests Through The Gateway

### Sprint 3
- 9. Fix Frontend React Hook And Lint Violations
- 10. Replace Hardcoded Notification Credentials And Non-Prod Stubs *(delete mailSender() bean, env-var SMTP)*
- 11. Remove Embedded Default Passwords And Placeholder Secrets
