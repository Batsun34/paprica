package noknockback;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.util.InputUtil;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.text.TextColor;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import noknockback.mixin.client.GameRendererAccessor;

import org.lwjgl.glfw.GLFW;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class NoKnockbackClient implements ClientModInitializer {

    private static final double WALK_SPEED = 0.1D;
    private static final double SPRINT_MULTIPLIER = 1.3D;
    private static final double SPEED_MULTIPLIER = 0.75D;
    private static final int PLAYER_LIST_X = 6;
    private static final int PLAYER_LIST_Y = 6;
    private static final int PLAYER_LIST_PADDING = 3;
    private static final int PLAYER_LIST_BG_COLOR = 0x65000000;
    private static final int PLAYER_LIST_BORDER_COLOR = 0x43FFFFFF;
    private static final float PLAYER_LIST_TEXT_SCALE = 0.8F;
    private static final float PLAYER_LIST_ALPHA_MULTIPLIER = 0.7F;
    private static final float MAX_BOTTOM_RAY_START_HEIGHT = 300.0F;

    private static boolean speedEnabled = true;
    private static boolean playerEspEnabled = false;
    private static boolean playerRaysEnabled = false;
    private static boolean playerListEnabled = false;
    private static float rayThickness = 2.0F;
    private static float outlineThickness = 1.0F;
    private static float rayBottomStartHeight = 2.0F;
    private static RayOrigin rayOrigin = RayOrigin.BOTTOM;
    private static KeyBinding toggleKey;
    private static KeyBinding togglePlayerEspKey;
    private static KeyBinding togglePlayerRaysKey;
    private static KeyBinding togglePlayerListKey;
    private static KeyBinding openMenuKey;

    private Vec3d lastVelocity = Vec3d.ZERO;

    public static boolean isPlayerEspEnabled() {
        return playerEspEnabled;
    }

    public static boolean isSpeedEnabled() {
        return speedEnabled;
    }

    public static void setSpeedEnabled(boolean enabled) {
        if (speedEnabled == enabled) return;
        speedEnabled = enabled;
        saveConfigNow();
    }

    public static void setPlayerEspEnabled(boolean enabled) {
        if (playerEspEnabled == enabled) return;
        playerEspEnabled = enabled;
        saveConfigNow();
    }

    public static boolean isPlayerRaysEnabled() {
        return playerRaysEnabled;
    }

    public static void setPlayerRaysEnabled(boolean enabled) {
        if (playerRaysEnabled == enabled) return;
        playerRaysEnabled = enabled;
        saveConfigNow();
    }

    public static boolean isPlayerListEnabled() {
        return playerListEnabled;
    }

    public static void setPlayerListEnabled(boolean enabled) {
        if (playerListEnabled == enabled) return;
        playerListEnabled = enabled;
        saveConfigNow();
    }

    public static float getRayThickness() {
        return rayThickness;
    }

    public static void setRayThickness(float thickness) {
        float clamped = MathHelper.clamp(thickness, 0.5F, 8.0F);
        if (Math.abs(rayThickness - clamped) < 0.0001F) return;
        rayThickness = clamped;
        saveConfigNow();
    }

    public static float getOutlineThickness() {
        return outlineThickness;
    }

    public static void setOutlineThickness(float thickness) {
        float clamped = MathHelper.clamp(thickness, 0.5F, 6.0F);
        if (Math.abs(outlineThickness - clamped) < 0.0001F) return;
        outlineThickness = clamped;
        saveConfigNow();
    }

    public static float getRayBottomStartHeight() {
        return rayBottomStartHeight;
    }

    public static void setRayBottomStartHeight(float height) {
        float clamped = MathHelper.clamp(height, 0.0F, MAX_BOTTOM_RAY_START_HEIGHT);
        if (Math.abs(rayBottomStartHeight - clamped) < 0.0001F) return;
        rayBottomStartHeight = clamped;
        saveConfigNow();
    }

    public static RayOrigin getRayOrigin() {
        return rayOrigin;
    }

    public static void setRayOrigin(RayOrigin origin) {
        RayOrigin updated = origin == null ? RayOrigin.BOTTOM : origin;
        if (rayOrigin == updated) return;
        rayOrigin = updated;
        saveConfigNow();
    }

    public static KeyBinding getSpeedToggleKeyBinding() {
        return toggleKey;
    }

    public static KeyBinding getPlayerEspKeyBinding() {
        return togglePlayerEspKey;
    }

    public static KeyBinding getPlayerRaysKeyBinding() {
        return togglePlayerRaysKey;
    }

    public static KeyBinding getPlayerListKeyBinding() {
        return togglePlayerListKey;
    }

    public static KeyBinding getOpenMenuKeyBinding() {
        return openMenuKey;
    }

    public static void saveConfigNow() {
        NoKnockbackConfig.save(captureConfig());
    }

    public static int getPlayerHighlightColor(PlayerEntity player) {
        TextColor textColor = player.getDisplayName().getStyle().getColor();
        return textColor != null ? textColor.getRgb() : player.getTeamColorValue();
    }

    @Override
    public void onInitializeClient() {
        NoKnockbackConfig.Data loadedConfig = NoKnockbackConfig.load();
        applyLoadedConfig(loadedConfig);

        toggleKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.noknockback.toggle",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_V,
                "category.noknockback"
        ));

        togglePlayerEspKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.noknockback.player_esp",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_H,
                "category.noknockback"
        ));

        togglePlayerRaysKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.noknockback.player_rays",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_J,
                "category.noknockback"
        ));

        togglePlayerListKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.noknockback.player_list",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_K,
                "category.noknockback"
        ));

        openMenuKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.noknockback.menu",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_RIGHT_SHIFT,
                "category.noknockback"
        ));

        applyConfiguredKey(toggleKey, loadedConfig.speedToggleKey);
        applyConfiguredKey(togglePlayerEspKey, loadedConfig.playerEspKey);
        applyConfiguredKey(togglePlayerRaysKey, loadedConfig.playerRaysKey);
        applyConfiguredKey(togglePlayerListKey, loadedConfig.playerListKey);
        applyConfiguredKey(openMenuKey, loadedConfig.menuKey);
        KeyBinding.updateKeysByCode();
        saveConfigNow();

        ClientTickEvents.END_CLIENT_TICK.register(this::onTick);
        HudRenderCallback.EVENT.register(this::onHudRender);
    }

    private void onTick(MinecraftClient client) {
        while (openMenuKey.wasPressed()) {
            if (!(client.currentScreen instanceof NoKnockbackMenuScreen)) {
                client.setScreen(new NoKnockbackMenuScreen(client.currentScreen));
            }
        }

        PlayerEntity player = client.player;
        if (player == null || player.getWorld() == null) return;

        // Toggle
        while (toggleKey.wasPressed()) {
            setSpeedEnabled(!speedEnabled);
            player.sendMessage(
                    Text.literal("Speed: " + (speedEnabled ? "ON" : "OFF")),
                    true
            );
        }

        while (togglePlayerEspKey.wasPressed()) {
            setPlayerEspEnabled(!playerEspEnabled);
            player.sendMessage(
                    Text.literal("Player ESP: " + (playerEspEnabled ? "ON" : "OFF")),
                    true
            );
        }

        while (togglePlayerRaysKey.wasPressed()) {
            setPlayerRaysEnabled(!playerRaysEnabled);
            player.sendMessage(
                    Text.literal("Player Rays: " + (playerRaysEnabled ? "ON" : "OFF")),
                    true
            );
        }

        while (togglePlayerListKey.wasPressed()) {
            setPlayerListEnabled(!playerListEnabled);
            player.sendMessage(
                    Text.literal("Player List: " + (playerListEnabled ? "ON" : "OFF")),
                    true
            );
        }

        Vec3d velocity = player.getVelocity();

        // ===== KNOCKBACK =====
        if (player.hurtTime > 0) {
            player.setVelocity(
                    lastVelocity.x,
                    velocity.y,
                    lastVelocity.z
            );
            return;
        }

        lastVelocity = velocity;

        if (!speedEnabled) return;

        // ===== ДВИЖЕНИЕ =====
        double forward = 0;
        double sideways = 0;

        if (client.options.forwardKey.isPressed()) forward += 1;
        if (client.options.backKey.isPressed()) forward -= 1;
        if (client.options.leftKey.isPressed()) sideways += 1;
        if (client.options.rightKey.isPressed()) sideways -= 1;

        if (forward == 0 && sideways == 0) return;

        double yawRad = Math.toRadians(player.getYaw());
        double sin = Math.sin(yawRad);
        double cos = Math.cos(yawRad);

        double speed = WALK_SPEED;
        if (player.isSprinting()) {
            speed *= SPRINT_MULTIPLIER;
        }

        speed *= SPEED_MULTIPLIER; // +10%

        double motionX = (forward * -sin + sideways * cos) * speed;
        double motionZ = (forward *  cos + sideways * sin) * speed;


        if (player.isOnGround() && player.isSneaking()) {
            player.setVelocity(motionX, velocity.y, motionZ);
        }

    }

    private void onHudRender(DrawContext drawContext, RenderTickCounter tickCounter) {
        MinecraftClient client = MinecraftClient.getInstance();
        PlayerEntity localPlayer = client.player;
        if (localPlayer == null || client.world == null) return;

        if (playerListEnabled) {
            renderPlayerList(drawContext, client, localPlayer);
        }
        if (!playerRaysEnabled || client.gameRenderer == null) return;

        Camera camera = client.gameRenderer.getCamera();
        if (camera == null || !camera.isReady()) return;

        int screenWidth = drawContext.getScaledWindowWidth();
        int screenHeight = drawContext.getScaledWindowHeight();
        if (screenWidth <= 0 || screenHeight <= 0) return;

        float tickDelta = tickCounter.getTickDelta(false);
        float fov = client.options.getFov().getValue().floatValue();
        if (client.gameRenderer instanceof GameRendererAccessor accessor) {
            fov = accessor.noknockback$getFov(camera, tickDelta, true);
        }
        final float renderFov = fov;

        float startX = screenWidth * 0.5F;
        float startY = rayOrigin == RayOrigin.CENTER
                ? screenHeight * 0.5F
                : MathHelper.clamp(screenHeight - 1.0F - rayBottomStartHeight, 1.0F, screenHeight - 1.0F);
        Vector3f projected = new Vector3f();

        drawContext.draw(vertexConsumers -> {
            VertexConsumer lineConsumer = vertexConsumers.getBuffer(RenderLayer.getDebugQuads());
            Matrix4f matrix = drawContext.getMatrices().peek().getPositionMatrix();

            for (PlayerEntity target : client.world.getPlayers()) {
                if (target == localPlayer || target.isRemoved()) continue;

                Vec3d targetPos = target.getLerpedPos(tickDelta).add(0.0, target.getHeight() * 0.5, 0.0);
                if (!projectToIndicator(targetPos, camera, screenWidth, screenHeight, renderFov, projected)) continue;

                int color = 0xFF000000 | getPlayerHighlightColor(target);
                drawThickRay(matrix, lineConsumer, startX, startY, projected.x, projected.y, rayThickness, color);
            }
        });
    }

    private static void drawThickRay(
            Matrix4f matrix,
            VertexConsumer consumer,
            float x1,
            float y1,
            float x2,
            float y2,
            float thickness,
            int color
    ) {
        float dx = x2 - x1;
        float dy = y2 - y1;
        float len = MathHelper.sqrt(dx * dx + dy * dy);
        if (len < 0.0001F) return;

        float half = Math.max(0.5F, thickness) * 0.5F;
        float nx = -dy / len * half;
        float ny = dx / len * half;

        consumer.vertex(matrix, x1 - nx, y1 - ny, 0.0F).color(color);
        consumer.vertex(matrix, x1 + nx, y1 + ny, 0.0F).color(color);
        consumer.vertex(matrix, x2 + nx, y2 + ny, 0.0F).color(color);
        consumer.vertex(matrix, x2 - nx, y2 - ny, 0.0F).color(color);
    }

    private void renderPlayerList(DrawContext drawContext, MinecraftClient client, PlayerEntity localPlayer) {
        if (client.textRenderer == null || client.world == null) return;

        Map<Integer, List<PlayerDistanceEntry>> groups = new LinkedHashMap<>();
        for (PlayerEntity target : client.world.getPlayers()) {
            if (target == localPlayer || target.isRemoved()) continue;

            int color = getPlayerHighlightColor(target) & 0xFFFFFF;
            double distance = localPlayer.getPos().distanceTo(target.getPos());
            String name = target.getName().getString();
            groups.computeIfAbsent(color, ignored -> new ArrayList<>()).add(new PlayerDistanceEntry(name, distance));
        }

        if (groups.isEmpty()) return;

        List<Integer> sortedColors = new ArrayList<>(groups.keySet());
        sortedColors.sort(Integer::compareUnsigned);

        List<PlayerListLine> lines = new ArrayList<>();
        for (int i = 0; i < sortedColors.size(); i++) {
            Integer color = sortedColors.get(i);
            List<PlayerDistanceEntry> entries = groups.get(color);
            entries.sort(Comparator.comparingDouble(PlayerDistanceEntry::distance));

            for (PlayerDistanceEntry entry : entries) {
                lines.add(new PlayerListLine(
                        entry.name() + " - " + (int) Math.round(entry.distance()) + " m",
                        0xFF000000 | color
                ));
            }
        }

        int textLineHeight = Math.max(6, Math.round(client.textRenderer.fontHeight * PLAYER_LIST_TEXT_SCALE) + 1);
        int maxVisibleLines = Math.max(2, (drawContext.getScaledWindowHeight() - PLAYER_LIST_Y - 16) / textLineHeight);
        if (lines.size() > maxVisibleLines) {
            int hiddenLines = lines.size() - maxVisibleLines + 1;
            lines = new ArrayList<>(lines.subList(0, maxVisibleLines - 1));
            lines.add(new PlayerListLine("... +" + hiddenLines, 0xFFFFFFFF));
        }

        int maxWidth = 0;
        for (PlayerListLine line : lines) {
            int scaledWidth = Math.round(client.textRenderer.getWidth(line.text()) * PLAYER_LIST_TEXT_SCALE);
            maxWidth = Math.max(maxWidth, scaledWidth);
        }

        int panelWidth = maxWidth + PLAYER_LIST_PADDING * 2;
        int panelHeight = lines.size() * textLineHeight + PLAYER_LIST_PADDING * 2;
        int x1 = PLAYER_LIST_X;
        int y1 = PLAYER_LIST_Y;
        int x2 = x1 + panelWidth;
        int y2 = y1 + panelHeight;

        drawContext.fill(x1 - 1, y1 - 1, x2 + 1, y2 + 1, PLAYER_LIST_BORDER_COLOR);
        drawContext.fill(x1, y1, x2, y2, PLAYER_LIST_BG_COLOR);

        int textX = x1 + PLAYER_LIST_PADDING;
        int textY = y1 + PLAYER_LIST_PADDING;
        for (PlayerListLine line : lines) {
            int textColor = withAlpha(line.color(), PLAYER_LIST_ALPHA_MULTIPLIER);
            drawContext.getMatrices().push();
            drawContext.getMatrices().translate(textX, textY, 0.0F);
            drawContext.getMatrices().scale(PLAYER_LIST_TEXT_SCALE, PLAYER_LIST_TEXT_SCALE, 1.0F);
            drawContext.drawTextWithShadow(client.textRenderer, line.text(), 0, 0, textColor);
            drawContext.getMatrices().pop();
            textY += textLineHeight;
        }
    }

    private static boolean projectToIndicator(
            Vec3d worldPos,
            Camera camera,
            int screenWidth,
            int screenHeight,
            float fovDegrees,
            Vector3f out
    ) {
        Vec3d cameraPos = camera.getPos();
        Vector3f cameraSpace = new Vector3f(
                (float) (worldPos.x - cameraPos.x),
                (float) (worldPos.y - cameraPos.y),
                (float) (worldPos.z - cameraPos.z)
        );
        camera.getRotation().transformInverse(cameraSpace);

        float tanHalfFovY = (float) Math.tan(Math.toRadians(fovDegrees) * 0.5);
        if (tanHalfFovY <= 0.0F) return false;
        float aspect = (float) screenWidth / (float) screenHeight;
        float tanHalfFovX = tanHalfFovY * aspect;
        if (tanHalfFovX <= 0.0F) return false;

        float zAbs = Math.max(0.0001F, Math.abs(cameraSpace.z));
        float ndcX = (cameraSpace.x / zAbs) / tanHalfFovX;
        float ndcY = (cameraSpace.y / zAbs) / tanHalfFovY;
        boolean inFront = cameraSpace.z < -0.0001F;

        if (inFront && Math.abs(ndcX) <= 1.0F && Math.abs(ndcY) <= 1.0F) {
            out.set(
                    (ndcX * 0.5F + 0.5F) * screenWidth,
                    (0.5F - ndcY * 0.5F) * screenHeight,
                    0.0F
            );
            return true;
        }

        float maxComponent = Math.max(Math.abs(ndcX), Math.abs(ndcY));
        if (inFront) {
            if (maxComponent > 1.0F) {
                ndcX /= maxComponent;
                ndcY /= maxComponent;
            }
        } else {
            if (maxComponent < 0.0001F) {
                ndcX = 0.0F;
                ndcY = cameraSpace.y >= 0.0F ? -1.0F : 1.0F;
            } else {
                ndcX /= maxComponent;
                ndcY /= maxComponent;
            }
        }

        float margin = 6.0F;
        float x = (ndcX * 0.5F + 0.5F) * (screenWidth - margin * 2.0F) + margin;
        float y = (0.5F - ndcY * 0.5F) * (screenHeight - margin * 2.0F) + margin;
        out.set(
                MathHelper.clamp(x, margin, screenWidth - margin),
                MathHelper.clamp(y, margin, screenHeight - margin),
                0.0F
        );
        return true;
    }

    private static int withAlpha(int color, float multiplier) {
        int alpha = (color >>> 24) & 0xFF;
        int adjustedAlpha = MathHelper.clamp(Math.round(alpha * multiplier), 0, 255);
        return (adjustedAlpha << 24) | (color & 0x00FFFFFF);
    }

    private static void applyLoadedConfig(NoKnockbackConfig.Data config) {
        if (config == null) return;

        speedEnabled = config.speedEnabled;
        playerEspEnabled = config.playerEspEnabled;
        playerRaysEnabled = config.playerRaysEnabled;
        playerListEnabled = config.playerListEnabled;
        rayThickness = MathHelper.clamp(config.rayThickness, 0.5F, 8.0F);
        outlineThickness = MathHelper.clamp(config.outlineThickness, 0.5F, 6.0F);
        rayBottomStartHeight = MathHelper.clamp(config.rayBottomStartHeight, 0.0F, MAX_BOTTOM_RAY_START_HEIGHT);
        rayOrigin = parseRayOrigin(config.rayOrigin);
    }

    private static void applyConfiguredKey(KeyBinding keyBinding, String translationKey) {
        if (keyBinding == null || translationKey == null || translationKey.isBlank()) return;

        try {
            keyBinding.setBoundKey(InputUtil.fromTranslationKey(translationKey));
        } catch (IllegalArgumentException ignored) {
        }
    }

    private static RayOrigin parseRayOrigin(String rawValue) {
        if (rawValue == null || rawValue.isBlank()) {
            return RayOrigin.BOTTOM;
        }

        try {
            return RayOrigin.valueOf(rawValue.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ignored) {
            return RayOrigin.BOTTOM;
        }
    }

    private static NoKnockbackConfig.Data captureConfig() {
        NoKnockbackConfig.Data data = new NoKnockbackConfig.Data();
        data.speedEnabled = speedEnabled;
        data.playerEspEnabled = playerEspEnabled;
        data.playerRaysEnabled = playerRaysEnabled;
        data.playerListEnabled = playerListEnabled;
        data.rayThickness = rayThickness;
        data.outlineThickness = outlineThickness;
        data.rayBottomStartHeight = rayBottomStartHeight;
        data.rayOrigin = rayOrigin.name();

        if (toggleKey != null) {
            data.speedToggleKey = toggleKey.getBoundKeyTranslationKey();
        }
        if (togglePlayerEspKey != null) {
            data.playerEspKey = togglePlayerEspKey.getBoundKeyTranslationKey();
        }
        if (togglePlayerRaysKey != null) {
            data.playerRaysKey = togglePlayerRaysKey.getBoundKeyTranslationKey();
        }
        if (togglePlayerListKey != null) {
            data.playerListKey = togglePlayerListKey.getBoundKeyTranslationKey();
        }
        if (openMenuKey != null) {
            data.menuKey = openMenuKey.getBoundKeyTranslationKey();
        }

        return data;
    }

    private record PlayerDistanceEntry(String name, double distance) {
    }

    private record PlayerListLine(String text, int color) {
    }

    public enum RayOrigin {
        BOTTOM,
        CENTER
    }
}
