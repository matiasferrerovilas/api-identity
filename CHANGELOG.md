# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Security

- `spring.rabbitmq.username`/`password` in `application-prod.yaml` were the only prod secret still
  hardcoded in plain text (`api-identity`/`api-identity`) while `DB_USERNAME`/`DB_PASSWORD`/
  `REDIS_PASSWORD`/`CORS_ALLOWED_ORIGINS` in the same file were already env vars — now
  `${RABBIT_USERNAME}`/`${RABBIT_PASSWORD}`, no default.

### Changed

- `WorkspaceController.deleteWorkspace`/`WorkspaceService.deleteWorkspace` renamed to
  `leaveWorkspace` — the method actually removes the authenticated user's own membership (`DELETE
  /v1/workspaces/{workspaceId}`, unchanged), not the workspace itself; the old name misled readers
  into thinking it deleted the workspace. Pure rename, no behavior or contract change.
- New `PATCH /v1/workspaces/{workspaceId}/transfer-ownership` (`TransferOwnershipDTO.newOwnerUserId`)
  lets an OWNER (or a global `ROLE_ADMIN`) explicitly hand ownership to another member. Demotes the
  workspace's actual current OWNER (looked up via `WorkspaceMemberRepository.
  findByWorkspaceIdAndRole`), not the acting user's own membership — an earlier version of this
  demoted the actor instead, so an admin-initiated transfer (admin isn't the OWNER, possibly not
  even a member) left the previous OWNER untouched and the workspace with two `OWNER` rows; caught
  before this ever shipped. Additive: `leaveWorkspace` still auto-picks another member as the new
  OWNER when its own OWNER leaves, unchanged, so existing "leave workspace" UI across the frontends
  keeps working without a required transfer-first step. New `AuditAction.OWNERSHIP_TRANSFERRED`
  audit entry recorded on transfer. No frontend wired up to this endpoint yet.
- CORS allowed origins moved out of `SecurityConfiguration.corsConfigurationSource()` and into config
  (new `CorsProperties`, `@ConfigurationProperties(prefix = "app.cors")`, same pattern as
  `JwtProperties`): `application.yaml` keeps the current 3 origins as the dev/base default,
  `application-prod.yaml` now reads `app.cors.allowed-origins` from the `CORS_ALLOWED_ORIGINS` env
  var (comma-separated, Spring's relaxed binding splits it into the list) instead of a fixed prod
  value baked into the same Java list. A self-hoster's own frontend origin no longer requires editing
  and recompiling `SecurityConfiguration.java` — just set the env var (prod) or edit `application.
  yaml` (dev/local).

### Added

- Correlation ids across the event/log chain: a new `CorrelationIdFilter` (`com.api.identity.
  logging`, highest-precedence servlet filter) stamps every request with an id — reused from an
  incoming `X-Correlation-Id` header when a gateway app already has one and it looks like an id
  (≤100 chars, `[A-Za-z0-9._-]+`), generated fresh otherwise, so an arbitrary caller-supplied string
  never lands in every log line/MDC value unvalidated. Puts it in MDC (`logging.pattern.level` now
  includes `%X{correlationId}` so every log line for the
  request carries it) and echoes it back as a response header. `CorrelationIdHolder.current()` reads
  it from anywhere in the call stack; `MemberRemovedEvent`, `InvitationCreatedEvent`, and
  `InvitationAcceptedEvent` all gained a `correlationId` field populated from it, so RabbitMQ
  payloads can finally be traced back to the HTTP request that caused them. Backward-compatible for
  existing consumers (api-movements, api-keep deserialize with Jackson's default
  ignore-unknown-properties behavior). Follow-up not done here: the consumers don't yet read
  `correlationId` back into their own MDC/logs, so cross-service tracing needs that other half too.

### Fixed

- README: the endpoint table still called `DELETE /v1/workspaces/{workspaceId}` "Delete a
  workspace" (predates the `leaveWorkspace` rename) and was missing `PATCH .../transfer-ownership`
  and `DELETE .../members/{userId}` entirely, despite both existing. Monitoring section claimed
  `/actuator/prometheus` serves metrics — it 404s, since `micrometer-registry-prometheus` isn't a
  dependency, even though the path is permitted in `SecurityConfiguration`.
- `WorkspaceService.leaveWorkspace` (the "leave workspace" endpoint) now deactivates the workspace
  (`isActive = false`) when the leaving member was the last one. Previously the membership row was
  deleted but the workspace itself lived on forever with no members and no owner — no way to find
  or clean it up.
- `WorkspaceInvitationService.acceptRejectInvitation` threw `LazyInitializationException` on
  `Workspace.name` when accepting an invitation: the method wasn't `@Transactional`, so by the time
  it read `invitation.getWorkspace().getName()` to build the `InvitationAcceptedEvent`, the session
  that fetched `invitation` had already closed, leaving the lazy `@ManyToOne workspace` association
  as an uninitialized proxy. Added `@Transactional` to keep one session open for the whole method.

### Added

- `POST /v1/onboarding/start` — creates a user and their first workspace(s) in a single
  `@Transactional` call (`OnboardingService.start`), so a client never ends up with a user but no
  workspace (or vice versa) if something fails between two separate requests. Replaces the pattern
  of a gateway app calling `POST /v1/users` then `POST /v1/workspaces` itself; used by both
  `api-movements` and `api-keep`'s onboarding flows.
- `WorkspaceMemberDTO.Metadata` now includes `memberDetails` (userId, email, role per member), not
  just the flat email list — needed so a client can call the kick-member endpoint, which requires a
  userId. Purely additive, existing `members: List<String>` consumers are unaffected.
- Two new RabbitMQ events on `identity.topic` (previously only invitation-sent existed): `identity.invitation.accepted`
  (published from `WorkspaceInvitationService.acceptRejectInvitation`) and `identity.member.removed` (published from
  `WorkspaceMembershipService.removeMembership`, alongside the new kick-member feature). Sibling services now know
  in real time when someone gains or loses access to a shared workspace, not just when they're invited.
- Minimal audit log (`AuditLog` entity, migration `008__create_audit_log.sql`): records who invited,
  accepted/rejected, joined, or left a workspace, with a timestamp. Inserted at the existing
  `sendInvitation`/`acceptRejectInvitation`/`addMembership`/`deleteWorkspace` (leave) call sites — no new
  business logic, no schema changes beyond the new table. New `GET /v1/workspaces/{workspaceId}/audit-log`
  endpoint returns it most-recent-first, gated to `OWNER`/`COLLABORATOR` (same permission level as inviting).

## [1.3.0] - 2026-08-17

### Added

- `demo` Spring profile guarded by `@Profile("demo")`, with a `DemoDataSeeder` (`ApplicationRunner`) that
  idempotently seeds a demo user (`demo@example.com`), a demo workspace with a fixed id of `1`, and a
  `WorkspaceMember` linking the two with role `OWNER`. The fixed workspace id is a suite-wide convention:
  `api-movements` and `api-keep` each seed their own demo domain data assuming this workspace already exists.
- `application-demo.yaml` with local-friendly defaults (overridable via env vars) for running the `demo` profile.
- README "Demo Mode" section documenting how to run it and how it relates to the sibling apps' demo profiles.

## [1.2.0] - 2026-08-16

### Added

- Redis-backed rate limiting (`RateLimiterService`), applied to invitation sending (10 batches/hour) and
  user creation (20/hour); fails open if Redis is unavailable.
- Centralized `ErrorHandler` and `RateLimitExceededException`.
- RabbitMQ publishing of invitation-created events (`InvitationEventPublisher`, `InvitationCreatedEvent`,
  `RabbitConfig`), consumed by `api-movements` and `api-keep`.
- `SourceServiceResolver` to resolve the calling app from the JWT's `app` claim.

### Fixed

- Security hardening across `ApiService`, `WorkspaceInvitationService`, and `OnboardingService`.

## [1.1.0] - 2026-08-11

### Added

- `Api` entity and `ApiService` to support multiple calling apps (`api-movements`, `api-keep`) instead of a
  single hardcoded app — onboarding and default-workspace resolution are now scoped per calling app.

### Changed

- User creation and lookup endpoints updated to resolve the caller's app rather than assuming a single client.

## [1.0.0] - 2026-07-11

Baseline release. Core identity and workspace service for the M2 suite:

### Added

- User auto-provisioning on first login, resolved from the JWT's `app` claim.
- Workspaces and role-based membership (`OWNER` / `COLLABORATOR` / `READ_ONLY`): create, list, delete.
- Workspace invitations by email, with accept/decline flow.
- Onboarding tracking (first login, product tour completion) per user.
- Keycloak OAuth2 / JWT (RS256) resource server authentication.
- Swagger/OpenAPI documentation UI.
- Liquibase database migrations (`ddl-auto: none`) against MySQL.

[Unreleased]: https://github.com/matiasferrerovilas/api-identity/compare/v1.3.0...HEAD
[1.3.0]: https://github.com/matiasferrerovilas/api-identity/compare/v1.2.0...v1.3.0
[1.2.0]: https://github.com/matiasferrerovilas/api-identity/compare/v1.1.0...v1.2.0
[1.1.0]: https://github.com/matiasferrerovilas/api-identity/compare/v1.0.0...v1.1.0
[1.0.0]: https://github.com/matiasferrerovilas/api-identity/releases/tag/v1.0.0
