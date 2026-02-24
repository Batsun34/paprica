package paprika.mixin.client;

import net.minecraft.client.render.OutlineVertexConsumerProvider;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import paprika.PaprikaClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(WorldRenderer.class)
public class WorldRendererOutlineScaleMixin {
    @Unique
    private boolean paprika$scaledOutline;

    @Inject(method = "renderEntity", at = @At("HEAD"))
    private void paprika$scaleOutline(
            Entity entity,
            double cameraX,
            double cameraY,
            double cameraZ,
            float tickDelta,
            MatrixStack matrices,
            VertexConsumerProvider vertexConsumers,
            CallbackInfo ci
    ) {
        this.paprika$scaledOutline = false;
        if (!(vertexConsumers instanceof OutlineVertexConsumerProvider)) return;
        if (!(entity instanceof PlayerEntity)) return;
        if (!PaprikaClient.isPlayerEspEnabled()) return;

        float thickness = PaprikaClient.getOutlineThickness();
        if (thickness <= 1.01F) return;

        float scale = 1.0F + (thickness - 1.0F) * 0.06F;
        matrices.push();
        matrices.scale(scale, scale, scale);
        this.paprika$scaledOutline = true;
    }

    @Inject(method = "renderEntity", at = @At("RETURN"))
    private void paprika$unscaleOutline(
            Entity entity,
            double cameraX,
            double cameraY,
            double cameraZ,
            float tickDelta,
            MatrixStack matrices,
            VertexConsumerProvider vertexConsumers,
            CallbackInfo ci
    ) {
        if (this.paprika$scaledOutline) {
            matrices.pop();
        }
    }
}
