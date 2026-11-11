# README and AGENTS.md design

## Intent

Give Jenkins plugin maintainers a concise, accurate entry point for installing and safely trying Plugin Modernizer, while giving coding agents only the repository-specific instructions they need.

## Goals

- Rewrite the root README as a focused landing page with installation, prerequisites, a dry-run quick start, build instructions, and links to the existing architecture and contributor guides.
- Create a short root `AGENTS.md` with project description, verified build/test commands, and links to relevant project documentation.
- Verify instructions against the Maven configuration and CLI source. Use direct, natural language.

## Non-goals

- Add CLI features or change implementation behavior.
- Add separate instruction files or duplicate the full CLI reference in the README.
- Rewrite `docs/ARCHITECTURE.md` or `docs/CONTRIBUTING.md`.

## Approaches

1. Keep the README as a full command and environment reference. This retains detail but increases duplication and drift risk.
2. Use a concise README for onboarding and link to existing guides; point readers to CLI `--help` for the live option reference. Keep `AGENTS.md` under 50 lines with only durable project-specific guidance. **Selected:** this is easier to maintain and matches the project's existing documentation structure.

## Constraints and decisions

- Keep the changes limited to the README and new `AGENTS.md` for product documentation.
- Document Java 25 and Maven 3.9.14 or later based on the configured compiler release and `Settings.MAVEN_MINIMAL_VERSION`.
- Use a dry-run example so the first suggested execution does not open pull requests.
- Preserve useful project links and installation channels when verifiable; remove duplicated, stale, and dedicated-file sections such as the README's contributing section.
