package io.github.stone_brick.spotteddog.client.command;

import io.github.stone_brick.spotteddog.client.data.Spot;
import net.minecraft.client.network.ClientPlayerEntity;

public interface TeleportStrategy {
    void teleportToSpot(ClientPlayerEntity player, Spot spot);
    void teleportToSpawn(ClientPlayerEntity player);
    void teleportToDeath(ClientPlayerEntity player);
    boolean teleportToRespawn(ClientPlayerEntity player);
}
