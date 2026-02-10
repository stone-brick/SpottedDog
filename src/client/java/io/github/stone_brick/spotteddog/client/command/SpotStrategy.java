package io.github.stone_brick.spotteddog.client.command;

import io.github.stone_brick.spotteddog.client.data.Spot;
import net.minecraft.client.network.ClientPlayerEntity;

public interface SpotStrategy {
    void teleportToSpot(ClientPlayerEntity player, Spot spot);
    void teleportToSpawn(ClientPlayerEntity player);
    void teleportToDeath(ClientPlayerEntity player);
    boolean teleportToRespawn(ClientPlayerEntity player);

    // 公开/取消公开 Spot
    void publishSpot(ClientPlayerEntity player, Spot spot);
    void unpublishSpot(ClientPlayerEntity player, String spotName);

    // 日志管理
    void showLogs(ClientPlayerEntity player, int count);
    void clearLogs(ClientPlayerEntity player);
}
