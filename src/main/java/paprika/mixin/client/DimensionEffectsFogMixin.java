package paprika.mixin.client;

import net.minecraft.client.render.DimensionEffects;
import net.minecraft.util.math.Vec3d;
import paprika.PaprikaClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin({
        DimensionEffects.Overworld.class,
        DimensionEffects.Nether.class,
        DimensionEffects.End.class
})
public class DimensionEffectsFogMixin {
    @Inject(method = "adjustFogColor", at = @At("HEAD"), cancellable = true)
    private void paprika$overrideFogColor(Vec3d color, float sunHeight, CallbackInfoReturnable<Vec3d> cir) {
        if (PaprikaClient.isCustomSkyEnabled()) {
            cir.setReturnValue(PaprikaClient.getSkyBottomColorVec());
        }
    }
}
