package paprika.mixin.client;

import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.network.packet.s2c.play.PlaySoundFromEntityS2CPacket;
import net.minecraft.network.packet.s2c.play.PlaySoundS2CPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import paprika.PaprikaClient;

@Mixin(ClientPlayNetworkHandler.class)
public class ClientPlayNetworkHandlerMixin {
    @Inject(method = "onPlaySound", at = @At("HEAD"))
    private void paprika$onPlaySound(PlaySoundS2CPacket packet, CallbackInfo ci) {
        if (packet == null) return;
        PaprikaClient.handleAttackSound(packet.getSound(), packet.getX(), packet.getY(), packet.getZ());
    }

    @Inject(method = "onPlaySoundFromEntity", at = @At("HEAD"))
    private void paprika$onPlaySoundFromEntity(PlaySoundFromEntityS2CPacket packet, CallbackInfo ci) {
        if (packet == null) return;
        PaprikaClient.handleAttackSoundFromEntity(packet.getSound(), packet.getEntityId());
    }
}
