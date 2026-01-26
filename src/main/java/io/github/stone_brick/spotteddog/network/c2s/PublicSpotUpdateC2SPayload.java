package io.github.stone_brick.spotteddog.network.c2s;

import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;

/**
 * 公开 Spot 更新请求（C2S）。
 * 用于更新/重命名已公开的 Spot。
 */
public record PublicSpotUpdateC2SPayload(
        String action,      // "update" 或 "rename"
        String oldName,     // 原名称（rename 时必填）
        String newName,     // 新名称（rename 时必填）
        double x,
        double y,
        double z,
        float yaw,
        float pitch,
        String dimension
) implements CustomPayload {

    public static final CustomPayload.Id<PublicSpotUpdateC2SPayload> ID =
            CustomPayload.id("spotteddog/public_spot_update");

    public static final PacketCodec<PacketByteBuf, PublicSpotUpdateC2SPayload> CODEC =
            PacketCodec.ofStatic(
                    (buf, payload) -> {
                        buf.writeString(payload.action());
                        buf.writeString(payload.oldName());
                        buf.writeString(payload.newName());
                        buf.writeDouble(payload.x());
                        buf.writeDouble(payload.y());
                        buf.writeDouble(payload.z());
                        buf.writeFloat(payload.yaw());
                        buf.writeFloat(payload.pitch());
                        buf.writeString(payload.dimension());
                    },
                    buf -> new PublicSpotUpdateC2SPayload(
                            buf.readString(),
                            buf.readString(),
                            buf.readString(),
                            buf.readDouble(),
                            buf.readDouble(),
                            buf.readDouble(),
                            buf.readFloat(),
                            buf.readFloat(),
                            buf.readString()
                    )
            );

    @Override
    public CustomPayload.Id<PublicSpotUpdateC2SPayload> getId() {
        return ID;
    }

    /**
     * 创建更新位置请求。
     */
    public static PublicSpotUpdateC2SPayload update(String spotName, double x, double y, double z,
                                                    float yaw, float pitch, String dimension) {
        return new PublicSpotUpdateC2SPayload("update", spotName, spotName, x, y, z, yaw, pitch, dimension);
    }

    /**
     * 创建重命名请求。
     */
    public static PublicSpotUpdateC2SPayload rename(String oldName, String newName) {
        return new PublicSpotUpdateC2SPayload("rename", oldName, newName, 0, 0, 0, 0, 0, "");
    }
}
