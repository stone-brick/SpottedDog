package io.github.stone_brick.spotteddog.client.command;

import io.github.stone_brick.spotteddog.client.data.Spot;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.network.packet.s2c.play.PositionFlag;
import net.minecraft.registry.RegistryKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.PlayerManager;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.GlobalPos;
import net.minecraft.world.World;

import java.util.EnumSet;
import java.util.Optional;

@Environment(EnvType.CLIENT)
public class SingleplayerTeleportStrategy implements TeleportStrategy {

    @Override
    public void teleportToSpot(ClientPlayerEntity player, Spot spot) {
        MinecraftClient client = MinecraftClient.getInstance();
        MinecraftServer server = client.getServer();
        if (server == null) {
            player.sendMessage(net.minecraft.text.Text.literal("[SpottedDog] 传送失败：无法获取服务器"), false);
            return;
        }

        ServerPlayerEntity serverPlayer = getServerPlayer(server, player);
        if (serverPlayer == null) {
            player.sendMessage(net.minecraft.text.Text.literal("[SpottedDog] 传送失败：无法获取玩家"), false);
            return;
        }

        // 获取目标世界
        RegistryKey<World> targetKey = getWorldKey(spot.getDimension());
        ServerWorld targetWorld = server.getWorld(targetKey);
        if (targetWorld == null) {
            player.sendMessage(net.minecraft.text.Text.literal("[SpottedDog] 传送失败：无法获取目标世界"), false);
            return;
        }

        // 使用 teleport 方法进行跨维度传送，X 和 Z 添加 0.5 使玩家居于方块中心
        serverPlayer.teleport(targetWorld, spot.getX() + 0.5, spot.getY(), spot.getZ() + 0.5,
            EnumSet.noneOf(PositionFlag.class), 0f, 0f, false);
    }

    private RegistryKey<World> getWorldKey(String dimension) {
        return switch (dimension) {
            case "minecraft:overworld" -> World.OVERWORLD;
            case "minecraft:nether" -> World.NETHER;
            case "minecraft:the_end" -> World.END;
            default -> {
                net.minecraft.util.Identifier dimId = net.minecraft.util.Identifier.of(dimension);
                yield RegistryKey.of(RegistryKey.ofRegistry(dimId), dimId);
            }
        };
    }

    @Override
    public void teleportToSpawn(ClientPlayerEntity player) {
        MinecraftClient client = MinecraftClient.getInstance();
        MinecraftServer server = client.getServer();
        if (server == null) {
            player.sendMessage(net.minecraft.text.Text.literal("[SpottedDog] 传送失败：无法获取服务器"), false);
            return;
        }

        ServerPlayerEntity serverPlayer = getServerPlayer(server, player);
        if (serverPlayer == null) {
            player.sendMessage(net.minecraft.text.Text.literal("[SpottedDog] 传送失败：无法获取玩家"), false);
            return;
        }

        // 直接传送到主世界出生点
        serverPlayer.requestTeleport(0.0, 64.0, 0.0);
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

    private ServerPlayerEntity getServerPlayer(MinecraftServer server, ClientPlayerEntity player) {
        PlayerManager playerManager = server.getPlayerManager();
        return playerManager.getPlayer(player.getUuid());
    }

    private Optional<Spot> getRespawnLocation(ClientPlayerEntity player) {
        MinecraftClient client = MinecraftClient.getInstance();
        MinecraftServer server = client.getServer();
        if (server == null) return Optional.empty();

        ServerPlayerEntity serverPlayer = getServerPlayer(server, player);
        if (serverPlayer == null) return Optional.empty();

        var respawn = serverPlayer.getRespawn();
        if (respawn == null) return Optional.empty();

        var respawnData = respawn.respawnData();
        if (respawnData == null) return Optional.empty();

        BlockPos pos = respawnData.getPos();
        return Optional.of(new Spot(
            "respawn", "RespawnPoint",
            pos.getX(), pos.getY(), pos.getZ(),
            "minecraft:overworld", "Singleplayer", null
        ));
    }

    private Optional<Spot> getLastDeathLocation(ClientPlayerEntity player) {
        MinecraftClient client = MinecraftClient.getInstance();
        MinecraftServer server = client.getServer();
        if (server == null) return Optional.empty();

        ServerPlayerEntity serverPlayer = getServerPlayer(server, player);
        if (serverPlayer == null) return Optional.empty();

        Optional<GlobalPos> deathPosOpt = serverPlayer.getLastDeathPos();
        if (deathPosOpt.isEmpty()) return Optional.empty();

        GlobalPos deathPos = deathPosOpt.get();
        BlockPos pos = deathPos.pos();
        String dimension = deathPos.dimension().getValue().toString();

        return Optional.of(new Spot(
            "death", "DeathPoint",
            pos.getX(), pos.getY(), pos.getZ(),
            dimension, "Singleplayer", null
        ));
    }
}
