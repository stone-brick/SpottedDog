package io.github.stone_brick.spotteddog.server.network;

import io.github.stone_brick.spotteddog.network.c2s.TeleportLogAdminC2SPayload;
import io.github.stone_brick.spotteddog.server.data.TeleportLog;
import io.github.stone_brick.spotteddog.server.data.TeleportLogManager;
import io.github.stone_brick.spotteddog.server.permission.PermissionManager;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

/**
 * 服务端传送日志管理处理器。
 * 处理日志查看请求。
 *
 * <p>注意：清除日志功能已被禁止，日志只能查看无法清除。</p>
 */
public class TeleportLogHandler {

    /**
     * 注册日志管理处理器。
     */
    public static void register() {
        // 注册 C2S Payload 类型
        PayloadTypeRegistry.playC2S().register(TeleportLogAdminC2SPayload.ID, TeleportLogAdminC2SPayload.CODEC);

        ServerPlayNetworking.registerGlobalReceiver(TeleportLogAdminC2SPayload.ID, (payload, context) -> {
            ServerPlayerEntity player = context.player();

            // 验证是否是 OP
            if (!PermissionManager.hasAdminPermission(player)) {
                player.sendMessage(Text.translatable("spotteddog.log.permission.denied"));
                return;
            }

            TeleportLogManager manager = TeleportLogManager.getInstance();

            switch (payload.action()) {
                case "list" -> {
                    int count = Math.min(payload.count(), 100);
                    listTeleportLogs(player, manager, count);
                }
                // clear_logs 操作已被禁止，不允许任何人清除日志
            }
        });
    }

    /**
     * 列出传送日志。
     */
    private static void listTeleportLogs(ServerPlayerEntity player, TeleportLogManager manager, int count) {
        java.util.List<TeleportLog> logs = manager.getRecentLogs(count);

        player.sendMessage(Text.translatable("spotteddog.log.list.header", logs.size()));

        if (logs.isEmpty()) {
            player.sendMessage(Text.translatable("spotteddog.log.empty"));
            return;
        }

        for (TeleportLog log : logs) {
            String spotInfo = log.getSpotName() != null ? log.getSpotName() : log.getTeleportType();
            String message = String.format("[%s] %s %s -> (%s, %.1f, %.1f, %.1f)",
                    log.getTimestamp().substring(11, 19),  // 只显示 HH:mm:ss
                    log.getPlayerName(),
                    spotInfo,
                    log.getTargetDimension(),
                    log.getTargetX(), log.getTargetY(), log.getTargetZ());
            player.sendMessage(Text.literal(message));
        }
    }
}
