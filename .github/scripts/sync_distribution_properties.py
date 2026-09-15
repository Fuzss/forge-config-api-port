from pathlib import Path
import json
import sys


PROPERTIES_FILE = Path("gradle.properties")


def flatten_properties(prefix: str, value: object) -> dict[str, str]:
    """
    Flatten a nested dictionary into Gradle property names.

    Nested keys are joined using dots. For example:

    {
        "curseforge": {
            "id": "323071"
        }
    }

    becomes:

    {
        "distributions.curseforge.id": "323071"
    }
    """

    properties = {}

    if isinstance(value, dict):
        for key, child_value in value.items():
            child_prefix = f"{prefix}.{key}" if prefix else key
            properties.update(flatten_properties(child_prefix, child_value))
    else:
        properties[prefix] = str(value)

    return properties


def update_gradle_properties(properties: dict[str, str]) -> None:
    """
    Update existing properties in gradle.properties.

    Properties that are not already present in the file are deliberately
    ignored. This allows branches using different Gradle property formats
    to retain their existing structure.
    """

    with PROPERTIES_FILE.open() as file:
        lines = file.readlines()

    updated_lines = []

    for line in lines:
        stripped_line = line.strip()

        if "=" not in stripped_line or stripped_line.startswith("#"):
            updated_lines.append(line)
            continue

        key = stripped_line.split("=", 1)[0]

        if key in properties:
            updated_lines.append(f"{key}={properties[key]}\n")
        else:
            updated_lines.append(line)

    with PROPERTIES_FILE.open("w") as file:
        file.writelines(updated_lines)


def main() -> None:
    """
    Synchronize distribution properties from versions.json with the currently
    checked out branch.

    The complete contents of versions.json are provided as the first command
    line argument. This allows the script to run while a version branch is
    checked out, even though versions.json only exists on the main branch.
    """

    versions = json.loads(sys.argv[1])

    properties = flatten_properties(
        "distributions",
        versions["distributions"],
    )

    legacy_properties = {
        "projectCurseForgeId": properties.get("distributions.curseforge.id"),
        "projectModrinthId": properties.get("distributions.modrinth.id"),
    }

    github_slug = properties.get("distributions.github.slug")

    if github_slug:
        github_project = (
            github_slug
            if "/" in github_slug
            else f"Fuzss/{github_slug}"
        )

        legacy_properties["modSourceUrl"] = (
            f"https://github.com/{github_project}"
        )
        legacy_properties["modIssueUrl"] = (
            f"https://github.com/{github_project}/issues"
        )

    properties.update({
        key: value
        for key, value in legacy_properties.items()
        if value
    })

    update_gradle_properties(properties)


if __name__ == "__main__":
    main()
