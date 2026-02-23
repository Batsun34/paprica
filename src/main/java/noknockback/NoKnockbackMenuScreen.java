
package noknockback;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.option.SimpleOption;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.Text;
import net.minecraft.util.math.MathHelper;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.DoubleConsumer;
import java.util.function.DoubleSupplier;
import java.util.function.IntConsumer;
import java.util.function.IntSupplier;
import java.util.function.Supplier;

public class NoKnockbackMenuScreen extends Screen {
    private static final int WINDOW_WIDTH = 940;
    private static final int WINDOW_HEIGHT = 560;
    private static final int HEADER_HEIGHT = 32;
    private static final int SIDEBAR_WIDTH = 220;
    private static final int PADDING = 12;
    private static final int COLUMN_GAP = 14;
    private static final int PANEL_GAP = 12;
    private static final int PANEL_PADDING = 10;
    private static final int PANEL_HEADER_HEIGHT = 18;
    private static final int ROW_HEIGHT = 18;
    private static final int ROW_GAP = 6;
    private static final int CONTROL_WIDTH = 170;
    private static final int MODULE_HEIGHT = 22;
    private static final int MODULE_GAP = 4;
    private static final int GROUP_GAP = 10;
    private static final int SCROLL_STEP = 24;

    private static final int COLOR_DIM = 0xAA0B0E14;
    private static final int COLOR_WINDOW_BG = 0xF012141B;
    private static final int COLOR_WINDOW_BORDER = 0xFF273040;
    private static final int COLOR_HEADER_BG = 0xFF0E1118;
    private static final int COLOR_SIDEBAR_BG = 0xFF0D1118;
    private static final int COLOR_PANEL_BG = 0xFF141B26;
    private static final int COLOR_PANEL_BORDER = 0xFF202A3A;
    private static final int COLOR_ACCENT = 0xFF4CB1FF;
    private static final int COLOR_ACCENT_DARK = 0xFF1E3E5B;
    private static final int COLOR_TEXT = 0xFFE6EEF7;
    private static final int COLOR_TEXT_DIM = 0xFF9FB0C7;
    private static final int COLOR_TEXT_MUTED = 0xFF7E8EA6;
    private static final int COLOR_TOGGLE_OFF = 0xFF2A3444;
    private static final int COLOR_CONTROL_BG = 0xFF1A2230;
    private static final int COLOR_CONTROL_BORDER = 0xFF2B3647;
    private static final int COLOR_ROW_HOVER = 0x1FFFFFFF;

    @Nullable
    private final Screen parent;
    private ModuleTab selectedModule = ModuleTab.RAYS;
    private double scrollOffset = 0.0;
    private double maxScroll = 0.0;

    private int windowX;
    private int windowY;
    private int windowWidth;
    private int windowHeight;
    private int sidebarX;
    private int sidebarY;
    private int sidebarWidth;
    private int contentX;
    private int contentY;
    private int contentWidth;
    private int contentHeight;

    private final List<ModuleEntry> moduleEntries = new ArrayList<>();
    private final List<HitTarget> hitTargets = new ArrayList<>();

    @Nullable
    private KeyBinding waitingForKey;
    private boolean ignoreNextMouseBind;
    @Nullable
    private SliderDrag sliderDrag;
    @Nullable
    private Integer previousBlurValue;
    @Nullable
    private String openDropdownId;
    @Nullable
    private Rect openDropdownRect;
    @Nullable
    private Control openDropdownControl;

    public NoKnockbackMenuScreen(@Nullable Screen parent) {
        super(Text.literal("Paprika"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        if (this.client != null) {
            SimpleOption<Integer> blurOption = this.client.options.getMenuBackgroundBlurriness();
            this.previousBlurValue = blurOption.getValue();
            int desired = Math.max(6, blurOption.getValue());
            blurOption.setValue(desired);
        }
    }

    @Override
    public void removed() {
        if (this.client != null && this.previousBlurValue != null) {
            this.client.options.getMenuBackgroundBlurriness().setValue(this.previousBlurValue);
        }
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    @Override
    public void close() {
        if (this.client != null) {
            this.client.setScreen(this.parent);
        }
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        if (this.client != null && this.client.world != null) {
            this.applyBlur();
        }
        context.fill(0, 0, this.width, this.height, COLOR_DIM);

        this.updateLayout();
        this.drawWindow(context);
        this.drawHeader(context);
        this.drawSidebar(context, mouseX, mouseY);
        this.drawContent(context, mouseX, mouseY);

        if (this.waitingForKey != null) {
            String label = "Press a key (ESC to cancel)";
            int textWidth = this.textRenderer.getWidth(label);
            int x = this.windowX + this.windowWidth - textWidth - 14;
            int y = this.windowY + this.windowHeight - 18;
            context.drawTextWithShadow(this.textRenderer, label, x, y, COLOR_TEXT_MUTED);
        }
    }
    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (this.waitingForKey != null) {
            if (this.ignoreNextMouseBind) {
                this.ignoreNextMouseBind = false;
                return true;
            }
            bindKey(this.waitingForKey, InputUtil.Type.MOUSE.createFromCode(button));
            this.waitingForKey = null;
            return true;
        }

        for (ModuleEntry entry : this.moduleEntries) {
            if (entry.rect.contains(mouseX, mouseY)) {
                if (this.selectedModule != entry.module) {
                    this.selectedModule = entry.module;
                    this.scrollOffset = 0.0;
                    this.openDropdownId = null;
                }
                return true;
            }
        }

        if (!isInsideContent(mouseX, mouseY)) {
            return super.mouseClicked(mouseX, mouseY, button);
        }

        boolean handled = false;
        HitTarget hitTarget = null;
        for (HitTarget target : this.hitTargets) {
            if (target.rect.contains(mouseX, mouseY)) {
                hitTarget = target;
                break;
            }
        }

        if (hitTarget != null) {
            switch (hitTarget.type) {
                case TOGGLE -> {
                    boolean current = hitTarget.control.toggleGetter.getAsBoolean();
                    hitTarget.control.toggleSetter.accept(!current);
                    handled = true;
                }
                case DROPDOWN -> {
                    if (hitTarget.control != null) {
                        if (hitTarget.control.id.equals(this.openDropdownId)) {
                            this.openDropdownId = null;
                        } else {
                            this.openDropdownId = hitTarget.control.id;
                        }
                    }
                    handled = true;
                }
                case DROPDOWN_OPTION -> {
                    hitTarget.control.cycleSetter.accept(hitTarget.optionIndex);
                    this.openDropdownId = null;
                    handled = true;
                }
                case KEYBIND -> {
                    if (hitTarget.control.keyBinding != null) {
                        this.waitingForKey = hitTarget.control.keyBinding;
                        this.ignoreNextMouseBind = true;
                        this.openDropdownId = null;
                    }
                    handled = true;
                }
                case SLIDER -> {
                    this.sliderDrag = new SliderDrag(hitTarget.control, hitTarget.track);
                    updateSlider(hitTarget.control, hitTarget.track, mouseX);
                    this.openDropdownId = null;
                    handled = true;
                }
            }
        }

        if (!handled && this.openDropdownId != null) {
            this.openDropdownId = null;
        }

        if (handled) {
            return true;
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        if (this.sliderDrag != null) {
            updateSlider(this.sliderDrag.control, this.sliderDrag.track, mouseX);
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        this.sliderDrag = null;
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (!isInsideContent(mouseX, mouseY)) {
            return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
        }
        if (this.maxScroll <= 0.0) {
            return true;
        }
        this.scrollOffset = MathHelper.clamp(this.scrollOffset + verticalAmount * SCROLL_STEP, -this.maxScroll, 0.0);
        return true;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (this.waitingForKey != null) {
            if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
                this.waitingForKey = null;
                return true;
            }
            if (keyCode == GLFW.GLFW_KEY_BACKSPACE || keyCode == GLFW.GLFW_KEY_DELETE) {
                bindKey(this.waitingForKey, InputUtil.UNKNOWN_KEY);
                this.waitingForKey = null;
                return true;
            }
            bindKey(this.waitingForKey, InputUtil.fromKeyCode(keyCode, scanCode));
            this.waitingForKey = null;
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    private void bindKey(KeyBinding keyBinding, InputUtil.Key key) {
        if (keyBinding == null || key == null) return;
        keyBinding.setBoundKey(key);
        KeyBinding.updateKeysByCode();
        NoKnockbackClient.saveConfigNow();
    }

    private boolean isInsideContent(double mouseX, double mouseY) {
        return mouseX >= this.contentX && mouseX <= this.contentX + this.contentWidth
                && mouseY >= this.contentY && mouseY <= this.contentY + this.contentHeight;
    }

    private void updateLayout() {
        this.windowWidth = Math.min(WINDOW_WIDTH, this.width - 20);
        this.windowHeight = Math.min(WINDOW_HEIGHT, this.height - 20);
        if (this.windowWidth < 540) {
            this.windowWidth = Math.max(320, this.width - 20);
        }
        if (this.windowHeight < 360) {
            this.windowHeight = Math.max(240, this.height - 20);
        }

        this.windowX = (this.width - this.windowWidth) / 2;
        this.windowY = (this.height - this.windowHeight) / 2;
        this.sidebarX = this.windowX;
        this.sidebarY = this.windowY + HEADER_HEIGHT;
        this.sidebarWidth = Math.min(SIDEBAR_WIDTH, this.windowWidth / 3);
        this.contentX = this.windowX + this.sidebarWidth + 1;
        this.contentY = this.windowY + HEADER_HEIGHT;
        this.contentWidth = this.windowWidth - this.sidebarWidth - 1;
        this.contentHeight = this.windowHeight - HEADER_HEIGHT;
    }

    private void drawWindow(DrawContext context) {
        int x1 = this.windowX;
        int y1 = this.windowY;
        int x2 = x1 + this.windowWidth;
        int y2 = y1 + this.windowHeight;
        context.fill(x1, y1, x2, y2, COLOR_WINDOW_BG);
        drawOutline(context, x1, y1, x2, y2, COLOR_WINDOW_BORDER);
        context.fill(x1, y1, x2, y1 + HEADER_HEIGHT, COLOR_HEADER_BG);
        context.fill(this.sidebarX, this.sidebarY, this.sidebarX + this.sidebarWidth, y2, COLOR_SIDEBAR_BG);
        context.fill(this.sidebarX + this.sidebarWidth, this.sidebarY, this.sidebarX + this.sidebarWidth + 1, y2, COLOR_PANEL_BORDER);
    }

    private void drawHeader(DrawContext context) {
        int titleX = this.windowX + 14;
        int titleY = this.windowY + 9;
        drawScaledText(context, "Paprika", titleX, titleY, 1.2F, COLOR_TEXT);

        String moduleTitle = this.selectedModule.title;
        int moduleWidth = this.textRenderer.getWidth(moduleTitle);
        int moduleX = this.windowX + this.windowWidth - moduleWidth - 16;
        int moduleY = this.windowY + 11;
        context.drawTextWithShadow(this.textRenderer, moduleTitle, moduleX, moduleY, COLOR_TEXT_DIM);
    }

    private void drawSidebar(DrawContext context, int mouseX, int mouseY) {
        this.moduleEntries.clear();

        int x = this.sidebarX + PADDING;
        int y = this.sidebarY + PADDING;
        for (ModuleGroup group : ModuleGroup.values()) {
            context.drawTextWithShadow(this.textRenderer, group.title, x, y, COLOR_TEXT_MUTED);
            y += 14;
            for (ModuleTab tab : ModuleTab.values()) {
                if (tab.group != group) continue;
                Rect rect = new Rect(x - 6, y - 2, this.sidebarWidth - PADDING * 2 + 12, MODULE_HEIGHT);
                boolean hovered = rect.contains(mouseX, mouseY);
                boolean selected = tab == this.selectedModule;
                if (selected) {
                    context.fill(rect.x, rect.y, rect.x + rect.width, rect.y + rect.height, COLOR_ACCENT_DARK);
                    context.fill(rect.x, rect.y, rect.x + 3, rect.y + rect.height, COLOR_ACCENT);
                } else if (hovered) {
                    context.fill(rect.x, rect.y, rect.x + rect.width, rect.y + rect.height, COLOR_ROW_HOVER);
                }
                context.drawTextWithShadow(this.textRenderer, tab.title, rect.x + 10, rect.y + 6, selected ? COLOR_TEXT : COLOR_TEXT_DIM);
                this.moduleEntries.add(new ModuleEntry(tab, rect));
                y += MODULE_HEIGHT + MODULE_GAP;
            }
            y += GROUP_GAP;
        }
    }
    private void drawContent(DrawContext context, int mouseX, int mouseY) {
        this.hitTargets.clear();
        this.openDropdownRect = null;
        this.openDropdownControl = null;

        int innerX = this.contentX + PADDING;
        int innerY = this.contentY + PADDING;
        int innerWidth = this.contentWidth - PADDING * 2;
        int innerHeight = this.contentHeight - PADDING * 2;

        List<Panel> panels = buildPanels(this.selectedModule);
        int columnWidth = (innerWidth - COLUMN_GAP) / 2;
        int colX0 = innerX;
        int colX1 = innerX + columnWidth + COLUMN_GAP;

        int col0Y = innerY;
        int col1Y = innerY;

        context.enableScissor(innerX, innerY, innerX + innerWidth, innerY + innerHeight);

        for (Panel panel : panels) {
            int panelWidth = columnWidth;
            int panelX = panel.column == 0 ? colX0 : colX1;
            int panelY = (int) ((panel.column == 0 ? col0Y : col1Y) + this.scrollOffset);
            int panelHeight = computePanelHeight(panel);

            drawPanel(context, panel, panelX, panelY, panelWidth, mouseX, mouseY);

            if (panel.column == 0) {
                col0Y += panelHeight + PANEL_GAP;
            } else {
                col1Y += panelHeight + PANEL_GAP;
            }
        }

        if (this.openDropdownId != null && this.openDropdownControl != null && this.openDropdownRect != null) {
            drawDropdownOptions(context, this.openDropdownControl, this.openDropdownRect, mouseX, mouseY);
        } else if (this.openDropdownId != null) {
            this.openDropdownId = null;
        }

        context.disableScissor();

        int col0Height = col0Y - innerY;
        int col1Height = col1Y - innerY;
        int maxColumnHeight = Math.max(col0Height, col1Height);
        this.maxScroll = Math.max(0, maxColumnHeight - innerHeight);
        this.scrollOffset = MathHelper.clamp(this.scrollOffset, -this.maxScroll, 0.0);

        drawScrollBar(context, innerX + innerWidth - 4, innerY, innerHeight);
    }

    private void drawPanel(DrawContext context, Panel panel, int x, int y, int width, int mouseX, int mouseY) {
        int height = computePanelHeight(panel);
        context.fill(x, y, x + width, y + height, COLOR_PANEL_BG);
        drawOutline(context, x, y, x + width, y + height, COLOR_PANEL_BORDER);
        context.fill(x, y, x + width, y + 2, COLOR_ACCENT_DARK);
        context.drawTextWithShadow(this.textRenderer, panel.title, x + PANEL_PADDING, y + 4, COLOR_TEXT);

        int rowY = y + PANEL_HEADER_HEIGHT + PANEL_PADDING;
        for (Control control : panel.controls) {
            drawControl(context, control, x + PANEL_PADDING, rowY, width - PANEL_PADDING * 2, mouseX, mouseY);
            rowY += ROW_HEIGHT + ROW_GAP;
        }
    }

    private void drawControl(DrawContext context, Control control, int x, int y, int width, int mouseX, int mouseY) {
        context.drawTextWithShadow(this.textRenderer, control.label, x, y + 4, COLOR_TEXT_DIM);
        int controlX = x + width - CONTROL_WIDTH;
        Rect rect = new Rect(controlX, y, CONTROL_WIDTH, ROW_HEIGHT);
        boolean hovered = rect.contains(mouseX, mouseY);

        switch (control.type) {
            case TOGGLE -> {
                boolean enabled = control.toggleGetter.getAsBoolean();
                int bg = enabled ? COLOR_ACCENT : COLOR_TOGGLE_OFF;
                int fg = enabled ? COLOR_TEXT : COLOR_TEXT_MUTED;
                context.fill(rect.x, rect.y + 2, rect.x + rect.width, rect.y + rect.height - 2, bg);
                drawOutline(context, rect.x, rect.y + 2, rect.x + rect.width, rect.y + rect.height - 2, COLOR_CONTROL_BORDER);
                if (hovered) {
                    context.fill(rect.x, rect.y + 2, rect.x + rect.width, rect.y + rect.height - 2, COLOR_ROW_HOVER);
                }
                String label = enabled ? "ON" : "OFF";
                context.drawCenteredTextWithShadow(this.textRenderer, label, rect.x + rect.width / 2, rect.y + 5, fg);
                this.hitTargets.add(new HitTarget(HitType.TOGGLE, rect, control, rect, -1));
            }
            case KEYBIND -> {
                int bg = hovered ? COLOR_CONTROL_BORDER : COLOR_CONTROL_BG;
                context.fill(rect.x, rect.y + 2, rect.x + rect.width, rect.y + rect.height - 2, bg);
                drawOutline(context, rect.x, rect.y + 2, rect.x + rect.width, rect.y + rect.height - 2, COLOR_CONTROL_BORDER);
                String keyLabel = "Unbound";
                if (control.keyBinding != null) {
                    keyLabel = control.keyBinding.getBoundKeyLocalizedText().getString();
                }
                if (this.waitingForKey == control.keyBinding) {
                    keyLabel = "Press key";
                }
                context.drawCenteredTextWithShadow(this.textRenderer, keyLabel, rect.x + rect.width / 2, rect.y + 5, COLOR_TEXT);
                this.hitTargets.add(new HitTarget(HitType.KEYBIND, rect, control, rect, -1));
            }
            case CYCLE -> {
                int bg = hovered ? COLOR_CONTROL_BORDER : COLOR_CONTROL_BG;
                context.fill(rect.x, rect.y + 2, rect.x + rect.width, rect.y + rect.height - 2, bg);
                drawOutline(context, rect.x, rect.y + 2, rect.x + rect.width, rect.y + rect.height - 2, COLOR_CONTROL_BORDER);
                int idx = MathHelper.clamp(control.cycleGetter.getAsInt(), 0, control.cycleLabels.length - 1);
                String value = control.cycleLabels[idx];
                int valueWidth = this.textRenderer.getWidth(value);
                int valueX = rect.x + (rect.width - valueWidth) / 2;
                context.drawTextWithShadow(this.textRenderer, value, valueX, rect.y + 5, COLOR_TEXT);
                boolean isOpen = control.id.equals(this.openDropdownId);
                String arrow = isOpen ? "^" : "v";
                context.drawTextWithShadow(this.textRenderer, arrow, rect.x + rect.width - 10, rect.y + 5, COLOR_TEXT_MUTED);
                this.hitTargets.add(new HitTarget(HitType.DROPDOWN, rect, control, rect, -1));
                if (isOpen) {
                    this.openDropdownRect = rect;
                    this.openDropdownControl = control;
                }
            }
            case SLIDER -> {
                double value = control.sliderGetter.getAsDouble();
                double t = (value - control.min) / (control.max - control.min);
                t = MathHelper.clamp(t, 0.0, 1.0);

                int trackX = rect.x + 6;
                int trackY = rect.y + rect.height - 6;
                int trackWidth = rect.width - 12;
                int trackHeight = 4;
                int fillWidth = (int) Math.round(trackWidth * t);
                Rect track = new Rect(trackX, trackY, trackWidth, trackHeight);

                context.fill(rect.x, rect.y + 2, rect.x + rect.width, rect.y + rect.height - 2, COLOR_CONTROL_BG);
                drawOutline(context, rect.x, rect.y + 2, rect.x + rect.width, rect.y + rect.height - 2, COLOR_CONTROL_BORDER);
                context.fill(track.x, track.y, track.x + track.width, track.y + track.height, COLOR_TOGGLE_OFF);
                context.fill(track.x, track.y, track.x + fillWidth, track.y + track.height, COLOR_ACCENT);

                String valueLabel = formatSliderValue(control, value);
                int valueWidth = this.textRenderer.getWidth(valueLabel);
                int valueX = rect.x + rect.width - valueWidth - 6;
                context.drawTextWithShadow(this.textRenderer, valueLabel, valueX, rect.y + 4, COLOR_TEXT_MUTED);

                if (hovered) {
                    context.fill(rect.x, rect.y + 2, rect.x + rect.width, rect.y + rect.height - 2, COLOR_ROW_HOVER);
                }
                this.hitTargets.add(new HitTarget(HitType.SLIDER, rect, control, track, -1));
            }
            case LABEL -> {
                String value = control.labelValue.get();
                int valueWidth = this.textRenderer.getWidth(value);
                int valueX = rect.x + rect.width - valueWidth - 4;
                context.drawTextWithShadow(this.textRenderer, value, valueX, rect.y + 4, COLOR_TEXT_MUTED);
            }
        }
    }

    private void drawDropdownOptions(DrawContext context, Control control, Rect baseRect, int mouseX, int mouseY) {
        if (control.cycleLabels == null || control.cycleLabels.length == 0) return;

        int optionHeight = ROW_HEIGHT;
        int optionCount = control.cycleLabels.length;
        int listX = baseRect.x;
        int listY = baseRect.y + baseRect.height + 2;
        int listWidth = baseRect.width;
        int listHeight = optionHeight * optionCount;

        context.fill(listX, listY, listX + listWidth, listY + listHeight, COLOR_CONTROL_BG);
        drawOutline(context, listX, listY, listX + listWidth, listY + listHeight, COLOR_CONTROL_BORDER);

        int selectedIndex = MathHelper.clamp(control.cycleGetter.getAsInt(), 0, optionCount - 1);
        for (int i = 0; i < optionCount; i++) {
            Rect optionRect = new Rect(listX, listY + i * optionHeight, listWidth, optionHeight);
            boolean hovered = optionRect.contains(mouseX, mouseY);
            boolean selected = i == selectedIndex;

            if (selected) {
                context.fill(optionRect.x, optionRect.y, optionRect.x + optionRect.width, optionRect.y + optionRect.height, COLOR_ACCENT_DARK);
            } else if (hovered) {
                context.fill(optionRect.x, optionRect.y, optionRect.x + optionRect.width, optionRect.y + optionRect.height, COLOR_ROW_HOVER);
            }

            int textX = optionRect.x + 6;
            int textY = optionRect.y + 5;
            int color = selected ? COLOR_TEXT : COLOR_TEXT_DIM;
            context.drawTextWithShadow(this.textRenderer, control.cycleLabels[i], textX, textY, color);
            this.hitTargets.add(new HitTarget(HitType.DROPDOWN_OPTION, optionRect, control, optionRect, i));
        }
    }

    private String formatSliderValue(Control control, double value) {
        if (control.step >= 1.0) {
            return Integer.toString((int) Math.round(value));
        }
        if (control.step >= 0.1) {
            return String.format(Locale.ROOT, "%.1f", value);
        }
        return String.format(Locale.ROOT, "%.2f", value);
    }

    private void drawScrollBar(DrawContext context, int x, int y, int height) {
        if (this.maxScroll <= 0.0) return;
        int barHeight = Math.max(24, (int) ((height / (height + this.maxScroll)) * height));
        int barY = y + (int) ((-this.scrollOffset / this.maxScroll) * (height - barHeight));
        context.fill(x, y, x + 2, y + height, COLOR_CONTROL_BG);
        context.fill(x, barY, x + 2, barY + barHeight, COLOR_ACCENT);
    }

    private void updateSlider(Control control, Rect track, double mouseX) {
        double t = (mouseX - track.x) / (double) track.width;
        t = MathHelper.clamp(t, 0.0, 1.0);
        double value = control.min + (control.max - control.min) * t;
        if (control.step > 0.0) {
            value = Math.round((value - control.min) / control.step) * control.step + control.min;
        }
        value = MathHelper.clamp(value, control.min, control.max);
        control.sliderSetter.accept(value);
    }

    private int computePanelHeight(Panel panel) {
        int controls = panel.controls.size();
        if (controls == 0) {
            return PANEL_HEADER_HEIGHT + PANEL_PADDING * 2;
        }
        return PANEL_HEADER_HEIGHT + PANEL_PADDING * 2 + controls * ROW_HEIGHT + (controls - 1) * ROW_GAP;
    }

    private void drawOutline(DrawContext context, int x1, int y1, int x2, int y2, int color) {
        context.fill(x1, y1, x2, y1 + 1, color);
        context.fill(x1, y2 - 1, x2, y2, color);
        context.fill(x1, y1, x1 + 1, y2, color);
        context.fill(x2 - 1, y1, x2, y2, color);
    }

    private void drawScaledText(DrawContext context, String text, int x, int y, float scale, int color) {
        context.getMatrices().push();
        context.getMatrices().translate(x, y, 0.0F);
        context.getMatrices().scale(scale, scale, 1.0F);
        context.drawTextWithShadow(this.textRenderer, text, 0, 0, color);
        context.getMatrices().pop();
    }
    private List<Panel> buildPanels(ModuleTab module) {
        List<Panel> panels = new ArrayList<>();
        switch (module) {
            case NO_KNOCKBACK -> panels.add(new Panel("No Knockback", 0, List.of(
                    Control.toggle("no_knockback.enabled", "Enabled", NoKnockbackClient::isNoKnockbackEnabled, NoKnockbackClient::setNoKnockbackEnabled),
                    Control.keybind("no_knockback.bind", "Bind", NoKnockbackClient.getNoKnockbackKeyBinding())
            )));
            case SPEED -> panels.add(new Panel("Sneak Movement Speed", 0, List.of(
                    Control.toggle("speed.enabled", "Enabled", NoKnockbackClient::isSpeedEnabled, NoKnockbackClient::setSpeedEnabled),
                    Control.keybind("speed.bind", "Bind", NoKnockbackClient.getSpeedToggleKeyBinding())
            )));
            case ESP -> panels.add(new Panel("ESP", 0, List.of(
                    Control.toggle("esp.enabled", "Enabled", NoKnockbackClient::isPlayerEspEnabled, NoKnockbackClient::setPlayerEspEnabled),
                    Control.keybind("esp.bind", "Bind", NoKnockbackClient.getPlayerEspKeyBinding()),
                    Control.slider("esp.outline_thickness", "Outline Thickness", 0.5, 6.0, 0.1, NoKnockbackClient::getOutlineThickness, value -> NoKnockbackClient.setOutlineThickness((float) value))
            )));
            case RAYS -> {
                panels.add(new Panel("Rays", 0, List.of(
                        Control.toggle("rays.enabled", "Enabled", NoKnockbackClient::isPlayerRaysEnabled, NoKnockbackClient::setPlayerRaysEnabled),
                        Control.keybind("rays.bind", "Bind", NoKnockbackClient.getPlayerRaysKeyBinding()),
                        Control.cycle("rays.origin", "Ray Origin", new String[]{"Bottom", "Center"},
                                () -> NoKnockbackClient.getRayOrigin() == NoKnockbackClient.RayOrigin.BOTTOM ? 0 : 1,
                                idx -> NoKnockbackClient.setRayOrigin(idx == 0 ? NoKnockbackClient.RayOrigin.BOTTOM : NoKnockbackClient.RayOrigin.CENTER)
                        ),
                        Control.slider("rays.bottom_height", "Bottom Start Height", 0.0, 300.0, 1.0, NoKnockbackClient::getRayBottomStartHeight, value -> NoKnockbackClient.setRayBottomStartHeight((float) value)),
                        Control.slider("rays.thickness", "Ray Thickness", 0.5, 8.0, 0.1, NoKnockbackClient::getRayThickness, value -> NoKnockbackClient.setRayThickness((float) value)),
                        Control.slider("rays.alpha", "Ray Alpha", 0.1, 1.0, 0.05, NoKnockbackClient::getRayAlpha, value -> NoKnockbackClient.setRayAlpha((float) value)),
                        Control.toggle("rays.glow", "Ray Glow", NoKnockbackClient::isRayVisualGlowEnabled, NoKnockbackClient::setRayVisualGlowEnabled),
                        Control.cycle("rays.color_mode", "Ray Color Mode", Control.COLOR_MODES,
                                () -> NoKnockbackClient.getRayVisualColorMode().ordinal(),
                                idx -> NoKnockbackClient.setRayVisualColorMode(NoKnockbackClient.VisualColorMode.values()[idx])
                        ),
                        Control.slider("rays.saturation", "Ray Saturation", 1.0, 2.5, 0.1, NoKnockbackClient::getRayVisualSaturationBoost, value -> NoKnockbackClient.setRayVisualSaturationBoost((float) value)),
                        Control.slider("rays.anim_speed", "Ray Animation Speed", 0.2, 4.0, 0.1, NoKnockbackClient::getRayVisualAnimationSpeed, value -> NoKnockbackClient.setRayVisualAnimationSpeed((float) value))
                )));
                panels.add(new Panel("Armor", 0, List.of(
                        Control.toggle("armor.enabled", "Armor Enabled", NoKnockbackClient::isPlayerArmorOverlayEnabled, NoKnockbackClient::setPlayerArmorOverlayEnabled),
                        Control.cycle("armor.position", "Armor Position", Control.ANCHOR_MODES,
                                () -> NoKnockbackClient.getArmorAnchorMode() == NoKnockbackClient.OverlayAnchorMode.ABOVE_PLAYER ? 0 : 1,
                                idx -> NoKnockbackClient.setArmorAnchorMode(idx == 0 ? NoKnockbackClient.OverlayAnchorMode.ABOVE_PLAYER : NoKnockbackClient.OverlayAnchorMode.RAY_MIDDLE)
                        ),
                        Control.slider("armor.size", "Armor Size", 0.35, 2.5, 0.1, NoKnockbackClient::getArmorOverlayScale, value -> NoKnockbackClient.setArmorOverlayScale((float) value)),
                        Control.slider("armor.alpha", "Armor Alpha", 0.1, 1.0, 0.05, NoKnockbackClient::getArmorAlpha, value -> NoKnockbackClient.setArmorAlpha((float) value)),
                        Control.toggle("armor.glow", "Armor Glow", NoKnockbackClient::isArmorVisualGlowEnabled, NoKnockbackClient::setArmorVisualGlowEnabled),
                        Control.cycle("armor.color_mode", "Armor Color Mode", Control.COLOR_MODES,
                                () -> NoKnockbackClient.getArmorVisualColorMode().ordinal(),
                                idx -> NoKnockbackClient.setArmorVisualColorMode(NoKnockbackClient.VisualColorMode.values()[idx])
                        ),
                        Control.slider("armor.saturation", "Armor Saturation", 1.0, 2.5, 0.1, NoKnockbackClient::getArmorVisualSaturationBoost, value -> NoKnockbackClient.setArmorVisualSaturationBoost((float) value)),
                        Control.slider("armor.anim_speed", "Armor Animation Speed", 0.2, 4.0, 0.1, NoKnockbackClient::getArmorVisualAnimationSpeed, value -> NoKnockbackClient.setArmorVisualAnimationSpeed((float) value))
                )));
                panels.add(new Panel("Held Item", 1, List.of(
                        Control.toggle("item.enabled", "Held Item Enabled", NoKnockbackClient::isHeldItemOverlayEnabled, NoKnockbackClient::setHeldItemOverlayEnabled),
                        Control.cycle("item.position", "Item Position", Control.ANCHOR_MODES,
                                () -> NoKnockbackClient.getHeldItemAnchorMode() == NoKnockbackClient.OverlayAnchorMode.ABOVE_PLAYER ? 0 : 1,
                                idx -> NoKnockbackClient.setHeldItemAnchorMode(idx == 0 ? NoKnockbackClient.OverlayAnchorMode.ABOVE_PLAYER : NoKnockbackClient.OverlayAnchorMode.RAY_MIDDLE)
                        ),
                        Control.slider("item.size", "Item Size", 0.35, 2.5, 0.1, NoKnockbackClient::getHeldItemOverlayScale, value -> NoKnockbackClient.setHeldItemOverlayScale((float) value)),
                        Control.slider("item.alpha", "Item Alpha", 0.1, 1.0, 0.05, NoKnockbackClient::getHeldItemAlpha, value -> NoKnockbackClient.setHeldItemAlpha((float) value)),
                        Control.toggle("item.glow", "Item Glow", NoKnockbackClient::isHeldItemVisualGlowEnabled, NoKnockbackClient::setHeldItemVisualGlowEnabled),
                        Control.cycle("item.color_mode", "Item Color Mode", Control.COLOR_MODES,
                                () -> NoKnockbackClient.getHeldItemVisualColorMode().ordinal(),
                                idx -> NoKnockbackClient.setHeldItemVisualColorMode(NoKnockbackClient.VisualColorMode.values()[idx])
                        ),
                        Control.slider("item.saturation", "Item Saturation", 1.0, 2.5, 0.1, NoKnockbackClient::getHeldItemVisualSaturationBoost, value -> NoKnockbackClient.setHeldItemVisualSaturationBoost((float) value)),
                        Control.slider("item.anim_speed", "Item Animation Speed", 0.2, 4.0, 0.1, NoKnockbackClient::getHeldItemVisualAnimationSpeed, value -> NoKnockbackClient.setHeldItemVisualAnimationSpeed((float) value))
                )));
                panels.add(new Panel("Distance", 1, List.of(
                        Control.toggle("distance.enabled", "Distance Enabled", NoKnockbackClient::isDistanceDisplayEnabled, NoKnockbackClient::setDistanceDisplayEnabled),
                        Control.cycle("distance.position", "Distance Position", Control.ANCHOR_MODES,
                                () -> NoKnockbackClient.getDistanceAnchorMode() == NoKnockbackClient.OverlayAnchorMode.ABOVE_PLAYER ? 0 : 1,
                                idx -> NoKnockbackClient.setDistanceAnchorMode(idx == 0 ? NoKnockbackClient.OverlayAnchorMode.ABOVE_PLAYER : NoKnockbackClient.OverlayAnchorMode.RAY_MIDDLE)
                        ),
                        Control.slider("distance.text_size", "Distance Text Size", 0.5, 2.0, 0.1, NoKnockbackClient::getDistanceTextScale, value -> NoKnockbackClient.setDistanceTextScale((float) value)),
                        Control.slider("distance.alpha", "Distance Alpha", 0.1, 1.0, 0.05, NoKnockbackClient::getDistanceAlpha, value -> NoKnockbackClient.setDistanceAlpha((float) value)),
                        Control.toggle("distance.glow", "Distance Glow", NoKnockbackClient::isDistanceVisualGlowEnabled, NoKnockbackClient::setDistanceVisualGlowEnabled),
                        Control.cycle("distance.color_mode", "Distance Color Mode", Control.COLOR_MODES,
                                () -> NoKnockbackClient.getDistanceVisualColorMode().ordinal(),
                                idx -> NoKnockbackClient.setDistanceVisualColorMode(NoKnockbackClient.VisualColorMode.values()[idx])
                        ),
                        Control.slider("distance.saturation", "Distance Saturation", 1.0, 2.5, 0.1, NoKnockbackClient::getDistanceVisualSaturationBoost, value -> NoKnockbackClient.setDistanceVisualSaturationBoost((float) value)),
                        Control.slider("distance.anim_speed", "Distance Animation Speed", 0.2, 4.0, 0.1, NoKnockbackClient::getDistanceVisualAnimationSpeed, value -> NoKnockbackClient.setDistanceVisualAnimationSpeed((float) value))
                )));
            }
            case TARGET_HEALTH -> panels.add(new Panel("Target Health", 0, List.of(
                    Control.toggle("target_health.enabled", "Enabled", NoKnockbackClient::isTargetHealthOverlayEnabled, NoKnockbackClient::setTargetHealthOverlayEnabled),
                    Control.toggle("target_health.dynamic_color", "Dynamic Color", NoKnockbackClient::isTargetHealthDynamicColorEnabled, NoKnockbackClient::setTargetHealthDynamicColorEnabled),
                    Control.slider("target_health.text_size", "Text Size", 0.5, 2.0, 0.1, NoKnockbackClient::getTargetHealthTextScale, value -> NoKnockbackClient.setTargetHealthTextScale((float) value))
            )));
            case PLAYER_LIST -> panels.add(new Panel("Player List", 0, List.of(
                    Control.toggle("player_list.enabled", "Enabled", NoKnockbackClient::isPlayerListEnabled, NoKnockbackClient::setPlayerListEnabled),
                    Control.keybind("player_list.bind", "Bind", NoKnockbackClient.getPlayerListKeyBinding()),
                    Control.slider("player_list.offset_x", "Offset X", 0.0, 4096.0, 1.0, () -> NoKnockbackClient.getPlayerListOffsetX(), value -> NoKnockbackClient.setPlayerListOffsetX((int) Math.round(value))),
                    Control.slider("player_list.offset_y", "Offset Y", 0.0, 4096.0, 1.0, () -> NoKnockbackClient.getPlayerListOffsetY(), value -> NoKnockbackClient.setPlayerListOffsetY((int) Math.round(value))),
                    Control.slider("player_list.scale", "Scale", 0.1, 2.0, 0.1, NoKnockbackClient::getPlayerListTextScale, value -> NoKnockbackClient.setPlayerListTextScale((float) value)),
                    Control.slider("player_list.max_height", "Max Height", 40.0, 4096.0, 10.0, NoKnockbackClient::getPlayerListMaxHeight, value -> NoKnockbackClient.setPlayerListMaxHeight((int) Math.round(value))),
                    Control.slider("player_list.alpha", "Alpha", 0.1, 1.0, 0.1, NoKnockbackClient::getPlayerListAlphaMultiplier, value -> NoKnockbackClient.setPlayerListAlphaMultiplier((float) value))
            )));
            case MENU -> panels.add(new Panel("Menu", 0, List.of(
                    Control.label("menu.enabled", "Enabled", () -> "Always ON"),
                    Control.keybind("menu.bind", "Menu Key", NoKnockbackClient.getOpenMenuKeyBinding())
            )));
        }

        return panels;
    }

    private enum ModuleGroup {
        MOVEMENT("Movement"),
        VISUAL("Visuals"),
        OVERLAY("Overlay"),
        SYSTEM("System");

        private final String title;

        ModuleGroup(String title) {
            this.title = title;
        }
    }

    private enum ModuleTab {
        NO_KNOCKBACK("No Knockback", ModuleGroup.MOVEMENT),
        SPEED("Sneak Movement Speed", ModuleGroup.MOVEMENT),
        ESP("Player ESP", ModuleGroup.VISUAL),
        RAYS("Rays", ModuleGroup.VISUAL),
        TARGET_HEALTH("Target Health", ModuleGroup.OVERLAY),
        PLAYER_LIST("Player List", ModuleGroup.OVERLAY),
        MENU("Menu", ModuleGroup.SYSTEM);

        private final String title;
        private final ModuleGroup group;

        ModuleTab(String title, ModuleGroup group) {
            this.title = title;
            this.group = group;
        }
    }

    private enum HitType {
        TOGGLE,
        KEYBIND,
        DROPDOWN,
        DROPDOWN_OPTION,
        SLIDER
    }

    private static final class Rect {
        private final int x;
        private final int y;
        private final int width;
        private final int height;

        private Rect(int x, int y, int width, int height) {
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
        }

        private boolean contains(double px, double py) {
            return px >= this.x && px <= this.x + this.width && py >= this.y && py <= this.y + this.height;
        }
    }

    private record ModuleEntry(ModuleTab module, Rect rect) {
    }

    private record HitTarget(HitType type, Rect rect, Control control, Rect track, int optionIndex) {
    }

    private record SliderDrag(Control control, Rect track) {
    }

    private static final class Panel {
        private final String title;
        private final int column;
        private final List<Control> controls;

        private Panel(String title, int column, List<Control> controls) {
            this.title = title;
            this.column = column;
            this.controls = controls;
        }
    }

    private static final class Control {
        private static final String[] COLOR_MODES = new String[]{"Nick", "Vivid", "Gradient", "Rainbow"};
        private static final String[] ANCHOR_MODES = new String[]{"Above", "Ray Middle"};

        private final String id;
        private final ControlType type;
        private final String label;
        private final BooleanSupplier toggleGetter;
        private final Consumer<Boolean> toggleSetter;
        private final DoubleSupplier sliderGetter;
        private final DoubleConsumer sliderSetter;
        private final double min;
        private final double max;
        private final double step;
        private final IntSupplier cycleGetter;
        private final IntConsumer cycleSetter;
        private final String[] cycleLabels;
        private final KeyBinding keyBinding;
        private final Supplier<String> labelValue;

        private Control(String id, ControlType type, String label, BooleanSupplier toggleGetter, Consumer<Boolean> toggleSetter,
                        DoubleSupplier sliderGetter, DoubleConsumer sliderSetter, double min, double max, double step,
                        IntSupplier cycleGetter, IntConsumer cycleSetter, String[] cycleLabels,
                        KeyBinding keyBinding, Supplier<String> labelValue) {
            this.id = id;
            this.type = type;
            this.label = label;
            this.toggleGetter = toggleGetter;
            this.toggleSetter = toggleSetter;
            this.sliderGetter = sliderGetter;
            this.sliderSetter = sliderSetter;
            this.min = min;
            this.max = max;
            this.step = step;
            this.cycleGetter = cycleGetter;
            this.cycleSetter = cycleSetter;
            this.cycleLabels = cycleLabels;
            this.keyBinding = keyBinding;
            this.labelValue = labelValue;
        }

        private static Control toggle(String id, String label, BooleanSupplier getter, Consumer<Boolean> setter) {
            return new Control(id, ControlType.TOGGLE, label, getter, setter, null, null, 0.0, 0.0, 0.0, null, null, null, null, () -> "");
        }

        private static Control keybind(String id, String label, KeyBinding keyBinding) {
            return new Control(id, ControlType.KEYBIND, label, () -> false, value -> {
            }, null, null, 0.0, 0.0, 0.0, null, null, null, keyBinding, () -> "");
        }

        private static Control slider(String id, String label, double min, double max, double step, DoubleSupplier getter, DoubleConsumer setter) {
            return new Control(id, ControlType.SLIDER, label, () -> false, value -> {
            }, getter, setter, min, max, step, null, null, null, null, () -> "");
        }

        private static Control cycle(String id, String label, String[] values, IntSupplier getter, IntConsumer setter) {
            return new Control(id, ControlType.CYCLE, label, () -> false, value -> {
            }, null, null, 0.0, 0.0, 0.0, getter, setter, values, null, () -> "");
        }

        private static Control label(String id, String label, Supplier<String> valueSupplier) {
            return new Control(id, ControlType.LABEL, label, () -> false, value -> {
            }, null, null, 0.0, 0.0, 0.0, null, null, null, null, valueSupplier);
        }
    }

    private enum ControlType {
        TOGGLE,
        KEYBIND,
        CYCLE,
        SLIDER,
        LABEL
    }
}
