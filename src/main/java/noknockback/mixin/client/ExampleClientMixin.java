package noknockback.mixin.client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import noknockback.NoKnockbackClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(MinecraftClient.class)
public class ExampleClientMixin {
	@Inject(method = "hasOutline", at = @At("HEAD"), cancellable = true)
	private void noknockback$forcePlayerOutline(Entity entity, CallbackInfoReturnable<Boolean> cir) {
		if (NoKnockbackClient.isPlayerEspEnabled() && entity instanceof PlayerEntity) {
			cir.setReturnValue(true);
		}
	}
}
