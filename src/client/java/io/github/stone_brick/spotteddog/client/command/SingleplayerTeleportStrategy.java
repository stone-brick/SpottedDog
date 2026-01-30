package io.github.stone_brick.spotteddog.client.command;

import io.github.stone_brick.spotteddog.client.data.Spot;
import io.github.stone_brick.spotteddog.client.data.TeleportLogManager;
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
            player.sendMessage(net.minecraft.text.Text.translatable("spotteddog.teleport.failed.server"), false);
            return;
        }

        ServerPlayerEntity serverPlayer = getServerPlayer(server, player);
        if (serverPlayer == null) {
            player.sendMessage(net.minecraft.text.Text.translatable("spotteddog.teleport.failed.player"), false);
            return;
        }

        // 获取目标世界
        RegistryKey<World> targetKey = getWorldKey(spot.getDimension());
        ServerWorld targetWorld = server.getWorld(targetKey);
        if (targetWorld == null) {
            player.sendMessage(net.minecraft.text.Text.translatable("spotteddog.teleport.failed.world"), false);
            return;
        }

        // 记录源位置
        String sourceDim = player.getEntityWorld().getRegistryKey().getValue().toString();
        double sourceX = player.getX();
        double sourceY = player.getY();
        double sourceZ = player.getZ();

        // 使用 teleport 方法进行跨维度传送，应用保存的 yaw/pitch
        serverPlayer.teleport(targetWorld, spot.getX(), spot.getY(), spot.getZ(),
            EnumSet.noneOf(PositionFlag.class), spot.getYaw(), spot.getPitch(), false);

        // 记录日志
        TeleportLogManager.getInstance().logTeleport("spot", spot.getName(),
                sourceDim, sourceX, sourceY, sourceZ,
                spot.getDimension(), spot.getX(), spot.getY(), spot.getZ());
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
            player.sendMessage(net.minecraft.text.Text.translatable("spotteddog.teleport.failed.server"), false);
            return;
        }

        ServerPlayerEntity serverPlayer = getServerPlayer(server, player);
        if (serverPlayer == null) {
            player.sendMessage(net.minecraft.text.Text.translatable("spotteddog.teleport.failed.player"), false);
            return;
        }

        // 使用 IntegratedServer.getSpawnPoint() 获取实际出生点坐标
        BlockPos spawnPos = server.getSpawnPoint().getPos();
        double targetX = spawnPos.getX() + 0.5;
        double targetY = spawnPos.getY();
        double targetZ = spawnPos.getZ() + 0.5;

        // 记录源位置
        String sourceDim = player.getEntityWorld().getRegistryKey().getValue().toString();
        double sourceX = player.getX();
        double sourceY = player.getY();
        double sourceZ = player.getZ();

        // 传送到主世界出生点（方块坐标需要添加 0.5 偏移，保持玩家当前朝向）
        serverPlayer.teleport(server.getOverworld(), targetX, targetY, targetZ,
                EnumSet.noneOf(PositionFlag.class), player.getYaw(), player.getPitch(), false);

        // 记录日志
        TeleportLogManager.getInstance().logTeleport("spawn", null,
                sourceDim, sourceX, sourceY, sourceZ,
                "minecraft:overworld", targetX, targetY, targetZ);
    }

    @Override
    public void teleportToDeath(ClientPlayerEntity player) {
        MinecraftClient client = MinecraftClient.getInstance();
        MinecraftServer server = client.getServer();
        if (server == null) {
            player.sendMessage(net.minecraft.text.Text.translatable("spotteddog.teleport.failed.server"), false);
            return;
        }

        ServerPlayerEntity serverPlayer = getServerPlayer(server, player);
        if (serverPlayer == null) {
            player.sendMessage(net.minecraft.text.Text.translatable("spotteddog.teleport.failed.player"), false);
            return;
        }

        Optional<GlobalPos> deathPosOpt = serverPlayer.getLastDeathPos();
        if (deathPosOpt.isEmpty()) {
            player.sendMessage(net.minecraft.text.Text.translatable("spotteddog.teleport.failed.death.not.found"), false);
            return;
        }

        GlobalPos deathPos = deathPosOpt.get();
        BlockPos pos = deathPos.pos();
        String dimension = deathPos.dimension().getValue().toString();
        double targetX = pos.getX() + 0.5;
        double targetY = pos.getY();
        double targetZ = pos.getZ() + 0.5;

        // 获取目标世界
        RegistryKey<World> targetKey = getWorldKey(dimension);
        ServerWorld targetWorld = server.getWorld(targetKey);
        if (targetWorld == null) {
            player.sendMessage(net.minecraft.text.Text.translatable("spotteddog.teleport.failed.world"), false);
            return;
        }

        // 记录源位置
        String sourceDim = player.getEntityWorld().getRegistryKey().getValue().toString();
        double sourceX = player.getX();
        double sourceY = player.getY();
        double sourceZ = player.getZ();

        // 传送到死亡点（方块坐标需要添加 0.5 偏移，保持玩家当前朝向）
        serverPlayer.teleport(targetWorld, targetX, targetY, targetZ,
                EnumSet.noneOf(PositionFlag.class), player.getYaw(), player.getPitch(), false);

        // 记录日志
        TeleportLogManager.getInstance().logTeleport("death", null,
                sourceDim, sourceX, sourceY, sourceZ,
                dimension, targetX, targetY, targetZ);
    }

    @Override
    public boolean teleportToRespawn(ClientPlayerEntity player) {
        MinecraftClient client = MinecraftClient.getInstance();
        MinecraftServer server = client.getServer();
        if (server == null) {
            player.sendMessage(net.minecraft.text.Text.translatable("spotteddog.teleport.failed.server"), false);
            return false;
        }

        ServerPlayerEntity serverPlayer = getServerPlayer(server, player);
        if (serverPlayer == null) {
            player.sendMessage(net.minecraft.text.Text.translatable("spotteddog.teleport.failed.player"), false);
            return false;
        }

        var respawn = serverPlayer.getRespawn();
        if (respawn == null || respawn.respawnData() == null) {
            player.sendMessage(net.minecraft.text.Text.translatable("spotteddog.teleport.failed.respawn.not.found"), false);
            return false;
        }

        BlockPos pos = respawn.respawnData().getPos();
        double targetX = pos.getX() + 0.5;
        double targetY = pos.getY();
        double targetZ = pos.getZ() + 0.5;

        // 记录源位置
        String sourceDim = player.getEntityWorld().getRegistryKey().getValue().toString();
        double sourceX = player.getX();
        double sourceY = player.getY();
        double sourceZ = player.getZ();

        // 传送到重生点（方块坐标需要添加 0.5 偏移，保持玩家当前朝向）
        serverPlayer.teleport(server.getOverworld(), targetX, targetY, targetZ,
                EnumSet.noneOf(PositionFlag.class), player.getYaw(), player.getPitch(), false);

        // 记录日志
        TeleportLogManager.getInstance().logTeleport("respawn", null,
                sourceDim, sourceX, sourceY, sourceZ,
                "minecraft:overworld", targetX, targetY, targetZ);

        return true;
    }

    private ServerPlayerEntity getServerPlayer(MinecraftServer server, ClientPlayerEntity player) {
        PlayerManager playerManager = server.getPlayerManager();
        return playerManager.getPlayer(player.getUuid());
    }
}
