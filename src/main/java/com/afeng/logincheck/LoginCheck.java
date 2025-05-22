package com.afeng.logincheck;

import org.bukkit.Bukkit;
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

    // 玩家名到UUID的缓存（小写名 -> uuid字符串）
    private final Map<String, String> nameToUUID = new ConcurrentHashMap<>();

    private FileConfiguration lang;

    private long lastSaveTime = 0;
    private static final long SAVE_INTERVAL = 5 * 60 * 1000; // 5分钟

    @Override
    public void onEnable() {
        instance = this;

        saveDefaultConfig();
        loadPlayersData();
        loadLang(); // 优先加载语言

        // 注册监听器
        getServer().getPluginManager().registerEvents(new PlayerJoinListener(this), this);

        // 注册命令和补全
        LoginCheckCommand commandExecutor = new LoginCheckCommand(this);
        this.getCommand("logincheck").setExecutor(commandExecutor);
        this.getCommand("logincheck").setTabCompleter(commandExecutor);
        this.getCommand("lc").setExecutor(commandExecutor);
        this.getCommand("lc").setTabCompleter(commandExecutor);

        // 输出可配置的 banner
        FileConfiguration lang = getLang();
        if (lang.isList("banner")) {
            for (String line : lang.getStringList("banner")) {
                getLogger().info(line);
            }
        } else {
            getLogger().info(lang.getString("plugin-enabled", "登录检查器插件已启用！"));
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
        boolean firstCreate = false;
        if (!playersFile.exists()) {
            try {
                playersFile.createNewFile();
                firstCreate = true;
            } catch (IOException e) {
                getLogger().severe("无法创建 players.yml：" + e.getMessage());
            }
        }
        if (firstCreate) {
            try (java.io.FileWriter fw = new java.io.FileWriter(playersFile)) {
                fw.write("# 玩家数据文件，请勿手动编辑！\n# 每个玩家以UUID为主键，包含name/status/首次登录/最近登录等信息。\n\n");
            } catch (IOException e) {
                getLogger().severe("无法写入 players.yml 注释：" + e.getMessage());
            }
        }
        playersData = YamlConfiguration.loadConfiguration(playersFile);
        refreshNameToUUIDCache();
    }

    /**
     * 保存 players.yml
     */
    public void savePlayersData() {
        try {
            playersData.save(playersFile);
        } catch (IOException e) {
            getLogger().severe("无法保存 players.yml：" + e.getMessage());
        }
    }

    /**
     * 异步保存 players.yml
     */
    public void savePlayersDataAsync() {
        Bukkit.getScheduler().runTaskAsynchronously(this, this::savePlayersData);
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

    public FileConfiguration getPlayersData() {
        return playersData;
    }

    public Map<String, String> getNameToUUIDCache() {
        return nameToUUID;
    }

    /**
     * 工具方法：加载任意yml文件
     */
    public FileConfiguration loadYaml(File file) {
        return YamlConfiguration.loadConfiguration(file);
    }

    /**
     * 加载语言文件，支持多语言
     */
    public void loadLang() {
        FileConfiguration config = getConfig();
        String langCode = config.getString("lang", "zh_CN").toLowerCase();
        String langFileName;
        switch (langCode) {
            case "en":
            case "en_us":
                langFileName = "messages_en.yml";
                break;
            case "zh":
            case "zh_cn":
            default:
                langFileName = "messages_zh.yml";
                break;
        }
        File langFile = new File(getDataFolder(), langFileName);
        if (!langFile.exists()) {
            saveResource(langFileName, false);
        }
        lang = YamlConfiguration.loadConfiguration(langFile);
    }

    public FileConfiguration getLang() {
        return lang;
    }

    public void trySavePlayersData() {
        long now = System.currentTimeMillis();
        if (now - lastSaveTime > SAVE_INTERVAL) {
            savePlayersData();
            lastSaveTime = now;
        }
    }
}
