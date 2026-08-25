# Copilot Instructions — Modern Claims Service

<!--
    This file is loaded automatically by GitHub Copilot Chat, Agent mode,
    Code Review, and Coding Agent for every conversation in this repository.
    It reflects the current architectural and security standards for the
    modernization programme.
-->

## Project context

- **Purpose:** Insurance claims intake, eligibility, premium calculation, and fraud scoring
- **Runtime:** Spring Boot 3.2.x, Java 21
- **Data layer:** Spring Data JPA (H2 for dev/test, Oracle for production via profile-scoped `application.yml`)
- **Deployment:** Multi-stage Dockerfile → distroless base → non-root user; deployed via GitHub Actions
- **Origin:** modernized from `legacy-claims-service`. See the migration PR for a full diff and rationale for every architectural choice

## Coding conventions

- **Package layout:** `com.infy.claims.{controller,service,repository,model,config,exception}`
- **Logging:** SLF4J façade with Logback backend. Use `LoggerFactory.getLogger(YourClass.class)`. Never `System.out.println`
- **Structured logs:** Use `log.info("event={} customer={} amount={}", ...)` — key-value form so Log Analytics can index it
- **Data access:** Spring Data JPA repositories. Native queries only when JPQL cannot express the intent, and always with `@Query` + parameterization
- **Stored procs:** Only via `@Procedure` on repository methods. Never build SQL by string concatenation. Ever
- **DTOs & value types:** Prefer Java 21 `record` for immutable value types. Reserve classes for JPA entities and Spring-managed beans
- **REST style:** Use `@GetMapping` / `@PostMapping` / `@PutMapping` / `@DeleteMapping`. Never the legacy `@RequestMapping(method = ...)` form
- **Config:** All configuration in `application.yml`. Every secret via environment variable — never a plaintext value in the file
- **Security:** Use the `SecurityFilterChain` bean pattern. Never subclass `WebSecurityConfigurerAdapter` (deprecated in Spring Security 5.7+)
- **Exceptions:** Use `@RestControllerAdvice` for global exception handling. Do not throw raw `Exception` from controllers

## Testing (non-negotiable)

- **Framework:** JUnit 5 + AssertJ
- **Coverage expectation:** ≥ 80% line coverage on every service-tier package; new business logic must ship with tests
- **Naming:** Test methods use snake-case, e.g. `rejects_expired_policy_claim()`
- **Use `@DisplayName`** on non-obvious tests so surefire reports read cleanly
- **No `@Ignore`** — either fix the test or delete it with a Jira ticket in the commit body

## Security & governance

- All PRs to `modern` must pass:
    - `build` (compile + tests)
    - CodeQL security scan
    - Dependabot audit (no HIGH/CRITICAL alerts on modified dependencies)
    - Human review (branch protection enforced)
- Never commit a secret. Push protection will reject it. Do not attempt to bypass.
- Any dependency added must have an active maintainer (last release within 12 months)

## When suggesting changes

- Prefer small, focused PRs with one behavioural intent per PR
- Reference an issue in the PR body (`Closes #123`)
- Include a "Testing" section in the PR body explaining what was verified
- If a change touches security-sensitive files (`SecurityConfig`, anything under `config/`, or `pom.xml`), tag `@security-team` for early review

## Team contacts

- **Modernization lead:** claims-modernization@example.com
- **Security review:** #security-modernization on Teams
