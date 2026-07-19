package com.norwood.mavic;

import com.atsuishio.superbwarfare.entity.vehicle.DroneEntity;
import com.atsuishio.superbwarfare.tools.EntityFindUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;

public final class MavicClient {

    private MavicClient() {
    }

    public static Entity getViewedDrone() {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null) {
            return null;
        }
        ItemStack stack = player.getMainHandItem();
        ResourceLocation id = ForgeRegistries.ITEMS.getKey(stack.getItem());
        if (!MavicStreaming.MONITOR_RL.equals(id)) {
            return null;
        }
        CompoundTag tag = stack.getTag();
        if (tag == null || !tag.getBoolean("Using") || !tag.getBoolean("Linked")) {
            return null;
        }
        DroneEntity drone = EntityFindUtil.findDrone(player.level(), tag.getString("LinkedDrone"));
        return drone;
    }
}
