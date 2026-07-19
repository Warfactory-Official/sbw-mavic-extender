package com.norwood.mavic.mixin;

import com.norwood.mavic.MavicClient;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LevelRenderer.class)
public class LevelRendererMixin {

    @Unique
    private net.minecraft.world.entity.Entity mavic$cachedDrone;

    @Inject(
            method = "setupRender(Lnet/minecraft/client/Camera;Lnet/minecraft/client/renderer/culling/Frustum;ZZ)V",
            at = @At("HEAD"))
    private void mavic$resolveDrone(net.minecraft.client.Camera camera, net.minecraft.client.renderer.culling.Frustum frustum, boolean b1, boolean b2, org.spongepowered.asm.mixin.injection.callback.CallbackInfo ci) {
        this.mavic$cachedDrone = MavicClient.getViewedDrone();
    }

    @Redirect(
            method = "setupRender(Lnet/minecraft/client/Camera;Lnet/minecraft/client/renderer/culling/Frustum;ZZ)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;getX()D"))
    private double mavic$renderOriginX(LocalPlayer player) {
        Entity drone = this.mavic$cachedDrone;
        return drone != null ? drone.getX() : player.getX();
    }

    @Redirect(
            method = "setupRender(Lnet/minecraft/client/Camera;Lnet/minecraft/client/renderer/culling/Frustum;ZZ)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;getY()D"))
    private double mavic$renderOriginY(LocalPlayer player) {
        Entity drone = this.mavic$cachedDrone;
        return drone != null ? drone.getY() : player.getY();
    }

    @Redirect(
            method = "setupRender(Lnet/minecraft/client/Camera;Lnet/minecraft/client/renderer/culling/Frustum;ZZ)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;getZ()D"))
    private double mavic$renderOriginZ(LocalPlayer player) {
        Entity drone = this.mavic$cachedDrone;
        return drone != null ? drone.getZ() : player.getZ();
    }
}
