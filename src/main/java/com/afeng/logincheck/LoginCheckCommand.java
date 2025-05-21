package com.afeng.logincheck;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;

/**
 * /logincheck 或 /lc 命令处理器 支持 /lc、/lc check <玩家名>、/lc reload
 */
public class LoginCheckCommand implements CommandExecutor, TabCompleter {

    private final LoginCheck plugin;

    public LoginCheckCommand(LoginCheck plugin) {
        this.plugin = plugin;
    }

    /**
     * 占位符替换工具方法
     */
    private String replacePlaceholders(String msg, String player, String uuid, String status,
            String pluginName) {
        return msg.replace("%player%", player).replace("%uuid%", uuid).replace("%status%", status)
                .replace("%plugin%", pluginName);
    }

    /**
     * 通过玩家名或曾用名查找UUID（优先用缓存，其次遍历 names 列表）
     */
    private String getUUIDByName(FileConfiguration playersData, String name) {
        // 先查缓存（当前名）
        String uuid = plugin.getNameToUUIDCache().get(name.toLowerCase());
        if (uuid != null)
            return uuid;
        // 遍历所有玩家的 names 列表
        if (!playersData.isConfigurationSection("players"))
            return null;
        for (String key : playersData.getConfigurationSection("players").getKeys(false)) {
            // 先查主名
            String storedName = playersData.getString("players." + key + ".name", "");
            if (storedName.equalsIgnoreCase(name)) {
                return key;
            }
            // 再查曾用名
            List<String> names = playersData.getStringList("players." + key + ".names");
            for (String oldName : names) {
                if (oldName.equalsIgnoreCase(name)) {
                    return key;
                }
            }
        }
        return null;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        FileConfiguration data = plugin.getPlayersData();
        FileConfiguration config = plugin.getConfig();
        String pluginName = plugin.getDescription().getName();

        // /lc 或 /logincheck，默认查询自己
        if (args.length == 0) {
            if (!(sender instanceof Player)) {
                sender.sendMessage("§c控制台请使用 /lc check <玩家名>");
                return true;
            }
            Player player = (Player) sender;
            String uuid = player.getUniqueId().toString();
            if (!data.contains("players." + uuid)) {
                sender.sendMessage(config.getString("messages.player-not-found", "§c未找到该玩家记录。"));
                return true;
            }
            String name = data.getString("players." + uuid + ".name", "未知");
            String status = data.getString("players." + uuid + ".status", "未知");
            String msg = config.getString("messages.player-info",
                    "§e玩家: §f%player%\n§eUUID: §f%uuid%\n§e状态: §f%status%");
            msg = replacePlaceholders(msg, name, uuid, status, pluginName);
            sender.sendMessage(Pattern.compile("\\r?\\n").split(msg));
            return true;
        }

        // /lc reload
        if (args.length == 1 && args[0].equalsIgnoreCase("reload")) {
            if (!sender.hasPermission("logincheck.reload")) {
                sender.sendMessage("§c你没有权限执行此操作。");
                return true;
            }
            plugin.reloadConfig();
            plugin.loadPlayersData();
            sender.sendMessage(config.getString("messages.reload-success", "§a配置文件已重新加载！")
                    .replace("%plugin%", pluginName));
            return true;
        }

        // /lc check <玩家名>
        if (args.length == 2 && args[0].equalsIgnoreCase("check")) {
            String name = args[1];
            String uuid = getUUIDByName(data, name);
            if (uuid == null) {
                sender.sendMessage(config.getString("messages.player-not-found", "§c未找到该玩家记录。"));
                return true;
            }
            String status = data.getString("players." + uuid + ".status", "未知");
            List<String> names = data.getStringList("players." + uuid + ".names");
            String namesStr = names.isEmpty() ? "无" : String.join(", ", names);
            String msg = config.getString("messages.player-info",
                    "§e玩家: §f%player%\n§eUUID: §f%uuid%\n§e状态: §f%status%\n§e曾用名: §f%names%");
            msg = replacePlaceholders(msg, name, uuid, status, pluginName).replace("%names%",
                    namesStr);
            sender.sendMessage(Pattern.compile("\\r?\\n").split(msg));
            return true;
        }

        // 参数不正确时提示用法
        sender.sendMessage("§c用法: /lc [check <玩家名>|reload]");
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias,
            String[] args) {
        // /lc <tab>
        if (args.length == 1) {
            List<String> sub = new ArrayList<>();
            sub.add("check");
            sub.add("reload");
            return sub;
        }
        // /lc check <tab>
        if (args.length == 2 && args[0].equalsIgnoreCase("check")) {
            List<String> names = new ArrayList<>();
            for (Player player : Bukkit.getOnlinePlayers()) {
                names.add(player.getName());
            }
            return names;
        }
        return Collections.emptyList();
    }
}
