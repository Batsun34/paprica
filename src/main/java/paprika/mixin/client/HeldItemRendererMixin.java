package paprika.mixin.client;

import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.item.HeldItemRenderer;
import net.minecraft.item.ModelTransformationMode;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Arm;
import net.minecraft.util.Hand;
import net.minecraft.util.math.RotationAxis;
import paprika.PaprikaClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(HeldItemRenderer.class)
public class HeldItemRendererMixin {
    @Unique
    private ItemStack paprika$currentItem = ItemStack.EMPTY;

    @Unique
    private boolean paprika$offsetPushed;

    @Unique
    private boolean paprika$itemRotationPushed;

    @Inject(method = "renderFirstPersonItem", at = @At("HEAD"))
    private void paprika$beginRender(
            AbstractClientPlayerEntity player,
            float tickDelta,
            float pitch,
            Hand hand,
            float swingProgress,
            ItemStack item,
            float equipProgress,
            MatrixStack matrices,
            VertexConsumerProvider vertexConsumers,
            int light,
            CallbackInfo ci
    ) {
        this.paprika$currentItem = item == null ? ItemStack.EMPTY : item;
        this.paprika$offsetPushed = false;

        float scale = PaprikaClient.getHandFovScale();
        float offsetX = PaprikaClient.getHandOffsetX();
        float offsetY = PaprikaClient.getHandOffsetY();
        boolean flipItem = PaprikaClient.isHandItemFlipEnabled();
        PaprikaClient.HandItemOrientation orientation = PaprikaClient.getHandItemOrientation();
        boolean hasItem = !this.paprika$currentItem.isEmpty();

        boolean needsTransform = Math.abs(scale - 1.0F) > 0.0001F
                || Math.abs(offsetX) > 0.0001F
                || Math.abs(offsetY) > 0.0001F
                || (hasItem && (flipItem || orientation != PaprikaClient.HandItemOrientation.DEFAULT));

        if (needsTransform) {
            matrices.push();
            this.paprika$offsetPushed = true;
            Arm arm = hand == Hand.MAIN_HAND ? player.getMainArm() : player.getMainArm().getOpposite();
            float signedOffsetX = arm == Arm.LEFT ? -offsetX : offsetX;
            matrices.translate(signedOffsetX, offsetY, 0.0F);
            matrices.scale(scale, scale, 1.0F);
        }
    }

    @Inject(method = "renderFirstPersonItem", at = @At("RETURN"))
    private void paprika$endRender(
            AbstractClientPlayerEntity player,
            float tickDelta,
            float pitch,
            Hand hand,
            float swingProgress,
            ItemStack item,
            float equipProgress,
            MatrixStack matrices,
            VertexConsumerProvider vertexConsumers,
            int light,
            CallbackInfo ci
    ) {
        if (this.paprika$offsetPushed) {
            matrices.pop();
        }
        this.paprika$currentItem = ItemStack.EMPTY;
    }

    @Inject(method = "renderItem", at = @At("HEAD"))
    private void paprika$applyItemRotation(
            LivingEntity entity,
            ItemStack stack,
            ModelTransformationMode mode,
            boolean leftHanded,
            MatrixStack matrices,
            VertexConsumerProvider vertexConsumers,
            int light,
            CallbackInfo ci
    ) {
        this.paprika$itemRotationPushed = false;
        if (stack == null || stack.isEmpty()) return;

        boolean flipItem = PaprikaClient.isHandItemFlipEnabled();
        PaprikaClient.HandItemOrientation orientation = PaprikaClient.getHandItemOrientation();
        if (!flipItem && orientation == PaprikaClient.HandItemOrientation.DEFAULT) return;

        matrices.push();
        this.paprika$itemRotationPushed = true;

        float zRot = switch (orientation) {
            case LEFT -> 25.0F;
            case RIGHT -> -25.0F;
            default -> 0.0F;
        };
        if (leftHanded) {
            zRot = -zRot;
        }
        if (Math.abs(zRot) > 0.0001F) {
            matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(zRot));
        }
        if (flipItem) {
            matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(180.0F));
        }
    }

    @Inject(method = "renderItem", at = @At("RETURN"))
    private void paprika$popItemRotation(
            LivingEntity entity,
            ItemStack stack,
            ModelTransformationMode mode,
            boolean leftHanded,
            MatrixStack matrices,
            VertexConsumerProvider vertexConsumers,
            int light,
            CallbackInfo ci
    ) {
        if (this.paprika$itemRotationPushed) {
            matrices.pop();
        }
    }

    @Inject(method = "renderArmHoldingItem", at = @At("HEAD"), cancellable = true)
    private void paprika$hideArmHoldingItem(
            MatrixStack matrices,
            VertexConsumerProvider vertexConsumers,
            int light,
            float equipProgress,
            float swingProgress,
            Arm arm,
            CallbackInfo ci
    ) {
        if (PaprikaClient.isHideHandsWithItemEnabled() && !this.paprika$currentItem.isEmpty()) {
            ci.cancel();
        }
    }

    @Inject(method = "renderArm", at = @At("HEAD"), cancellable = true)
    private void paprika$hideArm(
            MatrixStack matrices,
            VertexConsumerProvider vertexConsumers,
            int light,
            Arm arm,
            CallbackInfo ci
    ) {
        if (PaprikaClient.isHideHandsWithItemEnabled() && !this.paprika$currentItem.isEmpty()) {
            ci.cancel();
        }
    }
}
