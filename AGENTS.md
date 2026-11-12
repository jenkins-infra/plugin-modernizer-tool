# Plugin Modernizer Tool

This Java 25 Maven project modernizes Jenkins plugins using OpenRewrite. It has two modules: `plugin-modernizer-cli` (command-line interface) and `plugin-modernizer-core` (modernization logic and recipes).

## Quick reference

- Build and run the full test suite: `mvn verify`
- Run tests: `mvn test`
- Development setup and contribution workflow: [docs/CONTRIBUTING.md](docs/CONTRIBUTING.md)
- System architecture: [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md)

## Instructions

- Do not use or invoke Superpowers skills unless the user explicitly asks for them.
- Follow existing patterns in the relevant module and keep changes focused.
