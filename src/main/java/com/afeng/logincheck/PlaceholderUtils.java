package com.afeng.logincheck;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class PlaceholderUtils {
    @NotNull
    public static String replacePlaceholders(@Nullable String msg, @Nullable String player,
            @Nullable String uuid, @Nullable String status, @Nullable String pluginName) {
        if (msg == null)
            return "";
        return msg.replace("%player%", player == null ? "" : player)
                .replace("%uuid%", uuid == null ? "" : uuid)
                .replace("%status%", status == null ? "" : status)
                .replace("%plugin%", pluginName == null ? "" : pluginName);
    }
}
