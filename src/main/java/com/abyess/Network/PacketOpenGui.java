package com.abyess.Network;

import io.netty.buffer.ByteBuf;
import net.minecraft.client.Minecraft;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import com.abyess.items.Starcompass.GuiStarCompass;

public class PacketOpenGui implements IMessage {
    private EnumHand hand;
    private BlockPos target;
    private int dimension;

    public PacketOpenGui() {}

    public PacketOpenGui(EnumHand hand, BlockPos target, int dimension) {
        this.hand = hand;
        this.target = target;
        this.dimension = dimension;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        this.hand = buf.readBoolean() ? EnumHand.MAIN_HAND : EnumHand.OFF_HAND;
        this.target = BlockPos.fromLong(buf.readLong());
        this.dimension = buf.readInt();
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeBoolean(this.hand == EnumHand.MAIN_HAND);
        buf.writeLong(this.target.toLong());
        buf.writeInt(this.dimension);
    }

    public static class Handler implements IMessageHandler<PacketOpenGui, IMessage> {
        @Override
        public IMessage onMessage(PacketOpenGui message, MessageContext ctx) {
            // 在客户端主线程打开GUI
            Minecraft.getMinecraft().addScheduledTask(() -> {
                Minecraft.getMinecraft().displayGuiScreen(
                        new GuiStarCompass(
                                Minecraft.getMinecraft().player,
                                Minecraft.getMinecraft().world,
                                message.hand,
                                message.target,
                                message.dimension
                        )
                );
            });
            return null;
        }
    }
}