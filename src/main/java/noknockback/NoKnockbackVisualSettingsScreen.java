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
    private static final int PANEL_WIDTH = 400;
    private static final int ROW_HEIGHT = 20;
    private static final int ROW_GAP = 4;
    private static final int SECTION_GAP = 8;
    private static final int TITLE_COLOR = 0xFFD7E6FF;
    private static final int SUBTITLE_COLOR = 0xFF9FB3CF;

    @Nullable
    private final Screen parent;

    @Nullable
    private ButtonWidget rayGlowButton;
    @Nullable
    private ButtonWidget armorGlowButton;
    @Nullable
    private ButtonWidget heldItemGlowButton;
    @Nullable
    private ButtonWidget distanceGlowButton;

    @Nullable
    private CyclingButtonWidget<NoKnockbackClient.VisualColorMode> rayColorModeButton;
    @Nullable
    private CyclingButtonWidget<NoKnockbackClient.VisualColorMode> armorColorModeButton;
    @Nullable
    private CyclingButtonWidget<NoKnockbackClient.VisualColorMode> heldItemColorModeButton;
    @Nullable
    private CyclingButtonWidget<NoKnockbackClient.VisualColorMode> distanceColorModeButton;

    @Nullable
    private SettingSlider raySaturationSlider;
    @Nullable
    private SettingSlider armorSaturationSlider;
    @Nullable
    private SettingSlider heldItemSaturationSlider;
    @Nullable
    private SettingSlider distanceSaturationSlider;

    @Nullable
    private SettingSlider raySpeedSlider;
    @Nullable
    private SettingSlider armorSpeedSlider;
    @Nullable
    private SettingSlider heldItemSpeedSlider;
    @Nullable
    private SettingSlider distanceSpeedSlider;

    public NoKnockbackVisualSettingsScreen(@Nullable Screen parent) {
        super(Text.literal("Paprika Visual Settings"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        int left = (this.width - PANEL_WIDTH) / 2;
        int y = Math.max(36, this.height / 2 - 250);

        y = this.addStyleSectionTitle("Rays", left, y);
        this.rayGlowButton = this.addDrawableChild(ButtonWidget.builder(Text.empty(), button -> {
            NoKnockbackClient.setRayVisualGlowEnabled(!NoKnockbackClient.isRayVisualGlowEnabled());
            this.refreshLabels();
        }).dimensions(left, y, PANEL_WIDTH, ROW_HEIGHT).build());
        y += ROW_HEIGHT + ROW_GAP;
        this.rayColorModeButton = this.addDrawableChild(CyclingButtonWidget.builder(this::colorModeText)
                .values(NoKnockbackClient.VisualColorMode.NICK, NoKnockbackClient.VisualColorMode.VIVID, NoKnockbackClient.VisualColorMode.GRADIENT, NoKnockbackClient.VisualColorMode.RAINBOW)
                .initially(NoKnockbackClient.getRayVisualColorMode())
                .build(left, y, PANEL_WIDTH, ROW_HEIGHT, Text.literal("Ray Color Mode"), (button, value) -> {
                    NoKnockbackClient.setRayVisualColorMode(value);
                    this.refreshLabels();
                }));
        y += ROW_HEIGHT + ROW_GAP;
        this.raySaturationSlider = this.addDrawableChild(new SettingSlider(left, y, PANEL_WIDTH, "Ray Saturation", 1.0, 2.5, 0.1, NoKnockbackClient.getRayVisualSaturationBoost()) {
            @Override
            protected void onValueChanged(double value) {
                NoKnockbackClient.setRayVisualSaturationBoost((float) value);
            }
        });
        y += ROW_HEIGHT + ROW_GAP;
        this.raySpeedSlider = this.addDrawableChild(new SettingSlider(left, y, PANEL_WIDTH, "Ray Animation Speed", 0.2, 4.0, 0.1, NoKnockbackClient.getRayVisualAnimationSpeed()) {
            @Override
            protected void onValueChanged(double value) {
                NoKnockbackClient.setRayVisualAnimationSpeed((float) value);
            }
        });
        y += ROW_HEIGHT + SECTION_GAP;

        y = this.addStyleSectionTitle("Armor", left, y);
        this.armorGlowButton = this.addDrawableChild(ButtonWidget.builder(Text.empty(), button -> {
            NoKnockbackClient.setArmorVisualGlowEnabled(!NoKnockbackClient.isArmorVisualGlowEnabled());
            this.refreshLabels();
        }).dimensions(left, y, PANEL_WIDTH, ROW_HEIGHT).build());
        y += ROW_HEIGHT + ROW_GAP;
        this.armorColorModeButton = this.addDrawableChild(CyclingButtonWidget.builder(this::colorModeText)
                .values(NoKnockbackClient.VisualColorMode.NICK, NoKnockbackClient.VisualColorMode.VIVID, NoKnockbackClient.VisualColorMode.GRADIENT, NoKnockbackClient.VisualColorMode.RAINBOW)
                .initially(NoKnockbackClient.getArmorVisualColorMode())
                .build(left, y, PANEL_WIDTH, ROW_HEIGHT, Text.literal("Armor Color Mode"), (button, value) -> {
                    NoKnockbackClient.setArmorVisualColorMode(value);
                    this.refreshLabels();
                }));
        y += ROW_HEIGHT + ROW_GAP;
        this.armorSaturationSlider = this.addDrawableChild(new SettingSlider(left, y, PANEL_WIDTH, "Armor Saturation", 1.0, 2.5, 0.1, NoKnockbackClient.getArmorVisualSaturationBoost()) {
            @Override
            protected void onValueChanged(double value) {
                NoKnockbackClient.setArmorVisualSaturationBoost((float) value);
            }
        });
        y += ROW_HEIGHT + ROW_GAP;
        this.armorSpeedSlider = this.addDrawableChild(new SettingSlider(left, y, PANEL_WIDTH, "Armor Animation Speed", 0.2, 4.0, 0.1, NoKnockbackClient.getArmorVisualAnimationSpeed()) {
            @Override
            protected void onValueChanged(double value) {
                NoKnockbackClient.setArmorVisualAnimationSpeed((float) value);
            }
        });
        y += ROW_HEIGHT + SECTION_GAP;

        y = this.addStyleSectionTitle("Held Item", left, y);
        this.heldItemGlowButton = this.addDrawableChild(ButtonWidget.builder(Text.empty(), button -> {
            NoKnockbackClient.setHeldItemVisualGlowEnabled(!NoKnockbackClient.isHeldItemVisualGlowEnabled());
            this.refreshLabels();
        }).dimensions(left, y, PANEL_WIDTH, ROW_HEIGHT).build());
        y += ROW_HEIGHT + ROW_GAP;
        this.heldItemColorModeButton = this.addDrawableChild(CyclingButtonWidget.builder(this::colorModeText)
                .values(NoKnockbackClient.VisualColorMode.NICK, NoKnockbackClient.VisualColorMode.VIVID, NoKnockbackClient.VisualColorMode.GRADIENT, NoKnockbackClient.VisualColorMode.RAINBOW)
                .initially(NoKnockbackClient.getHeldItemVisualColorMode())
                .build(left, y, PANEL_WIDTH, ROW_HEIGHT, Text.literal("Held Item Color Mode"), (button, value) -> {
                    NoKnockbackClient.setHeldItemVisualColorMode(value);
                    this.refreshLabels();
                }));
        y += ROW_HEIGHT + ROW_GAP;
        this.heldItemSaturationSlider = this.addDrawableChild(new SettingSlider(left, y, PANEL_WIDTH, "Held Item Saturation", 1.0, 2.5, 0.1, NoKnockbackClient.getHeldItemVisualSaturationBoost()) {
            @Override
            protected void onValueChanged(double value) {
                NoKnockbackClient.setHeldItemVisualSaturationBoost((float) value);
            }
        });
        y += ROW_HEIGHT + ROW_GAP;
        this.heldItemSpeedSlider = this.addDrawableChild(new SettingSlider(left, y, PANEL_WIDTH, "Held Item Animation Speed", 0.2, 4.0, 0.1, NoKnockbackClient.getHeldItemVisualAnimationSpeed()) {
            @Override
            protected void onValueChanged(double value) {
                NoKnockbackClient.setHeldItemVisualAnimationSpeed((float) value);
            }
        });
        y += ROW_HEIGHT + SECTION_GAP;

        y = this.addStyleSectionTitle("Distance", left, y);
        this.distanceGlowButton = this.addDrawableChild(ButtonWidget.builder(Text.empty(), button -> {
            NoKnockbackClient.setDistanceVisualGlowEnabled(!NoKnockbackClient.isDistanceVisualGlowEnabled());
            this.refreshLabels();
        }).dimensions(left, y, PANEL_WIDTH, ROW_HEIGHT).build());
        y += ROW_HEIGHT + ROW_GAP;
        this.distanceColorModeButton = this.addDrawableChild(CyclingButtonWidget.builder(this::colorModeText)
                .values(NoKnockbackClient.VisualColorMode.NICK, NoKnockbackClient.VisualColorMode.VIVID, NoKnockbackClient.VisualColorMode.GRADIENT, NoKnockbackClient.VisualColorMode.RAINBOW)
                .initially(NoKnockbackClient.getDistanceVisualColorMode())
                .build(left, y, PANEL_WIDTH, ROW_HEIGHT, Text.literal("Distance Color Mode"), (button, value) -> {
                    NoKnockbackClient.setDistanceVisualColorMode(value);
                    this.refreshLabels();
                }));
        y += ROW_HEIGHT + ROW_GAP;
        this.distanceSaturationSlider = this.addDrawableChild(new SettingSlider(left, y, PANEL_WIDTH, "Distance Saturation", 1.0, 2.5, 0.1, NoKnockbackClient.getDistanceVisualSaturationBoost()) {
            @Override
            protected void onValueChanged(double value) {
                NoKnockbackClient.setDistanceVisualSaturationBoost((float) value);
            }
        });
        y += ROW_HEIGHT + ROW_GAP;
        this.distanceSpeedSlider = this.addDrawableChild(new SettingSlider(left, y, PANEL_WIDTH, "Distance Animation Speed", 0.2, 4.0, 0.1, NoKnockbackClient.getDistanceVisualAnimationSpeed()) {
            @Override
            protected void onValueChanged(double value) {
                NoKnockbackClient.setDistanceVisualAnimationSpeed((float) value);
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
        context.drawCenteredTextWithShadow(this.textRenderer, Text.literal("Separate visual profile for each function"), this.width / 2, 20, SUBTITLE_COLOR);
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

    private int addStyleSectionTitle(String title, int left, int y) {
        this.addDrawable((context, mouseX, mouseY, delta) -> context.drawTextWithShadow(this.textRenderer, Text.literal(title), left, y, 0xFFE5EEF9));
        return y + 12;
    }

    private void refreshLabels() {
        if (this.rayGlowButton != null) {
            this.rayGlowButton.setMessage(this.toggleText("Ray Glow", NoKnockbackClient.isRayVisualGlowEnabled()));
        }
        if (this.armorGlowButton != null) {
            this.armorGlowButton.setMessage(this.toggleText("Armor Glow", NoKnockbackClient.isArmorVisualGlowEnabled()));
        }
        if (this.heldItemGlowButton != null) {
            this.heldItemGlowButton.setMessage(this.toggleText("Held Item Glow", NoKnockbackClient.isHeldItemVisualGlowEnabled()));
        }
        if (this.distanceGlowButton != null) {
            this.distanceGlowButton.setMessage(this.toggleText("Distance Glow", NoKnockbackClient.isDistanceVisualGlowEnabled()));
        }

        if (this.rayColorModeButton != null) {
            this.rayColorModeButton.setValue(NoKnockbackClient.getRayVisualColorMode());
        }
        if (this.armorColorModeButton != null) {
            this.armorColorModeButton.setValue(NoKnockbackClient.getArmorVisualColorMode());
        }
        if (this.heldItemColorModeButton != null) {
            this.heldItemColorModeButton.setValue(NoKnockbackClient.getHeldItemVisualColorMode());
        }
        if (this.distanceColorModeButton != null) {
            this.distanceColorModeButton.setValue(NoKnockbackClient.getDistanceVisualColorMode());
        }

        if (this.raySaturationSlider != null) {
            this.raySaturationSlider.sync(NoKnockbackClient.getRayVisualSaturationBoost());
        }
        if (this.armorSaturationSlider != null) {
            this.armorSaturationSlider.sync(NoKnockbackClient.getArmorVisualSaturationBoost());
        }
        if (this.heldItemSaturationSlider != null) {
            this.heldItemSaturationSlider.sync(NoKnockbackClient.getHeldItemVisualSaturationBoost());
        }
        if (this.distanceSaturationSlider != null) {
            this.distanceSaturationSlider.sync(NoKnockbackClient.getDistanceVisualSaturationBoost());
        }

        if (this.raySpeedSlider != null) {
            this.raySpeedSlider.sync(NoKnockbackClient.getRayVisualAnimationSpeed());
        }
        if (this.armorSpeedSlider != null) {
            this.armorSpeedSlider.sync(NoKnockbackClient.getArmorVisualAnimationSpeed());
        }
        if (this.heldItemSpeedSlider != null) {
            this.heldItemSpeedSlider.sync(NoKnockbackClient.getHeldItemVisualAnimationSpeed());
        }
        if (this.distanceSpeedSlider != null) {
            this.distanceSpeedSlider.sync(NoKnockbackClient.getDistanceVisualAnimationSpeed());
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
