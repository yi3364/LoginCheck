package com.afeng.logincheck;

import org.bukkit.Bukkit;
import org.bukkit.command.*;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.util.*;
import java.util.regex.Pattern;

/**
 * /logincheck 或 /lc 命令处理器
 * 支持 /lc、/lc check <玩家名>、/lc check <页码>、/lc bots [关键词] [页码]、/lc reload
 */
public class LoginCheckCommand implements CommandExecutor, TabCompleter {

    private final LoginCheck plugin;

    public LoginCheckCommand(LoginCheck plugin) {
        this.plugin = plugin;
    }

    /**
     * 占位符替换工具方法
     */
    private String replacePlaceholders(String msg, String player, String uuid, String status, String pluginName) {
        return msg.replace("%player%", player)
                .replace("%uuid%", uuid)
                .replace("%status%", status)
                .replace("%plugin%", pluginName);
    }

    /**
     * 通过玩家名或曾用名查找UUID（优先用缓存，其次遍历 names 列表）
     */
    private String getUUIDByName(FileConfiguration playersData, String name) {
        // 先查缓存（当前名）
        String uuid = plugin.getNameToUUIDCache().get(name.toLowerCase());
        if (uuid != null) return uuid;
        // 遍历所有玩家的 names 列表
        if (!playersData.isConfigurationSection("players")) return null;
        for (String key : playersData.getConfigurationSection("players").getKeys(false)) {
            // 先查主名
            String storedName = playersData.getString("players." + key + ".name", "");
            if (storedName.equalsIgnoreCase(name)) return key;
            // 再查曾用名
            List<String> names = playersData.getStringList("players." + key + ".names");
            for (String oldName : names) {
                if (oldName.equalsIgnoreCase(name)) return key;
            }
        }
        return null;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        FileConfiguration data = plugin.getPlayersData();
        FileConfiguration config = plugin.getConfig();
        String pluginName = plugin.getDescription().getName();

        // === /lc bots [关键词] [页码] ===
        if (args.length >= 1 && args[0].equalsIgnoreCase("bots")) {
            // 读取 bots.yml
            String botsFileName = config.getString("bot.data-file", "bots.yml");
            File botsFile = new File(plugin.getDataFolder(), botsFileName);
            FileConfiguration botsData = YamlConfiguration.loadConfiguration(botsFile);

            if (!botsData.isConfigurationSection("bots")) {
                sender.sendMessage("§e暂无假人记录。");
                return true;
            }

            // 解析参数：支持 /lc bots [关键词] [页码]
            String filter = null;
            int page = 1;
            for (int i = 1; i < args.length; i++) {
                if (args[i].matches("\\d+")) {
                    page = Integer.parseInt(args[i]);
                } else {
                    filter = args[i].toLowerCase();
                }
            }

            // 收集并筛选假人
            List<Map<String, String>> botList = new ArrayList<>();
            for (String uuid : botsData.getConfigurationSection("bots").getKeys(false)) {
                String name = botsData.getString("bots." + uuid + ".name", "未知");
                String summoner = botsData.getString("bots." + uuid + ".summoner", "未知");
                String time = botsData.getString("bots." + uuid + ".summon-time", "未知");
                if (filter != null && !(name.toLowerCase().contains(filter) || summoner.toLowerCase().contains(filter))) {
                    continue;
                }
                Map<String, String> map = new HashMap<>();
                map.put("uuid", uuid);
                map.put("name", name);
                map.put("summoner", summoner);
                map.put("time", time);
                botList.add(map);
            }

            if (botList.isEmpty()) {
                sender.sendMessage("§e没有符合条件的假人记录。");
                return true;
            }

            // 分页
            int PAGE_SIZE = 8;
            int total = botList.size();
            int totalPages = (int) Math.ceil(total * 1.0 / PAGE_SIZE);
            if (page < 1) page = 1;
            if (page > totalPages) page = totalPages;
            int from = (page - 1) * PAGE_SIZE;
            int to = Math.min(from + PAGE_SIZE, total);

            // 构造 JSON 假人列表，召唤人可点击跳转
            StringBuilder json = new StringBuilder("[");
            for (int i = from; i < to; i++) {
                Map<String, String> bot = botList.get(i);
                if (i > from) json.append(",");
                // 假人名（点击复制）
                json.append("{\"text\":\"§6").append(bot.get("name")).append("§r\",")
                    .append("\"hoverEvent\":{\"action\":\"show_text\",\"value\":\"点击复制假人名\"},")
                    .append("\"clickEvent\":{\"action\":\"suggest_command\",\"value\":\"").append(bot.get("name")).append("\"}},");
                // UUID
                json.append("{\"text\":\" §7(UUID: ").append(bot.get("uuid")).append(") §f召唤人: \"},");
                // 召唤人（点击跳转到 /lc check 召唤人）
                json.append("{\"text\":\"§b").append(bot.get("summoner")).append("§r\",")
                    .append("\"hoverEvent\":{\"action\":\"show_text\",\"value\":\"点击查询召唤人信息\"},")
                    .append("\"clickEvent\":{\"action\":\"run_command\",\"value\":\"/lc check ")
                    .append(bot.get("summoner")).append("\"}},");
                // 时间
                json.append("{\"text\":\" §7时间: §e").append(bot.get("time")).append("\"}");
                if (i != to - 1) json.append(",");
            }
            json.append("]");

            sender.sendMessage("§a假人列表（共" + total + "个，页码 " + page + "/" + totalPages + "）：");
            if (sender instanceof Player) {
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(),
                    "tellraw " + sender.getName() + " " + json.toString());
                // 翻页按钮
                int prevPage = page > 1 ? page - 1 : 1;
                int nextPage = page < totalPages ? page + 1 : totalPages;
                String filterArg = (filter != null) ? filter + " " : "";

                StringBuilder nav = new StringBuilder("[");
                if (page > 1) {
                    nav.append("{\"text\":\"§b[上一页] \",\"clickEvent\":{\"action\":\"run_command\",\"value\":\"/lc bots ")
                        .append(filterArg).append(prevPage).append("\"}}");
                }
                nav.append("{\"text\":\"§7[第 ").append(page).append("/").append(totalPages).append(" 页]\"}");
                if (page < totalPages) {
                    if (page > 1) nav.append(",");
                    nav.append("{\"text\":\" §b[下一页]\",\"clickEvent\":{\"action\":\"run_command\",\"value\":\"/lc bots ")
                        .append(filterArg).append(nextPage).append("\"}}");
                }
                nav.append("]");
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(),
                    "tellraw " + sender.getName() + " " + nav.toString());
            } else {
                // 控制台只输出文本
                for (int i = from; i < to; i++) {
                    Map<String, String> bot = botList.get(i);
                    sender.sendMessage("§6" + bot.get("name") + " §7(UUID: " + bot.get("uuid") + ") §f召唤人: §b" + bot.get("summoner") + " §7时间: §e" + bot.get("time"));
                }
            }
            return true;
        }

        // === /lc 或 /logincheck，默认查询自己 ===
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

        // === /lc reload ===
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

        // === /lc check <玩家名> 或 /lc check <页码> 或 /lc check ===
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
                // 分页
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

                // 构造 JSON 按钮
                StringBuilder json = new StringBuilder("[");
                for (int i = from; i < to; i++) {
                    String name = playerNames.get(i);
                    if (i > from) json.append(",");
                    json.append("{\"text\":\"§b").append(name)
                        .append("\",\"clickEvent\":{\"action\":\"run_command\",\"value\":\"/lc check ")
                        .append(name).append("\"}}");
                }
                json.append("]");

                sender.sendMessage("§a点击下方玩家名可快速查询（第 " + page + "/" + totalPages + " 页）：");
                if (sender instanceof Player) {
                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(),
                        "tellraw " + sender.getName() + " " + json.toString());
                    // 翻页按钮
                    if (totalPages > 1) {
                        int prevPage = page > 1 ? page - 1 : 1;
                        int nextPage = page < totalPages ? page + 1 : totalPages;
                        StringBuilder nav = new StringBuilder("[");
                        if (page > 1) {
                            nav.append("{\"text\":\"§b[上一页] \",\"clickEvent\":{\"action\":\"run_command\",\"value\":\"/lc check ")
                                .append(prevPage).append("\"}}");
                        }
                        nav.append("{\"text\":\"§7[第 ").append(page).append("/").append(totalPages).append(" 页]\"}");
                        if (page < totalPages) {
                            if (page > 1) nav.append(",");
                            nav.append("{\"text\":\" §b[下一页]\",\"clickEvent\":{\"action\":\"run_command\",\"value\":\"/lc check ")
                                .append(nextPage).append("\"}}");
                        }
                        nav.append("]");
                        Bukkit.dispatchCommand(Bukkit.getConsoleSender(),
                            "tellraw " + sender.getName() + " " + nav.toString());
                    }
                } else {
                    sender.sendMessage(String.join(", ", playerNames.subList(from, to)));
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
                String msg = config.getString("messages.player-info",
                        "§e玩家: §f%player%\n§eUUID: §f%uuid%\n§e状态: §f%status%\n§e曾用名: §f%names%");
                msg = replacePlaceholders(msg, name, uuid, status, pluginName).replace("%names%", namesStr);
                sender.sendMessage(Pattern.compile("\\r?\\n").split(msg));
                return true;
            }
            // 参数不正确时提示用法
            sender.sendMessage("§c用法: /lc check [玩家名|页码]");
            return true;
        }

        // 其它参数不正确时提示用法
        sender.sendMessage("§c用法: /lc [check <玩家名>|reload|bots]");
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        // /lc <tab>
        if (args.length == 1) {
            return Arrays.asList("check", "reload", "bots");
        }
        // /lc check <tab>
        if (args.length == 2 && args[0].equalsIgnoreCase("check")) {
            List<String> names = new ArrayList<>();
            for (Player player : Bukkit.getOnlinePlayers()) {
                names.add(player.getName());
            }
            return names;
        }
        // /lc bots <tab> 可补全关键词或页码（可选扩展）
        return Collections.emptyList();
    }
}
