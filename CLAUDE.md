# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## System context

This repo is one microservice (`att`) in a larger system of ~5 microservices behind an API Gateway /
service discovery layer (see `../attendance-project-common/docs/TimeTracker_docs.md` for the overall
design). Do not implement authentication, user registration, or role management here — that's owned by
the sibling **Attendance-Accounting** service (`../Attendance-Accounting`), which issues the JWTs this
service only *validates* (`att.security`). This service is the source of attendance/time-tracking data
and monthly statistics; a downstream **AI Assistance & Notification** service consumes the statistics
over Kafka (topic `att.user-statistic-ai-analysis`) and is not part of this repo.

## Commands

```bash
./mvnw clean install              # build (also regenerates the OpenAPI interfaces/DTOs/client, see below)
./mvnw test                       # run all tests
./mvnw test -Dtest=AttendanceTimeTrackingControllerTest        # run one test class
./mvnw test -Dtest=AttendanceTimeTrackingControllerTest#openSession_asAdmin_opensNewRecord  # single test method
./mvnw clean verify -Pci          # what CI runs: build + tests + JaCoCo coverage check (min 85% line coverage)
./mvnw spring-boot:run            # run locally (H2 file DB at ./data/att, see application.properties)
```

Requires env var `ATTENDANCE_ACCOUNTING_JWT_SECRET` (HMAC secret for the JWT resource server) — tests supply their own via
`src/test/resources/application.properties`. Optional: `KAFKA_BOOTSTRAP_SERVERS` (default
`localhost:9092`) and `ATTENDANCE_ACCOUNTING_BASE_URL` (default `http://localhost:8081`).

The JaCoCo threshold is a real gate: `-Pci` fails the build under 85% line coverage (bundle-wide,
generated sources excluded — see the `<excludes>` in the `ci` profile). New production code needs tests
in the same change, or CI goes red. `.github/workflows/ci.yml` then builds and pushes a Docker image to
GHCR and tags the version on `main`.

There is no linter/formatter configured in this repo (the shared IntelliJ Google-style config lives in
`../attendance-project-common/sharedFiles/`).

## Architecture

### Everything contract-first: three OpenAPI specs → generated code

`src/main/resources/swagger-source/` holds three specs, each with its own `openapi-generator-maven-plugin`
execution bound to `generate-sources` (output in `target/generated-sources`):

| Spec                                 | Generates                                 | Role                                                |
|--------------------------------------|-------------------------------------------|-----------------------------------------------------|
| `attendance-time-tracking.yaml`      | `att.api.TimeTrackingApi` + `att.dto`     | this service's inbound REST API                     |
| `attendance-accounting-account.yaml` | `att.client.accounting.{api,dto,support}` | outbound client for Attendance-Accounting           |
| `attendance-statistic-events.yaml`   | `att.messaging.dto`                       | Kafka message payloads (schema-only spec, no paths) |

**To add/change an endpoint or an event payload, edit the yaml first**, then rebuild so the interface/DTO
regenerates, then implement/adjust the code. Never hand-write a class into `att.api`, `att.dto`,
`att.messaging.dto` or `att.client.accounting.*` — it will be overwritten and it breaks the JaCoCo
exclusions.

`attendance-accounting-account.yaml` is a **manually synced local copy** of Attendance-Accounting's own
spec, kept here only to drive client codegen. It carries a locally-added placeholder path
(`GET /account/user/{idUser}`, `getUserInfo`) that does not exist upstream, and it is already drifting
from upstream (which has since replaced it with `GET /account/tenant/{tenantId}/userId/{idUser}`). Re-copy
from upstream and reconcile rather than assuming the two match.

### Request pipeline: strategy services over a shared mutable context

**Always follow this existing pattern for new or changed endpoints — don't introduce a different
structure (no business logic in the controller, no bypassing the pipeline, no new base class).**

Each endpoint delegates to one `@Service` "strategy" class in `att.service.strategy`, all extending
`DataTimeServiceBase<R>` (`att.service.base`), which fixes a template method pipeline:

```
execute(context)  [@Transactional]
  -> fetch(context)              // load rows / derive inputs — override per service
  -> validate(context)           // reject bad input — override per service
  -> executeBusiness(context)    // mutate context lists — override per service
  -> persist(context)            // saveAll of userWorkSessionList + userLeaveDaysList + monthStatisticList
  -> mapResult(context)          // maps saved entities -> context.responseDataTimeDto
```

Services override only the step(s) they need (most override just one). `BaseGetService` is a further base
for read-only strategies; its `validate` rejects the request when `fetchIncompleteSessions(...)` finds
open sessions in range. Use `executeWithoutTransactional(...)` instead of `execute(...)` when a caller
needs to manage its own transaction boundary.

Strategies also compose: `StatisticInfoService` calls `countWorkedDaysService.fetch(context)`,
`getWorkedMinutesBetweenService.fetch(context)` and `getOvertimeMinutesBetweenService.fetch(context)`
directly, per user, inside its own `executeBusiness`. Reuse another strategy's step like this instead of
duplicating its query.

`DataTimeContext<T>` (`att.context`) is the single input/output object threaded through a pipeline — it
carries request params (`tenantId`, `idUser`, date ranges, the request DTO as `task`, etc.) as well as
pipeline output. All aggregate output is **keyed by user id** (`totalDaysPerUser`,
`totalWorkMinutesPerUser`, `totalOvertimeMinutesPerUser`, `vacationPerUser`, `sickDayPerUser`) so one
context can be reused across a multi-user pipeline without one user's result overwriting another's —
keep new aggregates per-user for the same reason. Controllers build a context via its Lombok builder,
hand it to one strategy service, then read the result back off the same context object.

`StatisticInfoHolder` (on the context) carries the monthly-statistics flow: the `targetUserIds` to
compute, the Guava `Table<userId, month, MonthlyUserStatisticInfoDto>` response, the `MonthStatistic`
rows to persist, and the `report` flag that decides whether events get published.

### Post-service action: how async publishing hangs off the pipeline

Kafka publishing deliberately happens **after** the transactional pipeline, not inside it. `DataTimeContext`
implements `BusinessStrategyContext`, whose `getPostServiceAction()` returns a `Consumer` that the
controller runs once the strategy has returned:

```java
statisticInfoService.execute(context);
context.

getPostServiceAction().

accept(context);   // -> AsyncMessageHandler.prepareAndSendAsyncStatMsg
```

The handler injected into the controller is `AsyncMessageHandler` (implementation:
`att.messaging.producer.StatisticAsyncMessageHandler`), and it publishes only when
`statisticInfoHolder.report` is true. A rejected/failed request therefore publishes nothing. Follow this
route for any new async side effect rather than sending from inside a strategy service.

### Messaging (Spring Cloud Stream + Kafka)

Bindings live in `src/main/resources/async-messages.yml` (imported via `spring.config.import`), binding
names are constants in `att.messaging.MessagingConstants`:

- `monthStatisticProducer-out-0` → topic `att.month-statistic`: a `MonthStatisticEvent` (identity + target
  `yearMonth` only) published per user when statistics are computed with `report=true`.
- `monthStatisticEnrichment-in-0` ← topic `att.month-statistic`, group `att-service`, DLQ
  `att.month-statistic.dlq`: this service consumes its own trigger event, loads up to 7 months of history
  (`MonthStatisticRepository.findTop7...`, anchored on the event's month, most recent first), and
  republishes.
- `statisticAnalysisRequestProducer-out-0` → topic `att.user-statistic-ai-analysis`: the enriched
  `UserStatisticAnalysisRequestEvent` for the downstream AI service.

`StatisticEventProducer` sends via `StreamBridge` with the user id as the Kafka message key (partition
affinity per user). The consumer (`StatisticEnrichmentConsumer`, a `java.util.function.Consumer` bean) is
gated by `att.messaging.enrichment.enabled` (default true; tests set it false) and validates the payload
with a `jakarta.validation.Validator` — a `ConstraintViolationException` is configured as non-retryable so
malformed messages go straight to the DLQ.

### Outbound call to Attendance-Accounting

`AccountingClientConfiguration` builds the generated `AccountApi` over a `RestClient` with JDK HTTP
connect/read timeouts from `att.integration.attendance-accounting.*`, an interceptor that **forwards the
caller's JWT** from the `SecurityContextHolder` (so downstream authorization sees the original user), and
`InternalApiResponseErrorHandler`, which maps 404 → `NotFoundException` and any other error →
`InternalApiException`. Reuse that error handler for any further internal client. Note: the bean is wired
and tested but **no production code calls `AccountApi` yet**.

### Multi-tenancy & authorization

- Every persisted row and most endpoints carry a `tenantId` path/column; `att.dao.*` queries are always
  scoped by tenant + user.
- `TenantInterceptor` (`att.security`, registered in `WebConfig`) is a `HandlerInterceptor` that rejects
  (403) any request whose path `tenantId` doesn't match the JWT's `tenantId` claim, unless the caller has
  `ROLE_ADMINISTRATOR`.
- Method-level authorization is additionally enforced with `@PreAuthorize` on controller methods
  (`att.security.SecurityConstants.SecurityRoles`: `USER`, `MODERATOR`, `ADMINISTRATOR`), typically
  `hasRole(ADMINISTRATOR) || #idUser.toString() == authentication.name` for self-service endpoints.
  Everything else is admin-only.
- Auth is JWT-only (`SecurityConfiguration`), HMAC-signed (`att.security.jwt.secret`), validated as an
  OAuth2 resource server, roles read from the `authorities` claim with a `ROLE_` prefix — no login/token
  issuance happens in this service.
- Errors surface through `@ResponseStatus` exceptions in `att.exceptions` (`BadRequestException`,
  `NotFoundException`, `InternalApiException`) with messages centralised in `ErrorConstants` /
  `ErrorProvider`; access-denied responses are JSON via `CustomAccessDeniedHandler`. There is no
  `@ControllerAdvice` — add a new `@ResponseStatus` exception instead of a handler class.

### Persistence

- Entities (`att.model`) ↔ tables: `DataTime` → `att_work_sessions` (one row per open/close session),
  `MonthStatistic` (`@EmbeddedId MonthStatisticKey` = tenant + user + month start) → `att_month_statistic`,
  `LeaveDay` (`@EmbeddedId LeaveDayKey` = tenant + user + date + `LeaveType` VACATION/SICK) →
  `att_user_leave_days`.
- Worked/overtime minutes are computed in SQL, not Java: see the native queries in
  `SessionAttendanceTimeRepository` (`calculateWorkedMinutes`, `calculateOvertimeMinutes` — overtime is
  minutes beyond 480/day, per day, summed). MapStruct mappers (`att.mapper`) do entity/DTO/event mapping.
- Local/dev runs on a file-based H2 database (`spring.jpa.hibernate.ddl-auto=update`); tests run on an
  in-memory H2 database with schema/seed data applied via Flyway migrations in
  `src/test/resources/db.migration` (`V1__schema.sql`, `V2__att_work_sessions_setup.sql`,
  `V3__att_month_statistic_setup.sql`). The `postgresql` dependency is present for production use but no
  active Postgres datasource is configured in this repo.

### Known gaps (spec vs. current code — don't assume these exist)

- **No scheduled jobs.** The designed nightly conflict-detection and monthly-aggregation schedulers do not
  exist; monthly statistics are computed on demand by `POST /attendance/statistic/tenant/{tenantId}` with
  `report=true`. There is no daily-conflict Kafka flow either — only the month-statistic pair of topics
  above.
- **No inbound call to Accounting.** `AccountApi` is configured but unused in production code (see above).
- Don't reference a scheduler, a conflict topic, or an Accounting lookup as if it exists — check first.

## Testing conventions

**New tests must follow this existing pattern — full end-to-end flow through a real running server, not
component/unit-style tests.** Do not write `@WebMvcTest`/`MockMvc`/mocked-service-layer tests for
controller behavior; this project's convention is a real `@SpringBootTest(webEnvironment = RANDOM_PORT)`
hitting the actual HTTP endpoint over `RestTemplate`, exercising the whole pipeline (security filter
chain, tenant interceptor, strategy service, real DB) exactly as production traffic would.

Tests extend `BaseApiControllerTest` (`src/test/java/att/controller`), which boots the full app on a
random port and drives it over a real `RestTemplate` using pre-built JWTs for admin/user roles
(`jwtTokenAdministrator`, `jwtTokenUser`, plus a tenant-scoped one). Reuse its `sendRequestWithAdmin` /
`sendRequestWithUserRole` helpers, the URL/`range(...)`/`statistic(...)` builders, the DTO factory methods
and the seeded IDs/date constants (`SEEDED_OPEN_ID`, `RANGE_START`/`RANGE_END`, `SEEDED_STAT_*`) rather
than re-deriving fixtures — the seed data comes from the Flyway migrations, and each test method that
needs a clean DB is annotated `@FlywayTest`.

Messaging is tested **without a broker**: the base class declares `@MockitoBean StatisticEventProducer`,
so publishing tests (`StatisticEventPublishingTest`) drive the real HTTP endpoint and verify captured
events, and the consumer test (`StatisticEnrichmentConsumerTest`) constructs
`StatisticEnrichmentConsumer` directly with real repository/mapper/validator beans and feeds it a
`Message<>`. Don't add an embedded Kafka or a test binder; extend this setup. Outbound-client behavior is
tested against WireMock (`AccountingClientConfigurationTest`).

Assertions carry a `"Reason: ..."` message explaining the expectation, and each test has a `@DisplayName`
describing the behavior — keep both.

## General coding principles

- Avoid duplicating logic — extend/reuse the existing base classes and helpers (`DataTimeServiceBase`,
  `BaseGetService`, another strategy's `fetch`, `AttUtility`, `BaseApiControllerTest`) instead of
  copy-pasting similar code across strategy services or tests.
- Apply SOLID principles and standard Java/OOP design-pattern practice (strategy for
  `att.service.strategy`, template method in `DataTimeServiceBase`, single-responsibility per
  service/class) — follow the idioms already present in the codebase rather than introducing new ones
  without reason.
