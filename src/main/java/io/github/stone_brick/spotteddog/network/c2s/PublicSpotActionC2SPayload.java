package io.github.stone_brick.spotteddog.network.c2s;

import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;

/**
 * 公开 Spot 操作请求（C2S）。
 * 用于公开/取消公开 Spot。
 */
public record PublicSpotActionC2SPayload(
        String action,      // "publish" 或 "unpublish"
        String spotName,    // Spot 名称
        double x,
        double y,
        double z,
        float yaw,
        float pitch,
        String dimension
) implements CustomPayload {

    public static final CustomPayload.Id<PublicSpotActionC2SPayload> ID =
            CustomPayload.id("spotteddog/public_spot_action");

    public static final PacketCodec<PacketByteBuf, PublicSpotActionC2SPayload> CODEC =
            PacketCodec.ofStatic(
                    (buf, payload) -> {
                        buf.writeString(payload.action());
                        buf.writeString(payload.spotName());
                        buf.writeDouble(payload.x());
                        buf.writeDouble(payload.y());
                        buf.writeDouble(payload.z());
                        buf.writeFloat(payload.yaw());
                        buf.writeFloat(payload.pitch());
                        buf.writeString(payload.dimension());
                    },
                    buf -> new PublicSpotActionC2SPayload(
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
    public CustomPayload.Id<PublicSpotActionC2SPayload> getId() {
        return ID;
    }

    /**
     * 创建公开 Spot 请求。
     */
    public static PublicSpotActionC2SPayload publish(String spotName, double x, double y, double z,
                                                      float yaw, float pitch, String dimension) {
        return new PublicSpotActionC2SPayload("publish", spotName, x, y, z, yaw, pitch, dimension);
    }

    /**
     * 创建取消公开 Spot 请求。
     */
    public static PublicSpotActionC2SPayload unpublish(String spotName) {
        return new PublicSpotActionC2SPayload("unpublish", spotName, 0, 0, 0, 0, 0, "");
    }
}
