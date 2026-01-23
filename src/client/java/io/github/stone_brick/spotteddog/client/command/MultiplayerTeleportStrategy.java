package io.github.stone_brick.spotteddog.client.command;

import io.github.stone_brick.spotteddog.client.data.Spot;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.PlayerManager;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.GlobalPos;

import java.util.Optional;

@Environment(EnvType.CLIENT)
public class MultiplayerTeleportStrategy implements TeleportStrategy {

    @Override
    public void teleportToSpot(ClientPlayerEntity player, Spot spot) {
        String[] parts = spot.getDimension().split(":");
        if (parts.length >= 2) {
            String dimension = parts[1];
            String command = String.format("execute as @s in %s run tp @s %.2f %.2f %.2f",
                    dimension, spot.getX(), spot.getY(), spot.getZ());
            player.networkHandler.sendChatCommand(command);
        }
    }

    @Override
    public void teleportToSpawn(ClientPlayerEntity player) {
        player.networkHandler.sendChatCommand("minecraft:execute in minecraft:overworld run tp @s 0 64 0");
    }

    @Override
    public void teleportToDeath(ClientPlayerEntity player) {
        Optional<Spot> deathSpot = getLastDeathLocation(player);
        if (deathSpot.isPresent()) {
            teleportToSpot(player, deathSpot.get());
        } else {
            player.sendMessage(net.minecraft.text.Text.literal("[SpottedDog] 未找到死亡点记录"), false);
        }
    }

    @Override
    public boolean teleportToRespawn(ClientPlayerEntity player) {
        Optional<Spot> respawnSpot = getRespawnLocation(player);
        if (respawnSpot.isPresent()) {
            teleportToSpot(player, respawnSpot.get());
            return true;
        }
        return false;
    }

    private Optional<ServerPlayerEntity> getServerPlayer(ClientPlayerEntity player) {
        MinecraftClient client = MinecraftClient.getInstance();
        MinecraftServer server = client.getServer();
        if (server == null) return Optional.empty();

        PlayerManager playerManager = server.getPlayerManager();
        return Optional.ofNullable(playerManager.getPlayer(player.getUuid()));
    }

    private Optional<Spot> getRespawnLocation(ClientPlayerEntity player) {
        return getServerPlayer(player).flatMap(serverPlayer -> {
            var respawn = serverPlayer.getRespawn();
            if (respawn == null) return Optional.empty();

            var respawnData = respawn.respawnData();
            if (respawnData == null) return Optional.empty();

            BlockPos pos = respawnData.getPos();
            return Optional.of(new Spot(
                "respawn", "RespawnPoint",
                pos.getX(), pos.getY(), pos.getZ(),
                "minecraft:overworld", "Server"
            ));
        });
    }

    private Optional<Spot> getLastDeathLocation(ClientPlayerEntity player) {
        return getServerPlayer(player).flatMap(serverPlayer -> {
            Optional<GlobalPos> deathPosOpt = serverPlayer.getLastDeathPos();
            if (deathPosOpt.isEmpty()) return Optional.empty();

            GlobalPos deathPos = deathPosOpt.get();
            BlockPos pos = deathPos.pos();
            String dimension = deathPos.dimension().getValue().toString();

            return Optional.of(new Spot(
                "death", "DeathPoint",
                pos.getX(), pos.getY(), pos.getZ(),
                dimension, "Server"
            ));
        });
    }
}
