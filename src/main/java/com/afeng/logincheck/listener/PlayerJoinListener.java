package com.afeng.logincheck.listener;

import com.afeng.logincheck.LoginCheck;
import com.afeng.logincheck.util.PlayerDataManager;
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

public class PlayerJoinListener implements Listener {
    private final LoginCheck plugin;

    public PlayerJoinListener(LoginCheck plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        // 拥有 logincheck.silentjoin 权限的玩家上线不广播，且不执行后续逻辑
        if (player.hasPermission("logincheck.silentjoin")) {
            event.setJoinMessage(null);
            return;
        }

        String name = player.getName();
        UUID uuid = player.getUniqueId();
        long now = System.currentTimeMillis();

        FileConfiguration playersData = plugin.getPlayerDataManager().getPlayersData();
        FileConfiguration config = plugin.getConfig();
        String pluginName = plugin.getDescription().getName();

        String path = "players." + uuid;
        boolean isFirstJoin = !playersData.contains(path);

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String nowStr = sdf.format(new Date(now));

        // 异步只做一次正版验证，失败直接视为盗版
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
            } catch (Exception ignored) {
                // 验证失败直接视为盗版
            }
            boolean finalIsPremium = isPremium;

            // 回主线程处理数据和广播
            Bukkit.getScheduler().runTask(plugin, () -> {
                playersData.set(path + ".name", name);
                String statusKey = finalIsPremium ? "status-text-premium" : "status-text-cracked";
                String statusText =
                        plugin.getMessages().getString(statusKey, finalIsPremium ? "正版" : "离线");
                playersData.set(path + ".status", statusText);
                playersData.set(path + ".last-login", nowStr);
                if (isFirstJoin) {
                    playersData.set(path + ".first-login", nowStr);
                }

                // 曾用名处理
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
                playersData.set(path + ".names", names); // 始终保存列表

                plugin.getPlayerDataManager().savePlayersData();
                plugin.getPlayerDataManager().refreshNameToUUIDCache();

                // 广播消息（无论正版/盗版都广播）
                if (config.getBoolean("broadcast-enabled", true)) {
                    String broadcastMsgKey =
                            finalIsPremium ? "broadcast-premium" : "broadcast-cracked";
                    String msg = plugin.getMessages().getString(broadcastMsgKey,
                            "玩家 %player% 上线，身份：%status%");
                    msg = msg.replace("%player%", name).replace("%uuid%", uuid.toString())
                            .replace("%status%", statusText).replace("%plugin%", pluginName);
                    Bukkit.broadcastMessage(org.bukkit.ChatColor.translateAlternateColorCodes('&',
                            plugin.getMessages().getString("prefix", "") + msg));
                }

                // 首次加入执行命令
                if (isFirstJoin) {
                    String cmdKey = finalIsPremium ? "commands.premium" : "commands.cracked";
                    if (config.getBoolean(
                            "enable-commands." + (finalIsPremium ? "premium" : "cracked"), true)) {
                        String command = config.getString(cmdKey, null);
                        if (command != null && !command.isEmpty()) {
                            command = command.replace("%player%", name)
                                    .replace("%uuid%", uuid.toString())
                                    .replace("%status%", statusText)
                                    .replace("%plugin%", pluginName);
                            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command);
                        }
                    }
                }
            });
        });
    }
}
