# Plugin Modernizer Tool

Plugin Modernizer applies [OpenRewrite](https://docs.openrewrite.org/) recipes to Jenkins plugin repositories. It can preview changes, validate them, and open pull requests. It also collects plugin metadata, including dependencies and build JDK versions.

## Install

The CLI is available through Homebrew, Scoop, and Nix, or from the [GitHub releases](https://github.com/jenkins-infra/plugin-modernizer-tool/releases).

**Homebrew**

```shell
brew tap jenkins-infra/tap
brew install plugin-modernizer
```

**Scoop**

```powershell
scoop bucket add jenkins-infra https://github.com/jenkins-infra/scoop-bucket.git
scoop install plugin-modernizer
```

**Nix**

```shell
nix profile install github:jenkins-infra/plugin-modernizer-tool
```

The tool needs Java 25 and Maven 3.9.14 or later to run recipes against plugin repositories.

## Try a dry run

Set a GitHub token and the account or organization that owns your forks:

```shell
export GH_TOKEN=your_github_token
export GH_OWNER=your_github_username
```

`GITHUB_TOKEN` and `GITHUB_OWNER` are accepted aliases. For `run`, the credentials need access to the source repositories and permission to create forks and pull requests. GitHub App authentication is also supported; see `plugin-modernizer run --help` for its settings.

List available recipes and preview a change:

```shell
plugin-modernizer recipes
plugin-modernizer dry-run --plugins git,git-client --recipe AddPluginsBom
```

`dry-run` does not push changes or open pull requests. Use `plugin-modernizer --help` or `plugin-modernizer dry-run --help` for the current options. Plugin lists can also come from a file; see [`docs/example-plugins.txt`](docs/example-plugins.txt).

> [!WARNING]
> The `run` command can open pull requests. `--clean-forks` can delete forked repositories, so use it only when you are sure those forks contain no work you need.

## Build from source

Build the CLI JAR with Maven:

```shell
mvn clean install
```

The JAR is written to `plugin-modernizer-cli/target/jenkins-plugin-modernizer-999999-SNAPSHOT.jar`.

Run the test suite with:

```shell
mvn test
```

`mvn verify` also runs the integration tests.

## Project layout

- `plugin-modernizer-cli` contains the Picocli command-line interface.
- `plugin-modernizer-core` contains the modernization logic, GitHub integration, and OpenRewrite recipes.

See the [architecture guide](docs/ARCHITECTURE.md) for the workflow and [development guide](docs/CONTRIBUTING.md) for local setup and contribution steps.

## Related projects

- [Plugin metadata repository](https://github.com/jenkins-infra/metadata-plugin-modernizer/)
- [Jenkins OpenRewrite recipes](https://docs.openrewrite.org/recipes/jenkins/)
- [Jenkins Plugin Modernizer project page](https://www.jenkins.io/projects/gsoc/2025/projects/plugin-modernizer-improvements/)
