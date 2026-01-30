package io.github.stone_brick.spotteddog.client.network;

import io.github.stone_brick.spotteddog.network.c2s.PublicSpotListC2SPayload;
import io.github.stone_brick.spotteddog.network.c2s.PublicSpotUpdateC2SPayload;
import io.github.stone_brick.spotteddog.network.s2c.PublicSpotListS2CPayload;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * 客户端公开 Spot 列表处理器。
 */
@Environment(EnvType.CLIENT)
public class PublicSpotListHandler {

    /**
     * 公开 Spot 信息。
     */
    public static class PublicSpotInfo {
        private final String ownerName;
        private final String displayName;
        private final double x;
        private final double y;
        private final double z;
        private final String dimension;

        public PublicSpotInfo(String ownerName, String displayName, double x, double y, double z,
                              String dimension) {
            this.ownerName = ownerName;
            this.displayName = displayName;
            this.x = x;
            this.y = y;
            this.z = z;
            this.dimension = dimension;
        }

        public String getFullName() {
            return "-" + displayName + "-" + ownerName;
        }

        public String getOwnerName() { return ownerName; }
        public String getDisplayName() { return displayName; }
        public double getX() { return x; }
        public double getY() { return y; }
        public double getZ() { return z; }
        public String getDimension() { return dimension; }
    }

    private static final List<PublicSpotInfo> publicSpots = new ArrayList<>();
    private static Consumer<List<PublicSpotInfo>> listCallback;

    /**
     * 注册处理器。
     */
    public static void register() {
        // 动态注册 S2C Payload 类型（单人模式需避免重复注册）
        try {
            PayloadTypeRegistry.playS2C().register(
                    PublicSpotListS2CPayload.ID,
                    PublicSpotListS2CPayload.CODEC
            );
        } catch (IllegalArgumentException e) {
            // 已注册过，忽略
        }

        // 注册接收处理器
        ClientPlayNetworking.registerGlobalReceiver(PublicSpotListS2CPayload.ID, (payload, context) -> {
            var player = context.player();
            if (player == null) return;

            // 更新缓存
            publicSpots.clear();
            for (PublicSpotListS2CPayload.PublicSpotInfo spot : payload.spots()) {
                publicSpots.add(new PublicSpotInfo(
                        spot.ownerName(),
                        spot.displayName(),
                        spot.x(), spot.y(), spot.z(),
                        spot.dimension()
                ));
            }

            // 回调（如果调用方需要自定义处理）
            if (listCallback != null) {
                listCallback.accept(new ArrayList<>(publicSpots));
                listCallback = null;
                return; // 回调负责显示消息，不显示默认消息
            }

            // 显示默认消息
            if (publicSpots.isEmpty()) {
                player.sendMessage(net.minecraft.text.Text.translatable("spotteddog.public.none"), false);
            } else {
                player.sendMessage(net.minecraft.text.Text.translatable("spotteddog.public.list.updated", publicSpots.size()), false);
            }
        });
    }

    /**
     * 请求获取公开 Spot 列表。
     */
    public static void requestPublicSpots() {
        ClientPlayNetworking.send(PublicSpotListC2SPayload.create());
    }

    /**
     * 请求获取公开 Spot 列表（异步回调）。
     */
    public static void requestPublicSpotsWithCallback(Consumer<List<PublicSpotInfo>> callback) {
        listCallback = callback;
        requestPublicSpots();
    }

    /**
     * 获取缓存的公开 Spot 列表。
     */
    public static List<PublicSpotInfo> getPublicSpots() {
        return new ArrayList<>(publicSpots);
    }

    /**
     * 清除缓存。
     */
    public static void clearCache() {
        publicSpots.clear();
    }

    /**
     * 检查指定名称的 Spot 是否已公开（本地缓存）。
     */
    public static boolean isSpotPublic(String spotName, String playerName) {
        for (PublicSpotInfo spot : publicSpots) {
            if (spot.getOwnerName().equals(playerName) && spot.getDisplayName().equals(spotName)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 发送更新公开 Spot 位置的请求。
     */
    public static void sendUpdatePublicSpot(String spotName, double x, double y, double z,
                                            float yaw, float pitch, String dimension) {
        ClientPlayNetworking.send(PublicSpotUpdateC2SPayload.update(spotName, x, y, z, yaw, pitch, dimension));
    }

    /**
     * 发送重命名公开 Spot 的请求。
     */
    public static void sendRenamePublicSpot(String oldName, String newName) {
        ClientPlayNetworking.send(PublicSpotUpdateC2SPayload.rename(oldName, newName));
    }
}
