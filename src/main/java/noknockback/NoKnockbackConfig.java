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
        sanitized.playerRaysEnabled = data.playerRaysEnabled;
        sanitized.playerListEnabled = data.playerListEnabled;
        sanitized.rayThickness = MathHelper.clamp(data.rayThickness, 0.5F, 8.0F);
        sanitized.outlineThickness = MathHelper.clamp(data.outlineThickness, 0.5F, 6.0F);
        sanitized.rayBottomStartHeight = MathHelper.clamp(data.rayBottomStartHeight, 0.0F, 300.0F);
        sanitized.rayOrigin = sanitizeRayOrigin(data.rayOrigin);

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

    public static final class Data {
        public boolean speedEnabled = true;
        public boolean playerEspEnabled = false;
        public boolean playerRaysEnabled = false;
        public boolean playerListEnabled = false;
        public float rayThickness = 2.0F;
        public float outlineThickness = 1.0F;
        public float rayBottomStartHeight = 2.0F;
        public String rayOrigin = NoKnockbackClient.RayOrigin.BOTTOM.name();

        public String speedToggleKey = "key.keyboard.v";
        public String playerEspKey = "key.keyboard.h";
        public String playerRaysKey = "key.keyboard.j";
        public String playerListKey = "key.keyboard.k";
        public String menuKey = "key.keyboard.right.shift";
    }
}
