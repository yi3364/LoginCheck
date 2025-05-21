package com.afeng.logincheck;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;

public final class LoginCheck extends JavaPlugin {

    private static LoginCheck instance;

    private File playersFile;
    private FileConfiguration playersData;

    @Override
    public void onEnable() {
        instance = this;

        saveDefaultConfig();
        loadPlayersData();

        // 注册监听器
        getServer().getPluginManager().registerEvents(new PlayerJoinListener(this), this);

        // 注册命令
        this.getCommand("logincheck").setExecutor(new LoginCheckCommand(this));
        this.getCommand("lc").setExecutor(new LoginCheckCommand(this));

        getLogger().info("登录检查器插件已启用！");
    }

    public static LoginCheck getInstance() {
        return instance;
    }

    public void loadPlayersData() {
        playersFile = new File(getDataFolder(), "players.yml");
        if (!playersFile.exists()) {
            try {
                playersFile.createNewFile();
            } catch (IOException e) {
                getLogger().severe("无法创建 players.yml！");
            }
        }
        playersData = YamlConfiguration.loadConfiguration(playersFile);
    }

    public FileConfiguration getPlayersData() {
        return playersData;
    }

    public void savePlayersData() {
        try {
            if (playersData != null && playersFile != null) {
                playersData.save(playersFile);
            }
        } catch (IOException e) {
            getLogger().severe("无法保存 players.yml！");
        }
    }
}
