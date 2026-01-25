package io.github.stone_brick.spotteddog.network.c2s;

import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;

/**
 * 获取公开 Spot 列表请求（C2S）。
 */
public record PublicSpotListC2SPayload(
        String worldIdentifier  // 世界标识符，为空则获取所有
) implements CustomPayload {

    public static final CustomPayload.Id<PublicSpotListC2SPayload> ID =
            CustomPayload.id("spotteddog/public_spot_list_request");

    public static final PacketCodec<PacketByteBuf, PublicSpotListC2SPayload> CODEC =
            PacketCodec.ofStatic(
                    (buf, payload) -> buf.writeString(payload.worldIdentifier()),
                    buf -> new PublicSpotListC2SPayload(buf.readString())
            );

    @Override
    public CustomPayload.Id<PublicSpotListC2SPayload> getId() {
        return ID;
    }

    public static PublicSpotListC2SPayload forWorld(String worldIdentifier) {
        return new PublicSpotListC2SPayload(worldIdentifier);
    }

    public static PublicSpotListC2SPayload forAll() {
        return new PublicSpotListC2SPayload("");
    }
}
