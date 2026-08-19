# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Added

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
