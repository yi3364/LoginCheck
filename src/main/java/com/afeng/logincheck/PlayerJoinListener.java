package com.afeng.logincheck;

import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.jetbrains.annotations.NotNull;

import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import net.kyori.adventure.text.Component;

/**
 * 玩家加入监听器，负责身份验证、数据记录、权限分配和消息广播。
 */
public class PlayerJoinListener implements Listener {

    private final LoginCheck plugin;

    public PlayerJoinListener(LoginCheck plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        String name = player.getName();
        UUID uuid = player.getUniqueId();
        long now = System.currentTimeMillis();

        FileConfiguration playersData = plugin.getPlayersData();
        FileConfiguration config = plugin.getConfig();
        FileConfiguration lang = plugin.getLang();
        String pluginName = plugin.getName();

        String path = "players." + uuid;
        boolean isFirstJoin = !playersData.contains(path);

        String nowStr = formatNow(now);

        // 异步验证正版身份
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            boolean isPremium = checkPremiumWithRetry(uuid);
            if (!isPremium) {
                plugin.getLogger().warning("验证玩家正版状态失败（已重试）：" + uuid);
            }
            // 回到主线程处理数据和广播
            Bukkit.getScheduler().runTask(plugin, () -> handlePlayerJoin(playersData, config, lang,
                    path, name, uuid, nowStr, isFirstJoin, isPremium, pluginName));
        });
    }

    /**
     * 格式化当前时间
     */
    private @NotNull String formatNow(long now) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        return sdf.format(new Date(now));
    }

    /**
     * 带重试的正版验证
     */
    private boolean checkPremiumWithRetry(@NotNull UUID uuid) {
        int retry = 0;
        final int maxRetry = 2;
        while (retry <= maxRetry) {
            try {
                String urlStr = "https://sessionserver.mojang.com/session/minecraft/profile/"
                        + uuid.toString().replace("-", "");
                URL url = java.net.URI.create(urlStr).toURL();
                HttpURLConnection con = (HttpURLConnection) url.openConnection();
                con.setRequestMethod("GET");
                con.setConnectTimeout(3000);
                con.setReadTimeout(3000);

                if (con.getResponseCode() == 200) {
                    return true;
                }
            } catch (Exception e) {
                retry++;
                try {
                    Thread.sleep(500);
                } catch (InterruptedException ignored) {
                }
            }
        }
        return false;
    }

    /**
     * 主线程处理玩家数据、广播和命令
     */
    private void handlePlayerJoin(@NotNull FileConfiguration playersData,
            @NotNull FileConfiguration config, @NotNull FileConfiguration lang,
            @NotNull String path, @NotNull String name, @NotNull UUID uuid, @NotNull String nowStr,
            boolean isFirstJoin, boolean isPremium, @NotNull String pluginName) {
        try {
            // 以UUID为主key存储，玩家名、uuid、status、首次登录、最近登录
            playersData.set(path + ".name", name);
            String statusText = lang.getString(isPremium ? "status-text-premium" : "status-text-cracked", isPremium ? "§b 正版 " : "§7 离线 ");
            playersData.set(path + ".status", statusText);
            playersData.set(path + ".last-login", nowStr);
            if (isFirstJoin) {
                playersData.set(path + ".first-login", nowStr);
            }

            // 曾用名处理（getStringList 若无字段会返回空列表，不会 NPE）
            List<String> names = playersData.getStringList(path + ".names");
            names = updatePlayerNames(names, name);
            if (names.size() > 1) {
                playersData.set(path + ".names", names);
            } else {
                playersData.set(path + ".names", null);
            }

            // plugin.savePlayersDataAsync();
            plugin.trySavePlayersData();

            plugin.refreshNameToUUIDCache();

            // 广播消息
            if (config.getBoolean("broadcast-enabled", true)) {
                broadcastJoinMessage(lang, isPremium, name, uuid, statusText, pluginName);
            }

            // 首次加入执行命令
            if (isFirstJoin) {
                executeFirstJoinCommand(config, lang, isPremium, name, uuid, statusText,
                        pluginName);
            }

            // 新增国际化消息时，务必先在 lang 文件中添加对应 key
            String msg = lang.getString("custom-welcome", "欢迎 %player% 加入服务器！");
            msg = PlaceholderUtils.replacePlaceholders(msg, name, uuid.toString(), statusText,
                    pluginName);
            try {
                // 推荐使用 Adventure API 广播，Paper 1.16+ 原生支持
                Bukkit.getServer().broadcast(Component.text(msg));
            } catch (Exception e) {
                // 广播异常
                plugin.getLogger().warning(
                        lang.getString("error-broadcast", "[LoginCheck] 广播消息异常：") + e.getMessage());
            }
        } catch (Exception e) {
            // 玩家数据写入异常
            plugin.getLogger().warning(
                    lang.getString("error-save", "[LoginCheck] 玩家数据保存异常：") + e.getMessage());
        }
    }

    /**
     * 广播玩家加入消息
     */
    private void broadcastJoinMessage(@NotNull FileConfiguration lang, boolean isPremium,
            @NotNull String name, @NotNull UUID uuid, @NotNull String statusText,
            @NotNull String pluginName) {
        String broadcastMsgKey = isPremium ? "broadcast-premium" : "broadcast-cracked";
        String msg = lang.getString(broadcastMsgKey, "玩家 §6%player% §a使用%status%§a登入服务器...");
        msg = PlaceholderUtils.replacePlaceholders(msg, name, uuid.toString(), statusText,
                pluginName);
        try {
            Bukkit.getServer().broadcast(Component.text(msg));
        } catch (Exception e) {
            // 广播异常
            plugin.getLogger().warning(
                    lang.getString("error-broadcast", "[LoginCheck] 广播消息异常：") + e.getMessage());
        }
    }

    /**
     * 首次加入执行命令
     */
    private void executeFirstJoinCommand(@NotNull FileConfiguration config,
            @NotNull FileConfiguration lang, boolean isPremium, @NotNull String name,
            @NotNull UUID uuid, @NotNull String statusText, @NotNull String pluginName) {
        String cmdKey = isPremium ? "commands.premium" : "commands.cracked";
        // 读取命令开关，若配置缺失则默认启用
        if (config.getBoolean("enable-commands." + (isPremium ? "premium" : "cracked"), true)) {
            String command = config.getString(cmdKey, null);
            if (command != null && !command.isEmpty()) {
                command = PlaceholderUtils.replacePlaceholders(command, name, uuid.toString(),
                        statusText, pluginName);
                try {
                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command);
                } catch (Exception e) {
                    // 命令执行异常
                    plugin.getLogger()
                            .warning(lang.getString("error-command", "[LoginCheck] 命令执行异常：")
                                    + e.getMessage());
                }
            }
        }
    }

    /**
     * 处理玩家曾用名列表，避免重复，返回处理后的列表
     */
    private @NotNull List<String> updatePlayerNames(@NotNull List<String> names,
            @NotNull String name) {
        boolean exists = false;
        for (String oldName : names) {
            if (oldName.equalsIgnoreCase(name)) {
                exists = true;
                break;
            }
        }
        if (!exists) {
            names.add(name);
        }
        return names;
    }
}
