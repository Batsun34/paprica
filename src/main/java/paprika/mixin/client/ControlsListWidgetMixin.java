package paprika.mixin.client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.option.ControlsListWidget;
import net.minecraft.client.gui.screen.option.KeybindsScreen;
import net.minecraft.client.option.KeyBinding;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import paprika.PaprikaClient;

import java.util.ArrayList;
import java.util.List;

@Mixin(ControlsListWidget.class)
public class ControlsListWidgetMixin {
    @Inject(method = "<init>", at = @At("TAIL"))
    private void paprika$hidePaprikaKeybinds(KeybindsScreen parent, MinecraftClient client, CallbackInfo ci) {
        if (!PaprikaClient.isPanicActive()) return;

        ControlsListWidget self = (ControlsListWidget) (Object) this;
        List<ControlsListWidget.Entry> existing = new ArrayList<>(self.children());
        List<ControlsListWidget.Entry> filtered = new ArrayList<>(existing.size());

        ControlsListWidget.CategoryEntry pendingCategory = null;
        boolean categoryHasEntries = false;

        for (ControlsListWidget.Entry entry : existing) {
            if (entry instanceof ControlsListWidget.CategoryEntry category) {
                pendingCategory = category;
                categoryHasEntries = false;
                continue;
            }

            boolean skip = false;
            if (entry instanceof ControlsListWidget.KeyBindingEntry keyEntry) {
                KeyBinding binding = ((ControlsListWidgetKeyBindingEntryAccessor) keyEntry).paprika$getBinding();
                if (binding != null && binding.getTranslationKey().startsWith("key.paprika.")) {
                    skip = true;
                }
            }

            if (skip) {
                continue;
            }

            if (!categoryHasEntries && pendingCategory != null) {
                filtered.add(pendingCategory);
                categoryHasEntries = true;
            }
            filtered.add(entry);
        }

        self.children().clear();
        self.children().addAll(filtered);
    }
}
