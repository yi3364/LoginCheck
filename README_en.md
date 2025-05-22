<!-- [English/中文] Switch: [English](README_en.md) | [中文](README.md) -->

# LoginCheck

LoginCheck is a Minecraft plugin for Paper/Spigot/Leaves servers. It supports automatic detection of premium/cracked player identities, triggers custom commands for automatic permission group assignment, broadcasts join messages, and provides player information query features.

---

## Features

- **Automatic detection of premium/cracked players**: Assign different permission groups automatically based on player identity when they join for the first time.
- **Customizable join message broadcast**
- **Records first/recent login time, previous names, UUID, etc.**
- **/lc check**: Query all players who have ever joined the server
- **Flexible configuration**: Supports custom commands, messages, groups, data file names, and more

---

## Command Reference

| Command                      | Permission          | Description                                 |
|------------------------------|---------------------|---------------------------------------------|
| /lc                          | -                   | Show your own identity info                 |
| /lc check                    | logincheck.check    | List all players (paginated, one per line)  |
| /lc check <player>           | logincheck.check    | Show detailed info for the specified player |
| /lc check <page>             | logincheck.check    | Browse player list by page                  |
| /lc reload                   | logincheck.reload   | Reload plugin configuration                 |

---

## Configuration

- `config.yml`: Feature switches, custom groups, commands, messages, etc.
- `plugin.yml`: All commands and permissions are declared

---

## Localization

- All messages, prompts, errors, and broadcasts can be customized in `messages_en.yml`, `messages_zh.yml`, etc.
- `config.yml` is only for feature switches, groups, and commands. All text content should be maintained in lang files.

---


## Build & Install

1. Clone this project and build with Maven:
   ```shell
   mvn clean package
   ```
2. Put `target/LoginCheck-x.x.x.jar` into your server's `plugins` directory
3. Start the server to auto-generate config files

---

## Compatibility

- Supports PaperMC 1.20+ / Spigot / Leaves servers
- Java 17 or above is recommended

## License

MIT

---

For suggestions or contributions, feel free to submit an Issue or PR!