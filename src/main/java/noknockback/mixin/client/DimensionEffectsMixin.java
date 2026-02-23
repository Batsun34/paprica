package noknockback.mixin.client;

import net.minecraft.client.render.DimensionEffects;
import noknockback.NoKnockbackClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(DimensionEffects.class)
public class DimensionEffectsMixin {
    @Inject(method = "getSkyColor", at = @At("HEAD"), cancellable = true)
    private void paprika$overrideSkyColor(float skyAngle, CallbackInfoReturnable<Integer> cir) {
        if (NoKnockbackClient.isCustomSkyEnabled()) {
            cir.setReturnValue(NoKnockbackClient.getSkyTopColor());
        }
    }

}
