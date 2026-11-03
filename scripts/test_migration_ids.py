"""Keep validate_metadata migration IDs aligned with declarative recipes."""

from __future__ import annotations

import re
import unittest
from pathlib import Path

REPO_ROOT = Path(__file__).resolve().parents[1]
RECIPES = REPO_ROOT / "plugin-modernizer-core/src/main/resources/META-INF/rewrite/recipes.yml"
VALIDATE_SCRIPT = REPO_ROOT / "scripts/validate_metadata.py"

SKIP_PREFIXES = ("io.jenkins.tools.pluginmodernizer.core.recipes.",)

LEGACY_MIGRATION_IDS = {
    "io.jenkins.tools.pluginmodernizer.UpgradeParentVersion",
}


def load_recipe_names() -> set[str]:
    text = RECIPES.read_text(encoding="utf-8")
    names = set(re.findall(r"^name: (io\.jenkins\.tools\.pluginmodernizer\.\S+)", text, re.MULTILINE))
    return {name for name in names if not name.startswith(SKIP_PREFIXES)}


def load_valid_migration_ids() -> set[str]:
    text = VALIDATE_SCRIPT.read_text(encoding="utf-8")
    block = text.split("valid_migration_ids = [", 1)[1].split("]", 1)[0]
    return set(re.findall(r'"([^"]+)"', block))


class MigrationIdSyncTest(unittest.TestCase):
    def test_migration_ids_cover_declarative_recipes(self) -> None:
        missing = sorted(load_recipe_names() - load_valid_migration_ids())
        self.assertEqual([], missing, f"Add to validate_metadata.py: {missing}")

    def test_migration_ids_reference_known_recipes(self) -> None:
        recipe_names = load_recipe_names()
        unknown = sorted(
            id_
            for id_ in load_valid_migration_ids()
            if id_ not in recipe_names and id_ not in LEGACY_MIGRATION_IDS
        )
        self.assertEqual([], unknown, f"Stale migration IDs: {unknown}")


if __name__ == "__main__":
    unittest.main()
