package com.afeng.logincheck;

import org.bukkit.Bukkit;
import org.bukkit.command.*;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class LoginCheckCommand implements CommandExecutor, TabCompleter {

    private final LoginCheck plugin;

    public LoginCheckCommand(LoginCheck plugin) {
        this.plugin = plugin;
    }

    private static final String SECTION_PLAYERS = "players";
    private static final String FIELD_NAME = "name";
    private static final String FIELD_STATUS = "status";
    private static final String FIELD_NAMES = "names";

    /**
     * 根据玩家名查找UUID
     */
    private @Nullable String getUUIDByName(@NotNull FileConfiguration playersData,
            @Nullable String name) {
        String uuid = plugin.getNameToUUIDCache().get(name == null ? "" : name.toLowerCase());
        if (uuid != null)
            return uuid;
        if (playersData == null || !playersData.isConfigurationSection(SECTION_PLAYERS))
            return null;
        for (String key : playersData.getConfigurationSection(SECTION_PLAYERS).getKeys(false)) {
            String storedName =
                    playersData.getString(SECTION_PLAYERS + "." + key + "." + FIELD_NAME, "");
            if (storedName.equalsIgnoreCase(name))
                return key;
            List<String> names =
                    playersData.getStringList(SECTION_PLAYERS + "." + key + "." + FIELD_NAMES);
            if (names != null) {
                for (String oldName : names) {
                    if (oldName.equalsIgnoreCase(name))
                        return key;
                }
            }
        }
        return null;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
            @NotNull String label, @NotNull String[] args) {
        FileConfiguration data = plugin.getPlayersData();
        FileConfiguration lang = plugin.getLang();
        String pluginName = plugin.getName();

        if (args.length == 0) {
            sendUsageMain(sender, lang);
            return true;
        }
        if (args.length == 1 && args[0].equalsIgnoreCase("reload")) {
            handleReload(sender, lang);
            return true;
        }
        if (args[0].equalsIgnoreCase("check")) {
            if (args.length == 1 || (args.length == 2 && args[1].matches("\\d+"))) {
                handleCheckPage(sender, args, data, lang, pluginName);
            } else if (args.length == 2) {
                handleCheckPlayer(sender, args[1], data, lang, pluginName);
            } else {
                sendUsageCheck(sender, lang);
            }
            return true;
        }
        sendUsageMain2(sender, lang);
        return true;
    }

    private void sendUsageMain(@NotNull CommandSender sender, @Nullable FileConfiguration lang) {
        sender.sendMessage(
                lang != null ? lang.getString("usage-main", "§e用法: /logincheck <check|reload>")
                        : "§e用法: /logincheck <check|reload>");
    }

    private void sendUsageMain2(@NotNull CommandSender sender, @Nullable FileConfiguration lang) {
        sender.sendMessage(
                lang != null ? lang.getString("usage-main2", "§c用法: /lc [check <玩家名>|reload]")
                        : "§c用法: /lc [check <玩家名>|reload]");
    }

    private void sendUsageCheck(@NotNull CommandSender sender, @Nullable FileConfiguration lang) {
        sender.sendMessage(lang != null ? lang.getString("usage-check", "§c用法: /lc check [玩家名|页码]")
                : "§c用法: /lc check [玩家名|页码]");
    }

    private void handleReload(@NotNull CommandSender sender, @Nullable FileConfiguration lang) {
        if (!sender.hasPermission("logincheck.reload")) {
            sender.sendMessage(lang != null ? lang.getString("no-permission", "§c你没有权限执行此操作。")
                    : "§c你没有权限执行此操作。");
            return;
        }
        plugin.reloadConfig();
        plugin.loadPlayersData();
        plugin.loadLang();
        sender.sendMessage(PlaceholderUtils.replacePlaceholders(
                lang != null ? lang.getString("reload-success", "§a配置文件已重新加载！") : "§a配置文件已重新加载！",
                null, null, null, plugin.getName()));
    }

    /**
     * 处理分页查询
     */
    private void handleCheckPage(@NotNull CommandSender sender, @NotNull String[] args,
            @NotNull FileConfiguration data, @Nullable FileConfiguration lang,
            @NotNull String pluginName) {
        if (!data.isConfigurationSection(SECTION_PLAYERS)) {
            sender.sendMessage(
                    lang != null ? lang.getString("no-player-record", "§e暂无玩家记录。") : "§e暂无玩家记录。");
            return;
        }
        List<String> uuids =
                new ArrayList<>(data.getConfigurationSection(SECTION_PLAYERS).getKeys(false));
        if (uuids.isEmpty()) {
            sender.sendMessage(
                    lang != null ? lang.getString("no-player-record", "§e暂无玩家记录。") : "§e暂无玩家记录。");
            return;
        }
        int PAGE_SIZE = 10;
        int page = 1;
        if (args.length == 2 && args[1].matches("\\d+")) {
            page = Integer.parseInt(args[1]);
        }
        List<String> pageList = CommandUtils.getPage(uuids, page, PAGE_SIZE);
        int total = uuids.size();
        int totalPages = Math.max(1, (int) Math.ceil(total * 1.0 / PAGE_SIZE));
        if (page < 1)
            page = 1;
        if (page > totalPages)
            page = totalPages;

        String header =
                lang != null
                        ? lang.getString("page-header", "").replace("%page%", String.valueOf(page))
                                .replace("%total%", String.valueOf(totalPages))
                        : "";
        sender.sendMessage(header);

        if (sender instanceof Player) {
            for (String uuid : pageList) {
                String name = data.getString(SECTION_PLAYERS + "." + uuid + "." + FIELD_NAME, uuid);
                String status =
                        data.getString(SECTION_PLAYERS + "." + uuid + "." + FIELD_STATUS, "");
                String json = CommandUtils.buildTellrawButton(name, status, uuid);
                try {
                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(),
                            "tellraw " + sender.getName() + " " + json);
                } catch (Exception e) {
                    sender.sendMessage((lang != null ? lang.getString("error-command", "") : "")
                            + e.getMessage());
                }
            }
        } else {
            for (String uuid : pageList) {
                String name = data.getString(SECTION_PLAYERS + "." + uuid + "." + FIELD_NAME, uuid);
                sender.sendMessage(name);
            }
        }
    }

    /**
     * 处理单玩家查询
     */
    private void handleCheckPlayer(@NotNull CommandSender sender, @NotNull String name,
            @NotNull FileConfiguration data, @Nullable FileConfiguration lang,
            @NotNull String pluginName) {
        String uuid = getUUIDByName(data, name);
        if (uuid == null) {
            sender.sendMessage(lang != null ? lang.getString("player-not-found", "§c未找到该玩家记录。")
                    : "§c未找到该玩家记录。");
            return;
        }
        String status = data.getString(SECTION_PLAYERS + "." + uuid + "." + FIELD_STATUS, "");
        String msg =
                lang != null
                        ? lang.getString("player-info", "").replace("%player%", name)
                                .replace("%uuid%", uuid).replace("%status%", status)
                        : "";
        if (sender instanceof Player) {
            String json = CommandUtils.buildTellrawButton(name, status, uuid);
            try {
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(),
                        "tellraw " + sender.getName() + " " + json);
            } catch (Exception e) {
                sender.sendMessage(
                        (lang != null ? lang.getString("error-command", "") : "") + e.getMessage());
            }
        } else {
            sender.sendMessage(msg.split("\\n"));
        }
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
            @NotNull String alias, @NotNull String[] args) {
        if (args.length == 1) {
            return Arrays.asList("check", "reload");
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("check")) {
            FileConfiguration data = plugin.getPlayersData();
            Set<String> names = new LinkedHashSet<>();
            if (data.isConfigurationSection(SECTION_PLAYERS)) {
                for (String uuid : data.getConfigurationSection(SECTION_PLAYERS).getKeys(false)) {
                    String name =
                            data.getString(SECTION_PLAYERS + "." + uuid + "." + FIELD_NAME, uuid);
                    names.add(name);
                }
            }
            return new ArrayList<>(names);
        }
        return Collections.emptyList();
    }
}
