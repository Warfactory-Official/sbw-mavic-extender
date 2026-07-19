package com.norwood.mavic;

import com.atsuishio.superbwarfare.entity.vehicle.DroneEntity;
import com.mojang.logging.LogUtils;
import com.norwood.mavic.mixin.ChunkMapAccessor;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import net.minecraft.network.protocol.game.ClientboundForgetLevelChunkPacket;
import net.minecraft.network.protocol.game.ClientboundLevelChunkWithLightPacket;
import net.minecraft.network.protocol.game.ClientboundSetChunkCacheCenterPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.TicketType;
import net.minecraft.util.Unit;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.EntityLeaveLevelEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.slf4j.Logger;

import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class DroneChunkStreamer {

    private static final Logger LOG = LogUtils.getLogger();
    private static final int HEARTBEAT_TICKS = 40;

    private static final Comparator<Unit> UNIT_COMPARATOR = (a, b) -> 0;
    private static final TicketType<Unit> STREAM_TICKET =
            TicketType.create("mavic_stream", UNIT_COMPARATOR, 40);

    private final Map<UUID, LongOpenHashSet> subscribed = new HashMap<>();
    private final Map<UUID, String> lastDroneStatus = new HashMap<>();
    private final Set<DroneEntity> trackedDrones = new HashSet<>();
    private final Map<UUID, ServerPlayer> viewing = new HashMap<>();
    private final Map<UUID, ChunkPos> droneChunk = new HashMap<>();
    private final Map<UUID, ServerLevel> droneLevel = new HashMap<>();
    private final Set<UUID> workUuids = new HashSet<>();
    private final Map<UUID, Long> lastCenter = new HashMap<>();
    private static final LongOpenHashSet EMPTY_CHUNKS = new LongOpenHashSet();
    private long ticks;

    @SubscribeEvent
    public void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        tick(event.getServer());
    }

    @SubscribeEvent
    public void onEntityJoin(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide()) {
            return;
        }
        if (event.getEntity() instanceof DroneEntity d) {
            trackedDrones.add(d);
        }
    }

    @SubscribeEvent
    public void onEntityLeave(EntityLeaveLevelEvent event) {
        if (event.getLevel().isClientSide()) {
            return;
        }
        if (event.getEntity() instanceof DroneEntity d) {
            trackedDrones.remove(d);
        }
    }

    private void tick(MinecraftServer server) {
        this.ticks++;
        if (trackedDrones.isEmpty() && subscribed.isEmpty()) {
            return;
        }
        boolean heartbeat = this.ticks % HEARTBEAT_TICKS == 0;
        boolean verbose = MavicConfig.verboseLogging();
        int viewDistance = server.getPlayerList().getViewDistance();
        int radius = Math.min(MavicStreaming.STREAM_RADIUS, viewDistance);

        viewing.clear();
        droneChunk.clear();
        droneLevel.clear();

        for (DroneEntity drone : trackedDrones) {
            if (!(drone.level() instanceof ServerLevel level)) {
                continue;
            }
            UUID droneId = drone.getUUID();
            if (!MavicStreaming.isLinked(drone)) {
                if (verbose) status(droneId, drone, "unlinked", heartbeat);
                continue;
            }
            String controller = MavicStreaming.controllerUuid(drone);
            ServerPlayer operator = resolveOperator(server, controller);
            if (operator == null) {
                if (verbose) status(droneId, drone, "controller-offline(" + controller + ")", heartbeat);
                continue;
            }
            if (operator.level() != level) {
                if (verbose) status(droneId, drone, "operator-other-dimension", heartbeat);
                continue;
            }
            String reason = MavicStreaming.viewingReason(operator, drone);
            if (!reason.isEmpty()) {
                if (verbose) status(droneId, drone, "not-viewing[" + reason + "]", heartbeat);
                continue;
            }
            if (verbose) status(droneId, drone, "VIEWING by " + operator.getScoreboardName()
                    + " droneChunk=" + drone.chunkPosition(), heartbeat);
            viewing.put(operator.getUUID(), operator);
            droneChunk.put(operator.getUUID(), drone.chunkPosition());
            droneLevel.put(operator.getUUID(), level);
        }

        workUuids.clear();
        workUuids.addAll(this.subscribed.keySet());
        workUuids.addAll(viewing.keySet());

        Map<UUID, LongOpenHashSet> snapshot = new HashMap<>();

        for (UUID id : workUuids) {
            ServerPlayer operator = viewing.get(id);
            if (operator == null) {
                operator = resolveOperator(server, id);
            }
            if (operator == null) {
                this.subscribed.remove(id);
                continue;
            }

            boolean nowViewing = viewing.containsKey(id);
            boolean wasViewing = this.subscribed.containsKey(id);
            LongOpenHashSet have = this.subscribed.getOrDefault(id, EMPTY_CHUNKS);

            if (nowViewing) {
                ServerLevel level = droneLevel.get(id);
                ChunkPos drone = droneChunk.get(id);
                long droneKey = ChunkPos.asLong(drone.x, drone.z);
                Long prev = lastCenter.get(id);
                boolean moved = prev == null || prev.longValue() != droneKey;
                if (moved || this.ticks % 20 == 0) {
                    level.getChunkSource().addRegionTicket(STREAM_TICKET, drone, radius, Unit.INSTANCE);
                    lastCenter.put(id, droneKey);
                }
                setCenter(operator, drone.x, drone.z);
                if (verbose && !wasViewing) {
                    LOG.info("[Mavic] TRANSFER->drone: player {} view-center -> chunk {} radius {}",
                            operator.getScoreboardName(), drone, radius);
                }

                int sent = 0;
                int missing = 0;
                LongOpenHashSet next = new LongOpenHashSet();
                for (int dx = -radius; dx <= radius; dx++) {
                    for (int dz = -radius; dz <= radius; dz++) {
                        int cx = drone.x + dx;
                        int cz = drone.z + dz;
                        long pos = ChunkPos.asLong(cx, cz);
                        if (have.contains(pos)) {
                            next.add(pos);
                            continue;
                        }
                        LevelChunk chunk = level.getChunkSource().getChunkNow(cx, cz);
                        if (chunk == null) {
                            missing++;
                            continue;
                        }
                        sendChunk(operator, level, chunk);
                        sent++;
                        next.add(pos);
                    }
                }
                this.subscribed.put(id, next);
                if (!next.isEmpty()) {
                    snapshot.put(id, next);
                }
                if (verbose && (sent > 0 || heartbeat)) {
                    LOG.info("[Mavic] stream {} @ {}: sent={} live={} notLoadedYet={} droneVisHits={} entityReveals={}",
                            operator.getScoreboardName(), drone, sent, next.size(), missing,
                            MavicStreaming.forceRangeHits, MavicStreaming.entityRevealHits);
                    MavicStreaming.forceRangeHits = 0;
                    MavicStreaming.entityRevealHits = 0;
                }
            } else {
                for (long old : have) {
                    forget(operator, ChunkPos.getX(old), ChunkPos.getZ(old));
                }
                this.subscribed.remove(id);
                lastCenter.remove(id);
                if (operator.level() instanceof ServerLevel opLevel) {
                    ChunkPos body = operator.chunkPosition();
                    setCenter(operator, body.x, body.z);
                    int resent = resendSquare(operator, opLevel, body, Math.min(MavicStreaming.STREAM_RADIUS, viewDistance));
                    if (verbose) {
                        LOG.info("[Mavic] TRANSFER->body: player {} view-center -> chunk {}, forgot {} drone chunks, resent {} body chunks",
                                operator.getScoreboardName(), body, have.size(), resent);
                    }
                }
            }
        }

        MavicStreaming.publish(snapshot);
        for (Map.Entry<UUID, ServerPlayer> e : viewing.entrySet()) {
            reevaluateVisibility(droneLevel.get(e.getKey()), e.getValue());
        }
    }

    private void status(UUID droneId, DroneEntity drone, String msg, boolean heartbeat) {
        String prev = this.lastDroneStatus.get(droneId);
        if (!msg.equals(prev)) {
            LOG.info("[Mavic] drone {} @ {}: {}", shortId(droneId), drone.chunkPosition(), msg);
            this.lastDroneStatus.put(droneId, msg);
        } else if (heartbeat && msg.startsWith("VIEWING")) {
            LOG.info("[Mavic] drone {} heartbeat: {}", shortId(droneId), msg);
        }
    }

    private void reevaluateVisibility(ServerLevel level, ServerPlayer operator) {
        ChunkMapAccessor accessor = (ChunkMapAccessor) (Object) level.getChunkSource().chunkMap;
        for (Object te : accessor.mavic$entityMap().values()) {
            ((MavicTracked) te).mavic$updatePlayer(operator);
        }
    }

    @SubscribeEvent
    public void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            this.subscribed.remove(player.getUUID());
            this.lastCenter.remove(player.getUUID());
        }
    }

    @SubscribeEvent
    public void onDeath(LivingDeathEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            disengage(player);
        }
    }

    private void disengage(ServerPlayer player) {
        LongOpenHashSet streamed = this.subscribed.remove(player.getUUID());
        this.lastCenter.remove(player.getUUID());
        if (streamed == null) {
            return;
        }
        for (long pos : streamed) {
            forget(player, ChunkPos.getX(pos), ChunkPos.getZ(pos));
        }
        if (player.level() instanceof ServerLevel level) {
            ChunkPos body = player.chunkPosition();
            setCenter(player, body.x, body.z);
            int viewDistanceThere = level.getServer().getPlayerList().getViewDistance();
            resendSquare(player, level, body, Math.min(MavicStreaming.STREAM_RADIUS, viewDistanceThere));
            if (MavicConfig.verboseLogging()) {
                LOG.info("[Mavic] disengage: player {} recentered to {}", player.getScoreboardName(), body);
            }
        }
    }

    private int resendSquare(ServerPlayer player, ServerLevel level, ChunkPos center, int radius) {
        int resent = 0;
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                LevelChunk chunk = level.getChunkSource().getChunkNow(center.x + dx, center.z + dz);
                if (chunk != null) {
                    sendChunk(player, level, chunk);
                    resent++;
                }
            }
        }
        return resent;
    }

    private static void setCenter(ServerPlayer player, int chunkX, int chunkZ) {
        player.connection.send(new ClientboundSetChunkCacheCenterPacket(chunkX, chunkZ));
    }

    private static void sendChunk(ServerPlayer player, ServerLevel level, LevelChunk chunk) {
        player.connection.send(new ClientboundLevelChunkWithLightPacket(chunk, level.getLightEngine(), null, null));
    }

    private static void forget(ServerPlayer player, int chunkX, int chunkZ) {
        player.connection.send(new ClientboundForgetLevelChunkPacket(chunkX, chunkZ));
    }

    private static String shortId(UUID id) {
        return id.toString().substring(0, 8);
    }

    private static ServerPlayer resolveOperator(MinecraftServer server, String uuid) {
        if (uuid == null || uuid.isEmpty()) {
            return null;
        }
        try {
            return server.getPlayerList().getPlayer(UUID.fromString(uuid));
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }

    private static ServerPlayer resolveOperator(MinecraftServer server, UUID uuid) {
        return server.getPlayerList().getPlayer(uuid);
    }
}
