<div align="center">
<h1>Plugin Modernizer Tool</h1>
<h3>
Using OpenRewrite Recipes for Plugin Modernization or Automation Plugin Build Metadata Updates
</h3>
</div>

[![Build Status](https://ci.jenkins.io/job/Tools/job/plugin-modernizer-tool/job/main/badge/icon)](https://ci.jenkins.io/job/Tools/job/plugin-modernizer-tool/job/main/)
[![Coverage](https://ci.jenkins.io/job/Tools/job/plugin-modernizer-tool/job/main/badge/icon?status=${instructionCoverage}&subject=coverage&color=${colorInstructionCoverage})](https://ci.jenkins.io/job/Tools/job/plugin-modernizer-tool/job/main)
[![LOC](https://ci.jenkins.io/job/Tools/job/plugin-modernizer-tool/job/main/badge/icon?job=test&status=${lineOfCode}&subject=line%20of%20code&color=blue)](https://ci.jenkins.io/job/Tools/job/plugin-modernizer-tool/job/main)
[![GitHub license](https://img.shields.io/github/license/jenkins-infra/plugin-modernizer-tool)](https://github.com/jenkins-infra/plugin-modernizer-tool/blob/main/LICENSE)

> [!Note]
> This tool is currently in development and looking for contributors and early adopters. Please report any issues or feature requests on the GitHub repository.
>
> It's configuration and APIs might change in future releases

## About

Plugin Modernizer is a generic CLI tool designed to automate the modernization of Jenkins plugins. It utilizes OpenRewrite recipes to apply transformations to the plugin, validating the applied transformations and creating pull requests with the results.

The CLI is also used to collect metadata from Jenkins plugins, such as the plugin's dependencies (including transitive) or JDKs used for building the plugin. Such metadata is planned to be integrated with existing Jenkins tooling such as 

- [Jenkins Plugin Site](https://plugins.jenkins.io/)
- [Jenkins statistics](https://stats.jenkins.io/)
- [Plugin Health Scoring](https://github.com/jenkins-infra/plugin-health-scoring)

Learn more at [this project page](https://www.jenkins.io/projects/gsoc/2024/projects/using-openrewrite-recipes-for-plugin-modernization-or-automation-plugin-build-metadata-updates/).

## Usage

- [Getting Started](#getting-started)
    - [Requirements to build](#requirements)
    - [Build](#build)
    - [Using the tool](#using-the-tool)
    - [Setup](#setup)
    - [Configuring SSH Key](#Configuring-SSH-Key-for-Commit-Signing)
- [Subcommands](#subcommands)
- [Global option](#global-option)
- [GitHub option](#github-options)
- [Run option](#run-option)
- [Plugin Input Format](#plugin-input-format)
    - [Plugin option](#plugin-option)
    - [Plugin file option](#plugin-file-option)
- [Configuring Environmental Variables](#configuring-environmental-variables)
- [Examples](#examples)
    - [without dry-run](#without-dry-run)
    - [with dry-run](#with-dry-run)
- [Running with Docker](#running-with-docker)
- [Running with GitPod](#running-with-gitpod)
- [Reproducibility](#reproducibility)
- [Contributing](#contributing)
- [Debugging recipes](#how-to-debug-recipes)

## Getting Started

### Requirements
- Maven version 3.9.7 or later, or mvnd
- Java 17 or Java 21 ([Eclipse Temurin](https://adoptium.net/temurin/releases) recommended)

### Build

```shell
mvn clean install
```

### Using the tool


The CLI is distributed by Homebrew and Scoop and it can be installed using the following command

1) Using Homebrew

Install Homebrew if you don't have it already:

```
/bin/bash -c "$(curl -fsSL https://raw.githubusercontent.com/Homebrew/install/HEAD/install.sh)"
```

Ensure to have Jenkins infra tap

```
brew tap jenkins-infra/tap
```

Then install the plugin-modernizer-tool

```
brew install plugin-modernizer
```
2) Using Scoop

Install [Scoop](https://github.com/ScoopInstaller/Install) if you don't have it already:

```
irm get.scoop.sh | iex
```

Ensure to Add this Bucket to Scoop

```
scoop bucket add jenkins-infra https://github.com/jenkins-infra/scoop-bucket.git
```

Then install the plugin-modernizer-tool

```
scoop install plugin-modernizer
```

### Setup
This tool requires forking repositories from GitHub, so you need to set the GitHub token and GitHub owner as environment variables.
Use either `GH_TOKEN` or `GITHUB_TOKEN` for the GitHub token, and either `GH_OWNER` or `GITHUB_OWNER` for the GitHub owner.
Alternatively, you can pass the GitHub owner through the CLI option `-g` or `--github-owner`.

Generate a GitHub Token:

Go to GitHub Settings > Developer settings > Personal access tokens.
Click on "Generate new token".
Give your token a descriptive name.

Your classic token should have the following scopes:

- `repo` (Full control of private repositories)
- `delete_repo` (Delete repositories) (Only if using the `--clean-forks` option)

Set Environment Variables:

Open your terminal and set the GH_TOKEN and GH_OWNER environment variables:
    
```shell
export GH_TOKEN=your_generated_token
export GH_OWNER=your_github_username
```

> [!Note]
> The GitHub owner can be either a personal account or an organization.
>

The CLI also support GitHub app installation for CI scenario.

The app must be installed on the owner's and target account and a private key generated and downloaded in PEM format.

From there you need to save both ID of installation (found on URL)

`https://github.com/organizations/<org>/settings/installations/<installation ID>`

### Configuring SSH Key for Commit Signing
To ensure the commits are signed with your SSH key and marked as `Verified` on GitHub, run the commands inside your repository directory.

Generate a SSH key pair if you don't already have one.
```shell
ssh-keygen -t ed25519 -C "your_email@example.com"
```

Add the public SSH key to your GitHub account.
- Go to GitHub Settings > SSH and GPG keys > New SSH key.
- Give your key a title and paste the contents of your public key file (usually `~/.ssh/id_ed25519.pub`).

Configure Git to use SSH for signing.
```shell
git config --global gpg.format ssh
git config --global user.signingkey ~/.ssh/id_ed25519.pub
```
Replace `id_ed25519.pub` with your actual public key filename.

Set up commit signing globally.
```shell
git config --global commit.gpgsign true
```
If you prefer to configure signing for individual repositories.
```shell
git config user.signingkey ~/.ssh/id_ed25519.pub
git config gpg.format ssh
git config commit.gpgsign true
```

# Subcommands

- `validate`: Validate the configuration and environment variables (work in progress)
- `run`: Run the modernization process
- `dry-run`: Run the modernization process in dry-run mode without forking or pushing changes
- `build-metadata / fetch-metadata`: Collect metadata for the given plugin and have them on the local cache
- `recipes`: List available recipes

# Auto completion

```shell
source <(plugin-modernizer generate-completion)
```

## Global option

- `--debug`: (optional) Enables debug mode. Defaults to false.

- `--allow-deprecated-plugins`: (optional) **Allow modernization of deprecated plugins.**  
  _Use with caution: This flag enables modernization and metadata collection for plugins that are marked as deprecated. Intended for new maintainers or development environments only. By default, deprecated plugins are blocked from modernization for safety._

- `--cache-path`: (optional) Custom path to the cache directory. Defaults to `${user.home}/.cache/jenkins-plugin-modernizer-cli`.


- `--maven-home`: (optional) Path to the Maven home directory. Required if both `MAVEN_HOME` and `M2_HOME` environment variables are not set. The minimum required version is 3.9.7.


- `--clean-local-data` (optional) Deletes the local plugin directory before running the tool.


- `--version` or `-v`: (optional) Displays the version of the Plugin Modernizer tool.


- `--help` or `-h`: (optional) Displays the help

## GitHub options

- `--github-owner` or `-g`: (Mandatory or optional if using GitHub app authentication) GitHub owner for forked repositories. Can also be set via the environment variable `GH_OWNER` or `GITHUB_OWNER`. Default to owner of the `GH_TOKEN`.


- `--github-app-id <app-id>`: (optional) The GitHub application if using app authentication. Defaults to `GH_APP_ID` environment variable if set.


- `--github-app-source-installation-id <installation-id>`: (optional) The GitHub app installation id for forked repositories. Defaults `GH_APP_SOURCE_INSTALLATION_ID` environment variable if set.


- `--github-app-target-installation-id <installation-id>`: (optional) The GitHub app installation id for repositories. Defaults `GH_APP_TARGET_INSTALLATION_ID` environment variable if set.- `--github-app-private-key <path-to-private-key>`: (optional)  or `GH_APP_PRIVATE_KEY` environment variable

# Run option

- `--plugins`: (optional) Name(s) of plugin directory cloned inside the `test-plugins` directory.


- `--plugin-file`: (optional) Path to the text file that contains a list of plugins. (see example [plugin file](docs/example-plugins.txt))


- `--plugin-path`: (optional) Path to the plugin directory containing the pom.xml file.


- `--recipe` or `-r`: (required) Name of recipe to apply to the plugins.


- `--skip-metadata` (optional) Skip collection and pushing the modernization metadata (i.e metadata after applying the recipes) to the metadata repository. Beneficial for testing or development purpose when we don't need to unnecessary add another step of collecting the metadata.


- `--clean-forks` (optional) Remove forked repositories before and after the modernization process. Might cause data loss if you have other changes pushed on those forks. Forks with open pull request targeting original repo are not removed to prevent closing unmerged pull requests.


- `--jenkins-update-center`: (optional) Sets main update center; will override JENKINS_UC environment variable. If not set via CLI option or environment variable, will default https://updates.jenkins.io/current/update-center.actual.json

- `--jenkins-plugin-info`: (optional) Set the URL for the Jenkins Plugin Info API. If not set via CLI option or environment variable, will default to https://updates.jenkins.io/current/plugin-versions.json

- `--jenkins-plugins-stats-installations-url` (optional) Set the URL for the Jenkins Plugins Stats installations API. If not set via CLI option or environment variable, will default to url in [properties file](plugin-modernizer-core/src/main/resources/urls.properties)


- `--plugin-health-score-url` (optional) Set the URL for the Plugin Health Score API. If not set via CLI option or environment variable, will default to https://plugin-health.jenkins.io/api/scores


- `--github-api-url` (optional) Set the URL for the GitHub API. If not set via CLI option or environment variable, will default to `https://api.github.com`. Automatically set if `GH_HOST` environment variable is set.

## Plugin Input Format

Plugins can be passed to the CLI tool in two ways:
- Plugin option
- Plugin file option

### Plugin option

Pass the plugin names directly using the `-p` or `--plugins option`. The expected input format for plugins is `artifact ID`.

```shell
plugin-modernizer --plugins git,git-client,jobcacher --recipe AddPluginsBom
```
Here, `git`, `git-client`, and `jobcacher` are plugin artifact IDs (also known as plugin names), while `AddPluginsBom` and `AddCodeOwners` are recipe names. For more details about available recipes, refer to the [recipe_data.yaml](plugin-modernizer-core/src/main/resources/recipe_data.yaml) file.

### Plugin file option

Pass the path to a file that contains plugin names. The expected input format for plugins in the .txt file is `artifact ID` or `artifact ID:version`.
See example [plugin file](docs/example-plugins.txt)

```shell
plugin-modernizer run --plugin-file path/to/plugin-file --recipe AddPluginsBom
```

## Configuring Environmental Variables
- `GITHUB_TOKEN` or `GH_TOKEN`: (required) GitHub Token.

- `GITHUB_OWNER` or `GH_OWNER`: (required) GitHub username or organization name. Can also be passed through the CLI option `-g` or `--github-owner`.

- `JENKINS_UC`: (optional) Update Center URL. Can also be passed through the CLI option `--jenkins-update-center`.

- `MAVEN_HOME` or `M2_HOME`: (required) Path to Maven home directory. Can also be passed through the CLI option `-m` or `--maven-home`.

- `CACHE_DIR`: (optional) Path to cache directory. Can also be passed through the CLI option `-c` or `--cache-path`.

## Examples

### without dry-run

```shell
plugin-modernizer run --plugins git,git-client,jobcacher --recipe AddPluginsBom
```
The above command creates pull requests in the respective remote repositories after applying the changes.

### modernizing a deprecated plugin (for new maintainers)

```shell
plugin-modernizer run --plugins bmc-change-manager-imstm --recipe AddPluginsBom --allow-deprecated-plugins
```
This command will allow modernization and metadata collection for the deprecated plugin `bmc-change-manager-imstm`.  
**Warning:** Only use `--allow-deprecated-plugins` if you are a maintainer or have a valid reason to modernize a deprecated plugin.

### with dry-run

```shell
plugin-modernizer dry-run --plugins git,git-client,jobcacher --recipe AddPluginsBom
```

The above command generates patch files instead of applying changes directly. These patch files are saved in `/target/rewrite/rewrite.patch` inside each plugin directory. No pull requests will be created.

> [!Note]
> Enable dry-run to avoid opening pull requests in the remote repositories.

## Running with Docker

You can use the Docker image supplied by this project to run the Plugin Modernizer Tool without needing to install Java or Maven on your local machine.

### Prerequisites

- Docker installed on your machine.
- A GitHub token with the necessary scopes.
- A file named `plugins.txt` containing the list of plugins.

Of course, you don't need a `plugins.txt` file if you are using the `--plugins` option.

### Example
Below is an example of how to use the Docker image with a local `plugins.txt` file.

```shell
docker run \
  -e GH_TOKEN=${GH_TOKEN} \
  -e GH_OWNER=${GH_OWNER} \
  -v $(pwd)/plugins.txt:/plugins.txt \
  ghcr.io/jenkins-infra/plugin-modernizer-tool:main \
  --plugin-file /plugins.txt --recipe AddCodeOwner
```

### Explanation

- `-e GH_TOKEN=${GH_TOKEN}`: Passes the GitHub token as an environment variable.
- `-e GH_OWNER=${GH_OWNER}`: Passes the GitHub owner as an environment variable.
- `-v $(pwd)/plugins.txt:/plugins.txt`: Mounts the plugins.txt file from the current directory to the Docker container.
- `ghcr.io/jenkins-infra/plugin-modernizer-tool:main`: Specifies the Docker image to use.
- `--plugin-file /plugins.txt`: Specifies the path to the plugin file inside the Docker container.
- `--recipe AddPluginsBom,`: Specifies the recipe to apply.

This command will run the Plugin Modernizer Tool inside the Docker container using the specified environment variables and plugin file.

## Running with GitPod

You can try this tool directly in your browser if you have a GitPod account connected to GitHub.  
If you already have the [GitPod extension](https://www.gitpod.io/docs/configure/user-settings/browser-extension) installed in your browser, you can open a workspace for this project by clicking the big green "Open" button next to "Code."  
If you haven’t installed the GitPod extension yet, you can access a workspace by clicking [this link](https://gitpod.io/?autostart=true#https://github.com/jenkins-infra/plugin-modernizer-tool).  

Please note that the Plugin Modernizer Tool has not yet been installed in the workspace as a standalone binary. However, you can still use it by running the following command:  
`java -jar plugin-modernizer-cli/target/jenkins-plugin-modernizer-999999-SNAPSHOT.jar [...]`

## Reproducibility

The maven build should be reproducible

See

- https://maven.apache.org/guides/mini/guide-reproducible-builds.html
- https://reproducible-builds.org
- https://github.com/jenkinsci/incrementals-tools/issues/103 for the support on the Jenkins incrementals tools and [JEP-229](https://github.com/jenkinsci/jep/blob/master/jep/229/README.adoc)

[Reproducible Builds](https://reproducible-builds.org/) for more information.

Ensure the repository is clean before running the following commands (otherwise you can pass the `-Dignore.dirty` flag to the maven command).

```shell
mvn -Dset.changelist clean install
mvn -Dset.changelist -Dreference.repo=central clean verify artifact:compare
```

The property `project.build.outputTimestamp` will be set with the timestamp of the latest commit.

If you are using a mirror for `central` you should adapt the `reference.repo` property accordingly to match the id of the mirror in your `settings.xml`.

## Contributing

Thanks to all our contributors! Check out our [CONTRIBUTING](docs/CONTRIBUTING.md) file to learn how to get started.

## How to debug recipes

Update the `rewrite.yml` into the src/test/resources/<plugin>

Example

```yaml
---
type: specs.openrewrite.org/v1beta/recipe
name: io.jenkins.tools.pluginmodernizer.Debug
displayName: Debug recipe
description: Debug recipe
conditions: []
recipeList: []
```

Then run openrewrite with the following command 

```shell
mvn org.openrewrite.maven:rewrite-maven-plugin:dryRun -Drewrite.recipeArtifactCoordinates=org.openrewrite.recipe:rewrite-jenkins:0.19.0 -Drewrite.activeRecipes=io.jenkins.tools.pluginmodernizer.Debug
```

If you want to test with recipes from modernizer core

```
mvn org.openrewrite.maven:rewrite-maven-plugin:dryRun -Drewrite.recipeArtifactCoordinates=io.jenkins.plugin-modernizer:plugin-modernizer-core:999999-SNAPSHOT -Drewrite.activeRecipes=io.jenkins.tools.pluginmodernizer.Debug

```

## References

- [GSoC 2024 Project Proposal](https://docs.google.com/document/d/1e1QkprPN6fLpFXk_QqBUQlJhZrAl9RvXbOXOiJ-gAuY/edit?usp=sharing)
- [Project Slack Channel](https://cdeliveryfdn.slack.com/archives/C071YTZ807)
- [OpenRewrite Jenkins Recipes](https://docs.openrewrite.org/recipes/jenkins/)
- [OpenRewrite LST](https://docs.openrewrite.org/concepts-explanations/lossless-semantic-trees)
