package io.github.stone_brick.spotteddog.client.network;

import io.github.stone_brick.spotteddog.network.s2c.TeleportConfirmS2CPayload;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;

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
            if (player == null) return;

            if (payload.success()) {
                // 传送成功不显示消息
                return;
            } else {
                // 失败时显示错误消息
                String message = "[SpottedDog] 传送失败: " + payload.message();
                player.sendMessage(net.minecraft.text.Text.literal(message), false);
            }
        });
    }
}
