package com.norwood.mavic;

import com.mojang.logging.LogUtils;
import net.minecraftforge.fml.ModList;
import org.slf4j.Logger;

import java.lang.reflect.Field;


public final class MavicAddonCompat {

    private static final Logger LOG = LogUtils.getLogger();

    private static final String DRONE_WARFARE_MODID = "sbwdroneconfig";
    private static final String ADDON_CONFIG_CLASS = "nl.smartstreamlabs.sbwdroneconfig.AddonConfig";

    private MavicAddonCompat() {
    }

    public static void applyOverrides() {
        if (!ModList.get().isLoaded(DRONE_WARFARE_MODID)) {
            return;
        }

        Class<?> addonConfig;
        try {
            addonConfig = Class.forName(ADDON_CONFIG_CLASS);
        } catch (ClassNotFoundException e) {
            LOG.warn("[Mavic] SBW Drone Warfare is loaded but {} was not found; skipping compat overrides "
                    + "(addon internals changed?).", ADDON_CONFIG_CLASS);
            return;
        }

        boolean anchor = forceOff(addonConfig, "ENABLE_DRONE_PLAYER_ANCHOR_VALUE", "enableDronePlayerAnchor");
        boolean chunks = forceOff(addonConfig, "ENABLE_DRONE_CHUNK_LOADING_VALUE", "enableDroneChunkLoading");

        if (anchor || chunks) {
            LOG.info("[Mavic] Detected SBW Drone Warfare; disabled its redundant drone-view workarounds "
                    + "(playerAnchor={}, chunkLoading={}). Mavic's detached-camera streaming supersedes them.",
                    anchor, chunks);
        } else {
            LOG.warn("[Mavic] SBW Drone Warfare detected but no overridable toggles were flipped; "
                    + "the addon's anchor/chunk-loading workarounds may still be active and conflict with Mavic.");
        }
    }


    private static boolean forceOff(Class<?> addonConfig, String specFieldName, String cachedFieldName) {
        boolean changed = false;

        try {
            Field specField = addonConfig.getDeclaredField(specFieldName);
            specField.setAccessible(true);
            Object configValue = specField.get(null);
            if (configValue != null) {
                configValue.getClass().getMethod("set", Object.class).invoke(configValue, Boolean.FALSE);
                changed = true;
            }
        } catch (Throwable t) {
            LOG.warn("[Mavic] Could not flip SBW Drone Warfare config spec {} (non-fatal): {}",
                    specFieldName, t.toString());
        }

        try {
            Field cachedField = addonConfig.getDeclaredField(cachedFieldName);
            cachedField.setAccessible(true);
            cachedField.setBoolean(null, false);
            changed = true;
        } catch (Throwable t) {
            LOG.warn("[Mavic] Could not flip SBW Drone Warfare cached field {} (non-fatal): {}",
                    cachedFieldName, t.toString());
        }

        return changed;
    }
}
