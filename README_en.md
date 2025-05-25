**Switch**: **English** | [简体中文](README.md)

# LoginCheck

LoginCheck is a Minecraft plugin for Paper/Spigot/Leaves servers. It supports automatic detection of premium/cracked player identities, automatic permission group assignment on first join, join message broadcast, and player information query.

---

## Features

- **Automatic detection of premium/cracked players**: Assign different permission groups automatically on first join
- **Customizable join message broadcast**
- **Records first/recent login time, previous names, UUID, etc.**
- **/lc check**: Query all players who have ever joined the server
- **Flexible configuration**: Supports custom commands, groups, data file names, etc.

---

## Command Reference

| Command                      | Permission          | Description                                 |
|------------------------------|---------------------|---------------------------------------------|
| /lc check                    | logincheck.check    | List all players (paginated)                |
| /lc check <player>           | logincheck.check    | Show detailed info for the specified player |
| /lc check <page>             | logincheck.check    | Browse player list by page                  |
| /lc reload                   | logincheck.reload   | Reload plugin configuration                 |

---

## Configuration

- `config.yml`: Feature switches, custom groups, commands, etc.
- `plugin.yml`: All commands and permissions are declared

---


## Build & Install

1. Clone this project and build with Maven:
   ```shell
   mvn clean package
   ```
2. Put `target/LoginCheck-${project.version}.jar` into your server's `plugins` directory
3. Start the server to auto-generate config files

---

## Compatibility

- Supports PaperMC 1.20+ / Spigot / Leaves servers
- Java 17 or above is recommended

## License

MIT

---

For suggestions or contributions, feel free to submit an Issue or PR!