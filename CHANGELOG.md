# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

## [1.6.1] - 2026-09-01

### Fixed
- New `WebBindingRuntimeHints` (wired via `@ImportRuntimeHints` on `UserApplication`) registers
  reflection for the three RabbitMQ-only event records — `InvitationCreatedEvent`,
  `InvitationAcceptedEvent`, `MemberRemovedEvent`. These are published solely through
  `RabbitTemplate.convertAndSend` (generic `Object`) and never returned from a `@RestController`,
  so Spring's AOT scan never discovered them and they had no reflection metadata in the native
  image. Under GraalVM's closed world that surfaces at runtime as
  `UnsupportedFeatureError: Record components not available` when the event is first serialized —
  never in tests, which run on the JVM. Same gap already fixed in api-movements (where it actually
  crashed prod, `NotificationRecord`) and proactively in api-keep. Not reproducible in this
  environment (no native compiler); needs confirmation via a real native-image container rebuild.

## [1.7.0] - 2026-09-01

### Added
- New `AdminController` (`/v1/admin`), `GET /v1/admin/users` — lists every user in the instance
  along with the active workspaces each one belongs to and their role in each. Didn't exist
  before: `GET /v1/users` needs ids up front and `GET /v1/workspaces/members` only returns the
  authenticated caller's own workspaces — there was no admin-wide view. Secured with
  `.requestMatchers("/v1/admin/**").hasRole("ADMIN")` in `SecurityConfiguration`, same pattern
  already used for `/actuator/**`. New `WorkspaceMemberRepository.findAllActiveWithWorkspaceAndUser()`
  (join-fetches workspace + user to avoid N+1 across the whole listing). New `AdminUserMapper`
  (MapStruct, same convention as `UserMapper`/`WorkspaceMapper`) does the entity→DTO mapping.
  Powers the new `/users` view in fe-identity.

### Fixed
- `GET /v1/admin/users` threw `HttpMessageNotWritableException` /
  `LazyInitializationException` on `User.userRoles` (`@ElementCollection(fetch = LAZY)`) —
  Jackson serializes the response *after* the `@Transactional` method returns and its Hibernate
  session closes, so just passing the lazy `Set` reference into the DTO wasn't enough.
  `AdminUserMapper` now does `Set.copyOf(user.getUserRoles())`, forcing the collection to load
  while the session is still open.

## [1.6.0] - 2026-08-31

### Added
- `WorkspaceSendInvitationDTO` now takes a required `role` (`COLLABORATOR` or `READ_ONLY` —
  `OWNER` is rejected with a `BusinessException`, that's `transferOwnership`'s job). The chosen
  role is stored on the `WorkspaceInvitation` and applied verbatim by
  `WorkspaceMembershipService.addMembership` when the invitation is accepted, instead of the
  hardcoded `COLLABORATOR` from before. Closes the roadmap gap where `READ_ONLY` was checked
  everywhere (`requireAtLeastCollaborator`) but unreachable — there was no path to actually
  become a `READ_ONLY` member. `WorkspaceInvitationDTO`/`WorkspaceSentInvitationDTO` and the
  `InvitationCreatedEvent` RabbitMQ payload also now carry `role`, so both gateway apps' live
  invitation notifications stay in sync without a refetch.
- New migration `009__add_role_to_workspace_invitations.sql` — `workspace_invitations.role`,
  `NOT NULL DEFAULT 'COLLABORATOR'` (matches the old hardcoded behavior for any row that predates
  this column).

## [1.5.0] - 2026-08-31

### Added
- `GET /v1/users/me?workspaceId=` — when given, the response now includes
  `metadata.workspaceRole`, the caller's role in that specific workspace (null if not a member).
  api-identity has no notion of "the" active workspace itself (that's app-specific state each
  caller owns), so the caller decides which workspace's role it wants, if any.

### Changed
- `WorkspaceMemberDTO.Metadata` dropped `members: string[]` — it was fully redundant with
  `memberDetails` (which already has email plus userId/role, everything `members` had and more).
  Every consumer already had `memberDetails` available.

## [1.4.1] - 2026-08-30

### Added
- `GET /v1/users/lookup?email=` (`UserController`) — resolves an email to a user id, 404 if no
  account matches. Wraps the existing `UserService.getUserByEmail(List<String>)` with a
  single-email call. Built for api-keep's new user-to-user file sharing, which needs to validate a
  share target *before* creating the grant (unlike `WorkspaceInvitationService.sendInvitation`,
  which silently drops emails that don't match a registered user — this fails loudly instead, so
  the caller can surface an honest "no account with that email" error).
- `GET /v1/invitations/sent` and `DELETE /v1/invitations/{invitationId}` — a sender can now list
  the invitations they've sent (most recent first, any status) and cancel one that's still
  `PENDING` before the recipient acts on it. New `InvitationStatus.CANCELLED` and
  `AuditAction.INVITATION_CANCELLED`. Mirrors `acceptRejectInvitation`'s ownership check (404, not
  403, when the caller didn't send the invitation being cancelled) and its "already answered"
  guard (`BusinessException` if the invitation isn't `PENDING` anymore). Wired through the
  api-movements and api-keep gateways (`IdentityClient.getSentInvitations`/`cancelInvitation`) and
  surfaced in both fe-movements and fe-keep as a "Sent Invitations" card next to the existing
  pending-invitations one.

### Security

- `GET /v1/workspaces/{workspaceId}` (`WorkspaceService.getWorkspaceDTOById`) never verified the
  authenticated user belonged to the requested workspace before returning it — unlike every sibling
  endpoint in `WorkspaceController`. Any authenticated user of any tenant could enumerate workspace
  ids and get back the owner plus the full list of member emails. Now calls
  `workspaceMembershipService.verifyMembership` first, same as `getWorkspaceMembers(Long
  workspaceId)`.
- `GET /v1/users/lookup?email=` and `GET /v1/users?ids=` had no rate limit and no requirement to
  share a workspace with the target — any authenticated user of any app in the suite could probe
  arbitrary emails/ids and learn who has an account, a real account-enumeration surface. Now
  rate-limited to 30 lookups/hour per caller (`UserService.enforceLookupRateLimit`, same
  `RateLimiterService` used by invitations/user-creation), and `getUsersByIds` additionally rejects
  a single call requesting more than 100 ids (`BusinessException`).
- `WorkspaceSendInvitationDTO.emails` had no validation annotations — the invitation rate limit
  counts "batches" (10/hour), so a single call with thousands of emails bypassed the abuse
  protection entirely. Now `@NotEmpty @Size(max = 20)` on the list and `@Email` per element.
- `UserAddService.createLogInUser` (`POST /v1/users`, and transitively `POST /v1/onboarding/start`)
  keyed the created/reused `User` — and the user-creation rate limit — off the `email` field in the
  request body, without ever checking it against the authenticated JWT's own `email` claim. Any
  authenticated caller could send an arbitrary email and attach their onboarding to someone else's
  existing account, and the rate limit was trivially bypassable the same way (a different email per
  call). Now the request email must match `Authentication.getName()` (case-insensitively) or the
  call is rejected with `PermissionDeniedException`; the rate-limit key is keyed off the
  authenticated email instead of the body field.
- `spring.rabbitmq.username`/`password` in `application-prod.yaml` were the only prod secret still
  hardcoded in plain text (`api-identity`/`api-identity`) while `DB_USERNAME`/`DB_PASSWORD`/
  `REDIS_PASSWORD`/`CORS_ALLOWED_ORIGINS` in the same file were already env vars — now
  `${RABBIT_USERNAME}`/`${RABBIT_PASSWORD}`, no default.

### Changed

- Removed the unused `spring-boot-starter-oauth2-authorization-server` and
  `spring-boot-starter-security-oauth2-client` Gradle dependencies — this service only ever acts as
  an OAuth2 resource server validating Keycloak JWTs, never issues tokens, and is never itself an
  OAuth2 client. Both classes were dead classpath/native-image weight; the actual resource-server
  classes in use (`JwtDecoder`, `NimbusJwtDecoder`, `.oauth2ResourceServer()`) were only reachable
  as a transitive dependency of the authorization-server starter, which no longer holds — replaced
  with the correct, minimal `spring-boot-starter-oauth2-resource-server` declared directly.
- New `SecurityUtils` (`com.api.identity.security`) collapses the
  `SecurityContextHolder.getContext().getAuthentication()...filter(isAuthenticated)...orElseThrow`
  chain that was copy-pasted 7 times across `UserService` (4x), `OnboardingService`, and
  `UserAddService` into `currentAuthentication()`/`currentEmail()`/`currentRoles()`. Pure
  refactor — same `SecurityContextHolder`, same `PermissionDeniedException` on no/unauthenticated
  principal, no behavior change.
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
