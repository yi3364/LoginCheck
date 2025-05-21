package com.afeng.logincheck;

import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerJoinEvent;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 假人监听器，负责追踪 /bot create 命令，分配权限组并记录假人信息
 */
public class BotJoinListener implements Listener {

    private final LoginCheck plugin;
    private final File botsFile;
    private final FileConfiguration botsData;

    // 临时缓存：假人名 -> 召唤人名
    private final Map<String, String> botSummonerMap = new ConcurrentHashMap<>();

    // 可配置项
    private final String botGroup;
    private final String botsFileName;

    public BotJoinListener(LoginCheck plugin) {
        this.plugin = plugin;
        FileConfiguration config = plugin.getConfig();
        this.botGroup = config.getString("bot.group", "tool");
        this.botsFileName = config.getString("bot.data-file", "bots.yml");

        botsFile = new File(plugin.getDataFolder(), botsFileName);
        if (!botsFile.exists()) {
            try {
                botsFile.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().severe("无法创建 " + botsFileName + "！");
            }
        }
        botsData = YamlConfiguration.loadConfiguration(botsFile);
    }

    /**
     * 监听 /bot create 命令，记录召唤人和假人名
     */
    @EventHandler
    public void onBotCreateCommand(PlayerCommandPreprocessEvent event) {
        String msg = event.getMessage().trim();
        if (msg.toLowerCase().startsWith("/bot create ")) {
            String[] args = msg.split("\\s+");
            if (args.length >= 3) {
                String botName = args[2];
                botSummonerMap.put(botName, event.getPlayer().getName());
            }
        }
    }

    /**
     * 监听玩家加入，判断是否为刚刚被召唤的假人
     */
    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        String name = player.getName();
        if (!botSummonerMap.containsKey(name))
            return;

        String summoner = botSummonerMap.remove(name);
        UUID uuid = player.getUniqueId();
        long now = System.currentTimeMillis();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String nowStr = sdf.format(new Date(now));

        // 分配权限组
        Bukkit.dispatchCommand(Bukkit.getConsoleSender(),
                "lp user " + name + " parent add " + botGroup);

        String path = "bots." + uuid;
        botsData.set(path + ".name", name);

        // 追加历史召唤人
        List<?> rawList = botsData.getList(path + ".summoners");
        List<Map<String, Object>> summoners = new java.util.ArrayList<>();
        if (rawList != null) {
            for (Object obj : rawList) {
                if (obj instanceof Map) {
                    // 进一步检查Map的key类型
                    Map<?, ?> map = (Map<?, ?>) obj;
                    boolean valid = true;
                    for (Object key : map.keySet()) {
                        if (!(key instanceof String)) {
                            valid = false;
                            break;
                        }
                    }
                    if (valid) {
                        @SuppressWarnings("unchecked")
                        Map<String, Object> safeMap = (Map<String, Object>) map;
                        summoners.add(safeMap);
                    }
                }
            }
        }
        Map<String, Object> record = new java.util.HashMap<>();
        record.put("summoner", summoner);
        record.put("time", nowStr);
        summoners.add(record);
        botsData.set(path + ".summoners", summoners);

        // 兼容旧字段
        botsData.set(path + ".summoner", summoner);
        botsData.set(path + ".summon-time", nowStr);

        try {
            botsData.save(botsFile);
        } catch (IOException e) {
            plugin.getLogger().severe("无法保存 " + botsFileName + "！");
        }

        plugin.getLogger().info("已为假人 " + name + " 分配 " + botGroup + " 权限组，并记录召唤人: " + summoner);
    }

    public FileConfiguration getBotsData() {
        return botsData;
    }
}
