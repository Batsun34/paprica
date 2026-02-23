package noknockback;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.HudLayerRegistrationCallback;
import net.fabricmc.fabric.api.client.rendering.v1.IdentifiedLayer;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.util.InputUtil;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.text.TextColor;
import net.minecraft.util.Identifier;
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
    private static final int DEFAULT_PLAYER_LIST_X = 6;
    private static final int DEFAULT_PLAYER_LIST_Y = 6;
    private static final float DEFAULT_PLAYER_LIST_TEXT_SCALE = 0.8F;
    private static final int DEFAULT_PLAYER_LIST_MAX_HEIGHT = 280;
    private static final float DEFAULT_PLAYER_LIST_ALPHA_MULTIPLIER = 0.7F;
    private static final float DEFAULT_RAY_LABEL_TEXT_SCALE = 0.75F;
    private static final float DEFAULT_TARGET_HEALTH_TEXT_SCALE = 1.0F;
    private static final float DEFAULT_EQUIP_ICON_SCALE = 0.75F;
    private static final float DEFAULT_STYLE_SATURATION = 1.35F;
    private static final float DEFAULT_STYLE_ANIMATION_SPEED = 1.0F;
    private static final int MAX_PLAYER_LIST_OFFSET = 4096;
    private static final int PLAYER_LIST_PADDING = 3;
    private static final int PLAYER_LIST_BG_COLOR = 0x65000000;
    private static final int PLAYER_LIST_BORDER_COLOR = 0x43FFFFFF;
    private static final float MAX_BOTTOM_RAY_START_HEIGHT = 300.0F;
    private static final float RAY_LABEL_POSITION_FACTOR = 0.62F;
    private static final float RAY_LABEL_PERP_OFFSET = 9.0F;
    private static final int RAY_LABEL_BG_COLOR = 0x6A000000;
    private static final int RAY_LABEL_TEXT_COLOR = 0xF0FFFFFF;
    private static final int ARMOR_OVERLAY_ICON_SPACING = 1;
    private static final int TARGET_HEALTH_BG_COLOR = 0x7A000000;
    private static final int TARGET_HEALTH_COLOR_LIME = 0xFF8DFF39;
    private static final int TARGET_HEALTH_COLOR_YELLOW = 0xFFFFE44A;
    private static final int TARGET_HEALTH_COLOR_RED = 0xFFFF4F4F;
    private static final int TARGET_HEALTH_COLOR_DARK_RED = 0xFF7A0019;
    private static final float TWO_PI = (float) (Math.PI * 2.0);
    private static final Identifier HUD_OVERLAY_LAYER_ID = Identifier.of("paprika", "hud_overlay");

    private static boolean speedEnabled = true;
    private static boolean playerEspEnabled = false;
    private static boolean playerArmorOverlayEnabled = false;
    private static boolean playerRaysEnabled = false;
    private static boolean playerListEnabled = false;
    private static boolean targetHealthOverlayEnabled = false;
    private static boolean targetHealthDynamicColorEnabled = true;
    private static boolean distanceDisplayEnabled = true;
    private static boolean heldItemOverlayEnabled = false;
    private static boolean rayVisualGlowEnabled = false;
    private static boolean armorVisualGlowEnabled = false;
    private static boolean heldItemVisualGlowEnabled = false;
    private static boolean distanceVisualGlowEnabled = false;
    private static float rayThickness = 2.0F;
    private static float outlineThickness = 1.0F;
    private static float rayBottomStartHeight = 2.0F;
    private static float distanceTextScale = DEFAULT_RAY_LABEL_TEXT_SCALE;
    private static float armorOverlayScale = DEFAULT_EQUIP_ICON_SCALE;
    private static float heldItemOverlayScale = DEFAULT_EQUIP_ICON_SCALE;
    private static float targetHealthTextScale = DEFAULT_TARGET_HEALTH_TEXT_SCALE;
    private static float playerListTextScale = DEFAULT_PLAYER_LIST_TEXT_SCALE;
    private static int playerListMaxHeight = DEFAULT_PLAYER_LIST_MAX_HEIGHT;
    private static float playerListAlphaMultiplier = DEFAULT_PLAYER_LIST_ALPHA_MULTIPLIER;
    private static float rayVisualSaturationBoost = DEFAULT_STYLE_SATURATION;
    private static float rayVisualAnimationSpeed = DEFAULT_STYLE_ANIMATION_SPEED;
    private static float armorVisualSaturationBoost = DEFAULT_STYLE_SATURATION;
    private static float armorVisualAnimationSpeed = DEFAULT_STYLE_ANIMATION_SPEED;
    private static float heldItemVisualSaturationBoost = DEFAULT_STYLE_SATURATION;
    private static float heldItemVisualAnimationSpeed = DEFAULT_STYLE_ANIMATION_SPEED;
    private static float distanceVisualSaturationBoost = DEFAULT_STYLE_SATURATION;
    private static float distanceVisualAnimationSpeed = DEFAULT_STYLE_ANIMATION_SPEED;
    private static int playerListOffsetX = DEFAULT_PLAYER_LIST_X;
    private static int playerListOffsetY = DEFAULT_PLAYER_LIST_Y;
    private static RayOrigin rayOrigin = RayOrigin.BOTTOM;
    private static OverlayAnchorMode armorAnchorMode = OverlayAnchorMode.ABOVE_PLAYER;
    private static OverlayAnchorMode heldItemAnchorMode = OverlayAnchorMode.ABOVE_PLAYER;
    private static OverlayAnchorMode distanceAnchorMode = OverlayAnchorMode.RAY_MIDDLE;
    private static VisualColorMode rayVisualColorMode = VisualColorMode.VIVID;
    private static VisualColorMode armorVisualColorMode = VisualColorMode.VIVID;
    private static VisualColorMode heldItemVisualColorMode = VisualColorMode.VIVID;
    private static VisualColorMode distanceVisualColorMode = VisualColorMode.VIVID;
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

    public static boolean isPlayerArmorOverlayEnabled() {
        return playerArmorOverlayEnabled;
    }

    public static void setPlayerArmorOverlayEnabled(boolean enabled) {
        if (playerArmorOverlayEnabled == enabled) return;
        playerArmorOverlayEnabled = enabled;
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

    public static boolean isTargetHealthOverlayEnabled() {
        return targetHealthOverlayEnabled;
    }

    public static void setTargetHealthOverlayEnabled(boolean enabled) {
        if (targetHealthOverlayEnabled == enabled) return;
        targetHealthOverlayEnabled = enabled;
        saveConfigNow();
    }

    public static boolean isTargetHealthDynamicColorEnabled() {
        return targetHealthDynamicColorEnabled;
    }

    public static void setTargetHealthDynamicColorEnabled(boolean enabled) {
        if (targetHealthDynamicColorEnabled == enabled) return;
        targetHealthDynamicColorEnabled = enabled;
        saveConfigNow();
    }

    public static boolean isDistanceDisplayEnabled() {
        return distanceDisplayEnabled;
    }

    public static void setDistanceDisplayEnabled(boolean enabled) {
        if (distanceDisplayEnabled == enabled) return;
        distanceDisplayEnabled = enabled;
        saveConfigNow();
    }

    public static boolean isHeldItemOverlayEnabled() {
        return heldItemOverlayEnabled;
    }

    public static void setHeldItemOverlayEnabled(boolean enabled) {
        if (heldItemOverlayEnabled == enabled) return;
        heldItemOverlayEnabled = enabled;
        saveConfigNow();
    }

    public static OverlayAnchorMode getArmorAnchorMode() {
        return armorAnchorMode;
    }

    public static void setArmorAnchorMode(OverlayAnchorMode mode) {
        OverlayAnchorMode updated = mode == null ? OverlayAnchorMode.ABOVE_PLAYER : mode;
        if (armorAnchorMode == updated) return;
        armorAnchorMode = updated;
        saveConfigNow();
    }

    public static OverlayAnchorMode getHeldItemAnchorMode() {
        return heldItemAnchorMode;
    }

    public static void setHeldItemAnchorMode(OverlayAnchorMode mode) {
        OverlayAnchorMode updated = mode == null ? OverlayAnchorMode.ABOVE_PLAYER : mode;
        if (heldItemAnchorMode == updated) return;
        heldItemAnchorMode = updated;
        saveConfigNow();
    }

    public static OverlayAnchorMode getDistanceAnchorMode() {
        return distanceAnchorMode;
    }

    public static void setDistanceAnchorMode(OverlayAnchorMode mode) {
        OverlayAnchorMode updated = mode == null ? OverlayAnchorMode.RAY_MIDDLE : mode;
        if (distanceAnchorMode == updated) return;
        distanceAnchorMode = updated;
        saveConfigNow();
    }

    public static float getArmorOverlayScale() {
        return armorOverlayScale;
    }

    public static void setArmorOverlayScale(float scale) {
        float clamped = MathHelper.clamp(scale, 0.35F, 2.5F);
        if (Math.abs(armorOverlayScale - clamped) < 0.0001F) return;
        armorOverlayScale = clamped;
        saveConfigNow();
    }

    public static float getHeldItemOverlayScale() {
        return heldItemOverlayScale;
    }

    public static void setHeldItemOverlayScale(float scale) {
        float clamped = MathHelper.clamp(scale, 0.35F, 2.5F);
        if (Math.abs(heldItemOverlayScale - clamped) < 0.0001F) return;
        heldItemOverlayScale = clamped;
        saveConfigNow();
    }

    public static boolean isRayVisualGlowEnabled() {
        return rayVisualGlowEnabled;
    }

    public static void setRayVisualGlowEnabled(boolean enabled) {
        if (rayVisualGlowEnabled == enabled) return;
        rayVisualGlowEnabled = enabled;
        saveConfigNow();
    }

    public static boolean isArmorVisualGlowEnabled() {
        return armorVisualGlowEnabled;
    }

    public static void setArmorVisualGlowEnabled(boolean enabled) {
        if (armorVisualGlowEnabled == enabled) return;
        armorVisualGlowEnabled = enabled;
        saveConfigNow();
    }

    public static boolean isHeldItemVisualGlowEnabled() {
        return heldItemVisualGlowEnabled;
    }

    public static void setHeldItemVisualGlowEnabled(boolean enabled) {
        if (heldItemVisualGlowEnabled == enabled) return;
        heldItemVisualGlowEnabled = enabled;
        saveConfigNow();
    }

    public static boolean isDistanceVisualGlowEnabled() {
        return distanceVisualGlowEnabled;
    }

    public static void setDistanceVisualGlowEnabled(boolean enabled) {
        if (distanceVisualGlowEnabled == enabled) return;
        distanceVisualGlowEnabled = enabled;
        saveConfigNow();
    }

    public static VisualColorMode getRayVisualColorMode() {
        return rayVisualColorMode;
    }

    public static void setRayVisualColorMode(VisualColorMode mode) {
        VisualColorMode updated = mode == null ? VisualColorMode.VIVID : mode;
        if (rayVisualColorMode == updated) return;
        rayVisualColorMode = updated;
        saveConfigNow();
    }

    public static VisualColorMode getArmorVisualColorMode() {
        return armorVisualColorMode;
    }

    public static void setArmorVisualColorMode(VisualColorMode mode) {
        VisualColorMode updated = mode == null ? VisualColorMode.VIVID : mode;
        if (armorVisualColorMode == updated) return;
        armorVisualColorMode = updated;
        saveConfigNow();
    }

    public static VisualColorMode getHeldItemVisualColorMode() {
        return heldItemVisualColorMode;
    }

    public static void setHeldItemVisualColorMode(VisualColorMode mode) {
        VisualColorMode updated = mode == null ? VisualColorMode.VIVID : mode;
        if (heldItemVisualColorMode == updated) return;
        heldItemVisualColorMode = updated;
        saveConfigNow();
    }

    public static VisualColorMode getDistanceVisualColorMode() {
        return distanceVisualColorMode;
    }

    public static void setDistanceVisualColorMode(VisualColorMode mode) {
        VisualColorMode updated = mode == null ? VisualColorMode.VIVID : mode;
        if (distanceVisualColorMode == updated) return;
        distanceVisualColorMode = updated;
        saveConfigNow();
    }

    public static float getRayVisualSaturationBoost() {
        return rayVisualSaturationBoost;
    }

    public static void setRayVisualSaturationBoost(float boost) {
        float clamped = MathHelper.clamp(boost, 1.0F, 2.5F);
        if (Math.abs(rayVisualSaturationBoost - clamped) < 0.0001F) return;
        rayVisualSaturationBoost = clamped;
        saveConfigNow();
    }

    public static float getArmorVisualSaturationBoost() {
        return armorVisualSaturationBoost;
    }

    public static void setArmorVisualSaturationBoost(float boost) {
        float clamped = MathHelper.clamp(boost, 1.0F, 2.5F);
        if (Math.abs(armorVisualSaturationBoost - clamped) < 0.0001F) return;
        armorVisualSaturationBoost = clamped;
        saveConfigNow();
    }

    public static float getHeldItemVisualSaturationBoost() {
        return heldItemVisualSaturationBoost;
    }

    public static void setHeldItemVisualSaturationBoost(float boost) {
        float clamped = MathHelper.clamp(boost, 1.0F, 2.5F);
        if (Math.abs(heldItemVisualSaturationBoost - clamped) < 0.0001F) return;
        heldItemVisualSaturationBoost = clamped;
        saveConfigNow();
    }

    public static float getDistanceVisualSaturationBoost() {
        return distanceVisualSaturationBoost;
    }

    public static void setDistanceVisualSaturationBoost(float boost) {
        float clamped = MathHelper.clamp(boost, 1.0F, 2.5F);
        if (Math.abs(distanceVisualSaturationBoost - clamped) < 0.0001F) return;
        distanceVisualSaturationBoost = clamped;
        saveConfigNow();
    }

    public static float getRayVisualAnimationSpeed() {
        return rayVisualAnimationSpeed;
    }

    public static void setRayVisualAnimationSpeed(float speed) {
        float clamped = MathHelper.clamp(speed, 0.2F, 4.0F);
        if (Math.abs(rayVisualAnimationSpeed - clamped) < 0.0001F) return;
        rayVisualAnimationSpeed = clamped;
        saveConfigNow();
    }

    public static float getArmorVisualAnimationSpeed() {
        return armorVisualAnimationSpeed;
    }

    public static void setArmorVisualAnimationSpeed(float speed) {
        float clamped = MathHelper.clamp(speed, 0.2F, 4.0F);
        if (Math.abs(armorVisualAnimationSpeed - clamped) < 0.0001F) return;
        armorVisualAnimationSpeed = clamped;
        saveConfigNow();
    }

    public static float getHeldItemVisualAnimationSpeed() {
        return heldItemVisualAnimationSpeed;
    }

    public static void setHeldItemVisualAnimationSpeed(float speed) {
        float clamped = MathHelper.clamp(speed, 0.2F, 4.0F);
        if (Math.abs(heldItemVisualAnimationSpeed - clamped) < 0.0001F) return;
        heldItemVisualAnimationSpeed = clamped;
        saveConfigNow();
    }

    public static float getDistanceVisualAnimationSpeed() {
        return distanceVisualAnimationSpeed;
    }

    public static void setDistanceVisualAnimationSpeed(float speed) {
        float clamped = MathHelper.clamp(speed, 0.2F, 4.0F);
        if (Math.abs(distanceVisualAnimationSpeed - clamped) < 0.0001F) return;
        distanceVisualAnimationSpeed = clamped;
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

    public static float getRayLabelTextScale() {
        return distanceTextScale;
    }

    public static void setRayLabelTextScale(float scale) {
        float clamped = MathHelper.clamp(scale, 0.5F, 2.0F);
        if (Math.abs(distanceTextScale - clamped) < 0.0001F) return;
        distanceTextScale = clamped;
        saveConfigNow();
    }

    public static float getDistanceTextScale() {
        return getRayLabelTextScale();
    }

    public static void setDistanceTextScale(float scale) {
        setRayLabelTextScale(scale);
    }

    public static float getTargetHealthTextScale() {
        return targetHealthTextScale;
    }

    public static void setTargetHealthTextScale(float scale) {
        float clamped = MathHelper.clamp(scale, 0.5F, 2.0F);
        if (Math.abs(targetHealthTextScale - clamped) < 0.0001F) return;
        targetHealthTextScale = clamped;
        saveConfigNow();
    }

    public static float getPlayerListTextScale() {
        return playerListTextScale;
    }

    public static void setPlayerListTextScale(float scale) {
        float clamped = MathHelper.clamp(scale, 0.1F, 2.0F);
        if (Math.abs(playerListTextScale - clamped) < 0.0001F) return;
        playerListTextScale = clamped;
        saveConfigNow();
    }

    public static int getPlayerListMaxHeight() {
        return playerListMaxHeight;
    }

    public static void setPlayerListMaxHeight(int maxHeight) {
        int clamped = MathHelper.clamp(maxHeight, 40, MAX_PLAYER_LIST_OFFSET);
        if (playerListMaxHeight == clamped) return;
        playerListMaxHeight = clamped;
        saveConfigNow();
    }

    public static float getPlayerListAlphaMultiplier() {
        return playerListAlphaMultiplier;
    }

    public static void setPlayerListAlphaMultiplier(float alpha) {
        float clamped = MathHelper.clamp(alpha, 0.1F, 1.0F);
        if (Math.abs(playerListAlphaMultiplier - clamped) < 0.0001F) return;
        playerListAlphaMultiplier = clamped;
        saveConfigNow();
    }

    public static int getPlayerListOffsetX() {
        return playerListOffsetX;
    }

    public static void setPlayerListOffsetX(int offsetX) {
        int clamped = MathHelper.clamp(offsetX, 0, MAX_PLAYER_LIST_OFFSET);
        if (playerListOffsetX == clamped) return;
        playerListOffsetX = clamped;
        saveConfigNow();
    }

    public static int getPlayerListOffsetY() {
        return playerListOffsetY;
    }

    public static void setPlayerListOffsetY(int offsetY) {
        int clamped = MathHelper.clamp(offsetY, 0, MAX_PLAYER_LIST_OFFSET);
        if (playerListOffsetY == clamped) return;
        playerListOffsetY = clamped;
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
        if (player == null) {
            return 0xFFFFFF;
        }

        return resolveVisualColor(
                player,
                0.0F,
                rayVisualColorMode,
                rayVisualSaturationBoost,
                rayVisualAnimationSpeed
        );
    }

    public static int getPlayerBaseColor(PlayerEntity player) {
        if (player == null) {
            return 0xFFFFFF;
        }

        TextColor textColor = player.getDisplayName().getStyle().getColor();
        return textColor != null ? textColor.getRgb() : player.getTeamColorValue();
    }

    @Override
    public void onInitializeClient() {
        NoKnockbackConfig.Data loadedConfig = NoKnockbackConfig.load();
        applyLoadedConfig(loadedConfig);

        toggleKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.paprika.toggle",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_V,
                "category.paprika"
        ));

        togglePlayerEspKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.paprika.player_esp",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_H,
                "category.paprika"
        ));

        togglePlayerRaysKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.paprika.player_rays",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_J,
                "category.paprika"
        ));

        togglePlayerListKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.paprika.player_list",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_K,
                "category.paprika"
        ));

        openMenuKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.paprika.menu",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_RIGHT_SHIFT,
                "category.paprika"
        ));

        applyConfiguredKey(toggleKey, loadedConfig.speedToggleKey);
        applyConfiguredKey(togglePlayerEspKey, loadedConfig.playerEspKey);
        applyConfiguredKey(togglePlayerRaysKey, loadedConfig.playerRaysKey);
        applyConfiguredKey(togglePlayerListKey, loadedConfig.playerListKey);
        applyConfiguredKey(openMenuKey, loadedConfig.menuKey);
        KeyBinding.updateKeysByCode();
        saveConfigNow();

        HudLayerRegistrationCallback.EVENT.register(layeredDrawer -> layeredDrawer.attachLayerAfter(
                IdentifiedLayer.SUBTITLES,
                HUD_OVERLAY_LAYER_ID,
                NoKnockbackClient::renderHudOverlay
        ));

        ClientTickEvents.END_CLIENT_TICK.register(this::onTick);
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
                    Text.literal("[Paprika] Speed: " + (speedEnabled ? "ON" : "OFF")),
                    true
            );
        }

        while (togglePlayerEspKey.wasPressed()) {
            setPlayerEspEnabled(!playerEspEnabled);
            player.sendMessage(
                    Text.literal("[Paprika] Player ESP: " + (playerEspEnabled ? "ON" : "OFF")),
                    true
            );
        }

        while (togglePlayerRaysKey.wasPressed()) {
            setPlayerRaysEnabled(!playerRaysEnabled);
            player.sendMessage(
                    Text.literal("[Paprika] Player Rays: " + (playerRaysEnabled ? "ON" : "OFF")),
                    true
            );
        }

        while (togglePlayerListKey.wasPressed()) {
            setPlayerListEnabled(!playerListEnabled);
            player.sendMessage(
                    Text.literal("[Paprika] Player List: " + (playerListEnabled ? "ON" : "OFF")),
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

    public static void renderHudOverlay(DrawContext drawContext, RenderTickCounter tickCounter) {
        MinecraftClient client = MinecraftClient.getInstance();
        PlayerEntity localPlayer = client.player;
        if (localPlayer == null || client.world == null) return;
        if (playerListEnabled) {
            renderPlayerList(drawContext, client, localPlayer);
        }
        if (client.gameRenderer == null) return;

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
        float renderFov = fov;
        float rayStartX = screenWidth * 0.5F;
        float rayStartY = rayOrigin == RayOrigin.CENTER
                ? screenHeight * 0.5F
                : MathHelper.clamp(screenHeight - 1.0F - rayBottomStartHeight, 1.0F, screenHeight - 1.0F);

        if (playerArmorOverlayEnabled || heldItemOverlayEnabled || (distanceDisplayEnabled && distanceAnchorMode == OverlayAnchorMode.ABOVE_PLAYER)) {
            renderPlayerArmorOverlay(
                    drawContext,
                    client,
                    localPlayer,
                    camera,
                    tickDelta,
                    renderFov,
                    screenWidth,
                    screenHeight,
                    rayStartX,
                    rayStartY
            );
        }
        if (targetHealthOverlayEnabled) {
            renderTargetHealthOverlay(drawContext, client, localPlayer, camera, tickDelta, renderFov, screenWidth, screenHeight);
        }
        if (!playerRaysEnabled) return;

        Vector3f projected = new Vector3f();
        List<RayDistanceLabel> labels = new ArrayList<>();

        drawContext.draw(vertexConsumers -> {
            VertexConsumer lineConsumer = vertexConsumers.getBuffer(RenderLayer.getDebugQuads());
            Matrix4f matrix = drawContext.getMatrices().peek().getPositionMatrix();

            for (PlayerEntity target : client.world.getPlayers()) {
                if (target == localPlayer || target.isRemoved()) continue;

                Vec3d targetPos = target.getLerpedPos(tickDelta).add(0.0, target.getHeight() * 0.5, 0.0);
                if (!projectToIndicator(targetPos, camera, screenWidth, screenHeight, renderFov, projected)) continue;

                int startColor = 0xFF000000 | resolveVisualColor(
                        target,
                        0.08F,
                        rayVisualColorMode,
                        rayVisualSaturationBoost,
                        rayVisualAnimationSpeed
                );
                int endColor = 0xFF000000 | resolveVisualColor(
                        target,
                        0.42F,
                        rayVisualColorMode,
                        rayVisualSaturationBoost,
                        rayVisualAnimationSpeed
                );
                if (rayVisualGlowEnabled) {
                    drawThickRay(
                            matrix,
                            lineConsumer,
                            rayStartX,
                            rayStartY,
                            projected.x,
                            projected.y,
                            rayThickness * 2.6F,
                            withAlpha(startColor, 0.38F),
                            withAlpha(endColor, 0.38F)
                    );
                }
                drawThickRay(matrix, lineConsumer, rayStartX, rayStartY, projected.x, projected.y, rayThickness, startColor, endColor);

                if (distanceDisplayEnabled && distanceAnchorMode == OverlayAnchorMode.RAY_MIDDLE) {
                    int meters = (int) Math.round(localPlayer.getPos().distanceTo(target.getPos()));
                    labels.add(createRayDistanceLabel(
                            Integer.toString(Math.max(0, meters)) + "m",
                            rayStartX,
                            rayStartY,
                            projected.x,
                            projected.y,
                            0xFF000000 | resolveVisualColor(
                                    target,
                                    0.18F,
                                    distanceVisualColorMode,
                                    distanceVisualSaturationBoost,
                                    distanceVisualAnimationSpeed
                            )
                    ));
                }
            }
        });

        renderRayDistanceLabels(drawContext, client, labels, screenWidth, screenHeight);
    }

    private static void drawThickRay(
            Matrix4f matrix,
            VertexConsumer consumer,
            float x1,
            float y1,
            float x2,
            float y2,
            float thickness,
            int startColor,
            int endColor
    ) {
        float dx = x2 - x1;
        float dy = y2 - y1;
        float len = MathHelper.sqrt(dx * dx + dy * dy);
        if (len < 0.0001F) return;

        float half = Math.max(0.5F, thickness) * 0.5F;
        float nx = -dy / len * half;
        float ny = dx / len * half;

        consumer.vertex(matrix, x1 - nx, y1 - ny, 0.0F).color(startColor);
        consumer.vertex(matrix, x1 + nx, y1 + ny, 0.0F).color(startColor);
        consumer.vertex(matrix, x2 + nx, y2 + ny, 0.0F).color(endColor);
        consumer.vertex(matrix, x2 - nx, y2 - ny, 0.0F).color(endColor);
    }

    private static RayDistanceLabel createRayDistanceLabel(
            String text,
            float startX,
            float startY,
            float endX,
            float endY,
            int color
    ) {
        float dx = endX - startX;
        float dy = endY - startY;
        float len = MathHelper.sqrt(dx * dx + dy * dy);

        float labelX = startX + dx * RAY_LABEL_POSITION_FACTOR;
        float labelY = startY + dy * RAY_LABEL_POSITION_FACTOR;
        if (len > 0.0001F) {
            float nx = -dy / len;
            float ny = dx / len;
            labelX += nx * RAY_LABEL_PERP_OFFSET;
            labelY += ny * RAY_LABEL_PERP_OFFSET;
        }

        return new RayDistanceLabel(text, labelX, labelY, color);
    }

    private static void renderRayDistanceLabels(
            DrawContext drawContext,
            MinecraftClient client,
            List<RayDistanceLabel> labels,
            int screenWidth,
            int screenHeight
    ) {
        if (labels.isEmpty() || client.textRenderer == null) return;

        int margin = 2;
        int fontHeight = client.textRenderer.fontHeight;
        for (RayDistanceLabel label : labels) {
            int rawWidth = client.textRenderer.getWidth(label.text());
            int scaledWidth = Math.max(1, Math.round(rawWidth * distanceTextScale));
            int scaledHeight = Math.max(1, Math.round(fontHeight * distanceTextScale));

            int x = Math.round(label.x()) - scaledWidth / 2;
            int y = Math.round(label.y()) - scaledHeight / 2;
            x = MathHelper.clamp(x, margin, Math.max(margin, screenWidth - scaledWidth - margin));
            y = MathHelper.clamp(y, margin, Math.max(margin, screenHeight - scaledHeight - margin));

            if (distanceVisualGlowEnabled) {
                drawContext.fill(
                        x - 4,
                        y - 3,
                        x + scaledWidth + 4,
                        y + scaledHeight + 3,
                        withAlpha(label.color(), 0.35F)
                );
            }
            drawContext.fill(
                    x - 2,
                    y - 1,
                    x + scaledWidth + 2,
                    y + scaledHeight + 1,
                    RAY_LABEL_BG_COLOR
            );

            drawContext.getMatrices().push();
            drawContext.getMatrices().translate(x, y, 0.0F);
            drawContext.getMatrices().scale(distanceTextScale, distanceTextScale, 1.0F);
            int textColor = withAlpha(label.color(), 0.95F);
            drawContext.drawTextWithShadow(client.textRenderer, label.text(), 0, 0, textColor);
            drawContext.getMatrices().pop();
        }
    }

    private static void renderPlayerArmorOverlay(
            DrawContext drawContext,
            MinecraftClient client,
            PlayerEntity localPlayer,
            Camera camera,
            float tickDelta,
            float fovDegrees,
            int screenWidth,
            int screenHeight,
            float rayStartX,
            float rayStartY
    ) {
        if (client.world == null) return;

        Vector3f centerProjected = new Vector3f();
        Vector3f aboveProjected = new Vector3f();
        Vector3f anchor = new Vector3f();
        for (PlayerEntity target : client.world.getPlayers()) {
            if (target == localPlayer || target.isRemoved()) continue;

            Vec3d centerPos = target.getLerpedPos(tickDelta).add(0.0, target.getHeight() * 0.5, 0.0);
            if (!projectToIndicator(centerPos, camera, screenWidth, screenHeight, fovDegrees, centerProjected)) continue;

            Vec3d abovePos = target.getLerpedPos(tickDelta).add(0.0, target.getHeight() + 0.35, 0.0);
            if (!projectToIndicator(abovePos, camera, screenWidth, screenHeight, fovDegrees, aboveProjected)) {
                aboveProjected.set(centerProjected);
            }

            if (playerArmorOverlayEnabled) {
                List<ItemStack> armorStacks = new ArrayList<>(4);
                ItemStack head = target.getEquippedStack(EquipmentSlot.HEAD);
                ItemStack chest = target.getEquippedStack(EquipmentSlot.CHEST);
                ItemStack legs = target.getEquippedStack(EquipmentSlot.LEGS);
                ItemStack feet = target.getEquippedStack(EquipmentSlot.FEET);
                if (!head.isEmpty()) armorStacks.add(head);
                if (!chest.isEmpty()) armorStacks.add(chest);
                if (!legs.isEmpty()) armorStacks.add(legs);
                if (!feet.isEmpty()) armorStacks.add(feet);

                if (!armorStacks.isEmpty()) {
                    resolveOverlayAnchor(armorAnchorMode, rayStartX, rayStartY, centerProjected, aboveProjected, anchor);
                    int accentColor = 0xFF000000 | resolveVisualColor(
                            target,
                            0.24F,
                            armorVisualColorMode,
                            armorVisualSaturationBoost,
                            armorVisualAnimationSpeed
                    );
                    renderItemRow(
                            drawContext,
                            armorStacks,
                            armorOverlayScale,
                            anchor,
                            screenWidth,
                            screenHeight,
                            accentColor,
                            armorVisualGlowEnabled,
                            -4,
                            true
                    );
                }
            }

            if (heldItemOverlayEnabled) {
                List<ItemStack> heldStacks = new ArrayList<>(2);
                ItemStack mainHand = target.getMainHandStack();
                ItemStack offHand = target.getOffHandStack();
                if (!mainHand.isEmpty()) heldStacks.add(mainHand);
                if (!offHand.isEmpty()) heldStacks.add(offHand);

                if (!heldStacks.isEmpty()) {
                    resolveOverlayAnchor(heldItemAnchorMode, rayStartX, rayStartY, centerProjected, aboveProjected, anchor);
                    int accentColor = 0xFF000000 | resolveVisualColor(
                            target,
                            0.34F,
                            heldItemVisualColorMode,
                            heldItemVisualSaturationBoost,
                            heldItemVisualAnimationSpeed
                    );
                    renderItemRow(
                            drawContext,
                            heldStacks,
                            heldItemOverlayScale,
                            anchor,
                            screenWidth,
                            screenHeight,
                            accentColor,
                            heldItemVisualGlowEnabled,
                            4,
                            false
                    );
                }
            }

            if (distanceDisplayEnabled && distanceAnchorMode == OverlayAnchorMode.ABOVE_PLAYER && client.textRenderer != null) {
                resolveOverlayAnchor(distanceAnchorMode, rayStartX, rayStartY, centerProjected, aboveProjected, anchor);
                int meters = Math.max(0, Math.round((float) localPlayer.getPos().distanceTo(target.getPos())));
                String distanceText = meters + "m";
                int accentColor = 0xFF000000 | resolveVisualColor(
                        target,
                        0.18F,
                        distanceVisualColorMode,
                        distanceVisualSaturationBoost,
                        distanceVisualAnimationSpeed
                );
                int textWidth = Math.max(1, Math.round(client.textRenderer.getWidth(distanceText) * distanceTextScale));
                int textHeight = Math.max(1, Math.round(client.textRenderer.fontHeight * distanceTextScale));
                int textX = MathHelper.clamp(
                        Math.round(anchor.x) - textWidth / 2,
                        2,
                        Math.max(2, screenWidth - textWidth - 2)
                );
                int textY = MathHelper.clamp(
                        Math.round(anchor.y) - textHeight - 8,
                        2,
                        Math.max(2, screenHeight - textHeight - 2)
                );

                if (distanceVisualGlowEnabled) {
                    drawContext.fill(textX - 3, textY - 2, textX + textWidth + 3, textY + textHeight + 2, withAlpha(accentColor, 0.32F));
                }
                drawContext.fill(textX - 2, textY - 1, textX + textWidth + 2, textY + textHeight + 1, 0x65000000);
                drawContext.getMatrices().push();
                drawContext.getMatrices().translate(textX, textY, 0.0F);
                drawContext.getMatrices().scale(distanceTextScale, distanceTextScale, 1.0F);
                drawContext.drawTextWithShadow(client.textRenderer, distanceText, 0, 0, withAlpha(accentColor, 0.95F));
                drawContext.getMatrices().pop();
            } else if (distanceDisplayEnabled && distanceAnchorMode == OverlayAnchorMode.RAY_MIDDLE && !playerRaysEnabled && client.textRenderer != null) {
                resolveOverlayAnchor(distanceAnchorMode, rayStartX, rayStartY, centerProjected, aboveProjected, anchor);
                int meters = Math.max(0, Math.round((float) localPlayer.getPos().distanceTo(target.getPos())));
                String distanceText = meters + "m";
                int accentColor = 0xFF000000 | resolveVisualColor(
                        target,
                        0.18F,
                        distanceVisualColorMode,
                        distanceVisualSaturationBoost,
                        distanceVisualAnimationSpeed
                );
                int textWidth = Math.max(1, Math.round(client.textRenderer.getWidth(distanceText) * distanceTextScale));
                int textHeight = Math.max(1, Math.round(client.textRenderer.fontHeight * distanceTextScale));
                int textX = MathHelper.clamp(Math.round(anchor.x) - textWidth / 2, 2, Math.max(2, screenWidth - textWidth - 2));
                int textY = MathHelper.clamp(Math.round(anchor.y) - textHeight / 2, 2, Math.max(2, screenHeight - textHeight - 2));

                if (distanceVisualGlowEnabled) {
                    drawContext.fill(textX - 3, textY - 2, textX + textWidth + 3, textY + textHeight + 2, withAlpha(accentColor, 0.32F));
                }
                drawContext.fill(textX - 2, textY - 1, textX + textWidth + 2, textY + textHeight + 1, 0x65000000);
                drawContext.getMatrices().push();
                drawContext.getMatrices().translate(textX, textY, 0.0F);
                drawContext.getMatrices().scale(distanceTextScale, distanceTextScale, 1.0F);
                drawContext.drawTextWithShadow(client.textRenderer, distanceText, 0, 0, withAlpha(accentColor, 0.95F));
                drawContext.getMatrices().pop();
            }
        }
    }

    private static void resolveOverlayAnchor(
            OverlayAnchorMode mode,
            float rayStartX,
            float rayStartY,
            Vector3f centerProjected,
            Vector3f aboveProjected,
            Vector3f out
    ) {
        if (mode == OverlayAnchorMode.ABOVE_PLAYER) {
            out.set(aboveProjected);
            return;
        }

        out.set(
                rayStartX + (centerProjected.x - rayStartX) * RAY_LABEL_POSITION_FACTOR,
                rayStartY + (centerProjected.y - rayStartY) * RAY_LABEL_POSITION_FACTOR,
                0.0F
        );
    }

    private static void renderItemRow(
            DrawContext drawContext,
            List<ItemStack> stacks,
            float itemScale,
            Vector3f anchor,
            int screenWidth,
            int screenHeight,
            int accentColor,
            boolean glowEnabled,
            int yOffset,
            boolean aboveAnchor
    ) {
        if (stacks.isEmpty()) return;

        int iconSize = Math.max(6, Math.round(16.0F * MathHelper.clamp(itemScale, 0.35F, 2.5F)));
        int count = stacks.size();
        int totalWidth = count * iconSize + (count - 1) * ARMOR_OVERLAY_ICON_SPACING;
        int startX = Math.round(anchor.x) - totalWidth / 2;
        int startY = aboveAnchor ? Math.round(anchor.y) - iconSize + yOffset : Math.round(anchor.y) + yOffset;
        startX = MathHelper.clamp(startX, 1, Math.max(1, screenWidth - totalWidth - 1));
        startY = MathHelper.clamp(startY, 1, Math.max(1, screenHeight - iconSize - 1));

        if (glowEnabled) {
            drawContext.fill(startX - 3, startY - 3, startX + totalWidth + 3, startY + iconSize + 3, withAlpha(accentColor, 0.35F));
        }
        drawContext.fill(startX - 1, startY - 1, startX + totalWidth + 1, startY + iconSize + 1, 0x5A000000);

        for (int i = 0; i < count; i++) {
            int iconX = startX + i * (iconSize + ARMOR_OVERLAY_ICON_SPACING);
            int iconY = startY;
            drawContext.getMatrices().push();
            drawContext.getMatrices().translate(iconX, iconY, 0.0F);
            drawContext.getMatrices().scale(itemScale, itemScale, 1.0F);
            drawContext.drawItem(stacks.get(i), 0, 0);
            drawContext.getMatrices().pop();
        }
    }

    private static void renderTargetHealthOverlay(
            DrawContext drawContext,
            MinecraftClient client,
            PlayerEntity localPlayer,
            Camera camera,
            float tickDelta,
            float fovDegrees,
            int screenWidth,
            int screenHeight
    ) {
        if (client.world == null || client.textRenderer == null) return;

        Vector3f projected = new Vector3f();
        for (PlayerEntity target : client.world.getPlayers()) {
            if (target == localPlayer || target.isRemoved()) continue;

            Vec3d textWorldPos = target.getLerpedPos(tickDelta).add(0.0, target.getHeight() + 0.2, 0.0);
            if (!projectToScreen(textWorldPos, camera, screenWidth, screenHeight, fovDegrees, projected)) continue;

            int currentHp = Math.max(0, Math.round(target.getHealth()));
            int maxHp = Math.max(1, Math.round(target.getMaxHealth()));
            String text = currentHp + "/" + maxHp;
            float hpPercent = (currentHp / (float) maxHp) * 100.0F;
            int color = targetHealthDynamicColorEnabled ? resolveTargetHealthColor(hpPercent) : 0xFFFFFFFF;

            int rawWidth = client.textRenderer.getWidth(text);
            int scaledWidth = Math.max(1, Math.round(rawWidth * targetHealthTextScale));
            int scaledHeight = Math.max(1, Math.round(client.textRenderer.fontHeight * targetHealthTextScale));

            int textX = Math.round(projected.x) - scaledWidth / 2;
            int textY = Math.round(projected.y) - scaledHeight - 2;
            textX = MathHelper.clamp(textX, 2, Math.max(2, screenWidth - scaledWidth - 2));
            textY = MathHelper.clamp(textY, 2, Math.max(2, screenHeight - scaledHeight - 2));

            if (rayVisualGlowEnabled) {
                int accentColor = 0xFF000000 | resolveVisualColor(target, 0.56F);
                drawContext.fill(textX - 4, textY - 3, textX + scaledWidth + 4, textY + scaledHeight + 3, withAlpha(accentColor, 0.33F));
            }
            drawContext.fill(textX - 2, textY - 1, textX + scaledWidth + 2, textY + scaledHeight + 1, TARGET_HEALTH_BG_COLOR);
            drawContext.getMatrices().push();
            drawContext.getMatrices().translate(textX, textY, 0.0F);
            drawContext.getMatrices().scale(targetHealthTextScale, targetHealthTextScale, 1.0F);
            drawContext.drawTextWithShadow(client.textRenderer, text, 0, 0, color);
            drawContext.getMatrices().pop();
        }
    }

    private static int resolveTargetHealthColor(float hpPercent) {
        if (hpPercent < 10.0F) {
            return TARGET_HEALTH_COLOR_DARK_RED;
        }
        if (hpPercent < 33.0F) {
            return TARGET_HEALTH_COLOR_RED;
        }
        if (hpPercent <= 66.0F) {
            return TARGET_HEALTH_COLOR_YELLOW;
        }
        return TARGET_HEALTH_COLOR_LIME;
    }

    private static void renderPlayerList(DrawContext drawContext, MinecraftClient client, PlayerEntity localPlayer) {
        if (client.textRenderer == null || client.world == null) return;

        Map<Integer, List<PlayerDistanceEntry>> groups = new LinkedHashMap<>();
        for (PlayerEntity target : client.world.getPlayers()) {
            if (target == localPlayer || target.isRemoved()) continue;

            int color = getPlayerGroupingColor(target) & 0xFFFFFF;
            double distance = localPlayer.getPos().distanceTo(target.getPos());
            String name = target.getName().getString();
            int displayColor = 0xFF000000 | resolveVisualColor(target, 0.18F);
            groups.computeIfAbsent(color, ignored -> new ArrayList<>()).add(new PlayerDistanceEntry(name, distance, displayColor));
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
                String lineText = entry.name() + " - " + (int) Math.round(entry.distance()) + " m";
                lines.add(new PlayerListLine(
                        lineText,
                        entry.displayColor()
                ));
            }
        }

        int screenWidth = drawContext.getScaledWindowWidth();
        int screenHeight = drawContext.getScaledWindowHeight();
        int textLineHeight = Math.max(4, Math.round(client.textRenderer.fontHeight * playerListTextScale) + 1);
        int baseY = MathHelper.clamp(playerListOffsetY, 0, Math.max(0, screenHeight - 12));
        int viewportHeight = Math.max(24, Math.min(playerListMaxHeight, screenHeight - baseY - 8));
        int maxVisibleLines = Math.max(2, viewportHeight / textLineHeight);
        if (lines.size() > maxVisibleLines) {
            lines = new ArrayList<>(lines.subList(0, maxVisibleLines - 1));
            lines.add(new PlayerListLine("and ... more", 0xFFFFFFFF));
        }

        int maxWidth = 0;
        for (PlayerListLine line : lines) {
            int scaledWidth = Math.round(client.textRenderer.getWidth(line.text()) * playerListTextScale);
            maxWidth = Math.max(maxWidth, scaledWidth);
        }

        int panelWidth = maxWidth + PLAYER_LIST_PADDING * 2;
        int panelHeight = lines.size() * textLineHeight + PLAYER_LIST_PADDING * 2;
        int x1 = MathHelper.clamp(playerListOffsetX, 0, Math.max(0, screenWidth - 12));
        int y1 = baseY;
        x1 = MathHelper.clamp(x1, 0, Math.max(0, screenWidth - panelWidth - 1));
        y1 = MathHelper.clamp(y1, 0, Math.max(0, screenHeight - panelHeight - 1));
        int x2 = x1 + panelWidth;
        int y2 = y1 + panelHeight;

        if (rayVisualGlowEnabled && !lines.isEmpty()) {
            int glowColor = withAlpha(lines.get(0).color(), 0.32F * playerListAlphaMultiplier);
            drawContext.fill(x1 - 4, y1 - 4, x2 + 4, y2 + 4, glowColor);
        }
        drawContext.fill(x1 - 1, y1 - 1, x2 + 1, y2 + 1, withAlpha(PLAYER_LIST_BORDER_COLOR, playerListAlphaMultiplier));
        drawContext.fill(x1, y1, x2, y2, withAlpha(PLAYER_LIST_BG_COLOR, playerListAlphaMultiplier));

        int textX = x1 + PLAYER_LIST_PADDING;
        int textY = y1 + PLAYER_LIST_PADDING;
        for (PlayerListLine line : lines) {
            int textColor = withAlpha(line.color(), playerListAlphaMultiplier);
            if (rayVisualGlowEnabled) {
                drawContext.fill(
                        textX - 2,
                        textY - 1,
                        textX + Math.max(2, Math.round(client.textRenderer.getWidth(line.text()) * playerListTextScale)) + 2,
                        textY + textLineHeight - 1,
                        withAlpha(line.color(), 0.18F * playerListAlphaMultiplier)
                );
            }
            drawContext.getMatrices().push();
            drawContext.getMatrices().translate(textX, textY, 0.0F);
            drawContext.getMatrices().scale(playerListTextScale, playerListTextScale, 1.0F);
            drawContext.drawTextWithShadow(client.textRenderer, line.text(), 0, 0, textColor);
            drawContext.getMatrices().pop();
            textY += textLineHeight;
        }
    }

    private static boolean projectToScreen(
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

        float forwardZ = -cameraSpace.z;
        if (forwardZ <= 0.05F) return false;

        float ndcX = (cameraSpace.x / forwardZ) / tanHalfFovX;
        float ndcY = (cameraSpace.y / forwardZ) / tanHalfFovY;
        if (Math.abs(ndcX) > 1.0F || Math.abs(ndcY) > 1.0F) return false;

        out.set(
                (ndcX * 0.5F + 0.5F) * screenWidth,
                (0.5F - ndcY * 0.5F) * screenHeight,
                0.0F
        );
        return true;
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

    private static int getPlayerGroupingColor(PlayerEntity player) {
        int baseColor = getPlayerBaseColor(player);
        if (rayVisualColorMode == VisualColorMode.NICK) {
            return baseColor;
        }

        return toVividColor(baseColor, rayVisualSaturationBoost);
    }

    private static int resolveVisualColor(PlayerEntity player, float offset) {
        return resolveVisualColor(
                getPlayerBaseColor(player),
                player.getId(),
                offset,
                rayVisualColorMode,
                rayVisualSaturationBoost,
                rayVisualAnimationSpeed
        );
    }

    private static int resolveVisualColor(
            PlayerEntity player,
            float offset,
            VisualColorMode colorMode,
            float saturationBoost,
            float animationSpeed
    ) {
        return resolveVisualColor(getPlayerBaseColor(player), player.getId(), offset, colorMode, saturationBoost, animationSpeed);
    }

    private static int resolveVisualColor(
            int baseColor,
            int seed,
            float offset,
            VisualColorMode colorMode,
            float saturationBoost,
            float animationSpeed
    ) {
        int rgbBase = baseColor & 0x00FFFFFF;
        return switch (colorMode) {
            case NICK -> rgbBase;
            case VIVID -> toVividColor(rgbBase, saturationBoost);
            case GRADIENT -> toGradientColor(rgbBase, seed, offset, saturationBoost, animationSpeed);
            case RAINBOW -> toRainbowColor(seed, offset, animationSpeed);
        };
    }

    private static int toVividColor(int rgbColor, float saturationBoost) {
        float[] hsv = rgbToHsv(rgbColor);
        float saturation = MathHelper.clamp(Math.max(hsv[1], 0.45F) * saturationBoost, 0.0F, 1.0F);
        float value = MathHelper.clamp(Math.max(hsv[2], 0.72F) * 1.08F, 0.0F, 1.0F);
        return MathHelper.hsvToRgb(hsv[0], saturation, value);
    }

    private static int toGradientColor(int rgbColor, int seed, float offset, float saturationBoost, float animationSpeed) {
        float[] hsv = rgbToHsv(rgbColor);
        float phase = wrapUnit(visualTime(animationSpeed) * 0.18F + seed * 0.037F + offset);
        float wave = 0.5F + 0.5F * MathHelper.sin(phase * TWO_PI);
        float hueShift = MathHelper.lerp(wave, -0.14F, 0.14F);
        float hue = wrapUnit(hsv[0] + hueShift);
        float saturation = MathHelper.clamp(Math.max(hsv[1], 0.55F) * saturationBoost, 0.0F, 1.0F);
        float value = MathHelper.clamp(Math.max(hsv[2], 0.8F) * 1.1F, 0.0F, 1.0F);
        return MathHelper.hsvToRgb(hue, saturation, value);
    }

    private static int toRainbowColor(int seed, float offset, float animationSpeed) {
        float hue = wrapUnit(visualTime(animationSpeed) * 0.22F + seed * 0.041F + offset);
        return MathHelper.hsvToRgb(hue, 0.92F, 1.0F);
    }

    private static float visualTime(float animationSpeed) {
        return (System.currentTimeMillis() / 1000.0F) * animationSpeed;
    }

    private static float wrapUnit(float value) {
        float wrapped = value % 1.0F;
        return wrapped < 0.0F ? wrapped + 1.0F : wrapped;
    }

    private static float[] rgbToHsv(int rgbColor) {
        float r = ((rgbColor >>> 16) & 0xFF) / 255.0F;
        float g = ((rgbColor >>> 8) & 0xFF) / 255.0F;
        float b = (rgbColor & 0xFF) / 255.0F;

        float max = Math.max(r, Math.max(g, b));
        float min = Math.min(r, Math.min(g, b));
        float delta = max - min;

        float hue;
        if (delta < 0.00001F) {
            hue = 0.0F;
        } else if (max == r) {
            hue = ((g - b) / delta) % 6.0F;
        } else if (max == g) {
            hue = ((b - r) / delta) + 2.0F;
        } else {
            hue = ((r - g) / delta) + 4.0F;
        }

        hue /= 6.0F;
        if (hue < 0.0F) {
            hue += 1.0F;
        }

        float saturation = max <= 0.0F ? 0.0F : (delta / max);
        return new float[]{hue, saturation, max};
    }

    private static void applyLoadedConfig(NoKnockbackConfig.Data config) {
        if (config == null) return;

        speedEnabled = config.speedEnabled;
        playerEspEnabled = config.playerEspEnabled;
        playerArmorOverlayEnabled = config.playerArmorOverlayEnabled;
        playerRaysEnabled = config.playerRaysEnabled;
        playerListEnabled = config.playerListEnabled;
        targetHealthOverlayEnabled = config.targetHealthOverlayEnabled;
        targetHealthDynamicColorEnabled = config.targetHealthDynamicColorEnabled;
        distanceDisplayEnabled = config.distanceDisplayEnabled;
        heldItemOverlayEnabled = config.heldItemOverlayEnabled;
        rayVisualGlowEnabled = config.rayVisualGlowEnabled;
        armorVisualGlowEnabled = config.armorVisualGlowEnabled;
        heldItemVisualGlowEnabled = config.heldItemVisualGlowEnabled;
        distanceVisualGlowEnabled = config.distanceVisualGlowEnabled;
        rayThickness = MathHelper.clamp(config.rayThickness, 0.5F, 8.0F);
        outlineThickness = MathHelper.clamp(config.outlineThickness, 0.5F, 6.0F);
        rayBottomStartHeight = MathHelper.clamp(config.rayBottomStartHeight, 0.0F, MAX_BOTTOM_RAY_START_HEIGHT);
        distanceTextScale = MathHelper.clamp(config.rayDistanceTextScale, 0.5F, 2.0F);
        armorOverlayScale = MathHelper.clamp(config.armorOverlayScale, 0.35F, 2.5F);
        heldItemOverlayScale = MathHelper.clamp(config.heldItemOverlayScale, 0.35F, 2.5F);
        targetHealthTextScale = MathHelper.clamp(config.targetHealthTextScale, 0.5F, 2.0F);
        playerListTextScale = MathHelper.clamp(config.playerListTextScale, 0.1F, 2.0F);
        playerListMaxHeight = MathHelper.clamp(config.playerListMaxHeight, 40, MAX_PLAYER_LIST_OFFSET);
        playerListAlphaMultiplier = MathHelper.clamp(config.playerListAlpha, 0.1F, 1.0F);
        rayVisualSaturationBoost = MathHelper.clamp(config.rayVisualSaturationBoost, 1.0F, 2.5F);
        rayVisualAnimationSpeed = MathHelper.clamp(config.rayVisualAnimationSpeed, 0.2F, 4.0F);
        armorVisualSaturationBoost = MathHelper.clamp(config.armorVisualSaturationBoost, 1.0F, 2.5F);
        armorVisualAnimationSpeed = MathHelper.clamp(config.armorVisualAnimationSpeed, 0.2F, 4.0F);
        heldItemVisualSaturationBoost = MathHelper.clamp(config.heldItemVisualSaturationBoost, 1.0F, 2.5F);
        heldItemVisualAnimationSpeed = MathHelper.clamp(config.heldItemVisualAnimationSpeed, 0.2F, 4.0F);
        distanceVisualSaturationBoost = MathHelper.clamp(config.distanceVisualSaturationBoost, 1.0F, 2.5F);
        distanceVisualAnimationSpeed = MathHelper.clamp(config.distanceVisualAnimationSpeed, 0.2F, 4.0F);
        playerListOffsetX = MathHelper.clamp(config.playerListOffsetX, 0, MAX_PLAYER_LIST_OFFSET);
        playerListOffsetY = MathHelper.clamp(config.playerListOffsetY, 0, MAX_PLAYER_LIST_OFFSET);
        rayOrigin = parseRayOrigin(config.rayOrigin);
        armorAnchorMode = parseOverlayAnchorMode(config.armorAnchorMode, OverlayAnchorMode.ABOVE_PLAYER);
        heldItemAnchorMode = parseOverlayAnchorMode(config.heldItemAnchorMode, OverlayAnchorMode.ABOVE_PLAYER);
        distanceAnchorMode = parseOverlayAnchorMode(config.distanceAnchorMode, OverlayAnchorMode.RAY_MIDDLE);
        rayVisualColorMode = parseVisualColorMode(config.rayVisualColorMode, VisualColorMode.VIVID);
        armorVisualColorMode = parseVisualColorMode(config.armorVisualColorMode, VisualColorMode.VIVID);
        heldItemVisualColorMode = parseVisualColorMode(config.heldItemVisualColorMode, VisualColorMode.VIVID);
        distanceVisualColorMode = parseVisualColorMode(config.distanceVisualColorMode, VisualColorMode.VIVID);
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

    private static VisualColorMode parseVisualColorMode(String rawValue, VisualColorMode fallback) {
        if (rawValue == null || rawValue.isBlank()) {
            return fallback;
        }

        try {
            return VisualColorMode.valueOf(rawValue.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ignored) {
            return fallback;
        }
    }

    private static OverlayAnchorMode parseOverlayAnchorMode(String rawValue, OverlayAnchorMode fallback) {
        if (rawValue == null || rawValue.isBlank()) {
            return fallback;
        }

        try {
            return OverlayAnchorMode.valueOf(rawValue.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ignored) {
            return fallback;
        }
    }

    private static NoKnockbackConfig.Data captureConfig() {
        NoKnockbackConfig.Data data = new NoKnockbackConfig.Data();
        data.speedEnabled = speedEnabled;
        data.playerEspEnabled = playerEspEnabled;
        data.playerArmorOverlayEnabled = playerArmorOverlayEnabled;
        data.playerRaysEnabled = playerRaysEnabled;
        data.playerListEnabled = playerListEnabled;
        data.targetHealthOverlayEnabled = targetHealthOverlayEnabled;
        data.targetHealthDynamicColorEnabled = targetHealthDynamicColorEnabled;
        data.distanceDisplayEnabled = distanceDisplayEnabled;
        data.heldItemOverlayEnabled = heldItemOverlayEnabled;
        data.rayVisualGlowEnabled = rayVisualGlowEnabled;
        data.armorVisualGlowEnabled = armorVisualGlowEnabled;
        data.heldItemVisualGlowEnabled = heldItemVisualGlowEnabled;
        data.distanceVisualGlowEnabled = distanceVisualGlowEnabled;
        data.rayThickness = rayThickness;
        data.outlineThickness = outlineThickness;
        data.rayBottomStartHeight = rayBottomStartHeight;
        data.rayDistanceTextScale = distanceTextScale;
        data.armorOverlayScale = armorOverlayScale;
        data.heldItemOverlayScale = heldItemOverlayScale;
        data.targetHealthTextScale = targetHealthTextScale;
        data.playerListTextScale = playerListTextScale;
        data.playerListMaxHeight = playerListMaxHeight;
        data.playerListAlpha = playerListAlphaMultiplier;
        data.rayVisualSaturationBoost = rayVisualSaturationBoost;
        data.rayVisualAnimationSpeed = rayVisualAnimationSpeed;
        data.armorVisualSaturationBoost = armorVisualSaturationBoost;
        data.armorVisualAnimationSpeed = armorVisualAnimationSpeed;
        data.heldItemVisualSaturationBoost = heldItemVisualSaturationBoost;
        data.heldItemVisualAnimationSpeed = heldItemVisualAnimationSpeed;
        data.distanceVisualSaturationBoost = distanceVisualSaturationBoost;
        data.distanceVisualAnimationSpeed = distanceVisualAnimationSpeed;
        data.playerListOffsetX = playerListOffsetX;
        data.playerListOffsetY = playerListOffsetY;
        data.rayOrigin = rayOrigin.name();
        data.armorAnchorMode = armorAnchorMode.name();
        data.heldItemAnchorMode = heldItemAnchorMode.name();
        data.distanceAnchorMode = distanceAnchorMode.name();
        data.rayVisualColorMode = rayVisualColorMode.name();
        data.armorVisualColorMode = armorVisualColorMode.name();
        data.heldItemVisualColorMode = heldItemVisualColorMode.name();
        data.distanceVisualColorMode = distanceVisualColorMode.name();

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

    private record PlayerDistanceEntry(String name, double distance, int displayColor) {
    }

    private record PlayerListLine(String text, int color) {
    }

    private record RayDistanceLabel(String text, float x, float y, int color) {
    }

    public enum RayOrigin {
        BOTTOM,
        CENTER
    }

    public enum VisualColorMode {
        NICK,
        VIVID,
        GRADIENT,
        RAINBOW
    }

    public enum OverlayAnchorMode {
        ABOVE_PLAYER,
        RAY_MIDDLE
    }
}
