# 🛡️ Policy-Driven Approval Agent

### Explainable, Rule-Based Expense Approval System built with Pure Java

A lightweight **policy-driven approval engine** that converts plain-English business rules into executable policies and automatically evaluates expense claims as:

**✅ APPROVE · ❌ REJECT · ⚠️ ESCALATE**

Every decision includes a **traceable rationale** showing exactly which business rule produced the decision.

> **No AI/LLM dependency. No database. No external libraries. Just Java + configurable business rules.**

---

## 🎥 Live Demo

### ▶️ Watch the Project Demo

> The demo shows the approval engine evaluating expense claims, explaining decisions, and applying new business rules without changing Java source code.

**Replace `YOUR_YOUTUBE_VIDEO_URL` with your actual YouTube/Loom video URL.**

---

## 📸 Application Preview

### Approval Dashboard

The project includes a lightweight built-in web interface for demonstrating the rule engine.

![Policy Approval Dashboard](YOUR_SCREENSHOT_URL)

### What the dashboard demonstrates

* 📋 Configurable business rules
* 📊 Expense claim decisions
* ✅ Automatic approvals
* ❌ Policy-based rejections
* ⚠️ Human-review escalations
* 🔎 Exact matched-rule rationale
* 🔄 Live rule updates

> Upload your dashboard screenshot to GitHub and replace `YOUR_SCREENSHOT_URL` with the image path.

---

# 💡 Why This Project?

In real organizations, approval decisions are often controlled by changing business policies.

For example:

> **"Auto-approve expenses under $500 for Sales."**

Instead of hard-coding this logic into Java, this project treats the rule as **configuration**.

A business user can modify:

```text
rules.txt
```

and change approval behavior without modifying the Java source code.

The engine then:

```text
Plain-English Rule
        ↓
    Rule Parser
        ↓
Structured Rule
        ↓
Specificity Matching
        ↓
   Approval Engine
        ↓
Approve / Reject / Escalate
        ↓
Traceable Rationale
```

---

# ✨ Key Features

| Feature                       | Description                                         |
| ----------------------------- | --------------------------------------------------- |
| 📝 Plain-English Rules        | Business policies are written as readable sentences |
| ⚙️ Configuration-Driven       | Change approval behavior without changing Java code |
| 🎯 Specificity-Based Priority | More specific rules override generic rules          |
| 🔍 Explainable Decisions      | Every decision includes the rule that caused it     |
| 🛡️ Fail-Safe Design          | Unmatched claims are escalated instead of guessed   |
| 🔄 Live Rule Updates          | Add rules through the built-in web UI               |
| 💻 Lightweight Web UI         | Run a browser-based dashboard locally               |
| 📦 Zero Dependencies          | No Maven, Gradle, Spring, or external libraries     |
| 📴 Offline Execution          | Fully deterministic local processing                |
| 🧪 Batch Processing           | Evaluates multiple claims from CSV                  |

---

# 🧠 How It Works

## 1️⃣ Business Rules

Rules are stored in:

```text
rules.txt
```

Example:

```text
Auto-approve expenses under $500 for Sales
Auto-approve travel expenses under $300 for any department
Escalate expenses above $2000 regardless of department
Reject expenses above $1000 for Marketing without justification
Auto-approve software expenses under $150 for Engineering
Escalate meals expenses above $200 for any department
Approve expenses under $200 regardless of department
Escalate expenses above $800 for HR
Escalate expenses above $400 for any department
```

---

## 2️⃣ Rule Parsing

`RuleParser.java` converts each plain-English sentence into a structured `Rule` object.

For example:

```text
Auto-approve expenses under $500 for Sales
```

becomes conceptually:

```text
Action       → APPROVE
Amount       → < $500
Department   → Sales
Category     → Any
```

This allows the engine to evaluate rules programmatically while keeping the original human-readable policy.

---

# 🎯 3️⃣ Specificity-Based Rule Priority

A major feature of the project is **specificity-based rule matching**.

Suppose we have:

```text
Escalate expenses above $400 for any department
```

and:

```text
Auto-approve software expenses under $150 for Engineering
```

The second rule is more specific because it constrains:

```text
Amount + Department + Category
```

The engine evaluates more specific rules before broader rules.

This prevents a generic rule from incorrectly overriding a more targeted business policy.

---

# 🛡️ 4️⃣ Fail-Safe Default

If no configured rule matches a claim, the system does **not** guess.

Instead:

```text
ESCALATE
```

The rationale clearly explains that the claim was escalated for human review because no configured policy matched.

This is especially important for financial/compliance workflows where an incorrect automatic approval could be more dangerous than requesting human review.

---

# 🔎 5️⃣ Explainable Decisions

Every decision contains a rationale.

Example:

```text
Decision:
APPROVE

Matched rule:
"Auto-approve expenses under $500 for Sales"

Conditions:
amount < $500
department == Sales

Claim:
amount = $420
department = Sales
category = client_dinner
```

The system therefore provides both:

**Decision + Reason**

instead of returning only:

```text
APPROVE
```

---

# 🖥️ Web Dashboard

The project includes a lightweight Java HTTP server-based web UI.

Run the application and open:

```text
http://localhost:8080
```

The dashboard provides:

### Current Rules

View the policies currently controlling the approval engine.

### Add Rule

Enter a new business rule such as:

```text
Escalate travel expenses above $800 for HR
```

and add it without modifying Java source code.

### Approval Decisions

View:

* Claim ID
* Employee
* Department
* Category
* Amount
* Decision
* Rationale

---

# 📊 Example Results

| Claim | Department  | Category      | Amount | Decision    |
| ----- | ----------- | ------------- | -----: | ----------- |
| C001  | Sales       | client_dinner |   $420 | ✅ APPROVE   |
| C002  | Marketing   | advertising   |  $1500 | ❌ REJECT    |
| C003  | Engineering | software      |   $120 | ✅ APPROVE   |
| C004  | Finance     | travel        |   $275 | ✅ APPROVE   |
| C005  | Sales       | equipment     |  $2500 | ⚠️ ESCALATE |
| C006  | Marketing   | meals         |   $250 | ⚠️ ESCALATE |
| C007  | Operations  | other         |   $180 | ✅ APPROVE   |
| C008  | Engineering | software      |   $300 | ⚠️ ESCALATE |
| C009  | HR          | travel        |   $900 | ⚠️ ESCALATE |
| C010  | Sales       | client_dinner |   $510 | ⚠️ ESCALATE |

---

# 🏗️ Project Architecture

```text
                    ┌──────────────────┐
                    │    rules.txt     │
                    │ Plain-English    │
                    │ Business Rules   │
                    └────────┬─────────┘
                             │
                             ▼
                    ┌──────────────────┐
                    │   RuleParser     │
                    │                  │
                    │ Text → Rule      │
                    └────────┬─────────┘
                             │
                             ▼
                    ┌──────────────────┐
                    │  ApprovalEngine  │
                    │                  │
                    │ Rule Matching    │
                    │ Specificity      │
                    │ Priority         │
                    └────────┬─────────┘
                             │
                 ┌───────────┼───────────┐
                 ▼           ▼           ▼
             APPROVE      REJECT     ESCALATE
                 │           │           │
                 └───────────┼───────────┘
                             ▼
                    ┌──────────────────┐
                    │    Decision      │
                    │                  │
                    │ Decision +       │
                    │ Traceable        │
                    │ Rationale        │
                    └──────────────────┘
```

---

# 📂 Project Structure

```text
policy-approval-agent-java/
│
├── rules.txt
├── claims.csv
├── README.md
│
└── src/
    └── agent/
        ├── Claim.java
        ├── Decision.java
        ├── Rule.java
        ├── RuleParser.java
        ├── ApprovalEngine.java
        ├── Main.java
        └── WebUI.java
```

### Core Components

| File                  | Responsibility                                     |
| --------------------- | -------------------------------------------------- |
| `Claim.java`          | Represents an expense claim                        |
| `Rule.java`           | Represents a parsed business rule                  |
| `Decision.java`       | Stores the final decision and rationale            |
| `RuleParser.java`     | Converts plain-English rules into structured rules |
| `ApprovalEngine.java` | Matches claims against policies                    |
| `Main.java`           | Command-line application entry point               |
| `WebUI.java`          | Lightweight browser-based demonstration UI         |
| `rules.txt`           | Configurable business policy                       |
| `claims.csv`          | Sample expense claims                              |

---

# 🚀 Getting Started

## Prerequisites

* Java JDK 17+ recommended
* IntelliJ IDEA or another Java IDE
* No Maven required
* No Gradle required
* No database required

---

## ▶️ Run with IntelliJ IDEA

### 1. Clone the repository

```bash
git clone YOUR_GITHUB_REPOSITORY_URL
```

### 2. Open the project

Open the cloned folder in IntelliJ IDEA.

### 3. Configure the JDK

Set the project SDK to your installed JDK.

### 4. Run

Run:

```text
Main.java
```

or:

```text
WebUI.java
```

### 5. Open the dashboard

Navigate to:

```text
http://localhost:8080
```

---

# 🧪 Testing the Rule Engine

You can test the system by modifying:

```text
rules.txt
```

For example, add:

```text
Auto-approve equipment expenses under $100 for Operations
```

Then run the application again and verify that matching claims receive the new policy.

No Java source code needs to be changed.

---

# 🔄 Live Policy Update Example

### Before

```text
Escalate travel expenses above $800 for HR
```

A $750 HR travel claim may follow another applicable rule.

### Add a new rule

```text
Auto-approve travel expenses under $800 for HR
```

The engine now has a more specific HR travel policy.

The important point:

> **The approval behavior changes through configuration rather than Java code changes.**

---

# 🔐 Design Principles

### Deterministic

The engine produces reproducible decisions from the same rules and claim data.

### Explainable

Every decision can be traced back to the rule that produced it.

### Configurable

Business policies live outside the Java implementation.

### Fail-Safe

Ambiguous or unmatched claims are escalated for human review.

### Lightweight

The application runs locally without external services.

---

# 🛠️ Tech Stack

**Language**

* Java

**Core Technologies**

* Java Collections
* Regular Expressions
* File I/O
* CSV processing
* Java HTTP Server

**Frontend**

* HTML
* CSS
* JavaScript

**Development**

* IntelliJ IDEA
* Git
* GitHub

**Dependencies**

```text
Zero external dependencies
```

---

# 📈 Future Improvements

Potential extensions include:

* [ ] User authentication and role-based access
* [ ] Database persistence
* [ ] Rule versioning
* [ ] Rule activation/deactivation
* [ ] Audit history
* [ ] Advanced filtering and search
* [ ] REST API
* [ ] Dashboard analytics
* [ ] Export decisions to CSV/PDF
* [ ] Automated unit/integration tests
* [ ] Docker deployment

---

# 🎓 What I Learned

This project helped me strengthen my understanding of:

* Object-oriented Java design
* Rule parsing
* Regular expressions
* Deterministic decision engines
* Priority and specificity algorithms
* File-based configuration
* CSV processing
* HTTP server implementation
* Explainable decision systems
* Fail-safe software design
* Building a complete Java application without frameworks

---

# 👩‍💻 Author

**Chandu**

Computer Science Engineering Student

Interested in:

* Java Development
* Backend Development
* Software Engineering
* AI-powered and rule-based systems

---

## ⭐ If You Found This Project Interesting

Consider giving the repository a ⭐ on GitHub.

The project demonstrates how **plain-English business policies can be transformed into deterministic, explainable automated decisions using pure Java.**

---

### 📹 Demo Video

**Project Demo:** [Watch on YouTube](YOUR_YOUTUBE_VIDEO_URL)

> The demo covers the dashboard, rule evaluation, approval/rejection/escalation decisions, traceable rationales, and live policy updates.
