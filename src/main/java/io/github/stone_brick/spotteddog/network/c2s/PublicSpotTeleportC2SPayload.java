package io.github.stone_brick.spotteddog.network.c2s;

import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;

/**
 * 传送到公开 Spot 请求（C2S）。
 */
public record PublicSpotTeleportC2SPayload(
        String fullName,     // 完整名称：-ownerName:spotName
        String worldIdentifier
) implements CustomPayload {

    public static final CustomPayload.Id<PublicSpotTeleportC2SPayload> ID =
            CustomPayload.id("spotteddog/public_spot_teleport");

    public static final PacketCodec<PacketByteBuf, PublicSpotTeleportC2SPayload> CODEC =
            PacketCodec.ofStatic(
                    (buf, payload) -> {
                        buf.writeString(payload.fullName());
                        buf.writeString(payload.worldIdentifier());
                    },
                    buf -> new PublicSpotTeleportC2SPayload(buf.readString(), buf.readString())
            );

    @Override
    public CustomPayload.Id<PublicSpotTeleportC2SPayload> getId() {
        return ID;
    }

    public static PublicSpotTeleportC2SPayload of(String fullName, String worldIdentifier) {
        return new PublicSpotTeleportC2SPayload(fullName, worldIdentifier);
    }
}
