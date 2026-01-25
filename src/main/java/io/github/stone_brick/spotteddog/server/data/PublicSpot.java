package io.github.stone_brick.spotteddog.server.data;

import com.google.gson.annotations.SerializedName;

/**
 * 公开的 Spot 数据模型。
 * 记录在服务端，供所有玩家查看和传送。
 */
public class PublicSpot {

    @SerializedName("id")
    private String id;

    @SerializedName("owner_name")
    private String ownerName;

    @SerializedName("owner_uuid")
    private String ownerUuid;

    @SerializedName("display_name")
    private String displayName;

    @SerializedName("x")
    private double x;

    @SerializedName("y")
    private double y;

    @SerializedName("z")
    private double z;

    @SerializedName("yaw")
    private float yaw;

    @SerializedName("pitch")
    private float pitch;

    @SerializedName("dimension")
    private String dimension;

    @SerializedName("world")
    private String world;

    @SerializedName("world_identifier")
    private String worldIdentifier;

    @SerializedName("created_at")
    private long createdAt;

    public PublicSpot() {
    }

    public PublicSpot(String id, String ownerName, String ownerUuid, String displayName,
                      double x, double y, double z, float yaw, float pitch,
                      String dimension, String world, String worldIdentifier) {
        this.id = id;
        this.ownerName = ownerName;
        this.ownerUuid = ownerUuid;
        this.displayName = displayName;
        this.x = x;
        this.y = y;
        this.z = z;
        this.yaw = yaw;
        this.pitch = pitch;
        this.dimension = dimension;
        this.world = world;
        this.worldIdentifier = worldIdentifier;
        this.createdAt = System.currentTimeMillis();
    }

    /**
     * 获取公开 Spot 的完整名称，格式为：-displayName-ownerName
     * 例如：-home-stone_brick
     */
    public String getFullName() {
        return "-" + displayName + "-" + ownerName;
    }

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getOwnerName() { return ownerName; }
    public void setOwnerName(String ownerName) { this.ownerName = ownerName; }

    public String getOwnerUuid() { return ownerUuid; }
    public void setOwnerUuid(String ownerUuid) { this.ownerUuid = ownerUuid; }

    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }

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

    public String getWorld() { return world; }
    public void setWorld(String world) { this.world = world; }

    public String getWorldIdentifier() { return worldIdentifier; }
    public void setWorldIdentifier(String worldIdentifier) { this.worldIdentifier = worldIdentifier; }

    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }
}
