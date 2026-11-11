# Plugin Modernizer Tool

This repository builds a CLI and core library for modernizing Jenkins plugins with OpenRewrite.

## Build and test

- Requires Java 25 and Maven 3.9.14 or later.
- `mvn clean install` builds the modules and CLI JAR.
- `mvn test` runs unit tests; `mvn verify` also runs integration tests.

## Project structure

- `plugin-modernizer-cli`: Picocli commands and CLI options.
- `plugin-modernizer-core`: modernization logic, GitHub operations, and OpenRewrite recipes.
- [Architecture guide](docs/ARCHITECTURE.md): process and component overview.
- [Development guide](docs/CONTRIBUTING.md): local setup and contribution steps.

When documenting CLI usage, check the Picocli command and option definitions under `plugin-modernizer-cli/src/main/java/io/jenkins/tools/pluginmodernizer/cli/`; the command's `--help` output is the live option reference.
