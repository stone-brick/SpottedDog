package io.github.stone_brick.spotteddog.network.c2s;

import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;

/**
 * 客户端到服务端的日志管理请求数据包。
 */
public record TeleportLogAdminC2SPayload(
        String action,
        int count
) implements CustomPayload {

    public static final CustomPayload.Id<TeleportLogAdminC2SPayload> ID =
            CustomPayload.id("spotteddog/teleport_log_admin");

    public static final PacketCodec<PacketByteBuf, TeleportLogAdminC2SPayload> CODEC =
            PacketCodec.ofStatic(
                    (buf, payload) -> {
                        buf.writeString(payload.action());
                        buf.writeInt(payload.count());
                    },
                    buf -> new TeleportLogAdminC2SPayload(
                            buf.readString(),
                            buf.readInt()
                    )
            );

    @Override
    public CustomPayload.Id<TeleportLogAdminC2SPayload> getId() {
        return ID;
    }
}
