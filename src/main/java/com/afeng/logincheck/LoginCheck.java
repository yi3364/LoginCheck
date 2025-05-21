package com.afeng.logincheck;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 插件主类，负责配置、数据加载与缓存、命令和监听器注册等。
 */
public final class LoginCheck extends JavaPlugin {

    private static LoginCheck instance;

    private File playersFile;
    private FileConfiguration playersData;

    // 玩家名到UUID的缓存
    private final Map<String, String> nameToUUID = new ConcurrentHashMap<>();

    @Override
    public void onEnable() {
        instance = this;

        saveDefaultConfig();
        loadPlayersData();

        // 注册监听器
        getServer().getPluginManager().registerEvents(new PlayerJoinListener(this), this);

        // 注册命令和补全
        LoginCheckCommand commandExecutor = new LoginCheckCommand(this);
        this.getCommand("logincheck").setExecutor(commandExecutor);
        this.getCommand("logincheck").setTabCompleter(commandExecutor);
        this.getCommand("lc").setExecutor(commandExecutor);
        this.getCommand("lc").setTabCompleter(commandExecutor);

        // 输出可配置的 banner
        FileConfiguration config = getConfig();
        if (config.isList("banner")) {
            for (String line : config.getStringList("banner")) {
                getLogger().info(line);
            }
        } else {
            getLogger().info("登录检查器插件已启用！");
        }
    }

    public static LoginCheck getInstance() {
        return instance;
    }

    /**
     * 加载 players.yml 数据文件，并自动添加注释头和刷新 name→uuid 缓存
     */
    public void loadPlayersData() {
        playersFile = new File(getDataFolder(), "players.yml");
        if (!playersFile.exists()) {
            try {
                playersFile.createNewFile();
                // 添加注释头
                YamlConfiguration temp = new YamlConfiguration();
                temp.options().header("玩家数据文件，请勿手动编辑！\n每个玩家以UUID为主键，包含name/status/首次登录/最近登录等信息。");
                temp.save(playersFile);
            } catch (IOException e) {
                getLogger().severe("无法创建 players.yml！");
            }
        }
        playersData = YamlConfiguration.loadConfiguration(playersFile);
        refreshNameToUUIDCache(); // 在加载后刷新缓存
    }

    /**
     * 获取 players.yml 配置对象
     */
    public FileConfiguration getPlayersData() {
        return playersData;
    }

    /**
     * 保存 players.yml 数据
     */
    public void savePlayersData() {
        try {
            if (playersData != null && playersFile != null) {
                playersData.save(playersFile);
                refreshNameToUUIDCache(); // 保存后刷新缓存
            }
        } catch (IOException e) {
            getLogger().severe("无法保存 players.yml！");
        }
    }

    /**
     * 获取玩家名到UUID的缓存
     */
    public Map<String, String> getNameToUUIDCache() {
        return nameToUUID;
    }

    /**
     * 刷新玩家名到UUID的缓存（遍历 players.yml）
     */
    public void refreshNameToUUIDCache() {
        nameToUUID.clear();
        if (playersData.isConfigurationSection("players")) {
            for (String uuid : playersData.getConfigurationSection("players").getKeys(false)) {
                String name = playersData.getString("players." + uuid + ".name", "");
                if (!name.isEmpty()) {
                    nameToUUID.put(name.toLowerCase(), uuid);
                }
            }
        }
    }
}
