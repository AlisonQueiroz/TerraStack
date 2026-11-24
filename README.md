# TerraStack

Terraria-style **quick stack** for Paper servers. Type `/qs` and every item in your main inventory
that already exists in a nearby chest flies into that chest. Your hotbar, armor and off hand are never touched.

![build](https://github.com/AlisonQueiroz/TerraStack/actions/workflows/build.yml/badge.svg)

## Features

- **Only matching items**: an item goes only into chests that already hold the same item (like Terraria).
- **Hotbar, armor and off hand are safe**: only the 27 main-inventory slots are deposited.
- **Flying item animation**: each stored item flies from you to its chest, then a small sparkle.
- **Keep rules per player**: `/qs keep arrow 1` always leaves one arrow; `/qs keep torch all` never stores torches.
- **Respects protection plugins**: containers you could not open yourself (WorldGuard, GriefPrevention,
  Lands, Towny, Bolt, ...) are skipped.
- **Custom radius**: `/qs` uses the default radius (16), `/qs 24` searches 24 blocks.
- Chests, trapped chests, copper chests and barrels (shulker boxes optional).
- Languages: English and Brazilian Portuguese; every message can be overridden.
- Works for Bedrock players too (via Geyser/Floodgate), since it is a command.

## Commands

| Command | Description |
|---|---|
| `/qs` | Quick stack with the default radius |
| `/qs <radius>` | Quick stack with a custom radius (up to `max-radius`) |
| `/qs keep` | List your keep rules |
| `/qs keep <item\|hand> [amount\|all]` | Always keep at least `amount` of the item (no amount or `all` = never store it) |
| `/qs keep <item> 0` | Remove a keep rule |
| `/qs reload` | Reload the config (permission `terrastack.admin`) |

Aliases: `/quickstack`, `/terrastack`. Item names are Minecraft ids (`arrow`, `torch`, `cooked_beef`) and tab-complete.

Tip for Java players: bind `/qs` to a key with a client mod such as
[Command Keys](https://modrinth.com/mod/commandkeys) for a one-key quick stack.

## Permissions

| Permission | Default | Description |
|---|---|---|
| `terrastack.use` | everyone | Use `/qs` and `/qs keep` |
| `terrastack.admin` | op | Use `/qs reload` |
| `terrastack.bypass-protection` | nobody | Deposit into containers protection plugins would deny |

## Configuration

```yaml
language: en            # en or pt_BR
radius: 16              # default radius for /qs
max-radius: 32
respect-protection: true
include-shulker-boxes: false
animation:
  enabled: true
  flight-ticks: 8
  max-items: 24
messages: {}            # per-key overrides, see src/main/resources/lang/en.yml
```

## How protection is checked

Before using a container, TerraStack sends a simulated right-click (`PlayerInteractEvent`) on it.
If a protection plugin denies it, the container is skipped. Disable with `respect-protection: false`
or give trusted staff `terrastack.bypass-protection`.

## Requirements

- Paper (or a fork such as Purpur) 1.21+ / 26.x (bytecode targets Java 21; building needs JDK 25 because paper-api 26.x is compiled for it)
- Not Folia-compatible yet

## Building

```sh
mvn package
# -> target/TerraStack-<version>.jar
```

## License

[MIT](LICENSE)
