package com.norwood.mavic;

import com.atsuishio.superbwarfare.entity.vehicle.DroneEntity;
import com.atsuishio.superbwarfare.tools.EntityFindUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

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
        ResourceLocation id = BuiltInRegistries.ITEM.getKey(stack.getItem());
        if (id == null || !MavicStreaming.MONITOR_RL.equals(id)) {
            return null;
        }
        CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
        if (customData == null) {
            return null;
        }
        CompoundTag tag = customData.getUnsafe();
        if (!tag.getBoolean("Using") || !tag.getBoolean("Linked")) {
            return null;
        }
        DroneEntity drone = EntityFindUtil.findDrone(player.level(), tag.getString("LinkedDrone"));
        return drone;
    }
}
