package com.afeng.logincheck.command;

import com.afeng.logincheck.LoginCheck;
import org.bukkit.Bukkit;
import org.bukkit.command.*;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.regex.Pattern;

/**
 * /logincheck 和 /lc 命令处理器
 */
public class LoginCheckCommand implements CommandExecutor, TabCompleter {
    private final LoginCheck plugin;

    public LoginCheckCommand(LoginCheck plugin) {
        this.plugin = plugin;
    }

    private String replacePlaceholders(String msg, String player, String uuid, String status, String pluginName) {
        return msg.replace("%player%", player).replace("%uuid%", uuid).replace("%status%", status).replace("%plugin%", pluginName);
    }

    private String getUUIDByName(FileConfiguration playersData, String name) {
        String uuid = plugin.getPlayerDataManager().getNameToUUIDCache().get(name.toLowerCase());
        if (uuid != null) return uuid;
        if (!playersData.isConfigurationSection("players")) return null;
        for (String key : playersData.getConfigurationSection("players").getKeys(false)) {
            String storedName = playersData.getString("players." + key + ".name", "");
            if (storedName.equalsIgnoreCase(name)) return key;
            List<String> names = playersData.getStringList("players." + key + ".names");
            for (String oldName : names) {
                if (oldName.equalsIgnoreCase(name)) return key;
            }
        }
        return null;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        FileConfiguration data = plugin.getPlayerDataManager().getPlayersData();
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
            String msg = config.getString("messages.player-info", "§e玩家: §f%player%\n§eUUID: §f%uuid%\n§e状态: §f%status%");
            msg = replacePlaceholders(msg, name, uuid, status, pluginName);
            sender.sendMessage(Pattern.compile("\\r?\\n").split(msg));
            return true;
        }

        // /lc check <玩家名> 或 /lc check <页码> 或 /lc check
        if (args.length >= 1 && args[0].equalsIgnoreCase("check")) {
            // 分页玩家列表
            if (args.length == 1 || (args.length == 2 && args[1].matches("\\d+"))) {
                if (!data.isConfigurationSection("players")) {
                    sender.sendMessage("§e暂无玩家记录。");
                    return true;
                }
                List<String> playerNames = new ArrayList<>();
                for (String uuid : data.getConfigurationSection("players").getKeys(false)) {
                    String name = data.getString("players." + uuid + ".name", uuid);
                    playerNames.add(name);
                }
                if (playerNames.isEmpty()) {
                    sender.sendMessage("§e暂无玩家记录。");
                    return true;
                }
                int PAGE_SIZE = 10;
                int page = 1;
                if (args.length == 2 && args[1].matches("\\d+")) {
                    page = Integer.parseInt(args[1]);
                }
                int total = playerNames.size();
                int totalPages = (int) Math.ceil(total * 1.0 / PAGE_SIZE);
                if (page < 1) page = 1;
                if (page > totalPages) page = totalPages;
                int from = (page - 1) * PAGE_SIZE;
                int to = Math.min(from + PAGE_SIZE, total);

                List<String> jsonList = new ArrayList<>();
                for (int i = from; i < to; i++) {
                    String name = playerNames.get(i);
                    String uuid = plugin.getPlayerDataManager().getNameToUUIDCache().get(name.toLowerCase());
                    String status = data.getString("players." + uuid + ".status", "未知");
                    jsonList.add("{\"text\":\"§b" + name + "\",\"hoverEvent\":{\"action\":\"show_text\",\"value\":\"身份: " + status + "\\nUUID: " + uuid + "\"},\"clickEvent\":{\"action\":\"run_command\",\"value\":\"/lc check " + name + "\"}}");
                    if (i < to - 1) jsonList.add("{\"text\":\"§7 | \"}");
                }
                String json = "[" + String.join(",", jsonList) + "]";

                sender.sendMessage("§a点击下方玩家名可快速查询（第 " + page + "/" + totalPages + " 页）：");
                if (sender instanceof Player) {
                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "tellraw " + sender.getName() + " " + json);
                    if (totalPages > 1) {
                        int prevPage = page > 1 ? page - 1 : 1;
                        int nextPage = page < totalPages ? page + 1 : totalPages;
                        List<String> navList = new ArrayList<>();
                        if (page > 1) navList.add("{\"text\":\"§b[上一页] \",\"clickEvent\":{\"action\":\"run_command\",\"value\":\"/lc check " + prevPage + "\"}}");
                        navList.add("{\"text\":\"§7[第 " + page + "/" + totalPages + " 页]\"}");
                        if (page < totalPages) navList.add("{\"text\":\" §b[下一页]\",\"clickEvent\":{\"action\":\"run_command\",\"value\":\"/lc check " + nextPage + "\"}}");
                        String nav = "[" + String.join(",", navList) + "]";
                        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "tellraw " + sender.getName() + " " + nav);
                    }
                } else {
                    for (int i = from; i < to; i++) {
                        sender.sendMessage(playerNames.get(i));
                    }
                }
                return true;
            }
            // /lc check <玩家名>
            if (args.length == 2) {
                String name = args[1];
                String uuid = getUUIDByName(data, name);
                if (uuid == null) {
                    sender.sendMessage(config.getString("messages.player-not-found", "§c未找到该玩家记录。"));
                    return true;
                }
                String status = data.getString("players." + uuid + ".status", "未知");
                List<String> names = data.getStringList("players." + uuid + ".names");
                String namesStr = names.isEmpty() ? "无" : String.join(", ", names);
                String msg = config.getString("messages.player-info", "§e玩家: §f%player%\n§eUUID: §f%uuid%\n§e状态: §f%status%\n§e曾用名: §f%names%");
                msg = replacePlaceholders(msg, name, uuid, status, pluginName).replace("%names%", namesStr);

                // 始终输出详细信息
                sender.sendMessage(msg.split("\\n"));

                // 如果是玩家，额外输出 tellraw
                if (sender instanceof Player) {
                    List<String> tellrawList = new ArrayList<>();
                    tellrawList.add("{\"text\":\"§e玩家: §f" + name + "\",\"hoverEvent\":{\"action\":\"show_text\",\"value\":\"身份: " + status + "\\nUUID: " + uuid + "\"}}");
                    tellrawList.add("{\"text\":\"\\n§e身份: §f" + status + "\"}");
                    tellrawList.add("{\"text\":\"\\n§eUUID: §f" + uuid + "\",\"hoverEvent\":{\"action\":\"show_text\",\"value\":\"点击复制UUID\"},\"clickEvent\":{\"action\":\"copy_to_clipboard\",\"value\":\"" + uuid + "\"}}");
                    if (!names.isEmpty()) {
                        tellrawList.add("{\"text\":\"\\n§e曾用名: §f" + namesStr + "\"}");
                    }
                    String tellrawJson = "[" + String.join(",", tellrawList) + "]";
                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "tellraw " + sender.getName() + " " + tellrawJson);
                }
                return true;
            }
            sender.sendMessage("§c用法: /lc check [玩家名|页码]");
            return true;
        }

        // /lc reload
        if (args.length == 1 && args[0].equalsIgnoreCase("reload")) {
            if (!sender.hasPermission("logincheck.reload")) {
                sender.sendMessage("§c你没有权限执行此操作。");
                return true;
            }
            plugin.reloadConfig();
            plugin.getPlayerDataManager().savePlayersData();
            sender.sendMessage(config.getString("messages.reload-success", "§a配置文件已重新加载！").replace("%plugin%", pluginName));
            return true;
        }

        sender.sendMessage("§c用法: /lc [check <玩家名>|reload]");
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return Arrays.asList("check", "reload");
        }
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
