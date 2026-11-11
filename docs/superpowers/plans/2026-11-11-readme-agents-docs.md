# README and AGENTS.md Documentation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace the long, inconsistent README with a concise onboarding page and add a minimal, accurate `AGENTS.md`.

**Architecture:** Keep project onboarding in the root README and project-specific agent guidance in the root `AGENTS.md`. Link to the existing architecture and contributor docs rather than copying their material.

**Tech Stack:** Markdown, Maven, Java 25, Picocli CLI.

**Spec:** `docs/superpowers/specs/2026-11-11-readme-agents-docs-design.md`

## Global Constraints

- Keep the product-documentation changes to `README.md` and `AGENTS.md`.
- State Java 25 and Maven 3.9.14 or later.
- Recommend `dry-run` for the first modernization example; do not make a PR-opening run the first example.
- Keep `AGENTS.md` under 50 lines and limited to project-specific instructions.
- Use natural, specific language and avoid generic promotional copy.

## Review Focus

- README command examples must match the current CLI command and option names.
- README prerequisites must match the configured compiler release and Maven minimum.
- Root-relative links in both documents must resolve to tracked files.
- The first modernization command must use `dry-run` and avoid opening pull requests.
- `AGENTS.md` must remain concise and avoid duplicating generic coding advice.

---

### Task 1: Rewrite project documentation

**Files:**
- Modify: `README.md`
- Create: `AGENTS.md`

**Interfaces:**
- Consumes: CLI commands from `plugin-modernizer-cli/src/main/java/io/jenkins/tools/pluginmodernizer/cli/` and project requirements from `pom.xml` and `Settings.java`.
- Produces: Root documentation with working links, accurate setup/build commands, safe first-run example, and minimal agent instructions.

- [ ] Rewrite `README.md` as a concise project landing page. Cover what the tool does, its modules, Java/Maven prerequisites, install choices verified in the repository, GitHub setup needed for `run`, a dry-run example, build/test commands, and links to architecture, contribution, and CLI `--help` guidance. Preserve applicable useful project links without keeping a long option table.
- [ ] Create `AGENTS.md` with a one-sentence project description, verified quick commands, a short module map, and links to existing project instructions. Keep it below 50 lines; add no generic style or workflow rules.
- [ ] Check Markdown links and command references. Confirm every local README/AGENTS link targets an existing file, inspect README examples against the CLI source, confirm the dry-run example uses a current recipe, and confirm the prerequisites match `pom.xml` and `Settings.java`.
- [ ] Review the final copy for stale references, repeated material, and unnatural or promotional phrasing.

Run: `git diff --check` and a short link check for root-relative Markdown links.
Expected: no whitespace errors, no broken local links, and all documented commands/options match the inspected source.
