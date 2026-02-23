package noknockback.mixin.client;

import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.math.Vec3d;
import noknockback.NoKnockbackClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ClientWorld.class)
public class ClientWorldMixin {
    @Inject(method = "getSkyColor", at = @At("HEAD"), cancellable = true)
    private void paprika$overrideSkyColor(Vec3d cameraPos, float tickDelta, CallbackInfoReturnable<Integer> cir) {
        if (NoKnockbackClient.isCustomSkyEnabled()) {
            cir.setReturnValue(NoKnockbackClient.getSkyTopColor());
        }
    }
}
