---
mode: 'agent'
description: 'Migrate the service off Log4j 1.x onto SLF4J + Logback'
---

The `pom.xml` uses log4j 1.2.17 which has known CVEs
(CVE-2019-17571, CVE-2022-23305). Migrate the service off log4j 1.x
onto SLF4J + Logback (Spring Boot's default logging backend).

Requirements:
- Update `pom.xml`: remove the `log4j` 1.2.17 dependency and un-exclude
  `spring-boot-starter-logging` so the Spring Boot default (Logback via
  SLF4J) is used.
- In every Java file that imports `org.apache.log4j.Logger`, replace with
  `org.slf4j.Logger` + `org.slf4j.LoggerFactory`. Update the `Logger.getLogger`
  call to `LoggerFactory.getLogger`.
- Remove `src/main/resources/log4j.properties`.
- Comment out the `logging.config` line in `application.properties`.

Do NOT change any business logic. Do NOT touch stored procs or DAO SQL.
Show me the diff before applying.

---

**Fallback prompt (tighter scope) — only use if the response above is too broad:**

Please redo that with a tighter scope. Only modify:
- `pom.xml` (remove log4j 1.2.17 dep, remove starter-logging exclusion,
  drop the `log4j.version` property)
- Every `.java` file that imports `org.apache.log4j.Logger` — swap the
  import for `org.slf4j.Logger` + `LoggerFactory` and change `getLogger()`.
- `application.properties` (comment out `logging.config` line)
- Delete `log4j.properties`

Do NOT touch any file under `db/`, `model/`, or `config/`. No business logic changes.
