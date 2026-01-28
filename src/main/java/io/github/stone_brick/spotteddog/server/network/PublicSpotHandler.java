package io.github.stone_brick.spotteddog.server.network;

import io.github.stone_brick.spotteddog.network.c2s.*;
import io.github.stone_brick.spotteddog.network.s2c.PublicSpotListS2CPayload;
import io.github.stone_brick.spotteddog.network.s2c.TeleportConfirmS2CPayload;
import io.github.stone_brick.spotteddog.server.config.CooldownManager;
import io.github.stone_brick.spotteddog.server.data.PublicSpot;
import io.github.stone_brick.spotteddog.server.data.PublicSpotManager;
import io.github.stone_brick.spotteddog.server.permission.PermissionManager;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.packet.s2c.play.PositionFlag;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
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
    private static final int MAX_NAME_LENGTH = 64; // Spot 名称最大长度

    static {
        // 注册 C2S Payload 类型
        PayloadTypeRegistry.playC2S().register(PublicSpotActionC2SPayload.ID, PublicSpotActionC2SPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(PublicSpotListC2SPayload.ID, PublicSpotListC2SPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(PublicSpotTeleportC2SPayload.ID, PublicSpotTeleportC2SPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(PublicSpotUpdateC2SPayload.ID, PublicSpotUpdateC2SPayload.CODEC);
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

            // 验证权限
            if (!PermissionManager.canManagePublicSpots(player)) {
                String messageType = "publish".equals(payload.action()) ? "public" : "unpublic";
                ServerPlayNetworking.send(player, TeleportConfirmS2CPayload.failure(
                        messageType, payload.spotName(), "spotteddog.permission.denied"));
                return;
            }

            // 验证冷却时间（使用独立的公开 Spot 冷却）
            if (CooldownManager.isInPublicSpotCooldown(player)) {
                int remaining = CooldownManager.getPublicSpotRemainingCooldown(player);
                String messageType = "publish".equals(payload.action()) ? "public" : "unpublic";
                ServerPlayNetworking.send(player, TeleportConfirmS2CPayload.failure(
                        messageType, payload.spotName(), Text.translatable("spotteddog.cooldown.message", remaining).getString()));
                return;
            }

            // 验证全局速率限制
            if (!CooldownManager.tryIncrementPublicSpotGlobalCount()) {
                String messageType = "publish".equals(payload.action()) ? "public" : "unpublic";
                ServerPlayNetworking.send(player, TeleportConfirmS2CPayload.failure(
                        messageType, payload.spotName(), "spotteddog.server.busy"));
                return;
            }

            switch (payload.action()) {
                case "publish" -> {
                    // 验证名称长度
                    if (payload.spotName().length() > MAX_NAME_LENGTH) {
                        ServerPlayNetworking.send(player, TeleportConfirmS2CPayload.failure(
                                "public", payload.spotName(), "spotteddog.spot.name.too.long"));
                        return;
                    }

                    // 公开 Spot
                    boolean success = PublicSpotManager.getInstance().publishSpot(
                            playerName, playerUuid, payload.spotName(),
                            payload.x(), payload.y(), payload.z(),
                            payload.yaw(), payload.pitch(),
                            payload.dimension());

                    if (success) {
                        CooldownManager.updateLastPublicSpotRequest(player);
                        ServerPlayNetworking.send(player, TeleportConfirmS2CPayload.success(
                                "public", payload.spotName()));
                    } else {
                        ServerPlayNetworking.send(player, TeleportConfirmS2CPayload.failure(
                                "public", payload.spotName(), "spotteddog.public.spot.duplicate"));
                    }
                }
                case "unpublish" -> {
                    // 验证名称长度
                    if (payload.spotName().length() > MAX_NAME_LENGTH) {
                        ServerPlayNetworking.send(player, TeleportConfirmS2CPayload.failure(
                                "unpublic", payload.spotName(), "spotteddog.spot.name.too.long"));
                        return;
                    }

                    // 取消公开 Spot
                    boolean success = PublicSpotManager.getInstance().unpublishSpot(
                            playerName, payload.spotName());

                    if (success) {
                        CooldownManager.updateLastPublicSpotRequest(player);
                        ServerPlayNetworking.send(player, TeleportConfirmS2CPayload.success(
                                "unpublic", payload.spotName()));
                    } else {
                        ServerPlayNetworking.send(player, TeleportConfirmS2CPayload.failure(
                                "unpublic", payload.spotName(), "spotteddog.public.spot.not.found"));
                    }
                }
            }
        });

        // 处理获取公开 Spot 列表请求
        ServerPlayNetworking.registerGlobalReceiver(PublicSpotListC2SPayload.ID, (payload, context) -> {
            ServerPlayerEntity player = context.player();

            // 验证冷却时间
            if (!CooldownManager.canRequestPublicList(player)) {
                return; // 忽略请求，不返回错误
            }
            CooldownManager.recordPublicListRequest(player);

            // 获取所有公开 Spot（服务器范围）
            List<PublicSpot> spots = PublicSpotManager.getInstance().getAllPublicSpots();

            // 转换为网络传输格式
            List<PublicSpotListS2CPayload.PublicSpotInfo> spotInfos = new ArrayList<>();
            for (PublicSpot spot : spots) {
                spotInfos.add(new PublicSpotListS2CPayload.PublicSpotInfo(
                        spot.getOwnerName(),
                        spot.getDisplayName(),
                        spot.getX(), spot.getY(), spot.getZ(),
                        spot.getDimension()
                ));
            }

            ServerPlayNetworking.send(player, new PublicSpotListS2CPayload(spotInfos));
        });

        // 处理传送到公开 Spot 请求
        ServerPlayNetworking.registerGlobalReceiver(PublicSpotTeleportC2SPayload.ID, (payload, context) -> {
            ServerPlayerEntity player = context.player();
            MinecraftServer server = player.getEntityWorld().getServer();
            String playerUuid = player.getUuid().toString();

            // 验证权限
            if (!PermissionManager.canTeleportToPublicSpot(player)) {
                ServerPlayNetworking.send(player, TeleportConfirmS2CPayload.failure(
                        "public_tp", payload.fullName(), "spotteddog.permission.denied"));
                return;
            }

            // 验证冷却时间
            if (CooldownManager.isInCooldown(player)) {
                int remaining = CooldownManager.getRemainingCooldown(player);
                ServerPlayNetworking.send(player, TeleportConfirmS2CPayload.failure(
                        "public_tp", payload.fullName(), Text.translatable("spotteddog.cooldown.message", remaining).getString()));
                return;
            }

            // 验证全局速率限制
            if (!CooldownManager.tryIncrementGlobalCount()) {
                ServerPlayNetworking.send(player, TeleportConfirmS2CPayload.failure(
                        "public_tp", payload.fullName(), "spotteddog.server.busy"));
                return;
            }

            // 解析完整名称
            PublicSpotManager.PublicSpotName name = PublicSpotManager.parsePublicSpotName(payload.fullName());
            if (name == null) {
                ServerPlayNetworking.send(player, TeleportConfirmS2CPayload.failure(
                        "public_tp", payload.fullName(), "spotteddog.public.spot.invalid.name"));
                return;
            }

            // 验证 spotName 长度（玩家名称是附加信息，不计入长度限制）
            if (name.spotName().length() > MAX_NAME_LENGTH) {
                ServerPlayNetworking.send(player, TeleportConfirmS2CPayload.failure(
                        "public_tp", payload.fullName(), "spotteddog.spot.name.too.long"));
                return;
            }

            // 使用 UUID 查找公开 Spot（避免同一玩家从不同地址连接时找不到 Spot）
            Optional<PublicSpot> spotOpt = PublicSpotManager.getInstance()
                    .getPublicSpotByFullNameWithUuid(payload.fullName(), playerUuid);

            if (spotOpt.isEmpty()) {
                ServerPlayNetworking.send(player, TeleportConfirmS2CPayload.failure(
                        "public_tp", payload.fullName(), "spotteddog.public.spot.not.found"));
                return;
            }

            PublicSpot spot = spotOpt.get();

            // 执行传送
            RegistryKey<World> worldKey = getWorldKey(spot.getDimension());
            ServerWorld targetWorld = server.getWorld(worldKey);
            if (targetWorld == null) {
                ServerPlayNetworking.send(player, TeleportConfirmS2CPayload.failure(
                        "public_tp", payload.fullName(), "spotteddog.world.unreachable"));
                return;
            }

            boolean success = teleportToPublicSpot(player, targetWorld, spot);
            if (success) {
                CooldownManager.updateLastTeleport(player);
                ServerPlayNetworking.send(player, TeleportConfirmS2CPayload.success(
                        "public_tp", payload.fullName()));
            } else {
                ServerPlayNetworking.send(player, TeleportConfirmS2CPayload.failure(
                        "public_tp", payload.fullName(), "spotteddog.teleport.failed.generic"));
            }
        });

        // 处理更新/重命名公开 Spot 请求
        ServerPlayNetworking.registerGlobalReceiver(PublicSpotUpdateC2SPayload.ID, (payload, context) -> {
            ServerPlayerEntity player = context.player();
            MinecraftServer server = player.getEntityWorld().getServer();
            String playerName = player.getName().getString();

            // 验证权限
            if (!PermissionManager.canManagePublicSpots(player)) {
                return;
            }

            switch (payload.action()) {
                case "update" -> {
                    // 更新公开 Spot 的位置信息
                    boolean success = PublicSpotManager.getInstance().updatePublicSpot(
                            playerName, payload.oldName(),
                            payload.x(), payload.y(), payload.z(),
                            payload.yaw(), payload.pitch(), payload.dimension());
                    if (success) {
                        broadcastMessage(server, Text.translatable("spotteddog.public.spot.updated.broadcast", playerName, payload.oldName()).getString());
                    }
                }
                case "rename" -> {
                    // 重命名公开 Spot
                    boolean success = PublicSpotManager.getInstance().renamePublicSpot(
                            playerName, payload.oldName(), payload.newName());
                    if (success) {
                        broadcastMessage(server, Text.translatable("spotteddog.public.spot.renamed.broadcast", playerName, payload.oldName(), payload.newName()).getString());
                    }
                }
            }
        });
    }

    /**
     * 广播消息给服务器所有玩家。
     */
    private static void broadcastMessage(MinecraftServer server, String message) {
        if (server == null) return;
        var playerManager = server.getPlayerManager();
        if (playerManager == null) return;
        net.minecraft.text.Text text = net.minecraft.text.Text.literal(message);
        for (ServerPlayerEntity p : playerManager.getPlayerList()) {
            p.sendMessage(text, false);
        }
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
