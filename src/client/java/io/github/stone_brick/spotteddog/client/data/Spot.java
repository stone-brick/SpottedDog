package io.github.stone_brick.spotteddog.client.data;

import java.util.UUID;

/**
 * 标记点数据模型。
 * 不存储 worldIdentifier，由 PlayerDataManager 按存档/服务器分组管理。
 */
public class Spot {
    private String id;
    private String name;
    private double x;
    private double y;
    private double z;
    private float yaw;
    private float pitch;
    private String dimension;
    private boolean isPublic; // 是否已公开（仅多人模式有效）

    public Spot() {
    }

    public Spot(String name, double x, double y, double z, float yaw, float pitch, String dimension) {
        this.id = UUID.randomUUID().toString();
        this.name = name;
        this.x = x;
        this.y = y;
        this.z = z;
        this.yaw = yaw;
        this.pitch = pitch;
        this.dimension = dimension;
        this.isPublic = false;
    }

    public Spot(String id, String name, double x, double y, double z, float yaw, float pitch, String dimension, boolean isPublic) {
        this.id = id;
        this.name = name;
        this.x = x;
        this.y = y;
        this.z = z;
        this.yaw = yaw;
        this.pitch = pitch;
        this.dimension = dimension;
        this.isPublic = isPublic;
    }

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public double getX() { return x; }
    public void setX(double x) { this.x = x; }

    public double getY() { return y; }
    public void setY(double y) { this.y = y; }

    public double getZ() { return z; }
    public void setZ(double z) { this.z = z; }

    public float getYaw() { return yaw; }
    public void setYaw(float yaw) { this.yaw = yaw; }

    public float getPitch() { return pitch; }
    public void setPitch(float pitch) { this.pitch = pitch; }

    public String getDimension() { return dimension; }
    public void setDimension(String dimension) { this.dimension = dimension; }

    public boolean isPublic() { return isPublic; }
    public void setPublic(boolean isPublic) { this.isPublic = isPublic; }

    public void setPosition(double x, double y, double z, String dimension) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.dimension = dimension;
    }

    public void setPositionAndRotation(double x, double y, double z, float yaw, float pitch, String dimension) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.yaw = yaw;
        this.pitch = pitch;
        this.dimension = dimension;
    }
}
