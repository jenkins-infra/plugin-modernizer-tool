import json
import os
import re
import requests
from github import Github
from jsonschema import validate, ValidationError

# GitHub API client
token = os.getenv('GH_TOKEN')
if not token:
    raise EnvironmentError("GITHUB TOKEN is not found.")
g = Github(token)
repo = g.get_repo('Raunak80Madan/metadata-plugin-modernizer')
pr_number = os.getenv("PR_NUMBER")
pr = repo.get_pull(int(pr_number))

# JSON schema for metadata validation
schema = {
    "type": "object",
    "required": ["pluginName", "pluginRepository", "pluginVersion", "effectiveBaseline", "targetBaseline", "jenkinsVersion", "migrationName", "migrationDescription", "tags", "migrationId", "migrationStatus", "pullRequestUrl", "pullRequestStatus", "dryRun", "additions", "deletions", "changedFiles", "key", "path"],
    "properties": {
        "pluginName": {"type": "string", "pattern": "^[a-zA-Z0-9-]+$"},
        "pluginRepository": {"type": "string", "format": "uri", "pattern": "^https://github.com/[^/]+/.+\\.git$"},
        "pluginVersion": {"type": "string", "pattern": "^[0-9]+(\\.[a-zA-Z0-9._-]+)*(-[a-zA-Z0-9._-]+)?$"},
        "effectiveBaseline": {"type": "string", "pattern": "^[0-9]+\\.[0-9]+$"},
        "targetBaseline": {"type": "string", "pattern": "^[0-9]+\\.[0-9]+$"},
        "jenkinsVersion": {"type": "string", "pattern": "^[0-9]+\\.[0-9]+(\\.[0-9]+)?$"},
        "migrationName": {"type": "string", "minLength": 1},
        "migrationDescription": {"type": "string", "minLength": 1},
        "tags": {"type": "array", "items": {"type": "string"}},
        "migrationId": {"type": "string", "pattern": "^io\\.jenkins\\.tools\\.pluginmodernizer\\..+$"},
        "migrationStatus": {"type": "string", "enum": ["success", "fail"]},
        "pullRequestUrl": {"type": "string", "anyOf": [{"pattern": "^$"}, {"format": "uri", "pattern": "^https://github.com/[^/]+/[^/]+/pull/[0-9]+$"}]},
        "pullRequestStatus": {"type": "string", "anyOf": [{"pattern": "^$"}, {"enum": ["open", "closed", "merged"]}]},
        "dryRun": {"type": "boolean"},
        "additions": {"type": "integer", "minimum": 0},
        "deletions": {"type": "integer", "minimum": 0},
        "changedFiles": {"type": "integer", "minimum": 0},
        "key": {"type": "string", "pattern": "^[0-9]{4}-[0-9]{2}-[0-9]{2}T[0-9]{2}-[0-9]{2}-[0-9]{2}\\.json$"},
        "path": {"type": "string", "pattern": "^metadata-plugin-modernizer/[^/]+/modernization-metadata$"}
    }
}

# migration IDs
valid_migration_ids = [
    "io.jenkins.tools.pluginmodernizer.FetchMetadata,"
    "io.jenkins.tools.pluginmodernizer.MergeGitIgnoreRecipe",
    "io.jenkins.tools.pluginmodernizer.UpdateScmUrl",
    "io.jenkins.tools.pluginmodernizer.SetupJenkinsfile",
    "io.jenkins.tools.pluginmodernizer.SetupGitIgnore",
    "io.jenkins.tools.pluginmodernizer.SetupSecurityScan",
    "io.jenkins.tools.pluginmodernizer.AddPluginsBom",
    "io.jenkins.tools.pluginmodernizer.MigrateToJUnit5",
    "io.jenkins.tools.pluginmodernizer.MigrateToJenkinsBaseLineProperty",
    "io.jenkins.tools.pluginmodernizer.AddCodeOwner",
    "io.jenkins.tools.pluginmodernizer.UpgradeParentVersion",
    "io.jenkins.tools.pluginmodernizer.UpgradeNextMajorParentVersion",
    "io.jenkins.tools.pluginmodernizer.UpgradeParent4Version",
    "io.jenkins.tools.pluginmodernizer.UpgradeParent5Version",
    "io.jenkins.tools.pluginmodernizer.UpgradeBomVersion",
    "io.jenkins.tools.pluginmodernizer.RemoveDependencyVersionOverride",
    "io.jenkins.tools.pluginmodernizer.RemoveDevelopersTag",
    "io.jenkins.tools.pluginmodernizer.RemoveExtraMavenProperties",
    "io.jenkins.tools.pluginmodernizer.ReplaceIOException2WithIOException",
    "io.jenkins.tools.pluginmodernizer.ReplaceLibrariesWithApiPlugin",
    "io.jenkins.tools.pluginmodernizer.UseJsonApiPlugin",
    "io.jenkins.tools.pluginmodernizer.UseJsonPathApiPlugin",
    "io.jenkins.tools.pluginmodernizer.UseAsmApiPlugin",
    "io.jenkins.tools.pluginmodernizer.UseJodaTimeApiPlugin",
    "io.jenkins.tools.pluginmodernizer.UseGsonApiPlugin",
    "io.jenkins.tools.pluginmodernizer.UseJsoupApiPlugin",
    "io.jenkins.tools.pluginmodernizer.UseCompressApiPlugin",
    "io.jenkins.tools.pluginmodernizer.UseCommonsLangApiPlugin",
    "io.jenkins.tools.pluginmodernizer.UseByteBuddyApiPlugin",
    "io.jenkins.tools.pluginmodernizer.UseCommonsTextApiPlugin",
    "io.jenkins.tools.pluginmodernizer.EnsureRelativePath",
    "io.jenkins.tools.pluginmodernizer.UpgradeToRecommendCoreVersion",
    "io.jenkins.tools.pluginmodernizer.UpgradeToLatestJava11CoreVersion",
    "io.jenkins.tools.pluginmodernizer.UpgradeToLatestJava8CoreVersion",
    "io.jenkins.tools.pluginmodernizer.SetupDependabot",
    "io.jenkins.tools.pluginmodernizer.RemoveReleaseDrafter",
    "io.jenkins.tools.pluginmodernizer.FixJellyIssues",
    "io.jenkins.tools.pluginmodernizer.conditions.IsUsingRecommendCoreVersion",
    "io.jenkins.tools.pluginmodernizer.conditions.IsUsingCoreVersionWithASMRemoved",
    "io.jenkins.tools.pluginmodernizer.conditions.IsUsingCoreVersionWithCommonsCompressRemoved",
    "io.jenkins.tools.pluginmodernizer.EnsureIndexJelly",
    "io.jenkins.tools.pluginmodernizer.MigrateTomakehurstToWiremock"
]

def validate_metadata(file_path):
    """Validate a metadata JSON file."""
    with open(file_path, 'r') as f:
        metadata = json.load(f)

    # Schema validation
    try:
        metadata['path'] = metadata['path'].replace('\\', '/')
        validate(instance=metadata, schema=schema)
    except ValidationError as e:
        pr.create_issue_comment(f"Invalid metadata in {file_path}: {e.message}")
        raise

    # Validate migrationId
    if metadata['migrationId'] not in valid_migration_ids:
        pr.create_issue_comment(f"Unknown migrationId '{metadata['migrationId']}' in {file_path}")
        raise ValueError("Invalid migrationId")

    # Validate plugin repository
    repo_name = metadata['pluginRepository'].replace('https://github.com/', '').replace('.git', '')
    try:
        g.get_repo(repo_name)
    except:
        pr.create_issue_comment(f"Invalid plugin repository '{metadata['pluginRepository']}' in {file_path}")
        raise ValueError("Invalid plugin repository")

    # Validate PR URL and Status only if pullRequestUrl is not empty
    pr_url = metadata.get('pullRequestUrl', '')
    pr_status = metadata.get('pullRequestStatus', '')

    if pr_url:
        pr_match = re.match(r'https://github.com/([^/]+)/([^/]+)/pull/([0-9]+)', pr_url)
        if not pr_match:
            pr.create_issue_comment(f"Invalid PR URL '{pr_url}' in {file_path}")
            raise ValueError("Invalid PR URL")

        owner, repo, pr_num = pr_match.groups()
        try:
            plugin_pr = g.get_repo(f"{owner}/{repo}").get_pull(int(pr_num))
            actual_status = 'merged' if plugin_pr.merged else plugin_pr.state
            if pr_status and pr_status != actual_status:
                pr.create_issue_comment(f"PR status mismatch in {file_path}: metadata says '{pr_status}', but actual status is '{actual_status}'")
                raise ValueError("PR status mismatch")
        except:
            pr.create_issue_comment(f"Unable to fetch PR '{pr_url}' in {file_path}")
            raise ValueError("Invalid PR")

# Process changed JSON files in the PR
files = pr.get_files()
for file in files:
    if file.filename.endswith('.json') and '/modernization-metadata/' in file.filename:
        validate_metadata(file.filename)
