package io.github.stone_brick.spotteddog;

import io.github.stone_brick.spotteddog.event.AdminLogEvents;
import io.github.stone_brick.spotteddog.event.DefaultAdminLogLogger;
import io.github.stone_brick.spotteddog.event.DefaultTeleportLogLogger;
import io.github.stone_brick.spotteddog.event.TeleportLogEvents;
import io.github.stone_brick.spotteddog.server.network.PublicSpotHandler;
import io.github.stone_brick.spotteddog.server.network.TeleportLogHandler;
import io.github.stone_brick.spotteddog.server.network.TeleportRequestHandler;
import io.github.stone_brick.spotteddog.server.network.WhitelistAdminHandler;
import net.fabricmc.api.ModInitializer;

/**
 * SpottedDog 模组主入口。
 * 处理服务端功能，包括网络通信和命令注册。
 */
public class Spotteddog implements ModInitializer {

    @Override
    public void onInitialize() {
        // 注册服务端网络处理器
        TeleportRequestHandler.register();
        PublicSpotHandler.register();
        WhitelistAdminHandler.register();
        TeleportLogHandler.register();

        // 注册日志事件监听器
        TeleportLogEvents.TELEPORT.register(new DefaultTeleportLogLogger());
        AdminLogEvents.ADMIN_OPERATION.register(new DefaultAdminLogLogger());
    }
}
