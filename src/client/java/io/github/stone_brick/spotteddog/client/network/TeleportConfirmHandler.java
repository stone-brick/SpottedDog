package io.github.stone_brick.spotteddog.client.network;

import io.github.stone_brick.spotteddog.network.s2c.TeleportConfirmS2CPayload;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.minecraft.text.Text;

/**
 * 客户端传送确认消息处理器。
 */
@Environment(EnvType.CLIENT)
public class TeleportConfirmHandler {

    /**
     * 注册客户端 S2C 处理器。
     * 在单人模式下，服务端可能已注册过，所以需要动态处理。
     */
    public static void register() {
        // 动态注册 S2C 负载类型（单人模式需避免重复注册）
        try {
            PayloadTypeRegistry.playS2C().register(
                TeleportConfirmS2CPayload.ID,
                TeleportConfirmS2CPayload.CODEC
            );
        } catch (IllegalArgumentException e) {
            // 已注册过，忽略
        }

        // 注册接收处理器
        ClientPlayNetworking.registerGlobalReceiver(TeleportConfirmS2CPayload.ID, (payload, context) -> {
            var player = context.player();

            if (payload.success()) {
                // 传送成功不显示消息
                if ("public".equals(payload.type())) {
                    // 公开 Spot 成功
                    player.sendMessage(Text.translatable("spotteddog.public.published", payload.targetName()), false);
                } else if ("unpublic".equals(payload.type())) {
                    // 取消公开 Spot 成功
                    player.sendMessage(Text.translatable("spotteddog.public.unpublished", payload.targetName()), false);
                }
                return;
            } else {
                // 失败时显示错误消息
                String message = payload.message();
                Text messageText;
                if (message != null && message.startsWith("spotteddog.")) {
                    // 翻译键，使用客户端语言翻译
                    messageText = Text.translatable(message);
                } else {
                    // 普通错误消息，直接显示
                    messageText = Text.literal(message != null ? message : "");
                }
                player.sendMessage(Text.translatable("spotteddog.teleport.failed.message",
                        Text.translatable(getActionMessageKey(payload.type())), messageText), false);
            }
        });
    }

    private static String getActionMessageKey(String type) {
        return switch (type) {
            case "public" -> "spotteddog.action.publish";
            case "unpublic" -> "spotteddog.action.unpublish";
            default -> "spotteddog.action.teleport"; // spot, spawn, respawn, death, public_tp 等传送操作
        };
    }
}
