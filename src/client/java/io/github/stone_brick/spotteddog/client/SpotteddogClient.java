package io.github.stone_brick.spotteddog.client;

import io.github.stone_brick.spotteddog.client.command.SpotCommand;
import io.github.stone_brick.spotteddog.client.network.PublicSpotListHandler;
import io.github.stone_brick.spotteddog.client.network.TeleportConfirmHandler;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.gui.widget.TextFieldWidget;

import java.lang.reflect.Field;

/**
 * 客户端模组入口。
 */
public class SpotteddogClient implements ClientModInitializer {

    // 使用反射访问 ChatScreen 的 protected chatField 字段
    private static final Field CHAT_FIELD;

    static {
        try {
            CHAT_FIELD = ChatScreen.class.getDeclaredField("chatField");
            CHAT_FIELD.setAccessible(true);
        } catch (NoSuchFieldException e) {
            throw new RuntimeException("Failed to access chatField", e);
        }
    }

    @Override
    public void onInitializeClient() {
        // 注册客户端网络处理器
        TeleportConfirmHandler.register();
        PublicSpotListHandler.register();

        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) ->
                SpotCommand.register(dispatcher));

        // 监听聊天输入，当玩家输入 "/spot " 时刷新公开 Spot 列表
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.currentScreen instanceof ChatScreen chatScreen) {
                try {
                    TextFieldWidget chatField = (TextFieldWidget) CHAT_FIELD.get(chatScreen);
                    String message = chatField.getText();
                    if (message != null && message.startsWith("/spot ")) {
                        SpotCommand.requestPublicSpotsIfNeeded();
                    }
                } catch (IllegalAccessException e) {
                    // 忽略反射异常
                }
            }
        });
    }
}
