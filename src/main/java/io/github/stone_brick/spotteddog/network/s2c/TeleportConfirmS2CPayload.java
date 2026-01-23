package io.github.stone_brick.spotteddog.network.s2c;

import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;

/**
 * 服务端到客户端的传送确认数据包。
 */
public record TeleportConfirmS2CPayload(
        boolean success,
        String type,
        String targetName,
        String message
) implements CustomPayload {

    public static final CustomPayload.Id<TeleportConfirmS2CPayload> ID =
            CustomPayload.id("spotteddog/teleport_confirm");

    public static final PacketCodec<PacketByteBuf, TeleportConfirmS2CPayload> CODEC =
            PacketCodec.ofStatic(
                    (buf, payload) -> {
                        buf.writeBoolean(payload.success());
                        buf.writeString(payload.type());
                        buf.writeString(payload.targetName());
                        buf.writeString(payload.message() != null ? payload.message() : "");
                    },
                    buf -> new TeleportConfirmS2CPayload(
                            buf.readBoolean(),
                            buf.readString(),
                            buf.readString(),
                            buf.readString()
                    )
            );

    @Override
    public CustomPayload.Id<TeleportConfirmS2CPayload> getId() {
        return ID;
    }

    public static TeleportConfirmS2CPayload success(String type, String targetName) {
        return new TeleportConfirmS2CPayload(true, type, targetName, "");
    }

    public static TeleportConfirmS2CPayload failure(String type, String targetName, String reason) {
        return new TeleportConfirmS2CPayload(false, type, targetName, reason);
    }
}
