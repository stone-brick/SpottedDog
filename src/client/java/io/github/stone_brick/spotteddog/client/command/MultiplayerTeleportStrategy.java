package io.github.stone_brick.spotteddog.client.command;

import io.github.stone_brick.spotteddog.client.data.Spot;
import io.github.stone_brick.spotteddog.network.c2s.TeleportRequestC2SPayload;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.network.ClientPlayerEntity;

/**
 * 多人模式传送策略实现。
 */
@Environment(EnvType.CLIENT)
public class MultiplayerTeleportStrategy implements TeleportStrategy {

    @Override
    public void teleportToSpot(ClientPlayerEntity player, Spot spot) {
        sendTeleportRequest(player, new TeleportRequestC2SPayload(
                "spot", spot.getName(),
                spot.getX(), spot.getY(), spot.getZ(), spot.getDimension()));
    }

    @Override
    public void teleportToSpawn(ClientPlayerEntity player) {
        sendTeleportRequest(player, new TeleportRequestC2SPayload(
                "spawn", "spawn", 0, 64, 0, "minecraft:overworld"));
    }

    @Override
    public void teleportToDeath(ClientPlayerEntity player) {
        // 发送请求，让服务端获取死亡点位置
        sendTeleportRequest(player, new TeleportRequestC2SPayload(
                "death", "death", 0, 0, 0, ""));
    }

    @Override
    public boolean teleportToRespawn(ClientPlayerEntity player) {
        // 发送请求，让服务端获取重生点位置
        sendTeleportRequest(player, new TeleportRequestC2SPayload(
                "respawn", "respawn", 0, 0, 0, ""));
        return true;
    }

    /**
     * 向服务端发送传送请求。
     */
    private void sendTeleportRequest(ClientPlayerEntity player, TeleportRequestC2SPayload payload) {
        ClientPlayNetworking.send(payload);
    }
}
