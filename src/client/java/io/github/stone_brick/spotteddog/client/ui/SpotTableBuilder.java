package io.github.stone_brick.spotteddog.client.ui;

import io.github.stone_brick.spotteddog.client.data.Spot;
import io.github.stone_brick.spotteddog.client.network.PublicSpotListHandler;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.HoverEvent;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;

/**
 * Spot 表格构建器。
 * 集中管理表格构建相关的功能和常量。
 */
public class SpotTableBuilder {

    // 列宽配置
    private static final int COL_NAME_WIDTH = 55;
    private static final int COL_DIM_WIDTH = 25;
    private static final int COL_COORD_WIDTH = 80;
    private static final int COL_VIS_WIDTH = 40;
    private static final int COL_OP_WIDTH = 40;
    private static final int COL_SPACING = 4;

    // 预定义列配置
    public static final int[] SINGLEPLAYER_COLS = {COL_NAME_WIDTH, COL_DIM_WIDTH, COL_COORD_WIDTH, COL_OP_WIDTH};
    public static final int[] MULTIPLAYER_COLS = {COL_NAME_WIDTH, COL_DIM_WIDTH, COL_COORD_WIDTH, COL_VIS_WIDTH, COL_OP_WIDTH};

    /**
     * 操作类型枚举
     */
    public enum SpotAction {
        TELEPORT('T', "spotteddog.action.teleport"),
        REMOVE('R', "spotteddog.action.remove"),
        UPDATE('U', "spotteddog.action.update"),
        RENAME('E', "spotteddog.action.rename"),
        PUBLIC('P', "spotteddog.action.public"),
        UNPUBLIC('P', "spotteddog.action.unpublish");

        final char symbol;
        final String translationKey;

        SpotAction(char symbol, String translationKey) {
            this.symbol = symbol;
            this.translationKey = translationKey;
        }

        /**
         * 获取带颜色的操作符号
         */
        public String getColoredSymbol() {
            return switch (this) {
                case TELEPORT -> "§bT";
                case REMOVE -> "§cR";
                case UPDATE -> "§aU";
                case RENAME -> "§eE";
                case PUBLIC, UNPUBLIC -> "§dP";
            };
        }

        /**
         * 获取操作的命令
         */
        public String getCommand(String spotName) {
            return switch (this) {
                case TELEPORT -> "/spot tp \"" + spotName + "\"";
                case REMOVE -> "/spot remove \"" + spotName + "\"";
                case UPDATE -> "/spot update \"" + spotName + "\"";
                case RENAME -> "/spot rename \"" + spotName + "\" ";
                case PUBLIC -> "/spot public \"" + spotName + "\"";
                case UNPUBLIC -> "/spot unpublic \"" + spotName + "\"";
            };
        }
    }

    /**
     * 获取玩家 Spot 的可用操作列表（按权限过滤）- 兼容旧版
     */
    public static SpotAction[] getPrivateSpotActions(Spot spot, boolean isSingleplayer) {
        if (isSingleplayer) {
            List<SpotAction> actions = new ArrayList<>();
            actions.add(SpotAction.TELEPORT);
            actions.add(SpotAction.REMOVE);
            actions.add(SpotAction.UPDATE);
            actions.add(SpotAction.RENAME);
            return actions.toArray(new SpotAction[0]);
        }

        String playerName = MinecraftClient.getInstance().player.getName().getString();
        boolean isOwner = spot.getName() != null;

        List<SpotAction> actions = new ArrayList<>();

        // T (传送)
        actions.add(SpotAction.TELEPORT);

        // R/U/E (删除/更新/重命名)
        if (isOwner) {
            actions.add(SpotAction.REMOVE);
            actions.add(SpotAction.UPDATE);
            actions.add(SpotAction.RENAME);
            // P/取消公开由调用方根据权限决定是否添加
        }

        return actions.toArray(new SpotAction[0]);
    }

    /**
     * 发送表格表头
     */
    public static void sendTableHeader(boolean isSingleplayer) {
        TextRenderer tr = MinecraftClient.getInstance().textRenderer;
        int[] widths = isSingleplayer ? SINGLEPLAYER_COLS : MULTIPLAYER_COLS;

        String name = Text.translatable("spotteddog.list.header.name").getString();
        String dim = Text.translatable("spotteddog.list.header.dimension").getString();
        String coord = Text.translatable("spotteddog.list.header.coord").getString();
        String action = Text.translatable("spotteddog.list.header.action").getString();

        if (isSingleplayer) {
            sendFeedback(padToWidth(tr, widths, name, dim, coord, action));
        } else {
            String vis = Text.translatable("spotteddog.list.header.visibility").getString();
            sendFeedback(padToWidth(tr, widths, name, dim, coord, vis, action));
        }

        // 分隔线
        sendSeparator(tr, widths);
    }

    /**
     * 发送表格分隔线
     */
    public static void sendSeparator(TextRenderer tr, int[] widths) {
        int totalWidth = calculateTotalWidth(widths);
        StringBuilder sep = new StringBuilder("§7");
        while (tr.getWidth(sep.toString()) < totalWidth) {
            sep.append("-");
        }
        sendFeedback(sep.toString());
    }

    /**
     * 发送玩家 Spot 表格行
     */
    public static void sendPrivateSpotRow(Spot spot, boolean isSingleplayer,
                                         SpotAction[] availableActions) {
        TextRenderer tr = MinecraftClient.getInstance().textRenderer;
        int[] widths = isSingleplayer ? SINGLEPLAYER_COLS : MULTIPLAYER_COLS;

        String coord = formatCoord(spot.getX(), spot.getY(), spot.getZ());
        String dimShort = formatDimension(spot.getDimension());
        String dimFull = localizeDimension(spot.getDimension());
        String coordFull = formatCoordFull(spot.getX(), spot.getY(), spot.getZ());

        if (isSingleplayer) {
            sendRow(tr, widths,
                    new String[]{spot.getName(), dimShort, coord},
                    new String[]{spot.getName(), dimFull, coordFull},
                    availableActions, spot.getName());
        } else {
            String playerName = MinecraftClient.getInstance().player.getName().getString();
            String visibility = PublicSpotListHandler.isSpotPublic(spot.getName(), playerName)
                    ? "§a" + Text.translatable("spotteddog.visibility.public").getString()
                    : "§7" + Text.translatable("spotteddog.visibility.private").getString();

            sendRow(tr, widths,
                    new String[]{spot.getName(), dimShort, coord, visibility},
                    new String[]{spot.getName(), dimFull, coordFull, null},
                    availableActions, spot.getName());
        }
    }

    /**
     * 发送其他玩家公开 Spot 表格行
     */
    public static void sendOtherPublicSpotRow(PublicSpotListHandler.PublicSpotInfo spot,
                                             SpotAction[] availableActions) {
        TextRenderer tr = MinecraftClient.getInstance().textRenderer;
        int[] widths = MULTIPLAYER_COLS;

        String displayName = spot.getDisplayName();
        String fullName = spot.getFullName();
        String coord = formatCoord(spot.getX(), spot.getY(), spot.getZ());
        String dimShort = formatDimension(spot.getDimension());
        String dimFull = localizeDimension(spot.getDimension());
        String coordFull = formatCoordFull(spot.getX(), spot.getY(), spot.getZ());
        String visibility = "§a" + Text.translatable("spotteddog.visibility.public").getString();

        String nameHover = spot.getDisplayName() + "\n" + Text.translatable("spotteddog.list.spot.owner", spot.getOwnerName()).getString();

        sendRow(tr, widths,
                new String[]{displayName, dimShort, coord, visibility},
                new String[]{nameHover, dimFull, coordFull, null},
                availableActions, fullName);
    }

    /**
     * 发送表格行
     */
    private static void sendRow(TextRenderer tr, int[] widths,
                               String[] parts, String[] hoverTexts,
                               SpotAction[] actions, String spotName) {
        if (parts == null) parts = new String[0];
        if (hoverTexts == null) hoverTexts = new String[parts.length];

        MutableText fullText = Text.empty();

        for (int i = 0; i < parts.length; i++) {
            String part = parts[i] != null ? parts[i] : "";
            String full = hoverTexts[i] != null ? hoverTexts[i] : part;
            String processed = truncateIfNeeded(tr, part, widths, i);

            MutableText partText = Text.literal(processed);
            if (full != null && !full.equals(processed)) {
                partText.setStyle(partText.getStyle()
                        .withHoverEvent(new HoverEvent.ShowText(Text.literal(full))));
            }
            fullText.append(partText);

            int spacesLen = (widths[i] - tr.getWidth(processed)) / tr.getWidth(" ");
            if (spacesLen > 0) {
                fullText.append(Text.literal(" ".repeat(spacesLen)));
            }
            fullText.append(Text.literal(" ".repeat(COL_SPACING)));
        }

        if (actions != null && actions.length > 0) {
            for (int i = 0; i < actions.length; i++) {
                SpotAction action = actions[i];
                MutableText actionText = Text.literal(action.getColoredSymbol());

                String actionDesc = Text.translatable(action.translationKey).getString();
                actionText.setStyle(actionText.getStyle()
                        .withHoverEvent(new HoverEvent.ShowText(Text.literal(actionDesc))));

                String command = action.getCommand(spotName);
                actionText.setStyle(actionText.getStyle()
                        .withClickEvent(new ClickEvent.SuggestCommand(command)));

                fullText.append(actionText);

                if (i < actions.length - 1) {
                    fullText.append(Text.literal("§7 "));
                }
            }
        } else {
            fullText.append(Text.literal("§7-"));
        }

        net.minecraft.client.network.ClientPlayerEntity player = getPlayer();
        if (player != null) {
            player.sendMessage(fullText, false);
        }
    }

    public static int calculateTotalWidth(int[] widths) {
        int total = 0;
        for (int width : widths) total += width;
        if (widths.length > 1) {
            total += COL_SPACING * (widths.length - 1);
        }
        return total;
    }

    private static String padToWidth(TextRenderer tr, int[] widths, String... parts) {
        if (parts == null) parts = new String[0];
        int totalTargetWidth = 0;
        int totalTextWidth = 0;
        int[] textWidths = new int[parts.length];

        for (int i = 0; i < parts.length; i++) {
            textWidths[i] = tr.getWidth(parts[i]);
            if (i < widths.length) {
                totalTargetWidth += widths[i];
            }
            totalTextWidth += textWidths[i];
        }
        int totalSpacesNeeded = totalTargetWidth - totalTextWidth;

        StringBuilder sb = new StringBuilder();
        int spacesRemaining = totalSpacesNeeded;
        for (int i = 0; i < parts.length; i++) {
            sb.append(parts[i]);
            if (i < widths.length && spacesRemaining > 0) {
                int spacesToAdd = Math.min(spacesRemaining, (widths[i] - textWidths[i]) / tr.getWidth(" "));
                if (spacesToAdd > 0) {
                    sb.append(" ".repeat(spacesToAdd));
                    spacesRemaining -= spacesToAdd;
                }
            }
            if (i < parts.length - 1) {
                sb.append(" ".repeat(COL_SPACING));
            }
        }
        return sb.toString();
    }

    private static String truncateIfNeeded(TextRenderer tr, String part, int[] widths, int colIndex) {
        if (part == null || part.isEmpty()) return "";
        if (colIndex >= widths.length) return part;

        int partWidth = tr.getWidth(part);
        int colWidth = widths[colIndex];

        if (partWidth > colWidth) {
            String truncated = truncateToWidth(tr, part, colWidth - tr.getWidth("..."));
            return truncated + "...";
        }
        return part;
    }

    private static String truncateToWidth(TextRenderer tr, String text, int maxWidth) {
        if (text == null || text.isEmpty() || maxWidth <= 0) return "";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            sb.append(c);
            if (tr.getWidth(sb.toString()) > maxWidth) {
                sb.deleteCharAt(sb.length() - 1);
                break;
            }
        }
        return sb.toString();
    }

    public static String formatDimension(String dimension) {
        return switch (dimension) {
            case "minecraft:overworld", "overworld" -> "§aO";
            case "minecraft:the_nether", "nether" -> "§cN";
            case "minecraft:the_end", "the_end", "end" -> "§dE";
            default -> dimension.substring(0, 1).toUpperCase();
        };
    }

    public static String localizeDimension(String dimension) {
        return switch (dimension) {
            case "minecraft:overworld", "overworld" -> Text.translatable("spotteddog.dimension.overworld").getString();
            case "minecraft:the_nether", "nether" -> Text.translatable("spotteddog.dimension.nether").getString();
            case "minecraft:the_end", "the_end", "end" -> Text.translatable("spotteddog.dimension.the_end").getString();
            default -> dimension;
        };
    }

    public static String formatCoord(double x, double y, double z) {
        return String.format("§f[%.0f, %.0f, %.0f]", x, y, z);
    }

    public static String formatCoordFull(double x, double y, double z) {
        return String.format("X: %.1f  Y: %.1f  Z: %.1f", x, y, z);
    }

    private static void sendFeedback(String key) {
        net.minecraft.client.network.ClientPlayerEntity player = getPlayer();
        if (player != null) {
            player.sendMessage(Text.translatable(key), false);
        }
    }

    private static net.minecraft.client.network.ClientPlayerEntity getPlayer() {
        return MinecraftClient.getInstance().player;
    }
}
