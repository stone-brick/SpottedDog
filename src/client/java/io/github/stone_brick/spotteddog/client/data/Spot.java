package io.github.stone_brick.spotteddog.client.data;

public class Spot {
    private String id;
    private String name;
    private double x;
    private double y;
    private double z;
    private float yaw;
    private float pitch;
    private String dimension;
    private String world;
    private String worldIdentifier; // 存档/服务器唯一标识
    private boolean isPublic; // 是否已公开（仅多人模式有效）

    public Spot() {
    }

    public Spot(String id, String name, double x, double y, double z, float yaw, float pitch, String dimension, String world, String worldIdentifier) {
        this(id, name, x, y, z, yaw, pitch, dimension, world, worldIdentifier, false);
    }

    public Spot(String id, String name, double x, double y, double z, float yaw, float pitch, String dimension, String world, String worldIdentifier, boolean isPublic) {
        this.id = id;
        this.name = name;
        this.x = x;
        this.y = y;
        this.z = z;
        this.yaw = yaw;
        this.pitch = pitch;
        this.dimension = dimension;
        this.world = world;
        this.worldIdentifier = worldIdentifier;
        this.isPublic = isPublic;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public double getX() {
        return x;
    }

    public void setX(double x) {
        this.x = x;
    }

    public double getY() {
        return y;
    }

    public void setY(double y) {
        this.y = y;
    }

    public double getZ() {
        return z;
    }

    public void setZ(double z) {
        this.z = z;
    }

    public String getDimension() {
        return dimension;
    }

    public void setDimension(String dimension) {
        this.dimension = dimension;
    }

    public String getWorld() {
        return world;
    }

    public void setWorld(String world) {
        this.world = world;
    }

    public String getWorldIdentifier() {
        return worldIdentifier;
    }

    public void setWorldIdentifier(String worldIdentifier) {
        this.worldIdentifier = worldIdentifier;
    }

    public void setPosition(double x, double y, double z, String dimension, String world, String worldIdentifier) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.dimension = dimension;
        this.world = world;
        this.worldIdentifier = worldIdentifier;
    }

    public void setPositionAndRotation(double x, double y, double z, float yaw, float pitch, String dimension, String world, String worldIdentifier) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.yaw = yaw;
        this.pitch = pitch;
        this.dimension = dimension;
        this.world = world;
        this.worldIdentifier = worldIdentifier;
    }

    public float getYaw() {
        return yaw;
    }

    public void setYaw(float yaw) {
        this.yaw = yaw;
    }

    public float getPitch() {
        return pitch;
    }

    public void setPitch(float pitch) {
        this.pitch = pitch;
    }

    public boolean isPublic() {
        return isPublic;
    }

    public void setPublic(boolean isPublic) {
        this.isPublic = isPublic;
    }
}
