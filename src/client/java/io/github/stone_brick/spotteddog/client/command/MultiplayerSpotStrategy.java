package io.github.stone_brick.spotteddog.client.command;

import io.github.stone_brick.spotteddog.client.data.Spot;
import io.github.stone_brick.spotteddog.client.network.PublicSpotListHandler;
import io.github.stone_brick.spotteddog.network.c2s.PublicSpotActionC2SPayload;
import io.github.stone_brick.spotteddog.network.c2s.PublicSpotTeleportC2SPayload;
import io.github.stone_brick.spotteddog.network.c2s.TeleportLogAdminC2SPayload;
import io.github.stone_brick.spotteddog.network.c2s.TeleportRequestC2SPayload;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;

import java.util.List;
import net.minecraft.util.WorldSavePath;

/**
 * 多人模式传送策略实现。
 */
@Environment(EnvType.CLIENT)
public class MultiplayerSpotStrategy implements SpotStrategy {

    @Override
    public void teleportToSpot(ClientPlayerEntity player, Spot spot) {
        sendTeleportRequest(player, new TeleportRequestC2SPayload(
                "spot", spot.getName(),
                spot.getX(), spot.getY(), spot.getZ(),
                spot.getYaw(), spot.getPitch(), spot.getDimension()));
    }

    @Override
    public void teleportToSpawn(ClientPlayerEntity player) {
        // 发送当前玩家的 yaw/pitch，服务端会使用它
        sendTeleportRequest(player, new TeleportRequestC2SPayload(
                "spawn", "spawn", 0, 64, 0, player.getYaw(), player.getPitch(), "minecraft:overworld"));
    }

    @Override
    public void teleportToDeath(ClientPlayerEntity player) {
        // 发送当前玩家的 yaw/pitch，服务端会使用它
        sendTeleportRequest(player, new TeleportRequestC2SPayload(
                "death", "death", 0, 0, 0, player.getYaw(), player.getPitch(), ""));
    }

    @Override
    public boolean teleportToRespawn(ClientPlayerEntity player) {
        // 发送当前玩家的 yaw/pitch，服务端会使用它
        sendTeleportRequest(player, new TeleportRequestC2SPayload(
                "respawn", "respawn", 0, 0, 0, player.getYaw(), player.getPitch(), ""));
        return true;
    }

    /**
     * 公开 Spot。
     */
    @Override
    public void publishSpot(ClientPlayerEntity player, Spot spot) {
        ClientPlayNetworking.send(PublicSpotActionC2SPayload.publish(
                spot.getName(),
                spot.getX(), spot.getY(), spot.getZ(),
                spot.getYaw(), spot.getPitch(),
                spot.getDimension()));
    }

    /**
     * 取消公开 Spot。
     */
    @Override
    public void unpublishSpot(ClientPlayerEntity player, String spotName) {
        ClientPlayNetworking.send(PublicSpotActionC2SPayload.unpublish(spotName));
    }

    /**
     * 请求公开 Spot 列表。
     */
    public void requestPublicSpotList(ClientPlayerEntity player) {
        PublicSpotListHandler.requestPublicSpots();
    }

    /**
     * 请求公开 Spot 列表（异步回调）。
     */
    public void requestPublicSpotListWithCallback(ClientPlayerEntity player,
                                                   java.util.function.Consumer<List<PublicSpotListHandler.PublicSpotInfo>> callback) {
        PublicSpotListHandler.requestPublicSpotsWithCallback(callback);
    }

    /**
     * 传送到公开 Spot。
     */
    public void teleportToPublicSpot(ClientPlayerEntity player, String fullName) {
        ClientPlayNetworking.send(PublicSpotTeleportC2SPayload.of(fullName));
    }

    /**
     * 向服务端发送传送请求。
     */
    private void sendTeleportRequest(ClientPlayerEntity player, TeleportRequestC2SPayload payload) {
        ClientPlayNetworking.send(payload);
    }

    /**
     * 获取世界标识符。
     */
    private String getWorldIdentifier() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.getServer() != null) {
            // 单人模式
            return "singleplayer:" + client.getServer().getSavePath(WorldSavePath.ROOT).getParent().getFileName().toString();
        }
        // 多人模式
        return "multiplayer:" + (client.getCurrentServerEntry() != null ? client.getCurrentServerEntry().address : "unknown");
    }

    @Override
    public void showLogs(ClientPlayerEntity player, int count) {
        ClientPlayNetworking.send(new TeleportLogAdminC2SPayload("list", count));
    }

    @Override
    public void clearLogs(ClientPlayerEntity player) {
        ClientPlayNetworking.send(new TeleportLogAdminC2SPayload("clear", 0));
    }
}
