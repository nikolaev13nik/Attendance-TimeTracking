# Attendance TimeTracking

The time-tracking service of the [Attendance Platform](https://github.com/nikolaev13nik/attendance-project-common):
employees' work sessions (clock in / clock out), leave days, and monthly statistics. It is also where the
reporting flow starts: a monthly report is published to Kafka and ends as an AI-written email.

## API

Defined contract-first in
[`attendance-time-tracking.yaml`](src/main/resources/swagger-source/attendance-time-tracking.yaml).
Every path carries the tenant; "self" means an employee acting on their own records.

| Method | Path | Who |
| --- | --- | --- |
| `PUT` | `/attendance/openSession/tenant/{tenantId}/userId/{idUser}` | administrator or self |
| `PUT` | `/attendance/closeSession/tenant/{tenantId}/userId/{idUser}` | administrator or self |
| `GET` | `/attendance/sessions/tenant/{tenantId}?idUser&startDate&endDate` | administrator or self |
| `GET` | `/attendance/check/tenant/{tenantId}/user/{idUser}` | administrator or self — sessions left open |
| `POST` | `/attendance/addLeaveDays/tenant/{tenantId}/userId/{idUser}` | administrator or self |
| `GET` | `/attendance/leaveDays/tenant/{tenantId}/userId/{idUser}` | administrator or self |
| `POST` | `/attendance/sessionChange/tenant/{tenantId}`, `/leaveDayChange/...` | administrator |
| `DELETE` | `/attendance/sessionRemove/...`, `/leaveDayRemove/...` | administrator |
| `GET` | `/attendance/workdays`, `/minutes`, `/overtimeMinutes` `/tenant/{tenantId}` | administrator |
| `POST` | `/attendance/statistic/tenant/{tenantId}?targetMonth&idUser&report` | administrator |

Employees can only clock in and out for today; earlier days need an administrator.

## Monthly statistics and the report flow

`POST /attendance/statistic` computes, for one employee or the whole company, the worked days, hours,
overtime, vacation and sick days of a month, and stores them in `att_month_statistic`. It refuses while any
session of that month is still open, so totals are never built on half-recorded days. Overtime is counted
per day above a configurable threshold (`ATT_OVERTIME_DAILY_THRESHOLD`, default 8h).

With `report=true` it continues asynchronously:

```
statistic API ──> att.month-statistic ──> enrichment consumer ──> att.user-statistic-ai-analysis ──> AI service
                 (one event per employee,   (adds up to 6 earlier
                  keyed by user id)          months of history)
```

Before publishing, it asks [Accounting](https://github.com/nikolaev13nik/Attendance-Accounting) for the
names and email address of the requester and the employees, through a client generated from Accounting's
OpenAPI spec that forwards the caller's own JWT. A failed name lookup degrades to ids instead of failing the
report. The enrichment consumer has a dead-letter topic (`att.month-statistic.dlq`), and malformed events go
there without being retried.

## How it works

- **Security.** OAuth2 resource server validating Accounting's JWT; tenant scoping and the
  `@AdminOnly` / `@AdminOrSelf` annotations come from
  [attendance-common-lib](https://github.com/nikolaev13nik/attendance-common-lib).
- **Pipeline.** Each endpoint is a strategy service (`service/strategy`) running
  `fetch → validate → executeBusiness → persist → mapResult` over a `DataTimeContext`; `DataTimeServiceBase`
  owns the transaction and the log line for each call.
- **SQL where it belongs.** Worked and overtime minutes are aggregated in native queries rather than in Java.
- **Schema.** Flyway migrations; Hibernate only validates them.

## Run and test

```bash
./mvnw test                  # API tests on H2, Accounting stubbed with WireMock
./mvnw clean verify -Pci     # what CI runs: tests + JaCoCo, at least 85% line coverage
```

The build needs a GitHub token with `read:packages` under server id `github` in `~/.m2/settings.xml` to
download `attendance-common-lib`. To run the whole system, use the Docker Compose stack in
[attendance-project-common](https://github.com/nikolaev13nik/attendance-project-common).

| Variable | Purpose |
| --- | --- |
| `ATTENDANCE_ACCOUNTING_JWT_SECRET` | the key Accounting signs tokens with |
| `ATTENDANCE_ACCOUNTING_BASE_URL` | where Accounting is reachable |
| `ATT_OVERTIME_DAILY_THRESHOLD` | daily hours before overtime starts, as `H.MM` (default `8.0`) |
| `KAFKA_BOOTSTRAP_SERVERS` | Kafka brokers |
| `SPRING_DATASOURCE_URL` / `_USERNAME` / `_PASSWORD` | PostgreSQL connection |

## Deployment

CI (GitHub Actions) builds and tests, pushes the image to GitHub Container Registry, tags the version and
writes it into [`helm/timetracking`](helm/timetracking), which is deployed to Kubernetes.
