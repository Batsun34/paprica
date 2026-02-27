package paprika.mixin.client;

import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.network.packet.s2c.play.EntityDamageS2CPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import paprika.PaprikaClient;

@Mixin(ClientPlayNetworkHandler.class)
public class ClientPlayNetworkHandlerMixin {
    @Inject(method = "onEntityDamage", at = @At("HEAD"))
    private void paprika$onEntityDamage(EntityDamageS2CPacket packet, CallbackInfo ci) {
        if (packet == null) return;
        PaprikaClient.handleEntityDamage(packet.sourceCauseId(), packet.sourceDirectId());
    }
}
