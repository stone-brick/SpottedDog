package io.github.stone_brick.spotteddog.network.c2s;

import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;

/**
 * 获取公开 Spot 列表请求（C2S）。
 */
public record PublicSpotListC2SPayload() implements CustomPayload {

    public static final CustomPayload.Id<PublicSpotListC2SPayload> ID =
            CustomPayload.id("spotteddog/public_spot_list_request");

    public static final PacketCodec<PacketByteBuf, PublicSpotListC2SPayload> CODEC =
            PacketCodec.ofStatic(
                    (buf, payload) -> {},
                    buf -> new PublicSpotListC2SPayload()
            );

    @Override
    public CustomPayload.Id<PublicSpotListC2SPayload> getId() {
        return ID;
    }

    public static PublicSpotListC2SPayload create() {
        return new PublicSpotListC2SPayload();
    }
}
