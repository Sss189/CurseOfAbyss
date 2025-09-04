package com.abyess.Network;

import io.netty.buffer.ByteBuf;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.fml.common.network.ByteBufUtils;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

// PacketSyncPersistentNBT.java
public class PacketSyncPersistentNBT implements IMessage {
    private NBTTagCompound nbt;

    public PacketSyncPersistentNBT() {}
    public PacketSyncPersistentNBT(NBTTagCompound nbt) {
        this.nbt = nbt;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        nbt = ByteBufUtils.readTag(buf);
    }

    @Override
    public void toBytes(ByteBuf buf) {
        ByteBufUtils.writeTag(buf, nbt);
    }

    public static class Handler implements IMessageHandler<PacketSyncPersistentNBT, IMessage> {
        @Override
        public IMessage onMessage(PacketSyncPersistentNBT message, MessageContext ctx) {
            Minecraft.getMinecraft().addScheduledTask(() -> {
                EntityPlayer player = Minecraft.getMinecraft().player;
                player.getEntityData().setTag(EntityPlayer.PERSISTED_NBT_TAG, message.nbt);
            });
            return null;
        }
    }
}