package paprika.mixin.client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import paprika.PaprikaClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(MinecraftClient.class)
public class ExampleClientMixin {
	@Inject(method = "hasOutline", at = @At("HEAD"), cancellable = true)
	private void paprika$forcePlayerOutline(Entity entity, CallbackInfoReturnable<Boolean> cir) {
		if (PaprikaClient.isPlayerEspEnabled() && entity instanceof PlayerEntity) {
			cir.setReturnValue(true);
		}
	}
}
