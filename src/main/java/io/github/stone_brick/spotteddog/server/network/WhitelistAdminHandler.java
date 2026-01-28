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
                player.sendMessage(Text.translatable("spotteddog.whitelist.permission.denied"));
                return;
            }

            String playerName = payload.playerName();
            WhitelistManager.WhitelistType type = payload.type();
            boolean add = payload.add();

            // 查找在线玩家 UUID
            UUID targetUuid = WhitelistManager.findOnlinePlayerUuid(server, playerName);

            if (targetUuid == null) {
                player.sendMessage(Text.translatable("spotteddog.whitelist.player.not.found", playerName));
                return;
            }

            // 1. 禁止 OP 将自己添加到白名单
            if (add && targetUuid.equals(player.getUuid())) {
                player.sendMessage(Text.translatable("spotteddog.whitelist.cannot.add.self"));
                return;
            }

            // 2. 禁止 OP 将自己从白名单移除
            if (!add && targetUuid.equals(player.getUuid())) {
                player.sendMessage(Text.translatable("spotteddog.whitelist.cannot.remove.self"));
                return;
            }

            // 3. 禁止修改其他 OP 的白名单状态
            ServerPlayerEntity targetPlayer = server.getPlayerManager().getPlayer(playerName);
            if (targetPlayer != null && PermissionManager.hasAdminPermission(targetPlayer) && !targetUuid.equals(player.getUuid())) {
                player.sendMessage(Text.translatable("spotteddog.whitelist.cannot.modify.op"));
                return;
            }

            // 执行操作
            if (add) {
                boolean added = WhitelistManager.addPlayerToWhitelist(targetUuid, playerName, type);
                if (added) {
                    player.sendMessage(Text.translatable("spotteddog.whitelist.added.success", playerName, getTypeNameKey(type)));
                } else {
                    player.sendMessage(Text.translatable("spotteddog.whitelist.already.in", playerName));
                }
            } else {
                boolean removed = WhitelistManager.removePlayerFromWhitelist(targetUuid, type);
                if (removed) {
                    player.sendMessage(Text.translatable("spotteddog.whitelist.removed.success", playerName, getTypeNameKey(type)));
                } else {
                    player.sendMessage(Text.translatable("spotteddog.whitelist.not.in", playerName));
                }
            }
        });
    }

    /**
     * 获取白名单类型的本地化键名。
     */
    private static String getTypeNameKey(WhitelistManager.WhitelistType type) {
        return switch (type) {
            case TELEPORT -> "spotteddog.whitelist.type.teleport";
            case PUBLIC_SPOT -> "spotteddog.whitelist.type.public";
            case PUBLIC_SPOT_TELEPORT -> "spotteddog.whitelist.type.publictp";
        };
    }
}
