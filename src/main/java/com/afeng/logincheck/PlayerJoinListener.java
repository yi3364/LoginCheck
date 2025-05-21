package com.afeng.logincheck;

import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

import java.io.InputStreamReader;
import java.io.BufferedReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.UUID;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

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

        FileConfiguration playersData = plugin.getPlayersData();

        boolean isFirstJoin = !playersData.contains("players." + name);

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            boolean isPremium = false;

            try {
                String urlStr = "https://sessionserver.mojang.com/session/minecraft/profile/" + uuid.toString().replace("-", "");
                URL url = java.net.URI.create(urlStr).toURL();
                HttpURLConnection con = (HttpURLConnection) url.openConnection();
                con.setRequestMethod("GET");

                if (con.getResponseCode() == 200) {
                    BufferedReader reader = new BufferedReader(new InputStreamReader(con.getInputStream()));
                    JSONParser parser = new JSONParser();
                    JSONObject response = (JSONObject) parser.parse(reader);
                    String officialName = (String) response.get("name");
                    isPremium = officialName.equalsIgnoreCase(name);
                }
            } catch (Exception e) {
                plugin.getLogger().warning("验证玩家正版状态失败：" + e.getMessage());
            }

            boolean finalIsPremium = isPremium;

            Bukkit.getScheduler().runTask(plugin, () -> {
                playersData.set("players." + name + ".uuid", uuid.toString());
                playersData.set("players." + name + ".premium", finalIsPremium);
                String statusKey = finalIsPremium ? "status-text.premium" : "status-text.cracked";
                String statusText = plugin.getConfig().getString(statusKey, finalIsPremium ? "正版" : "盗版");
                playersData.set("players." + name + ".status", statusText);
                plugin.savePlayersData();

                plugin.getLogger().info("玩家 " + name + " 上线，身份：" + statusText);

                if (plugin.getConfig().getBoolean("broadcast-enabled", true)) {
                    String joinMessageKey = finalIsPremium ? "messages.broadcast-premium" : "messages.broadcast-cracked";
                    String joinMessage = plugin.getConfig().getString(joinMessageKey, "").replace("%player%", name);
                    if (!joinMessage.isEmpty()) {
                        for (Player p : Bukkit.getOnlinePlayers()) {
                            if (p.hasPermission("minecraft.broadcast.user")) {
                                p.sendMessage(joinMessage);
                            }
                        }
                    }
                }

                if (isFirstJoin) {
                    boolean enablePremium = plugin.getConfig().getBoolean("enable-commands.premium", true);
                    boolean enableCracked = plugin.getConfig().getBoolean("enable-commands.cracked", true);
                    String command = null;

                    if (finalIsPremium && enablePremium) {
                        command = plugin.getConfig().getString("commands.premium");
                    } else if (!finalIsPremium && enableCracked) {
                        command = plugin.getConfig().getString("commands.cracked");
                    }

                    if (command != null && !command.isEmpty()) {
                        String finalCommand = command.replace("%player%", name);
                        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), finalCommand);
                    }
                }
            });
        });
    }
}