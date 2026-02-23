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
    private static final Path CONFIG_PATH = FabricLoader.getInstance().getConfigDir().resolve("noknockback.json");

    private NoKnockbackConfig() {
    }

    public static Data load() {
        Data defaults = new Data();

        if (!Files.exists(CONFIG_PATH)) {
            save(defaults);
            return defaults;
        }

        try (Reader reader = Files.newBufferedReader(CONFIG_PATH, StandardCharsets.UTF_8)) {
            Data loaded = GSON.fromJson(reader, Data.class);
            return sanitize(loaded == null ? defaults : loaded);
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
        sanitized.playerEspEnabled = data.playerEspEnabled;
        sanitized.playerArmorOverlayEnabled = data.playerArmorOverlayEnabled;
        sanitized.playerRaysEnabled = data.playerRaysEnabled;
        sanitized.playerListEnabled = data.playerListEnabled;
        sanitized.targetHealthOverlayEnabled = data.targetHealthOverlayEnabled;
        sanitized.targetHealthDynamicColorEnabled = data.targetHealthDynamicColorEnabled;
        sanitized.distanceDisplayEnabled = data.distanceDisplayEnabled;
        sanitized.heldItemOverlayEnabled = data.heldItemOverlayEnabled;
        sanitized.visualGlowEnabled = data.visualGlowEnabled;
        sanitized.rayThickness = MathHelper.clamp(data.rayThickness, 0.5F, 8.0F);
        sanitized.outlineThickness = MathHelper.clamp(data.outlineThickness, 0.5F, 6.0F);
        sanitized.rayBottomStartHeight = MathHelper.clamp(data.rayBottomStartHeight, 0.0F, 300.0F);
        sanitized.rayDistanceTextScale = MathHelper.clamp(data.rayDistanceTextScale, 0.5F, 2.0F);
        sanitized.targetHealthTextScale = MathHelper.clamp(data.targetHealthTextScale, 0.5F, 2.0F);
        sanitized.playerListTextScale = MathHelper.clamp(data.playerListTextScale, 0.1F, 2.0F);
        sanitized.playerListMaxHeight = MathHelper.clamp(data.playerListMaxHeight, 40, 4096);
        sanitized.playerListAlpha = MathHelper.clamp(data.playerListAlpha, 0.1F, 1.0F);
        sanitized.playerListOffsetX = MathHelper.clamp(data.playerListOffsetX, 0, 4096);
        sanitized.playerListOffsetY = MathHelper.clamp(data.playerListOffsetY, 0, 4096);
        sanitized.rayOrigin = sanitizeRayOrigin(data.rayOrigin);
        sanitized.visualColorMode = sanitizeVisualColorMode(data.visualColorMode);
        sanitized.visualSaturationBoost = MathHelper.clamp(data.visualSaturationBoost, 1.0F, 2.5F);
        sanitized.visualAnimationSpeed = MathHelper.clamp(data.visualAnimationSpeed, 0.2F, 4.0F);

        sanitized.speedToggleKey = sanitizeKey(data.speedToggleKey, "key.keyboard.v");
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

    private static String sanitizeVisualColorMode(String value) {
        if (value == null) {
            return NoKnockbackClient.VisualColorMode.VIVID.name();
        }

        try {
            return NoKnockbackClient.VisualColorMode.valueOf(value.toUpperCase(Locale.ROOT)).name();
        } catch (IllegalArgumentException ignored) {
            return NoKnockbackClient.VisualColorMode.VIVID.name();
        }
    }

    public static final class Data {
        public boolean speedEnabled = true;
        public boolean playerEspEnabled = false;
        public boolean playerArmorOverlayEnabled = false;
        public boolean playerRaysEnabled = false;
        public boolean playerListEnabled = false;
        public boolean targetHealthOverlayEnabled = false;
        public boolean targetHealthDynamicColorEnabled = true;
        public boolean distanceDisplayEnabled = true;
        public boolean heldItemOverlayEnabled = false;
        public boolean visualGlowEnabled = false;
        public float rayThickness = 2.0F;
        public float outlineThickness = 1.0F;
        public float rayBottomStartHeight = 2.0F;
        public float rayDistanceTextScale = 0.75F;
        public float targetHealthTextScale = 1.0F;
        public float playerListTextScale = 0.8F;
        public int playerListMaxHeight = 280;
        public float playerListAlpha = 0.7F;
        public int playerListOffsetX = 6;
        public int playerListOffsetY = 6;
        public String rayOrigin = NoKnockbackClient.RayOrigin.BOTTOM.name();
        public String visualColorMode = NoKnockbackClient.VisualColorMode.VIVID.name();
        public float visualSaturationBoost = 1.35F;
        public float visualAnimationSpeed = 1.0F;

        public String speedToggleKey = "key.keyboard.v";
        public String playerEspKey = "key.keyboard.h";
        public String playerRaysKey = "key.keyboard.j";
        public String playerListKey = "key.keyboard.k";
        public String menuKey = "key.keyboard.right.shift";
    }
}
