package paprika;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.HudLayerRegistrationCallback;
import net.fabricmc.fabric.api.client.rendering.v1.IdentifiedLayer;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.RenderPhase;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.util.InputUtil;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.Sprite;
import net.minecraft.client.texture.SpriteContents;
import net.minecraft.client.item.ItemModelManager;
import net.minecraft.client.render.item.ItemRenderState;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ModelTransformationMode;
import net.minecraft.registry.Registries;
import net.minecraft.text.Text;
import net.minecraft.text.TextColor;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;
import paprika.mixin.client.GameRendererAccessor;

import org.lwjgl.glfw.GLFW;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class PaprikaClient implements ClientModInitializer {

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
    private static final int DEFAULT_SKY_TOP_COLOR = 0x78A7FF;
    private static final int DEFAULT_SKY_BOTTOM_COLOR = 0xA0C8FF;
    private static final int SKY_TOP_RAINBOW_SEED = 0x52E8D4;
    private static final int SKY_BOTTOM_RAINBOW_SEED = 0xC32B9F;
    private static final float DEFAULT_HAND_FOV_SCALE = 1.0F;
    private static final float DEFAULT_AUTO_ATTACK_RATE = 6.0F;
    private static final float DEFAULT_AUTO_ATTACK_CIRCLE_RADIUS = 120.0F;
    private static final int DEFAULT_AUTO_ATTACK_CIRCLE_COLOR = 0x4CB1FF;
    private static final int MAX_PLAYER_LIST_OFFSET = 4096;
    private static final int PLAYER_LIST_PADDING = 3;
    private static final int PLAYER_LIST_BG_COLOR = 0x65000000;
    private static final int PLAYER_LIST_BORDER_COLOR = 0x43FFFFFF;
    private static final float MAX_BOTTOM_RAY_START_HEIGHT = 300.0F;
    private static final float RAY_LABEL_POSITION_FACTOR = 0.62F;
    private static final float RAY_START_CLEARANCE = 12.0F;
    private static final int RAY_LABEL_BG_COLOR = 0x6A000000;
    private static final float AUTO_ATTACK_CIRCLE_THICKNESS = 1.6F;
    private static final int AUTO_ATTACK_CIRCLE_SEED = 0xC1CC1E;
    private static final float MARK_AIM_RADIUS = 18.0F;
    private static final int MAX_FRIEND_NAME_LENGTH = 16;
    private static final int ARMOR_OVERLAY_ICON_SPACING = 1;
    private static final int OVERLAY_GROUP_GAP = 4;
    private static final int TARGET_HEALTH_BG_COLOR = 0x7A000000;
    private static final int TARGET_HEALTH_COLOR_LIME = 0xFF8DFF39;
    private static final int TARGET_HEALTH_COLOR_YELLOW = 0xFFFFE44A;
    private static final int TARGET_HEALTH_COLOR_RED = 0xFFFF4F4F;
    private static final int TARGET_HEALTH_COLOR_DARK_RED = 0xFF7A0019;
    private static final float TWO_PI = (float) (Math.PI * 2.0);
    private static final float AUTO_ATTACK_AIM_SMOOTHING = 0.25F;
    private static final Identifier HUD_OVERLAY_LAYER_ID = Identifier.of("paprika", "hud_overlay");
    private static final RenderLayer ITEM_OUTLINE_QUADS = RenderLayer.of(
            "paprika_item_outline_quads",
            VertexFormats.POSITION_COLOR,
            VertexFormat.DrawMode.QUADS,
            1536,
            RenderLayer.MultiPhaseParameters.builder()
                    .program(RenderPhase.POSITION_COLOR_PROGRAM)
                    .transparency(RenderPhase.TRANSLUCENT_TRANSPARENCY)
                    .depthTest(RenderPhase.ALWAYS_DEPTH_TEST)
                    .cull(RenderPhase.DISABLE_CULLING)
                    .writeMaskState(RenderPhase.COLOR_MASK)
                    .target(RenderPhase.MAIN_TARGET)
                    .layering(RenderPhase.VIEW_OFFSET_Z_LAYERING)
                    .build(false)
    );
    private static int rayVisualRevision = 0;
    private static int espVisualRevision = 0;
    private static int armorVisualRevision = 0;
    private static int heldItemVisualRevision = 0;
    private static int distanceVisualRevision = 0;
    private static int itemOutlineVisualRevision = 0;

    private static boolean speedEnabled = true;
    private static boolean noKnockbackEnabled = true;
    private static boolean playerEspEnabled = false;
    private static boolean espVisualGlowEnabled = false;
    private static boolean playerArmorOverlayEnabled = false;
    private static boolean playerRaysEnabled = false;
    private static boolean playerListEnabled = false;
    private static boolean playerTrailsEnabled = false;
    private static boolean trailSelfEnabled = true;
    private static boolean trailOthersEnabled = true;
    private static boolean autoAttackEnabled = false;
    private static boolean autoAttackAimEnabled = false;
    private static boolean itemOutlineEnabled = false;
    private static boolean panicActive = false;
    private static boolean targetHealthOverlayEnabled = false;
    private static boolean targetHealthDynamicColorEnabled = true;
    private static boolean distanceDisplayEnabled = true;
    private static boolean heldItemOverlayEnabled = false;
    private static boolean customSkyEnabled = false;
    private static boolean skyTopRainbowEnabled = false;
    private static boolean skyBottomRainbowEnabled = false;
    private static boolean hideHandsWithItemEnabled = false;
    private static boolean rayVisualGlowEnabled = false;
    private static boolean armorVisualGlowEnabled = false;
    private static boolean heldItemVisualGlowEnabled = false;
    private static boolean distanceVisualGlowEnabled = false;
    private static boolean autoAttackRequireLineOfSight = true;
    private static boolean itemOutlineGlowEnabled = false;
    private static float rayThickness = 2.0F;
    private static float outlineThickness = 1.0F;
    private static float rayBottomStartHeight = 2.0F;
    private static float distanceTextScale = DEFAULT_RAY_LABEL_TEXT_SCALE;
    private static float armorOverlayScale = DEFAULT_EQUIP_ICON_SCALE;
    private static float heldItemOverlayScale = DEFAULT_EQUIP_ICON_SCALE;
    private static float rayAlpha = 1.0F;
    private static float armorAlpha = 1.0F;
    private static float heldItemAlpha = 1.0F;
    private static float distanceAlpha = 1.0F;
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
    private static float espVisualSaturationBoost = DEFAULT_STYLE_SATURATION;
    private static float espVisualAnimationSpeed = DEFAULT_STYLE_ANIMATION_SPEED;
    private static float itemOutlineAlpha = 1.0F;
    private static float itemOutlineSaturationBoost = DEFAULT_STYLE_SATURATION;
    private static float itemOutlineAnimationSpeed = DEFAULT_STYLE_ANIMATION_SPEED;
    private static float itemOutlineThickness = 1.0F;
    private static int itemOutlineSolidColor = 0x4CB1FF;
    private static float trailStripeHeight = 1.4F;
    private static float trailLifetimeSeconds = 2.5F;
    private static float trailGradientSpeed = 1.0F;
    private static float trailAlpha = 1.0F;
    private static float autoAttackRate = DEFAULT_AUTO_ATTACK_RATE;
    private static float autoAttackCircleRadius = DEFAULT_AUTO_ATTACK_CIRCLE_RADIUS;
    private static float autoAttackMaxDistance = 3.0F;
    private static boolean jumpBoostEnabled = false;
    private static float jumpBoostHeight = 0.5F;
    private static float handFovScale = DEFAULT_HAND_FOV_SCALE;
    private static float handOffsetX = 0.0F;
    private static float handOffsetY = 0.0F;
    private static boolean handItemFlipEnabled = false;
    private static HandItemOrientation handItemOrientation = HandItemOrientation.DEFAULT;
    private static int playerListOffsetX = DEFAULT_PLAYER_LIST_X;
    private static int playerListOffsetY = DEFAULT_PLAYER_LIST_Y;
    private static String menuLastTabId = "RAYS";
    private static double menuScrollOffset = 0.0;
    private static RayOrigin rayOrigin = RayOrigin.BOTTOM;
    private static OverlayAnchorMode armorAnchorMode = OverlayAnchorMode.ABOVE_PLAYER;
    private static OverlayAnchorMode heldItemAnchorMode = OverlayAnchorMode.ABOVE_PLAYER;
    private static OverlayAnchorMode distanceAnchorMode = OverlayAnchorMode.RAY_MIDDLE;
    private static VisualColorMode rayVisualColorMode = VisualColorMode.NICK;
    private static VisualColorMode armorVisualColorMode = VisualColorMode.NICK;
    private static VisualColorMode heldItemVisualColorMode = VisualColorMode.NICK;
    private static VisualColorMode distanceVisualColorMode = VisualColorMode.NICK;
    private static VisualColorMode espVisualColorMode = VisualColorMode.NICK;
    private static ItemOutlineColorMode itemOutlineColorMode = ItemOutlineColorMode.NICK;
    private static ItemOutlineMode itemOutlineMode = ItemOutlineMode.ALL;
    private static AutoAttackMode autoAttackMode = AutoAttackMode.CIRCLE;
    private static CircleColorMode autoAttackCircleColorMode = CircleColorMode.FIXED;
    private static TrailType trailType = TrailType.THIN_LINE;
    private static TrailOrigin trailOrigin = TrailOrigin.BACK;
    private static TrailColorMode trailColorMode = TrailColorMode.NICK;
    private static int skyTopColor = DEFAULT_SKY_TOP_COLOR;
    private static int skyBottomColor = DEFAULT_SKY_BOTTOM_COLOR;
    private static int trailFixedColor = 0x4CB1FF;
    private static int autoAttackCircleColor = DEFAULT_AUTO_ATTACK_CIRCLE_COLOR;
    private static KeyBinding toggleKey;
    private static KeyBinding toggleNoKnockbackKey;
    private static KeyBinding togglePlayerEspKey;
    private static KeyBinding togglePlayerRaysKey;
    private static KeyBinding togglePlayerListKey;
    private static KeyBinding togglePlayerTrailsKey;
    private static KeyBinding toggleAutoAttackKey;
    private static KeyBinding toggleItemOutlineKey;
    private static KeyBinding markTargetKey;
    private static KeyBinding unmarkTargetKey;
    private static KeyBinding markFriendKey;
    private static KeyBinding panicKey;
    private static KeyBinding openMenuKey;

    private Vec3d lastVelocity = Vec3d.ZERO;
    private static String markedPlayerName;
    private static double lastAutoAttackTime;
    private static String lastFriendMarkName;
    private static boolean jumpKeyWasPressed;
    private static final Map<String, String> friendNames = new LinkedHashMap<>();
    private static final Map<String, String> itemFilterIds = new LinkedHashMap<>();
    private static final Map<String, Integer> itemAverageColorCache = new HashMap<>();

    private static final Map<UUID, TrailState> trailStates = new HashMap<>();
    private static final List<TrailSegment> trailSegments = new ArrayList<>();

    public static boolean isPlayerEspEnabled() {
        return playerEspEnabled;
    }

    public static boolean isSpeedEnabled() {
        return speedEnabled;
    }

    public static boolean isNoKnockbackEnabled() {
        return noKnockbackEnabled;
    }

    public static void setSpeedEnabled(boolean enabled) {
        if (speedEnabled == enabled) return;
        speedEnabled = enabled;
        saveConfigNow();
    }

    public static void setNoKnockbackEnabled(boolean enabled) {
        if (noKnockbackEnabled == enabled) return;
        noKnockbackEnabled = enabled;
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

    public static boolean isPlayerTrailsEnabled() {
        return playerTrailsEnabled;
    }

    public static void setPlayerTrailsEnabled(boolean enabled) {
        if (playerTrailsEnabled == enabled) return;
        playerTrailsEnabled = enabled;
        if (!enabled) {
            trailSegments.clear();
            trailStates.clear();
        }
        saveConfigNow();
    }

    public static boolean isItemOutlineEnabled() {
        return itemOutlineEnabled;
    }

    public static void setItemOutlineEnabled(boolean enabled) {
        if (itemOutlineEnabled == enabled) return;
        itemOutlineEnabled = enabled;
        saveConfigNow();
    }

    public static boolean isPanicActive() {
        return panicActive;
    }

    public static boolean isTrailSelfEnabled() {
        return trailSelfEnabled;
    }

    public static void setTrailSelfEnabled(boolean enabled) {
        if (trailSelfEnabled == enabled) return;
        trailSelfEnabled = enabled;
        saveConfigNow();
    }

    public static boolean isTrailOthersEnabled() {
        return trailOthersEnabled;
    }

    public static void setTrailOthersEnabled(boolean enabled) {
        if (trailOthersEnabled == enabled) return;
        trailOthersEnabled = enabled;
        saveConfigNow();
    }

    public static boolean isAutoAttackEnabled() {
        return autoAttackEnabled;
    }

    public static void setAutoAttackEnabled(boolean enabled) {
        if (autoAttackEnabled == enabled) return;
        autoAttackEnabled = enabled;
        saveConfigNow();
    }

    public static boolean isAutoAttackAimEnabled() {
        return autoAttackAimEnabled;
    }

    public static void setAutoAttackAimEnabled(boolean enabled) {
        if (autoAttackAimEnabled == enabled) return;
        autoAttackAimEnabled = enabled;
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

    public static boolean isAutoAttackRequireLineOfSight() {
        return autoAttackRequireLineOfSight;
    }

    public static void setAutoAttackRequireLineOfSight(boolean enabled) {
        if (autoAttackRequireLineOfSight == enabled) return;
        autoAttackRequireLineOfSight = enabled;
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

    public static boolean isCustomSkyEnabled() {
        return customSkyEnabled;
    }

    public static void setCustomSkyEnabled(boolean enabled) {
        if (customSkyEnabled == enabled) return;
        customSkyEnabled = enabled;
        saveConfigNow();
    }

    public static boolean isSkyTopRainbowEnabled() {
        return skyTopRainbowEnabled;
    }

    public static void setSkyTopRainbowEnabled(boolean enabled) {
        if (skyTopRainbowEnabled == enabled) return;
        skyTopRainbowEnabled = enabled;
        saveConfigNow();
    }

    public static boolean isSkyBottomRainbowEnabled() {
        return skyBottomRainbowEnabled;
    }

    public static void setSkyBottomRainbowEnabled(boolean enabled) {
        if (skyBottomRainbowEnabled == enabled) return;
        skyBottomRainbowEnabled = enabled;
        saveConfigNow();
    }

    public static boolean isHideHandsWithItemEnabled() {
        return hideHandsWithItemEnabled;
    }

    public static void setHideHandsWithItemEnabled(boolean enabled) {
        if (hideHandsWithItemEnabled == enabled) return;
        hideHandsWithItemEnabled = enabled;
        saveConfigNow();
    }

    public static List<String> getFriendNames() {
        return new ArrayList<>(friendNames.values());
    }

    public static boolean addFriendName(String name) {
        String sanitized = sanitizeFriendName(name);
        if (sanitized == null) return false;
        String key = sanitized.toLowerCase(Locale.ROOT);
        if (friendNames.containsKey(key)) return false;
        friendNames.put(key, sanitized);
        saveConfigNow();
        return true;
    }

    public static void clearFriends() {
        if (friendNames.isEmpty()) return;
        friendNames.clear();
        saveConfigNow();
    }

    static String sanitizeFriendName(String name) {
        if (name == null) return null;
        String trimmed = name.trim();
        if (trimmed.isEmpty()) return null;
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < trimmed.length(); i++) {
            char c = trimmed.charAt(i);
            if (Character.isLetterOrDigit(c) || c == '_') {
                builder.append(c);
                if (builder.length() >= MAX_FRIEND_NAME_LENGTH) {
                    break;
                }
            }
        }
        if (builder.length() == 0) return null;
        return builder.toString();
    }

    public static List<String> getItemFilterEntries() {
        return new ArrayList<>(itemFilterIds.values());
    }

    public static boolean addItemFilterEntry(String rawId) {
        String sanitized = sanitizeItemId(rawId);
        if (sanitized == null) return false;
        String key = sanitized.toLowerCase(Locale.ROOT);
        if (itemFilterIds.containsKey(key)) return false;
        itemFilterIds.put(key, sanitized);
        saveConfigNow();
        return true;
    }

    public static void clearItemFilterEntries() {
        if (itemFilterIds.isEmpty()) return;
        itemFilterIds.clear();
        saveConfigNow();
    }

    static String sanitizeItemId(String rawId) {
        if (rawId == null) return null;
        String trimmed = rawId.trim().toLowerCase(Locale.ROOT);
        if (trimmed.isEmpty()) return null;
        Identifier id = Identifier.tryParse(trimmed);
        if (id == null) return null;
        return id.toString();
    }

    public static float getHandFovScale() {
        return handFovScale;
    }

    public static void setHandFovScale(float scale) {
        float clamped = MathHelper.clamp(scale, -1.6F, 1.6F);
        if (Math.abs(handFovScale - clamped) < 0.0001F) return;
        handFovScale = clamped;
        saveConfigNow();
    }

    public static float getHandOffsetX() {
        return handOffsetX;
    }

    public static void setHandOffsetX(float offset) {
        float clamped = MathHelper.clamp(offset, -1.5F, 1.5F);
        if (Math.abs(handOffsetX - clamped) < 0.0001F) return;
        handOffsetX = clamped;
        saveConfigNow();
    }

    public static float getHandOffsetY() {
        return handOffsetY;
    }

    public static void setHandOffsetY(float offset) {
        float clamped = MathHelper.clamp(offset, -1.5F, 1.5F);
        if (Math.abs(handOffsetY - clamped) < 0.0001F) return;
        handOffsetY = clamped;
        saveConfigNow();
    }

    public static boolean isHandItemFlipEnabled() {
        return handItemFlipEnabled;
    }

    public static void setHandItemFlipEnabled(boolean enabled) {
        if (handItemFlipEnabled == enabled) return;
        handItemFlipEnabled = enabled;
        saveConfigNow();
    }

    public static HandItemOrientation getHandItemOrientation() {
        return handItemOrientation;
    }

    public static void setHandItemOrientation(HandItemOrientation orientation) {
        HandItemOrientation updated = orientation == null ? HandItemOrientation.DEFAULT : orientation;
        if (handItemOrientation == updated) return;
        handItemOrientation = updated;
        saveConfigNow();
    }

    public static int getSkyTopColor() {
        if (customSkyEnabled && skyTopRainbowEnabled) {
            return toRainbowColor(SKY_TOP_RAINBOW_SEED, 0.0F, 1.1F, 1.0F);
        }
        return skyTopColor & 0xFFFFFF;
    }

    public static int getSkyBottomColor() {
        if (customSkyEnabled && skyBottomRainbowEnabled) {
            return toRainbowColor(SKY_BOTTOM_RAINBOW_SEED, 0.45F, 1.1F, 1.0F);
        }
        return skyBottomColor & 0xFFFFFF;
    }

    public static Vec3d getSkyTopColorVec() {
        return rgbToVec3(getSkyTopColor());
    }

    public static Vec3d getSkyBottomColorVec() {
        return rgbToVec3(getSkyBottomColor());
    }

    public static int getSkyTopRed() {
        return (skyTopColor >> 16) & 0xFF;
    }

    public static int getSkyTopGreen() {
        return (skyTopColor >> 8) & 0xFF;
    }

    public static int getSkyTopBlue() {
        return skyTopColor & 0xFF;
    }

    public static int getSkyBottomRed() {
        return (skyBottomColor >> 16) & 0xFF;
    }

    public static int getSkyBottomGreen() {
        return (skyBottomColor >> 8) & 0xFF;
    }

    public static int getSkyBottomBlue() {
        return skyBottomColor & 0xFF;
    }

    public static void setSkyTopRed(int value) {
        int clamped = MathHelper.clamp(value, 0, 255);
        int updated = (skyTopColor & 0x00FFFF) | (clamped << 16);
        if (skyTopColor == updated) return;
        skyTopColor = updated;
        saveConfigNow();
    }

    public static void setSkyTopGreen(int value) {
        int clamped = MathHelper.clamp(value, 0, 255);
        int updated = (skyTopColor & 0xFF00FF) | (clamped << 8);
        if (skyTopColor == updated) return;
        skyTopColor = updated;
        saveConfigNow();
    }

    public static void setSkyTopBlue(int value) {
        int clamped = MathHelper.clamp(value, 0, 255);
        int updated = (skyTopColor & 0xFFFF00) | clamped;
        if (skyTopColor == updated) return;
        skyTopColor = updated;
        saveConfigNow();
    }

    public static void setSkyBottomRed(int value) {
        int clamped = MathHelper.clamp(value, 0, 255);
        int updated = (skyBottomColor & 0x00FFFF) | (clamped << 16);
        if (skyBottomColor == updated) return;
        skyBottomColor = updated;
        saveConfigNow();
    }

    public static void setSkyBottomGreen(int value) {
        int clamped = MathHelper.clamp(value, 0, 255);
        int updated = (skyBottomColor & 0xFF00FF) | (clamped << 8);
        if (skyBottomColor == updated) return;
        skyBottomColor = updated;
        saveConfigNow();
    }

    public static void setSkyBottomBlue(int value) {
        int clamped = MathHelper.clamp(value, 0, 255);
        int updated = (skyBottomColor & 0xFFFF00) | clamped;
        if (skyBottomColor == updated) return;
        skyBottomColor = updated;
        saveConfigNow();
    }

    public static String getMenuLastTabId() {
        return menuLastTabId;
    }

    public static void setMenuLastTabId(String tabId) {
        if (tabId == null || tabId.isBlank()) {
            return;
        }
        if (tabId.equals(menuLastTabId)) return;
        menuLastTabId = tabId;
    }

    public static double getMenuScrollOffset() {
        return menuScrollOffset;
    }

    public static void setMenuScrollOffset(double offset) {
        if (Math.abs(menuScrollOffset - offset) < 0.0001) return;
        menuScrollOffset = offset;
    }

    public static void persistMenuState() {
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

    public static float getRayAlpha() {
        return rayAlpha;
    }

    public static void setRayAlpha(float alpha) {
        float clamped = MathHelper.clamp(alpha, 0.1F, 1.0F);
        if (Math.abs(rayAlpha - clamped) < 0.0001F) return;
        rayAlpha = clamped;
        saveConfigNow();
    }

    public static float getArmorAlpha() {
        return armorAlpha;
    }

    public static void setArmorAlpha(float alpha) {
        float clamped = MathHelper.clamp(alpha, 0.1F, 1.0F);
        if (Math.abs(armorAlpha - clamped) < 0.0001F) return;
        armorAlpha = clamped;
        saveConfigNow();
    }

    public static float getHeldItemAlpha() {
        return heldItemAlpha;
    }

    public static void setHeldItemAlpha(float alpha) {
        float clamped = MathHelper.clamp(alpha, 0.1F, 1.0F);
        if (Math.abs(heldItemAlpha - clamped) < 0.0001F) return;
        heldItemAlpha = clamped;
        saveConfigNow();
    }

    public static float getDistanceAlpha() {
        return distanceAlpha;
    }

    public static void setDistanceAlpha(float alpha) {
        float clamped = MathHelper.clamp(alpha, 0.1F, 1.0F);
        if (Math.abs(distanceAlpha - clamped) < 0.0001F) return;
        distanceAlpha = clamped;
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

    public static boolean isEspVisualGlowEnabled() {
        return espVisualGlowEnabled;
    }

    public static void setEspVisualGlowEnabled(boolean enabled) {
        if (espVisualGlowEnabled == enabled) return;
        espVisualGlowEnabled = enabled;
        saveConfigNow();
    }

    public static boolean isItemOutlineGlowEnabled() {
        return itemOutlineGlowEnabled;
    }

    public static void setItemOutlineGlowEnabled(boolean enabled) {
        if (itemOutlineGlowEnabled == enabled) return;
        itemOutlineGlowEnabled = enabled;
        saveConfigNow();
    }

    public static VisualColorMode getEspVisualColorMode() {
        return espVisualColorMode;
    }

    public static void setEspVisualColorMode(VisualColorMode mode) {
        VisualColorMode updated = mode == null ? VisualColorMode.NICK : mode;
        if (espVisualColorMode == updated) return;
        espVisualColorMode = updated;
        bumpEspRevision();
        saveConfigNow();
    }

    public static ItemOutlineColorMode getItemOutlineColorMode() {
        return itemOutlineColorMode;
    }

    public static void setItemOutlineColorMode(ItemOutlineColorMode mode) {
        ItemOutlineColorMode updated = mode == null ? ItemOutlineColorMode.NICK : mode;
        if (itemOutlineColorMode == updated) return;
        itemOutlineColorMode = updated;
        bumpItemOutlineRevision();
        saveConfigNow();
    }

    public static ItemOutlineMode getItemOutlineMode() {
        return itemOutlineMode;
    }

    public static void setItemOutlineMode(ItemOutlineMode mode) {
        ItemOutlineMode updated = mode == null ? ItemOutlineMode.ALL : mode;
        if (itemOutlineMode == updated) return;
        itemOutlineMode = updated;
        saveConfigNow();
    }

    public static float getEspVisualSaturationBoost() {
        return espVisualSaturationBoost;
    }

    public static void setEspVisualSaturationBoost(float boost) {
        float clamped = MathHelper.clamp(boost, 1.0F, 2.5F);
        if (Math.abs(espVisualSaturationBoost - clamped) < 0.0001F) return;
        espVisualSaturationBoost = clamped;
        bumpEspRevision();
        saveConfigNow();
    }

    public static float getItemOutlineSaturationBoost() {
        return itemOutlineSaturationBoost;
    }

    public static void setItemOutlineSaturationBoost(float boost) {
        float clamped = MathHelper.clamp(boost, 1.0F, 2.5F);
        if (Math.abs(itemOutlineSaturationBoost - clamped) < 0.0001F) return;
        itemOutlineSaturationBoost = clamped;
        bumpItemOutlineRevision();
        saveConfigNow();
    }

    public static float getEspVisualAnimationSpeed() {
        return espVisualAnimationSpeed;
    }

    public static void setEspVisualAnimationSpeed(float speed) {
        float clamped = MathHelper.clamp(speed, 0.2F, 4.0F);
        if (Math.abs(espVisualAnimationSpeed - clamped) < 0.0001F) return;
        espVisualAnimationSpeed = clamped;
        bumpEspRevision();
        saveConfigNow();
    }

    public static float getItemOutlineAnimationSpeed() {
        return itemOutlineAnimationSpeed;
    }

    public static void setItemOutlineAnimationSpeed(float speed) {
        float clamped = MathHelper.clamp(speed, 0.2F, 4.0F);
        if (Math.abs(itemOutlineAnimationSpeed - clamped) < 0.0001F) return;
        itemOutlineAnimationSpeed = clamped;
        bumpItemOutlineRevision();
        saveConfigNow();
    }

    public static float getItemOutlineAlpha() {
        return itemOutlineAlpha;
    }

    public static void setItemOutlineAlpha(float alpha) {
        float clamped = MathHelper.clamp(alpha, 0.05F, 1.0F);
        if (Math.abs(itemOutlineAlpha - clamped) < 0.0001F) return;
        itemOutlineAlpha = clamped;
        saveConfigNow();
    }

    public static float getItemOutlineThickness() {
        return itemOutlineThickness;
    }

    public static void setItemOutlineThickness(float thickness) {
        float clamped = MathHelper.clamp(thickness, 0.5F, 6.0F);
        if (Math.abs(itemOutlineThickness - clamped) < 0.0001F) return;
        itemOutlineThickness = clamped;
        saveConfigNow();
    }

    public static int getItemOutlineSolidRed() {
        return (itemOutlineSolidColor >> 16) & 0xFF;
    }

    public static void setItemOutlineSolidRed(int value) {
        int clamped = MathHelper.clamp(value, 0, 255);
        itemOutlineSolidColor = (itemOutlineSolidColor & 0xFF00FFFF) | (clamped << 16);
        saveConfigNow();
    }

    public static int getItemOutlineSolidGreen() {
        return (itemOutlineSolidColor >> 8) & 0xFF;
    }

    public static void setItemOutlineSolidGreen(int value) {
        int clamped = MathHelper.clamp(value, 0, 255);
        itemOutlineSolidColor = (itemOutlineSolidColor & 0xFFFF00FF) | (clamped << 8);
        saveConfigNow();
    }

    public static int getItemOutlineSolidBlue() {
        return itemOutlineSolidColor & 0xFF;
    }

    public static void setItemOutlineSolidBlue(int value) {
        int clamped = MathHelper.clamp(value, 0, 255);
        itemOutlineSolidColor = (itemOutlineSolidColor & 0xFFFFFF00) | clamped;
        saveConfigNow();
    }

    public static TrailType getTrailType() {
        return trailType;
    }

    public static void setTrailType(TrailType type) {
        TrailType updated = type == null ? TrailType.THIN_LINE : type;
        if (trailType == updated) return;
        trailType = updated;
        saveConfigNow();
    }

    public static TrailOrigin getTrailOrigin() {
        return trailOrigin;
    }

    public static void setTrailOrigin(TrailOrigin origin) {
        TrailOrigin updated = origin == null ? TrailOrigin.BACK : origin;
        if (trailOrigin == updated) return;
        trailOrigin = updated;
        saveConfigNow();
    }

    public static TrailColorMode getTrailColorMode() {
        return trailColorMode;
    }

    public static void setTrailColorMode(TrailColorMode mode) {
        TrailColorMode updated = mode == null ? TrailColorMode.NICK : mode;
        if (trailColorMode == updated) return;
        trailColorMode = updated;
        saveConfigNow();
    }

    public static float getTrailStripeHeight() {
        return trailStripeHeight;
    }

    public static void setTrailStripeHeight(float height) {
        float clamped = MathHelper.clamp(height, 0.2F, 4.0F);
        if (Math.abs(trailStripeHeight - clamped) < 0.0001F) return;
        trailStripeHeight = clamped;
        saveConfigNow();
    }

    public static float getTrailLifetimeSeconds() {
        return trailLifetimeSeconds;
    }

    public static void setTrailLifetimeSeconds(float seconds) {
        float clamped = MathHelper.clamp(seconds, 0.1F, 10.0F);
        if (Math.abs(trailLifetimeSeconds - clamped) < 0.0001F) return;
        trailLifetimeSeconds = clamped;
        saveConfigNow();
    }

    public static float getTrailAlpha() {
        return trailAlpha;
    }

    public static void setTrailAlpha(float alpha) {
        float clamped = MathHelper.clamp(alpha, 0.1F, 1.0F);
        if (Math.abs(trailAlpha - clamped) < 0.0001F) return;
        trailAlpha = clamped;
        saveConfigNow();
    }

    public static float getTrailGradientSpeed() {
        return trailGradientSpeed;
    }

    public static void setTrailGradientSpeed(float speed) {
        float clamped = MathHelper.clamp(speed, 0.1F, 5.0F);
        if (Math.abs(trailGradientSpeed - clamped) < 0.0001F) return;
        trailGradientSpeed = clamped;
        saveConfigNow();
    }

    public static float getAutoAttackRate() {
        return autoAttackRate;
    }

    public static void setAutoAttackRate(float rate) {
        float clamped = MathHelper.clamp(rate, 1.0F, 20.0F);
        if (Math.abs(autoAttackRate - clamped) < 0.0001F) return;
        autoAttackRate = clamped;
        saveConfigNow();
    }

    public static float getAutoAttackMaxDistance() {
        return autoAttackMaxDistance;
    }

    public static void setAutoAttackMaxDistance(float distance) {
        float clamped = MathHelper.clamp(distance, 3.0F, 20.0F);
        if (Math.abs(autoAttackMaxDistance - clamped) < 0.0001F) return;
        autoAttackMaxDistance = clamped;
        saveConfigNow();
    }

    public static float getAutoAttackCircleRadius() {
        return autoAttackCircleRadius;
    }

    public static void setAutoAttackCircleRadius(float radius) {
        float clamped = MathHelper.clamp(radius, 20.0F, 600.0F);
        if (Math.abs(autoAttackCircleRadius - clamped) < 0.0001F) return;
        autoAttackCircleRadius = clamped;
        saveConfigNow();
    }

    public static boolean isJumpBoostEnabled() {
        return jumpBoostEnabled;
    }

    public static void setJumpBoostEnabled(boolean enabled) {
        if (jumpBoostEnabled == enabled) return;
        jumpBoostEnabled = enabled;
        saveConfigNow();
    }

    public static float getJumpBoostHeight() {
        return jumpBoostHeight;
    }

    public static void setJumpBoostHeight(float height) {
        float clamped = MathHelper.clamp(height, 0.0F, 2.5F);
        if (Math.abs(jumpBoostHeight - clamped) < 0.0001F) return;
        jumpBoostHeight = clamped;
        saveConfigNow();
    }

    public static int getTrailFixedColor() {
        return trailFixedColor & 0xFFFFFF;
    }

    public static int getTrailFixedRed() {
        return (trailFixedColor >> 16) & 0xFF;
    }

    public static int getTrailFixedGreen() {
        return (trailFixedColor >> 8) & 0xFF;
    }

    public static int getTrailFixedBlue() {
        return trailFixedColor & 0xFF;
    }

    public static void setTrailFixedRed(int value) {
        int clamped = MathHelper.clamp(value, 0, 255);
        int updated = (trailFixedColor & 0x00FFFF) | (clamped << 16);
        if (trailFixedColor == updated) return;
        trailFixedColor = updated;
        saveConfigNow();
    }

    public static void setTrailFixedGreen(int value) {
        int clamped = MathHelper.clamp(value, 0, 255);
        int updated = (trailFixedColor & 0xFF00FF) | (clamped << 8);
        if (trailFixedColor == updated) return;
        trailFixedColor = updated;
        saveConfigNow();
    }

    public static void setTrailFixedBlue(int value) {
        int clamped = MathHelper.clamp(value, 0, 255);
        int updated = (trailFixedColor & 0xFFFF00) | clamped;
        if (trailFixedColor == updated) return;
        trailFixedColor = updated;
        saveConfigNow();
    }

    public static AutoAttackMode getAutoAttackMode() {
        return autoAttackMode;
    }

    public static void setAutoAttackMode(AutoAttackMode mode) {
        AutoAttackMode updated = mode == null ? AutoAttackMode.CIRCLE : mode;
        if (autoAttackMode == updated) return;
        autoAttackMode = updated;
        saveConfigNow();
    }

    public static CircleColorMode getAutoAttackCircleColorMode() {
        return autoAttackCircleColorMode;
    }

    public static void setAutoAttackCircleColorMode(CircleColorMode mode) {
        CircleColorMode updated = mode == null ? CircleColorMode.FIXED : mode;
        if (autoAttackCircleColorMode == updated) return;
        autoAttackCircleColorMode = updated;
        saveConfigNow();
    }

    public static int getAutoAttackCircleColor() {
        return autoAttackCircleColor & 0xFFFFFF;
    }

    public static int getAutoAttackCircleRed() {
        return (autoAttackCircleColor >> 16) & 0xFF;
    }

    public static int getAutoAttackCircleGreen() {
        return (autoAttackCircleColor >> 8) & 0xFF;
    }

    public static int getAutoAttackCircleBlue() {
        return autoAttackCircleColor & 0xFF;
    }

    public static void setAutoAttackCircleRed(int value) {
        int clamped = MathHelper.clamp(value, 0, 255);
        int updated = (autoAttackCircleColor & 0x00FFFF) | (clamped << 16);
        if (autoAttackCircleColor == updated) return;
        autoAttackCircleColor = updated;
        saveConfigNow();
    }

    public static void setAutoAttackCircleGreen(int value) {
        int clamped = MathHelper.clamp(value, 0, 255);
        int updated = (autoAttackCircleColor & 0xFF00FF) | (clamped << 8);
        if (autoAttackCircleColor == updated) return;
        autoAttackCircleColor = updated;
        saveConfigNow();
    }

    public static void setAutoAttackCircleBlue(int value) {
        int clamped = MathHelper.clamp(value, 0, 255);
        int updated = (autoAttackCircleColor & 0xFFFF00) | clamped;
        if (autoAttackCircleColor == updated) return;
        autoAttackCircleColor = updated;
        saveConfigNow();
    }

    public static VisualColorMode getRayVisualColorMode() {
        return rayVisualColorMode;
    }

    public static void setRayVisualColorMode(VisualColorMode mode) {
        VisualColorMode updated = mode == null ? VisualColorMode.NICK : mode;
        if (rayVisualColorMode == updated) return;
        rayVisualColorMode = updated;
        bumpRayRevision();
        saveConfigNow();
    }

    public static VisualColorMode getArmorVisualColorMode() {
        return armorVisualColorMode;
    }

    public static void setArmorVisualColorMode(VisualColorMode mode) {
        VisualColorMode updated = mode == null ? VisualColorMode.NICK : mode;
        if (armorVisualColorMode == updated) return;
        armorVisualColorMode = updated;
        bumpArmorRevision();
        saveConfigNow();
    }

    public static VisualColorMode getHeldItemVisualColorMode() {
        return heldItemVisualColorMode;
    }

    public static void setHeldItemVisualColorMode(VisualColorMode mode) {
        VisualColorMode updated = mode == null ? VisualColorMode.NICK : mode;
        if (heldItemVisualColorMode == updated) return;
        heldItemVisualColorMode = updated;
        bumpHeldItemRevision();
        saveConfigNow();
    }

    public static VisualColorMode getDistanceVisualColorMode() {
        return distanceVisualColorMode;
    }

    public static void setDistanceVisualColorMode(VisualColorMode mode) {
        VisualColorMode updated = mode == null ? VisualColorMode.NICK : mode;
        if (distanceVisualColorMode == updated) return;
        distanceVisualColorMode = updated;
        bumpDistanceRevision();
        saveConfigNow();
    }

    public static float getRayVisualSaturationBoost() {
        return rayVisualSaturationBoost;
    }

    public static void setRayVisualSaturationBoost(float boost) {
        float clamped = MathHelper.clamp(boost, 1.0F, 2.5F);
        if (Math.abs(rayVisualSaturationBoost - clamped) < 0.0001F) return;
        rayVisualSaturationBoost = clamped;
        bumpRayRevision();
        saveConfigNow();
    }

    public static float getArmorVisualSaturationBoost() {
        return armorVisualSaturationBoost;
    }

    public static void setArmorVisualSaturationBoost(float boost) {
        float clamped = MathHelper.clamp(boost, 1.0F, 2.5F);
        if (Math.abs(armorVisualSaturationBoost - clamped) < 0.0001F) return;
        armorVisualSaturationBoost = clamped;
        bumpArmorRevision();
        saveConfigNow();
    }

    public static float getHeldItemVisualSaturationBoost() {
        return heldItemVisualSaturationBoost;
    }

    public static void setHeldItemVisualSaturationBoost(float boost) {
        float clamped = MathHelper.clamp(boost, 1.0F, 2.5F);
        if (Math.abs(heldItemVisualSaturationBoost - clamped) < 0.0001F) return;
        heldItemVisualSaturationBoost = clamped;
        bumpHeldItemRevision();
        saveConfigNow();
    }

    public static float getDistanceVisualSaturationBoost() {
        return distanceVisualSaturationBoost;
    }

    public static void setDistanceVisualSaturationBoost(float boost) {
        float clamped = MathHelper.clamp(boost, 1.0F, 2.5F);
        if (Math.abs(distanceVisualSaturationBoost - clamped) < 0.0001F) return;
        distanceVisualSaturationBoost = clamped;
        bumpDistanceRevision();
        saveConfigNow();
    }

    public static float getRayVisualAnimationSpeed() {
        return rayVisualAnimationSpeed;
    }

    public static void setRayVisualAnimationSpeed(float speed) {
        float clamped = MathHelper.clamp(speed, 0.2F, 4.0F);
        if (Math.abs(rayVisualAnimationSpeed - clamped) < 0.0001F) return;
        rayVisualAnimationSpeed = clamped;
        bumpRayRevision();
        saveConfigNow();
    }

    public static float getArmorVisualAnimationSpeed() {
        return armorVisualAnimationSpeed;
    }

    public static void setArmorVisualAnimationSpeed(float speed) {
        float clamped = MathHelper.clamp(speed, 0.2F, 4.0F);
        if (Math.abs(armorVisualAnimationSpeed - clamped) < 0.0001F) return;
        armorVisualAnimationSpeed = clamped;
        bumpArmorRevision();
        saveConfigNow();
    }

    public static float getHeldItemVisualAnimationSpeed() {
        return heldItemVisualAnimationSpeed;
    }

    public static void setHeldItemVisualAnimationSpeed(float speed) {
        float clamped = MathHelper.clamp(speed, 0.2F, 4.0F);
        if (Math.abs(heldItemVisualAnimationSpeed - clamped) < 0.0001F) return;
        heldItemVisualAnimationSpeed = clamped;
        bumpHeldItemRevision();
        saveConfigNow();
    }

    public static float getDistanceVisualAnimationSpeed() {
        return distanceVisualAnimationSpeed;
    }

    public static void setDistanceVisualAnimationSpeed(float speed) {
        float clamped = MathHelper.clamp(speed, 0.2F, 4.0F);
        if (Math.abs(distanceVisualAnimationSpeed - clamped) < 0.0001F) return;
        distanceVisualAnimationSpeed = clamped;
        bumpDistanceRevision();
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

    public static KeyBinding getNoKnockbackKeyBinding() {
        return toggleNoKnockbackKey;
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

    public static KeyBinding getPlayerTrailsKeyBinding() {
        return togglePlayerTrailsKey;
    }

    public static KeyBinding getItemOutlineKeyBinding() {
        return toggleItemOutlineKey;
    }

    public static KeyBinding getAutoAttackKeyBinding() {
        return toggleAutoAttackKey;
    }

    public static KeyBinding getMarkTargetKeyBinding() {
        return markTargetKey;
    }

    public static KeyBinding getUnmarkTargetKeyBinding() {
        return unmarkTargetKey;
    }

    public static KeyBinding getMarkFriendKeyBinding() {
        return markFriendKey;
    }

    public static KeyBinding getPanicKeyBinding() {
        return panicKey;
    }

    public static KeyBinding getOpenMenuKeyBinding() {
        return openMenuKey;
    }

    public static void saveConfigNow() {
        PaprikaConfig.save(captureConfig());
    }

    public static int getPlayerHighlightColor(PlayerEntity player) {
        if (player == null) {
            return 0xFFFFFF;
        }

        int rgb = resolveVisualColor(
                player,
                0.0F,
                espVisualColorMode,
                espVisualSaturationBoost,
                espVisualAnimationSpeed,
                espVisualRevision
        );
        float emissive = espVisualGlowEnabled ? 1.0F : 0.6F;
        return applyEmissive(rgb, emissive);
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
        PaprikaConfig.Data loadedConfig = PaprikaConfig.load();
        applyLoadedConfig(loadedConfig);

        toggleKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.paprika.toggle",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_V,
                "category.paprika"
        ));

        toggleNoKnockbackKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.paprika.no_knockback",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_N,
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

        togglePlayerTrailsKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.paprika.player_trails",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_L,
                "category.paprika"
        ));

        toggleItemOutlineKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.paprika.item_outline",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_Y,
                "category.paprika"
        ));

        toggleAutoAttackKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.paprika.auto_attack",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_R,
                "category.paprika"
        ));

        markTargetKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.paprika.mark_target",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_M,
                "category.paprika"
        ));

        unmarkTargetKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.paprika.unmark_target",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_U,
                "category.paprika"
        ));

        markFriendKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.paprika.mark_friend",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_F,
                "category.paprika"
        ));

        panicKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.paprika.panic",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_P,
                "category.paprika"
        ));

        openMenuKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.paprika.menu",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_RIGHT_SHIFT,
                "category.paprika"
        ));

        applyConfiguredKey(toggleKey, loadedConfig.speedToggleKey);
        applyConfiguredKey(toggleNoKnockbackKey, loadedConfig.noKnockbackKey);
        applyConfiguredKey(togglePlayerEspKey, loadedConfig.playerEspKey);
        applyConfiguredKey(togglePlayerRaysKey, loadedConfig.playerRaysKey);
        applyConfiguredKey(togglePlayerListKey, loadedConfig.playerListKey);
        applyConfiguredKey(togglePlayerTrailsKey, loadedConfig.playerTrailsKey);
        applyConfiguredKey(toggleItemOutlineKey, loadedConfig.itemOutlineKey);
        applyConfiguredKey(toggleAutoAttackKey, loadedConfig.autoAttackKey);
        applyConfiguredKey(markTargetKey, loadedConfig.markTargetKey);
        applyConfiguredKey(unmarkTargetKey, loadedConfig.unmarkTargetKey);
        applyConfiguredKey(markFriendKey, loadedConfig.markFriendKey);
        applyConfiguredKey(panicKey, loadedConfig.panicKey);
        applyConfiguredKey(openMenuKey, loadedConfig.menuKey);
        KeyBinding.updateKeysByCode();
        saveConfigNow();

        HudLayerRegistrationCallback.EVENT.register(layeredDrawer -> layeredDrawer.attachLayerAfter(
                IdentifiedLayer.SUBTITLES,
                HUD_OVERLAY_LAYER_ID,
                PaprikaClient::renderHudOverlay
        ));

        WorldRenderEvents.AFTER_ENTITIES.register(PaprikaClient::renderPlayerTrails);
        WorldRenderEvents.AFTER_ENTITIES.register(PaprikaClient::renderItemOutlines);
        WorldRenderEvents.AFTER_ENTITIES.register(PaprikaClient::renderMarkedTargetDecal);
        ClientTickEvents.END_CLIENT_TICK.register(this::onTick);
    }

    private void onTick(MinecraftClient client) {
        while (openMenuKey.wasPressed()) {
            if (!(client.currentScreen instanceof PaprikaMenuScreen)) {
                client.setScreen(new PaprikaMenuScreen(client.currentScreen));
            }
        }

        PlayerEntity player = client.player;
        if (player == null || client.world == null) {
            trailSegments.clear();
            trailStates.clear();
            return;
        }

        // Toggle
        while (toggleKey.wasPressed()) {
            setSpeedEnabled(!speedEnabled);
            player.sendMessage(
                    Text.literal("[Paprika] Sneak Movement Speed: " + (speedEnabled ? "ON" : "OFF")),
                    true
            );
        }

        while (toggleNoKnockbackKey.wasPressed()) {
            setNoKnockbackEnabled(!noKnockbackEnabled);
            player.sendMessage(
                    Text.literal("[Paprika] No Knockback: " + (noKnockbackEnabled ? "ON" : "OFF")),
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

        while (togglePlayerTrailsKey.wasPressed()) {
            setPlayerTrailsEnabled(!playerTrailsEnabled);
            player.sendMessage(
                    Text.literal("[Paprika] Player Trails: " + (playerTrailsEnabled ? "ON" : "OFF")),
                    true
            );
        }

        while (toggleItemOutlineKey.wasPressed()) {
            setItemOutlineEnabled(!itemOutlineEnabled);
            player.sendMessage(
                    Text.literal("[Paprika] Item Outline: " + (itemOutlineEnabled ? "ON" : "OFF")),
                    true
            );
        }

        while (panicKey.wasPressed()) {
            triggerPanic(client);
            return;
        }

        while (toggleAutoAttackKey.wasPressed()) {
            setAutoAttackEnabled(!autoAttackEnabled);
            player.sendMessage(
                    Text.literal("[Paprika] Auto Attack: " + (autoAttackEnabled ? "ON" : "OFF")),
                    true
            );
        }

        while (markTargetKey.wasPressed()) {
            if (markTarget(client)) {
                player.sendMessage(
                        Text.literal("[Paprika] Marked: " + markedPlayerName),
                        true
                );
            } else {
                player.sendMessage(
                        Text.literal("[Paprika] Mark failed: aim at a player"),
                        true
                );
            }
        }

        while (unmarkTargetKey.wasPressed()) {
            markedPlayerName = null;
            player.sendMessage(
                    Text.literal("[Paprika] Mark cleared"),
                    true
            );
        }

        while (markFriendKey.wasPressed()) {
            FriendMarkResult result = markFriend(client);
            if (result == FriendMarkResult.ADDED) {
                player.sendMessage(
                        Text.literal("[Paprika] Friend added: " + lastFriendMarkName),
                        true
                );
            } else if (result == FriendMarkResult.ALREADY) {
                player.sendMessage(
                        Text.literal("[Paprika] Friend already added: " + lastFriendMarkName),
                        true
                );
            } else {
                player.sendMessage(
                        Text.literal("[Paprika] Friend mark failed: aim at a player"),
                        true
                );
            }
        }

        updatePlayerTrails(client);
        tryAutoAttack(client);

        Vec3d velocity = player.getVelocity();

        // ===== KNOCKBACK =====
        if (noKnockbackEnabled && player.hurtTime > 0) {
            player.setVelocity(
                    lastVelocity.x,
                    velocity.y,
                    lastVelocity.z
            );
            return;
        }

        lastVelocity = velocity;

        boolean jumpPressed = client.options.jumpKey.isPressed();
        if (jumpBoostEnabled && jumpPressed && !jumpKeyWasPressed) {
            double currentY = velocity.y;
            if (currentY > 0.0) {
                double targetVelocity = currentY + jumpBoostHeight;
                player.setVelocity(velocity.x, targetVelocity, velocity.z);
            }
        }
        jumpKeyWasPressed = jumpPressed;

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
            fov = accessor.paprika$getFov(camera, tickDelta, true);
        }
        float renderFov = fov;
        float rayStartX = screenWidth * 0.5F;
        float rayStartY = rayOrigin == RayOrigin.CENTER
                ? screenHeight * 0.5F
                : MathHelper.clamp(screenHeight - 1.0F - rayBottomStartHeight, 1.0F, screenHeight - 1.0F);

        if (playerArmorOverlayEnabled || heldItemOverlayEnabled || distanceDisplayEnabled) {
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
        if (autoAttackEnabled && (autoAttackMode == AutoAttackMode.CIRCLE || autoAttackMode == AutoAttackMode.CIRCLE_MARK)) {
            renderAutoAttackCircle(drawContext, screenWidth, screenHeight);
        }
        if (!playerRaysEnabled) return;

        Vector3f projected = new Vector3f();
        Vector3f rayStart = new Vector3f();

        drawContext.draw(vertexConsumers -> {
            VertexConsumer lineConsumer = vertexConsumers.getBuffer(RenderLayer.getDebugQuads());
            Matrix4f matrix = drawContext.getMatrices().peek().getPositionMatrix();

            for (PlayerEntity target : client.world.getPlayers()) {
                if (target == localPlayer || target.isRemoved()) continue;

                Vec3d targetPos = target.getLerpedPos(tickDelta).add(0.0, target.getHeight() * 0.5, 0.0);
                if (!projectToIndicator(targetPos, camera, screenWidth, screenHeight, renderFov, projected)) continue;
                if (!computeRayStart(rayStartX, rayStartY, projected.x, projected.y, rayStart)) continue;

                int baseStart = resolveVisualColor(
                        target,
                        0.12F,
                        rayVisualColorMode,
                        rayVisualSaturationBoost,
                        rayVisualAnimationSpeed
                );
                int baseEnd = resolveVisualColor(
                        target,
                        0.88F,
                        rayVisualColorMode,
                        rayVisualSaturationBoost,
                        rayVisualAnimationSpeed
                );
                int coreStart = 0xFF000000 | applyEmissive(baseStart, 0.6F);
                int coreEnd = 0xFF000000 | applyEmissive(baseEnd, 0.6F);
                int glowStart = 0xFF000000 | applyEmissive(baseStart, 1.0F);
                int glowEnd = 0xFF000000 | applyEmissive(baseEnd, 1.0F);
                if (rayVisualGlowEnabled) {
                    drawThickRay(
                            matrix,
                            lineConsumer,
                            rayStart.x,
                            rayStart.y,
                            projected.x,
                            projected.y,
                            rayThickness * 2.6F,
                            withAlpha(glowStart, 0.38F * rayAlpha),
                            withAlpha(glowEnd, 0.38F * rayAlpha)
                    );
                }
                drawThickRay(
                        matrix,
                        lineConsumer,
                        rayStart.x,
                        rayStart.y,
                        projected.x,
                        projected.y,
                        rayThickness,
                        withAlpha(coreStart, rayAlpha),
                        withAlpha(coreEnd, rayAlpha)
                );
            }
        });
    }

    private static void renderAutoAttackCircle(DrawContext drawContext, int screenWidth, int screenHeight) {
        float radius = autoAttackCircleRadius;
        if (radius <= 2.0F) return;

        float centerX = screenWidth * 0.5F;
        float centerY = screenHeight * 0.5F;
        int segments = Math.max(32, Math.round(radius * 0.6F));

        drawContext.draw(vertexConsumers -> {
            VertexConsumer lineConsumer = vertexConsumers.getBuffer(RenderLayer.getDebugQuads());
            Matrix4f matrix = drawContext.getMatrices().peek().getPositionMatrix();

            for (int i = 0; i < segments; i++) {
                float t1 = (float) i / segments;
                float t2 = (float) (i + 1) / segments;
                float angle1 = TWO_PI * t1;
                float angle2 = TWO_PI * t2;
                float x1 = centerX + MathHelper.cos(angle1) * radius;
                float y1 = centerY + MathHelper.sin(angle1) * radius;
                float x2 = centerX + MathHelper.cos(angle2) * radius;
                float y2 = centerY + MathHelper.sin(angle2) * radius;

                int c1 = resolveAutoAttackCircleColor(t1);
                int c2 = resolveAutoAttackCircleColor(t2);
                drawThickRay(
                        matrix,
                        lineConsumer,
                        x1,
                        y1,
                        x2,
                        y2,
                        AUTO_ATTACK_CIRCLE_THICKNESS,
                        c1,
                        c2
                );
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

    private static boolean computeRayStart(
            float startX,
            float startY,
            float endX,
            float endY,
            Vector3f out
    ) {
        float dx = endX - startX;
        float dy = endY - startY;
        float len = MathHelper.sqrt(dx * dx + dy * dy);
        if (len <= RAY_START_CLEARANCE) return false;
        float inv = 1.0F / len;
        out.set(
                startX + dx * inv * RAY_START_CLEARANCE,
                startY + dy * inv * RAY_START_CLEARANCE,
                0.0F
        );
        return true;
    }

    private static void updatePlayerTrails(MinecraftClient client) {
        if (!playerTrailsEnabled || client.world == null) {
            trailSegments.clear();
            trailStates.clear();
            return;
        }

        double now = currentTimeSeconds();
        double lifetime = trailLifetimeSeconds;
        if (!trailSegments.isEmpty()) {
            trailSegments.removeIf(segment -> now - segment.spawnTime() > lifetime);
        }

        Set<UUID> seen = new HashSet<>();
        PlayerEntity localPlayer = client.player;
        for (PlayerEntity target : client.world.getPlayers()) {
            if (target == null || target.isRemoved()) continue;
            if (target == localPlayer) {
                if (!trailSelfEnabled) continue;
            } else if (!trailOthersEnabled) {
                continue;
            }
            UUID uuid = target.getUuid();
            seen.add(uuid);

            Vec3d backDir = computeTrailBackDirection(target);
            Vec3d currentPos = computeTrailOrigin(target, backDir);
            TrailState state = trailStates.get(uuid);
            if (state != null && state.lastPos() != null) {
                if (state.lastPos().squaredDistanceTo(currentPos) > 0.0004) {
                    int color = resolveTrailColor(target);
                    trailSegments.add(new TrailSegment(state.lastPos(), currentPos, backDir, color, now));
                }
            }

            trailStates.put(uuid, new TrailState(currentPos, backDir));
        }

        if (!trailStates.isEmpty()) {
            trailStates.keySet().removeIf(uuid -> !seen.contains(uuid));
        }
    }

    private static void renderPlayerTrails(WorldRenderContext context) {
        if (!playerTrailsEnabled || trailSegments.isEmpty()) return;

        Matrix4f matrix = context.matrixStack().peek().getPositionMatrix();
        Vec3d cameraPos = context.camera().getPos();
        float width = switch (trailType) {
            case THIN_LINE -> 0.02F;
            case FLOATING_LINE -> 0.08F;
            case STRIP -> 0.0F;
        };

        context.matrixStack().push();
        context.matrixStack().translate(-cameraPos.x, -cameraPos.y, -cameraPos.z);
        matrix = context.matrixStack().peek().getPositionMatrix();

        VertexConsumer consumer = context.consumers().getBuffer(RenderLayer.getDebugQuads());
        for (TrailSegment segment : trailSegments) {
            Vec3d start = segment.start();
            Vec3d end = segment.end();
            int color = segment.color();

            if (trailType == TrailType.STRIP) {
                Vec3d backDir = segment.backDirection();
                if (backDir.lengthSquared() < 0.0001) {
                    backDir = new Vec3d(0.0, 0.0, 1.0);
                }
                Vec3d up = new Vec3d(0.0, trailStripeHeight, 0.0);
                Vec3d s = start;
                Vec3d e = end;
                addQuad(consumer, matrix, s, e, e.add(up), s.add(up), color);
                addQuad(consumer, matrix, s.add(up), e.add(up), e, s, color);
                continue;
            }

            Vec3d dir = end.subtract(start);
            double lenSq = dir.lengthSquared();
            if (lenSq < 0.000001) continue;

            Vec3d mid = start.add(end).multiply(0.5);
            Vec3d camDir = cameraPos.subtract(mid);
            Vec3d right = dir.crossProduct(camDir);
            if (right.lengthSquared() < 0.000001) continue;
            right = right.normalize().multiply(width * 0.5);

            Vec3d v1 = start.subtract(right);
            Vec3d v2 = start.add(right);
            Vec3d v3 = end.add(right);
            Vec3d v4 = end.subtract(right);
            addQuad(consumer, matrix, v1, v2, v3, v4, color);
            addQuad(consumer, matrix, v4, v3, v2, v1, color);
        }

        context.matrixStack().pop();
    }

    private static void renderItemOutlines(WorldRenderContext context) {
        if (!itemOutlineEnabled) return;
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null || client.world == null) return;

        Vec3d cameraPos = context.camera().getPos();
        double range = 512.0;
        double rangeSq = range * range;

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();

        context.matrixStack().push();
        context.matrixStack().translate(-cameraPos.x, -cameraPos.y, -cameraPos.z);
        Matrix4f matrix = context.matrixStack().peek().getPositionMatrix();

        float thickness = Math.max(0.5F, itemOutlineThickness);
        float width = Math.max(0.004F, thickness * 0.02F);
        VertexConsumer coreConsumer = context.consumers().getBuffer(ITEM_OUTLINE_QUADS);

        Iterable<Entity> entities = client.world.getEntities();
        if (entities == null) {
            context.matrixStack().pop();
            RenderSystem.depthMask(true);
            RenderSystem.enableDepthTest();
            return;
        }

        for (Entity entity : entities) {
            if (!(entity instanceof ItemEntity item)) continue;
            if (item.isRemoved()) continue;
            if (item.getStack().isEmpty()) continue;
            if (item.squaredDistanceTo(cameraPos) > rangeSq) continue;
            if (!isItemOutlineAllowed(item)) continue;

            Box box = item.getBoundingBox();
            if (box == null) continue;

            if (itemOutlineGlowEnabled) {
                addItemOutlineBox(coreConsumer, matrix, cameraPos, box, item, width, 1.0F, itemOutlineAlpha * 0.38F);
            }

            addItemOutlineBox(coreConsumer, matrix, cameraPos, box, item, width, 0.6F, itemOutlineAlpha);
        }

        context.matrixStack().pop();

    }

    private static void renderMarkedTargetDecal(WorldRenderContext context) {
        if (markedPlayerName == null || markedPlayerName.isBlank()) return;
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null || client.world == null) return;
        PlayerEntity target = findMarkedPlayer(client);
        if (target == null) return;

        float tickDelta = client.getRenderTickCounter() != null ? client.getRenderTickCounter().getTickDelta(false) : 0.0F;
        Vec3d cameraPos = context.camera().getPos();
        Vec3d targetPos = target.getLerpedPos(tickDelta).add(0.0, target.getHeight() * 0.6, 0.0);

        float size = Math.max(0.6F, target.getHeight() * 0.5F);
        float radius = Math.max(0.5F, target.getWidth() * 1.2F);
        float angle = visualTime(1.0F) * 90.0F;

        int color = 0xFF000000 | applyEmissive(0x4CB1FF, 1.0F);
        VertexConsumer consumer = context.consumers().getBuffer(RenderLayer.getDebugQuads());

        renderMarkBillboard(context, cameraPos, targetPos, angle, radius, size, color, consumer);
        renderMarkBillboard(context, cameraPos, targetPos, angle + 180.0F, radius, size, color, consumer);
    }

    private static void renderMarkBillboard(
            WorldRenderContext context,
            Vec3d cameraPos,
            Vec3d targetPos,
            float orbitAngle,
            float radius,
            float size,
            int color,
            VertexConsumer consumer
    ) {
        float half = size * 0.5F;
        context.matrixStack().push();
        context.matrixStack().translate(targetPos.x - cameraPos.x, targetPos.y - cameraPos.y, targetPos.z - cameraPos.z);
        context.matrixStack().multiply(RotationAxis.POSITIVE_Y.rotationDegrees(orbitAngle));
        context.matrixStack().translate(radius, 0.0F, 0.0F);
        context.matrixStack().multiply(context.camera().getRotation());

        Matrix4f matrix = context.matrixStack().peek().getPositionMatrix();
        addQuad(consumer, matrix,
                new Vec3d(-half, -half, 0.0),
                new Vec3d(half, -half, 0.0),
                new Vec3d(half, half, 0.0),
                new Vec3d(-half, half, 0.0),
                color
        );
        addQuad(consumer, matrix,
                new Vec3d(-half, half, 0.0),
                new Vec3d(half, half, 0.0),
                new Vec3d(half, -half, 0.0),
                new Vec3d(-half, -half, 0.0),
                color
        );
        context.matrixStack().pop();
    }

    private static void addItemOutlineBox(VertexConsumer consumer, Matrix4f matrix, Vec3d cameraPos, Box box, ItemEntity item, float width, float emissive, float alpha) {
        double minX = box.minX;
        double minY = box.minY;
        double minZ = box.minZ;
        double maxX = box.maxX;
        double maxY = box.maxY;
        double maxZ = box.maxZ;

        float[] offsets = new float[]{0.08F, 0.14F, 0.2F, 0.26F, 0.32F, 0.38F, 0.44F, 0.5F, 0.58F, 0.66F, 0.74F, 0.82F};

        int idx = 0;
        addItemOutlineLine(consumer, matrix, cameraPos, width, minX, minY, minZ, maxX, minY, minZ, resolveItemOutlineColor(item, offsets[idx++], emissive, alpha));
        addItemOutlineLine(consumer, matrix, cameraPos, width, maxX, minY, minZ, maxX, minY, maxZ, resolveItemOutlineColor(item, offsets[idx++], emissive, alpha));
        addItemOutlineLine(consumer, matrix, cameraPos, width, maxX, minY, maxZ, minX, minY, maxZ, resolveItemOutlineColor(item, offsets[idx++], emissive, alpha));
        addItemOutlineLine(consumer, matrix, cameraPos, width, minX, minY, maxZ, minX, minY, minZ, resolveItemOutlineColor(item, offsets[idx++], emissive, alpha));

        addItemOutlineLine(consumer, matrix, cameraPos, width, minX, maxY, minZ, maxX, maxY, minZ, resolveItemOutlineColor(item, offsets[idx++], emissive, alpha));
        addItemOutlineLine(consumer, matrix, cameraPos, width, maxX, maxY, minZ, maxX, maxY, maxZ, resolveItemOutlineColor(item, offsets[idx++], emissive, alpha));
        addItemOutlineLine(consumer, matrix, cameraPos, width, maxX, maxY, maxZ, minX, maxY, maxZ, resolveItemOutlineColor(item, offsets[idx++], emissive, alpha));
        addItemOutlineLine(consumer, matrix, cameraPos, width, minX, maxY, maxZ, minX, maxY, minZ, resolveItemOutlineColor(item, offsets[idx++], emissive, alpha));

        addItemOutlineLine(consumer, matrix, cameraPos, width, minX, minY, minZ, minX, maxY, minZ, resolveItemOutlineColor(item, offsets[idx++], emissive, alpha));
        addItemOutlineLine(consumer, matrix, cameraPos, width, maxX, minY, minZ, maxX, maxY, minZ, resolveItemOutlineColor(item, offsets[idx++], emissive, alpha));
        addItemOutlineLine(consumer, matrix, cameraPos, width, maxX, minY, maxZ, maxX, maxY, maxZ, resolveItemOutlineColor(item, offsets[idx++], emissive, alpha));
        addItemOutlineLine(consumer, matrix, cameraPos, width, minX, minY, maxZ, minX, maxY, maxZ, resolveItemOutlineColor(item, offsets[idx], emissive, alpha));
    }

    private static void addItemOutlineLine(VertexConsumer consumer, Matrix4f matrix, Vec3d cameraPos, float width, double x1, double y1, double z1, double x2, double y2, double z2, int color) {
        Vec3d start = new Vec3d(x1, y1, z1);
        Vec3d end = new Vec3d(x2, y2, z2);
        addThickLine(consumer, matrix, cameraPos, start, end, width, color);
    }

    private static void addThickLine(VertexConsumer consumer, Matrix4f matrix, Vec3d cameraPos, Vec3d start, Vec3d end, float width, int color) {
        Vec3d dir = end.subtract(start);
        double lenSq = dir.lengthSquared();
        if (lenSq < 0.000001) return;

        Vec3d mid = start.add(end).multiply(0.5);
        Vec3d camDir = cameraPos.subtract(mid);
        Vec3d right = dir.crossProduct(camDir);
        if (right.lengthSquared() < 0.000001) {
            right = dir.crossProduct(new Vec3d(0.0, 1.0, 0.0));
            if (right.lengthSquared() < 0.000001) {
                right = new Vec3d(1.0, 0.0, 0.0);
            }
        }
        right = right.normalize().multiply(width * 0.5);

        Vec3d v1 = start.subtract(right);
        Vec3d v2 = start.add(right);
        Vec3d v3 = end.add(right);
        Vec3d v4 = end.subtract(right);
        addQuad(consumer, matrix, v1, v2, v3, v4, color);
        addQuad(consumer, matrix, v4, v3, v2, v1, color);
    }

    private static int resolveItemOutlineColor(ItemEntity item, float offset, float emissive, float alpha) {
        int seed = getItemOutlineSeed(item);
        int rgb = switch (itemOutlineColorMode) {
            case SOLID -> itemOutlineSolidColor;
            case ITEM_AVERAGE -> getAverageItemColor(item.getStack(), seed);
            case NICK, GRADIENT, NICK_GRADIENT, RAINBOW -> resolveVisualColor(
                    seedBaseColor(seed),
                    seed,
                    offset,
                    itemOutlineColorMode.getVisualColorMode(),
                    itemOutlineSaturationBoost,
                    itemOutlineAnimationSpeed,
                    itemOutlineVisualRevision
            );
        };
        int argb = 0xFF000000 | applyEmissive(rgb, emissive);
        return withAlpha(argb, alpha);
    }

    private static int getAverageItemColor(ItemStack stack, int seed) {
        Identifier id = Registries.ITEM.getId(stack.getItem());
        if (id == null) {
            return seedBaseColor(seed);
        }
        String key = id.toString();
        Integer cached = itemAverageColorCache.get(key);
        if (cached != null) {
            return cached;
        }
        int computed = computeAverageItemColor(stack, seed);
        if (computed >= 0) {
            itemAverageColorCache.put(key, computed);
            return computed;
        }
        return seedBaseColor(seed);
    }

    private static int computeAverageItemColor(ItemStack stack, int seed) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null) {
            return -1;
        }
        try {
            ItemModelManager modelManager = client.getItemModelManager();
            if (modelManager == null) return -1;
            ItemRenderState renderState = new ItemRenderState();
            modelManager.update(renderState, stack, ModelTransformationMode.GUI, client.world, null, seed);
            Sprite sprite = renderState.getParticleSprite(Random.create(seed));
            if (sprite == null) return -1;
            NativeImage image = resolveSpriteImage(sprite);
            if (image == null) return -1;
            return averageNativeImage(image);
        } catch (Exception ignored) {
        }
        return -1;
    }

    private static NativeImage resolveSpriteImage(Sprite sprite) {
        if (sprite == null) return null;
        SpriteContents contents = sprite.getContents();
        if (contents == null) return null;
        try {
            var imageField = SpriteContents.class.getDeclaredField("image");
            imageField.setAccessible(true);
            Object value = imageField.get(contents);
            if (value instanceof NativeImage image) {
                return image;
            }
        } catch (Exception ignored) {
        }
        try {
            var mipmapField = SpriteContents.class.getDeclaredField("mipmapLevelsImages");
            mipmapField.setAccessible(true);
            Object value = mipmapField.get(contents);
            if (value instanceof NativeImage[] images && images.length > 0 && images[0] != null) {
                return images[0];
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    private static int averageNativeImage(NativeImage image) {
        try {
            int width = image.getWidth();
            int height = image.getHeight();
            if (width <= 0 || height <= 0) return 0;
            int step = Math.max(1, Math.min(width, height) / 16);

            long sumR = 0;
            long sumG = 0;
            long sumB = 0;
            long count = 0;

            for (int y = 0; y < height; y += step) {
                for (int x = 0; x < width; x += step) {
                    int color = image.getColorArgb(x, y);
                    int a = (color >>> 24) & 0xFF;
                    if (a == 0) continue;
                    int r = (color >>> 16) & 0xFF;
                    int g = (color >>> 8) & 0xFF;
                    int b = color & 0xFF;
                    sumR += r;
                    sumG += g;
                    sumB += b;
                    count++;
                }
            }
            if (count == 0) return 0;
            int r = (int) (sumR / count);
            int g = (int) (sumG / count);
            int b = (int) (sumB / count);
            return (r << 16) | (g << 8) | b;
        } catch (Exception ignored) {
            return 0;
        }
    }

    private static int getItemOutlineSeed(ItemEntity item) {
        Identifier id = Registries.ITEM.getId(item.getStack().getItem());
        if (id != null) {
            return id.hashCode();
        }
        return item.getId();
    }

    private static boolean isItemOutlineAllowed(ItemEntity item) {
        if (itemOutlineMode == ItemOutlineMode.ALL) return true;
        Identifier id = Registries.ITEM.getId(item.getStack().getItem());
        if (id == null) {
            return itemOutlineMode == ItemOutlineMode.BLACKLIST;
        }
        String key = id.toString().toLowerCase(Locale.ROOT);
        boolean listed = itemFilterIds.containsKey(key);
        return itemOutlineMode == ItemOutlineMode.WHITELIST ? listed : !listed;
    }

    private static void addQuad(VertexConsumer consumer, Matrix4f matrix, Vec3d v1, Vec3d v2, Vec3d v3, Vec3d v4, int color) {
        consumer.vertex(matrix, (float) v1.x, (float) v1.y, (float) v1.z).color(color);
        consumer.vertex(matrix, (float) v2.x, (float) v2.y, (float) v2.z).color(color);
        consumer.vertex(matrix, (float) v3.x, (float) v3.y, (float) v3.z).color(color);
        consumer.vertex(matrix, (float) v4.x, (float) v4.y, (float) v4.z).color(color);
    }

    private static boolean markTarget(MinecraftClient client) {
        if (client == null || client.player == null || client.world == null || client.gameRenderer == null) return false;
        PlayerEntity target = findCrosshairTarget(client);
        if (target == null) {
            Camera camera = client.gameRenderer.getCamera();
            if (camera == null || !camera.isReady()) return false;
            int width = client.getWindow().getScaledWidth();
            int height = client.getWindow().getScaledHeight();
            if (width <= 0 || height <= 0) return false;
            float tickDelta = client.getRenderTickCounter() != null ? client.getRenderTickCounter().getTickDelta(false) : 0.0F;
            float fov = client.options.getFov().getValue().floatValue();
            if (client.gameRenderer instanceof GameRendererAccessor accessor) {
                fov = accessor.paprika$getFov(camera, tickDelta, true);
            }
            target = findTargetInCircle(client, camera, tickDelta, fov, width, height, MARK_AIM_RADIUS);
        }
        if (target == null) return false;
        markedPlayerName = target.getGameProfile().getName();
        return true;
    }

    private static FriendMarkResult markFriend(MinecraftClient client) {
        if (client == null || client.player == null || client.world == null || client.gameRenderer == null) {
            lastFriendMarkName = null;
            return FriendMarkResult.FAILED;
        }
        PlayerEntity target = findCrosshairTarget(client);
        if (target == null) {
            Camera camera = client.gameRenderer.getCamera();
            if (camera == null || !camera.isReady()) {
                lastFriendMarkName = null;
                return FriendMarkResult.FAILED;
            }
            int width = client.getWindow().getScaledWidth();
            int height = client.getWindow().getScaledHeight();
            if (width <= 0 || height <= 0) {
                lastFriendMarkName = null;
                return FriendMarkResult.FAILED;
            }
            float tickDelta = client.getRenderTickCounter() != null ? client.getRenderTickCounter().getTickDelta(false) : 0.0F;
            float fov = client.options.getFov().getValue().floatValue();
            if (client.gameRenderer instanceof GameRendererAccessor accessor) {
                fov = accessor.paprika$getFov(camera, tickDelta, true);
            }
            target = findTargetInCircle(client, camera, tickDelta, fov, width, height, MARK_AIM_RADIUS);
        }
        if (target == null) {
            lastFriendMarkName = null;
            return FriendMarkResult.FAILED;
        }

        String name = target.getGameProfile().getName();
        lastFriendMarkName = name;
        return addFriendName(name) ? FriendMarkResult.ADDED : FriendMarkResult.ALREADY;
    }

    private static void triggerPanic(MinecraftClient client) {
        panicActive = true;
        speedEnabled = false;
        noKnockbackEnabled = false;
        playerEspEnabled = false;
        playerArmorOverlayEnabled = false;
        playerRaysEnabled = false;
        playerListEnabled = false;
        playerTrailsEnabled = false;
        autoAttackEnabled = false;
        autoAttackAimEnabled = false;
        itemOutlineEnabled = false;
        targetHealthOverlayEnabled = false;
        distanceDisplayEnabled = false;
        heldItemOverlayEnabled = false;
        customSkyEnabled = false;
        hideHandsWithItemEnabled = false;
        handItemFlipEnabled = false;
        handItemOrientation = HandItemOrientation.DEFAULT;
        handFovScale = DEFAULT_HAND_FOV_SCALE;
        handOffsetX = 0.0F;
        handOffsetY = 0.0F;
        jumpBoostEnabled = false;

        trailSegments.clear();
        trailStates.clear();
        markedPlayerName = null;
        lastAutoAttackTime = 0.0;

        if (client != null) {
            client.setScreen(null);
        }

        clearAllKeybinds();
        saveConfigNow();
    }

    private static void clearAllKeybinds() {
        clearKey(toggleKey);
        clearKey(toggleNoKnockbackKey);
        clearKey(togglePlayerEspKey);
        clearKey(togglePlayerRaysKey);
        clearKey(togglePlayerListKey);
        clearKey(togglePlayerTrailsKey);
        clearKey(toggleItemOutlineKey);
        clearKey(toggleAutoAttackKey);
        clearKey(markTargetKey);
        clearKey(unmarkTargetKey);
        clearKey(markFriendKey);
        clearKey(panicKey);
        clearKey(openMenuKey);
        KeyBinding.updateKeysByCode();
    }

    private static void clearKey(KeyBinding keyBinding) {
        if (keyBinding == null) return;
        keyBinding.setBoundKey(InputUtil.UNKNOWN_KEY);
    }

    private static void tryAutoAttack(MinecraftClient client) {
        if (!autoAttackEnabled) return;
        if (client == null || client.player == null || client.world == null || client.interactionManager == null) return;
        if (client.currentScreen != null) return;

        Camera camera = client.gameRenderer != null ? client.gameRenderer.getCamera() : null;
        if (camera == null || !camera.isReady()) return;

        int width = client.getWindow().getScaledWidth();
        int height = client.getWindow().getScaledHeight();
        if (width <= 0 || height <= 0) return;

        float tickDelta = client.getRenderTickCounter() != null ? client.getRenderTickCounter().getTickDelta(false) : 0.0F;
        float fov = client.options.getFov().getValue().floatValue();
        if (client.gameRenderer instanceof GameRendererAccessor accessor) {
            fov = accessor.paprika$getFov(camera, tickDelta, true);
        }

        PlayerEntity target = selectAutoAttackTarget(client, camera, tickDelta, fov, width, height);
        if (target == null) return;

        double reach = getAttackReach(client.player);
        if (client.player.squaredDistanceTo(target) > reach * reach) return;
        if (autoAttackRequireLineOfSight && !client.player.canSee(target)) return;

        if (autoAttackAimEnabled) {
            applyAutoAttackAim(client.player, target, tickDelta);
        }

        double now = currentTimeSeconds();
        double interval = 1.0 / Math.max(0.1, autoAttackRate);
        if (now - lastAutoAttackTime < interval) return;

        client.interactionManager.attackEntity(client.player, target);
        client.player.swingHand(Hand.MAIN_HAND);
        lastAutoAttackTime = now;
    }

    private static void applyAutoAttackAim(PlayerEntity player, PlayerEntity target, float tickDelta) {
        if (player == null || target == null) return;
        Vec3d eyePos = player.getEyePos();
        Vec3d targetPos = target.getLerpedPos(tickDelta).add(0.0, target.getHeight() * 0.5, 0.0);
        Vec3d diff = targetPos.subtract(eyePos);
        double dx = diff.x;
        double dy = diff.y;
        double dz = diff.z;
        double horizontal = MathHelper.sqrt((float) (dx * dx + dz * dz));

        float targetYaw = (float) (MathHelper.atan2(dz, dx) * 57.295776) - 90.0F;
        float targetPitch = (float) (-(MathHelper.atan2(dy, horizontal) * 57.295776));

        float yaw = player.getYaw();
        float pitch = player.getPitch();
        float yawDelta = MathHelper.wrapDegrees(targetYaw - yaw);
        float pitchDelta = targetPitch - pitch;

        float nextYaw = yaw + yawDelta * AUTO_ATTACK_AIM_SMOOTHING;
        float nextPitch = pitch + pitchDelta * AUTO_ATTACK_AIM_SMOOTHING;
        player.setYaw(nextYaw);
        player.setPitch(MathHelper.clamp(nextPitch, -90.0F, 90.0F));
    }

    private static PlayerEntity selectAutoAttackTarget(
            MinecraftClient client,
            Camera camera,
            float tickDelta,
            float fov,
            int width,
            int height
    ) {
        return switch (autoAttackMode) {
            case MARK_ONLY -> findMarkedPlayer(client);
            case CIRCLE_MARK -> {
                PlayerEntity marked = findMarkedPlayer(client);
                if (marked == null) {
                    yield null;
                }
                yield isPlayerInCircle(marked, camera, tickDelta, fov, width, height, autoAttackCircleRadius) ? marked : null;
            }
            case ALL_NEARBY -> findNearestPlayer(client, getAttackReach(client.player), autoAttackRequireLineOfSight);
            case CIRCLE -> findTargetInCircle(client, camera, tickDelta, fov, width, height, autoAttackCircleRadius);
        };
    }

    private static PlayerEntity findTargetInCircle(
            MinecraftClient client,
            Camera camera,
            float tickDelta,
            float fov,
            int width,
            int height,
            float radius
    ) {
        if (client.world == null || client.player == null) return null;
        float centerX = width * 0.5F;
        float centerY = height * 0.5F;
        float radiusSq = radius * radius;
        PlayerEntity best = null;
        float bestDistSq = Float.MAX_VALUE;
        Vector3f projected = new Vector3f();

        for (PlayerEntity target : client.world.getPlayers()) {
            if (target == null || target.isRemoved() || target == client.player) continue;
            Vec3d pos = target.getLerpedPos(tickDelta).add(0.0, target.getHeight() * 0.5, 0.0);
            if (!projectToScreen(pos, camera, width, height, fov, projected)) continue;

            float dx = projected.x - centerX;
            float dy = projected.y - centerY;
            float distSq = dx * dx + dy * dy;
            if (distSq > radiusSq) continue;
            if (distSq < bestDistSq) {
                bestDistSq = distSq;
                best = target;
            }
        }
        return best;
    }

    private static PlayerEntity findCrosshairTarget(MinecraftClient client) {
        HitResult hit = client.crosshairTarget;
        if (hit instanceof EntityHitResult entityHit) {
            if (entityHit.getEntity() instanceof PlayerEntity player) {
                if (!player.isRemoved() && player != client.player) {
                    return player;
                }
            }
        }
        return null;
    }

    private static boolean isPlayerInCircle(
            PlayerEntity target,
            Camera camera,
            float tickDelta,
            float fov,
            int width,
            int height,
            float radius
    ) {
        if (target == null || target.isRemoved()) return false;
        Vector3f projected = new Vector3f();
        Vec3d pos = target.getLerpedPos(tickDelta).add(0.0, target.getHeight() * 0.5, 0.0);
        if (!projectToScreen(pos, camera, width, height, fov, projected)) return false;
        float dx = projected.x - width * 0.5F;
        float dy = projected.y - height * 0.5F;
        return dx * dx + dy * dy <= radius * radius;
    }

    private static PlayerEntity findNearestPlayer(MinecraftClient client, double maxDistance, boolean requireSight) {
        if (client.world == null || client.player == null) return null;
        PlayerEntity best = null;
        double bestDistSq = Double.MAX_VALUE;
        double maxDistSq = maxDistance * maxDistance;

        for (PlayerEntity target : client.world.getPlayers()) {
            if (target == null || target.isRemoved() || target == client.player) continue;
            double distSq = client.player.squaredDistanceTo(target);
            if (distSq > maxDistSq) continue;
            if (requireSight && !client.player.canSee(target)) continue;
            if (distSq < bestDistSq) {
                bestDistSq = distSq;
                best = target;
            }
        }
        return best;
    }

    private static PlayerEntity findMarkedPlayer(MinecraftClient client) {
        if (client.world == null || markedPlayerName == null || markedPlayerName.isBlank()) return null;
        for (PlayerEntity target : client.world.getPlayers()) {
            if (target == null || target.isRemoved()) continue;
            if (target.getGameProfile().getName().equalsIgnoreCase(markedPlayerName)) {
                return target;
            }
        }
        return null;
    }

    private static double getAttackReach(PlayerEntity player) {
        if (player == null) return 3.0;
        double reach = player.getAttributeValue(EntityAttributes.ENTITY_INTERACTION_RANGE);
        double base = reach > 0.0 ? reach : 3.0;
        return Math.max(base, autoAttackMaxDistance);
    }

    private static Vec3d computeTrailOrigin(PlayerEntity player, Vec3d backDir) {
        Vec3d basePos = player.getPos();
        Vec3d back = backDir.lengthSquared() < 0.0001 ? new Vec3d(0.0, 0.0, 1.0) : backDir.normalize();
        double backOffset = player.getWidth() * 0.5 + 0.06;
        if (trailOrigin == TrailOrigin.HEAD) {
            return basePos.add(0.0, player.getStandingEyeHeight(), 0.0).add(back.multiply(backOffset));
        }

        double bodyHeight = player.getHeight() * 0.6;
        Vec3d spine = basePos.add(0.0, bodyHeight, 0.0);
        return spine.add(back.multiply(backOffset));
    }

    private static Vec3d computeTrailBackDirection(PlayerEntity player) {
        float yawRad = (float) Math.toRadians(player.getYaw());
        return new Vec3d(MathHelper.sin(yawRad), 0.0, -MathHelper.cos(yawRad));
    }

    private static int resolveTrailColor(PlayerEntity player) {
        int baseColor = getPlayerBaseColor(player);
        int rgb = switch (trailColorMode) {
            case NICK -> baseColor;
            case FIXED -> trailFixedColor;
            case GRADIENT -> toRainbowColor(player.getId(), 0.15F, 1.2F, trailGradientSpeed);
            case NICK_GRADIENT -> toNickGradientColor(baseColor, player.getId(), 0.55F, DEFAULT_STYLE_SATURATION, trailGradientSpeed);
        };
        return withAlpha(0xFF000000 | applyEmissive(rgb, 0.8F), trailAlpha);
    }

    private static double currentTimeSeconds() {
        return System.nanoTime() / 1_000_000_000.0;
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

            List<ItemStack> armorStacks = null;
            boolean hasArmor = false;
            if (playerArmorOverlayEnabled) {
                List<ItemStack> stacks = new ArrayList<>(4);
                ItemStack head = target.getEquippedStack(EquipmentSlot.HEAD);
                ItemStack chest = target.getEquippedStack(EquipmentSlot.CHEST);
                ItemStack legs = target.getEquippedStack(EquipmentSlot.LEGS);
                ItemStack feet = target.getEquippedStack(EquipmentSlot.FEET);
                if (!head.isEmpty()) stacks.add(head);
                if (!chest.isEmpty()) stacks.add(chest);
                if (!legs.isEmpty()) stacks.add(legs);
                if (!feet.isEmpty()) stacks.add(feet);
                if (!stacks.isEmpty()) {
                    armorStacks = stacks;
                    hasArmor = true;
                }
            }

            List<ItemStack> heldStacks = null;
            boolean hasHeld = false;
            if (heldItemOverlayEnabled) {
                List<ItemStack> stacks = new ArrayList<>(2);
                ItemStack mainHand = target.getMainHandStack();
                ItemStack offHand = target.getOffHandStack();
                if (!mainHand.isEmpty()) stacks.add(mainHand);
                if (!offHand.isEmpty()) stacks.add(offHand);
                if (!stacks.isEmpty()) {
                    heldStacks = stacks;
                    hasHeld = true;
                }
            }

            boolean hasDistance = distanceDisplayEnabled && client.textRenderer != null;
            String distanceText = null;
            if (hasDistance) {
                int meters = Math.max(0, Math.round((float) localPlayer.getPos().distanceTo(target.getPos())));
                distanceText = meters + "m";
            }

            boolean rayArmor = hasArmor && armorAnchorMode == OverlayAnchorMode.RAY_MIDDLE;
            boolean aboveArmor = hasArmor && armorAnchorMode == OverlayAnchorMode.ABOVE_PLAYER;
            boolean rayHeld = hasHeld && heldItemAnchorMode == OverlayAnchorMode.RAY_MIDDLE;
            boolean aboveHeld = hasHeld && heldItemAnchorMode == OverlayAnchorMode.ABOVE_PLAYER;
            boolean rayDistance = hasDistance && distanceAnchorMode == OverlayAnchorMode.RAY_MIDDLE;
            boolean aboveDistance = hasDistance && distanceAnchorMode == OverlayAnchorMode.ABOVE_PLAYER;

            int armorColor = 0;
            if (rayArmor || aboveArmor) {
                armorColor = 0xFF000000 | resolveVisualColor(
                        target,
                        0.24F,
                        armorVisualColorMode,
                        armorVisualSaturationBoost,
                        armorVisualAnimationSpeed,
                        armorVisualRevision
                );
            }
            int heldColor = 0;
            if (rayHeld || aboveHeld) {
                heldColor = 0xFF000000 | resolveVisualColor(
                        target,
                        0.34F,
                        heldItemVisualColorMode,
                        heldItemVisualSaturationBoost,
                        heldItemVisualAnimationSpeed,
                        heldItemVisualRevision
                );
            }
            int distanceColor = 0;
            if (rayDistance || aboveDistance) {
                distanceColor = 0xFF000000 | resolveVisualColor(
                        target,
                        0.18F,
                        distanceVisualColorMode,
                        distanceVisualSaturationBoost,
                        distanceVisualAnimationSpeed,
                        distanceVisualRevision
                );
            }

            if (rayArmor || rayHeld || rayDistance) {
                resolveOverlayAnchor(OverlayAnchorMode.RAY_MIDDLE, rayStartX, rayStartY, centerProjected, aboveProjected, anchor);
                renderOverlayGroup(
                        drawContext,
                        client,
                        anchor,
                        screenWidth,
                        screenHeight,
                        rayArmor ? armorStacks : null,
                        armorOverlayScale,
                        armorColor,
                        armorVisualGlowEnabled,
                        armorAlpha,
                        rayHeld ? heldStacks : null,
                        heldItemOverlayScale,
                        heldColor,
                        heldItemVisualGlowEnabled,
                        heldItemAlpha,
                        rayDistance ? distanceText : null,
                        distanceTextScale,
                        distanceColor,
                        distanceVisualGlowEnabled,
                        distanceAlpha,
                        false
                );
            }

            if (aboveArmor || aboveHeld || aboveDistance) {
                resolveOverlayAnchor(OverlayAnchorMode.ABOVE_PLAYER, rayStartX, rayStartY, centerProjected, aboveProjected, anchor);
                renderOverlayGroup(
                        drawContext,
                        client,
                        anchor,
                        screenWidth,
                        screenHeight,
                        aboveArmor ? armorStacks : null,
                        armorOverlayScale,
                        armorColor,
                        armorVisualGlowEnabled,
                        armorAlpha,
                        aboveHeld ? heldStacks : null,
                        heldItemOverlayScale,
                        heldColor,
                        heldItemVisualGlowEnabled,
                        heldItemAlpha,
                        aboveDistance ? distanceText : null,
                        distanceTextScale,
                        distanceColor,
                        distanceVisualGlowEnabled,
                        distanceAlpha,
                        true
                );
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

    private static void renderOverlayGroup(
            DrawContext drawContext,
            MinecraftClient client,
            Vector3f anchor,
            int screenWidth,
            int screenHeight,
            List<ItemStack> armorStacks,
            float armorScale,
            int armorColor,
            boolean armorGlow,
            float armorAlpha,
            List<ItemStack> heldStacks,
            float heldScale,
            int heldColor,
            boolean heldGlow,
            float heldAlpha,
            String distanceText,
            float distanceScale,
            int distanceColor,
            boolean distanceGlow,
            float distanceAlpha,
            boolean anchorAbove
    ) {
        boolean showDistance = distanceText != null && client.textRenderer != null;
        boolean showArmor = armorStacks != null && !armorStacks.isEmpty();
        boolean showHeld = heldStacks != null && !heldStacks.isEmpty();
        int blocks = 0;
        if (showDistance) blocks++;
        if (showArmor) blocks++;
        if (showHeld) blocks++;
        if (blocks == 0) return;

        int distanceWidth = showDistance ? Math.max(1, Math.round(client.textRenderer.getWidth(distanceText) * distanceScale)) : 0;
        int distanceHeight = showDistance ? Math.max(1, Math.round(client.textRenderer.fontHeight * distanceScale)) : 0;
        int armorWidth = showArmor ? getItemRowWidth(armorStacks, armorScale) : 0;
        int armorHeight = showArmor ? getItemRowHeight(armorScale) : 0;
        int heldWidth = showHeld ? getItemRowWidth(heldStacks, heldScale) : 0;
        int heldHeight = showHeld ? getItemRowHeight(heldScale) : 0;

        int groupWidth = Math.max(1, Math.max(distanceWidth, Math.max(armorWidth, heldWidth)));
        int groupHeight = distanceHeight + armorHeight + heldHeight + (blocks - 1) * OVERLAY_GROUP_GAP;

        int rawX = Math.round(anchor.x) - groupWidth / 2;
        int rawY = anchorAbove ? Math.round(anchor.y) - groupHeight : Math.round(anchor.y) - groupHeight / 2;
        int maxX = Math.max(2, screenWidth - groupWidth - 2);
        int maxY = Math.max(2, screenHeight - groupHeight - 2);
        int startX = MathHelper.clamp(rawX, 2, maxX);
        int startY = MathHelper.clamp(rawY, 2, maxY);

        int currentY = startY;
        if (showDistance) {
            int textX = startX + (groupWidth - distanceWidth) / 2;
            renderDistanceTextAt(
                    drawContext,
                    client,
                    distanceText,
                    distanceScale,
                    textX,
                    currentY,
                    distanceColor,
                    distanceGlow,
                    distanceAlpha
            );
            currentY += distanceHeight + OVERLAY_GROUP_GAP;
        }
        if (showArmor) {
            int rowX = startX + (groupWidth - armorWidth) / 2;
            renderItemRowAt(
                    drawContext,
                    armorStacks,
                    armorScale,
                    rowX,
                    currentY,
                    armorColor,
                    armorGlow,
                    armorAlpha
            );
            currentY += armorHeight + OVERLAY_GROUP_GAP;
        }
        if (showHeld) {
            int rowX = startX + (groupWidth - heldWidth) / 2;
            renderItemRowAt(
                    drawContext,
                    heldStacks,
                    heldScale,
                    rowX,
                    currentY,
                    heldColor,
                    heldGlow,
                    heldAlpha
            );
        }
    }

    private static int getItemRowWidth(List<ItemStack> stacks, float itemScale) {
        int iconSize = getItemRowHeight(itemScale);
        int count = stacks.size();
        return count * iconSize + (count - 1) * ARMOR_OVERLAY_ICON_SPACING;
    }

    private static int getItemRowHeight(float itemScale) {
        return Math.max(6, Math.round(16.0F * MathHelper.clamp(itemScale, 0.35F, 2.5F)));
    }

    private static void renderItemRowAt(
            DrawContext drawContext,
            List<ItemStack> stacks,
            float itemScale,
            int startX,
            int startY,
            int accentColor,
            boolean glowEnabled,
            float alpha
    ) {
        if (stacks.isEmpty()) return;

        float clampedAlpha = MathHelper.clamp(alpha, 0.0F, 1.0F);
        int baseRgb = accentColor & 0xFFFFFF;
        int coreColor = 0xFF000000 | applyEmissive(baseRgb, 0.6F);
        int glowColor = 0xFF000000 | applyEmissive(baseRgb, 1.0F);
        int iconSize = getItemRowHeight(itemScale);
        int count = stacks.size();
        int totalWidth = count * iconSize + (count - 1) * ARMOR_OVERLAY_ICON_SPACING;
        int endX = startX + totalWidth;
        int endY = startY + iconSize;

        if (glowEnabled) {
            drawContext.fill(
                    startX - 3,
                    startY - 3,
                    endX + 3,
                    endY + 3,
                    withAlpha(glowColor, 0.35F * clampedAlpha)
            );
        }
        drawContext.fill(
                startX - 1,
                startY - 1,
                endX + 1,
                endY + 1,
                withAlpha(0x5A000000, clampedAlpha)
        );

        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, clampedAlpha);
        for (int i = 0; i < count; i++) {
            int iconX = startX + i * (iconSize + ARMOR_OVERLAY_ICON_SPACING);
            int iconY = startY;
            drawContext.getMatrices().push();
            drawContext.getMatrices().translate(iconX, iconY, 0.0F);
            drawContext.getMatrices().scale(itemScale, itemScale, 1.0F);
            drawContext.drawItem(stacks.get(i), 0, 0);
            drawContext.getMatrices().pop();
        }
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
    }

    private static void renderDistanceTextAt(
            DrawContext drawContext,
            MinecraftClient client,
            String distanceText,
            float distanceScale,
            int textX,
            int textY,
            int accentColor,
            boolean glowEnabled,
            float alpha
    ) {
        if (client.textRenderer == null) return;

        float clampedAlpha = MathHelper.clamp(alpha, 0.0F, 1.0F);
        int baseRgb = accentColor & 0xFFFFFF;
        int coreColor = 0xFF000000 | applyEmissive(baseRgb, 0.6F);
        int glowColor = 0xFF000000 | applyEmissive(baseRgb, 1.0F);
        int textWidth = Math.max(1, Math.round(client.textRenderer.getWidth(distanceText) * distanceScale));
        int textHeight = Math.max(1, Math.round(client.textRenderer.fontHeight * distanceScale));

        if (glowEnabled) {
            drawContext.fill(
                    textX - 3,
                    textY - 2,
                    textX + textWidth + 3,
                    textY + textHeight + 2,
                    withAlpha(glowColor, 0.32F * clampedAlpha)
            );
        }
        drawContext.fill(
                textX - 2,
                textY - 1,
                textX + textWidth + 2,
                textY + textHeight + 1,
                withAlpha(RAY_LABEL_BG_COLOR, clampedAlpha)
        );
        drawContext.getMatrices().push();
        drawContext.getMatrices().translate(textX, textY, 0.0F);
        drawContext.getMatrices().scale(distanceScale, distanceScale, 1.0F);
        drawContext.drawTextWithShadow(
                client.textRenderer,
                distanceText,
                0,
                0,
                withAlpha(coreColor, 0.95F * clampedAlpha)
        );
        drawContext.getMatrices().pop();
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
                int accentRgb = resolveVisualColor(target, 0.56F);
                int accentColor = 0xFF000000 | applyEmissive(accentRgb, 1.0F);
                drawContext.fill(textX - 4, textY - 3, textX + scaledWidth + 4, textY + scaledHeight + 3, withAlpha(accentColor, 0.33F));
            }
            drawContext.fill(textX - 2, textY - 1, textX + scaledWidth + 2, textY + scaledHeight + 1, TARGET_HEALTH_BG_COLOR);
            drawContext.getMatrices().push();
            drawContext.getMatrices().translate(textX, textY, 0.0F);
            drawContext.getMatrices().scale(targetHealthTextScale, targetHealthTextScale, 1.0F);
            int coreColor = 0xFF000000 | applyEmissive(color & 0xFFFFFF, 0.6F);
            drawContext.drawTextWithShadow(client.textRenderer, text, 0, 0, coreColor);
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
            int baseRgb = lines.get(0).color() & 0xFFFFFF;
            int glowColor = 0xFF000000 | applyEmissive(baseRgb, 1.0F);
            glowColor = withAlpha(glowColor, 0.32F * playerListAlphaMultiplier);
            drawContext.fill(x1 - 4, y1 - 4, x2 + 4, y2 + 4, glowColor);
        }
        drawContext.fill(x1 - 1, y1 - 1, x2 + 1, y2 + 1, withAlpha(PLAYER_LIST_BORDER_COLOR, playerListAlphaMultiplier));
        drawContext.fill(x1, y1, x2, y2, withAlpha(PLAYER_LIST_BG_COLOR, playerListAlphaMultiplier));

        int textX = x1 + PLAYER_LIST_PADDING;
        int textY = y1 + PLAYER_LIST_PADDING;
        for (PlayerListLine line : lines) {
            int baseRgb = line.color() & 0xFFFFFF;
            int coreColor = 0xFF000000 | applyEmissive(baseRgb, 0.6F);
            int glowColor = 0xFF000000 | applyEmissive(baseRgb, 1.0F);
            int textColor = withAlpha(coreColor, playerListAlphaMultiplier);
            if (rayVisualGlowEnabled) {
                drawContext.fill(
                        textX - 2,
                        textY - 1,
                        textX + Math.max(2, Math.round(client.textRenderer.getWidth(line.text()) * playerListTextScale)) + 2,
                        textY + textLineHeight - 1,
                        withAlpha(glowColor, 0.18F * playerListAlphaMultiplier)
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

    private static int applyEmissive(int rgbColor, float intensity) {
        float clamped = MathHelper.clamp(intensity, 0.0F, 1.0F);
        float[] hsv = rgbToHsv(rgbColor);
        float baseSat = Math.max(hsv[1], 0.35F);
        float baseVal = Math.max(hsv[2], 0.35F);
        float saturation = MathHelper.clamp(baseSat + 0.25F * clamped, 0.0F, 1.0F);
        float value = MathHelper.clamp(baseVal + 0.45F * clamped, 0.0F, 1.0F);
        return MathHelper.hsvToRgb(hsv[0], saturation, value);
    }

    private static Vec3d rgbToVec3(int rgbColor) {
        int rgb = rgbColor & 0xFFFFFF;
        double r = ((rgb >> 16) & 0xFF) / 255.0;
        double g = ((rgb >> 8) & 0xFF) / 255.0;
        double b = (rgb & 0xFF) / 255.0;
        return new Vec3d(r, g, b);
    }

    private static int getPlayerGroupingColor(PlayerEntity player) {
        return getPlayerBaseColor(player);
    }

    private static int resolveVisualColor(PlayerEntity player, float offset) {
        return resolveVisualColor(
                getPlayerBaseColor(player),
                player.getId(),
                offset,
                rayVisualColorMode,
                rayVisualSaturationBoost,
                rayVisualAnimationSpeed,
                rayVisualRevision
        );
    }

    private static int resolveVisualColor(
            PlayerEntity player,
            float offset,
            VisualColorMode colorMode,
            float saturationBoost,
            float animationSpeed
    ) {
        return resolveVisualColor(getPlayerBaseColor(player), player.getId(), offset, colorMode, saturationBoost, animationSpeed, rayVisualRevision);
    }

    private static int resolveVisualColor(
            PlayerEntity player,
            float offset,
            VisualColorMode colorMode,
            float saturationBoost,
            float animationSpeed,
            int revision
    ) {
        return resolveVisualColor(getPlayerBaseColor(player), player.getId(), offset, colorMode, saturationBoost, animationSpeed, revision);
    }

    private static int resolveVisualColor(
            int baseColor,
            int seed,
            float offset,
            VisualColorMode colorMode,
            float saturationBoost,
            float animationSpeed,
            int revision
    ) {
        int rgbBase = baseColor & 0x00FFFFFF;
        int saltedSeed = seed ^ (revision * 0x9E3779B9);
        return switch (colorMode) {
            case NICK -> rgbBase;
            case GRADIENT -> toGradientColor(seedBaseColor(saltedSeed), saltedSeed, offset, saturationBoost, animationSpeed);
            case NICK_GRADIENT -> toNickGradientColor(rgbBase, saltedSeed, offset, saturationBoost, animationSpeed);
            case RAINBOW -> toRainbowColor(saltedSeed, offset, saturationBoost, animationSpeed);
        };
    }

    private static int resolveAutoAttackCircleColor(float offset) {
        int rgb = switch (autoAttackCircleColorMode) {
            case FIXED -> autoAttackCircleColor;
            case GRADIENT -> toGradientColor(seedBaseColor(AUTO_ATTACK_CIRCLE_SEED), AUTO_ATTACK_CIRCLE_SEED, offset, DEFAULT_STYLE_SATURATION, 1.0F);
            case RAINBOW -> toRainbowColor(AUTO_ATTACK_CIRCLE_SEED, offset, DEFAULT_STYLE_SATURATION, 1.0F);
        };
        return 0xFF000000 | applyEmissive(rgb, 0.9F);
    }

    private static int toGradientColor(int rgbColor, int seed, float offset, float saturationBoost, float animationSpeed) {
        float[] hsv = rgbToHsv(rgbColor);
        float baseHue = hsv[0];
        float baseSat = hsv[1];
        float baseVal = hsv[2];
        float time = visualTime(animationSpeed);
        float seedUnit = seedToUnit(seed);

        if (baseSat < 0.12F) {
            baseHue = wrapUnit(seedUnit * 0.7F + time * 0.08F);
            baseSat = 0.55F;
            baseVal = Math.max(baseVal, 0.7F);
        }

        float t = wrapUnit(time * 0.18F + offset * 1.15F + seedUnit);
        float wave = 0.5F + 0.5F * MathHelper.sin(TWO_PI * t);

        float hue = wrapUnit(baseHue + (wave - 0.5F) * 0.18F + (seedUnit - 0.5F) * 0.08F);
        float satBase = MathHelper.clamp(baseSat * saturationBoost, 0.25F, 1.0F);
        float saturation = MathHelper.clamp(satBase * (0.75F + 0.5F * wave), 0.2F, 1.0F);

        float valBase = MathHelper.clamp(baseVal, 0.35F, 1.0F);
        float darkVal = MathHelper.clamp(valBase * 0.55F, 0.2F, 1.0F);
        float brightVal = MathHelper.clamp(valBase * 1.25F, 0.2F, 1.0F);
        float value = MathHelper.lerp(wave, darkVal, brightVal);

        return MathHelper.hsvToRgb(hue, saturation, value);
    }

    private static int toNickGradientColor(int rgbColor, int seed, float offset, float saturationBoost, float animationSpeed) {
        float[] hsv = rgbToHsv(rgbColor);
        float baseHue = hsv[0];
        float baseSat = hsv[1];
        float baseVal = hsv[2];
        float time = visualTime(animationSpeed);
        float seedUnit = seedToUnit(seed);

        float t = wrapUnit(time * 0.16F + offset * 1.1F + seedUnit * 0.25F);
        float wave = 0.5F + 0.5F * MathHelper.sin(TWO_PI * t);

        float hue = wrapUnit(baseHue + (wave - 0.5F) * 0.08F + (seedUnit - 0.5F) * 0.02F);
        float satBase = MathHelper.clamp(baseSat * saturationBoost, 0.2F, 1.0F);
        float saturation = MathHelper.clamp(satBase * (0.8F + 0.35F * wave), 0.2F, 1.0F);

        float valBase = MathHelper.clamp(baseVal, 0.35F, 1.0F);
        float value = MathHelper.clamp(valBase * (0.7F + 0.45F * wave), 0.2F, 1.0F);
        return MathHelper.hsvToRgb(hue, saturation, value);
    }

    private static int toRainbowColor(int seed, float offset, float saturationBoost, float animationSpeed) {
        float time = visualTime(animationSpeed);
        float seedUnit = seedToUnit(seed);
        float hue = wrapUnit(time * 0.22F + offset * 1.2F + seedUnit * 0.8F);
        float saturation = MathHelper.clamp(0.85F * saturationBoost, 0.65F, 1.0F);
        return MathHelper.hsvToRgb(hue, saturation, 1.0F);
    }

    private static float visualTime(float animationSpeed) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client != null && client.getRenderTickCounter() != null) {
            float tickDelta = client.getRenderTickCounter().getTickDelta(false);
            if (client.player != null) {
                return (client.player.age + tickDelta) * 0.05F * animationSpeed;
            }
            if (client.world != null) {
                return (client.world.getTime() + tickDelta) * 0.05F * animationSpeed;
            }
        }
        return (System.nanoTime() / 1_000_000_000.0F) * animationSpeed;
    }

    private static int seedBaseColor(int seed) {
        float hue = seedToUnit(seed * 0x1F123BB5);
        float saturation = 0.75F;
        float value = 1.0F;
        return MathHelper.hsvToRgb(hue, saturation, value);
    }

    private static int nextRevision(int value) {
        return (value + 1) & 0x7FFFFFFF;
    }

    private static void bumpRayRevision() {
        rayVisualRevision = nextRevision(rayVisualRevision);
    }

    private static void bumpEspRevision() {
        espVisualRevision = nextRevision(espVisualRevision);
    }

    private static void bumpArmorRevision() {
        armorVisualRevision = nextRevision(armorVisualRevision);
    }

    private static void bumpHeldItemRevision() {
        heldItemVisualRevision = nextRevision(heldItemVisualRevision);
    }

    private static void bumpDistanceRevision() {
        distanceVisualRevision = nextRevision(distanceVisualRevision);
    }

    private static void bumpItemOutlineRevision() {
        itemOutlineVisualRevision = nextRevision(itemOutlineVisualRevision);
    }

    private static float wrapUnit(float value) {
        float wrapped = value % 1.0F;
        return wrapped < 0.0F ? wrapped + 1.0F : wrapped;
    }

    private static float seedToUnit(int seed) {
        int hash = seed;
        hash ^= (hash >>> 16);
        hash *= 0x7FEB352D;
        hash ^= (hash >>> 15);
        hash *= 0x846CA68B;
        hash ^= (hash >>> 16);
        return (hash & 0xFFFF) / 65535.0F;
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

    private static void applyLoadedConfig(PaprikaConfig.Data config) {
        if (config == null) return;

        panicActive = false;
        speedEnabled = config.speedEnabled;
        noKnockbackEnabled = config.noKnockbackEnabled;
        playerEspEnabled = config.playerEspEnabled;
        playerArmorOverlayEnabled = config.playerArmorOverlayEnabled;
        playerRaysEnabled = config.playerRaysEnabled;
        playerListEnabled = config.playerListEnabled;
        playerTrailsEnabled = config.playerTrailsEnabled;
        trailSelfEnabled = config.trailSelfEnabled;
        trailOthersEnabled = config.trailOthersEnabled;
        autoAttackEnabled = config.autoAttackEnabled;
        autoAttackAimEnabled = config.autoAttackAimEnabled;
        itemOutlineEnabled = config.itemOutlineEnabled;
        loadFriends(config.friendNames);
        loadItemFilters(config.itemFilterIds);
        targetHealthOverlayEnabled = config.targetHealthOverlayEnabled;
        targetHealthDynamicColorEnabled = config.targetHealthDynamicColorEnabled;
        distanceDisplayEnabled = config.distanceDisplayEnabled;
        heldItemOverlayEnabled = config.heldItemOverlayEnabled;
        customSkyEnabled = config.customSkyEnabled;
        skyTopRainbowEnabled = config.skyTopRainbowEnabled;
        skyBottomRainbowEnabled = config.skyBottomRainbowEnabled;
        hideHandsWithItemEnabled = config.hideHandsWithItemEnabled;
        handItemFlipEnabled = config.handItemFlipEnabled;
        rayVisualGlowEnabled = config.rayVisualGlowEnabled;
        espVisualGlowEnabled = config.espVisualGlowEnabled;
        armorVisualGlowEnabled = config.armorVisualGlowEnabled;
        heldItemVisualGlowEnabled = config.heldItemVisualGlowEnabled;
        distanceVisualGlowEnabled = config.distanceVisualGlowEnabled;
        autoAttackRequireLineOfSight = config.autoAttackRequireLineOfSight;
        itemOutlineGlowEnabled = config.itemOutlineGlowEnabled;
        rayThickness = MathHelper.clamp(config.rayThickness, 0.5F, 8.0F);
        outlineThickness = MathHelper.clamp(config.outlineThickness, 0.5F, 6.0F);
        rayBottomStartHeight = MathHelper.clamp(config.rayBottomStartHeight, 0.0F, MAX_BOTTOM_RAY_START_HEIGHT);
        distanceTextScale = MathHelper.clamp(config.rayDistanceTextScale, 0.5F, 2.0F);
        armorOverlayScale = MathHelper.clamp(config.armorOverlayScale, 0.35F, 2.5F);
        heldItemOverlayScale = MathHelper.clamp(config.heldItemOverlayScale, 0.35F, 2.5F);
        rayAlpha = MathHelper.clamp(config.rayAlpha, 0.1F, 1.0F);
        armorAlpha = MathHelper.clamp(config.armorAlpha, 0.1F, 1.0F);
        heldItemAlpha = MathHelper.clamp(config.heldItemAlpha, 0.1F, 1.0F);
        distanceAlpha = MathHelper.clamp(config.distanceAlpha, 0.1F, 1.0F);
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
        espVisualSaturationBoost = MathHelper.clamp(config.espVisualSaturationBoost, 1.0F, 2.5F);
        espVisualAnimationSpeed = MathHelper.clamp(config.espVisualAnimationSpeed, 0.2F, 4.0F);
        itemOutlineSaturationBoost = MathHelper.clamp(config.itemOutlineSaturationBoost, 1.0F, 2.5F);
        itemOutlineAnimationSpeed = MathHelper.clamp(config.itemOutlineAnimationSpeed, 0.2F, 4.0F);
        trailStripeHeight = MathHelper.clamp(config.trailStripeHeight, 0.2F, 4.0F);
        trailLifetimeSeconds = MathHelper.clamp(config.trailLifetimeSeconds, 0.1F, 10.0F);
        trailGradientSpeed = MathHelper.clamp(config.trailGradientSpeed, 0.1F, 5.0F);
        trailAlpha = MathHelper.clamp(config.trailAlpha, 0.1F, 1.0F);
        itemOutlineAlpha = MathHelper.clamp(config.itemOutlineAlpha, 0.05F, 1.0F);
        itemOutlineThickness = MathHelper.clamp(config.itemOutlineThickness, 0.5F, 6.0F);
        autoAttackRate = MathHelper.clamp(config.autoAttackRate, 1.0F, 20.0F);
        autoAttackCircleRadius = MathHelper.clamp(config.autoAttackCircleRadius, 20.0F, 600.0F);
        autoAttackMaxDistance = MathHelper.clamp(config.autoAttackMaxDistance, 3.0F, 20.0F);
        jumpBoostHeight = MathHelper.clamp(config.jumpBoostHeight, 0.0F, 2.5F);
        handFovScale = MathHelper.clamp(config.handFovScale, -1.6F, 1.6F);
        handOffsetX = MathHelper.clamp(config.handOffsetX, -1.5F, 1.5F);
        handOffsetY = MathHelper.clamp(config.handOffsetY, -1.5F, 1.5F);
        playerListOffsetX = MathHelper.clamp(config.playerListOffsetX, 0, MAX_PLAYER_LIST_OFFSET);
        playerListOffsetY = MathHelper.clamp(config.playerListOffsetY, 0, MAX_PLAYER_LIST_OFFSET);
        skyTopColor = config.skyTopColor & 0xFFFFFF;
        skyBottomColor = config.skyBottomColor & 0xFFFFFF;
        trailFixedColor = config.trailFixedColor & 0xFFFFFF;
        autoAttackCircleColor = config.autoAttackCircleColor & 0xFFFFFF;
        itemOutlineSolidColor = config.itemOutlineSolidColor & 0xFFFFFF;
        menuLastTabId = (config.menuLastTab == null || config.menuLastTab.isBlank()) ? "RAYS" : config.menuLastTab;
        menuScrollOffset = config.menuScrollOffset;
        rayOrigin = parseRayOrigin(config.rayOrigin);
        handItemOrientation = parseHandItemOrientation(config.handItemOrientation);
        armorAnchorMode = parseOverlayAnchorMode(config.armorAnchorMode, OverlayAnchorMode.ABOVE_PLAYER);
        heldItemAnchorMode = parseOverlayAnchorMode(config.heldItemAnchorMode, OverlayAnchorMode.ABOVE_PLAYER);
        distanceAnchorMode = parseOverlayAnchorMode(config.distanceAnchorMode, OverlayAnchorMode.RAY_MIDDLE);
        rayVisualColorMode = parseVisualColorMode(config.rayVisualColorMode, VisualColorMode.NICK);
        armorVisualColorMode = parseVisualColorMode(config.armorVisualColorMode, VisualColorMode.NICK);
        heldItemVisualColorMode = parseVisualColorMode(config.heldItemVisualColorMode, VisualColorMode.NICK);
        distanceVisualColorMode = parseVisualColorMode(config.distanceVisualColorMode, VisualColorMode.NICK);
        espVisualColorMode = parseVisualColorMode(config.espVisualColorMode, VisualColorMode.NICK);
        itemOutlineColorMode = parseItemOutlineColorMode(config.itemOutlineColorMode);
        itemOutlineMode = parseItemOutlineMode(config.itemOutlineMode);
        autoAttackMode = parseAutoAttackMode(config.autoAttackMode);
        autoAttackCircleColorMode = parseCircleColorMode(config.autoAttackCircleColorMode);
        trailType = parseTrailType(config.trailType);
        trailOrigin = parseTrailOrigin(config.trailOrigin);
        trailColorMode = parseTrailColorMode(config.trailColorMode);
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

    private static HandItemOrientation parseHandItemOrientation(String rawValue) {
        if (rawValue == null || rawValue.isBlank()) {
            return HandItemOrientation.DEFAULT;
        }

        try {
            return HandItemOrientation.valueOf(rawValue.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ignored) {
            return HandItemOrientation.DEFAULT;
        }
    }

    private static VisualColorMode parseVisualColorMode(String rawValue, VisualColorMode fallback) {
        if (rawValue == null || rawValue.isBlank()) {
            return fallback;
        }

        try {
            String normalized = rawValue.toUpperCase(Locale.ROOT);
            if ("VIVID".equals(normalized)) {
                return VisualColorMode.NICK;
            }
            return VisualColorMode.valueOf(normalized);
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

    private static TrailType parseTrailType(String rawValue) {
        if (rawValue == null || rawValue.isBlank()) {
            return TrailType.THIN_LINE;
        }

        try {
            return TrailType.valueOf(rawValue.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ignored) {
            return TrailType.THIN_LINE;
        }
    }

    private static TrailOrigin parseTrailOrigin(String rawValue) {
        if (rawValue == null || rawValue.isBlank()) {
            return TrailOrigin.BACK;
        }

        try {
            return TrailOrigin.valueOf(rawValue.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ignored) {
            return TrailOrigin.BACK;
        }
    }

    private static TrailColorMode parseTrailColorMode(String rawValue) {
        if (rawValue == null || rawValue.isBlank()) {
            return TrailColorMode.NICK;
        }

        try {
            return TrailColorMode.valueOf(rawValue.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ignored) {
            return TrailColorMode.NICK;
        }
    }

    private static ItemOutlineColorMode parseItemOutlineColorMode(String rawValue) {
        if (rawValue == null || rawValue.isBlank()) {
            return ItemOutlineColorMode.NICK;
        }

        try {
            String normalized = rawValue.toUpperCase(Locale.ROOT);
            if ("AVERAGE".equals(normalized) || "AVG".equals(normalized)) {
                return ItemOutlineColorMode.ITEM_AVERAGE;
            }
            if ("ITEM".equals(normalized)) {
                return ItemOutlineColorMode.ITEM_AVERAGE;
            }
            return ItemOutlineColorMode.valueOf(normalized);
        } catch (IllegalArgumentException ignored) {
            return ItemOutlineColorMode.NICK;
        }
    }

    private static ItemOutlineMode parseItemOutlineMode(String rawValue) {
        if (rawValue == null || rawValue.isBlank()) {
            return ItemOutlineMode.ALL;
        }

        try {
            return ItemOutlineMode.valueOf(rawValue.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ignored) {
            return ItemOutlineMode.ALL;
        }
    }

    private static AutoAttackMode parseAutoAttackMode(String rawValue) {
        if (rawValue == null || rawValue.isBlank()) {
            return AutoAttackMode.CIRCLE;
        }

        try {
            return AutoAttackMode.valueOf(rawValue.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ignored) {
            return AutoAttackMode.CIRCLE;
        }
    }

    private static CircleColorMode parseCircleColorMode(String rawValue) {
        if (rawValue == null || rawValue.isBlank()) {
            return CircleColorMode.FIXED;
        }

        try {
            return CircleColorMode.valueOf(rawValue.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ignored) {
            return CircleColorMode.FIXED;
        }
    }

    private static PaprikaConfig.Data captureConfig() {
        PaprikaConfig.Data data = new PaprikaConfig.Data();
        data.speedEnabled = speedEnabled;
        data.noKnockbackEnabled = noKnockbackEnabled;
        data.playerEspEnabled = playerEspEnabled;
        data.playerArmorOverlayEnabled = playerArmorOverlayEnabled;
        data.playerRaysEnabled = playerRaysEnabled;
        data.playerListEnabled = playerListEnabled;
        data.playerTrailsEnabled = playerTrailsEnabled;
        data.trailSelfEnabled = trailSelfEnabled;
        data.trailOthersEnabled = trailOthersEnabled;
        data.autoAttackEnabled = autoAttackEnabled;
        data.autoAttackAimEnabled = autoAttackAimEnabled;
        data.itemOutlineEnabled = itemOutlineEnabled;
        data.jumpBoostEnabled = jumpBoostEnabled;
        data.targetHealthOverlayEnabled = targetHealthOverlayEnabled;
        data.targetHealthDynamicColorEnabled = targetHealthDynamicColorEnabled;
        data.distanceDisplayEnabled = distanceDisplayEnabled;
        data.heldItemOverlayEnabled = heldItemOverlayEnabled;
        data.customSkyEnabled = customSkyEnabled;
        data.skyTopRainbowEnabled = skyTopRainbowEnabled;
        data.skyBottomRainbowEnabled = skyBottomRainbowEnabled;
        data.hideHandsWithItemEnabled = hideHandsWithItemEnabled;
        data.itemOutlineGlowEnabled = itemOutlineGlowEnabled;
        data.handItemFlipEnabled = handItemFlipEnabled;
        data.rayVisualGlowEnabled = rayVisualGlowEnabled;
        data.espVisualGlowEnabled = espVisualGlowEnabled;
        data.armorVisualGlowEnabled = armorVisualGlowEnabled;
        data.heldItemVisualGlowEnabled = heldItemVisualGlowEnabled;
        data.distanceVisualGlowEnabled = distanceVisualGlowEnabled;
        data.autoAttackRequireLineOfSight = autoAttackRequireLineOfSight;
        data.rayThickness = rayThickness;
        data.outlineThickness = outlineThickness;
        data.rayBottomStartHeight = rayBottomStartHeight;
        data.rayDistanceTextScale = distanceTextScale;
        data.armorOverlayScale = armorOverlayScale;
        data.heldItemOverlayScale = heldItemOverlayScale;
        data.rayAlpha = rayAlpha;
        data.armorAlpha = armorAlpha;
        data.heldItemAlpha = heldItemAlpha;
        data.distanceAlpha = distanceAlpha;
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
        data.espVisualSaturationBoost = espVisualSaturationBoost;
        data.espVisualAnimationSpeed = espVisualAnimationSpeed;
        data.itemOutlineSaturationBoost = itemOutlineSaturationBoost;
        data.itemOutlineAnimationSpeed = itemOutlineAnimationSpeed;
        data.trailStripeHeight = trailStripeHeight;
        data.trailLifetimeSeconds = trailLifetimeSeconds;
        data.trailGradientSpeed = trailGradientSpeed;
        data.trailAlpha = trailAlpha;
        data.itemOutlineAlpha = itemOutlineAlpha;
        data.itemOutlineThickness = itemOutlineThickness;
        data.autoAttackRate = autoAttackRate;
        data.autoAttackCircleRadius = autoAttackCircleRadius;
        data.autoAttackMaxDistance = autoAttackMaxDistance;
        data.jumpBoostHeight = jumpBoostHeight;
        data.handFovScale = handFovScale;
        data.handOffsetX = handOffsetX;
        data.handOffsetY = handOffsetY;
        data.playerListOffsetX = playerListOffsetX;
        data.playerListOffsetY = playerListOffsetY;
        data.skyTopColor = skyTopColor & 0xFFFFFF;
        data.skyBottomColor = skyBottomColor & 0xFFFFFF;
        data.trailFixedColor = trailFixedColor & 0xFFFFFF;
        data.autoAttackCircleColor = autoAttackCircleColor & 0xFFFFFF;
        data.menuLastTab = menuLastTabId;
        data.menuScrollOffset = menuScrollOffset;
        data.rayOrigin = rayOrigin.name();
        data.handItemOrientation = handItemOrientation.name();
        data.armorAnchorMode = armorAnchorMode.name();
        data.heldItemAnchorMode = heldItemAnchorMode.name();
        data.distanceAnchorMode = distanceAnchorMode.name();
        data.rayVisualColorMode = rayVisualColorMode.name();
        data.armorVisualColorMode = armorVisualColorMode.name();
        data.heldItemVisualColorMode = heldItemVisualColorMode.name();
        data.distanceVisualColorMode = distanceVisualColorMode.name();
        data.espVisualColorMode = espVisualColorMode.name();
        data.autoAttackMode = autoAttackMode.name();
        data.autoAttackCircleColorMode = autoAttackCircleColorMode.name();
        data.trailType = trailType.name();
        data.trailOrigin = trailOrigin.name();
        data.trailColorMode = trailColorMode.name();
        data.itemOutlineColorMode = itemOutlineColorMode.name();
        data.itemOutlineMode = itemOutlineMode.name();
        data.friendNames = new ArrayList<>(friendNames.values());
        data.itemFilterIds = new ArrayList<>(itemFilterIds.values());
        data.itemOutlineSolidColor = itemOutlineSolidColor & 0xFFFFFF;

        if (toggleKey != null) {
            data.speedToggleKey = toggleKey.getBoundKeyTranslationKey();
        }
        if (toggleNoKnockbackKey != null) {
            data.noKnockbackKey = toggleNoKnockbackKey.getBoundKeyTranslationKey();
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
        if (togglePlayerTrailsKey != null) {
            data.playerTrailsKey = togglePlayerTrailsKey.getBoundKeyTranslationKey();
        }
        if (toggleItemOutlineKey != null) {
            data.itemOutlineKey = toggleItemOutlineKey.getBoundKeyTranslationKey();
        }
        if (toggleAutoAttackKey != null) {
            data.autoAttackKey = toggleAutoAttackKey.getBoundKeyTranslationKey();
        }
        if (markTargetKey != null) {
            data.markTargetKey = markTargetKey.getBoundKeyTranslationKey();
        }
        if (unmarkTargetKey != null) {
            data.unmarkTargetKey = unmarkTargetKey.getBoundKeyTranslationKey();
        }
        if (markFriendKey != null) {
            data.markFriendKey = markFriendKey.getBoundKeyTranslationKey();
        }
        if (panicKey != null) {
            data.panicKey = panicKey.getBoundKeyTranslationKey();
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

    private static void loadFriends(List<String> names) {
        friendNames.clear();
        if (names == null) return;
        for (String name : names) {
            String sanitized = sanitizeFriendName(name);
            if (sanitized == null) continue;
            friendNames.put(sanitized.toLowerCase(Locale.ROOT), sanitized);
        }
    }

    private static void loadItemFilters(List<String> ids) {
        itemFilterIds.clear();
        if (ids == null) return;
        for (String rawId : ids) {
            String sanitized = sanitizeItemId(rawId);
            if (sanitized == null) continue;
            itemFilterIds.put(sanitized.toLowerCase(Locale.ROOT), sanitized);
        }
    }

    public enum RayOrigin {
        BOTTOM,
        CENTER
    }

    public enum HandItemOrientation {
        DEFAULT,
        LEFT,
        RIGHT
    }

    public enum VisualColorMode {
        NICK,
        GRADIENT,
        NICK_GRADIENT,
        RAINBOW
    }

    public enum ItemOutlineColorMode {
        NICK(VisualColorMode.NICK),
        GRADIENT(VisualColorMode.GRADIENT),
        NICK_GRADIENT(VisualColorMode.NICK_GRADIENT),
        RAINBOW(VisualColorMode.RAINBOW),
        SOLID(VisualColorMode.NICK),
        ITEM_AVERAGE(VisualColorMode.NICK);

        private final VisualColorMode visualColorMode;

        ItemOutlineColorMode(VisualColorMode visualColorMode) {
            this.visualColorMode = visualColorMode;
        }

        public VisualColorMode getVisualColorMode() {
            return visualColorMode;
        }
    }

    public enum AutoAttackMode {
        CIRCLE,
        CIRCLE_MARK,
        MARK_ONLY,
        ALL_NEARBY
    }

    public enum CircleColorMode {
        FIXED,
        GRADIENT,
        RAINBOW
    }

    public enum TrailType {
        THIN_LINE,
        FLOATING_LINE,
        STRIP
    }

    public enum TrailOrigin {
        BACK,
        HEAD
    }

    public enum TrailColorMode {
        NICK,
        FIXED,
        GRADIENT,
        NICK_GRADIENT
    }

    public enum ItemOutlineMode {
        ALL,
        WHITELIST,
        BLACKLIST
    }

    public enum OverlayAnchorMode {
        ABOVE_PLAYER,
        RAY_MIDDLE
    }

    private enum FriendMarkResult {
        ADDED,
        ALREADY,
        FAILED
    }

    private record TrailState(Vec3d lastPos, Vec3d backDir) {
    }

    private record TrailSegment(Vec3d start, Vec3d end, Vec3d backDirection, int color, double spawnTime) {
    }
}
