package io.github.stone_brick.spotteddog.server.network;

import io.github.stone_brick.spotteddog.network.c2s.TeleportRequestC2SPayload;
import io.github.stone_brick.spotteddog.network.s2c.TeleportConfirmS2CPayload;
import io.github.stone_brick.spotteddog.server.config.ConfigManager;
import io.github.stone_brick.spotteddog.server.config.CooldownManager;
import io.github.stone_brick.spotteddog.server.permission.PermissionManager;
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
    private static final int MAX_NAME_LENGTH = 64; // Spot 名称最大长度

    static {
        // 注册 C2S 负载类型（服务端接收客户端请求）
        PayloadTypeRegistry.playC2S().register(TeleportRequestC2SPayload.ID, TeleportRequestC2SPayload.CODEC);
    }

    /**
     * 注册服务端数据包处理器。
     */
    public static void register() {
        // 加载配置（首次使用时创建配置文件）
        ConfigManager.loadOrCreate();

        // 动态注册 S2C 负载类型
        PayloadTypeRegistry.playS2C().register(TeleportConfirmS2CPayload.ID, TeleportConfirmS2CPayload.CODEC);

        ServerPlayNetworking.registerGlobalReceiver(TeleportRequestC2SPayload.ID, (payload, context) -> {
            ServerPlayerEntity player = context.player();

            String type = payload.type();
            String targetName = payload.targetName();

            // 验证冷却时间
            if (CooldownManager.isInCooldown(player)) {
                int remaining = CooldownManager.getRemainingCooldown(player);
                ServerPlayNetworking.send(player, TeleportConfirmS2CPayload.failure(
                        type, targetName, "冷却中，请等待 " + remaining + " 秒"));
                return;
            }

            // 验证全局速率限制
            if (!CooldownManager.tryIncrementGlobalCount()) {
                ServerPlayNetworking.send(player, TeleportConfirmS2CPayload.failure(
                        type, targetName, "服务器繁忙，请稍后再试"));
                return;
            }

            // 验证名称长度（仅 spot 类型）
            if ("spot".equals(type) && targetName != null && targetName.length() > MAX_NAME_LENGTH) {
                ServerPlayNetworking.send(player, TeleportConfirmS2CPayload.failure(
                        type, targetName, "Spot 名称超过最大长度"));
                return;
            }

            // 验证权限
            if (!PermissionManager.canTeleport(player)) {
                ServerPlayNetworking.send(player, TeleportConfirmS2CPayload.failure(
                        type, targetName, "No permission"));
                return;
            }

            // 根据类型获取位置并执行传送
            TeleportResult result = executeTeleport(player, payload);

            if (result.success()) {
                // 传送成功，更新冷却时间
                CooldownManager.updateLastTeleport(player);
                ServerPlayNetworking.send(player, TeleportConfirmS2CPayload.success(payload.type(), payload.targetName()));
            } else {
                ServerPlayNetworking.send(player, TeleportConfirmS2CPayload.failure(type, targetName, result.message()));
            }
        });
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
        // 对于 spawn/respawn/death，使用玩家当前朝向；spot 使用保存的朝向
        float yaw = payload.type().equals("spot") ? payload.yaw() : player.getYaw();
        float pitch = payload.type().equals("spot") ? payload.pitch() : player.getPitch();
        return switch (type) {
            case "spawn" -> {
                // 传送到世界出生点（BlockPos 需要添加 0.5 偏移，保持玩家当前朝向）
                ServerWorld overworld = server.getOverworld();
                if (overworld == null) {
                    yield TeleportResult.fail("Overworld not available");
                }
                // 使用 MinecraftServer.getSpawnPoint() 获取世界的实际出生点坐标
                BlockPos spawnPos = server.getSpawnPoint().getPos();
                double targetX = spawnPos.getX() + 0.5;
                double targetY = spawnPos.getY();
                double targetZ = spawnPos.getZ() + 0.5;
                yield teleportTo(player, World.OVERWORLD, targetX, targetY, targetZ, yaw, pitch) ?
                        TeleportResult.ok() : TeleportResult.fail("Teleport failed");
            }
            case "respawn" -> {
                // 获取重生点（BlockPos 需要添加 0.5 偏移，保持玩家当前朝向）
                var respawn = player.getRespawn();
                if (respawn == null || respawn.respawnData() == null) {
                    yield TeleportResult.fail("No respawn point set");
                }
                BlockPos pos = respawn.respawnData().getPos();
                yield teleportTo(player, World.OVERWORLD, pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, yaw, pitch) ?
                        TeleportResult.ok() : TeleportResult.fail("Teleport failed");
            }
            case "death" -> {
                // 获取死亡点（BlockPos 需要添加 0.5 偏移，保持玩家当前朝向）
                var deathPosOpt = player.getLastDeathPos();
                if (deathPosOpt.isEmpty()) {
                    yield TeleportResult.fail("No death point recorded");
                }
                GlobalPos deathPos = deathPosOpt.get();
                BlockPos pos = deathPos.pos();
                // GlobalPos.dimension() 直接返回 RegistryKey<World>
                yield teleportTo(player, deathPos.dimension(), pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, yaw, pitch) ?
                        TeleportResult.ok() : TeleportResult.fail("Teleport failed");
            }
            case "spot" -> {
                // spot 类型使用客户端发送的坐标（玩家坐标，无需偏移）
                double x = payload.x();
                double y = payload.y();
                double z = payload.z();
                String dimension = payload.dimension();
                RegistryKey<World> worldKey = getWorldKey(dimension);
                yield teleportTo(player, worldKey, x, y, z, yaw, pitch) ?
                        TeleportResult.ok() : TeleportResult.fail("Teleport failed");
            }
            default -> TeleportResult.fail("Unknown teleport type: " + type);
        };
    }

    @SuppressWarnings("ConstantConditions")
    private static boolean teleportTo(ServerPlayerEntity player, RegistryKey<World> dimension, double x, double y, double z, float yaw, float pitch) {
        try {
            MinecraftServer server = player.getEntityWorld().getServer();
            ServerWorld targetWorld = server.getWorld(dimension);
            if (targetWorld == null) {
                return false;
            }
            player.teleport(targetWorld, x, y, z,
                    EnumSet.noneOf(PositionFlag.class), yaw, pitch, false);
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
