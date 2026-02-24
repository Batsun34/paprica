package paprika;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.CyclingButtonWidget;
import net.minecraft.client.gui.widget.SliderWidget;
import net.minecraft.text.Text;
import net.minecraft.util.math.MathHelper;
import org.jetbrains.annotations.Nullable;

import java.util.Locale;

public class PaprikaVisualSettingsScreen extends Screen {
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
    private CyclingButtonWidget<PaprikaClient.VisualColorMode> rayColorModeButton;
    @Nullable
    private CyclingButtonWidget<PaprikaClient.VisualColorMode> armorColorModeButton;
    @Nullable
    private CyclingButtonWidget<PaprikaClient.VisualColorMode> heldItemColorModeButton;
    @Nullable
    private CyclingButtonWidget<PaprikaClient.VisualColorMode> distanceColorModeButton;

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

    public PaprikaVisualSettingsScreen(@Nullable Screen parent) {
        super(Text.literal("Paprika Visual Settings"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        int left = (this.width - PANEL_WIDTH) / 2;
        int y = Math.max(36, this.height / 2 - 250);

        y = this.addStyleSectionTitle("Rays", left, y);
        this.rayGlowButton = this.addDrawableChild(ButtonWidget.builder(Text.empty(), button -> {
            PaprikaClient.setRayVisualGlowEnabled(!PaprikaClient.isRayVisualGlowEnabled());
            this.refreshLabels();
        }).dimensions(left, y, PANEL_WIDTH, ROW_HEIGHT).build());
        y += ROW_HEIGHT + ROW_GAP;
        this.rayColorModeButton = this.addDrawableChild(CyclingButtonWidget.builder(this::colorModeText)
                .values(PaprikaClient.VisualColorMode.NICK, PaprikaClient.VisualColorMode.GRADIENT, PaprikaClient.VisualColorMode.NICK_GRADIENT, PaprikaClient.VisualColorMode.RAINBOW)
                .initially(PaprikaClient.getRayVisualColorMode())
                .build(left, y, PANEL_WIDTH, ROW_HEIGHT, Text.literal("Ray Color Mode"), (button, value) -> {
                    PaprikaClient.setRayVisualColorMode(value);
                    this.refreshLabels();
                }));
        y += ROW_HEIGHT + ROW_GAP;
        this.raySaturationSlider = this.addDrawableChild(new SettingSlider(left, y, PANEL_WIDTH, "Ray Saturation", 1.0, 2.5, 0.1, PaprikaClient.getRayVisualSaturationBoost()) {
            @Override
            protected void onValueChanged(double value) {
                PaprikaClient.setRayVisualSaturationBoost((float) value);
            }
        });
        y += ROW_HEIGHT + ROW_GAP;
        this.raySpeedSlider = this.addDrawableChild(new SettingSlider(left, y, PANEL_WIDTH, "Ray Animation Speed", 0.2, 4.0, 0.1, PaprikaClient.getRayVisualAnimationSpeed()) {
            @Override
            protected void onValueChanged(double value) {
                PaprikaClient.setRayVisualAnimationSpeed((float) value);
            }
        });
        y += ROW_HEIGHT + SECTION_GAP;

        y = this.addStyleSectionTitle("Armor", left, y);
        this.armorGlowButton = this.addDrawableChild(ButtonWidget.builder(Text.empty(), button -> {
            PaprikaClient.setArmorVisualGlowEnabled(!PaprikaClient.isArmorVisualGlowEnabled());
            this.refreshLabels();
        }).dimensions(left, y, PANEL_WIDTH, ROW_HEIGHT).build());
        y += ROW_HEIGHT + ROW_GAP;
        this.armorColorModeButton = this.addDrawableChild(CyclingButtonWidget.builder(this::colorModeText)
                .values(PaprikaClient.VisualColorMode.NICK, PaprikaClient.VisualColorMode.GRADIENT, PaprikaClient.VisualColorMode.NICK_GRADIENT, PaprikaClient.VisualColorMode.RAINBOW)
                .initially(PaprikaClient.getArmorVisualColorMode())
                .build(left, y, PANEL_WIDTH, ROW_HEIGHT, Text.literal("Armor Color Mode"), (button, value) -> {
                    PaprikaClient.setArmorVisualColorMode(value);
                    this.refreshLabels();
                }));
        y += ROW_HEIGHT + ROW_GAP;
        this.armorSaturationSlider = this.addDrawableChild(new SettingSlider(left, y, PANEL_WIDTH, "Armor Saturation", 1.0, 2.5, 0.1, PaprikaClient.getArmorVisualSaturationBoost()) {
            @Override
            protected void onValueChanged(double value) {
                PaprikaClient.setArmorVisualSaturationBoost((float) value);
            }
        });
        y += ROW_HEIGHT + ROW_GAP;
        this.armorSpeedSlider = this.addDrawableChild(new SettingSlider(left, y, PANEL_WIDTH, "Armor Animation Speed", 0.2, 4.0, 0.1, PaprikaClient.getArmorVisualAnimationSpeed()) {
            @Override
            protected void onValueChanged(double value) {
                PaprikaClient.setArmorVisualAnimationSpeed((float) value);
            }
        });
        y += ROW_HEIGHT + SECTION_GAP;

        y = this.addStyleSectionTitle("Held Item", left, y);
        this.heldItemGlowButton = this.addDrawableChild(ButtonWidget.builder(Text.empty(), button -> {
            PaprikaClient.setHeldItemVisualGlowEnabled(!PaprikaClient.isHeldItemVisualGlowEnabled());
            this.refreshLabels();
        }).dimensions(left, y, PANEL_WIDTH, ROW_HEIGHT).build());
        y += ROW_HEIGHT + ROW_GAP;
        this.heldItemColorModeButton = this.addDrawableChild(CyclingButtonWidget.builder(this::colorModeText)
                .values(PaprikaClient.VisualColorMode.NICK, PaprikaClient.VisualColorMode.GRADIENT, PaprikaClient.VisualColorMode.NICK_GRADIENT, PaprikaClient.VisualColorMode.RAINBOW)
                .initially(PaprikaClient.getHeldItemVisualColorMode())
                .build(left, y, PANEL_WIDTH, ROW_HEIGHT, Text.literal("Held Item Color Mode"), (button, value) -> {
                    PaprikaClient.setHeldItemVisualColorMode(value);
                    this.refreshLabels();
                }));
        y += ROW_HEIGHT + ROW_GAP;
        this.heldItemSaturationSlider = this.addDrawableChild(new SettingSlider(left, y, PANEL_WIDTH, "Held Item Saturation", 1.0, 2.5, 0.1, PaprikaClient.getHeldItemVisualSaturationBoost()) {
            @Override
            protected void onValueChanged(double value) {
                PaprikaClient.setHeldItemVisualSaturationBoost((float) value);
            }
        });
        y += ROW_HEIGHT + ROW_GAP;
        this.heldItemSpeedSlider = this.addDrawableChild(new SettingSlider(left, y, PANEL_WIDTH, "Held Item Animation Speed", 0.2, 4.0, 0.1, PaprikaClient.getHeldItemVisualAnimationSpeed()) {
            @Override
            protected void onValueChanged(double value) {
                PaprikaClient.setHeldItemVisualAnimationSpeed((float) value);
            }
        });
        y += ROW_HEIGHT + SECTION_GAP;

        y = this.addStyleSectionTitle("Distance", left, y);
        this.distanceGlowButton = this.addDrawableChild(ButtonWidget.builder(Text.empty(), button -> {
            PaprikaClient.setDistanceVisualGlowEnabled(!PaprikaClient.isDistanceVisualGlowEnabled());
            this.refreshLabels();
        }).dimensions(left, y, PANEL_WIDTH, ROW_HEIGHT).build());
        y += ROW_HEIGHT + ROW_GAP;
        this.distanceColorModeButton = this.addDrawableChild(CyclingButtonWidget.builder(this::colorModeText)
                .values(PaprikaClient.VisualColorMode.NICK, PaprikaClient.VisualColorMode.GRADIENT, PaprikaClient.VisualColorMode.NICK_GRADIENT, PaprikaClient.VisualColorMode.RAINBOW)
                .initially(PaprikaClient.getDistanceVisualColorMode())
                .build(left, y, PANEL_WIDTH, ROW_HEIGHT, Text.literal("Distance Color Mode"), (button, value) -> {
                    PaprikaClient.setDistanceVisualColorMode(value);
                    this.refreshLabels();
                }));
        y += ROW_HEIGHT + ROW_GAP;
        this.distanceSaturationSlider = this.addDrawableChild(new SettingSlider(left, y, PANEL_WIDTH, "Distance Saturation", 1.0, 2.5, 0.1, PaprikaClient.getDistanceVisualSaturationBoost()) {
            @Override
            protected void onValueChanged(double value) {
                PaprikaClient.setDistanceVisualSaturationBoost((float) value);
            }
        });
        y += ROW_HEIGHT + ROW_GAP;
        this.distanceSpeedSlider = this.addDrawableChild(new SettingSlider(left, y, PANEL_WIDTH, "Distance Animation Speed", 0.2, 4.0, 0.1, PaprikaClient.getDistanceVisualAnimationSpeed()) {
            @Override
            protected void onValueChanged(double value) {
                PaprikaClient.setDistanceVisualAnimationSpeed((float) value);
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
            this.rayGlowButton.setMessage(this.toggleText("Ray Glow", PaprikaClient.isRayVisualGlowEnabled()));
        }
        if (this.armorGlowButton != null) {
            this.armorGlowButton.setMessage(this.toggleText("Armor Glow", PaprikaClient.isArmorVisualGlowEnabled()));
        }
        if (this.heldItemGlowButton != null) {
            this.heldItemGlowButton.setMessage(this.toggleText("Held Item Glow", PaprikaClient.isHeldItemVisualGlowEnabled()));
        }
        if (this.distanceGlowButton != null) {
            this.distanceGlowButton.setMessage(this.toggleText("Distance Glow", PaprikaClient.isDistanceVisualGlowEnabled()));
        }

        if (this.rayColorModeButton != null) {
            this.rayColorModeButton.setValue(PaprikaClient.getRayVisualColorMode());
        }
        if (this.armorColorModeButton != null) {
            this.armorColorModeButton.setValue(PaprikaClient.getArmorVisualColorMode());
        }
        if (this.heldItemColorModeButton != null) {
            this.heldItemColorModeButton.setValue(PaprikaClient.getHeldItemVisualColorMode());
        }
        if (this.distanceColorModeButton != null) {
            this.distanceColorModeButton.setValue(PaprikaClient.getDistanceVisualColorMode());
        }

        if (this.raySaturationSlider != null) {
            this.raySaturationSlider.sync(PaprikaClient.getRayVisualSaturationBoost());
        }
        if (this.armorSaturationSlider != null) {
            this.armorSaturationSlider.sync(PaprikaClient.getArmorVisualSaturationBoost());
        }
        if (this.heldItemSaturationSlider != null) {
            this.heldItemSaturationSlider.sync(PaprikaClient.getHeldItemVisualSaturationBoost());
        }
        if (this.distanceSaturationSlider != null) {
            this.distanceSaturationSlider.sync(PaprikaClient.getDistanceVisualSaturationBoost());
        }

        if (this.raySpeedSlider != null) {
            this.raySpeedSlider.sync(PaprikaClient.getRayVisualAnimationSpeed());
        }
        if (this.armorSpeedSlider != null) {
            this.armorSpeedSlider.sync(PaprikaClient.getArmorVisualAnimationSpeed());
        }
        if (this.heldItemSpeedSlider != null) {
            this.heldItemSpeedSlider.sync(PaprikaClient.getHeldItemVisualAnimationSpeed());
        }
        if (this.distanceSpeedSlider != null) {
            this.distanceSpeedSlider.sync(PaprikaClient.getDistanceVisualAnimationSpeed());
        }
    }

    private Text toggleText(String name, boolean enabled) {
        return Text.literal(name + ": " + (enabled ? "ON" : "OFF"));
    }

    private Text colorModeText(PaprikaClient.VisualColorMode mode) {
        return switch (mode) {
            case NICK -> Text.literal("Nick");
            case GRADIENT -> Text.literal("Gradient");
            case NICK_GRADIENT -> Text.literal("Nick Gradient");
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
