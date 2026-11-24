# Store listing (Modrinth / Hangar)

Copy-paste material for publishing a new version. Upload the jar from the GitHub Release of the tag.

## Project fields

| Field | Value |
|---|---|
| Name | TerraStack |
| Slug / URL | `terrastack` |
| Summary (short) | Terraria-style quick stack: `/qs` deposits your inventory into nearby chests that already hold the same item. |
| Project type | Plugin |
| Loaders / platforms | Paper, Purpur (Modrinth: `paper`, `purpur`; Hangar: Paper) |
| Environment | Server-side only (clients need nothing) |
| Categories | Utility, Storage, Game mechanics |
| License | MIT |
| Source | https://github.com/AlisonQueiroz/TerraStack |
| Issues | https://github.com/AlisonQueiroz/TerraStack/issues |
| Minecraft versions | 26.3 (tested). Add older versions only after testing them. |

## Description

Use the README from "Features" down to "Configuration" (it renders the same on Modrinth and Hangar).
Suggested first lines:

> Press one command and every item in your main inventory that already exists in a nearby chest flies into it,
> just like Terraria's *Quick stack to nearby chests*. Your hotbar, armor and off hand are never touched.

## Version entry

| Field | Value |
|---|---|
| Version number | `1.5.0` |
| Channel | Release (use Beta while protection-plugin support is untested by users) |
| Changelog | First public release. Quick stack with radius, flying item animation, per-player keep rules, protection-plugin check, en/pt_BR. |
| File | `TerraStack-1.5.0.jar` from https://github.com/AlisonQueiroz/TerraStack/releases/tag/v1.5.0 |

## Release checklist

1. Bump `<version>` in `pom.xml`, commit.
2. `git tag -a vX.Y.Z -m "TerraStack X.Y.Z" && git push origin vX.Y.Z` — CI builds and attaches the jar to the GitHub Release.
3. Upload that jar to Modrinth and Hangar with the changelog.
