package io.github.stone_brick.spotteddog.network.c2s;

import io.github.stone_brick.spotteddog.server.permission.WhitelistManager;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;

/**
 * 白名单管理请求（C2S）。
 */
public record WhitelistAdminC2SPayload(
        String playerName,           // 玩家名称
        WhitelistManager.WhitelistType type,  // 白名单类型
        boolean add                  // true = add, false = remove
) implements CustomPayload {

    public static final CustomPayload.Id<WhitelistAdminC2SPayload> ID =
            CustomPayload.id("spotteddog/whitelist_admin");

    public static final PacketCodec<PacketByteBuf, WhitelistAdminC2SPayload> CODEC =
            PacketCodec.ofStatic(
                    (buf, payload) -> {
                        buf.writeString(payload.playerName());
                        buf.writeString(payload.type().name());
                        buf.writeBoolean(payload.add());
                    },
                    buf -> new WhitelistAdminC2SPayload(
                            buf.readString(),
                            WhitelistManager.WhitelistType.valueOf(buf.readString()),
                            buf.readBoolean()
                    )
            );

    @Override
    public CustomPayload.Id<WhitelistAdminC2SPayload> getId() {
        return ID;
    }

    /**
     * 创建添加玩家到白名单的请求。
     */
    public static WhitelistAdminC2SPayload add(String playerName, WhitelistManager.WhitelistType type) {
        return new WhitelistAdminC2SPayload(playerName, type, true);
    }

    /**
     * 创建从白名单移除玩家的请求。
     */
    public static WhitelistAdminC2SPayload remove(String playerName, WhitelistManager.WhitelistType type) {
        return new WhitelistAdminC2SPayload(playerName, type, false);
    }
}
