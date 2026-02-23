package noknockback;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.CyclingButtonWidget;
import net.minecraft.client.gui.widget.SliderWidget;
import net.minecraft.text.Text;
import net.minecraft.util.math.MathHelper;
import org.jetbrains.annotations.Nullable;

import java.util.Locale;

public class NoKnockbackVisualSettingsScreen extends Screen {
    private static final int PANEL_WIDTH = 380;
    private static final int ROW_HEIGHT = 20;
    private static final int ROW_GAP = 4;
    private static final int TITLE_COLOR = 0xFFD7E6FF;
    private static final int SUBTITLE_COLOR = 0xFF9FB3CF;

    @Nullable
    private final Screen parent;

    @Nullable
    private ButtonWidget armorToggleButton;
    @Nullable
    private ButtonWidget distanceToggleButton;
    @Nullable
    private ButtonWidget heldItemToggleButton;
    @Nullable
    private ButtonWidget glowToggleButton;
    @Nullable
    private CyclingButtonWidget<NoKnockbackClient.VisualColorMode> colorModeButton;
    @Nullable
    private SettingSlider saturationSlider;
    @Nullable
    private SettingSlider animationSpeedSlider;

    public NoKnockbackVisualSettingsScreen(@Nullable Screen parent) {
        super(Text.literal("Visual Settings"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        int left = (this.width - PANEL_WIDTH) / 2;
        int y = Math.max(34, this.height / 2 - 95);

        this.armorToggleButton = this.addDrawableChild(ButtonWidget.builder(Text.empty(), button -> {
            NoKnockbackClient.setPlayerArmorOverlayEnabled(!NoKnockbackClient.isPlayerArmorOverlayEnabled());
            this.refreshLabels();
        }).dimensions(left, y, PANEL_WIDTH, ROW_HEIGHT).build());
        y += ROW_HEIGHT + ROW_GAP;

        this.distanceToggleButton = this.addDrawableChild(ButtonWidget.builder(Text.empty(), button -> {
            NoKnockbackClient.setDistanceDisplayEnabled(!NoKnockbackClient.isDistanceDisplayEnabled());
            this.refreshLabels();
        }).dimensions(left, y, PANEL_WIDTH, ROW_HEIGHT).build());
        y += ROW_HEIGHT + ROW_GAP;

        this.heldItemToggleButton = this.addDrawableChild(ButtonWidget.builder(Text.empty(), button -> {
            NoKnockbackClient.setHeldItemOverlayEnabled(!NoKnockbackClient.isHeldItemOverlayEnabled());
            this.refreshLabels();
        }).dimensions(left, y, PANEL_WIDTH, ROW_HEIGHT).build());
        y += ROW_HEIGHT + ROW_GAP;

        this.glowToggleButton = this.addDrawableChild(ButtonWidget.builder(Text.empty(), button -> {
            NoKnockbackClient.setVisualGlowEnabled(!NoKnockbackClient.isVisualGlowEnabled());
            this.refreshLabels();
        }).dimensions(left, y, PANEL_WIDTH, ROW_HEIGHT).build());
        y += ROW_HEIGHT + ROW_GAP;

        this.colorModeButton = this.addDrawableChild(CyclingButtonWidget.builder(this::colorModeText)
                .values(NoKnockbackClient.VisualColorMode.NICK, NoKnockbackClient.VisualColorMode.VIVID, NoKnockbackClient.VisualColorMode.GRADIENT, NoKnockbackClient.VisualColorMode.RAINBOW)
                .initially(NoKnockbackClient.getVisualColorMode())
                .build(left, y, PANEL_WIDTH, ROW_HEIGHT, Text.literal("Color Mode"), (button, value) -> {
                    NoKnockbackClient.setVisualColorMode(value);
                    this.refreshLabels();
                }));
        y += ROW_HEIGHT + ROW_GAP;

        this.saturationSlider = this.addDrawableChild(new SettingSlider(
                left,
                y,
                PANEL_WIDTH,
                "Color Saturation",
                1.0,
                2.5,
                0.1,
                NoKnockbackClient.getVisualSaturationBoost()
        ) {
            @Override
            protected void onValueChanged(double value) {
                NoKnockbackClient.setVisualSaturationBoost((float) value);
            }
        });
        y += ROW_HEIGHT + ROW_GAP;

        this.animationSpeedSlider = this.addDrawableChild(new SettingSlider(
                left,
                y,
                PANEL_WIDTH,
                "Color Animation Speed",
                0.2,
                4.0,
                0.1,
                NoKnockbackClient.getVisualAnimationSpeed()
        ) {
            @Override
            protected void onValueChanged(double value) {
                NoKnockbackClient.setVisualAnimationSpeed((float) value);
            }
        });

        this.addDrawableChild(ButtonWidget.builder(Text.literal("Back"), button -> this.close())
                .dimensions(left + PANEL_WIDTH - 100, this.height - ROW_HEIGHT - 8, 100, ROW_HEIGHT).build());

        this.refreshLabels();
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        this.renderBackground(context, mouseX, mouseY, delta);
        super.render(context, mouseX, mouseY, delta);

        context.drawCenteredTextWithShadow(this.textRenderer, this.title, this.width / 2, 10, TITLE_COLOR);
        context.drawCenteredTextWithShadow(this.textRenderer, Text.literal("Glow, color styles and equipment details"), this.width / 2, 20, SUBTITLE_COLOR);
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

    private void refreshLabels() {
        if (this.armorToggleButton != null) {
            this.armorToggleButton.setMessage(this.toggleText("Armor Through Walls", NoKnockbackClient.isPlayerArmorOverlayEnabled()));
        }

        if (this.distanceToggleButton != null) {
            this.distanceToggleButton.setMessage(this.toggleText("Show Distances", NoKnockbackClient.isDistanceDisplayEnabled()));
        }

        if (this.heldItemToggleButton != null) {
            this.heldItemToggleButton.setMessage(this.toggleText("Show Held Items", NoKnockbackClient.isHeldItemOverlayEnabled()));
        }

        if (this.glowToggleButton != null) {
            this.glowToggleButton.setMessage(this.toggleText("Glow Effects", NoKnockbackClient.isVisualGlowEnabled()));
        }

        if (this.colorModeButton != null) {
            this.colorModeButton.setValue(NoKnockbackClient.getVisualColorMode());
        }

        if (this.saturationSlider != null) {
            this.saturationSlider.sync(NoKnockbackClient.getVisualSaturationBoost());
        }

        if (this.animationSpeedSlider != null) {
            this.animationSpeedSlider.sync(NoKnockbackClient.getVisualAnimationSpeed());
        }
    }

    private Text toggleText(String name, boolean enabled) {
        return Text.literal(name + ": " + (enabled ? "ON" : "OFF"));
    }

    private Text colorModeText(NoKnockbackClient.VisualColorMode mode) {
        return switch (mode) {
            case NICK -> Text.literal("Nick");
            case VIVID -> Text.literal("Vivid");
            case GRADIENT -> Text.literal("Gradient");
            case RAINBOW -> Text.literal("Rainbow");
        };
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
