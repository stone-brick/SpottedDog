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
import java.util.List;
import java.util.Optional;

@Environment(EnvType.CLIENT)
public class SingleplayerSpotStrategy implements SpotStrategy {

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

        // 获取目标世界（安全获取，支持未访问过的维度）
        RegistryKey<World> targetKey = getWorldKey(spot.getDimension());
        ServerWorld targetWorld = getWorld(server, targetKey);
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
            case "minecraft:overworld", "overworld" -> World.OVERWORLD;
            case "minecraft:the_nether", "nether" -> World.NETHER;
            case "minecraft:the_end", "the_end", "end" -> World.END;
            default -> {
                net.minecraft.util.Identifier dimId = net.minecraft.util.Identifier.of(dimension);
                yield RegistryKey.of(net.minecraft.registry.RegistryKeys.WORLD, dimId);
            }
        };
    }

    /**
     * 安全地获取世界，可能世界还未被访问过。
     */
    private ServerWorld getWorld(MinecraftServer server, RegistryKey<World> worldKey) {
        ServerWorld world = server.getWorld(worldKey);
        if (world != null) {
            return world;
        }
        // 如果世界还未被访问过，从 server.getWorlds() 中查找
        for (ServerWorld candidate : server.getWorlds()) {
            if (candidate.getRegistryKey().equals(worldKey)) {
                return candidate;
            }
        }
        return null;
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

        // 获取目标世界（安全获取，支持未访问过的维度）
        RegistryKey<World> targetKey = getWorldKey(dimension);
        ServerWorld targetWorld = getWorld(server, targetKey);
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

    @Override
    public void publishSpot(ClientPlayerEntity player, Spot spot) {
        player.sendMessage(net.minecraft.text.Text.translatable("spotteddog.spot.multiplayer.only"), false);
    }

    @Override
    public void unpublishSpot(ClientPlayerEntity player, String spotName) {
        player.sendMessage(net.minecraft.text.Text.translatable("spotteddog.spot.multiplayer.only"), false);
    }

    @Override
    public void showLogs(ClientPlayerEntity player, int count) {
        TeleportLogManager logManager = TeleportLogManager.getInstance();
        List<TeleportLogManager.ClientTeleportLog> logs = logManager.getRecentLogs(count);

        player.sendMessage(net.minecraft.text.Text.translatable("spotteddog.log.list.header", logs.size()), false);

        if (logs.isEmpty()) {
            player.sendMessage(net.minecraft.text.Text.translatable("spotteddog.log.empty"), false);
        } else {
            for (var log : logs) {
                String spotInfo = log.spotName != null ? log.spotName : log.teleportType;
                String message = String.format("[%s] %s %s -> (%s, %.1f, %.1f, %.1f)",
                        log.timestamp.substring(11, 19),
                        log.playerName, spotInfo,
                        log.targetDimension, log.targetX, log.targetY, log.targetZ);
                player.sendMessage(net.minecraft.text.Text.literal(message), false);
            }
        }
    }

    @Override
    public void clearLogs(ClientPlayerEntity player) {
        TeleportLogManager.getInstance().clearLogs();
        player.sendMessage(net.minecraft.text.Text.translatable("spotteddog.log.cleared"), false);
    }
}
