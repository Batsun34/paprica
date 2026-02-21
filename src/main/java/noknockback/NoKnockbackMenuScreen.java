package noknockback;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.CyclingButtonWidget;
import net.minecraft.client.gui.widget.SliderWidget;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.Text;
import net.minecraft.util.math.MathHelper;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class NoKnockbackMenuScreen extends Screen {
    private static final int PANEL_WIDTH = 420;
    private static final int ROW_HEIGHT = 20;
    private static final int SECTION_GAP = 8;
    private static final int ROW_GAP = 4;
    private static final int LEFT_COLUMN_WIDTH = 220;
    private static final int RIGHT_COLUMN_WIDTH = PANEL_WIDTH - LEFT_COLUMN_WIDTH - 8;
    private static final int TITLE_COLOR = 0xFFD7E6FF;
    private static final int SUBTITLE_COLOR = 0xFF9FB3CF;

    @Nullable
    private final Screen parent;
    @Nullable
    private KeyBinding waitingForKey;

    private final Map<KeyBinding, ButtonWidget> keyButtons = new LinkedHashMap<>();
    private final List<SectionTitle> sectionTitles = new ArrayList<>();

    @Nullable
    private ButtonWidget speedToggleButton;
    @Nullable
    private ButtonWidget playerEspToggleButton;
    @Nullable
    private ButtonWidget playerRaysToggleButton;
    @Nullable
    private ButtonWidget playerListToggleButton;
    @Nullable
    private CyclingButtonWidget<NoKnockbackClient.RayOrigin> rayOriginButton;
    @Nullable
    private SettingSlider rayThicknessSlider;
    @Nullable
    private SettingSlider outlineThicknessSlider;

    public NoKnockbackMenuScreen(@Nullable Screen parent) {
        super(Text.literal("NoKnockback Settings"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        this.sectionTitles.clear();
        this.keyButtons.clear();
        this.waitingForKey = null;

        int left = (this.width - PANEL_WIDTH) / 2;
        int rightColumnX = left + LEFT_COLUMN_WIDTH + 8;
        int y = 26;

        this.sectionTitles.add(new SectionTitle("Movement", y));
        y += 12;
        this.speedToggleButton = this.addDrawableChild(ButtonWidget.builder(Text.empty(), button -> {
            NoKnockbackClient.setSpeedEnabled(!NoKnockbackClient.isSpeedEnabled());
            this.refreshLabels();
        }).dimensions(left, y, LEFT_COLUMN_WIDTH, ROW_HEIGHT).build());
        this.createKeyBindButton(NoKnockbackClient.getSpeedToggleKeyBinding(), rightColumnX, y, RIGHT_COLUMN_WIDTH);
        y += ROW_HEIGHT + SECTION_GAP;

        this.sectionTitles.add(new SectionTitle("ESP", y));
        y += 12;
        this.playerEspToggleButton = this.addDrawableChild(ButtonWidget.builder(Text.empty(), button -> {
            NoKnockbackClient.setPlayerEspEnabled(!NoKnockbackClient.isPlayerEspEnabled());
            this.refreshLabels();
        }).dimensions(left, y, LEFT_COLUMN_WIDTH, ROW_HEIGHT).build());
        this.createKeyBindButton(NoKnockbackClient.getPlayerEspKeyBinding(), rightColumnX, y, RIGHT_COLUMN_WIDTH);
        y += ROW_HEIGHT + ROW_GAP;

        this.outlineThicknessSlider = this.addDrawableChild(new SettingSlider(
                left,
                y,
                PANEL_WIDTH,
                "Outline Thickness",
                0.5,
                6.0,
                0.1,
                NoKnockbackClient.getOutlineThickness()
        ) {
            @Override
            protected void onValueChanged(double value) {
                NoKnockbackClient.setOutlineThickness((float) value);
            }
        });
        y += ROW_HEIGHT + SECTION_GAP;

        this.sectionTitles.add(new SectionTitle("Rays", y));
        y += 12;
        this.playerRaysToggleButton = this.addDrawableChild(ButtonWidget.builder(Text.empty(), button -> {
            NoKnockbackClient.setPlayerRaysEnabled(!NoKnockbackClient.isPlayerRaysEnabled());
            this.refreshLabels();
        }).dimensions(left, y, LEFT_COLUMN_WIDTH, ROW_HEIGHT).build());
        this.createKeyBindButton(NoKnockbackClient.getPlayerRaysKeyBinding(), rightColumnX, y, RIGHT_COLUMN_WIDTH);
        y += ROW_HEIGHT + ROW_GAP;

        this.rayOriginButton = this.addDrawableChild(CyclingButtonWidget.builder(this::rayOriginText)
                .values(NoKnockbackClient.RayOrigin.BOTTOM, NoKnockbackClient.RayOrigin.CENTER)
                .initially(NoKnockbackClient.getRayOrigin())
                .build(left, y, PANEL_WIDTH, ROW_HEIGHT, Text.literal("Ray Origin"), (button, value) -> NoKnockbackClient.setRayOrigin(value)));
        y += ROW_HEIGHT + ROW_GAP;

        this.rayThicknessSlider = this.addDrawableChild(new SettingSlider(
                left,
                y,
                PANEL_WIDTH,
                "Ray Thickness",
                0.5,
                8.0,
                0.1,
                NoKnockbackClient.getRayThickness()
        ) {
            @Override
            protected void onValueChanged(double value) {
                NoKnockbackClient.setRayThickness((float) value);
            }
        });
        y += ROW_HEIGHT + SECTION_GAP;

        this.sectionTitles.add(new SectionTitle("Player List", y));
        y += 12;
        this.playerListToggleButton = this.addDrawableChild(ButtonWidget.builder(Text.empty(), button -> {
            NoKnockbackClient.setPlayerListEnabled(!NoKnockbackClient.isPlayerListEnabled());
            this.refreshLabels();
        }).dimensions(left, y, LEFT_COLUMN_WIDTH, ROW_HEIGHT).build());
        this.createKeyBindButton(NoKnockbackClient.getPlayerListKeyBinding(), rightColumnX, y, RIGHT_COLUMN_WIDTH);
        y += ROW_HEIGHT + SECTION_GAP;

        this.sectionTitles.add(new SectionTitle("Menu", y));
        y += 12;
        this.addDrawableChild(ButtonWidget.builder(Text.literal("Open Settings Menu"), button -> {
        }).dimensions(left, y, LEFT_COLUMN_WIDTH, ROW_HEIGHT).build()).active = false;
        this.createKeyBindButton(NoKnockbackClient.getOpenMenuKeyBinding(), rightColumnX, y, RIGHT_COLUMN_WIDTH);
        y += ROW_HEIGHT + SECTION_GAP;

        this.addDrawableChild(ButtonWidget.builder(Text.literal("Close"), button -> this.close())
                .dimensions(left + PANEL_WIDTH - 100, y, 100, ROW_HEIGHT).build());

        this.refreshLabels();
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        this.renderBackground(context, mouseX, mouseY, delta);
        super.render(context, mouseX, mouseY, delta);

        context.drawCenteredTextWithShadow(this.textRenderer, this.title, this.width / 2, 8, TITLE_COLOR);
        context.drawCenteredTextWithShadow(this.textRenderer, Text.literal("Toggle features, edit hotkeys and adjust parameters"), this.width / 2, 18, SUBTITLE_COLOR);

        int left = (this.width - PANEL_WIDTH) / 2;
        for (SectionTitle sectionTitle : this.sectionTitles) {
            context.drawTextWithShadow(this.textRenderer, sectionTitle.text(), left, sectionTitle.y(), 0xFFE5EEF9);
        }

        if (this.waitingForKey != null) {
            context.drawCenteredTextWithShadow(
                    this.textRenderer,
                    Text.literal("Press any key (or mouse button), ESC to cancel"),
                    this.width / 2,
                    this.height - 16,
                    0xFFFFFF99
            );
        }
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (this.waitingForKey != null) {
            if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
                this.waitingForKey = null;
                this.refreshLabels();
                return true;
            }

            InputUtil.Key key = keyCode == InputUtil.UNKNOWN_KEY.getCode()
                    ? InputUtil.Type.SCANCODE.createFromCode(scanCode)
                    : InputUtil.Type.KEYSYM.createFromCode(keyCode);
            this.applyKeyBinding(this.waitingForKey, key);
            return true;
        }

        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (this.waitingForKey != null) {
            this.applyKeyBinding(this.waitingForKey, InputUtil.Type.MOUSE.createFromCode(button));
            return true;
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public void close() {
        if (this.client != null) {
            this.client.setScreen(this.parent);
        }
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    private void applyKeyBinding(KeyBinding binding, InputUtil.Key key) {
        binding.setBoundKey(key);
        KeyBinding.updateKeysByCode();
        if (this.client != null) {
            this.client.options.write();
        }

        this.waitingForKey = null;
        this.refreshLabels();
    }

    private ButtonWidget createKeyBindButton(KeyBinding binding, int x, int y, int width) {
        ButtonWidget button = this.addDrawableChild(ButtonWidget.builder(Text.empty(), widget -> {
            this.waitingForKey = binding;
            this.refreshLabels();
        }).dimensions(x, y, width, ROW_HEIGHT).build());
        this.keyButtons.put(binding, button);
        return button;
    }

    private void refreshLabels() {
        if (this.speedToggleButton != null) {
            this.speedToggleButton.setMessage(this.toggleText("Speed", NoKnockbackClient.isSpeedEnabled()));
        }

        if (this.playerEspToggleButton != null) {
            this.playerEspToggleButton.setMessage(this.toggleText("Player ESP", NoKnockbackClient.isPlayerEspEnabled()));
        }

        if (this.playerRaysToggleButton != null) {
            this.playerRaysToggleButton.setMessage(this.toggleText("Player Rays", NoKnockbackClient.isPlayerRaysEnabled()));
        }

        if (this.playerListToggleButton != null) {
            this.playerListToggleButton.setMessage(this.toggleText("Player List", NoKnockbackClient.isPlayerListEnabled()));
        }

        if (this.rayOriginButton != null) {
            this.rayOriginButton.setValue(NoKnockbackClient.getRayOrigin());
        }

        if (this.rayThicknessSlider != null) {
            this.rayThicknessSlider.sync(NoKnockbackClient.getRayThickness());
        }

        if (this.outlineThicknessSlider != null) {
            this.outlineThicknessSlider.sync(NoKnockbackClient.getOutlineThickness());
        }

        for (Map.Entry<KeyBinding, ButtonWidget> entry : this.keyButtons.entrySet()) {
            KeyBinding binding = entry.getKey();
            ButtonWidget button = entry.getValue();
            if (this.waitingForKey == binding) {
                button.setMessage(Text.literal("[Press key]"));
            } else {
                button.setMessage(Text.literal("Key: ").append(binding.getBoundKeyLocalizedText()));
            }
        }
    }

    private Text toggleText(String name, boolean enabled) {
        return Text.literal(name + ": " + (enabled ? "ON" : "OFF"));
    }

    private Text rayOriginText(NoKnockbackClient.RayOrigin origin) {
        return origin == NoKnockbackClient.RayOrigin.CENTER ? Text.literal("Center") : Text.literal("Bottom");
    }

    private record SectionTitle(String text, int y) {
    }

    private abstract static class SettingSlider extends SliderWidget {
        private final String label;
        private final double min;
        private final double max;
        private final double step;

        protected SettingSlider(int x, int y, int width, String label, double min, double max, double step, double currentValue) {
            super(x, y, width, ROW_HEIGHT, Text.empty(), normalize(currentValue, min, max));
            this.label = label;
            this.min = min;
            this.max = max;
            this.step = step;
            this.sync(currentValue);
        }

        void sync(double currentValue) {
            this.value = normalize(currentValue, this.min, this.max);
            this.updateMessage();
        }

        @Override
        protected void updateMessage() {
            this.setMessage(Text.literal(this.label + ": " + this.format(this.current())));
        }

        @Override
        protected void applyValue() {
            double newValue = this.current();
            this.value = normalize(newValue, this.min, this.max);
            this.onValueChanged(newValue);
            this.updateMessage();
        }

        protected abstract void onValueChanged(double value);

        private double current() {
            double raw = this.min + (this.max - this.min) * this.value;
            double snapped = Math.round((raw - this.min) / this.step) * this.step + this.min;
            return MathHelper.clamp(snapped, this.min, this.max);
        }

        private String format(double value) {
            if (this.step >= 1.0) {
                return Integer.toString((int) Math.round(value));
            }

            return String.format(Locale.ROOT, "%.1f", value);
        }

        private static double normalize(double value, double min, double max) {
            if (max - min <= 0.0) {
                return 0.0;
            }

            return MathHelper.clamp((value - min) / (max - min), 0.0, 1.0);
        }
    }
}
