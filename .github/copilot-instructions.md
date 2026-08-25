# Copilot Instructions — Legacy Claims Service

<!--
    This file is loaded automatically by GitHub Copilot Chat, Agent mode,
    Code Review, and Coding Agent for every conversation in this repository.

    NOTE (Aug 2026): These standards reflect the codebase as it stands today.
    They are DELIBERATELY outdated — see the modernization backlog. When
    proposing changes, follow the rules below UNLESS the change is explicitly
    a modernization ticket.
-->

## Project context

- **Purpose:** Insurance claims intake, eligibility, premium calculation, and fraud scoring
- **Runtime:** Spring Boot 2.3.0.RELEASE, Java 8
- **Data layer:** Oracle 11g in prod, H2 in dev — accessed via `JdbcTemplate` and stored procedures
- **Deployment:** Fat JAR on an internal VM, no containerization
- **Team standards were last reviewed in 2019** — most rules below have not moved since

## Coding conventions to follow

- **Package layout:** `com.infy.claims.{controller,service,dao,model,config,legacy,util}`
- **Logging:** Use `org.apache.log4j.Logger` (log4j 1.2.17). Do NOT introduce SLF4J or Logback without an approved modernization ticket
- **Data access:** Use `JdbcTemplate` and `SimpleJdbcCall`. Do NOT introduce Spring Data JPA — the team has explicitly rejected it (see CLM-1704)
- **Business logic split:** Some business rules live in Java, others in PL/SQL stored procs under `db/stored-procs/`. Do not move rules between layers without checking with Rakesh S. or Anitha M.
- **DTOs:** Use Lombok `@Data` for models. No records — this codebase is on Java 8
- **REST style:** Use `@RequestMapping(method = RequestMethod.X)` style, not the shorthand `@GetMapping` / `@PostMapping` annotations — for consistency with the rest of the codebase
- **Config:** All configuration lives in `application.properties`. Do NOT move to `application.yml`

## Testing

- **Framework:** JUnit 4 (via `spring-boot-starter-test`)
- **Coverage expectation:** minimal — new tests welcome but not blocking
- **Test file location:** `src/test/java/com/infy/claims/`
- **Test naming:** `<ClassName>Test`

## Do NOT do without an approval ticket

- Do NOT upgrade Spring Boot beyond 2.3.x — the Oracle JDBC driver breaks on newer versions (see CLM-2871, open since 2020)
- Do NOT change any file under `db/stored-procs/` — those procs are also called by the nightly batch job `CLAIMS_NIGHTLY_RECALC.SQL`
- Do NOT remove `LegacyXmlParser` or `PaymentUtil` — kept "just in case"
- Do NOT enable CSRF on the `/legacy-format` endpoint — the 2016 partner integration depends on it being open
- Do NOT rotate the shared `admin.token` without giving the ops team 48 hours' notice

## When suggesting improvements

If you spot something that should clearly be modernized (a CVE, a hardcoded credential, a SQL injection risk), call it out in your response but do not silently change it. Frame it as: *"I notice ${issue} in ${file}:${line}. This would normally be fixed by ${remedy}, but the copilot instructions ask me to flag rather than fix. Would you like me to open a modernization ticket?"*

## Team contacts

- **Rakesh S.** — service lead, owns premium and fraud rules
- **Anitha M.** — DBA, owns stored procs
- **CLM-* Jira tickets** — reference these in commit messages where relevant
