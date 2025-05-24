<!-- [English/中文] 切换: [English](README_en.md) | [中文](README.md) -->

# LoginCheck

LoginCheck 是一款适用于 Paper/Spigot/Leaves 服务器的 Minecraft 插件，支持自动识别正版/离线玩家身份，首次进服自动分配权限组，进服消息广播，玩家信息查询等功能。

---

## 功能特性

- **自动识别正版/离线玩家**，首次进服自动分配不同权限组
- **自定义进服消息广播**
- **记录玩家首次/最近登录、曾用名、UUID**
- **/lc check** 查询服务器所有曾加入玩家
- **灵活配置**：支持自定义命令、分组、数据文件名等

---

## 指令说明

| 指令                        | 权限                | 说明                                 |
|-----------------------------|---------------------|--------------------------------------|
| /lc check                   | logincheck.check    | 分页列出所有玩家                     |
| /lc check <玩家名>          | logincheck.check    | 查询指定玩家详细信息                 |
| /lc check <页码>            | logincheck.check    | 分页浏览玩家列表                     |
| /lc reload                  | logincheck.reload   | 重载插件配置                         |

---

## 配置说明

- `config.yml`：功能开关、自定义分组、命令等
- `plugin.yml`：声明所有命令与权限

---

## 多语言与本地化

- 所有消息、提示、错误、广播等均可在 `messages_zh.yml`、`messages_en.yml` 等文件中自定义
- `config.yml` 仅用于功能开关、分组、命令等配置，所有文本内容请在 lang 文件中维护

---

## 构建与安装

1. 克隆本项目，使用 Maven 构建：
   ```shell
   mvn clean package
   ```
2. 将 `target/LoginCheck-x.x.x.jar` 放入服务器 `plugins` 目录
3. 启动服务器，自动生成配置文件

---

## 兼容性

- 支持 PaperMC 1.20+ / Spigot / Leaves 服务端
- 推荐 Java 17 及以上

## 开源协议

MIT

---

如需反馈建议或参与开发，欢迎提交 Issue 或 PR！