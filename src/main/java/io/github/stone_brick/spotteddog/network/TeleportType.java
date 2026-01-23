package io.github.stone_brick.spotteddog.network;

import net.minecraft.util.StringIdentifiable;

import java.util.Arrays;

/**
 * 传送类型枚举。
 */
public enum TeleportType implements StringIdentifiable {
    SPOT("spot"),
    DEATH("death"),
    RESPAWN("respawn"),
    SPAWN("spawn");

    private final String name;

    TeleportType(String name) {
        this.name = name;
    }

    @Override
    public String asString() {
        return name;
    }

    /**
     * 从字符串获取枚举值。
     */
    public static TeleportType fromString(String name) {
        return Arrays.stream(values())
                .filter(t -> t.asString().equals(name))
                .findFirst()
                .orElse(SPOT);
    }
}
