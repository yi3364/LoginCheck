package com.afeng.logincheck.listener;

import com.afeng.logincheck.LoginCheck;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.server.ServerListPingEvent;

import java.util.*;

public class VanishListener implements Listener, TabCompleter {
    private final LoginCheck plugin;

    public VanishListener(LoginCheck plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onServerListPing(ServerListPingEvent event) {
        // 移除拥有 logincheck.vanish 权限的玩家，使其不显示在服务器 MOTD 玩家列表
        event.iterator().forEachRemaining(profile -> {
            Player player = Bukkit.getPlayer(profile.getUniqueId());
            if (player != null && player.hasPermission("logincheck.vanish")) {
                event.iterator().remove();
            }
        });
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player joined = event.getPlayer();
        // 隐藏所有已隐身玩家
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (p.hasPermission("logincheck.vanish")) {
                joined.hidePlayer(p);
            }
        }
        // 如果自己有权限，隐藏自己给所有人
        if (joined.hasPermission("logincheck.vanish")) {
            for (Player p : Bukkit.getOnlinePlayers()) {
                p.hidePlayer(joined);
            }
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias,
            String[] args) {
        if (args.length == 1) {
            return Arrays.asList("check", "reload");
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("check")) {
            Set<String> names = new HashSet<>();
            // 在线玩家过滤隐身
            for (Player player : Bukkit.getOnlinePlayers()) {
                if (!player.hasPermission("logincheck.vanish")) {
                    names.add(player.getName());
                }
            }
            // 离线玩家（历史数据）过滤隐身
            FileConfiguration data = plugin.getPlayerDataManager().getPlayersData();
            if (data.isConfigurationSection("players")) {
                for (String uuid : data.getConfigurationSection("players").getKeys(false)) {
                    String name = data.getString("players." + uuid + ".name", uuid);
                    Player online = Bukkit.getPlayerExact(name);
                    if (online != null && online.hasPermission("logincheck.vanish"))
                        continue;
                    names.add(name);
                }
            }
            return new ArrayList<>(names);
        }
        return Collections.emptyList();
    }
}
