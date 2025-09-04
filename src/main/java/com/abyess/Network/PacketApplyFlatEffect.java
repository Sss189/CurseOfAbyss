// 文件：PacketApplyFlatEffect.java
package com.abyess.Network;

import io.netty.buffer.ByteBuf;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.FlatEntities;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

// 消息：在客户端触发扁平化效果
public class PacketApplyFlatEffect implements IMessage {
    private int duration;

    public PacketApplyFlatEffect() {}

    public PacketApplyFlatEffect(int duration) {
        this.duration = duration;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        duration = buf.readInt();
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeInt(duration);
    }

    public static class Handler implements IMessageHandler<PacketApplyFlatEffect, IMessage> {
        @Override
        public IMessage onMessage(PacketApplyFlatEffect message, MessageContext ctx) {
            // 切回主线程执行
            Minecraft.getMinecraft().addScheduledTask(() -> {
                FlatEntities.applyEffect(message.duration);
            });
            return null;
        }
    }
}