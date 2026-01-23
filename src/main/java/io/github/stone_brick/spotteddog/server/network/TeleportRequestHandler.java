package io.github.stone_brick.spotteddog.server.network;

import io.github.stone_brick.spotteddog.network.c2s.TeleportRequestC2SPayload;
import io.github.stone_brick.spotteddog.network.s2c.TeleportConfirmS2CPayload;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.packet.s2c.play.PositionFlag;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.GlobalPos;
import net.minecraft.world.World;

import java.util.EnumSet;

/**
 * 服务端传送请求处理器。
 * 客户端发送请求类型（death/respawn/spawn/spot），服务端自行获取位置信息并执行传送。
 */
public class TeleportRequestHandler {

    static {
        // 注册 C2S 负载类型（服务端接收客户端请求）
        PayloadTypeRegistry.playC2S().register(TeleportRequestC2SPayload.ID, TeleportRequestC2SPayload.CODEC);
    }

    /**
     * 注册服务端数据包处理器。
     */
    public static void register() {
        // 动态注册 S2C 负载类型
        PayloadTypeRegistry.playS2C().register(TeleportConfirmS2CPayload.ID, TeleportConfirmS2CPayload.CODEC);

        ServerPlayNetworking.registerGlobalReceiver(TeleportRequestC2SPayload.ID, (payload, context) -> {
            ServerPlayerEntity player = context.player();
            if (player == null) {
                return;
            }

            String type = payload.type();
            String targetName = payload.targetName();

            // 验证权限（TODO: 后续添加权限管理）
            if (!hasPermission(player, type)) {
                ServerPlayNetworking.send(player, TeleportConfirmS2CPayload.failure(
                        type, targetName, "No permission"));
                return;
            }

            // 根据类型获取位置并执行传送
            TeleportResult result = executeTeleport(player, payload);

            if (result.success()) {
                ServerPlayNetworking.send(player, TeleportConfirmS2CPayload.success(payload.type(), payload.targetName()));
            } else {
                ServerPlayNetworking.send(player, TeleportConfirmS2CPayload.failure(type, targetName, result.message()));
            }
        });
    }

    /**
     * 检查玩家是否有执行该类型传送的权限。
     */
    private static boolean hasPermission(ServerPlayerEntity player, String type) {
        // TODO: 后续实现权限管理
        return true;
    }

    /**
     * 传送结果。
     */
    private record TeleportResult(boolean success, String message) {
        static TeleportResult ok() {
            return new TeleportResult(true, "");
        }
        static TeleportResult fail(String message) {
            return new TeleportResult(false, message);
        }
    }

    /**
     * 根据类型获取位置并执行传送。
     */
    private static TeleportResult executeTeleport(ServerPlayerEntity player, TeleportRequestC2SPayload payload) {
        String type = payload.type();
        MinecraftServer server = player.getEntityWorld().getServer();
        return switch (type) {
            case "spawn" -> {
                // 传送到世界出生点
                ServerWorld overworld = server.getOverworld();
                BlockPos spawnPos = overworld.getSpawnPoint().getPos();
                yield teleportTo(player, World.OVERWORLD, spawnPos.getX(), spawnPos.getY(), spawnPos.getZ()) ?
                        TeleportResult.ok() : TeleportResult.fail("Teleport failed");
            }
            case "respawn" -> {
                // 获取重生点
                var respawn = player.getRespawn();
                if (respawn == null || respawn.respawnData() == null) {
                    yield TeleportResult.fail("No respawn point set");
                }
                BlockPos pos = respawn.respawnData().getPos();
                yield teleportTo(player, World.OVERWORLD, pos.getX(), pos.getY(), pos.getZ()) ?
                        TeleportResult.ok() : TeleportResult.fail("Teleport failed");
            }
            case "death" -> {
                // 获取死亡点
                var deathPosOpt = player.getLastDeathPos();
                if (deathPosOpt.isEmpty()) {
                    yield TeleportResult.fail("No death point recorded");
                }
                GlobalPos deathPos = deathPosOpt.get();
                BlockPos pos = deathPos.pos();
                // GlobalPos.dimension() 直接返回 RegistryKey<World>
                yield teleportTo(player, deathPos.dimension(), pos.getX(), pos.getY(), pos.getZ()) ?
                        TeleportResult.ok() : TeleportResult.fail("Teleport failed");
            }
            case "spot" -> {
                // spot 类型使用客户端发送的坐标
                double x = payload.x();
                double y = payload.y();
                double z = payload.z();
                String dimension = payload.dimension();
                RegistryKey<World> worldKey = getWorldKey(dimension);
                yield teleportTo(player, worldKey, x, y, z) ?
                        TeleportResult.ok() : TeleportResult.fail("Teleport failed");
            }
            default -> TeleportResult.fail("Unknown teleport type: " + type);
        };
    }

    private static boolean teleportTo(ServerPlayerEntity player, RegistryKey<World> dimension, double x, double y, double z) {
        try {
            MinecraftServer server = player.getEntityWorld().getServer();
            ServerWorld targetWorld = server.getWorld(dimension);
            if (targetWorld == null) {
                return false;
            }
            // X 和 Z 坐标添加 0.5，使玩家居于方块中心
            player.teleport(targetWorld, x + 0.5, y, z + 0.5,
                    EnumSet.noneOf(PositionFlag.class), 0f, 0f, false);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private static RegistryKey<World> getWorldKey(String dimension) {
        return switch (dimension) {
            case "minecraft:overworld", "overworld" -> World.OVERWORLD;
            case "minecraft:the_nether", "nether" -> World.NETHER;
            case "minecraft:the_end", "the_end", "end" -> World.END;
            default -> {
                // 使用 RegistryKeys.WORLD 作为 registry，维度 ID 作为值
                Identifier dimId = Identifier.of(dimension);
                yield RegistryKey.of(RegistryKeys.WORLD, dimId);
            }
        };
    }
}
