# Claims Service

Java service for the claims team.

## Overview

This service handles insurance claims. It talks to the Oracle DB and returns claim
data to the web front-end. Also has an admin endpoint for the ops team.

## Build

```
mvn clean install
```

## Run

```
java -jar target/legacy-claims-service.jar
```

Requires Java 8. **Do not use Java 11 — the Oracle driver fails silently.**

## Endpoints

- `POST /claims` — submit a new claim
- `GET /claims/{id}` — fetch a claim
- `GET /admin/health` — for the ops team

*(there are a few more, ask Rakesh)*

## Database

We use Oracle 11g in prod and H2 in dev. The stored procs live under
`db/stored-procs/`. **Please don't change them without talking to the DBA team**,
some of them have logic that isn't reflected anywhere else.

## Config

`application.properties` has the DB connection details. The prod values are
committed for now — we'll move them to Vault "next quarter" (see CLM-2103).

## Team

- **Rakesh S.** — lead, knows the premium calc rules by heart
- **Anitha M.** — DB / stored procs
- **Vikram P.** — front-end integration (moved to another project Aug 2020,
  reach out to Rakesh instead)
- **New joiner** — TBD, position open since Q3 2021

## Recent changes

- v1.7.3 (Mar 2019) — added fraud scoring flag (Rakesh)
- v1.7.2 (Jan 2019) — bumped log4j — see CLM-1988
- v1.7.1 (Nov 2018) — Oracle driver update

## Known issues

- H2 dev DB doesn't support all the PL/SQL, some tests are `@Ignore`d
- `LegacyXmlParser` is no longer used but kept "just in case"
- Startup logs a couple of deprecation warnings — safe to ignore

---

*Last updated: 12-Mar-2019 by Rakesh S.*
