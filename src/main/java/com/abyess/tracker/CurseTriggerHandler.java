package com.abyess.tracker;

import com.abyess.Network.NetworkHandler;
import com.abyess.Network.PacketSyncLayerStatus;
import com.abyess.config.ModConfig;
import com.abyess.items.Scapegoat.CustomScapegoatManager;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.util.*;

public class CurseTriggerHandler {

    private static final Map<Integer, List<LayerTrigger>> dimensionTriggers = new HashMap<>();
    private static final Map<String, PlayerTriggerData> playerDataMap = new HashMap<>();
    private static final int SYNC_INTERVAL = 10;

    // This remains public as it's an initialization method, usually called by the mod's lifecycle.
    public static void initializeTriggers() {
        dimensionTriggers.clear();
        Map<Integer, ModConfig.DimensionConfig> dims = ModConfig.getConfigData().getDimensions();

        for (Map.Entry<Integer, ModConfig.DimensionConfig> entry : dims.entrySet()) {
            int dimensionId = entry.getKey();
            ModConfig.DimensionConfig dimensionConfig = entry.getValue();

            if (dimensionConfig.isEnabled()) {
                List<LayerTrigger> triggers = new ArrayList<>();
                for (ModConfig.LayerConfig layerConfig : dimensionConfig.getLayers()) {
                    if (layerConfig.isEnabled()) {
                        triggers.add(new LayerTrigger(layerConfig));
                    }
                }
                dimensionTriggers.put(dimensionId, triggers);
            }
        }
        CustomScapegoatManager.initialize();
    }

    // Changed to private, as it was likely only intended for internal use
    // or to support the now-removed public API method.
    private static List<LayerTrigger> getLayerTriggersForDimension(int dimensionId) {
        return Collections.unmodifiableList(dimensionTriggers.getOrDefault(dimensionId, Collections.emptyList()));
    }

    // REMOVED: This method provided external API logic. Its functionality will
    // now be handled by AbyessAPI using network-synced client-side state.
    /*
    public static boolean isPlayerInActiveCursedLayer(EntityPlayer player) {
        if (player == null || player.world.isRemote) {
            return false;
        }

        int currentDimension = player.dimension;
        List<LayerTrigger> triggers = dimensionTriggers.get(currentDimension);

        if (triggers != null && !triggers.isEmpty()) {
            double currentY = player.posY;
            for (LayerTrigger trigger : triggers) {
                if (trigger.isPlayerInLayer(currentY)) {
                    ModConfig.LayerConfig config = trigger.getLayerConfig();
                    if (config.getThreshold() != 0 || config.getContinuousTriggerInterval() > 0) {
                        return true;
                    }
                }
            }
        }
        return false;
    }
    */

    @SubscribeEvent
    public void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        EntityPlayer player = event.player;
        String uuid = player.getPersistentID().toString();
        playerDataMap.put(uuid, new PlayerTriggerData(player));

        if (!player.world.isRemote) {
            ModConfig.loadConfig();
            initializeTriggers();
        }
    }

    @SubscribeEvent
    public void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.player.world.isRemote) return;

        EntityPlayer player = event.player;
        String uuid = player.getPersistentID().toString();

        PlayerTriggerData playerData = playerDataMap.computeIfAbsent(uuid, id -> new PlayerTriggerData(player));

        int currentDimension = player.dimension;
        if (playerData.getLastKnownDimension() != currentDimension) {
            playerData.clearAllLayerStates();
            playerData.setLastKnownDimension(currentDimension);
            if (player instanceof EntityPlayerMP) {
                // Ensure NetworkHandler.sendLayerStatusPacket no longer takes hasScapegoat
                NetworkHandler.sendLayerStatusPacket((EntityPlayerMP) player, new ArrayList<>());
            }
        }

        List<LayerTrigger> triggers = dimensionTriggers.get(currentDimension);
        List<PacketSyncLayerStatus.LayerStatus> currentStatuses = new ArrayList<>();
        int currentTick = player.ticksExisted;

        if (triggers != null && !triggers.isEmpty()) {
            double currentY = player.posY;
            for (LayerTrigger trigger : triggers) {
                if (!trigger.isPlayerInLayer(currentY)) {
                    playerData.resetLayerState(trigger);
                    continue;
                }
                trigger.checkAndTrigger(player, playerData, currentY, currentTick);
                currentStatuses.add(trigger.getCurrentStatus(playerData, currentY, currentTick, currentDimension));
            }
        }

        if (currentTick % SYNC_INTERVAL == 0) {
            if (player instanceof EntityPlayerMP) {
                // Ensure NetworkHandler.sendLayerStatusPacket no longer takes hasScapegoat
                NetworkHandler.sendLayerStatusPacket((EntityPlayerMP) player, currentStatuses);
            }
        }
    }

    @SubscribeEvent
    public void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        String uuid = event.player.getPersistentID().toString();
        playerDataMap.remove(uuid);
    }

    @SubscribeEvent
    public void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.player.world.isRemote) return;

        String uuid = event.player.getPersistentID().toString();
        PlayerTriggerData playerData = playerDataMap.get(uuid);

        if (playerData != null) {
            // 保存当前维度信息
            int currentDimension = event.player.dimension;

            // 完全重置玩家数据
            playerDataMap.put(uuid, new PlayerTriggerData(event.player));
            playerData = playerDataMap.get(uuid);
            playerData.setLastKnownDimension(currentDimension);
        }
    }

    private static class PlayerTriggerData {
        private final EntityPlayer player;
        private final Map<LayerTrigger, LayerState> layerStates = new HashMap<>();
        private int lastKnownDimension;

        public PlayerTriggerData(EntityPlayer player) {
            this.player = player;
            this.lastKnownDimension = player.dimension;
        }

        public LayerState getLayerState(LayerTrigger trigger, double currentY) {
            return layerStates.computeIfAbsent(trigger,
                    t -> new LayerState(currentY, currentY, player.ticksExisted));
        }

        public void resetLayerState(LayerTrigger trigger) {
            layerStates.remove(trigger);
        }

        public void clearAllLayerStates() {
            this.layerStates.clear();
        }

        public int getLastKnownDimension() {
            return this.lastKnownDimension;
        }

        public void setLastKnownDimension(int dimension) {
            this.lastKnownDimension = dimension;
        }
    }

    private static class LayerState {
        double minY;
        double maxY;
        int lastTriggerTick;

        public LayerState(double minY, double maxY, int lastTriggerTick) {
            this.minY = minY;
            this.maxY = maxY;
            this.lastTriggerTick = lastTriggerTick;
        }
    }

    public static class LayerTrigger { // This class might remain public if other Abyess components access it directly
        private final ModConfig.LayerConfig layerConfig;

        public LayerTrigger(ModConfig.LayerConfig layerConfig) {
            this.layerConfig = layerConfig;
        }

        public ModConfig.LayerConfig getLayerConfig() {
            return layerConfig;
        }

        public boolean isPlayerInLayer(double currentY) {
            int[] range = layerConfig.getRange();
            return currentY >= range[0] && currentY <= range[1];
        }

        public PacketSyncLayerStatus.LayerStatus getCurrentStatus(PlayerTriggerData playerData, double currentY, int currentTick, int dimensionId) {
            LayerState state = playerData.getLayerState(this, currentY);
            int threshold = layerConfig.getThreshold();
            String layerName = layerConfig.getName();
            String color = layerConfig.getColor();
            boolean hudEnabled = layerConfig.isHudEnabled();
            boolean showTimeDistance = layerConfig.isShowTimeDistance();

            boolean fogEnabled = layerConfig.isFogEnabled();
            float[] fogColorRGB = layerConfig.getFogColorRGB();
            float fogDensity = layerConfig.getFogDensity();
            String fogType = layerConfig.getFogType();
            float fogStart = layerConfig.getFogStart();
            float fogEnd = layerConfig.getFogEnd();

            if (threshold > 0) {
                double progress = currentY - state.minY;
                float remaining = (float) Math.max(0, threshold - progress);
                return new PacketSyncLayerStatus.LayerStatus(
                        layerName, hudEnabled, color, false, remaining, threshold, dimensionId, showTimeDistance,
                        fogEnabled, fogColorRGB, fogDensity, fogType, fogStart, fogEnd);
            } else if (threshold < 0) {
                int absThreshold = Math.abs(threshold);
                double progress = state.maxY - currentY;
                float remaining = (float) Math.max(0, absThreshold - progress);
                return new PacketSyncLayerStatus.LayerStatus(
                        layerName, hudEnabled, color, false, remaining, absThreshold, dimensionId, showTimeDistance,
                        fogEnabled, fogColorRGB, fogDensity, fogType, fogStart, fogEnd);
            } else {
                int intervalTicks = layerConfig.getContinuousTriggerInterval() * 20;
                int remainingTicks = Math.max(0, state.lastTriggerTick + intervalTicks - currentTick);
                float remainingSeconds = remainingTicks / 20.0f;
                return new PacketSyncLayerStatus.LayerStatus(
                        layerName, hudEnabled, color, true,
                        remainingSeconds, layerConfig.getContinuousTriggerInterval(), dimensionId, showTimeDistance,
                        fogEnabled, fogColorRGB, fogDensity, fogType, fogStart, fogEnd);
            }
        }

        public void checkAndTrigger(EntityPlayer player, PlayerTriggerData playerData, double currentY, int currentTick) {
            LayerState state = playerData.getLayerState(this, currentY);
            int threshold = layerConfig.getThreshold();

            if (threshold > 0) {
                handleRisingTrigger(player, state, currentY);
            } else if (threshold < 0) {
                handleFallingTrigger(player, state, currentY);
            } else {
                handleContinuousTrigger(player, state, currentTick);
            }
        }

        private void handleRisingTrigger(EntityPlayer player, LayerState state, double currentY) {
            int threshold = layerConfig.getThreshold();

            if (currentY > state.maxY) {
                state.maxY = currentY;
            }

            if (currentY < state.minY) {
                state.minY = currentY;
                state.maxY = currentY;
            }

            double heightDifference = state.maxY - state.minY;

            if (heightDifference >= threshold) {
                applyCurses(player);
                state.minY = state.maxY = currentY;
            }
        }

        private void handleFallingTrigger(EntityPlayer player, LayerState state, double currentY) {
            int absThreshold = Math.abs(layerConfig.getThreshold());

            if (currentY < state.minY) {
                state.minY = currentY;
            }

            if (currentY > state.maxY) {
                state.maxY = currentY;
                state.minY = currentY;
            }

            double heightDifference = state.maxY - state.minY;

            if (heightDifference >= absThreshold) {
                applyCurses(player);
                state.minY = state.maxY = currentY;
            }
        }

        private void handleContinuousTrigger(EntityPlayer player, LayerState state, int currentTick) {
            int intervalTicks = layerConfig.getContinuousTriggerInterval() * 20;

            // 确保不会因为玩家死亡而导致冷却时间异常
            if (state.lastTriggerTick > currentTick) {
                // 如果 lastTriggerTick 大于当前 tick（可能由于死亡重置），则调整它
                state.lastTriggerTick = currentTick - intervalTicks;
            }

            if (currentTick - state.lastTriggerTick >= intervalTicks) {
                applyCurses(player);
                state.lastTriggerTick = currentTick;
            }
        }

        private void applyCurses(EntityPlayer player) {
            String layerName = layerConfig.getName();
            int durabilityCost = layerConfig.getDurabilityCost();

            boolean absorbed = CustomScapegoatManager.tryAbsorbCurse(player, durabilityCost, layerName);

            if (!absorbed) {
                for (ModConfig.CurseConfig curse : layerConfig.getCurses()) {
                    CurseApplication.applyCurse(player, curse);
                }
            }
        }
    }
}