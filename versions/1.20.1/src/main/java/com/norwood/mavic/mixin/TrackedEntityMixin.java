package com.norwood.mavic.mixin;

import com.norwood.mavic.MavicStreaming;
import com.norwood.mavic.MavicTracked;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(targets = "net.minecraft.server.level.ChunkMap$TrackedEntity")
public abstract class TrackedEntityMixin implements MavicTracked {

    @Shadow
    @Final
    Entity entity;

    @Shadow
    public abstract void updatePlayer(ServerPlayer player);

    @ModifyVariable(
            method = "updatePlayer(Lnet/minecraft/server/level/ServerPlayer;)V",
            at = @At("STORE"), ordinal = 0)
    private boolean mavic$forceVisible(boolean flag, ServerPlayer player) {
        return flag || MavicStreaming.shouldReveal(this.entity, player);
    }

    @Override
    public void mavic$updatePlayer(ServerPlayer player) {
        updatePlayer(player);
    }
}
