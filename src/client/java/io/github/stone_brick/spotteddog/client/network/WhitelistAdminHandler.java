package io.github.stone_brick.spotteddog.client.network;

import io.github.stone_brick.spotteddog.network.c2s.WhitelistAdminC2SPayload;
import io.github.stone_brick.spotteddog.server.permission.WhitelistManager;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

/**
 * 客户端白名单管理处理器。
 */
@Environment(EnvType.CLIENT)
public final class WhitelistAdminHandler {

    private WhitelistAdminHandler() {
        // 工具类，禁止实例化
    }

    /**
     * 发送白名单操作请求到服务端。
     *
     * @param playerName 玩家名称
     * @param type       白名单类型
     * @param add        true = 添加，false = 移除
     */
    public static void sendWhitelistAction(String playerName, WhitelistManager.WhitelistType type, boolean add) {
        WhitelistAdminC2SPayload payload = add
                ? WhitelistAdminC2SPayload.add(playerName, type)
                : WhitelistAdminC2SPayload.remove(playerName, type);

        ClientPlayNetworking.send(payload);
    }
}
