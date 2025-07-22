import json
import os
import requests
import logging
from github import Github

# Configure logging
logging.basicConfig(level=logging.INFO, format='%(levelname)s: %(message)s')

# Authenticate with GitHub using GH_TOKEN
token = os.getenv('GH_TOKEN')
if not token:
    logging.error("GITHUB_TOKEN not found.")
    exit(1)

g = Github(token)

def update_metadata(file_path):
    logging.info(f"Processing file: {file_path}")
    try:
        with open(file_path, 'r') as f:
            metadata = json.load(f)
    except Exception as e:
        logging.error(f"Failed to read {file_path}: {e}")
        return

    pr_url = metadata.get('pullRequestUrl')
    if not pr_url:
        logging.warning(f"No pullRequestUrl found in {file_path}, skipping.")
        return

    try:
        parts = pr_url.split('/')
        owner, repo_name, pr_num = parts[3], parts[4], parts[6]
        logging.info(f"Extracted PR: {owner}/{repo_name}#{pr_num}")

        repo = g.get_repo(f"{owner}/{repo_name}")
        pr = repo.get_pull(int(pr_num))
        status = 'merged' if pr.merged else pr.state
        metadata['pullRequestStatus'] = status

        # Get Check Runs status
        commit = repo.get_commit(pr.head.sha)
        check_runs = commit.get_check_runs()

        sorted_checks = {
            check.name: check.conclusion
            for check in sorted(check_runs, key=lambda c: c.name)
        }
        # Only update if changed
        if metadata.get('checkRuns') != sorted_checks:
            metadata['checkRuns'] = sorted_checks
            metadata['checkRunsSummary'] = summarize_check_runs(sorted_checks)
            logging.info(f"Check runs updated for PR #{pr_num}: {sorted_checks}")
            logging.info(f"Check runs summary updated for PR #{pr_num}: {metadata['checkRunsSummary']}")

        # Get default branch and latest commit sha
        default_branch, latest_commit_sha = get_default_branch_info(repo)
        metadata['defaultBranch'] = default_branch
        metadata['defaultBranchLatestCommitSha'] = latest_commit_sha

        logging.info(f"Default branch: {default_branch}, latest commit SHA: {latest_commit_sha}")

        # Write updated metadata back to the file
        with open(file_path, 'w') as f:
            json.dump(metadata, f, indent=2)

        logging.info(f"Updated pull request status and branch info in {file_path}")

    except Exception as e:
        logging.error(f"Failed to update pull request status and branch info for {file_path}: {e}")

def get_default_branch_info(repo):
    try:
        default_branch = repo.default_branch
        latest_commit_sha = repo.get_branch(default_branch).commit.sha
        return default_branch, latest_commit_sha
    except Exception as e:
        logging.error(f"Failed to get default branch info for repo {repo.full_name}: {e}")
        return None, None


def summarize_check_runs(checks_summary):
    conclusions = list(checks_summary.values())

    if any(c is None for c in conclusions):
        return 'pending'
    elif any(c in ['failure', 'timed_out', 'cancelled'] for c in conclusions):
        return 'failure'
    elif all(c == 'success' for c in conclusions):
        return 'success'
    else:
        return 'neutral'  # fallback if mixed states like 'neutral', 'skipped', etc.

# Find all 'modernization-metadata' folders anywhere under root_dir
def find_all_metadata_dirs(root_dir='.'):
    matched_dirs = []
    for dirpath, dirnames, _ in os.walk(root_dir):
        for dirname in dirnames:
            if dirname == 'modernization-metadata':
                full_path = os.path.join(dirpath, dirname)
                matched_dirs.append(full_path)
    return matched_dirs

root_dir = '.'  # or your project root path
metadata_dirs = find_all_metadata_dirs(root_dir)

if not metadata_dirs:
    logging.error("No 'modernization-metadata' directories found.")
    exit(1)

logging.info(f"Found {len(metadata_dirs)} 'modernization-metadata' directories.")

for metadata_dir in metadata_dirs:
    logging.info(f"Processing directory: {metadata_dir}")
    for root, _, files in os.walk(metadata_dir):
        for file in files:
            if file.endswith('.json'):
                file_path = os.path.join(root, file)
                update_metadata(file_path)