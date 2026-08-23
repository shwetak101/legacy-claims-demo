# Modern Claims Service

Insurance claims processing service — modernized from `legacy-claims-service`.

## What this service does

Handles claim intake, eligibility validation, premium calculation, fraud
scoring, and payout determination for health, motor and life insurance
policies. This is a **service-oriented rewrite** of the legacy Spring Boot 2.3
codebase, retaining every documented business rule and every rule discovered
in the legacy PL/SQL that had never been surfaced in Java.

## Modernization summary

| Concern | Legacy | Modern |
| --- | --- | --- |
| Runtime | Spring Boot 2.3 / Java 8 | Spring Boot 3.2 / Java 21 |
| Logging | Log4j 1.2.17 (CVE-2019-17571) | SLF4J + Logback |
| Data access | JDBC + stored procs (business logic split) | Spring Data JPA + focused `@Procedure` calls |
| Business rules | Split across `ClaimService` and 4 stored procs | Extracted into `PremiumCalculator`, `FraudScorer`, `EligibilityValidator` |
| Config | Hardcoded prod DB password | Environment variables via `application.yml` |
| Security | `WebSecurityConfigurerAdapter` (deprecated), `csrf().disable()`, hardcoded ops/admin creds | `SecurityFilterChain` bean, CSRF for state-changing routes, no in-code credentials |
| SQL injection | Raw concat in `ClaimDAO.findPolicy` | Parameterized JPA queries |
| Vulnerable deps | log4j 1.x, commons-collections 3.2.1, commons-fileupload 1.3.2, dom4j 1.6.1, jackson 2.9.10 | All removed or replaced with current versions |
| Dead code | `LegacyXmlParser`, `PaymentUtil`, `/legacy-format`, `/batch-import`, `getClaimHistoryLegacy` | Removed |
| Tests | `HelloTest.sanity` (1 assertion) | JUnit 5 tests for each extracted domain service |
| Delivery | `java -jar` on VM | Multi-stage Dockerfile, GitHub Actions CI, CodeQL, Dependabot |

## Business rules recovered from PL/SQL

The following rules existed **only** in `SP_CALCULATE_PREMIUM.sql` in the
legacy code — the Java layer was silently missing them:

- **Tobacco loading** — HEALTH: +40%, LIFE: +55%
- **BMI loading** — HEALTH: +30% (>35), +15% (>30), +10% (<18)
- **Group discount** — up to 30% depending on group size
- **War / disturbed-region loading** — from `RISK_STATES` table

All four are now first-class methods on `PremiumCalculator` with tests.

## Build

```
mvn clean verify
```

## Run

```
export DB_URL=jdbc:h2:mem:claims
export DB_USER=sa
export DB_PASSWORD=
export ADMIN_TOKEN=...   # from secret store, never committed

java -jar target/modern-claims-service-2.0.0.jar
```

Or via Docker:

```
docker build -t modern-claims-service:2.0.0 .
docker run -e DB_URL=... -e DB_USER=... -e DB_PASSWORD=... \
           -e ADMIN_TOKEN=... -p 8080:8080 modern-claims-service:2.0.0
```

## Endpoints

- `POST /api/v1/claims` — submit a new claim
- `GET  /api/v1/claims/{id}` — fetch a claim
- `GET  /api/v1/claims/{id}/fraud-score` — retrieve fraud score
- `POST /api/v1/claims/{id}/reprocess` — admin re-run (ADMIN role required)
- `GET  /actuator/health` — liveness/readiness probe

## Design

```
Controller  ─┐
             ├─► ClaimService ──► EligibilityValidator
             │                    PremiumCalculator      ─► Policy/Customer repositories
             │                    FraudScorer
             └─► SecurityFilterChain
```

`ClaimService` is now a thin orchestrator (~150 lines). All rules live
in dedicated domain services with their own tests.

## Testing

```
mvn test               # JUnit 5 tests for all domain services
mvn verify             # includes CodeQL security scan in CI
```

## AI-assisted modernization

This service was produced with GitHub Copilot Agent mode against the
legacy repository, iterated over one working day, and reviewed by a human
engineer before merge. See the migration PR for the full diff and review
comments.
