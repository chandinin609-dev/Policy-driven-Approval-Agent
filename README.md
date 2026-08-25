# Policy-Driven Approval Agent (Java)

An agent that takes **plain-English business rules** as configuration and
applies them to a batch of expense claims, producing an **approve / reject /
escalate** decision for every claim with a **traceable rationale**.

Pure Java, **zero external dependencies** — no Maven/Gradle setup needed,
so it opens and runs in IntelliJ in under a minute.

## Why this design

- **Rules are configuration, not code.** All business logic lives in
  [`rules.txt`](rules.txt) as plain sentences like
  `"Auto-approve expenses under $500 for Sales"`. Changing behavior means
  editing a sentence — never touching Java.
- **Deterministic, not an LLM call at decision time.** `RuleParser.java`
  uses transparent regex/keyword extraction to turn each sentence into a
  structured condition (action, amount comparator, department, category).
  Reproducible and easy to demo offline — important for something that
  approves or rejects money.
- **Specificity-based priority.** Rules constraining more fields (amount +
  department + category) are checked before broader ones, so a narrow
  exception rule always beats a generic catch-all rule.
- **Fail-safe default.** If no rule matches a claim, it is always
  **escalated** for human review — never silently approved or rejected.
- **Full traceability.** Every decision's rationale quotes the exact rule
  text that matched (or explains the default-escalation fallback).

## Project structure

```
policy-approval-agent-java/
├── rules.txt              # <- the configurable rules layer (edit this!)
├── claims.csv              # sample batch of expense claims
└── src/agent/
    ├── Claim.java
    ├── Decision.java
    ├── Rule.java
    ├── RuleParser.java      # plain English -> structured Rule objects
    ├── ApprovalEngine.java   # applies rules to claims, produces Decisions
    ├── Main.java               # CLI entry point
    └── WebUI.java                # tiny built-in web UI for a live demo
```

## Open in IntelliJ IDEA

1. **File → Open** → select the `policy-approval-agent-java` folder.
2. IntelliJ auto-detects it as a Java project from the `src` layout. If
   prompted, pick your installed JDK (17+ recommended; written against 21).
3. Right-click `Main.java` → **Run 'Main.main()'**.

No `pom.xml`, no `build.gradle`, nothing to download — it's plain `.java`
files using only the standard library.

## Run from the command line

```bash
cd policy-approval-agent-java
mkdir out
javac -d out src/agent/*.java
java -cp out agent.Main
```

This prints a decision table and writes `decisions.csv` with every claim's
decision, matched rule, and rationale.

## Run the web UI (best for the demo video)

```bash
java -cp out agent.WebUI
```
Then open **http://localhost:8080** — you'll see current rules, the claims
batch, every decision color-coded (green/red/yellow), and a text box to add
a brand-new plain-English rule live, no restart needed.

## How a non-technical user adds or edits a rule

1. Open `rules.txt` in any text editor (or use the text box at
   `http://localhost:8080`).
2. Add a new line, e.g. `"Escalate travel expenses above $800 for HR"`, or
   edit an existing sentence.
3. Save the file (or click **Add rule** in the web UI).
4. Re-run `Main` (or refresh the web UI page — it re-reads `rules.txt` on
   every request).

No code, no compiling, no developer involved.

The parser understands these patterns out of the box:
- **Actions:** approve / auto-approve, reject / deny, escalate
- **Amounts:** `under $X`, `below $X`, `above $X`, `over $X`,
  `between $X and $Y`
- **Scope:** `for <Department>` / `in <Department>`, or
  `regardless of department` / `any department` for a global rule
- **Category keywords:** travel, meals, software, equipment, client_dinner,
  advertising, other
- **Special case:** `without justification` — matches only claims that lack
  a justification field

## Handling edge cases and ambiguity

- **Conflicting/overlapping rules:** resolved by specificity — the rule
  naming the most fields (amount + department + category) wins over a
  broader rule.
- **No rule matches at all:** the claim is escalated by default, with a
  rationale explaining that no configured rule applied. The system never
  guesses on money decisions.
- **Unparseable action verb in a rule:** the parser defaults that rule's
  action to `escalate` (the safe direction) rather than doing nothing.

## Tech stack

Java 21, standard library only (`java.util.regex` for rule parsing,
`com.sun.net.httpserver` for the web UI). No Maven, no Spring, no external
JARs — this was a deliberate choice so the project has zero setup friction
and nothing that can fail to resolve during a live demo.
