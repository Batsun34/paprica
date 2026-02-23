package noknockback;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.ClickableWidget;
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
    private static final int SCROLL_STEP = 20;
    private static final int FOOTER_HEIGHT = 32;
    private static final int TITLE_COLOR = 0xFFD7E6FF;
    private static final int SUBTITLE_COLOR = 0xFF9FB3CF;

    @Nullable
    private final Screen parent;
    @Nullable
    private KeyBinding waitingForKey;

    private final Map<KeyBinding, ButtonWidget> keyButtons = new LinkedHashMap<>();
    private final List<WidgetAnchor> scrollAnchors = new ArrayList<>();
    private final List<SectionTitle> sectionTitles = new ArrayList<>();
    private int scrollTop;
    private int scrollBottom;
    private int contentBottom;
    private int scrollOffset;
    private int maxScroll;

    @Nullable
    private ButtonWidget speedToggleButton;
    @Nullable
    private ButtonWidget playerEspToggleButton;
    @Nullable
    private ButtonWidget playerRaysToggleButton;
    @Nullable
    private ButtonWidget playerListToggleButton;
    @Nullable
    private ButtonWidget targetHealthToggleButton;
    @Nullable
    private ButtonWidget targetHealthColorToggleButton;
    @Nullable
    private CyclingButtonWidget<NoKnockbackClient.RayOrigin> rayOriginButton;
    @Nullable
    private SettingSlider rayThicknessSlider;
    @Nullable
    private SettingSlider outlineThicknessSlider;
    @Nullable
    private SettingSlider rayBottomStartHeightSlider;
    @Nullable
    private SettingSlider rayDistanceTextSizeSlider;
    @Nullable
    private SettingSlider targetHealthTextSizeSlider;
    @Nullable
    private SettingSlider playerListXSlider;
    @Nullable
    private SettingSlider playerListYSlider;
    @Nullable
    private SettingSlider playerListScaleSlider;
    @Nullable
    private SettingSlider playerListMaxHeightSlider;
    @Nullable
    private SettingSlider playerListAlphaSlider;

    public NoKnockbackMenuScreen(@Nullable Screen parent) {
        super(Text.literal("NoKnockback Settings"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        this.sectionTitles.clear();
        this.keyButtons.clear();
        this.scrollAnchors.clear();
        this.waitingForKey = null;
        this.scrollOffset = 0;

        int left = (this.width - PANEL_WIDTH) / 2;
        int rightColumnX = left + LEFT_COLUMN_WIDTH + 8;
        this.scrollTop = 26;
        this.scrollBottom = Math.max(this.scrollTop + 40, this.height - FOOTER_HEIGHT);
        int y = this.scrollTop;

        this.sectionTitles.add(new SectionTitle("Movement", y));
        y += 12;
        this.speedToggleButton = this.addScrollableWidget(ButtonWidget.builder(Text.empty(), button -> {
            NoKnockbackClient.setSpeedEnabled(!NoKnockbackClient.isSpeedEnabled());
            this.refreshLabels();
        }).dimensions(left, y, LEFT_COLUMN_WIDTH, ROW_HEIGHT).build());
        this.createKeyBindButton(NoKnockbackClient.getSpeedToggleKeyBinding(), rightColumnX, y, RIGHT_COLUMN_WIDTH);
        y += ROW_HEIGHT + SECTION_GAP;

        this.sectionTitles.add(new SectionTitle("ESP", y));
        y += 12;
        this.playerEspToggleButton = this.addScrollableWidget(ButtonWidget.builder(Text.empty(), button -> {
            NoKnockbackClient.setPlayerEspEnabled(!NoKnockbackClient.isPlayerEspEnabled());
            this.refreshLabels();
        }).dimensions(left, y, LEFT_COLUMN_WIDTH, ROW_HEIGHT).build());
        this.createKeyBindButton(NoKnockbackClient.getPlayerEspKeyBinding(), rightColumnX, y, RIGHT_COLUMN_WIDTH);
        y += ROW_HEIGHT + ROW_GAP;

        this.outlineThicknessSlider = this.addScrollableWidget(new SettingSlider(
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
        y += ROW_HEIGHT + ROW_GAP;

        y += SECTION_GAP;

        this.sectionTitles.add(new SectionTitle("Rays", y));
        y += 12;
        this.playerRaysToggleButton = this.addScrollableWidget(ButtonWidget.builder(Text.empty(), button -> {
            NoKnockbackClient.setPlayerRaysEnabled(!NoKnockbackClient.isPlayerRaysEnabled());
            this.refreshLabels();
        }).dimensions(left, y, LEFT_COLUMN_WIDTH, ROW_HEIGHT).build());
        this.createKeyBindButton(NoKnockbackClient.getPlayerRaysKeyBinding(), rightColumnX, y, RIGHT_COLUMN_WIDTH);
        y += ROW_HEIGHT + ROW_GAP;

        this.rayOriginButton = this.addScrollableWidget(CyclingButtonWidget.builder(this::rayOriginText)
                .values(NoKnockbackClient.RayOrigin.BOTTOM, NoKnockbackClient.RayOrigin.CENTER)
                .initially(NoKnockbackClient.getRayOrigin())
                .build(left, y, PANEL_WIDTH, ROW_HEIGHT, Text.literal("Ray Origin"), (button, value) -> {
                    NoKnockbackClient.setRayOrigin(value);
                    this.refreshLabels();
                }));
        y += ROW_HEIGHT + ROW_GAP;

        this.rayBottomStartHeightSlider = this.addScrollableWidget(new SettingSlider(
                left,
                y,
                PANEL_WIDTH,
                "Bottom Start Height",
                0.0,
                300.0,
                1.0,
                NoKnockbackClient.getRayBottomStartHeight()
        ) {
            @Override
            protected void onValueChanged(double value) {
                NoKnockbackClient.setRayBottomStartHeight((float) value);
            }
        });
        y += ROW_HEIGHT + ROW_GAP;

        this.rayThicknessSlider = this.addScrollableWidget(new SettingSlider(
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
        y += ROW_HEIGHT + ROW_GAP;

        this.rayDistanceTextSizeSlider = this.addScrollableWidget(new SettingSlider(
                left,
                y,
                PANEL_WIDTH,
                "Ray Distance Text Size",
                0.5,
                2.0,
                0.1,
                NoKnockbackClient.getRayLabelTextScale()
        ) {
            @Override
            protected void onValueChanged(double value) {
                NoKnockbackClient.setRayLabelTextScale((float) value);
            }
        });
        y += ROW_HEIGHT + SECTION_GAP;

        this.sectionTitles.add(new SectionTitle("Target Health", y));
        y += 12;
        this.targetHealthToggleButton = this.addScrollableWidget(ButtonWidget.builder(Text.empty(), button -> {
            NoKnockbackClient.setTargetHealthOverlayEnabled(!NoKnockbackClient.isTargetHealthOverlayEnabled());
            this.refreshLabels();
        }).dimensions(left, y, PANEL_WIDTH, ROW_HEIGHT).build());
        y += ROW_HEIGHT + ROW_GAP;

        this.targetHealthColorToggleButton = this.addScrollableWidget(ButtonWidget.builder(Text.empty(), button -> {
            NoKnockbackClient.setTargetHealthDynamicColorEnabled(!NoKnockbackClient.isTargetHealthDynamicColorEnabled());
            this.refreshLabels();
        }).dimensions(left, y, PANEL_WIDTH, ROW_HEIGHT).build());
        y += ROW_HEIGHT + ROW_GAP;

        this.targetHealthTextSizeSlider = this.addScrollableWidget(new SettingSlider(
                left,
                y,
                PANEL_WIDTH,
                "Target HP Text Size",
                0.5,
                2.0,
                0.1,
                NoKnockbackClient.getTargetHealthTextScale()
        ) {
            @Override
            protected void onValueChanged(double value) {
                NoKnockbackClient.setTargetHealthTextScale((float) value);
            }
        });
        y += ROW_HEIGHT + SECTION_GAP;

        this.sectionTitles.add(new SectionTitle("Player List", y));
        y += 12;
        this.playerListToggleButton = this.addScrollableWidget(ButtonWidget.builder(Text.empty(), button -> {
            NoKnockbackClient.setPlayerListEnabled(!NoKnockbackClient.isPlayerListEnabled());
            this.refreshLabels();
        }).dimensions(left, y, LEFT_COLUMN_WIDTH, ROW_HEIGHT).build());
        this.createKeyBindButton(NoKnockbackClient.getPlayerListKeyBinding(), rightColumnX, y, RIGHT_COLUMN_WIDTH);
        y += ROW_HEIGHT + ROW_GAP;

        this.playerListXSlider = this.addScrollableWidget(new SettingSlider(
                left,
                y,
                PANEL_WIDTH,
                "Player List X",
                0.0,
                4096.0,
                1.0,
                NoKnockbackClient.getPlayerListOffsetX()
        ) {
            @Override
            protected void onValueChanged(double value) {
                NoKnockbackClient.setPlayerListOffsetX((int) Math.round(value));
            }
        });
        y += ROW_HEIGHT + ROW_GAP;

        this.playerListYSlider = this.addScrollableWidget(new SettingSlider(
                left,
                y,
                PANEL_WIDTH,
                "Player List Y",
                0.0,
                4096.0,
                1.0,
                NoKnockbackClient.getPlayerListOffsetY()
        ) {
            @Override
            protected void onValueChanged(double value) {
                NoKnockbackClient.setPlayerListOffsetY((int) Math.round(value));
            }
        });
        y += ROW_HEIGHT + ROW_GAP;

        this.playerListScaleSlider = this.addScrollableWidget(new SettingSlider(
                left,
                y,
                PANEL_WIDTH,
                "Player List Size",
                0.1,
                2.0,
                0.1,
                NoKnockbackClient.getPlayerListTextScale()
        ) {
            @Override
            protected void onValueChanged(double value) {
                NoKnockbackClient.setPlayerListTextScale((float) value);
            }
        });
        y += ROW_HEIGHT + ROW_GAP;

        this.playerListMaxHeightSlider = this.addScrollableWidget(new SettingSlider(
                left,
                y,
                PANEL_WIDTH,
                "Player List Max Height",
                40.0,
                1200.0,
                10.0,
                NoKnockbackClient.getPlayerListMaxHeight()
        ) {
            @Override
            protected void onValueChanged(double value) {
                NoKnockbackClient.setPlayerListMaxHeight((int) Math.round(value));
            }
        });
        y += ROW_HEIGHT + ROW_GAP;

        this.playerListAlphaSlider = this.addScrollableWidget(new SettingSlider(
                left,
                y,
                PANEL_WIDTH,
                "Player List Alpha",
                0.1,
                1.0,
                0.1,
                NoKnockbackClient.getPlayerListAlphaMultiplier()
        ) {
            @Override
            protected void onValueChanged(double value) {
                NoKnockbackClient.setPlayerListAlphaMultiplier((float) value);
            }
        });
        y += ROW_HEIGHT + SECTION_GAP;

        this.sectionTitles.add(new SectionTitle("Menu", y));
        y += 12;
        ButtonWidget menuInfoButton = ButtonWidget.builder(Text.literal("Open Settings Menu"), button -> {
        }).dimensions(left, y, LEFT_COLUMN_WIDTH, ROW_HEIGHT).build();
        menuInfoButton.active = false;
        this.addScrollableWidget(menuInfoButton);
        this.createKeyBindButton(NoKnockbackClient.getOpenMenuKeyBinding(), rightColumnX, y, RIGHT_COLUMN_WIDTH);
        y += ROW_HEIGHT + SECTION_GAP;

        this.contentBottom = y;
        this.addDrawableChild(ButtonWidget.builder(Text.literal("Visual Settings"), button -> {
            if (this.client != null) {
                this.client.setScreen(new NoKnockbackVisualSettingsScreen(this));
            }
        }).dimensions(left, this.height - ROW_HEIGHT - 6, 130, ROW_HEIGHT).build());
        this.addDrawableChild(ButtonWidget.builder(Text.literal("Close"), button -> this.close())
                .dimensions(left + PANEL_WIDTH - 100, this.height - ROW_HEIGHT - 6, 100, ROW_HEIGHT).build());

        this.recalculateScrollBounds();
        this.applyScroll();
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
            int y = sectionTitle.baseY() - this.scrollOffset;
            if (y + 8 < this.scrollTop || y > this.scrollBottom) {
                continue;
            }
            context.drawTextWithShadow(this.textRenderer, sectionTitle.text(), left, y, 0xFFE5EEF9);
        }
        this.renderScrollBar(context, left);

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
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (this.maxScroll <= 0 || mouseY < this.scrollTop || mouseY > this.scrollBottom) {
            return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
        }

        if (verticalAmount == 0.0D) {
            return false;
        }

        this.setScrollOffset(this.scrollOffset - (int) Math.round(verticalAmount * SCROLL_STEP));
        return true;
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
        NoKnockbackClient.saveConfigNow();
        if (this.client != null) {
            this.client.options.write();
        }

        this.waitingForKey = null;
        this.refreshLabels();
    }

    private ButtonWidget createKeyBindButton(KeyBinding binding, int x, int y, int width) {
        ButtonWidget button = this.addScrollableWidget(ButtonWidget.builder(Text.empty(), widget -> {
            this.waitingForKey = binding;
            this.refreshLabels();
        }).dimensions(x, y, width, ROW_HEIGHT).build());
        this.keyButtons.put(binding, button);
        return button;
    }

    private <T extends ClickableWidget> T addScrollableWidget(T widget) {
        T added = this.addDrawableChild(widget);
        this.scrollAnchors.add(new WidgetAnchor(added, added.getY(), added.active));
        return added;
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

        if (this.rayBottomStartHeightSlider != null) {
            this.rayBottomStartHeightSlider.sync(NoKnockbackClient.getRayBottomStartHeight());
            this.rayBottomStartHeightSlider.active = NoKnockbackClient.getRayOrigin() == NoKnockbackClient.RayOrigin.BOTTOM;
        }

        if (this.rayDistanceTextSizeSlider != null) {
            this.rayDistanceTextSizeSlider.sync(NoKnockbackClient.getRayLabelTextScale());
            this.rayDistanceTextSizeSlider.active = NoKnockbackClient.isDistanceDisplayEnabled();
        }

        if (this.targetHealthToggleButton != null) {
            this.targetHealthToggleButton.setMessage(this.toggleText("All Players HP", NoKnockbackClient.isTargetHealthOverlayEnabled()));
        }

        if (this.targetHealthColorToggleButton != null) {
            this.targetHealthColorToggleButton.setMessage(this.toggleText("HP Dynamic Color", NoKnockbackClient.isTargetHealthDynamicColorEnabled()));
            this.targetHealthColorToggleButton.active = NoKnockbackClient.isTargetHealthOverlayEnabled();
        }

        if (this.targetHealthTextSizeSlider != null) {
            this.targetHealthTextSizeSlider.sync(NoKnockbackClient.getTargetHealthTextScale());
            this.targetHealthTextSizeSlider.active = NoKnockbackClient.isTargetHealthOverlayEnabled();
        }

        if (this.playerListXSlider != null) {
            this.playerListXSlider.sync(NoKnockbackClient.getPlayerListOffsetX());
        }

        if (this.playerListYSlider != null) {
            this.playerListYSlider.sync(NoKnockbackClient.getPlayerListOffsetY());
        }

        if (this.playerListScaleSlider != null) {
            this.playerListScaleSlider.sync(NoKnockbackClient.getPlayerListTextScale());
        }

        if (this.playerListMaxHeightSlider != null) {
            this.playerListMaxHeightSlider.sync(NoKnockbackClient.getPlayerListMaxHeight());
        }

        if (this.playerListAlphaSlider != null) {
            this.playerListAlphaSlider.sync(NoKnockbackClient.getPlayerListAlphaMultiplier());
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

    private void recalculateScrollBounds() {
        int viewportHeight = Math.max(1, this.scrollBottom - this.scrollTop);
        int contentHeight = Math.max(0, this.contentBottom - this.scrollTop);
        this.maxScroll = Math.max(0, contentHeight - viewportHeight);
        this.scrollOffset = MathHelper.clamp(this.scrollOffset, 0, this.maxScroll);
    }

    private void setScrollOffset(int offset) {
        this.scrollOffset = MathHelper.clamp(offset, 0, this.maxScroll);
        this.applyScroll();
        this.refreshLabels();
    }

    private void applyScroll() {
        for (WidgetAnchor anchor : this.scrollAnchors) {
            ClickableWidget widget = anchor.widget();
            int y = anchor.baseY() - this.scrollOffset;
            widget.setY(y);

            boolean visible = y + widget.getHeight() >= this.scrollTop && y <= this.scrollBottom;
            widget.visible = visible;
            widget.active = anchor.baseActive() && visible;
        }
    }

    private void renderScrollBar(DrawContext context, int left) {
        if (this.maxScroll <= 0) return;

        int trackX1 = left + PANEL_WIDTH - 3;
        int trackX2 = trackX1 + 3;
        int trackY1 = this.scrollTop;
        int trackY2 = this.scrollBottom;
        int trackHeight = Math.max(1, trackY2 - trackY1);
        int contentHeight = Math.max(trackHeight, this.contentBottom - this.scrollTop);
        int thumbHeight = MathHelper.clamp((int) (trackHeight * (trackHeight / (float) contentHeight)), 16, trackHeight);
        int thumbTravel = trackHeight - thumbHeight;
        int thumbY = trackY1 + (int) (thumbTravel * (this.scrollOffset / (float) this.maxScroll));

        context.fill(trackX1, trackY1, trackX2, trackY2, 0x4A2A3342);
        context.fill(trackX1, thumbY, trackX2, thumbY + thumbHeight, 0xB5DBE7FB);
    }

    private Text toggleText(String name, boolean enabled) {
        return Text.literal(name + ": " + (enabled ? "ON" : "OFF"));
    }

    private Text rayOriginText(NoKnockbackClient.RayOrigin origin) {
        return origin == NoKnockbackClient.RayOrigin.CENTER ? Text.literal("Center") : Text.literal("Bottom");
    }

    private record SectionTitle(String text, int baseY) {
    }

    private record WidgetAnchor(ClickableWidget widget, int baseY, boolean baseActive) {
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
