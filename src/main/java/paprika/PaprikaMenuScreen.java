
package paprika;

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

public class PaprikaMenuScreen extends Screen {
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
    @Nullable
    private Rect openDropdownListRect;
    @Nullable
    private Control activeTextControl;
    private String friendInputText = "";

    public PaprikaMenuScreen(@Nullable Screen parent) {
        super(Text.literal("Paprika"));
        this.parent = parent;
        this.applySavedState();
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
        storeMenuState();
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
        storeMenuState();
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
                    storeMenuState();
                }
                return true;
            }
        }

        if (!isInsideContent(mouseX, mouseY)) {
            return super.mouseClicked(mouseX, mouseY, button);
        }

        boolean handled = false;
        HitTarget hitTarget = null;
        for (int i = this.hitTargets.size() - 1; i >= 0; i--) {
            HitTarget target = this.hitTargets.get(i);
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
                case BUTTON -> {
                    if (hitTarget.control.buttonAction != null) {
                        hitTarget.control.buttonAction.run();
                    }
                    handled = true;
                }
                case TEXT -> {
                    this.activeTextControl = hitTarget.control;
                    handled = true;
                }
            }
        }

        if (!handled && this.openDropdownId != null) {
            this.openDropdownId = null;
        }

        if (!handled) {
            this.activeTextControl = null;
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
        PaprikaClient.setMenuScrollOffset(this.scrollOffset);
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
        if (this.activeTextControl != null) {
            if (keyCode == GLFW.GLFW_KEY_ESCAPE || keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER) {
                this.activeTextControl = null;
                return true;
            }
            if (keyCode == GLFW.GLFW_KEY_BACKSPACE) {
                String current = this.activeTextControl.labelValue.get();
                if (!current.isEmpty()) {
                    if (this.activeTextControl.textSetter != null) {
                        this.activeTextControl.textSetter.accept(current.substring(0, current.length() - 1));
                    }
                }
                return true;
            }
            if (keyCode == GLFW.GLFW_KEY_DELETE) {
                if (this.activeTextControl.textSetter != null) {
                    this.activeTextControl.textSetter.accept("");
                }
                return true;
            }
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char chr, int modifiers) {
        if (this.activeTextControl != null) {
            if (Character.isISOControl(chr)) {
                return false;
            }
            String current = this.activeTextControl.labelValue.get();
            int limit = this.activeTextControl.textMaxLength;
            if (limit > 0 && current.length() >= limit) {
                return true;
            }
            if (this.activeTextControl.textSetter != null) {
                this.activeTextControl.textSetter.accept(current + chr);
            }
            return true;
        }
        return super.charTyped(chr, modifiers);
    }

    private void bindKey(KeyBinding keyBinding, InputUtil.Key key) {
        if (keyBinding == null || key == null) return;
        keyBinding.setBoundKey(key);
        KeyBinding.updateKeysByCode();
        PaprikaClient.saveConfigNow();
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
        this.openDropdownListRect = null;
        PaprikaClient.setMenuLastTabId(this.selectedModule.name());

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

        context.disableScissor();

        int col0Height = col0Y - innerY;
        int col1Height = col1Y - innerY;
        int maxColumnHeight = Math.max(col0Height, col1Height);
        this.maxScroll = Math.max(0, maxColumnHeight - innerHeight);
        if (this.openDropdownListRect != null) {
            int listBottom = this.openDropdownListRect.y + this.openDropdownListRect.height;
            int innerBottom = innerY + innerHeight;
            int extra = listBottom - innerBottom;
            if (extra > 0) {
                this.maxScroll += extra;
            }
        }
        this.scrollOffset = MathHelper.clamp(this.scrollOffset, -this.maxScroll, 0.0);

        drawScrollBar(context, innerX + innerWidth - 4, innerY, innerHeight);

        if (this.openDropdownId != null && this.openDropdownControl != null && this.openDropdownRect != null) {
            context.enableScissor(innerX, innerY, innerX + innerWidth, innerY + innerHeight);
            drawDropdownOptions(context, this.openDropdownControl, this.openDropdownRect, mouseX, mouseY);
            context.disableScissor();
        } else if (this.openDropdownId != null) {
            this.openDropdownId = null;
        }
    }

    private void applySavedState() {
        String savedTab = PaprikaClient.getMenuLastTabId();
        if (savedTab != null && !savedTab.isBlank()) {
            for (ModuleTab tab : ModuleTab.values()) {
                if (tab.name().equalsIgnoreCase(savedTab)) {
                    this.selectedModule = tab;
                    break;
                }
            }
        }
        this.scrollOffset = PaprikaClient.getMenuScrollOffset();
    }

    private void storeMenuState() {
        PaprikaClient.setMenuLastTabId(this.selectedModule.name());
        PaprikaClient.setMenuScrollOffset(this.scrollOffset);
        PaprikaClient.persistMenuState();
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
            case BUTTON -> {
                int bg = hovered ? COLOR_CONTROL_BORDER : COLOR_CONTROL_BG;
                context.fill(rect.x, rect.y + 2, rect.x + rect.width, rect.y + rect.height - 2, bg);
                drawOutline(context, rect.x, rect.y + 2, rect.x + rect.width, rect.y + rect.height - 2, COLOR_CONTROL_BORDER);
                String buttonLabel = control.labelValue.get();
                if (buttonLabel == null || buttonLabel.isBlank()) {
                    buttonLabel = control.label;
                }
                context.drawCenteredTextWithShadow(this.textRenderer, buttonLabel, rect.x + rect.width / 2, rect.y + 5, COLOR_TEXT);
                this.hitTargets.add(new HitTarget(HitType.BUTTON, rect, control, rect, -1));
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
                        this.openDropdownListRect = buildDropdownListRect(control, rect);
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
                    Rect valueRect = new Rect(valueX, rect.y + 4, valueWidth, this.textRenderer.fontHeight);
                    if (!isDropdownOverlapping(valueRect)) {
                        context.drawTextWithShadow(this.textRenderer, valueLabel, valueX, rect.y + 4, COLOR_TEXT_MUTED);
                    }

                if (hovered) {
                    context.fill(rect.x, rect.y + 2, rect.x + rect.width, rect.y + rect.height - 2, COLOR_ROW_HOVER);
                }
                this.hitTargets.add(new HitTarget(HitType.SLIDER, rect, control, track, -1));
            }
            case TEXT -> {
                boolean active = control == this.activeTextControl;
                int bg = hovered || active ? COLOR_CONTROL_BORDER : COLOR_CONTROL_BG;
                context.fill(rect.x, rect.y + 2, rect.x + rect.width, rect.y + rect.height - 2, bg);
                drawOutline(context, rect.x, rect.y + 2, rect.x + rect.width, rect.y + rect.height - 2, COLOR_CONTROL_BORDER);

                String current = control.labelValue.get();
                if (current == null) current = "";
                boolean showCaret = active && (System.currentTimeMillis() / 350) % 2 == 0;
                String display = current.isEmpty() ? "Type name" : current;
                if (showCaret) {
                    display = current + "_";
                }
                display = this.textRenderer.trimToWidth(display, rect.width - 10);
                int color = current.isEmpty() ? COLOR_TEXT_MUTED : COLOR_TEXT;
                context.drawTextWithShadow(this.textRenderer, display, rect.x + 6, rect.y + 5, color);
                this.hitTargets.add(new HitTarget(HitType.TEXT, rect, control, rect, -1));
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

        context.getMatrices().push();
        context.getMatrices().translate(0.0F, 0.0F, 200.0F);

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

        context.getMatrices().pop();
    }
    @Nullable
    private Rect buildDropdownListRect(Control control, Rect baseRect) {
        if (control.cycleLabels == null || control.cycleLabels.length == 0) return null;
        int optionHeight = ROW_HEIGHT;
        int listHeight = optionHeight * control.cycleLabels.length;
        int listX = baseRect.x;
        int listY = baseRect.y + baseRect.height + 2;
        return new Rect(listX, listY, baseRect.width, listHeight);
    }

    private boolean isDropdownOverlapping(Rect rect) {
        if (this.openDropdownListRect == null) return false;
        return this.openDropdownListRect.intersects(rect);
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
                    Control.toggle("no_knockback.enabled", "Enabled", PaprikaClient::isNoKnockbackEnabled, PaprikaClient::setNoKnockbackEnabled),
                    Control.keybind("no_knockback.bind", "Bind", PaprikaClient.getNoKnockbackKeyBinding())
            )));
            case SPEED -> panels.add(new Panel("Sneak Movement Speed", 0, List.of(
                    Control.toggle("speed.enabled", "Enabled", PaprikaClient::isSpeedEnabled, PaprikaClient::setSpeedEnabled),
                    Control.keybind("speed.bind", "Bind", PaprikaClient.getSpeedToggleKeyBinding())
            )));
            case AUTO_ATTACK -> {
                panels.add(new Panel("Auto Attack", 0, List.of(
                        Control.toggle("auto_attack.enabled", "Enabled", PaprikaClient::isAutoAttackEnabled, PaprikaClient::setAutoAttackEnabled),
                        Control.keybind("auto_attack.bind", "Bind", PaprikaClient.getAutoAttackKeyBinding()),
                        Control.cycle("auto_attack.mode", "Attack Mode", Control.AUTO_ATTACK_MODES,
                                () -> PaprikaClient.getAutoAttackMode().ordinal(),
                                idx -> PaprikaClient.setAutoAttackMode(PaprikaClient.AutoAttackMode.values()[idx])
                        ),
                        Control.slider("auto_attack.rate", "Hits Per Second", 1.0, 20.0, 0.5, PaprikaClient::getAutoAttackRate, value -> PaprikaClient.setAutoAttackRate((float) value)),
                        Control.slider("auto_attack.reach", "Max Reach", 3.0, 20.0, 0.5, PaprikaClient::getAutoAttackMaxDistance, value -> PaprikaClient.setAutoAttackMaxDistance((float) value)),
                        Control.toggle("auto_attack.los", "Require Line of Sight", PaprikaClient::isAutoAttackRequireLineOfSight, PaprikaClient::setAutoAttackRequireLineOfSight)
                )));
                panels.add(new Panel("Mark", 0, List.of(
                        Control.keybind("auto_attack.mark", "Mark Target", PaprikaClient.getMarkTargetKeyBinding()),
                        Control.keybind("auto_attack.unmark", "Unmark Target", PaprikaClient.getUnmarkTargetKeyBinding())
                )));
                panels.add(new Panel("Circle", 1, List.of(
                        Control.slider("auto_attack.radius", "Circle Radius", 20.0, 600.0, 2.0, PaprikaClient::getAutoAttackCircleRadius, value -> PaprikaClient.setAutoAttackCircleRadius((float) value)),
                        Control.cycle("auto_attack.circle_color", "Circle Color", Control.CIRCLE_COLOR_MODES,
                                () -> PaprikaClient.getAutoAttackCircleColorMode().ordinal(),
                                idx -> PaprikaClient.setAutoAttackCircleColorMode(PaprikaClient.CircleColorMode.values()[idx])
                        ),
                        Control.slider("auto_attack.circle_r", "Circle Red", 0, 255, 1, PaprikaClient::getAutoAttackCircleRed, value -> PaprikaClient.setAutoAttackCircleRed((int) value)),
                        Control.slider("auto_attack.circle_g", "Circle Green", 0, 255, 1, PaprikaClient::getAutoAttackCircleGreen, value -> PaprikaClient.setAutoAttackCircleGreen((int) value)),
                        Control.slider("auto_attack.circle_b", "Circle Blue", 0, 255, 1, PaprikaClient::getAutoAttackCircleBlue, value -> PaprikaClient.setAutoAttackCircleBlue((int) value))
                )));
            }
            case FRIENDS -> {
                List<Control> friendControls = new ArrayList<>();
                friendControls.add(Control.keybind("friends.mark", "Mark Friend", PaprikaClient.getMarkFriendKeyBinding()));
                friendControls.add(Control.textInput("friends.input", "Friend Name", () -> this.friendInputText, value -> this.friendInputText = value, 16));
                friendControls.add(Control.button("friends.add", "Add Friend", () -> "Add", () -> {
                    if (PaprikaClient.addFriendName(this.friendInputText)) {
                        this.friendInputText = "";
                    }
                }));
                friendControls.add(Control.button("friends.clear", "Clear Friends", () -> "Clear", PaprikaClient::clearFriends));
                panels.add(new Panel("Friends", 0, friendControls));

                List<Control> friendList = new ArrayList<>();
                List<String> names = PaprikaClient.getFriendNames();
                if (names.isEmpty()) {
                    friendList.add(Control.label("friends.empty", "No friends added", () -> ""));
                } else {
                    int idx = 1;
                    for (String name : names) {
                        friendList.add(Control.label("friends.entry." + idx, name, () -> ""));
                        idx++;
                    }
                }
                panels.add(new Panel("Friend List", 1, friendList));
            }
            case ESP -> panels.add(new Panel("ESP", 0, List.of(
                    Control.toggle("esp.enabled", "Enabled", PaprikaClient::isPlayerEspEnabled, PaprikaClient::setPlayerEspEnabled),
                    Control.keybind("esp.bind", "Bind", PaprikaClient.getPlayerEspKeyBinding()),
                    Control.slider("esp.outline_thickness", "Outline Thickness", 0.5, 6.0, 0.1, PaprikaClient::getOutlineThickness, value -> PaprikaClient.setOutlineThickness((float) value)),
                    Control.toggle("esp.glow", "Glow", PaprikaClient::isEspVisualGlowEnabled, PaprikaClient::setEspVisualGlowEnabled),
                    Control.cycle("esp.color_mode", "Color Mode", Control.COLOR_MODES,
                            () -> PaprikaClient.getEspVisualColorMode().ordinal(),
                            idx -> PaprikaClient.setEspVisualColorMode(PaprikaClient.VisualColorMode.values()[idx])
                    ),
                    Control.slider("esp.saturation", "Saturation", 1.0, 2.5, 0.1, PaprikaClient::getEspVisualSaturationBoost, value -> PaprikaClient.setEspVisualSaturationBoost((float) value)),
                    Control.slider("esp.anim_speed", "Animation Speed", 0.2, 4.0, 0.1, PaprikaClient::getEspVisualAnimationSpeed, value -> PaprikaClient.setEspVisualAnimationSpeed((float) value))
            )));
            case RAYS -> {
                panels.add(new Panel("Rays", 0, List.of(
                        Control.toggle("rays.enabled", "Enabled", PaprikaClient::isPlayerRaysEnabled, PaprikaClient::setPlayerRaysEnabled),
                        Control.keybind("rays.bind", "Bind", PaprikaClient.getPlayerRaysKeyBinding()),
                        Control.cycle("rays.origin", "Ray Origin", new String[]{"Bottom", "Center"},
                                () -> PaprikaClient.getRayOrigin() == PaprikaClient.RayOrigin.BOTTOM ? 0 : 1,
                                idx -> PaprikaClient.setRayOrigin(idx == 0 ? PaprikaClient.RayOrigin.BOTTOM : PaprikaClient.RayOrigin.CENTER)
                        ),
                        Control.slider("rays.bottom_height", "Bottom Start Height", 0.0, 300.0, 1.0, PaprikaClient::getRayBottomStartHeight, value -> PaprikaClient.setRayBottomStartHeight((float) value)),
                        Control.slider("rays.thickness", "Ray Thickness", 0.5, 8.0, 0.1, PaprikaClient::getRayThickness, value -> PaprikaClient.setRayThickness((float) value)),
                        Control.slider("rays.alpha", "Ray Alpha", 0.1, 1.0, 0.05, PaprikaClient::getRayAlpha, value -> PaprikaClient.setRayAlpha((float) value)),
                        Control.toggle("rays.glow", "Ray Glow", PaprikaClient::isRayVisualGlowEnabled, PaprikaClient::setRayVisualGlowEnabled),
                        Control.cycle("rays.color_mode", "Ray Color Mode", Control.COLOR_MODES,
                                () -> PaprikaClient.getRayVisualColorMode().ordinal(),
                                idx -> PaprikaClient.setRayVisualColorMode(PaprikaClient.VisualColorMode.values()[idx])
                        ),
                        Control.slider("rays.saturation", "Ray Saturation", 1.0, 2.5, 0.1, PaprikaClient::getRayVisualSaturationBoost, value -> PaprikaClient.setRayVisualSaturationBoost((float) value)),
                        Control.slider("rays.anim_speed", "Ray Animation Speed", 0.2, 4.0, 0.1, PaprikaClient::getRayVisualAnimationSpeed, value -> PaprikaClient.setRayVisualAnimationSpeed((float) value))
                )));
                panels.add(new Panel("Armor", 0, List.of(
                        Control.toggle("armor.enabled", "Armor Enabled", PaprikaClient::isPlayerArmorOverlayEnabled, PaprikaClient::setPlayerArmorOverlayEnabled),
                        Control.cycle("armor.position", "Armor Position", Control.ANCHOR_MODES,
                                () -> PaprikaClient.getArmorAnchorMode() == PaprikaClient.OverlayAnchorMode.ABOVE_PLAYER ? 0 : 1,
                                idx -> PaprikaClient.setArmorAnchorMode(idx == 0 ? PaprikaClient.OverlayAnchorMode.ABOVE_PLAYER : PaprikaClient.OverlayAnchorMode.RAY_MIDDLE)
                        ),
                        Control.slider("armor.size", "Armor Size", 0.35, 2.5, 0.1, PaprikaClient::getArmorOverlayScale, value -> PaprikaClient.setArmorOverlayScale((float) value)),
                        Control.slider("armor.alpha", "Armor Alpha", 0.1, 1.0, 0.05, PaprikaClient::getArmorAlpha, value -> PaprikaClient.setArmorAlpha((float) value)),
                        Control.toggle("armor.glow", "Armor Glow", PaprikaClient::isArmorVisualGlowEnabled, PaprikaClient::setArmorVisualGlowEnabled),
                        Control.cycle("armor.color_mode", "Armor Color Mode", Control.COLOR_MODES,
                                () -> PaprikaClient.getArmorVisualColorMode().ordinal(),
                                idx -> PaprikaClient.setArmorVisualColorMode(PaprikaClient.VisualColorMode.values()[idx])
                        ),
                        Control.slider("armor.saturation", "Armor Saturation", 1.0, 2.5, 0.1, PaprikaClient::getArmorVisualSaturationBoost, value -> PaprikaClient.setArmorVisualSaturationBoost((float) value)),
                        Control.slider("armor.anim_speed", "Armor Animation Speed", 0.2, 4.0, 0.1, PaprikaClient::getArmorVisualAnimationSpeed, value -> PaprikaClient.setArmorVisualAnimationSpeed((float) value))
                )));
                panels.add(new Panel("Held Item", 1, List.of(
                        Control.toggle("item.enabled", "Held Item Enabled", PaprikaClient::isHeldItemOverlayEnabled, PaprikaClient::setHeldItemOverlayEnabled),
                        Control.cycle("item.position", "Item Position", Control.ANCHOR_MODES,
                                () -> PaprikaClient.getHeldItemAnchorMode() == PaprikaClient.OverlayAnchorMode.ABOVE_PLAYER ? 0 : 1,
                                idx -> PaprikaClient.setHeldItemAnchorMode(idx == 0 ? PaprikaClient.OverlayAnchorMode.ABOVE_PLAYER : PaprikaClient.OverlayAnchorMode.RAY_MIDDLE)
                        ),
                        Control.slider("item.size", "Item Size", 0.35, 2.5, 0.1, PaprikaClient::getHeldItemOverlayScale, value -> PaprikaClient.setHeldItemOverlayScale((float) value)),
                        Control.slider("item.alpha", "Item Alpha", 0.1, 1.0, 0.05, PaprikaClient::getHeldItemAlpha, value -> PaprikaClient.setHeldItemAlpha((float) value)),
                        Control.toggle("item.glow", "Item Glow", PaprikaClient::isHeldItemVisualGlowEnabled, PaprikaClient::setHeldItemVisualGlowEnabled),
                        Control.cycle("item.color_mode", "Item Color Mode", Control.COLOR_MODES,
                                () -> PaprikaClient.getHeldItemVisualColorMode().ordinal(),
                                idx -> PaprikaClient.setHeldItemVisualColorMode(PaprikaClient.VisualColorMode.values()[idx])
                        ),
                        Control.slider("item.saturation", "Item Saturation", 1.0, 2.5, 0.1, PaprikaClient::getHeldItemVisualSaturationBoost, value -> PaprikaClient.setHeldItemVisualSaturationBoost((float) value)),
                        Control.slider("item.anim_speed", "Item Animation Speed", 0.2, 4.0, 0.1, PaprikaClient::getHeldItemVisualAnimationSpeed, value -> PaprikaClient.setHeldItemVisualAnimationSpeed((float) value))
                )));
                panels.add(new Panel("Distance", 1, List.of(
                        Control.toggle("distance.enabled", "Distance Enabled", PaprikaClient::isDistanceDisplayEnabled, PaprikaClient::setDistanceDisplayEnabled),
                        Control.cycle("distance.position", "Distance Position", Control.ANCHOR_MODES,
                                () -> PaprikaClient.getDistanceAnchorMode() == PaprikaClient.OverlayAnchorMode.ABOVE_PLAYER ? 0 : 1,
                                idx -> PaprikaClient.setDistanceAnchorMode(idx == 0 ? PaprikaClient.OverlayAnchorMode.ABOVE_PLAYER : PaprikaClient.OverlayAnchorMode.RAY_MIDDLE)
                        ),
                        Control.slider("distance.text_size", "Distance Text Size", 0.5, 2.0, 0.1, PaprikaClient::getDistanceTextScale, value -> PaprikaClient.setDistanceTextScale((float) value)),
                        Control.slider("distance.alpha", "Distance Alpha", 0.1, 1.0, 0.05, PaprikaClient::getDistanceAlpha, value -> PaprikaClient.setDistanceAlpha((float) value)),
                        Control.toggle("distance.glow", "Distance Glow", PaprikaClient::isDistanceVisualGlowEnabled, PaprikaClient::setDistanceVisualGlowEnabled),
                        Control.cycle("distance.color_mode", "Distance Color Mode", Control.COLOR_MODES,
                                () -> PaprikaClient.getDistanceVisualColorMode().ordinal(),
                                idx -> PaprikaClient.setDistanceVisualColorMode(PaprikaClient.VisualColorMode.values()[idx])
                        ),
                        Control.slider("distance.saturation", "Distance Saturation", 1.0, 2.5, 0.1, PaprikaClient::getDistanceVisualSaturationBoost, value -> PaprikaClient.setDistanceVisualSaturationBoost((float) value)),
                        Control.slider("distance.anim_speed", "Distance Animation Speed", 0.2, 4.0, 0.1, PaprikaClient::getDistanceVisualAnimationSpeed, value -> PaprikaClient.setDistanceVisualAnimationSpeed((float) value))
                )));
            }
            case TRAILS -> {
                panels.add(new Panel("Trails", 0, List.of(
                        Control.toggle("trails.enabled", "Enabled", PaprikaClient::isPlayerTrailsEnabled, PaprikaClient::setPlayerTrailsEnabled),
                        Control.keybind("trails.bind", "Bind", PaprikaClient.getPlayerTrailsKeyBinding()),
                        Control.toggle("trails.self", "Self Trails", PaprikaClient::isTrailSelfEnabled, PaprikaClient::setTrailSelfEnabled),
                        Control.toggle("trails.others", "Other Trails", PaprikaClient::isTrailOthersEnabled, PaprikaClient::setTrailOthersEnabled),
                        Control.cycle("trails.type", "Trail Type", Control.TRAIL_TYPES,
                                () -> PaprikaClient.getTrailType().ordinal(),
                                idx -> PaprikaClient.setTrailType(PaprikaClient.TrailType.values()[idx])
                        ),
                        Control.cycle("trails.origin", "Trail Origin", Control.TRAIL_ORIGINS,
                                () -> PaprikaClient.getTrailOrigin().ordinal(),
                                idx -> PaprikaClient.setTrailOrigin(PaprikaClient.TrailOrigin.values()[idx])
                        ),
                        Control.slider("trails.lifetime", "Lifetime (s)", 0.1, 10.0, 0.1, PaprikaClient::getTrailLifetimeSeconds, value -> PaprikaClient.setTrailLifetimeSeconds((float) value)),
                        Control.slider("trails.gradient_speed", "Gradient Speed", 0.1, 5.0, 0.1, PaprikaClient::getTrailGradientSpeed, value -> PaprikaClient.setTrailGradientSpeed((float) value)),
                        Control.slider("trails.strip_height", "Strip Height", 0.2, 4.0, 0.1, PaprikaClient::getTrailStripeHeight, value -> PaprikaClient.setTrailStripeHeight((float) value))
                )));
                panels.add(new Panel("Trail Color", 0, List.of(
                        Control.cycle("trails.color_mode", "Color Mode", Control.TRAIL_COLOR_MODES,
                                () -> PaprikaClient.getTrailColorMode().ordinal(),
                                idx -> PaprikaClient.setTrailColorMode(PaprikaClient.TrailColorMode.values()[idx])
                        ),
                        Control.slider("trails.alpha", "Trail Alpha", 0.1, 1.0, 0.05, PaprikaClient::getTrailAlpha, value -> PaprikaClient.setTrailAlpha((float) value)),
                        Control.slider("trails.fixed_r", "Fixed Red", 0, 255, 1, PaprikaClient::getTrailFixedRed, value -> PaprikaClient.setTrailFixedRed((int) value)),
                        Control.slider("trails.fixed_g", "Fixed Green", 0, 255, 1, PaprikaClient::getTrailFixedGreen, value -> PaprikaClient.setTrailFixedGreen((int) value)),
                        Control.slider("trails.fixed_b", "Fixed Blue", 0, 255, 1, PaprikaClient::getTrailFixedBlue, value -> PaprikaClient.setTrailFixedBlue((int) value))
                )));
            }
            case VIEW -> {
                panels.add(new Panel("Sky", 0, List.of(
                        Control.toggle("sky.enabled", "Custom Sky", PaprikaClient::isCustomSkyEnabled, PaprikaClient::setCustomSkyEnabled),
                        Control.toggle("sky.top_rainbow", "Top Rainbow", PaprikaClient::isSkyTopRainbowEnabled, PaprikaClient::setSkyTopRainbowEnabled),
                        Control.toggle("sky.bottom_rainbow", "Bottom Rainbow", PaprikaClient::isSkyBottomRainbowEnabled, PaprikaClient::setSkyBottomRainbowEnabled),
                        Control.slider("sky.top_r", "Top Red", 0.0, 255.0, 1.0, PaprikaClient::getSkyTopRed, value -> PaprikaClient.setSkyTopRed((int) Math.round(value))),
                        Control.slider("sky.top_g", "Top Green", 0.0, 255.0, 1.0, PaprikaClient::getSkyTopGreen, value -> PaprikaClient.setSkyTopGreen((int) Math.round(value))),
                        Control.slider("sky.top_b", "Top Blue", 0.0, 255.0, 1.0, PaprikaClient::getSkyTopBlue, value -> PaprikaClient.setSkyTopBlue((int) Math.round(value))),
                        Control.slider("sky.bottom_r", "Bottom Red", 0.0, 255.0, 1.0, PaprikaClient::getSkyBottomRed, value -> PaprikaClient.setSkyBottomRed((int) Math.round(value))),
                        Control.slider("sky.bottom_g", "Bottom Green", 0.0, 255.0, 1.0, PaprikaClient::getSkyBottomGreen, value -> PaprikaClient.setSkyBottomGreen((int) Math.round(value))),
                        Control.slider("sky.bottom_b", "Bottom Blue", 0.0, 255.0, 1.0, PaprikaClient::getSkyBottomBlue, value -> PaprikaClient.setSkyBottomBlue((int) Math.round(value)))
                )));
                panels.add(new Panel("Hands", 1, List.of(
                        Control.toggle("hands.hide", "Hide Hands With Item", PaprikaClient::isHideHandsWithItemEnabled, PaprikaClient::setHideHandsWithItemEnabled),
                        Control.slider("hands.fov", "Hand FOV", -1.6, 1.6, 0.01, PaprikaClient::getHandFovScale, value -> PaprikaClient.setHandFovScale((float) value)),
                        Control.slider("hands.offset_x", "Hand Offset X", -1.5, 1.5, 0.01, PaprikaClient::getHandOffsetX, value -> PaprikaClient.setHandOffsetX((float) value)),
                        Control.slider("hands.offset_y", "Hand Offset Y", -1.5, 1.5, 0.01, PaprikaClient::getHandOffsetY, value -> PaprikaClient.setHandOffsetY((float) value)),
                        Control.toggle("hands.flip_item", "Flip Item", PaprikaClient::isHandItemFlipEnabled, PaprikaClient::setHandItemFlipEnabled),
                        Control.cycle("hands.orientation", "Item Orientation", new String[]{"Default", "Left", "Right"},
                                () -> PaprikaClient.getHandItemOrientation().ordinal(),
                                idx -> PaprikaClient.setHandItemOrientation(PaprikaClient.HandItemOrientation.values()[idx])
                        )
                )));
            }
            case TARGET_HEALTH -> panels.add(new Panel("Target Health", 0, List.of(
                    Control.toggle("target_health.enabled", "Enabled", PaprikaClient::isTargetHealthOverlayEnabled, PaprikaClient::setTargetHealthOverlayEnabled),
                    Control.toggle("target_health.dynamic_color", "Dynamic Color", PaprikaClient::isTargetHealthDynamicColorEnabled, PaprikaClient::setTargetHealthDynamicColorEnabled),
                    Control.slider("target_health.text_size", "Text Size", 0.5, 2.0, 0.1, PaprikaClient::getTargetHealthTextScale, value -> PaprikaClient.setTargetHealthTextScale((float) value))
            )));
            case PLAYER_LIST -> panels.add(new Panel("Player List", 0, List.of(
                    Control.toggle("player_list.enabled", "Enabled", PaprikaClient::isPlayerListEnabled, PaprikaClient::setPlayerListEnabled),
                    Control.keybind("player_list.bind", "Bind", PaprikaClient.getPlayerListKeyBinding()),
                    Control.slider("player_list.offset_x", "Offset X", 0.0, 4096.0, 1.0, () -> PaprikaClient.getPlayerListOffsetX(), value -> PaprikaClient.setPlayerListOffsetX((int) Math.round(value))),
                    Control.slider("player_list.offset_y", "Offset Y", 0.0, 4096.0, 1.0, () -> PaprikaClient.getPlayerListOffsetY(), value -> PaprikaClient.setPlayerListOffsetY((int) Math.round(value))),
                    Control.slider("player_list.scale", "Scale", 0.1, 2.0, 0.1, PaprikaClient::getPlayerListTextScale, value -> PaprikaClient.setPlayerListTextScale((float) value)),
                    Control.slider("player_list.max_height", "Max Height", 40.0, 4096.0, 10.0, PaprikaClient::getPlayerListMaxHeight, value -> PaprikaClient.setPlayerListMaxHeight((int) Math.round(value))),
                    Control.slider("player_list.alpha", "Alpha", 0.1, 1.0, 0.1, PaprikaClient::getPlayerListAlphaMultiplier, value -> PaprikaClient.setPlayerListAlphaMultiplier((float) value))
            )));
            case MENU -> panels.add(new Panel("Menu", 0, List.of(
                    Control.label("menu.enabled", "Enabled", () -> "Always ON"),
                    Control.keybind("menu.bind", "Menu Key", PaprikaClient.getOpenMenuKeyBinding()),
                    Control.keybind("menu.panic", "Panic Key", PaprikaClient.getPanicKeyBinding())
            )));
        }

        return panels;
    }

    private enum ModuleGroup {
        MOVEMENT("Movement"),
        COMBAT("Combat"),
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
        AUTO_ATTACK("Auto Attack", ModuleGroup.COMBAT),
        FRIENDS("Friends", ModuleGroup.COMBAT),
        ESP("Player ESP", ModuleGroup.VISUAL),
        RAYS("Rays", ModuleGroup.VISUAL),
        TRAILS("Trails", ModuleGroup.VISUAL),
        VIEW("View", ModuleGroup.VISUAL),
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
        SLIDER,
        BUTTON,
        TEXT
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

        private boolean intersects(Rect other) {
            return this.x < other.x + other.width
                    && this.x + this.width > other.x
                    && this.y < other.y + other.height
                    && this.y + this.height > other.y;
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
        private static final String[] COLOR_MODES = new String[]{"Nick", "Gradient", "Nick Gradient", "Rainbow"};
        private static final String[] ANCHOR_MODES = new String[]{"Above", "Ray Middle"};
        private static final String[] TRAIL_TYPES = new String[]{"Thin Line", "Floating Line", "Strip"};
        private static final String[] TRAIL_ORIGINS = new String[]{"Back", "Head"};
        private static final String[] TRAIL_COLOR_MODES = new String[]{"Nick", "Fixed", "Gradient", "Nick Gradient"};
        private static final String[] AUTO_ATTACK_MODES = new String[]{"Circle", "Circle + Mark", "Marked Only", "All Nearby"};
        private static final String[] CIRCLE_COLOR_MODES = new String[]{"Fixed", "Gradient", "Rainbow"};

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
        private final Consumer<String> textSetter;
        private final Runnable buttonAction;
        private final int textMaxLength;

        private Control(String id, ControlType type, String label, BooleanSupplier toggleGetter, Consumer<Boolean> toggleSetter,
                        DoubleSupplier sliderGetter, DoubleConsumer sliderSetter, double min, double max, double step,
                        IntSupplier cycleGetter, IntConsumer cycleSetter, String[] cycleLabels,
                        KeyBinding keyBinding, Supplier<String> labelValue, Consumer<String> textSetter,
                        Runnable buttonAction, int textMaxLength) {
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
            this.textSetter = textSetter;
            this.buttonAction = buttonAction;
            this.textMaxLength = textMaxLength;
        }

        private static Control toggle(String id, String label, BooleanSupplier getter, Consumer<Boolean> setter) {
            return new Control(id, ControlType.TOGGLE, label, getter, setter, null, null, 0.0, 0.0, 0.0, null, null, null, null, () -> "", null, null, 0);
        }

        private static Control keybind(String id, String label, KeyBinding keyBinding) {
            return new Control(id, ControlType.KEYBIND, label, () -> false, value -> {
            }, null, null, 0.0, 0.0, 0.0, null, null, null, keyBinding, () -> "", null, null, 0);
        }

        private static Control slider(String id, String label, double min, double max, double step, DoubleSupplier getter, DoubleConsumer setter) {
            return new Control(id, ControlType.SLIDER, label, () -> false, value -> {
            }, getter, setter, min, max, step, null, null, null, null, () -> "", null, null, 0);
        }

        private static Control cycle(String id, String label, String[] values, IntSupplier getter, IntConsumer setter) {
            return new Control(id, ControlType.CYCLE, label, () -> false, value -> {
            }, null, null, 0.0, 0.0, 0.0, getter, setter, values, null, () -> "", null, null, 0);
        }

        private static Control label(String id, String label, Supplier<String> valueSupplier) {
            return new Control(id, ControlType.LABEL, label, () -> false, value -> {
            }, null, null, 0.0, 0.0, 0.0, null, null, null, null, valueSupplier, null, null, 0);
        }

        private static Control textInput(String id, String label, Supplier<String> valueSupplier, Consumer<String> setter, int maxLength) {
            return new Control(id, ControlType.TEXT, label, () -> false, value -> {
            }, null, null, 0.0, 0.0, 0.0, null, null, null, null, valueSupplier, setter, null, maxLength);
        }

        private static Control button(String id, String label, Supplier<String> buttonLabel, Runnable action) {
            return new Control(id, ControlType.BUTTON, label, () -> false, value -> {
            }, null, null, 0.0, 0.0, 0.0, null, null, null, null, buttonLabel, null, action, 0);
        }
    }

    private enum ControlType {
        TOGGLE,
        KEYBIND,
        CYCLE,
        SLIDER,
        LABEL,
        BUTTON,
        TEXT
    }
}
