package io.github.stone_brick.spotteddog.network.s2c;

import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;

import java.util.ArrayList;
import java.util.List;

/**
 * 公开 Spot 列表响应（S2C）。
 * 同时包含玩家的权限信息。
 */
public record PublicSpotListS2CPayload(
        List<PublicSpotInfo> spots,
        boolean canTeleport,
        boolean canManagePublicSpots
) implements CustomPayload {

    public static final CustomPayload.Id<PublicSpotListS2CPayload> ID =
            CustomPayload.id("spotteddog/public_spot_list_response");

    public static final PacketCodec<PacketByteBuf, PublicSpotListS2CPayload> CODEC =
            PacketCodec.ofStatic(
                    (buf, payload) -> {
                        buf.writeInt(payload.spots().size());
                        for (PublicSpotInfo spot : payload.spots()) {
                            buf.writeString(spot.ownerName());
                            buf.writeString(spot.displayName());
                            buf.writeDouble(spot.x());
                            buf.writeDouble(spot.y());
                            buf.writeDouble(spot.z());
                            buf.writeString(spot.dimension());
                        }
                        buf.writeBoolean(payload.canTeleport());
                        buf.writeBoolean(payload.canManagePublicSpots());
                    },
                    buf -> {
                        int size = buf.readInt();
                        List<PublicSpotInfo> spots = new ArrayList<>();
                        for (int i = 0; i < size; i++) {
                            spots.add(new PublicSpotInfo(
                                    buf.readString(),
                                    buf.readString(),
                                    buf.readDouble(),
                                    buf.readDouble(),
                                    buf.readDouble(),
                                    buf.readString()
                            ));
                        }
                        return new PublicSpotListS2CPayload(
                                spots,
                                buf.readBoolean(),
                                buf.readBoolean()
                        );
                    }
            );

    @Override
    public CustomPayload.Id<PublicSpotListS2CPayload> getId() {
        return ID;
    }

    /**
     * 公开 Spot 信息（用于网络传输）。
     */
    public record PublicSpotInfo(
            String ownerName,
            String displayName,
            double x,
            double y,
            double z,
            String dimension
    ) {
        public String getFullName() {
            return "-" + displayName + "-" + ownerName;
        }
    }

    /**
     * 创建只有 Spot 列表的负载（向后兼容）。
     */
    public static PublicSpotListS2CPayload create(List<PublicSpotInfo> spots) {
        return new PublicSpotListS2CPayload(spots, true, false);
    }

    /**
     * 创建包含权限信息的负载。
     */
    public static PublicSpotListS2CPayload create(List<PublicSpotInfo> spots,
                                                   boolean canTeleport, boolean canManagePublicSpots) {
        return new PublicSpotListS2CPayload(spots, canTeleport, canManagePublicSpots);
    }
}
