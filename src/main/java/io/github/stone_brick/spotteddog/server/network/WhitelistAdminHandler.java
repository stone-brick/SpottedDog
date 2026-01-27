package io.github.stone_brick.spotteddog.server.network;

import io.github.stone_brick.spotteddog.network.c2s.WhitelistAdminC2SPayload;
import io.github.stone_brick.spotteddog.server.permission.PermissionManager;
import io.github.stone_brick.spotteddog.server.permission.WhitelistManager;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

import java.util.UUID;

/**
 * 服务端白名单管理处理器。
 */
public class WhitelistAdminHandler {

    static {
        // 注册 C2S Payload 类型
        PayloadTypeRegistry.playC2S().register(WhitelistAdminC2SPayload.ID, WhitelistAdminC2SPayload.CODEC);
    }

    /**
     * 注册所有处理器。
     */
    public static void register() {
        // 处理白名单操作请求
        ServerPlayNetworking.registerGlobalReceiver(WhitelistAdminC2SPayload.ID, (payload, context) -> {
            ServerPlayerEntity player = context.player();
            MinecraftServer server = player.getEntityWorld().getServer();

            // 验证是否是 OP
            if (!PermissionManager.hasAdminPermission(player)) {
                player.sendMessage(Text.literal("[SpottedDog] 权限不足，需要 OP 权限"));
                return;
            }

            String playerName = payload.playerName();
            WhitelistManager.WhitelistType type = payload.type();
            boolean add = payload.add();

            // 查找在线玩家 UUID
            UUID targetUuid = WhitelistManager.findOnlinePlayerUuid(server, playerName);

            if (targetUuid == null) {
                player.sendMessage(Text.literal("[SpottedDog] 找不到在线玩家: " + playerName));
                return;
            }

            // 1. 禁止 OP 将自己添加到白名单
            if (add && targetUuid.equals(player.getUuid())) {
                player.sendMessage(Text.literal("[SpottedDog] 不能将自己添加到白名单"));
                return;
            }

            // 2. 禁止 OP 将自己从白名单移除
            if (!add && targetUuid.equals(player.getUuid())) {
                player.sendMessage(Text.literal("[SpottedDog] 不能从白名单中移除自己"));
                return;
            }

            // 3. 禁止修改其他 OP 的白名单状态
            ServerPlayerEntity targetPlayer = server.getPlayerManager().getPlayer(playerName);
            if (targetPlayer != null && PermissionManager.hasAdminPermission(targetPlayer) && !targetUuid.equals(player.getUuid())) {
                player.sendMessage(Text.literal("[SpottedDog] 不能修改其他 OP 的白名单状态"));
                return;
            }

            // 执行操作
            if (add) {
                boolean added = WhitelistManager.addPlayerToWhitelist(targetUuid, playerName, type);
                if (added) {
                    player.sendMessage(Text.literal("[SpottedDog] 已将 " + playerName + " 添加到 " + getTypeName(type) + " 白名单"));
                } else {
                    player.sendMessage(Text.literal("[SpottedDog] " + playerName + " 已在白名单中（已更新名称）"));
                }
            } else {
                boolean removed = WhitelistManager.removePlayerFromWhitelist(targetUuid, type);
                if (removed) {
                    player.sendMessage(Text.literal("[SpottedDog] 已将 " + playerName + " 从 " + getTypeName(type) + " 白名单移除"));
                } else {
                    player.sendMessage(Text.literal("[SpottedDog] " + playerName + " 不在白名单中"));
                }
            }
        });
    }

    /**
     * 获取白名单类型的中文名称。
     */
    private static String getTypeName(WhitelistManager.WhitelistType type) {
        return switch (type) {
            case TELEPORT -> "传送";
            case PUBLIC_SPOT -> "公开 Spot";
            case PUBLIC_SPOT_TELEPORT -> "公开 Spot 传送";
        };
    }
}
