---
name: requirements-analysis
description:
  Analyze the given requirements and clarify ambiguous points through structured Q&A with the developer.
  Once clarification is complete, produce sequence diagrams, class diagrams, and ERDs using Mermaid syntax.
  This process is used to clarify requirements before writing any code.
---

# Requirements Analysis Guide

When analyzing requirements, **you must follow the flow below.**

---

## 1️⃣ Do Not Trust Requirements at Face Value — Restate as a Problem Situation

- Do not stop at rephrasing requirement sentences.
- Do not ask “What are we building?”
- Instead ask: **“What problem currently exists, and why are we solving it?”**

Separate analysis into:

- **User perspective**
- **Business perspective**
- **System perspective**

> Example  
> “Cancel payment if order fails” →  
> “This is about maintaining consistency so that payment success/failure and order status do not become misaligned.”

---

## 2️⃣ Expose Ambiguity — Do Not Hide It

- Do not assume or silently decide missing details.
- Explicitly list what has not been decided.

You must include the following types of questions:

### 🔹 Policy Questions
- At what timing?
- What defines success or failure?
- How are exceptions handled?

### 🔹 Boundary Questions
- What is considered one responsibility?
- Where should separation occur?

### 🔹 Expansion Questions
- Is this likely to change in the future?

---

## 3️⃣ Ask Questions in a Developer-Friendly Way

- Prioritize questions (most critical first).
- If choices exist, provide **options with trade-offs**.

**Example Format:**

- **Option A:** Single transaction  
  → Simpler implementation, lower scalability

- **Option B:** Step-by-step separation  
  → More complex structure, better extensibility and compensation handling

---

## 4️⃣ Define the Conceptual Model First

Do not jump into code or technical implementation.

First define:

- **Actors** (users, external systems)
- **Core domains**
- **Supporting/external systems**

This stage aligns **design thinking**, not implementation.

---

## 5️⃣ Always Present Diagrams as: Reason → Diagram → Interpretation

Before drawing any diagram, explain:

- Why this diagram is necessary
- What we are trying to validate

All diagrams must use **Mermaid syntax**.

### 📌 Sequence Diagram
Used for:
- Responsibility separation
- Call order
- Transaction boundary validation

### 📌 Class Diagram
Used for:
- Domain responsibilities
- Dependency direction
- Cohesion verification

### 📌 ERD
Used for:
- Persistence structure
- Ownership of relationships
- Normalization considerations

---

## 6️⃣ Explain How to Read the Diagram

Do not drop diagrams without explanation.

After each diagram:

- Highlight 2–3 key focus points
- Clarify the design intent

---

## 7️⃣ Always Mention Design Risks

Do not hide structural risks:

- Transaction bloat
- Increased domain coupling
- Wider impact when policies change

Do not present solutions as absolute answers.  
Provide alternatives when possible.

---

# Tone & Style Guide

- Maintain a **design review tone**, not a lecture tone.
- Avoid presenting a single “correct” answer.
- Emphasize **intent, responsibility, and boundaries** over code.
- Focus on surfacing what must be thought through before implementation.