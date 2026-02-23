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
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.BooleanSupplier;

public class NoKnockbackMenuScreen extends Screen {
    private static final int PANEL_WIDTH = 460;
    private static final int ROW_HEIGHT = 20;
    private static final int ROW_GAP = 4;
    private static final int SECTION_GAP = 8;
    private static final int SCROLL_STEP = 20;
    private static final int FOOTER_HEIGHT = 32;
    private static final int TITLE_COLOR = 0xFFE7F0FF;
    private static final int SUBTITLE_COLOR = 0xFF9BB2CF;

    @Nullable
    private final Screen parent;
    @Nullable
    private KeyBinding waitingForKey;

    private final Map<KeyBinding, ButtonWidget> keyButtons = new LinkedHashMap<>();
    private final List<WidgetAnchor> scrollAnchors = new ArrayList<>();
    private final EnumMap<SectionId, Boolean> expandedSections = new EnumMap<>(SectionId.class);
    private final EnumMap<SectionId, SectionHeader> sectionHeaders = new EnumMap<>(SectionId.class);

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
    private ButtonWidget playerArmorOverlayToggleButton;
    @Nullable
    private ButtonWidget distanceDisplayToggleButton;
    @Nullable
    private ButtonWidget heldItemOverlayToggleButton;
    @Nullable
    private ButtonWidget playerListToggleButton;
    @Nullable
    private ButtonWidget targetHealthToggleButton;
    @Nullable
    private ButtonWidget targetHealthColorToggleButton;
    @Nullable
    private ButtonWidget targetHealthBindPlaceholderButton;
    @Nullable
    private ButtonWidget menuAlwaysOnPlaceholderButton;

    @Nullable
    private ButtonWidget rayGlowButton;
    @Nullable
    private ButtonWidget armorGlowButton;
    @Nullable
    private ButtonWidget heldItemGlowButton;
    @Nullable
    private ButtonWidget distanceGlowButton;

    @Nullable
    private CyclingButtonWidget<NoKnockbackClient.RayOrigin> rayOriginButton;
    @Nullable
    private CyclingButtonWidget<NoKnockbackClient.OverlayAnchorMode> armorAnchorButton;
    @Nullable
    private CyclingButtonWidget<NoKnockbackClient.OverlayAnchorMode> heldItemAnchorButton;
    @Nullable
    private CyclingButtonWidget<NoKnockbackClient.OverlayAnchorMode> distanceAnchorButton;

    @Nullable
    private CyclingButtonWidget<NoKnockbackClient.VisualColorMode> rayColorModeButton;
    @Nullable
    private CyclingButtonWidget<NoKnockbackClient.VisualColorMode> armorColorModeButton;
    @Nullable
    private CyclingButtonWidget<NoKnockbackClient.VisualColorMode> heldItemColorModeButton;
    @Nullable
    private CyclingButtonWidget<NoKnockbackClient.VisualColorMode> distanceColorModeButton;

    @Nullable
    private SettingSlider rayThicknessSlider;
    @Nullable
    private SettingSlider outlineThicknessSlider;
    @Nullable
    private SettingSlider rayBottomStartHeightSlider;
    @Nullable
    private SettingSlider distanceTextSizeSlider;
    @Nullable
    private SettingSlider armorSizeSlider;
    @Nullable
    private SettingSlider heldItemSizeSlider;
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

    public NoKnockbackMenuScreen(@Nullable Screen parent) {
        super(Text.literal("NoKnockback Overlay"));
        this.parent = parent;
        for (SectionId sectionId : SectionId.values()) {
            this.expandedSections.put(sectionId, false);
        }
    }

    @Override
    protected void init() {
        this.keyButtons.clear();
        this.scrollAnchors.clear();
        this.sectionHeaders.clear();
        this.waitingForKey = null;
        this.scrollOffset = 0;

        int left = (this.width - PANEL_WIDTH) / 2;
        this.scrollTop = 34;
        this.scrollBottom = Math.max(this.scrollTop + 40, this.height - FOOTER_HEIGHT);
        int y = this.scrollTop;

        y = this.addSectionHeader(left, y, SectionId.SPEED, "Speed", NoKnockbackClient::isSpeedEnabled);
        if (this.isExpanded(SectionId.SPEED)) {
            y = this.buildSpeedSection(left, y);
            y += SECTION_GAP;
        }

        y = this.addSectionHeader(left, y, SectionId.ESP, "Player ESP", NoKnockbackClient::isPlayerEspEnabled);
        if (this.isExpanded(SectionId.ESP)) {
            y = this.buildEspSection(left, y);
            y += SECTION_GAP;
        }

        y = this.addSectionHeader(left, y, SectionId.RAYS, "Rays", NoKnockbackClient::isPlayerRaysEnabled);
        if (this.isExpanded(SectionId.RAYS)) {
            y = this.buildRaysSection(left, y);
            y += SECTION_GAP;
        }

        y = this.addSectionHeader(left, y, SectionId.TARGET_HEALTH, "Health Overlay", NoKnockbackClient::isTargetHealthOverlayEnabled);
        if (this.isExpanded(SectionId.TARGET_HEALTH)) {
            y = this.buildTargetHealthSection(left, y);
            y += SECTION_GAP;
        }

        y = this.addSectionHeader(left, y, SectionId.PLAYER_LIST, "Player List", NoKnockbackClient::isPlayerListEnabled);
        if (this.isExpanded(SectionId.PLAYER_LIST)) {
            y = this.buildPlayerListSection(left, y);
            y += SECTION_GAP;
        }

        y = this.addSectionHeader(left, y, SectionId.MENU, "Menu", () -> true);
        if (this.isExpanded(SectionId.MENU)) {
            y = this.buildMenuSection(left, y);
            y += SECTION_GAP;
        }

        this.contentBottom = y;

        this.addDrawableChild(ButtonWidget.builder(Text.literal("Close"), button -> this.close())
                .dimensions(left + PANEL_WIDTH - 100, this.height - ROW_HEIGHT - 6, 100, ROW_HEIGHT).build());

        this.recalculateScrollBounds();
        this.applyScroll();
        this.refreshLabels();
    }

    private int addSectionHeader(int left, int y, SectionId sectionId, String title, @Nullable BooleanSupplier stateSupplier) {
        ButtonWidget button = this.addScrollableWidget(ButtonWidget.builder(Text.empty(), widget -> {
            this.expandedSections.put(sectionId, !this.isExpanded(sectionId));
            this.clearAndInit();
        }).dimensions(left, y, PANEL_WIDTH, ROW_HEIGHT).build());

        this.sectionHeaders.put(sectionId, new SectionHeader(button, title, stateSupplier));
        return y + ROW_HEIGHT + ROW_GAP;
    }

    private int buildSpeedSection(int left, int y) {
        this.speedToggleButton = this.addScrollableWidget(ButtonWidget.builder(Text.empty(), button -> {
            NoKnockbackClient.setSpeedEnabled(!NoKnockbackClient.isSpeedEnabled());
            this.refreshLabels();
        }).dimensions(left, y, PANEL_WIDTH, ROW_HEIGHT).build());
        y += ROW_HEIGHT + ROW_GAP;

        this.createKeyBindButton(NoKnockbackClient.getSpeedToggleKeyBinding(), left, y, PANEL_WIDTH);
        y += ROW_HEIGHT + ROW_GAP;
        return y;
    }

    private int buildEspSection(int left, int y) {
        this.playerEspToggleButton = this.addScrollableWidget(ButtonWidget.builder(Text.empty(), button -> {
            NoKnockbackClient.setPlayerEspEnabled(!NoKnockbackClient.isPlayerEspEnabled());
            this.refreshLabels();
        }).dimensions(left, y, PANEL_WIDTH, ROW_HEIGHT).build());
        y += ROW_HEIGHT + ROW_GAP;

        this.createKeyBindButton(NoKnockbackClient.getPlayerEspKeyBinding(), left, y, PANEL_WIDTH);
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
        return y;
    }

    private int buildRaysSection(int left, int y) {
        this.playerRaysToggleButton = this.addScrollableWidget(ButtonWidget.builder(Text.empty(), button -> {
            NoKnockbackClient.setPlayerRaysEnabled(!NoKnockbackClient.isPlayerRaysEnabled());
            this.refreshLabels();
        }).dimensions(left, y, PANEL_WIDTH, ROW_HEIGHT).build());
        y += ROW_HEIGHT + ROW_GAP;

        this.createKeyBindButton(NoKnockbackClient.getPlayerRaysKeyBinding(), left, y, PANEL_WIDTH);
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

        this.rayGlowButton = this.addScrollableWidget(ButtonWidget.builder(Text.empty(), button -> {
            NoKnockbackClient.setRayVisualGlowEnabled(!NoKnockbackClient.isRayVisualGlowEnabled());
            this.refreshLabels();
        }).dimensions(left, y, PANEL_WIDTH, ROW_HEIGHT).build());
        y += ROW_HEIGHT + ROW_GAP;

        this.rayColorModeButton = this.addScrollableWidget(CyclingButtonWidget.builder(this::colorModeText)
                .values(NoKnockbackClient.VisualColorMode.NICK, NoKnockbackClient.VisualColorMode.VIVID, NoKnockbackClient.VisualColorMode.GRADIENT, NoKnockbackClient.VisualColorMode.RAINBOW)
                .initially(NoKnockbackClient.getRayVisualColorMode())
                .build(left, y, PANEL_WIDTH, ROW_HEIGHT, Text.literal("Ray Color Mode"), (button, value) -> {
                    NoKnockbackClient.setRayVisualColorMode(value);
                    this.refreshLabels();
                }));
        y += ROW_HEIGHT + ROW_GAP;

        this.raySaturationSlider = this.addScrollableWidget(new SettingSlider(left, y, PANEL_WIDTH, "Ray Saturation", 1.0, 2.5, 0.1, NoKnockbackClient.getRayVisualSaturationBoost()) {
            @Override
            protected void onValueChanged(double value) {
                NoKnockbackClient.setRayVisualSaturationBoost((float) value);
            }
        });
        y += ROW_HEIGHT + ROW_GAP;

        this.raySpeedSlider = this.addScrollableWidget(new SettingSlider(left, y, PANEL_WIDTH, "Ray Animation Speed", 0.2, 4.0, 0.1, NoKnockbackClient.getRayVisualAnimationSpeed()) {
            @Override
            protected void onValueChanged(double value) {
                NoKnockbackClient.setRayVisualAnimationSpeed((float) value);
            }
        });
        y += ROW_HEIGHT + ROW_GAP;

        this.playerArmorOverlayToggleButton = this.addScrollableWidget(ButtonWidget.builder(Text.empty(), button -> {
            NoKnockbackClient.setPlayerArmorOverlayEnabled(!NoKnockbackClient.isPlayerArmorOverlayEnabled());
            this.refreshLabels();
        }).dimensions(left, y, PANEL_WIDTH, ROW_HEIGHT).build());
        y += ROW_HEIGHT + ROW_GAP;

        this.armorAnchorButton = this.addScrollableWidget(CyclingButtonWidget.builder(this::anchorModeText)
                .values(NoKnockbackClient.OverlayAnchorMode.ABOVE_PLAYER, NoKnockbackClient.OverlayAnchorMode.RAY_MIDDLE)
                .initially(NoKnockbackClient.getArmorAnchorMode())
                .build(left, y, PANEL_WIDTH, ROW_HEIGHT, Text.literal("Armor Position"), (button, value) -> {
                    NoKnockbackClient.setArmorAnchorMode(value);
                    this.refreshLabels();
                }));
        y += ROW_HEIGHT + ROW_GAP;

        this.armorSizeSlider = this.addScrollableWidget(new SettingSlider(
                left,
                y,
                PANEL_WIDTH,
                "Armor Size",
                0.35,
                2.5,
                0.1,
                NoKnockbackClient.getArmorOverlayScale()
        ) {
            @Override
            protected void onValueChanged(double value) {
                NoKnockbackClient.setArmorOverlayScale((float) value);
            }
        });
        y += ROW_HEIGHT + ROW_GAP;

        this.armorGlowButton = this.addScrollableWidget(ButtonWidget.builder(Text.empty(), button -> {
            NoKnockbackClient.setArmorVisualGlowEnabled(!NoKnockbackClient.isArmorVisualGlowEnabled());
            this.refreshLabels();
        }).dimensions(left, y, PANEL_WIDTH, ROW_HEIGHT).build());
        y += ROW_HEIGHT + ROW_GAP;

        this.armorColorModeButton = this.addScrollableWidget(CyclingButtonWidget.builder(this::colorModeText)
                .values(NoKnockbackClient.VisualColorMode.NICK, NoKnockbackClient.VisualColorMode.VIVID, NoKnockbackClient.VisualColorMode.GRADIENT, NoKnockbackClient.VisualColorMode.RAINBOW)
                .initially(NoKnockbackClient.getArmorVisualColorMode())
                .build(left, y, PANEL_WIDTH, ROW_HEIGHT, Text.literal("Armor Color Mode"), (button, value) -> {
                    NoKnockbackClient.setArmorVisualColorMode(value);
                    this.refreshLabels();
                }));
        y += ROW_HEIGHT + ROW_GAP;

        this.armorSaturationSlider = this.addScrollableWidget(new SettingSlider(left, y, PANEL_WIDTH, "Armor Saturation", 1.0, 2.5, 0.1, NoKnockbackClient.getArmorVisualSaturationBoost()) {
            @Override
            protected void onValueChanged(double value) {
                NoKnockbackClient.setArmorVisualSaturationBoost((float) value);
            }
        });
        y += ROW_HEIGHT + ROW_GAP;

        this.armorSpeedSlider = this.addScrollableWidget(new SettingSlider(left, y, PANEL_WIDTH, "Armor Animation Speed", 0.2, 4.0, 0.1, NoKnockbackClient.getArmorVisualAnimationSpeed()) {
            @Override
            protected void onValueChanged(double value) {
                NoKnockbackClient.setArmorVisualAnimationSpeed((float) value);
            }
        });
        y += ROW_HEIGHT + ROW_GAP;

        this.heldItemOverlayToggleButton = this.addScrollableWidget(ButtonWidget.builder(Text.empty(), button -> {
            NoKnockbackClient.setHeldItemOverlayEnabled(!NoKnockbackClient.isHeldItemOverlayEnabled());
            this.refreshLabels();
        }).dimensions(left, y, PANEL_WIDTH, ROW_HEIGHT).build());
        y += ROW_HEIGHT + ROW_GAP;

        this.heldItemAnchorButton = this.addScrollableWidget(CyclingButtonWidget.builder(this::anchorModeText)
                .values(NoKnockbackClient.OverlayAnchorMode.ABOVE_PLAYER, NoKnockbackClient.OverlayAnchorMode.RAY_MIDDLE)
                .initially(NoKnockbackClient.getHeldItemAnchorMode())
                .build(left, y, PANEL_WIDTH, ROW_HEIGHT, Text.literal("Held Item Position"), (button, value) -> {
                    NoKnockbackClient.setHeldItemAnchorMode(value);
                    this.refreshLabels();
                }));
        y += ROW_HEIGHT + ROW_GAP;

        this.heldItemSizeSlider = this.addScrollableWidget(new SettingSlider(
                left,
                y,
                PANEL_WIDTH,
                "Held Item Size",
                0.35,
                2.5,
                0.1,
                NoKnockbackClient.getHeldItemOverlayScale()
        ) {
            @Override
            protected void onValueChanged(double value) {
                NoKnockbackClient.setHeldItemOverlayScale((float) value);
            }
        });
        y += ROW_HEIGHT + ROW_GAP;

        this.heldItemGlowButton = this.addScrollableWidget(ButtonWidget.builder(Text.empty(), button -> {
            NoKnockbackClient.setHeldItemVisualGlowEnabled(!NoKnockbackClient.isHeldItemVisualGlowEnabled());
            this.refreshLabels();
        }).dimensions(left, y, PANEL_WIDTH, ROW_HEIGHT).build());
        y += ROW_HEIGHT + ROW_GAP;

        this.heldItemColorModeButton = this.addScrollableWidget(CyclingButtonWidget.builder(this::colorModeText)
                .values(NoKnockbackClient.VisualColorMode.NICK, NoKnockbackClient.VisualColorMode.VIVID, NoKnockbackClient.VisualColorMode.GRADIENT, NoKnockbackClient.VisualColorMode.RAINBOW)
                .initially(NoKnockbackClient.getHeldItemVisualColorMode())
                .build(left, y, PANEL_WIDTH, ROW_HEIGHT, Text.literal("Held Item Color Mode"), (button, value) -> {
                    NoKnockbackClient.setHeldItemVisualColorMode(value);
                    this.refreshLabels();
                }));
        y += ROW_HEIGHT + ROW_GAP;

        this.heldItemSaturationSlider = this.addScrollableWidget(new SettingSlider(left, y, PANEL_WIDTH, "Held Item Saturation", 1.0, 2.5, 0.1, NoKnockbackClient.getHeldItemVisualSaturationBoost()) {
            @Override
            protected void onValueChanged(double value) {
                NoKnockbackClient.setHeldItemVisualSaturationBoost((float) value);
            }
        });
        y += ROW_HEIGHT + ROW_GAP;

        this.heldItemSpeedSlider = this.addScrollableWidget(new SettingSlider(left, y, PANEL_WIDTH, "Held Item Animation Speed", 0.2, 4.0, 0.1, NoKnockbackClient.getHeldItemVisualAnimationSpeed()) {
            @Override
            protected void onValueChanged(double value) {
                NoKnockbackClient.setHeldItemVisualAnimationSpeed((float) value);
            }
        });
        y += ROW_HEIGHT + ROW_GAP;

        this.distanceDisplayToggleButton = this.addScrollableWidget(ButtonWidget.builder(Text.empty(), button -> {
            NoKnockbackClient.setDistanceDisplayEnabled(!NoKnockbackClient.isDistanceDisplayEnabled());
            this.refreshLabels();
        }).dimensions(left, y, PANEL_WIDTH, ROW_HEIGHT).build());
        y += ROW_HEIGHT + ROW_GAP;

        this.distanceAnchorButton = this.addScrollableWidget(CyclingButtonWidget.builder(this::anchorModeText)
                .values(NoKnockbackClient.OverlayAnchorMode.ABOVE_PLAYER, NoKnockbackClient.OverlayAnchorMode.RAY_MIDDLE)
                .initially(NoKnockbackClient.getDistanceAnchorMode())
                .build(left, y, PANEL_WIDTH, ROW_HEIGHT, Text.literal("Distance Position"), (button, value) -> {
                    NoKnockbackClient.setDistanceAnchorMode(value);
                    this.refreshLabels();
                }));
        y += ROW_HEIGHT + ROW_GAP;

        this.distanceTextSizeSlider = this.addScrollableWidget(new SettingSlider(
                left,
                y,
                PANEL_WIDTH,
                "Distance Text Size",
                0.5,
                2.0,
                0.1,
                NoKnockbackClient.getDistanceTextScale()
        ) {
            @Override
            protected void onValueChanged(double value) {
                NoKnockbackClient.setDistanceTextScale((float) value);
            }
        });
        y += ROW_HEIGHT + ROW_GAP;

        this.distanceGlowButton = this.addScrollableWidget(ButtonWidget.builder(Text.empty(), button -> {
            NoKnockbackClient.setDistanceVisualGlowEnabled(!NoKnockbackClient.isDistanceVisualGlowEnabled());
            this.refreshLabels();
        }).dimensions(left, y, PANEL_WIDTH, ROW_HEIGHT).build());
        y += ROW_HEIGHT + ROW_GAP;

        this.distanceColorModeButton = this.addScrollableWidget(CyclingButtonWidget.builder(this::colorModeText)
                .values(NoKnockbackClient.VisualColorMode.NICK, NoKnockbackClient.VisualColorMode.VIVID, NoKnockbackClient.VisualColorMode.GRADIENT, NoKnockbackClient.VisualColorMode.RAINBOW)
                .initially(NoKnockbackClient.getDistanceVisualColorMode())
                .build(left, y, PANEL_WIDTH, ROW_HEIGHT, Text.literal("Distance Color Mode"), (button, value) -> {
                    NoKnockbackClient.setDistanceVisualColorMode(value);
                    this.refreshLabels();
                }));
        y += ROW_HEIGHT + ROW_GAP;

        this.distanceSaturationSlider = this.addScrollableWidget(new SettingSlider(left, y, PANEL_WIDTH, "Distance Saturation", 1.0, 2.5, 0.1, NoKnockbackClient.getDistanceVisualSaturationBoost()) {
            @Override
            protected void onValueChanged(double value) {
                NoKnockbackClient.setDistanceVisualSaturationBoost((float) value);
            }
        });
        y += ROW_HEIGHT + ROW_GAP;

        this.distanceSpeedSlider = this.addScrollableWidget(new SettingSlider(left, y, PANEL_WIDTH, "Distance Animation Speed", 0.2, 4.0, 0.1, NoKnockbackClient.getDistanceVisualAnimationSpeed()) {
            @Override
            protected void onValueChanged(double value) {
                NoKnockbackClient.setDistanceVisualAnimationSpeed((float) value);
            }
        });
        y += ROW_HEIGHT + ROW_GAP;

        return y;
    }

    private int buildTargetHealthSection(int left, int y) {
        this.targetHealthToggleButton = this.addScrollableWidget(ButtonWidget.builder(Text.empty(), button -> {
            NoKnockbackClient.setTargetHealthOverlayEnabled(!NoKnockbackClient.isTargetHealthOverlayEnabled());
            this.refreshLabels();
        }).dimensions(left, y, PANEL_WIDTH, ROW_HEIGHT).build());
        y += ROW_HEIGHT + ROW_GAP;

        this.targetHealthBindPlaceholderButton = this.addScrollableWidget(ButtonWidget.builder(Text.literal("Bind: not assigned"), button -> {
        }).dimensions(left, y, PANEL_WIDTH, ROW_HEIGHT).build());
        this.targetHealthBindPlaceholderButton.active = false;
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
        y += ROW_HEIGHT + ROW_GAP;

        return y;
    }

    private int buildPlayerListSection(int left, int y) {
        this.playerListToggleButton = this.addScrollableWidget(ButtonWidget.builder(Text.empty(), button -> {
            NoKnockbackClient.setPlayerListEnabled(!NoKnockbackClient.isPlayerListEnabled());
            this.refreshLabels();
        }).dimensions(left, y, PANEL_WIDTH, ROW_HEIGHT).build());
        y += ROW_HEIGHT + ROW_GAP;

        this.createKeyBindButton(NoKnockbackClient.getPlayerListKeyBinding(), left, y, PANEL_WIDTH);
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
        y += ROW_HEIGHT + ROW_GAP;

        return y;
    }

    private int buildMenuSection(int left, int y) {
        this.menuAlwaysOnPlaceholderButton = this.addScrollableWidget(ButtonWidget.builder(Text.literal("Enabled: ON"), button -> {
        }).dimensions(left, y, PANEL_WIDTH, ROW_HEIGHT).build());
        this.menuAlwaysOnPlaceholderButton.active = false;
        y += ROW_HEIGHT + ROW_GAP;

        this.createKeyBindButton(NoKnockbackClient.getOpenMenuKeyBinding(), left, y, PANEL_WIDTH);
        y += ROW_HEIGHT + ROW_GAP;

        return y;
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        int left = (this.width - PANEL_WIDTH) / 2;
        int panelTop = 6;
        int panelBottom = this.height - 6;

        context.fill(0, 0, this.width, this.height, 0x86060A12);
        context.fill(left - 8, panelTop, left + PANEL_WIDTH + 8, panelBottom, 0xC4141D2D);
        context.fill(left - 8, panelTop, left + PANEL_WIDTH + 8, panelTop + 22, 0xD4203048);

        super.render(context, mouseX, mouseY, delta);

        context.drawCenteredTextWithShadow(this.textRenderer, this.title, this.width / 2, 10, TITLE_COLOR);
        context.drawCenteredTextWithShadow(this.textRenderer, Text.literal("Expandable function blocks (ON/OFF, bind, settings)"), this.width / 2, 20, SUBTITLE_COLOR);

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
        for (Map.Entry<SectionId, SectionHeader> entry : this.sectionHeaders.entrySet()) {
            SectionHeader header = entry.getValue();
            boolean expanded = this.isExpanded(entry.getKey());
            boolean enabledState = header.stateSupplier() == null || header.stateSupplier().getAsBoolean();
            header.button().setMessage(this.sectionText(header.title(), expanded, enabledState));
        }

        if (this.speedToggleButton != null) {
            this.speedToggleButton.setMessage(this.toggleText("Enabled", NoKnockbackClient.isSpeedEnabled()));
        }
        if (this.playerEspToggleButton != null) {
            this.playerEspToggleButton.setMessage(this.toggleText("Enabled", NoKnockbackClient.isPlayerEspEnabled()));
        }
        if (this.playerRaysToggleButton != null) {
            this.playerRaysToggleButton.setMessage(this.toggleText("Enabled", NoKnockbackClient.isPlayerRaysEnabled()));
        }
        if (this.playerListToggleButton != null) {
            this.playerListToggleButton.setMessage(this.toggleText("Enabled", NoKnockbackClient.isPlayerListEnabled()));
        }
        if (this.targetHealthToggleButton != null) {
            this.targetHealthToggleButton.setMessage(this.toggleText("Enabled", NoKnockbackClient.isTargetHealthOverlayEnabled()));
        }
        if (this.targetHealthColorToggleButton != null) {
            this.targetHealthColorToggleButton.setMessage(this.toggleText("HP Dynamic Color", NoKnockbackClient.isTargetHealthDynamicColorEnabled()));
            this.targetHealthColorToggleButton.active = NoKnockbackClient.isTargetHealthOverlayEnabled();
        }

        if (this.rayOriginButton != null) {
            this.rayOriginButton.setValue(NoKnockbackClient.getRayOrigin());
            this.rayOriginButton.active = NoKnockbackClient.isPlayerRaysEnabled();
        }
        if (this.rayBottomStartHeightSlider != null) {
            this.rayBottomStartHeightSlider.sync(NoKnockbackClient.getRayBottomStartHeight());
            this.rayBottomStartHeightSlider.active = NoKnockbackClient.isPlayerRaysEnabled() && NoKnockbackClient.getRayOrigin() == NoKnockbackClient.RayOrigin.BOTTOM;
        }
        if (this.rayThicknessSlider != null) {
            this.rayThicknessSlider.sync(NoKnockbackClient.getRayThickness());
            this.rayThicknessSlider.active = NoKnockbackClient.isPlayerRaysEnabled();
        }
        if (this.rayGlowButton != null) {
            this.rayGlowButton.setMessage(this.toggleText("Ray Glow", NoKnockbackClient.isRayVisualGlowEnabled()));
            this.rayGlowButton.active = NoKnockbackClient.isPlayerRaysEnabled();
        }
        if (this.rayColorModeButton != null) {
            this.rayColorModeButton.setValue(NoKnockbackClient.getRayVisualColorMode());
            this.rayColorModeButton.active = NoKnockbackClient.isPlayerRaysEnabled();
        }
        if (this.raySaturationSlider != null) {
            this.raySaturationSlider.sync(NoKnockbackClient.getRayVisualSaturationBoost());
            this.raySaturationSlider.active = NoKnockbackClient.isPlayerRaysEnabled();
        }
        if (this.raySpeedSlider != null) {
            this.raySpeedSlider.sync(NoKnockbackClient.getRayVisualAnimationSpeed());
            this.raySpeedSlider.active = NoKnockbackClient.isPlayerRaysEnabled();
        }

        if (this.outlineThicknessSlider != null) {
            this.outlineThicknessSlider.sync(NoKnockbackClient.getOutlineThickness());
            this.outlineThicknessSlider.active = NoKnockbackClient.isPlayerEspEnabled();
        }

        if (this.playerArmorOverlayToggleButton != null) {
            this.playerArmorOverlayToggleButton.setMessage(this.toggleText("Armor Through Walls", NoKnockbackClient.isPlayerArmorOverlayEnabled()));
        }
        if (this.armorAnchorButton != null) {
            this.armorAnchorButton.setValue(NoKnockbackClient.getArmorAnchorMode());
            this.armorAnchorButton.active = NoKnockbackClient.isPlayerArmorOverlayEnabled();
        }
        if (this.armorSizeSlider != null) {
            this.armorSizeSlider.sync(NoKnockbackClient.getArmorOverlayScale());
            this.armorSizeSlider.active = NoKnockbackClient.isPlayerArmorOverlayEnabled();
        }
        if (this.armorGlowButton != null) {
            this.armorGlowButton.setMessage(this.toggleText("Armor Glow", NoKnockbackClient.isArmorVisualGlowEnabled()));
            this.armorGlowButton.active = NoKnockbackClient.isPlayerArmorOverlayEnabled();
        }
        if (this.armorColorModeButton != null) {
            this.armorColorModeButton.setValue(NoKnockbackClient.getArmorVisualColorMode());
            this.armorColorModeButton.active = NoKnockbackClient.isPlayerArmorOverlayEnabled();
        }
        if (this.armorSaturationSlider != null) {
            this.armorSaturationSlider.sync(NoKnockbackClient.getArmorVisualSaturationBoost());
            this.armorSaturationSlider.active = NoKnockbackClient.isPlayerArmorOverlayEnabled();
        }
        if (this.armorSpeedSlider != null) {
            this.armorSpeedSlider.sync(NoKnockbackClient.getArmorVisualAnimationSpeed());
            this.armorSpeedSlider.active = NoKnockbackClient.isPlayerArmorOverlayEnabled();
        }

        if (this.heldItemOverlayToggleButton != null) {
            this.heldItemOverlayToggleButton.setMessage(this.toggleText("Held Items Overlay", NoKnockbackClient.isHeldItemOverlayEnabled()));
        }
        if (this.heldItemAnchorButton != null) {
            this.heldItemAnchorButton.setValue(NoKnockbackClient.getHeldItemAnchorMode());
            this.heldItemAnchorButton.active = NoKnockbackClient.isHeldItemOverlayEnabled();
        }
        if (this.heldItemSizeSlider != null) {
            this.heldItemSizeSlider.sync(NoKnockbackClient.getHeldItemOverlayScale());
            this.heldItemSizeSlider.active = NoKnockbackClient.isHeldItemOverlayEnabled();
        }
        if (this.heldItemGlowButton != null) {
            this.heldItemGlowButton.setMessage(this.toggleText("Held Item Glow", NoKnockbackClient.isHeldItemVisualGlowEnabled()));
            this.heldItemGlowButton.active = NoKnockbackClient.isHeldItemOverlayEnabled();
        }
        if (this.heldItemColorModeButton != null) {
            this.heldItemColorModeButton.setValue(NoKnockbackClient.getHeldItemVisualColorMode());
            this.heldItemColorModeButton.active = NoKnockbackClient.isHeldItemOverlayEnabled();
        }
        if (this.heldItemSaturationSlider != null) {
            this.heldItemSaturationSlider.sync(NoKnockbackClient.getHeldItemVisualSaturationBoost());
            this.heldItemSaturationSlider.active = NoKnockbackClient.isHeldItemOverlayEnabled();
        }
        if (this.heldItemSpeedSlider != null) {
            this.heldItemSpeedSlider.sync(NoKnockbackClient.getHeldItemVisualAnimationSpeed());
            this.heldItemSpeedSlider.active = NoKnockbackClient.isHeldItemOverlayEnabled();
        }

        if (this.distanceDisplayToggleButton != null) {
            this.distanceDisplayToggleButton.setMessage(this.toggleText("Distance Overlay", NoKnockbackClient.isDistanceDisplayEnabled()));
        }
        if (this.distanceAnchorButton != null) {
            this.distanceAnchorButton.setValue(NoKnockbackClient.getDistanceAnchorMode());
            this.distanceAnchorButton.active = NoKnockbackClient.isDistanceDisplayEnabled();
        }
        if (this.distanceTextSizeSlider != null) {
            this.distanceTextSizeSlider.sync(NoKnockbackClient.getDistanceTextScale());
            this.distanceTextSizeSlider.active = NoKnockbackClient.isDistanceDisplayEnabled();
        }
        if (this.distanceGlowButton != null) {
            this.distanceGlowButton.setMessage(this.toggleText("Distance Glow", NoKnockbackClient.isDistanceVisualGlowEnabled()));
            this.distanceGlowButton.active = NoKnockbackClient.isDistanceDisplayEnabled();
        }
        if (this.distanceColorModeButton != null) {
            this.distanceColorModeButton.setValue(NoKnockbackClient.getDistanceVisualColorMode());
            this.distanceColorModeButton.active = NoKnockbackClient.isDistanceDisplayEnabled();
        }
        if (this.distanceSaturationSlider != null) {
            this.distanceSaturationSlider.sync(NoKnockbackClient.getDistanceVisualSaturationBoost());
            this.distanceSaturationSlider.active = NoKnockbackClient.isDistanceDisplayEnabled();
        }
        if (this.distanceSpeedSlider != null) {
            this.distanceSpeedSlider.sync(NoKnockbackClient.getDistanceVisualAnimationSpeed());
            this.distanceSpeedSlider.active = NoKnockbackClient.isDistanceDisplayEnabled();
        }

        if (this.targetHealthTextSizeSlider != null) {
            this.targetHealthTextSizeSlider.sync(NoKnockbackClient.getTargetHealthTextScale());
            this.targetHealthTextSizeSlider.active = NoKnockbackClient.isTargetHealthOverlayEnabled();
        }
        if (this.targetHealthBindPlaceholderButton != null) {
            this.targetHealthBindPlaceholderButton.setMessage(Text.literal("Bind: not assigned"));
            this.targetHealthBindPlaceholderButton.active = false;
        }

        if (this.playerListXSlider != null) {
            this.playerListXSlider.sync(NoKnockbackClient.getPlayerListOffsetX());
            this.playerListXSlider.active = NoKnockbackClient.isPlayerListEnabled();
        }
        if (this.playerListYSlider != null) {
            this.playerListYSlider.sync(NoKnockbackClient.getPlayerListOffsetY());
            this.playerListYSlider.active = NoKnockbackClient.isPlayerListEnabled();
        }
        if (this.playerListScaleSlider != null) {
            this.playerListScaleSlider.sync(NoKnockbackClient.getPlayerListTextScale());
            this.playerListScaleSlider.active = NoKnockbackClient.isPlayerListEnabled();
        }
        if (this.playerListMaxHeightSlider != null) {
            this.playerListMaxHeightSlider.sync(NoKnockbackClient.getPlayerListMaxHeight());
            this.playerListMaxHeightSlider.active = NoKnockbackClient.isPlayerListEnabled();
        }
        if (this.playerListAlphaSlider != null) {
            this.playerListAlphaSlider.sync(NoKnockbackClient.getPlayerListAlphaMultiplier());
            this.playerListAlphaSlider.active = NoKnockbackClient.isPlayerListEnabled();
        }
        if (this.menuAlwaysOnPlaceholderButton != null) {
            this.menuAlwaysOnPlaceholderButton.setMessage(Text.literal("Enabled: ON"));
            this.menuAlwaysOnPlaceholderButton.active = false;
        }

        for (Map.Entry<KeyBinding, ButtonWidget> entry : this.keyButtons.entrySet()) {
            KeyBinding binding = entry.getKey();
            ButtonWidget button = entry.getValue();
            if (this.waitingForKey == binding) {
                button.setMessage(Text.literal("Bind: [Press key]"));
            } else {
                button.setMessage(Text.literal("Bind: ").append(binding.getBoundKeyLocalizedText()));
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

        int trackX1 = left + PANEL_WIDTH - 4;
        int trackX2 = trackX1 + 4;
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

    private Text sectionText(String title, boolean expanded, boolean enabled) {
        return Text.literal((expanded ? "v " : "> ") + title + " [" + (enabled ? "ON" : "OFF") + "]");
    }

    private boolean isExpanded(SectionId sectionId) {
        return this.expandedSections.getOrDefault(sectionId, false);
    }

    private Text rayOriginText(NoKnockbackClient.RayOrigin origin) {
        return origin == NoKnockbackClient.RayOrigin.CENTER ? Text.literal("Center") : Text.literal("Bottom");
    }

    private Text anchorModeText(NoKnockbackClient.OverlayAnchorMode mode) {
        return mode == NoKnockbackClient.OverlayAnchorMode.RAY_MIDDLE ? Text.literal("Middle of Ray") : Text.literal("Above Player");
    }

    private Text colorModeText(NoKnockbackClient.VisualColorMode mode) {
        return switch (mode) {
            case NICK -> Text.literal("Nick");
            case VIVID -> Text.literal("Vivid");
            case GRADIENT -> Text.literal("Gradient");
            case RAINBOW -> Text.literal("Rainbow");
        };
    }

    private enum SectionId {
        SPEED,
        ESP,
        RAYS,
        TARGET_HEALTH,
        PLAYER_LIST,
        MENU
    }

    private record SectionHeader(ButtonWidget button, String title, @Nullable BooleanSupplier stateSupplier) {
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
