package com.abyess.Network;

import com.abyess.api.AbyessAPI;
import com.abyess.tracker.CurseHUD;
import com.abyess.tracker.FogRenderer;
import io.netty.buffer.ByteBuf;
import net.minecraft.client.Minecraft;
import net.minecraft.util.text.TextComponentString;
import net.minecraftforge.fml.common.network.ByteBufUtils;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class PacketSyncLayerStatus implements IMessage {

    public static class LayerStatus {
        public final String layerName;
        public final String color;
        public final float value;
        public final boolean isContinuous;
        public final int maxValue;
        public final boolean hudEnabled;
        public final int dimensionId;
        public final boolean showTimeDistance;

        public final boolean fogEnabled;
        public final float[] fogColorRGB;
        public final float fogDensity;
        public final String fogType;
        public final float fogStart;
        public final float fogEnd;

        public LayerStatus(String layerName, boolean hudEnabled, String color, boolean isContinuous, float value,
                           int maxValue, int dimensionId, boolean showTimeDistance,
                           boolean fogEnabled, float[] fogColorRGB, float fogDensity,
                           String fogType, float fogStart, float fogEnd) {
            this.layerName = layerName;
            this.hudEnabled = hudEnabled;
            this.color = color;
            this.isContinuous = isContinuous;
            this.value = value;
            this.maxValue = maxValue;
            this.dimensionId = dimensionId;
            this.showTimeDistance = showTimeDistance;

            this.fogEnabled = fogEnabled;
            if (fogColorRGB != null && fogColorRGB.length == 3) {
                this.fogColorRGB = Arrays.copyOf(fogColorRGB, 3);
            } else {
                this.fogColorRGB = new float[]{1.0F, 1.0F, 1.0F};
            }
            this.fogDensity = fogDensity;
            this.fogType = (fogType != null && !fogType.isEmpty()) ? fogType : "EXP2";
            this.fogStart = fogStart;
            this.fogEnd = fogEnd;
        }
    }

    private List<LayerStatus> statusList = new ArrayList<>();

    public PacketSyncLayerStatus() {}

    public PacketSyncLayerStatus(List<LayerStatus> statusList) {
        this.statusList = statusList;
    }

    public List<LayerStatus> getStatusList() {
        return statusList;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        int count = buf.readInt();
        statusList.clear();
        for (int i = 0; i < count; i++) {
            String layerName = ByteBufUtils.readUTF8String(buf);
            boolean hudEnabled = buf.readBoolean();
            String color = ByteBufUtils.readUTF8String(buf);
            boolean isContinuous = buf.readBoolean();
            float value = buf.readFloat();
            int maxValue = buf.readInt();
            int dimensionId = buf.readInt();
            boolean showTimeDistance = buf.readBoolean();

            boolean fogEnabled = buf.readBoolean();
            float fogR = buf.readFloat();
            float fogG = buf.readFloat();
            float fogB = buf.readFloat();
            float fogDensity = buf.readFloat();
            String fogType = ByteBufUtils.readUTF8String(buf);
            float fogStart = buf.readFloat();
            float fogEnd = buf.readFloat();

            statusList.add(new LayerStatus(
                    layerName, hudEnabled, color, isContinuous, value,
                    maxValue, dimensionId, showTimeDistance,
                    fogEnabled, new float[]{fogR, fogG, fogB}, fogDensity,
                    fogType, fogStart, fogEnd
            ));
        }
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeInt(statusList.size());
        for (LayerStatus status : statusList) {
            ByteBufUtils.writeUTF8String(buf, status.layerName);
            buf.writeBoolean(status.hudEnabled);
            ByteBufUtils.writeUTF8String(buf, status.color);
            buf.writeBoolean(status.isContinuous);
            buf.writeFloat(status.value);
            buf.writeInt(status.maxValue);
            buf.writeInt(status.dimensionId);
            buf.writeBoolean(status.showTimeDistance);

            buf.writeBoolean(status.fogEnabled);
            buf.writeFloat(status.fogColorRGB[0]);
            buf.writeFloat(status.fogColorRGB[1]);
            buf.writeFloat(status.fogColorRGB[2]);
            buf.writeFloat(status.fogDensity);
            ByteBufUtils.writeUTF8String(buf, status.fogType);
            buf.writeFloat(status.fogStart);
            buf.writeFloat(status.fogEnd);
        }
    }

    public static class Handler implements IMessageHandler<PacketSyncLayerStatus, IMessage> {

        @Override
        @SideOnly(Side.CLIENT)
        public IMessage onMessage(PacketSyncLayerStatus message, MessageContext ctx) {
            Minecraft.getMinecraft().addScheduledTask(() -> {
                List<LayerStatus> statuses = message.getStatusList();

                CurseHUD.updateStatuses(statuses);

                FogRenderer.updateActiveFogLayers(statuses);

                AbyessAPI.updateCachedLayerStatuses(statuses);
            });
            return null;
        }
    }
}