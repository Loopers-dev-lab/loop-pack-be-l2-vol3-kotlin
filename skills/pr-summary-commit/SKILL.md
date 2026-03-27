---
name: pr-summary-commit
description: Create pull request summaries and commit messages from git diffs and recent commits. Use when the user asks to summarize changes for a PR, write commit messages, or format output into structured sections such as Summary, Context & Decision, Design Overview, and Flow Diagram.
---

# PR Summary + Commit Message

Inspect recent changes and produce:
- PR body in the team template
- concise commit message candidates

## Workflow

1. Collect source material.
   - Read git diff (`git diff`, `git diff --staged`)
   - Read recent commits (`git log --oneline -n 20`)
   - If needed, inspect touched files for intent and impact
2. Extract key facts.
   - Background/problem, target outcome, actual result
   - Scope, major components, design decisions, trade-offs
   - Risks/follow-ups and test evidence
3. Fill the PR template.
   - Use the exact headings and order from `references/pr-template.md`
   - Keep each bullet concrete and verifiable from diff/commits
4. Generate commit messages.
   - Propose 3-7 messages
   - Prefer Conventional Commits (`feat:`, `fix:`, `refactor:`, `test:`, `docs:`, `chore:`)
   - Keep subject line <= 72 chars

## Output Rules

- Do not invent behavior not present in the diff.
- Mark uncertain points as assumptions.
- Prefer short bullets over paragraphs.
- If flow is unclear, provide a minimal Mermaid diagram with only confirmed steps.
- If no diagram evidence exists, leave a TODO note under Flow section.

## Template

Use `references/pr-template.md` as the base.

