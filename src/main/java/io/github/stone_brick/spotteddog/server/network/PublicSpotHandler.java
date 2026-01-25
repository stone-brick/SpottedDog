package io.github.stone_brick.spotteddog.server.network;

import io.github.stone_brick.spotteddog.network.c2s.*;
import io.github.stone_brick.spotteddog.network.s2c.PublicSpotListS2CPayload;
import io.github.stone_brick.spotteddog.network.s2c.TeleportConfirmS2CPayload;
import io.github.stone_brick.spotteddog.server.config.CooldownManager;
import io.github.stone_brick.spotteddog.server.data.PublicSpot;
import io.github.stone_brick.spotteddog.server.data.PublicSpotManager;
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
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;

/**
 * 服务端公开 Spot 处理器。
 * 处理公开/取消公开/列表查询/传送请求。
 */
public class PublicSpotHandler {

    static {
        // 注册 C2S Payload 类型
        PayloadTypeRegistry.playC2S().register(PublicSpotActionC2SPayload.ID, PublicSpotActionC2SPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(PublicSpotListC2SPayload.ID, PublicSpotListC2SPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(PublicSpotTeleportC2SPayload.ID, PublicSpotTeleportC2SPayload.CODEC);
    }

    /**
     * 注册所有处理器。
     */
    public static void register() {
        // 初始化 PublicSpotManager
        PublicSpotManager.getInstance().initialize(null);

        // 动态注册 S2C Payload 类型
        PayloadTypeRegistry.playS2C().register(PublicSpotListS2CPayload.ID, PublicSpotListS2CPayload.CODEC);

        // 处理公开/取消公开请求
        ServerPlayNetworking.registerGlobalReceiver(PublicSpotActionC2SPayload.ID, (payload, context) -> {
            ServerPlayerEntity player = context.player();
            MinecraftServer server = player.getEntityWorld().getServer();
            String playerName = player.getName().getString();
            String playerUuid = player.getUuid().toString();
            String worldIdentifier = payload.worldIdentifier();

            // 验证冷却时间
            if (CooldownManager.isInCooldown(player)) {
                int remaining = CooldownManager.getRemainingCooldown(player);
                ServerPlayNetworking.send(player, TeleportConfirmS2CPayload.failure(
                        "public", payload.spotName(), "冷却中，请等待 " + remaining + " 秒"));
                return;
            }

            switch (payload.action()) {
                case "publish" -> {
                    // 公开 Spot
                    boolean success = PublicSpotManager.getInstance().publishSpot(
                            playerName, playerUuid, payload.spotName(),
                            payload.x(), payload.y(), payload.z(),
                            payload.yaw(), payload.pitch(),
                            payload.dimension(), payload.world(), worldIdentifier);

                    if (success) {
                        ServerPlayNetworking.send(player, TeleportConfirmS2CPayload.success(
                                "public", payload.spotName()));
                    } else {
                        ServerPlayNetworking.send(player, TeleportConfirmS2CPayload.failure(
                                "public", payload.spotName(), "Spot 已公开或名称重复"));
                    }
                }
                case "unpublish" -> {
                    // 取消公开 Spot
                    boolean success = PublicSpotManager.getInstance().unpublishSpot(
                            playerName, payload.spotName(), worldIdentifier);

                    if (success) {
                        ServerPlayNetworking.send(player, TeleportConfirmS2CPayload.success(
                                "unpublic", payload.spotName()));
                    } else {
                        ServerPlayNetworking.send(player, TeleportConfirmS2CPayload.failure(
                                "unpublic", payload.spotName(), "未找到公开的 Spot"));
                    }
                }
            }
        });

        // 处理获取公开 Spot 列表请求
        ServerPlayNetworking.registerGlobalReceiver(PublicSpotListC2SPayload.ID, (payload, context) -> {
            ServerPlayerEntity player = context.player();

            List<PublicSpot> spots;
            if (payload.worldIdentifier().isEmpty()) {
                spots = PublicSpotManager.getInstance().getAllPublicSpots();
            } else {
                spots = PublicSpotManager.getInstance().getPublicSpotsByWorld(payload.worldIdentifier());
            }

            // 转换为网络传输格式
            List<PublicSpotListS2CPayload.PublicSpotInfo> spotInfos = new ArrayList<>();
            for (PublicSpot spot : spots) {
                spotInfos.add(new PublicSpotListS2CPayload.PublicSpotInfo(
                        spot.getOwnerName(),
                        spot.getDisplayName(),
                        spot.getX(), spot.getY(), spot.getZ(),
                        spot.getDimension(),
                        spot.getWorld()
                ));
            }

            ServerPlayNetworking.send(player, new PublicSpotListS2CPayload(spotInfos));
        });

        // 处理传送到公开 Spot 请求
        ServerPlayNetworking.registerGlobalReceiver(PublicSpotTeleportC2SPayload.ID, (payload, context) -> {
            ServerPlayerEntity player = context.player();
            MinecraftServer server = player.getEntityWorld().getServer();

            // 验证冷却时间
            if (CooldownManager.isInCooldown(player)) {
                int remaining = CooldownManager.getRemainingCooldown(player);
                ServerPlayNetworking.send(player, TeleportConfirmS2CPayload.failure(
                        "public_tp", payload.fullName(), "冷却中，请等待 " + remaining + " 秒"));
                return;
            }

            // 验证全局速率限制
            if (!CooldownManager.tryIncrementGlobalCount()) {
                ServerPlayNetworking.send(player, TeleportConfirmS2CPayload.failure(
                        "public_tp", payload.fullName(), "服务器繁忙，请稍后再试"));
                return;
            }

            // 解析完整名称
            PublicSpotManager.PublicSpotName name = PublicSpotManager.parsePublicSpotName(payload.fullName());
            if (name == null) {
                ServerPlayNetworking.send(player, TeleportConfirmS2CPayload.failure(
                        "public_tp", payload.fullName(), "无效的公开 Spot 名称格式"));
                return;
            }

            // 查找公开 Spot
            Optional<PublicSpot> spotOpt = PublicSpotManager.getInstance()
                    .getPublicSpotByFullName(payload.fullName(), payload.worldIdentifier());

            if (spotOpt.isEmpty()) {
                ServerPlayNetworking.send(player, TeleportConfirmS2CPayload.failure(
                        "public_tp", payload.fullName(), "未找到公开的 Spot"));
                return;
            }

            PublicSpot spot = spotOpt.get();

            // 执行传送
            RegistryKey<World> worldKey = getWorldKey(spot.getDimension());
            ServerWorld targetWorld = server.getWorld(worldKey);
            if (targetWorld == null) {
                ServerPlayNetworking.send(player, TeleportConfirmS2CPayload.failure(
                        "public_tp", payload.fullName(), "无法访问目标世界"));
                return;
            }

            boolean success = teleportToPublicSpot(player, targetWorld, spot);
            if (success) {
                CooldownManager.updateLastTeleport(player);
                ServerPlayNetworking.send(player, TeleportConfirmS2CPayload.success(
                        "public_tp", payload.fullName()));
            } else {
                ServerPlayNetworking.send(player, TeleportConfirmS2CPayload.failure(
                        "public_tp", payload.fullName(), "传送失败"));
            }
        });
    }

    /**
     * 传送到公开 Spot。
     * 公开 Spot 保存的是精确坐标，无需偏移。
     */
    private static boolean teleportToPublicSpot(ServerPlayerEntity player, ServerWorld world, PublicSpot spot) {
        try {
            player.teleport(world, spot.getX(), spot.getY(), spot.getZ(),
                    EnumSet.noneOf(PositionFlag.class), spot.getYaw(), spot.getPitch(), false);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 获取世界 Key。
     */
    private static RegistryKey<World> getWorldKey(String dimension) {
        return switch (dimension) {
            case "minecraft:overworld", "overworld" -> World.OVERWORLD;
            case "minecraft:the_nether", "nether" -> World.NETHER;
            case "minecraft:the_end", "the_end", "end" -> World.END;
            default -> {
                Identifier dimId = Identifier.of(dimension);
                yield RegistryKey.of(RegistryKeys.WORLD, dimId);
            }
        };
    }
}
