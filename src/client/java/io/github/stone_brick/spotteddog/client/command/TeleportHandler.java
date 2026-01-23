package io.github.stone_brick.spotteddog.client.command;

import io.github.stone_brick.spotteddog.client.data.Spot;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;

@Environment(EnvType.CLIENT)
public class TeleportHandler {

    private static final TeleportStrategy STRATEGY;

    static {
        boolean isSingleplayer = MinecraftClient.getInstance().isInSingleplayer();
        STRATEGY = isSingleplayer
                ? new SingleplayerTeleportStrategy()
                : new MultiplayerTeleportStrategy();
    }

    public static void teleportToSpot(ClientPlayerEntity player, Spot spot) {
        STRATEGY.teleportToSpot(player, spot);
    }

    public static void teleportToSpawn(ClientPlayerEntity player) {
        STRATEGY.teleportToSpawn(player);
    }

    public static void teleportToDeath(ClientPlayerEntity player) {
        STRATEGY.teleportToDeath(player);
    }

    public static boolean teleportToRespawn(ClientPlayerEntity player) {
        return STRATEGY.teleportToRespawn(player);
    }
}
