---
mode: 'ask'
description: 'Comprehension pass — architecture, hidden rules, risk register'
---

@workspace Analyze this Spring Boot service end-to-end and produce ONE markdown
document with these sections:

1. **Purpose** — one paragraph, what this service actually does.

2. **Component relationships** — a Mermaid `graph TD` showing:
   - REST controllers → services → DAO → Oracle stored procs
   - Which Java class calls which stored procedure
   - Model classes used across boundaries
   - Any dead/unused components (mark with dashed lines)

3. **Hidden business rules** — list every business rule in `ClaimService.java`
   AND every rule in the `.sql` stored procs under `db/stored-procs`. Call out
   any rule that exists in ONE place but not the other (rules the Java team
   likely doesn't know about).

4. **Top 10 risks** — security, dead code, outdated dependencies, hardcoded
   secrets, SQL injection. Each risk cites `file:line`.

5. **Modernization priorities** — top 5 things to fix first, ranked by
   risk vs effort.

Save the entire output to `AI_ANALYSIS.md` in the project root.
