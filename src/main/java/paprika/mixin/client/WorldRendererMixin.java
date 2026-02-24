package paprika.mixin.client;

import net.minecraft.client.render.WorldRenderer;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import paprika.PaprikaClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(WorldRenderer.class)
public class WorldRendererMixin {
	@Redirect(
		method = "renderEntities",
		at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/Entity;getTeamColorValue()I")
	)
	private int paprika$useNickColorForOutline(Entity entity) {
		if (PaprikaClient.isPlayerEspEnabled() && entity instanceof PlayerEntity playerEntity) {
			return PaprikaClient.getPlayerHighlightColor(playerEntity);
		}

		return entity.getTeamColorValue();
	}
}
