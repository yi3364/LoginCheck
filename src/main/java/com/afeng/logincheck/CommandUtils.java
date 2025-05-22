package com.afeng.logincheck;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;

public class CommandUtils {
    /**
     * 分页获取子列表
     */
    @NotNull
    public static <T> List<T> getPage(@Nullable List<T> list, int page, int pageSize) {
        if (list == null || list.isEmpty() || pageSize <= 0)
            return Collections.emptyList();
        int total = list.size();
        int totalPages = Math.max(1, (int) Math.ceil(total * 1.0 / pageSize));
        if (page < 1)
            page = 1;
        if (page > totalPages)
            page = totalPages;
        int from = (page - 1) * pageSize;
        int to = Math.min(from + pageSize, total);
        if (from >= total || from < 0 || to < 0 || from > to)
            return Collections.emptyList();
        return list.subList(from, to);
    }

    /**
     * 构建 tellraw 按钮 JSON
     */
    @NotNull
    public static String buildTellrawButton(@NotNull String name, @NotNull String status,
            @NotNull String uuid) {
        return "[{\"text\":\"§b" + name
                + "\",\"hoverEvent\":{\"action\":\"show_text\",\"value\":\"身份: " + status
                + "\\nUUID: " + uuid
                + "\"},\"clickEvent\":{\"action\":\"run_command\",\"value\":\"/lc check " + name
                + "\"}}]";
    }
}
