---
mode: 'agent'
description: 'Extract stored-proc business rules into a testable Java class'
---

Read `db/stored-procs/SP_CALCULATE_PREMIUM.sql`. Extract the tobacco loading,
BMI loading, group discount, and war/terror region loading rules into a new
Java class `PremiumCalculator` in the folder `ai-scratch/`. Requirements:

- Package: `com.infy.claims.modern`
- Java 21 style, no Lombok
- Each rule becomes a separate public method that takes typed inputs and
  returns a `double` multiplier (e.g. `tobaccoLoading(policyType, isSmoker)`)
- Add JavaDoc on each method citing the original PL/SQL line range
- Also generate `PremiumCalculatorTest.java` in the same folder with JUnit 5
  tests — one test per rule, covering both branches

Do NOT modify any existing files. Do NOT touch `src/main`. Output only to
`ai-scratch/`.
