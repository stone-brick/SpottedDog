package io.github.stone_brick.spotteddog.client.command;

import io.github.stone_brick.spotteddog.client.data.Spot;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;

@Environment(EnvType.CLIENT)
public class SpotHandler {

    /**
     * 获取当前的策略。
     * 每次调用时动态检查当前模式，确保策略正确。
     */
    public static SpotStrategy getStrategy() {
        return MinecraftClient.getInstance().isInSingleplayer()
                ? new SingleplayerSpotStrategy()
                : new MultiplayerSpotStrategy();
    }

    public static void teleportToSpot(ClientPlayerEntity player, Spot spot) {
        getStrategy().teleportToSpot(player, spot);
    }

    public static void teleportToSpawn(ClientPlayerEntity player) {
        getStrategy().teleportToSpawn(player);
    }

    public static void teleportToDeath(ClientPlayerEntity player) {
        getStrategy().teleportToDeath(player);
    }

    public static boolean teleportToRespawn(ClientPlayerEntity player) {
        return getStrategy().teleportToRespawn(player);
    }

    public static void publishSpot(ClientPlayerEntity player, Spot spot) {
        getStrategy().publishSpot(player, spot);
    }

    public static void unpublishSpot(ClientPlayerEntity player, String spotName) {
        getStrategy().unpublishSpot(player, spotName);
    }

    public static void showLogs(ClientPlayerEntity player, int count) {
        getStrategy().showLogs(player, count);
    }

    public static void clearLogs(ClientPlayerEntity player) {
        getStrategy().clearLogs(player);
    }
}
