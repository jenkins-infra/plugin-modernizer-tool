import os
import json

script_dir = os.path.dirname(os.path.abspath(__file__))
root_dir = os.path.abspath(os.path.join(script_dir, ".."))

if not os.path.exists(root_dir):
    print(f"[WARN] Root directory '{root_dir}' does not exist. Exiting.")
    exit(0)

plugins_processed = 0

for plugin_dir in os.listdir(root_dir):
    plugin_path = os.path.join(root_dir, plugin_dir)
    if os.path.isdir(plugin_path):
        metadata_dir = os.path.join(plugin_path, "modernization-metadata")
        if os.path.isdir(metadata_dir):
            json_files = [f for f in os.listdir(metadata_dir) if f.endswith(".json")]
            json_files.sort(reverse=True)

            migrations = []
            for json_file in json_files:
                timestamp = json_file[:-5]  # Remove ".json"
                with open(os.path.join(metadata_dir, json_file), "r") as f:
                    data = json.load(f)
                data["timestamp"] = timestamp
                migrations.append(data)

            if migrations:
                plugin_name = migrations[0].get("pluginName", "Unknown")
                plugin_repository = migrations[0].get("pluginRepository", "Unknown")

                # Remove pluginName and pluginRepository only from migrations array
                for migration in migrations:
                    migration.pop("pluginName", None)
                    migration.pop("pluginRepository", None)

                aggregated_data = {
                    "pluginName": plugin_name,
                    "pluginRepository": plugin_repository,
                    "migrations": migrations
                }

                reports_dir = os.path.join(plugin_path, "reports")
                os.makedirs(reports_dir, exist_ok=True)

                output_path = os.path.join(reports_dir, "aggregated_migrations.json")
                with open(output_path, "w") as f:
                    json.dump(aggregated_data, f, indent=2)

                print(f"[INFO] Aggregated migrations written for plugin '{plugin_name}' at: {output_path}")
                plugins_processed += 1

if plugins_processed == 0:
    print("[WARN] No plugins with migrations found.")
else:
    print(f"[INFO] Aggregation complete. Processed {plugins_processed} plugin(s).")
