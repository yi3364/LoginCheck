package com.afeng.logincheck;

import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.UUID;

/**
 * 玩家加入监听器，负责身份验证、数据记录、权限分配和消息广播。
 */
public class PlayerJoinListener implements Listener {

    private final LoginCheck plugin;

    public PlayerJoinListener(LoginCheck plugin) {
        this.plugin = plugin;
    }

    /**
     * 占位符统一替换工具方法
     */
    private String replacePlaceholders(String msg, String player, String uuid, String status,
            String pluginName) {
        return msg.replace("%player%", player).replace("%uuid%", uuid).replace("%status%", status)
                .replace("%plugin%", pluginName);
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        String name = player.getName();
        UUID uuid = player.getUniqueId();
        long now = System.currentTimeMillis();

        FileConfiguration playersData = plugin.getPlayersData();
        FileConfiguration config = plugin.getConfig();
        String pluginName = plugin.getDescription().getName();

        // 以UUID为主key
        String path = "players." + uuid;
        boolean isFirstJoin = !playersData.contains(path);

        // 时间格式化
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String nowStr = sdf.format(new Date(now));

        // 异步验证正版身份
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            boolean isPremium = false;

            try {
                String urlStr = "https://sessionserver.mojang.com/session/minecraft/profile/"
                        + uuid.toString().replace("-", "");
                URL url = java.net.URI.create(urlStr).toURL();
                HttpURLConnection con = (HttpURLConnection) url.openConnection();
                con.setRequestMethod("GET");
                con.setConnectTimeout(3000);
                con.setReadTimeout(3000);

                if (con.getResponseCode() == 200) {
                    isPremium = true;
                }
            } catch (Exception e) {
                plugin.getLogger().warning("验证玩家正版状态失败：" + e.getMessage());
            }

            boolean finalIsPremium = isPremium;

            // 回到主线程处理数据和广播
            Bukkit.getScheduler().runTask(plugin, () -> {
                try {
                    // 以UUID为主key存储，玩家名、uuid、status、首次登录、最近登录
                    playersData.set(path + ".name", name);
                    String statusKey =
                            finalIsPremium ? "status-text.premium" : "status-text.cracked";
                    String statusText = config.getString(statusKey, finalIsPremium ? "正版" : "离线");
                    playersData.set(path + ".status", statusText);
                    playersData.set(path + ".last-login", nowStr);
                    if (isFirstJoin) {
                        playersData.set(path + ".first-login", nowStr);
                    }

                    // 曾用名处理（忽略大小写，避免重复）
                    List<String> names = playersData.getStringList(path + ".names");
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
                    if (names.size() > 1) { // 有曾用名才写入
                        playersData.set(path + ".names", names);
                    } else {
                        playersData.set(path + ".names", null);
                    }

                    plugin.savePlayersData();
                    plugin.refreshNameToUUIDCache(); // 保存后刷新缓存

                    // 广播消息
                    if (config.getBoolean("broadcast-enabled", true)) {
                        String broadcastMsgKey = finalIsPremium ? "messages.broadcast-premium"
                                : "messages.broadcast-cracked";
                        String msg =
                                config.getString(broadcastMsgKey, "玩家 %player% 上线，身份：%status%");
                        msg = replacePlaceholders(msg, name, uuid.toString(), statusText,
                                pluginName);
                        Bukkit.broadcastMessage(msg);
                    }

                    // 首次加入执行命令
                    if (isFirstJoin) {
                        String cmdKey = finalIsPremium ? "commands.premium" : "commands.cracked";
                        if (config.getBoolean(
                                "enable-commands." + (finalIsPremium ? "premium" : "cracked"),
                                true)) {
                            String command = config.getString(cmdKey, null);
                            if (command != null && !command.isEmpty()) {
                                command = replacePlaceholders(command, name, uuid.toString(),
                                        statusText, pluginName);
                                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command);
                            }
                        }
                    }
                } catch (Exception e) {
                    plugin.getLogger().severe("主线程处理玩家数据时出错：" + e.getMessage());
                }
            });
        });
    }
}
