package com.abyess.Network;



import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

public class PacketCompassUpdate implements IMessage {
    private EnumHand hand;
    private BlockPos target;
    private int dimension;

    public PacketCompassUpdate() {}

    public PacketCompassUpdate(EnumHand hand, BlockPos target, int dimension) {
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

    public static class Handler implements IMessageHandler<PacketCompassUpdate, IMessage> {
        @Override
        public IMessage onMessage(PacketCompassUpdate message, MessageContext ctx) {
            // 在服务端主线程执行
            ctx.getServerHandler().player.getServerWorld().addScheduledTask(() -> {
                EntityPlayer player = ctx.getServerHandler().player;
                ItemStack stack = player.getHeldItem(message.hand);

                if (!stack.isEmpty() && stack.getItem() instanceof com.abyess.items.Starcompass.ItemStarCompass) {
                    // 创建或获取NBT标签
                    NBTTagCompound nbt = stack.getTagCompound();
                    if (nbt == null) {
                        nbt = new NBTTagCompound();
                    }

                    // 设置坐标和维度
                    nbt.setLong("TargetPos", message.target.toLong());
                    nbt.setInteger("TargetDim", message.dimension);
                    stack.setTagCompound(nbt);

                    // 更新玩家手中的物品
                    if (message.hand == EnumHand.MAIN_HAND) {
                        player.inventory.setInventorySlotContents(player.inventory.currentItem, stack);
                    } else {
                        player.inventory.offHandInventory.set(0, stack);
                    }
                    player.inventory.markDirty();
                }
            });
            return null;
        }
    }
}