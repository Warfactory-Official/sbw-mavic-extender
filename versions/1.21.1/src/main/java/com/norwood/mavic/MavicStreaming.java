package com.norwood.mavic;

import com.atsuishio.superbwarfare.entity.vehicle.DroneEntity;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.ChunkPos;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class MavicStreaming {

    public static final int STREAM_RADIUS = 12;
    public static final String MONITOR_ID = "superbwarfare:monitor";
    public static final ResourceLocation MONITOR_RL =
            ResourceLocation.fromNamespaceAndPath("superbwarfare", "monitor");

    private static volatile Map<UUID, LongOpenHashSet> snapshot = Collections.emptyMap();

    public static long forceRangeHits = 0L;
    public static long entityRevealHits = 0L;

    private MavicStreaming() {
    }

    public static void publish(Map<UUID, LongOpenHashSet> next) {
        snapshot = next;
    }

    public static boolean isChunkStreamedTo(UUID playerId, int chunkX, int chunkZ) {
        if (playerId == null) {
            return false;
        }
        LongOpenHashSet set = snapshot.get(playerId);
        return set != null && set.contains(ChunkPos.asLong(chunkX, chunkZ));
    }

    public static boolean shouldReveal(Entity entity, Player player) {
        if (entity == null || player == null) {
            return false;
        }
        if (isOperatorDrone(entity, player)) {
            forceRangeHits++;
            return true;
        }
        Map<UUID, LongOpenHashSet> snap = snapshot;
        if (snap.isEmpty()) {
            return false;
        }
        LongOpenHashSet set = snap.get(player.getUUID());
        if (set != null) {
            ChunkPos cp = entity.chunkPosition();
            if (set.contains(ChunkPos.asLong(cp.x, cp.z))) {
                forceRangeHits++;
                entityRevealHits++;
                return true;
            }
        }
        return false;
    }

    public static boolean isDrone(Entity entity) {
        return entity instanceof DroneEntity;
    }

    public static boolean isLinked(DroneEntity drone) {
        return drone.getEntityData().get(DroneEntity.LINKED);
    }

    public static String controllerUuid(DroneEntity drone) {
        return drone.getEntityData().get(DroneEntity.CONTROLLER);
    }

    public static boolean isOperatorDrone(Entity entity, Player player) {
        if (!(entity instanceof DroneEntity drone) || !isLinked(drone)) {
            return false;
        }
        return player.getStringUUID().equals(controllerUuid(drone));
    }

    public static boolean isViewingDrone(Player operator, DroneEntity drone) {
        return viewingReason(operator, drone).isEmpty();
    }

    public static String viewingReason(Player operator, DroneEntity drone) {
        String ctrl = controllerUuid(drone);
        if (!operator.getStringUUID().equals(ctrl)) {
            return "not-controller(ctrl=" + ctrl + ")";
        }
        if (!isLinked(drone)) {
            return "drone-not-linked";
        }
        ItemStack stack = operator.getMainHandItem();
        ResourceLocation id = BuiltInRegistries.ITEM.getKey(stack.getItem());
        if (id == null || !MONITOR_RL.equals(id)) {
            return "mainhand-not-monitor(" + id + ")";
        }
        CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
        if (customData == null) {
            return "monitor-no-nbt";
        }
        CompoundTag tag = customData.copyTag();
        if (!tag.getBoolean("Using")) {
            return "monitor-not-using";
        }
        String linked = tag.getString("LinkedDrone");
        if (!drone.getStringUUID().equals(linked)) {
            return "linkeddrone-mismatch(" + linked + ")";
        }
        return "";
    }
}
