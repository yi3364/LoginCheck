package com.afeng.logincheck;

import com.afeng.logincheck.command.LoginCheckCommand;
import com.afeng.logincheck.listener.PlayerJoinListener;
import com.afeng.logincheck.util.PlayerDataManager;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.List;

/**
 * 插件主类，只负责生命周期、配置、命令和监听器注册
 */
public final class LoginCheck extends JavaPlugin {

    private static LoginCheck instance;
    private PlayerDataManager playerDataManager;
    private YamlConfiguration messages;

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();
        loadMessages();

        // 初始化玩家数据管理器
        playerDataManager = new PlayerDataManager(this);

        // 注册监听器
        getServer().getPluginManager().registerEvents(new PlayerJoinListener(this), this);

        // 注册命令和补全
        LoginCheckCommand commandExecutor = new LoginCheckCommand(this);
        if (getCommand("logincheck") != null) {
            getCommand("logincheck").setExecutor(commandExecutor);
            getCommand("logincheck").setTabCompleter(commandExecutor);
        }
        if (getCommand("lc") != null) {
            getCommand("lc").setExecutor(commandExecutor);
            getCommand("lc").setTabCompleter(commandExecutor);
        }

        // 广播 banner 横幅
        List<String> banner = messages.getStringList("banner");
        if (banner != null && !banner.isEmpty()) {
            for (String line : banner) {
                getServer().getConsoleSender().sendMessage(line);
            }
            // 也可以广播给所有在线玩家
            // for (String line : banner) {
            //     getServer().broadcastMessage(line);
            // }
        }

        getLogger().info("LoginCheck 插件已启用！");
    }

    @Override
    public void onDisable() {
        getLogger().info("LoginCheck 插件已卸载！");
    }

    public static LoginCheck getInstance() {
        return instance;
    }

    public PlayerDataManager getPlayerDataManager() {
        return playerDataManager;
    }

    public void loadMessages() {
        String lang = getConfig().getString("lang", "zh");
        File file = new File(getDataFolder(), "messages_" + lang + ".yml");
        if (!file.exists()) {
            saveResource("messages_" + lang + ".yml", false);
        }
        messages = YamlConfiguration.loadConfiguration(file);
    }

    public String getMessage(String key) {
        String prefix = messages.getString("prefix", "");
        String msg = messages.getString(key, "&c消息缺失: " + key);
        return org.bukkit.ChatColor.translateAlternateColorCodes('&', prefix + msg);
    }

    public YamlConfiguration getMessages() {
        return messages;
    }

    @Override
    public void reloadConfig() {
        super.reloadConfig();
        loadMessages();
    }
}