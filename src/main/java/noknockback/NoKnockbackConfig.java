package noknockback;

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
import java.util.Locale;

public final class NoKnockbackConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path CONFIG_PATH = FabricLoader.getInstance().getConfigDir().resolve("paprika.json");
    private static final Path LEGACY_CONFIG_PATH = FabricLoader.getInstance().getConfigDir().resolve("noknockback.json");

    private NoKnockbackConfig() {
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
        sanitized.targetHealthOverlayEnabled = data.targetHealthOverlayEnabled;
        sanitized.targetHealthDynamicColorEnabled = data.targetHealthDynamicColorEnabled;
        sanitized.distanceDisplayEnabled = data.distanceDisplayEnabled;
        sanitized.heldItemOverlayEnabled = data.heldItemOverlayEnabled;
        sanitized.rayVisualGlowEnabled = data.rayVisualGlowEnabled;
        sanitized.armorVisualGlowEnabled = data.armorVisualGlowEnabled;
        sanitized.heldItemVisualGlowEnabled = data.heldItemVisualGlowEnabled;
        sanitized.distanceVisualGlowEnabled = data.distanceVisualGlowEnabled;
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
        sanitized.rayOrigin = sanitizeRayOrigin(data.rayOrigin);
        sanitized.armorAnchorMode = sanitizeOverlayAnchorMode(data.armorAnchorMode, NoKnockbackClient.OverlayAnchorMode.ABOVE_PLAYER);
        sanitized.heldItemAnchorMode = sanitizeOverlayAnchorMode(data.heldItemAnchorMode, NoKnockbackClient.OverlayAnchorMode.ABOVE_PLAYER);
        sanitized.distanceAnchorMode = sanitizeOverlayAnchorMode(data.distanceAnchorMode, NoKnockbackClient.OverlayAnchorMode.RAY_MIDDLE);
        sanitized.rayVisualColorMode = sanitizeVisualColorMode(data.rayVisualColorMode, NoKnockbackClient.VisualColorMode.VIVID);
        sanitized.armorVisualColorMode = sanitizeVisualColorMode(data.armorVisualColorMode, NoKnockbackClient.VisualColorMode.VIVID);
        sanitized.heldItemVisualColorMode = sanitizeVisualColorMode(data.heldItemVisualColorMode, NoKnockbackClient.VisualColorMode.VIVID);
        sanitized.distanceVisualColorMode = sanitizeVisualColorMode(data.distanceVisualColorMode, NoKnockbackClient.VisualColorMode.VIVID);
        sanitized.rayVisualSaturationBoost = MathHelper.clamp(data.rayVisualSaturationBoost, 1.0F, 2.5F);
        sanitized.rayVisualAnimationSpeed = MathHelper.clamp(data.rayVisualAnimationSpeed, 0.2F, 4.0F);
        sanitized.armorVisualSaturationBoost = MathHelper.clamp(data.armorVisualSaturationBoost, 1.0F, 2.5F);
        sanitized.armorVisualAnimationSpeed = MathHelper.clamp(data.armorVisualAnimationSpeed, 0.2F, 4.0F);
        sanitized.heldItemVisualSaturationBoost = MathHelper.clamp(data.heldItemVisualSaturationBoost, 1.0F, 2.5F);
        sanitized.heldItemVisualAnimationSpeed = MathHelper.clamp(data.heldItemVisualAnimationSpeed, 0.2F, 4.0F);
        sanitized.distanceVisualSaturationBoost = MathHelper.clamp(data.distanceVisualSaturationBoost, 1.0F, 2.5F);
        sanitized.distanceVisualAnimationSpeed = MathHelper.clamp(data.distanceVisualAnimationSpeed, 0.2F, 4.0F);

        sanitized.speedToggleKey = sanitizeKey(data.speedToggleKey, "key.keyboard.v");
        sanitized.noKnockbackKey = sanitizeKey(data.noKnockbackKey, "key.keyboard.n");
        sanitized.playerEspKey = sanitizeKey(data.playerEspKey, "key.keyboard.h");
        sanitized.playerRaysKey = sanitizeKey(data.playerRaysKey, "key.keyboard.j");
        sanitized.playerListKey = sanitizeKey(data.playerListKey, "key.keyboard.k");
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
            return NoKnockbackClient.RayOrigin.BOTTOM.name();
        }

        try {
            return NoKnockbackClient.RayOrigin.valueOf(value.toUpperCase(Locale.ROOT)).name();
        } catch (IllegalArgumentException ignored) {
            return NoKnockbackClient.RayOrigin.BOTTOM.name();
        }
    }

    private static String sanitizeVisualColorMode(String value, NoKnockbackClient.VisualColorMode fallback) {
        if (value == null) {
            return fallback.name();
        }

        try {
            return NoKnockbackClient.VisualColorMode.valueOf(value.toUpperCase(Locale.ROOT)).name();
        } catch (IllegalArgumentException ignored) {
            return fallback.name();
        }
    }

    private static String sanitizeOverlayAnchorMode(String value, NoKnockbackClient.OverlayAnchorMode fallback) {
        if (value == null) {
            return fallback.name();
        }

        try {
            return NoKnockbackClient.OverlayAnchorMode.valueOf(value.toUpperCase(Locale.ROOT)).name();
        } catch (IllegalArgumentException ignored) {
            return fallback.name();
        }
    }

    public static final class Data {
        public boolean speedEnabled = true;
        public boolean noKnockbackEnabled = true;
        public boolean playerEspEnabled = false;
        public boolean playerArmorOverlayEnabled = false;
        public boolean playerRaysEnabled = false;
        public boolean playerListEnabled = false;
        public boolean targetHealthOverlayEnabled = false;
        public boolean targetHealthDynamicColorEnabled = true;
        public boolean distanceDisplayEnabled = true;
        public boolean heldItemOverlayEnabled = false;
        public boolean rayVisualGlowEnabled = false;
        public boolean armorVisualGlowEnabled = false;
        public boolean heldItemVisualGlowEnabled = false;
        public boolean distanceVisualGlowEnabled = false;
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
        public String rayOrigin = NoKnockbackClient.RayOrigin.BOTTOM.name();
        public String armorAnchorMode = NoKnockbackClient.OverlayAnchorMode.ABOVE_PLAYER.name();
        public String heldItemAnchorMode = NoKnockbackClient.OverlayAnchorMode.ABOVE_PLAYER.name();
        public String distanceAnchorMode = NoKnockbackClient.OverlayAnchorMode.RAY_MIDDLE.name();
        public String rayVisualColorMode = NoKnockbackClient.VisualColorMode.VIVID.name();
        public String armorVisualColorMode = NoKnockbackClient.VisualColorMode.VIVID.name();
        public String heldItemVisualColorMode = NoKnockbackClient.VisualColorMode.VIVID.name();
        public String distanceVisualColorMode = NoKnockbackClient.VisualColorMode.VIVID.name();
        public float rayVisualSaturationBoost = 1.35F;
        public float rayVisualAnimationSpeed = 1.0F;
        public float armorVisualSaturationBoost = 1.35F;
        public float armorVisualAnimationSpeed = 1.0F;
        public float heldItemVisualSaturationBoost = 1.35F;
        public float heldItemVisualAnimationSpeed = 1.0F;
        public float distanceVisualSaturationBoost = 1.35F;
        public float distanceVisualAnimationSpeed = 1.0F;

        public String speedToggleKey = "key.keyboard.v";
        public String noKnockbackKey = "key.keyboard.n";
        public String playerEspKey = "key.keyboard.h";
        public String playerRaysKey = "key.keyboard.j";
        public String playerListKey = "key.keyboard.k";
        public String menuKey = "key.keyboard.right.shift";
    }
}
