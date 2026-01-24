package io.github.stone_brick.spotteddog.network.c2s;

import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;

/**
 * 客户端到服务端的传送请求数据包。
 */
public record TeleportRequestC2SPayload(
        String type,
        String targetName,
        double x,
        double y,
        double z,
        float yaw,
        float pitch,
        String dimension
) implements CustomPayload {

    public static final CustomPayload.Id<TeleportRequestC2SPayload> ID =
            CustomPayload.id("spotteddog/teleport_request");

    public static final PacketCodec<PacketByteBuf, TeleportRequestC2SPayload> CODEC =
            PacketCodec.ofStatic(
                    (buf, payload) -> {
                        buf.writeString(payload.type());
                        buf.writeString(payload.targetName());
                        buf.writeDouble(payload.x());
                        buf.writeDouble(payload.y());
                        buf.writeDouble(payload.z());
                        buf.writeFloat(payload.yaw());
                        buf.writeFloat(payload.pitch());
                        buf.writeString(payload.dimension());
                    },
                    buf -> new TeleportRequestC2SPayload(
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
    public CustomPayload.Id<TeleportRequestC2SPayload> getId() {
        return ID;
    }
}
