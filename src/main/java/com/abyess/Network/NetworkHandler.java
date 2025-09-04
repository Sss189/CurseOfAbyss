package com.abyess.Network;

import com.abyess.Network.PacketSyncLayerStatus; // 确保导入新创建的数据包类
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.common.network.NetworkRegistry;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.SimpleNetworkWrapper;
import net.minecraftforge.fml.relauncher.Side;

import java.util.List;

public class NetworkHandler {
    // 创建网络通道
    public static final SimpleNetworkWrapper INSTANCE =
            NetworkRegistry.INSTANCE.newSimpleChannel("starcompass");

    private static int packetId = 0;

    public static void registerPackets() {
        // 注册PacketOpenGui（客户端处理）
        INSTANCE.registerMessage(
                PacketOpenGui.Handler.class,
                PacketOpenGui.class,
                packetId++,
                Side.CLIENT
        );

        // 注册PacketCompassUpdate（服务端处理）
        INSTANCE.registerMessage(
                PacketCompassUpdate.Handler.class,
                PacketCompassUpdate.class,
                packetId++,
                Side.SERVER
        );

        // 注册扁平化效果数据包
        INSTANCE.registerMessage(
                PacketApplyFlatEffect.Handler.class,
                PacketApplyFlatEffect.class,
                packetId++,
                Side.CLIENT
        );

        // ===== 新增：层状态同步数据包 =====
        INSTANCE.registerMessage(
                PacketSyncLayerStatus.Handler.class, // 处理器类
                PacketSyncLayerStatus.class,          // 数据包类
                packetId++,                          // 递增ID
                Side.CLIENT                           // 客户端处理
        );
    }

    // 发送打开GUI的数据包到客户端
    public static void sendOpenGuiPacket(EntityPlayerMP player, EnumHand hand,
                                         BlockPos target, int dimension) {
        INSTANCE.sendTo(new PacketOpenGui(hand, target, dimension), player);
    }

    // 发送罗盘更新数据包到服务端
    public static void sendCompassUpdate(EntityPlayer player, EnumHand hand,
                                         BlockPos target, int dimension) {
        if (player.world.isRemote) {
            INSTANCE.sendToServer(new PacketCompassUpdate(hand, target, dimension));
        }
    }

    // 发送扁平化效果数据包
    public static void sendFlatEffectPacket(EntityPlayerMP player, int durationSeconds) {
        INSTANCE.sendTo(new PacketApplyFlatEffect(durationSeconds), player);
    }

    // ===== 新增：发送层状态同步数据包 =====
    /**
     * 向玩家发送层状态同步数据包
     * @param player 目标玩家
     * @param statusList 层状态列表
     */
    public static void sendLayerStatusPacket(EntityPlayerMP player, List<PacketSyncLayerStatus.LayerStatus> statusList) {
        INSTANCE.sendTo(new PacketSyncLayerStatus(statusList), player);
    }
}