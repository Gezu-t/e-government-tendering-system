# JWT Architecture Decision

**Date:** 2026-03-17
**Status:** Accepted

## Decision

This platform uses **first-party HS512 JWTs issued by `user-service`**.

No external OIDC/OAuth2 provider (Keycloak, Auth0, etc.) is part of this stack.
All services validate tokens using the same shared symmetric secret.

## Rationale

- The project has no external identity provider in its service inventory.
- First-party JWTs require only the `user-service` and a shared secret — no additional infrastructure.
- HS512 with a 256-bit+ secret provides adequate security for an e-government procurement system.
- Moving to asymmetric (RS256) or an external OIDC provider remains possible later and requires only a swap of the `JwtDecoder` bean.

## Token Contract

| Field | Value |
|-------|-------|
| Issuer | `user-service` (no `iss` claim — single-issuer system) |
| Algorithm | HS512 |
| Subject (`sub`) | username |
| Expiry (`exp`) | `${JWT_EXPIRATION:86400000}` ms from issuance |

### Claims

| Claim | Config property | Example value |
|-------|-----------------|---------------|
| `roles` | `app.security.jwt.claim-roles` | `["ROLE_ADMIN","ROLE_TENDERER"]` |
| `userId` | `app.security.jwt.claim-user-id` | `42` |
| `permissions` | `app.security.jwt.claim-permissions` | `["READ_TENDER"]` |

The claim names are read from `app.security.jwt.claim-*` properties (config-server canonical, `config-client.yml`).
**Roles are stored as a JSON array** — `JwtGrantedAuthoritiesConverter` processes them natively.

## Signing Key

- Provided as `${JWT_SECRET}` environment variable (no plaintext default).
- Must be at least 64 ASCII characters (512 bits) to safely drive HS512.
- The same value is used by `user-service` for signing and by all resource services for verification.
- Canonical config location: `app.security.jwt.secret: ${JWT_SECRET}` in `config-client.yml` (shared).

## Validation Path

```
Client  →  Gateway (NimbusReactiveJwtDecoder, HS512)
        →  Resource service (NimbusJwtDecoder, HS512)
```

Each service defines its own `JwtDecoder` (or `ReactiveJwtDecoder`) bean using `MacAlgorithm.HS512`
with `app.security.jwt.secret`. Spring Security auto-configuration backs off when the bean is present.

## Rejected Alternatives

| Option | Reason rejected |
|--------|-----------------|
| OIDC / JWK-Set-URI | No external IdP exists in this stack; startup would fail with connection errors |
| Shared per-service secrets | Prevents a single token from working across services |
| RS256 asymmetric keys | Higher operational overhead; first-party symmetric is sufficient for current scale |
