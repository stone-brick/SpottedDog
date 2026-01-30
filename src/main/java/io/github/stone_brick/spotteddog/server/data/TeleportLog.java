package io.github.stone_brick.spotteddog.server.data;

import com.google.gson.annotations.SerializedName;
import java.time.Instant;

/**
 * 传送日志条目。
 * 记录每次传送的详细信息用于审计追踪。
 */
public class TeleportLog {

    @SerializedName("timestamp")
    private String timestamp;

    @SerializedName("player_name")
    private String playerName;

    @SerializedName("player_uuid")
    private String playerUuid;

    @SerializedName("teleport_type")
    private String teleportType;

    @SerializedName("spot_name")
    private String spotName;

    @SerializedName("source_dimension")
    private String sourceDimension;

    @SerializedName("source_x")
    private double sourceX;

    @SerializedName("source_y")
    private double sourceY;

    @SerializedName("source_z")
    private double sourceZ;

    @SerializedName("target_dimension")
    private String targetDimension;

    @SerializedName("target_x")
    private double targetX;

    @SerializedName("target_y")
    private double targetY;

    @SerializedName("target_z")
    private double targetZ;

    public TeleportLog() {
        this.timestamp = Instant.now().toString();
    }

    private TeleportLog(Builder builder) {
        this.timestamp = Instant.now().toString();
        this.playerName = builder.playerName;
        this.playerUuid = builder.playerUuid;
        this.teleportType = builder.teleportType;
        this.spotName = builder.spotName;
        this.sourceDimension = builder.sourceDimension;
        this.sourceX = builder.sourceX;
        this.sourceY = builder.sourceY;
        this.sourceZ = builder.sourceZ;
        this.targetDimension = builder.targetDimension;
        this.targetX = builder.targetX;
        this.targetY = builder.targetY;
        this.targetZ = builder.targetZ;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String playerName;
        private String playerUuid;
        private String teleportType;
        private String spotName;
        private String sourceDimension;
        private double sourceX;
        private double sourceY;
        private double sourceZ;
        private String targetDimension;
        private double targetX;
        private double targetY;
        private double targetZ;

        public Builder playerName(String playerName) {
            this.playerName = playerName;
            return this;
        }

        public Builder playerUuid(String playerUuid) {
            this.playerUuid = playerUuid;
            return this;
        }

        public Builder teleportType(String teleportType) {
            this.teleportType = teleportType;
            return this;
        }

        public Builder spotName(String spotName) {
            this.spotName = spotName;
            return this;
        }

        public Builder source(String dimension, double x, double y, double z) {
            this.sourceDimension = dimension;
            this.sourceX = x;
            this.sourceY = y;
            this.sourceZ = z;
            return this;
        }

        public Builder target(String dimension, double x, double y, double z) {
            this.targetDimension = dimension;
            this.targetX = x;
            this.targetY = y;
            this.targetZ = z;
            return this;
        }

        public TeleportLog build() {
            return new TeleportLog(this);
        }
    }

    // Getters
    public String getTimestamp() { return timestamp; }
    public String getPlayerName() { return playerName; }
    public String getPlayerUuid() { return playerUuid; }
    public String getTeleportType() { return teleportType; }
    public String getSpotName() { return spotName; }
    public String getSourceDimension() { return sourceDimension; }
    public double getSourceX() { return sourceX; }
    public double getSourceY() { return sourceY; }
    public double getSourceZ() { return sourceZ; }
    public String getTargetDimension() { return targetDimension; }
    public double getTargetX() { return targetX; }
    public double getTargetY() { return targetY; }
    public double getTargetZ() { return targetZ; }
}
