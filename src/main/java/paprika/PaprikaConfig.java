package paprika;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.util.InputUtil;
import net.minecraft.util.math.MathHelper;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public final class PaprikaConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path CONFIG_PATH = FabricLoader.getInstance().getConfigDir().resolve("paprika.json");
    private static final Path LEGACY_CONFIG_PATH = FabricLoader.getInstance().getConfigDir().resolve("noknockback.json");

    private PaprikaConfig() {
    }

    public static Data load() {
        Data defaults = new Data();
        Path sourcePath = CONFIG_PATH;

        if (!Files.exists(CONFIG_PATH) && Files.exists(LEGACY_CONFIG_PATH)) {
            sourcePath = LEGACY_CONFIG_PATH;
        }

        if (!Files.exists(sourcePath)) {
            save(defaults);
            return defaults;
        }

        try (Reader reader = Files.newBufferedReader(sourcePath, StandardCharsets.UTF_8)) {
            Data loaded = GSON.fromJson(reader, Data.class);
            Data sanitized = sanitize(loaded == null ? defaults : loaded);
            if (!sourcePath.equals(CONFIG_PATH)) {
                save(sanitized);
            }
            return sanitized;
        } catch (Exception ignored) {
            return defaults;
        }
    }

    public static void save(Data data) {
        Data sanitized = sanitize(data);

        try {
            Files.createDirectories(CONFIG_PATH.getParent());
            try (Writer writer = Files.newBufferedWriter(CONFIG_PATH, StandardCharsets.UTF_8)) {
                GSON.toJson(sanitized, writer);
            }
        } catch (IOException ignored) {
        }
    }

    private static Data sanitize(Data source) {
        Data data = source == null ? new Data() : source;

        Data sanitized = new Data();
        sanitized.speedEnabled = data.speedEnabled;
        sanitized.noKnockbackEnabled = data.noKnockbackEnabled;
        sanitized.playerEspEnabled = data.playerEspEnabled;
        sanitized.playerArmorOverlayEnabled = data.playerArmorOverlayEnabled;
        sanitized.playerRaysEnabled = data.playerRaysEnabled;
        sanitized.playerListEnabled = data.playerListEnabled;
        sanitized.playerDollEnabled = data.playerDollEnabled;
        sanitized.playerTrailsEnabled = data.playerTrailsEnabled;
        sanitized.trailSelfEnabled = data.trailSelfEnabled;
        sanitized.trailOthersEnabled = data.trailOthersEnabled;
        sanitized.autoAttackEnabled = data.autoAttackEnabled;
        sanitized.autoAttackAimEnabled = data.autoAttackAimEnabled;
        sanitized.autoAttackExtendReachEnabled = data.autoAttackExtendReachEnabled;
        sanitized.autoAttackAimSmoothing = MathHelper.clamp(data.autoAttackAimSmoothing, 0.02F, 0.6F);
        sanitized.itemOutlineEnabled = data.itemOutlineEnabled;
        sanitized.jumpBoostEnabled = data.jumpBoostEnabled;
        sanitized.targetHealthOverlayEnabled = data.targetHealthOverlayEnabled;
        sanitized.targetHealthDynamicColorEnabled = data.targetHealthDynamicColorEnabled;
        sanitized.distanceDisplayEnabled = data.distanceDisplayEnabled;
        sanitized.heldItemOverlayEnabled = data.heldItemOverlayEnabled;
        sanitized.customSkyEnabled = data.customSkyEnabled;
        sanitized.skyTopRainbowEnabled = data.skyTopRainbowEnabled;
        sanitized.skyBottomRainbowEnabled = data.skyBottomRainbowEnabled;
        sanitized.hideHandsWithItemEnabled = data.hideHandsWithItemEnabled;
        sanitized.friendNames = sanitizeFriendNames(data.friendNames);
        sanitized.itemFilterIds = sanitizeItemFilterIds(data.itemFilterIds);
        sanitized.handItemFlipEnabled = data.handItemFlipEnabled;
        sanitized.rayVisualGlowEnabled = data.rayVisualGlowEnabled;
        sanitized.espVisualGlowEnabled = data.espVisualGlowEnabled;
        sanitized.armorVisualGlowEnabled = data.armorVisualGlowEnabled;
        sanitized.heldItemVisualGlowEnabled = data.heldItemVisualGlowEnabled;
        sanitized.distanceVisualGlowEnabled = data.distanceVisualGlowEnabled;
        sanitized.autoAttackRequireLineOfSight = data.autoAttackRequireLineOfSight;
        sanitized.itemOutlineGlowEnabled = data.itemOutlineGlowEnabled;
        sanitized.rayThickness = MathHelper.clamp(data.rayThickness, 0.5F, 8.0F);
        sanitized.outlineThickness = MathHelper.clamp(data.outlineThickness, 0.5F, 6.0F);
        sanitized.rayBottomStartHeight = MathHelper.clamp(data.rayBottomStartHeight, 0.0F, 300.0F);
        sanitized.rayDistanceTextScale = MathHelper.clamp(data.rayDistanceTextScale, 0.5F, 2.0F);
        sanitized.armorOverlayScale = MathHelper.clamp(data.armorOverlayScale, 0.35F, 2.5F);
        sanitized.heldItemOverlayScale = MathHelper.clamp(data.heldItemOverlayScale, 0.35F, 2.5F);
        sanitized.rayAlpha = MathHelper.clamp(data.rayAlpha, 0.1F, 1.0F);
        sanitized.armorAlpha = MathHelper.clamp(data.armorAlpha, 0.1F, 1.0F);
        sanitized.heldItemAlpha = MathHelper.clamp(data.heldItemAlpha, 0.1F, 1.0F);
        sanitized.distanceAlpha = MathHelper.clamp(data.distanceAlpha, 0.1F, 1.0F);
        sanitized.targetHealthTextScale = MathHelper.clamp(data.targetHealthTextScale, 0.5F, 2.0F);
        sanitized.playerListTextScale = MathHelper.clamp(data.playerListTextScale, 0.1F, 2.0F);
        sanitized.playerListMaxHeight = MathHelper.clamp(data.playerListMaxHeight, 40, 4096);
        sanitized.playerListAlpha = MathHelper.clamp(data.playerListAlpha, 0.1F, 1.0F);
        sanitized.playerListOffsetX = MathHelper.clamp(data.playerListOffsetX, 0, 4096);
        sanitized.playerListOffsetY = MathHelper.clamp(data.playerListOffsetY, 0, 4096);
        sanitized.playerDollSize = MathHelper.clamp(data.playerDollSize, 30.0F, 240.0F);
        sanitized.playerDollOffsetX = MathHelper.clamp(data.playerDollOffsetX, -4096, 4096);
        sanitized.playerDollOffsetY = MathHelper.clamp(data.playerDollOffsetY, -4096, 4096);
        sanitized.rayOrigin = sanitizeRayOrigin(data.rayOrigin);
        sanitized.armorAnchorMode = sanitizeOverlayAnchorMode(data.armorAnchorMode, PaprikaClient.OverlayAnchorMode.ABOVE_PLAYER);
        sanitized.heldItemAnchorMode = sanitizeOverlayAnchorMode(data.heldItemAnchorMode, PaprikaClient.OverlayAnchorMode.ABOVE_PLAYER);
        sanitized.distanceAnchorMode = sanitizeOverlayAnchorMode(data.distanceAnchorMode, PaprikaClient.OverlayAnchorMode.RAY_MIDDLE);
        sanitized.playerDollCorner = sanitizeHudCorner(data.playerDollCorner, PaprikaClient.HudCorner.TOP_LEFT);
        sanitized.rayVisualColorMode = sanitizeVisualColorMode(data.rayVisualColorMode, PaprikaClient.VisualColorMode.NICK);
        sanitized.armorVisualColorMode = sanitizeVisualColorMode(data.armorVisualColorMode, PaprikaClient.VisualColorMode.NICK);
        sanitized.heldItemVisualColorMode = sanitizeVisualColorMode(data.heldItemVisualColorMode, PaprikaClient.VisualColorMode.NICK);
        sanitized.distanceVisualColorMode = sanitizeVisualColorMode(data.distanceVisualColorMode, PaprikaClient.VisualColorMode.NICK);
        sanitized.espVisualColorMode = sanitizeVisualColorMode(data.espVisualColorMode, PaprikaClient.VisualColorMode.NICK);
        sanitized.itemOutlineColorMode = sanitizeItemOutlineColorMode(data.itemOutlineColorMode);
        sanitized.handItemOrientation = sanitizeHandItemOrientation(data.handItemOrientation);
        sanitized.rayVisualSaturationBoost = MathHelper.clamp(data.rayVisualSaturationBoost, 1.0F, 2.5F);
        sanitized.rayVisualAnimationSpeed = MathHelper.clamp(data.rayVisualAnimationSpeed, 0.2F, 4.0F);
        sanitized.armorVisualSaturationBoost = MathHelper.clamp(data.armorVisualSaturationBoost, 1.0F, 2.5F);
        sanitized.armorVisualAnimationSpeed = MathHelper.clamp(data.armorVisualAnimationSpeed, 0.2F, 4.0F);
        sanitized.heldItemVisualSaturationBoost = MathHelper.clamp(data.heldItemVisualSaturationBoost, 1.0F, 2.5F);
        sanitized.heldItemVisualAnimationSpeed = MathHelper.clamp(data.heldItemVisualAnimationSpeed, 0.2F, 4.0F);
        sanitized.distanceVisualSaturationBoost = MathHelper.clamp(data.distanceVisualSaturationBoost, 1.0F, 2.5F);
        sanitized.distanceVisualAnimationSpeed = MathHelper.clamp(data.distanceVisualAnimationSpeed, 0.2F, 4.0F);
        sanitized.espVisualSaturationBoost = MathHelper.clamp(data.espVisualSaturationBoost, 1.0F, 2.5F);
        sanitized.espVisualAnimationSpeed = MathHelper.clamp(data.espVisualAnimationSpeed, 0.2F, 4.0F);
        sanitized.itemOutlineSaturationBoost = MathHelper.clamp(data.itemOutlineSaturationBoost, 1.0F, 2.5F);
        sanitized.itemOutlineAnimationSpeed = MathHelper.clamp(data.itemOutlineAnimationSpeed, 0.2F, 4.0F);
        sanitized.trailStripeHeight = MathHelper.clamp(data.trailStripeHeight, 0.2F, 4.0F);
        sanitized.trailLifetimeSeconds = MathHelper.clamp(data.trailLifetimeSeconds, 0.1F, 10.0F);
        sanitized.trailGradientSpeed = MathHelper.clamp(data.trailGradientSpeed, 0.1F, 5.0F);
        sanitized.trailAlpha = MathHelper.clamp(data.trailAlpha, 0.1F, 1.0F);
        sanitized.itemOutlineAlpha = MathHelper.clamp(data.itemOutlineAlpha, 0.05F, 1.0F);
        sanitized.itemOutlineThickness = MathHelper.clamp(data.itemOutlineThickness, 0.5F, 6.0F);
        sanitized.autoAttackRate = MathHelper.clamp(data.autoAttackRate, 1.0F, 20.0F);
        sanitized.autoAttackCircleRadius = MathHelper.clamp(data.autoAttackCircleRadius, 20.0F, 600.0F);
        sanitized.autoAttackMaxDistance = MathHelper.clamp(data.autoAttackMaxDistance, 3.0F, 20.0F);
        sanitized.jumpBoostHeight = MathHelper.clamp(data.jumpBoostHeight, 0.0F, 2.5F);
        sanitized.handFovScale = MathHelper.clamp(data.handFovScale, -1.6F, 1.6F);
        sanitized.handOffsetX = MathHelper.clamp(data.handOffsetX, -1.5F, 1.5F);
        sanitized.handOffsetY = MathHelper.clamp(data.handOffsetY, -1.5F, 1.5F);
        sanitized.skyTopColor = data.skyTopColor & 0xFFFFFF;
        sanitized.skyBottomColor = data.skyBottomColor & 0xFFFFFF;
        sanitized.trailFixedColor = data.trailFixedColor & 0xFFFFFF;
        sanitized.autoAttackCircleColor = data.autoAttackCircleColor & 0xFFFFFF;
        sanitized.itemOutlineSolidColor = data.itemOutlineSolidColor & 0xFFFFFF;
        sanitized.menuLastTab = data.menuLastTab == null ? "RAYS" : data.menuLastTab;
        sanitized.menuScrollOffset = data.menuScrollOffset;
        sanitized.trailType = sanitizeTrailType(data.trailType);
        sanitized.trailOrigin = sanitizeTrailOrigin(data.trailOrigin);
        sanitized.trailColorMode = sanitizeTrailColorMode(data.trailColorMode);
        sanitized.itemOutlineMode = sanitizeItemOutlineMode(data.itemOutlineMode);
        sanitized.autoAttackMode = sanitizeAutoAttackMode(data.autoAttackMode);
        sanitized.autoAttackCircleColorMode = sanitizeCircleColorMode(data.autoAttackCircleColorMode);

        sanitized.speedToggleKey = sanitizeKey(data.speedToggleKey, "key.keyboard.v");
        sanitized.noKnockbackKey = sanitizeKey(data.noKnockbackKey, "key.keyboard.n");
        sanitized.playerEspKey = sanitizeKey(data.playerEspKey, "key.keyboard.h");
        sanitized.playerRaysKey = sanitizeKey(data.playerRaysKey, "key.keyboard.j");
        sanitized.playerListKey = sanitizeKey(data.playerListKey, "key.keyboard.k");
        sanitized.playerTrailsKey = sanitizeKey(data.playerTrailsKey, "key.keyboard.l");
        sanitized.itemOutlineKey = sanitizeKey(data.itemOutlineKey, "key.keyboard.y");
        sanitized.autoAttackKey = sanitizeKey(data.autoAttackKey, "key.keyboard.r");
        sanitized.markTargetKey = sanitizeKey(data.markTargetKey, "key.keyboard.m");
        sanitized.unmarkTargetKey = sanitizeKey(data.unmarkTargetKey, "key.keyboard.u");
        sanitized.markFriendKey = sanitizeKey(data.markFriendKey, "key.keyboard.f");
        sanitized.panicKey = sanitizeKey(data.panicKey, "key.keyboard.p");
        sanitized.menuKey = sanitizeKey(data.menuKey, "key.keyboard.right.shift");

        return sanitized;
    }

    private static String sanitizeKey(String key, String fallback) {
        if (key == null || key.isBlank()) {
            return fallback;
        }

        try {
            InputUtil.fromTranslationKey(key);
            return key;
        } catch (IllegalArgumentException ignored) {
            return fallback;
        }
    }

    private static String sanitizeRayOrigin(String value) {
        if (value == null) {
            return PaprikaClient.RayOrigin.BOTTOM.name();
        }

        try {
            return PaprikaClient.RayOrigin.valueOf(value.toUpperCase(Locale.ROOT)).name();
        } catch (IllegalArgumentException ignored) {
            return PaprikaClient.RayOrigin.BOTTOM.name();
        }
    }

    private static String sanitizeVisualColorMode(String value, PaprikaClient.VisualColorMode fallback) {
        if (value == null) {
            return fallback.name();
        }

        try {
            String normalized = value.toUpperCase(Locale.ROOT);
            if ("VIVID".equals(normalized)) {
                return PaprikaClient.VisualColorMode.NICK.name();
            }
            return PaprikaClient.VisualColorMode.valueOf(normalized).name();
        } catch (IllegalArgumentException ignored) {
            return fallback.name();
        }
    }

    private static String sanitizeOverlayAnchorMode(String value, PaprikaClient.OverlayAnchorMode fallback) {
        if (value == null) {
            return fallback.name();
        }

        try {
            return PaprikaClient.OverlayAnchorMode.valueOf(value.toUpperCase(Locale.ROOT)).name();
        } catch (IllegalArgumentException ignored) {
            return fallback.name();
        }
    }

    private static String sanitizeHudCorner(String value, PaprikaClient.HudCorner fallback) {
        if (value == null || value.isBlank()) {
            return fallback.name();
        }

        try {
            return PaprikaClient.HudCorner.valueOf(value.toUpperCase(Locale.ROOT)).name();
        } catch (IllegalArgumentException ignored) {
            return fallback.name();
        }
    }

    private static String sanitizeHandItemOrientation(String value) {
        if (value == null || value.isBlank()) {
            return PaprikaClient.HandItemOrientation.DEFAULT.name();
        }

        try {
            return PaprikaClient.HandItemOrientation.valueOf(value.toUpperCase(Locale.ROOT)).name();
        } catch (IllegalArgumentException ignored) {
            return PaprikaClient.HandItemOrientation.DEFAULT.name();
        }
    }

    private static String sanitizeTrailType(String value) {
        if (value == null || value.isBlank()) {
            return PaprikaClient.TrailType.THIN_LINE.name();
        }

        try {
            return PaprikaClient.TrailType.valueOf(value.toUpperCase(Locale.ROOT)).name();
        } catch (IllegalArgumentException ignored) {
            return PaprikaClient.TrailType.THIN_LINE.name();
        }
    }

    private static String sanitizeTrailOrigin(String value) {
        if (value == null || value.isBlank()) {
            return PaprikaClient.TrailOrigin.BACK.name();
        }

        try {
            return PaprikaClient.TrailOrigin.valueOf(value.toUpperCase(Locale.ROOT)).name();
        } catch (IllegalArgumentException ignored) {
            return PaprikaClient.TrailOrigin.BACK.name();
        }
    }

    private static String sanitizeTrailColorMode(String value) {
        if (value == null || value.isBlank()) {
            return PaprikaClient.TrailColorMode.NICK.name();
        }

        try {
            return PaprikaClient.TrailColorMode.valueOf(value.toUpperCase(Locale.ROOT)).name();
        } catch (IllegalArgumentException ignored) {
            return PaprikaClient.TrailColorMode.NICK.name();
        }
    }

    private static String sanitizeAutoAttackMode(String value) {
        if (value == null || value.isBlank()) {
            return PaprikaClient.AutoAttackMode.CIRCLE.name();
        }

        try {
            return PaprikaClient.AutoAttackMode.valueOf(value.toUpperCase(Locale.ROOT)).name();
        } catch (IllegalArgumentException ignored) {
            return PaprikaClient.AutoAttackMode.CIRCLE.name();
        }
    }

    private static String sanitizeCircleColorMode(String value) {
        if (value == null || value.isBlank()) {
            return PaprikaClient.CircleColorMode.FIXED.name();
        }

        try {
            return PaprikaClient.CircleColorMode.valueOf(value.toUpperCase(Locale.ROOT)).name();
        } catch (IllegalArgumentException ignored) {
            return PaprikaClient.CircleColorMode.FIXED.name();
        }
    }

    private static List<String> sanitizeFriendNames(List<String> names) {
        List<String> sanitized = new ArrayList<>();
        if (names == null) {
            return sanitized;
        }
        Set<String> seen = new HashSet<>();
        for (String name : names) {
            String cleaned = PaprikaClient.sanitizeFriendName(name);
            if (cleaned == null) continue;
            String key = cleaned.toLowerCase(Locale.ROOT);
            if (!seen.add(key)) continue;
            sanitized.add(cleaned);
        }
        return sanitized;
    }

    private static List<String> sanitizeItemFilterIds(List<String> ids) {
        List<String> sanitized = new ArrayList<>();
        if (ids == null) {
            return sanitized;
        }
        Set<String> seen = new HashSet<>();
        for (String rawId : ids) {
            String cleaned = PaprikaClient.sanitizeItemId(rawId);
            if (cleaned == null) continue;
            String key = cleaned.toLowerCase(Locale.ROOT);
            if (!seen.add(key)) continue;
            sanitized.add(cleaned);
        }
        return sanitized;
    }

    private static String sanitizeItemOutlineMode(String value) {
        if (value == null || value.isBlank()) {
            return PaprikaClient.ItemOutlineMode.ALL.name();
        }

        try {
            return PaprikaClient.ItemOutlineMode.valueOf(value.toUpperCase(Locale.ROOT)).name();
        } catch (IllegalArgumentException ignored) {
            return PaprikaClient.ItemOutlineMode.ALL.name();
        }
    }

    private static String sanitizeItemOutlineColorMode(String value) {
        if (value == null || value.isBlank()) {
            return PaprikaClient.ItemOutlineColorMode.NICK.name();
        }

        try {
            String normalized = value.toUpperCase(Locale.ROOT);
            if ("AVERAGE".equals(normalized) || "AVG".equals(normalized) || "ITEM".equals(normalized)) {
                return PaprikaClient.ItemOutlineColorMode.ITEM_AVERAGE.name();
            }
            return PaprikaClient.ItemOutlineColorMode.valueOf(normalized).name();
        } catch (IllegalArgumentException ignored) {
            return PaprikaClient.ItemOutlineColorMode.NICK.name();
        }
    }

    public static final class Data {
        public boolean speedEnabled = true;
        public boolean noKnockbackEnabled = true;
        public boolean playerEspEnabled = false;
        public boolean playerArmorOverlayEnabled = false;
        public boolean playerRaysEnabled = false;
        public boolean playerListEnabled = false;
        public boolean playerDollEnabled = false;
        public boolean playerTrailsEnabled = false;
        public boolean trailSelfEnabled = true;
        public boolean trailOthersEnabled = true;
        public boolean autoAttackEnabled = false;
        public boolean autoAttackAimEnabled = false;
        public boolean autoAttackExtendReachEnabled = false;
        public float autoAttackAimSmoothing = 0.12F;
        public boolean itemOutlineEnabled = false;
        public boolean jumpBoostEnabled = false;
        public boolean targetHealthOverlayEnabled = false;
        public boolean targetHealthDynamicColorEnabled = true;
        public boolean distanceDisplayEnabled = true;
        public boolean heldItemOverlayEnabled = false;
        public boolean customSkyEnabled = false;
        public boolean skyTopRainbowEnabled = false;
        public boolean skyBottomRainbowEnabled = false;
        public boolean hideHandsWithItemEnabled = false;
        public List<String> friendNames = new ArrayList<>();
        public List<String> itemFilterIds = new ArrayList<>();
        public boolean handItemFlipEnabled = false;
        public boolean rayVisualGlowEnabled = false;
        public boolean espVisualGlowEnabled = false;
        public boolean armorVisualGlowEnabled = false;
        public boolean heldItemVisualGlowEnabled = false;
        public boolean distanceVisualGlowEnabled = false;
        public boolean itemOutlineGlowEnabled = false;
        public boolean autoAttackRequireLineOfSight = true;
        public float rayThickness = 2.0F;
        public float outlineThickness = 1.0F;
        public float rayBottomStartHeight = 2.0F;
        public float rayDistanceTextScale = 0.75F;
        public float armorOverlayScale = 0.75F;
        public float heldItemOverlayScale = 0.75F;
        public float rayAlpha = 1.0F;
        public float armorAlpha = 1.0F;
        public float heldItemAlpha = 1.0F;
        public float distanceAlpha = 1.0F;
        public float targetHealthTextScale = 1.0F;
        public float playerListTextScale = 0.8F;
        public int playerListMaxHeight = 280;
        public float playerListAlpha = 0.7F;
        public int playerListOffsetX = 6;
        public int playerListOffsetY = 6;
        public float playerDollSize = 70.0F;
        public int playerDollOffsetX = 0;
        public int playerDollOffsetY = 0;
        public String rayOrigin = PaprikaClient.RayOrigin.BOTTOM.name();
        public String armorAnchorMode = PaprikaClient.OverlayAnchorMode.ABOVE_PLAYER.name();
        public String heldItemAnchorMode = PaprikaClient.OverlayAnchorMode.ABOVE_PLAYER.name();
        public String distanceAnchorMode = PaprikaClient.OverlayAnchorMode.RAY_MIDDLE.name();
        public String playerDollCorner = PaprikaClient.HudCorner.TOP_LEFT.name();
        public String rayVisualColorMode = PaprikaClient.VisualColorMode.NICK.name();
        public String armorVisualColorMode = PaprikaClient.VisualColorMode.NICK.name();
        public String heldItemVisualColorMode = PaprikaClient.VisualColorMode.NICK.name();
        public String distanceVisualColorMode = PaprikaClient.VisualColorMode.NICK.name();
        public String espVisualColorMode = PaprikaClient.VisualColorMode.NICK.name();
        public String itemOutlineColorMode = PaprikaClient.VisualColorMode.NICK.name();
        public String handItemOrientation = PaprikaClient.HandItemOrientation.DEFAULT.name();
        public float rayVisualSaturationBoost = 1.35F;
        public float rayVisualAnimationSpeed = 1.0F;
        public float armorVisualSaturationBoost = 1.35F;
        public float armorVisualAnimationSpeed = 1.0F;
        public float heldItemVisualSaturationBoost = 1.35F;
        public float heldItemVisualAnimationSpeed = 1.0F;
        public float distanceVisualSaturationBoost = 1.35F;
        public float distanceVisualAnimationSpeed = 1.0F;
        public float espVisualSaturationBoost = 1.35F;
        public float espVisualAnimationSpeed = 1.0F;
        public float itemOutlineSaturationBoost = 1.35F;
        public float itemOutlineAnimationSpeed = 1.0F;
        public float trailStripeHeight = 1.4F;
        public float trailLifetimeSeconds = 2.5F;
        public float trailGradientSpeed = 1.0F;
        public float trailAlpha = 1.0F;
        public float itemOutlineAlpha = 1.0F;
        public float itemOutlineThickness = 1.0F;
        public float autoAttackRate = 6.0F;
        public float autoAttackCircleRadius = 120.0F;
        public float autoAttackMaxDistance = 3.0F;
        public float jumpBoostHeight = 0.5F;
        public float handFovScale = 1.0F;
        public float handOffsetX = 0.0F;
        public float handOffsetY = 0.0F;
        public int skyTopColor = 0x78A7FF;
        public int skyBottomColor = 0xA0C8FF;
        public int trailFixedColor = 0x4CB1FF;
        public int autoAttackCircleColor = 0x4CB1FF;
        public int itemOutlineSolidColor = 0x4CB1FF;
        public String menuLastTab = "RAYS";
        public double menuScrollOffset = 0.0;
        public String trailType = PaprikaClient.TrailType.THIN_LINE.name();
        public String trailOrigin = PaprikaClient.TrailOrigin.BACK.name();
        public String trailColorMode = PaprikaClient.TrailColorMode.NICK.name();
        public String itemOutlineMode = PaprikaClient.ItemOutlineMode.ALL.name();
        public String autoAttackMode = PaprikaClient.AutoAttackMode.CIRCLE.name();
        public String autoAttackCircleColorMode = PaprikaClient.CircleColorMode.FIXED.name();

        public String speedToggleKey = "key.keyboard.v";
        public String noKnockbackKey = "key.keyboard.n";
        public String playerEspKey = "key.keyboard.h";
        public String playerRaysKey = "key.keyboard.j";
        public String playerListKey = "key.keyboard.k";
        public String playerTrailsKey = "key.keyboard.l";
        public String itemOutlineKey = "key.keyboard.y";
        public String autoAttackKey = "key.keyboard.r";
        public String markTargetKey = "key.keyboard.m";
        public String unmarkTargetKey = "key.keyboard.u";
        public String markFriendKey = "key.keyboard.f";
        public String panicKey = "key.keyboard.p";
        public String menuKey = "key.keyboard.right.shift";
    }
}
