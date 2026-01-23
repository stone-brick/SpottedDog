package io.github.stone_brick.spotteddog.client.command;

import io.github.stone_brick.spotteddog.client.data.Spot;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;

@Environment(EnvType.CLIENT)
public class TeleportHandler {

    /**
     * 获取当前的传送策略。
     * 每次调用时动态检查当前模式，确保策略正确。
     */
    private static TeleportStrategy getStrategy() {
        boolean isSingleplayer = MinecraftClient.getInstance().isInSingleplayer();
        return isSingleplayer
                ? new SingleplayerTeleportStrategy()
                : new MultiplayerTeleportStrategy();
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
}
