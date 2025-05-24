package com.afeng.logincheck.util;

import com.afeng.logincheck.LoginCheck;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 玩家数据管理器，负责玩家数据的加载、保存、缓存与查询。
 */
public class PlayerDataManager {

    private final LoginCheck plugin;
    private final File playersFile;
    private final FileConfiguration playersData;
    // 玩家名到UUID的缓存（小写名 -> uuid字符串）
    private final Map<String, String> nameToUUID = new ConcurrentHashMap<>();

    public PlayerDataManager(LoginCheck plugin) {
        this.plugin = plugin;
        this.playersFile = new File(plugin.getDataFolder(), "players.yml");
        if (!playersFile.exists()) {
            try {
                playersFile.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().severe("无法创建 players.yml：" + e.getMessage());
            }
        }
        this.playersData = YamlConfiguration.loadConfiguration(playersFile);
        refreshNameToUUIDCache();
    }

    /**
     * 保存 players.yml
     */
    public void savePlayersData() {
        try {
            playersData.save(playersFile);
        } catch (IOException e) {
            plugin.getLogger().severe("无法保存 players.yml：" + e.getMessage());
        }
    }

    /**
     * 异步保存 players.yml
     */
    public void savePlayersDataAsync() {
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, this::savePlayersData);
    }

    /**
     * 刷新玩家名到UUID的缓存
     */
    public void refreshNameToUUIDCache() {
        nameToUUID.clear();
        if (playersData.isConfigurationSection("players")) {
            for (String uuid : playersData.getConfigurationSection("players").getKeys(false)) {
                String name = playersData.getString("players." + uuid + ".name", null);
                if (name != null) {
                    nameToUUID.put(name.toLowerCase(), uuid);
                }
            }
        }
    }

    /**
     * 记录或更新玩家数据
     */
    public void recordPlayer(String uuid, String name, String status) {
        playersData.set("players." + uuid + ".name", name);
        playersData.set("players." + uuid + ".status", status);
        List<String> names = playersData.getStringList("players." + uuid + ".names");
        if (!names.contains(name)) {
            names.add(name);
            playersData.set("players." + uuid + ".names", names);
        }
        savePlayersDataAsync();
        refreshNameToUUIDCache();
    }

    /**
     * 获取玩家状态文本（正版/盗版/未知）
     */
    public String getStatusText(String uuid) {
        return playersData.getString("players." + uuid + ".status", "未知");
    }

    /**
     * 获取玩家名
     */
    public String getName(String uuid) {
        return playersData.getString("players." + uuid + ".name", "未知");
    }

    /**
     * 判断玩家是否已记录
     */
    public boolean isPlayerRecorded(String uuid) {
        return playersData.isConfigurationSection("players." + uuid);
    }

    /**
     * 获取玩家数据配置对象
     */
    public FileConfiguration getPlayersData() {
        return playersData;
    }

    /**
     * 获取玩家名到UUID的缓存
     */
    public Map<String, String> getNameToUUIDCache() {
        return nameToUUID;
    }
}
