package com.norwood.mavic.mixin;

import com.norwood.mavic.MavicClient;
import com.norwood.mavic.MavicConfig;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(targets = "com.atsuishio.superbwarfare.client.overlay.DroneHudOverlay", remap = false)
public class DroneHudOverlayMixin {

    @Inject(method = "getMaxDistance", at = @At("HEAD"), cancellable = true, remap = false)
    private void mavic$maxDistance(CallbackInfoReturnable<Integer> cir) {
        Entity drone = MavicClient.getViewedDrone();
        if (drone != null) {
            cir.setReturnValue(MavicConfig.maxDistanceFor(drone));
        }
    }
}
