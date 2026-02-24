package paprika.mixin.client;

import net.minecraft.client.gui.screen.option.ControlsListWidget;
import net.minecraft.client.option.KeyBinding;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ControlsListWidget.KeyBindingEntry.class)
public interface ControlsListWidgetKeyBindingEntryAccessor {
    @Accessor("binding")
    KeyBinding paprika$getBinding();
}
