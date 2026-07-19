package com.norwood.mavic;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.neoforged.neoforge.common.ModConfigSpec;

public final class MavicConfig {

    public static final ModConfigSpec COMMON_SPEC;

    private static final ModConfigSpec.IntValue DEFAULT_MAX_DISTANCE;
    private static final ModConfigSpec.ConfigValue<String> PER_VEHICLE;
    private static final ModConfigSpec.BooleanValue VERBOSE_LOGGING;

    static {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();
        builder.push("drone");
        DEFAULT_MAX_DISTANCE = builder
                .comment("Max distance (blocks) before the drone HUD shows the out-of-range WARNING and red distance marker.")
                .defineInRange("defaultMaxDistance", 2048, 16, 1_000_000);
        PER_VEHICLE = builder
                .comment("Per-vehicle overrides, so addon drones can differ. Format: \"namespace:entity=distance\", separated by ';' or ','. Example: \"superbwarfare:drone=4096;mymod:big_drone=8192\".")
                .define("perVehicleMaxDistance", "");
        builder.pop();
        VERBOSE_LOGGING = builder
                .comment("Log verbose [Mavic] chunk-streaming diagnostics (per-drone status, transfers, stream counts). Off by default.")
                .define("verboseLogging", false);
        COMMON_SPEC = builder.build();
    }

    private MavicConfig() {
    }

    public static boolean verboseLogging() {
        try {
            return VERBOSE_LOGGING.get();
        } catch (Exception e) {
            return false;
        }
    }

    public static int maxDistanceFor(Entity drone) {
        try {
            ResourceLocation key = BuiltInRegistries.ENTITY_TYPE.getKey(drone.getType());
            if (key != null) {
                String id = key.toString();
                for (String entry : PER_VEHICLE.get().split("[;,]")) {
                    int eq = entry.indexOf('=');
                    if (eq <= 0) {
                        continue;
                    }
                    if (entry.substring(0, eq).trim().equals(id)) {
                        return Integer.parseInt(entry.substring(eq + 1).trim());
                    }
                }
            }
            return DEFAULT_MAX_DISTANCE.get();
        } catch (Exception e) {
            return 2048;
        }
    }
}
