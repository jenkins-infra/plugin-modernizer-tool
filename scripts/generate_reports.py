import json
import os
import pandas as pd
from collections import Counter

script_dir = os.path.dirname(os.path.abspath(__file__))
json_dir = os.path.abspath(os.path.join(script_dir, ".."))

def extract_timestamp(key):
    try:
        if not key or not isinstance(key, str) or not key.endswith('.json'):
            print(f"[WARN] Invalid key format: {key}")
            return pd.NaT
        timestamp_str = key.replace('.json', '')
        date_part, time_part = timestamp_str.split('T')
        time_part = time_part.replace('-', ':')
        timestamp_str = f"{date_part}T{time_part}"
        return pd.to_datetime(timestamp_str)
    except (ValueError, AttributeError) as e:
        print(f"[WARN] Failed to parse timestamp from key: {key}, error: {str(e)}")
        return pd.NaT

# Collect data from all JSON files in modernization-metadata directories
data_list = []
for root, dirs, files in os.walk(json_dir):
    if os.path.basename(root) == "modernization-metadata":
        for file in files:
            if file.endswith(".json"):
                with open(os.path.join(root, file), "r") as f:
                    data = json.load(f)
                    # Set missing migrationStatus to empty string
                    if 'migrationStatus' not in data:
                        data['migrationStatus'] = ''
                    data_list.append(data)

# Create a DataFrame for analysis
df = pd.DataFrame(data_list)
if df.empty:
    print("[WARN] No data found. Exiting early.")
    exit(0)

# Add timestamp column
df['timestamp'] = df['key'].apply(extract_timestamp)

# Create overall reports directory if it doesn't exist
os.makedirs("reports", exist_ok=True)

# Report 1: Per-plugin Failed Migrations (CSV)
grouped = df.groupby("pluginName")
for plugin_name, group_df in grouped:
    failed_migrations = group_df[group_df["migrationStatus"] == "fail"]
    if not failed_migrations.empty:
        # Define the report directory for this plugin
        report_dir = os.path.join(json_dir, plugin_name, "reports")
        os.makedirs(report_dir, exist_ok=True)
        # Save the failure report
        report_path = os.path.join(report_dir, "failed_migrations.csv")
        report_columns = ["migrationId", "migrationStatus"]
        failed_migrations[report_columns].to_csv(report_path, index=False)
        print(f"[INFO] Generated failed_migrations.csv for plugin '{plugin_name}' at: {report_path}")

# Report 2: Recipe.json files for each migrationId
recipe_groups = df.groupby("migrationId")
for recipe_id, group_df in recipe_groups:
    total_applications = len(group_df)
    success_count = len(group_df[group_df["migrationStatus"] == "success"])
    failure_count = len(group_df[group_df["migrationStatus"] == "fail"])
    plugins_list = group_df[["pluginName", "migrationStatus", "timestamp"]].sort_values("timestamp", ascending=False)  # newest to oldest
    plugins_list = plugins_list.rename(columns={"migrationStatus": "status"})
    plugins_list["timestamp"] = plugins_list["timestamp"].dt.strftime("%Y-%m-%dT%H-%M-%S")
    plugins_list = plugins_list.to_dict(orient="records")
    recipe_data = {
        "recipeId": recipe_id,
        "totalApplications": total_applications,
        "successCount": success_count,
        "failureCount": failure_count,
        "plugins": plugins_list
    }
    recipe_dir = os.path.join(json_dir, "reports", "recipes")
    os.makedirs(recipe_dir, exist_ok=True)
    recipe_path = os.path.join(recipe_dir, f"{recipe_id}.json")
    with open(recipe_path, "w") as f:
        json.dump(recipe_data, f, indent=2)
    print(f"[INFO] Generated recipe file for '{recipe_id}' at: {recipe_path}")


# Report 3: Overall Summary Report (Markdown)
total_migrations = len(df)
failed_migrations_count = len(df[df["migrationStatus"] == "fail"])
success_rate = ((total_migrations - failed_migrations_count) / total_migrations * 100) if total_migrations > 0 else 0

# Breakdown of failures by migrationId across all plugins
failure_by_recipe = Counter(df[df["migrationStatus"] == "fail"]["migrationId"]).most_common()
failure_table = "\n".join([f"- {recipe}: {count} failures" for recipe, count in failure_by_recipe])

# List of plugins with at least one failed migration
failed_plugins = sorted(df[df["migrationStatus"] == "fail"]["pluginName"].unique())
failed_plugins_list = "\n".join([
    f"- [{plugin}]({os.path.join("..", plugin, 'reports', 'failed_migrations.csv')})"
    for plugin in failed_plugins
]) if failed_plugins else "No plugins with failed migrations."

# Pull Request statistics
unique_prs = df[(df["pullRequestUrl"] != "") & (df["pullRequestStatus"] != "") & (pd.notna(df["pullRequestUrl"])) & (pd.notna(df["pullRequestStatus"]))]
unique_prs = unique_prs.drop_duplicates(subset=["pullRequestUrl"])
total_prs = len(unique_prs)
pr_status_counts = Counter(unique_prs["pullRequestStatus"])
merged_prs = pr_status_counts.get("merged", 0)
open_prs = pr_status_counts.get("open", 0)
closed_prs = pr_status_counts.get("closed", 0)
merge_rate = (merged_prs / total_prs * 100) if total_prs > 0 else 0
open_rate = (open_prs / total_prs * 100) if total_prs > 0 else 0
closed_rate = (closed_prs / total_prs * 100) if total_prs > 0 else 0

# PR statistics table
pr_stats_table = f"""
| Status | Count | Percentage |
|--------|-------|------------|
| Total PRs | {total_prs} | - |
| Open PRs | {open_prs} | {open_rate:.2f}% |
| Closed PRs | {closed_prs} | {closed_rate:.2f}% |
| Merged PRs | {merged_prs} | {merge_rate:.2f}% |
"""

summary = f"""
# Jenkins Plugin Modernizer Report
Generated on: {pd.Timestamp.now(tz='UTC').strftime('%Y-%m-%d %H:%M:%S UTC')}

## Overview
- **Total Migrations**: {total_migrations}
- **Failed Migrations**: {failed_migrations_count}
- **Success Rate**: {success_rate:.2f}%

## Failures by Recipe
{failure_table if failure_by_recipe else "No failures recorded."}

## Plugins with Failed Migrations
{failed_plugins_list}

## Pull Request Statistics
{pr_stats_table}

*Note: No. of Migrations != No. of PRs. A migration applied may trigger force push on already opened PR.*
"""
summary_path = os.path.join(json_dir, "reports", "summary.md")
os.makedirs(os.path.dirname(summary_path), exist_ok=True)
with open(summary_path, "w") as f:
    f.write(summary)

print(f"[INFO] Summary report generated at: {summary_path}")
