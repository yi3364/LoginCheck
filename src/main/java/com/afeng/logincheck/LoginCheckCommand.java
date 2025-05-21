package com.afeng.logincheck;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public class LoginCheckCommand implements CommandExecutor, TabCompleter {

    private final LoginCheck plugin;

    public LoginCheckCommand(LoginCheck plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length != 1) {
            sender.sendMessage("§c用法: /lc check <玩家名>");
            return true;
        }

        String name = args[0];
        FileConfiguration data = plugin.getPlayersData();

        if (!data.contains("players." + name)) {
            sender.sendMessage("§e玩家 " + name + " 没有记录。");
            return true;
        }

        String status = data.getString("players." + name + ".status", "未知");
        sender.sendMessage("§a玩家 " + name + " 身份：§b" + status);
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();
        if (args.length == 1) {
            for (Player player : Bukkit.getOnlinePlayers()) {
                completions.add(player.getName());
            }
        }
        return completions;
    }
}