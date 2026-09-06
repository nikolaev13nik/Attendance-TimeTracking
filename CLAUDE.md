# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## System context

This repo is one microservice (`att`) in a larger system of ~4 microservices behind an API Gateway /
service discovery layer. Do not implement authentication, user registration, or role management here —
that's owned by an upstream Accounting service, which issues the JWTs this service only *validates*
(`att.security`). This service is the source of attendance/time-tracking data and monthly statistics;
a separate AI Assistance & Notification service is the intended downstream consumer of that data (e.g.
conflict detection, monthly trend summaries) via async events — that integration is not yet implemented
in this codebase (see "Known gaps" below).

## Commands

```bash
./mvnw clean install              # build (also regenerates the OpenAPI interfaces/DTOs, see below)
./mvnw test                       # run all tests
./mvnw test -Dtest=AttendanceTimeTrackingControllerTest        # run one test class
./mvnw test -Dtest=AttendanceTimeTrackingControllerTest#openSession_asAdmin_opensNewRecord  # single test method
./mvnw clean verify -Pci          # what CI runs: build + tests + JaCoCo coverage check (min 60% line coverage)
./mvnw spring-boot:run            # run locally (H2 file DB at ./data/att, see application.properties)
```

Requires env var `ATT_JWT_SECRET` (HMAC secret for the JWT resource server) — tests supply their own via
`src/test/resources/application.properties`.

There is no linter/formatter configured in this repo.

## Architecture

### API is contract-first (OpenAPI → generated interface)

`src/main/resources/swagger-source/attendance-time-tracking.yaml` is the source of truth for the REST
API. The `openapi-generator-maven-plugin` (bound to `generate-sources`) generates the `TimeTrackingApi`
interface (`att.api`) and request/response DTOs (`att.dto`) into `target/generated-sources` at build
time. `AttendanceTimeTrackingController` implements `TimeTrackingApi`. **To add/change an endpoint,
edit the yaml first**, then rebuild so the interface regenerates, then implement/adjust the method.

### Request pipeline: strategy services over a shared mutable context

**Always follow this existing pattern for new or changed endpoints — don't introduce a different
structure (no business logic in the controller, no bypassing the pipeline, no new base class).**

Each endpoint delegates to one `@Service` "strategy" class in `att.service.strategy`, all extending
`DataTimeServiceBase<R>` (`att.service.base`), which fixes a template method pipeline:

```
execute(context)  [@Transactional]
  -> fetchAndValidate(context)   // load rows, validate input — override per service
  -> executeBusiness(context)    // mutate context.userWorkSessionList — override per service
  -> persist(context)            // timeRepository.saveAll(context.getUserWorkSessionList())
  -> mapResult(context)          // maps saved entities -> context.responseDataTimeDto
```

Services override only the step(s) they need (most override just one). `BaseGetService` is a further
base for read-only strategies that need `fetchIncompleteSessions(...)`. Use
`executeWithoutTransactional(...)` instead of `execute(...)` when a caller needs to manage its own
transaction boundary.

`DataTimeContext<T>` (`att.context`) is the single input/output object threaded through a pipeline —
it carries request params (`tenantId`, `idUser`, date ranges, the request DTO as `task`, etc.) as well
as pipeline output (`userWorkSessionList`, `responseDataTimeDto`, `totalHours`/`totalDays`/
`totalOvertimeHours`, and a `StatisticInfoHolder` for the monthly-statistics flow). Controllers build a
context via its Lombok builder, hand it to one strategy service, then read the result back off the same
context object. When adding a new strategy service, follow this pattern rather than inventing a new
return path.

### Multi-tenancy & authorization

- Every persisted row and most endpoints carry a `tenantId` path/column; `att.dao.SessionAttendanceTimeRepository`
  queries are always scoped by tenant + user.
- `TenantInterceptor` (`att.security`) is a `HandlerInterceptor` that rejects (403) any request whose
  path `tenantId` doesn't match the JWT's `tenantId` claim, unless the caller has `ROLE_ADMINISTRATOR`.
- Method-level authorization is additionally enforced with `@PreAuthorize` on controller methods
  (`att.security.SecurityConstants.SecurityRoles`: `USER`, `MODERATOR`, `ADMINISTRATOR`), typically
  `hasRole(ADMINISTRATOR) || #idUser.toString() == authentication.name` for self-service endpoints.
- Auth is JWT-only (`SecurityConfiguration`), HMAC-signed (`att.security.jwt.secret`), validated as an
  OAuth2 resource server — no login/token issuance happens in this service.

### Persistence

- Entity: `att.model.DataTime` maps to table `att_work_sessions` (one row per open/close session).
- Local/dev runs on a file-based H2 database (`spring.jpa.hibernate.ddl-auto=update`); tests run on an
  in-memory H2 database with schema/seed data applied via Flyway migrations in
  `src/test/resources/db.migration` (`V1__schema.sql`, `V2__att_work_sessions_setup.sql`). The
  `postgresql` dependency is present for production use but no active Postgres datasource is configured
  in this repo.
- `V1__schema.sql` also defines `att_month_statistic` and `att_user_leave_days`, but only
  `att_work_sessions` currently has a corresponding entity/repository — the other two are not yet wired
  up in code.

### Known gaps (spec vs. current code — don't assume these exist)

- `StatisticInfoService` (the `getMonthStatistic` endpoint's strategy) is a stub: vacation/sick-day
  aggregation and persistence of monthly stats are marked TODO, not implemented.
- `addLeaveDays` is unimplemented in the controller (returns `null`; call is commented out).
- No Kafka integration, and no scheduled jobs exist in this codebase yet, despite that being part of
  the intended design (daily conflict detection, monthly aggregation, events to a downstream AI/notification
  service). Don't reference Kafka topics/schedulers as if they exist — check before building on them.

## Testing conventions

Controller tests (`src/test/java/att/controller`) extend `BaseApiControllerTest`, which boots the full
app on a random port and drives it over a real `RestTemplate` (not `MockMvc`) using pre-built JWTs
for admin/user roles (`jwtTokenAdministrator`, `jwtTokenUser`, plus a tenant-scoped one). Reuse its
`sendRequestWithAdmin` / `sendRequestWithUserRole` helpers and the seeded record IDs/date constants
(e.g. `SEEDED_OPEN_ID`, `RANGE_START`/`RANGE_END`) rather than re-deriving fixtures — the seed data
comes from `V2__att_work_sessions_setup.sql`.
