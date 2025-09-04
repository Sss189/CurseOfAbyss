package com.abyess.items.Starcompass;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.text.TextComponentString; // 尽管不再直接使用，但如果其他地方依赖它，可以保留
import net.minecraft.world.World;
import net.minecraftforge.common.DimensionManager;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent;
import net.minecraftforge.fml.common.network.ByteBufUtils;
import net.minecraftforge.fml.common.network.NetworkRegistry;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import net.minecraftforge.fml.common.network.simpleimpl.SimpleNetworkWrapper;
import net.minecraftforge.fml.relauncher.Side;
import io.netty.buffer.ByteBuf;

@Mod.EventBusSubscriber
public class DeathRespawnNotifier {

    // 自定义数据标签，用于在玩家持久化 NBT 中存储我们的数据
    public static final String MOD_DATA_TAG = "StarcompassModData";
    private static final String DEATH_TAG = "DeathData";
    private static final String RESPAWN_TAG = "RespawnData";

    // 创建网络通道
    public static final SimpleNetworkWrapper CHANNEL = NetworkRegistry.INSTANCE.newSimpleChannel("starcompass_sync");
    private static int packetId = 0;

    static {
        // 注册网络数据包处理器
        CHANNEL.registerMessage(PacketSyncPersistentNBT.Handler.class, PacketSyncPersistentNBT.class, packetId++, Side.CLIENT);
    }

    // 处理玩家登录事件，加载持久化数据并同步到客户端
    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.player instanceof EntityPlayerMP) {
            EntityPlayerMP player = (EntityPlayerMP) event.player;
            // 确保在玩家登录时加载并同步数据
            loadAndSyncPlayerData(player);

            // 登录时也尝试显示上次的死亡位置
            NBTTagCompound playerPersistentData = getOrCreateModTag(player);
            NBTTagCompound deathData = playerPersistentData.getCompoundTag(DEATH_TAG);

            // --- 修复开始 ---
            if (deathData.hasKey("deathDim")) {
                double deathX = deathData.getDouble("deathX");
                double deathY = deathData.getDouble("deathY");
                double deathZ = deathData.getDouble("deathZ");
                int deathDim = deathData.getInteger("deathDim");

                // **关键修改：先检查维度是否已注册，再尝试获取提供者**
                if (DimensionManager.isDimensionRegistered(deathDim)) {
                    // 如果需要，这里可以安全地获取 WorldProvider
                    // String deathDimensionName = DimensionManager.getProvider(deathDim).getDimensionType().getName();
                    // 消息发送逻辑已删除
                } else {
                    // 维度未注册，可以记录日志或采取默认行为
                    System.err.println("Warning: Player " + player.getName() + " logged in with an unregistered death dimension ID: " + deathDim + ". Defaulting death location to player's current dimension.");
                    // 你可以选择将死亡点维度修改为当前维度，或者直接忽略此死亡点数据
                    // 例如：deathData.setInteger("deathDim", player.dimension);
                    // 消息发送逻辑已删除
                }
            }
            // --- 修复结束 ---
        }
    }

    // 处理玩家死亡事件
    @SubscribeEvent
    public static void onPlayerDeath(LivingDeathEvent event) {
        if (event.getEntity() instanceof EntityPlayerMP) {
            EntityPlayerMP player = (EntityPlayerMP) event.getEntity();

            // 获取死亡位置
            double deathX = player.posX;
            double deathY = player.posY;
            double deathZ = player.posZ;
            int deathDim = player.dimension;

            // 存储到自定义 NBT
            NBTTagCompound modData = getOrCreateModTag(player);
            NBTTagCompound deathData = new NBTTagCompound();
            deathData.setDouble("deathX", deathX);
            deathData.setDouble("deathY", deathY);
            deathData.setDouble("deathZ", deathZ);
            deathData.setInteger("deathDim", deathDim);
            modData.setTag(DEATH_TAG, deathData);

            // 显示消息 (此处的 sendMessage 方法调用将被删除)
            // if (DimensionManager.isDimensionRegistered(deathDim)) { // 修正此处
            //     String dimensionName = DimensionManager.getProvider(deathDim).getDimensionType().getName();
            //     sendMessage(player, String.format(
            //             "§c你已死亡！位置: §e%s §c坐标: §e%s",
            //             dimensionName,
            //             formatPosition(deathX, deathY, deathZ)
            //     ));
            // } else {
            //     sendMessage(player, String.format(
            //             "§c你已死亡！坐标: §e%s §c(维度ID无效：%d)",
            //             formatPosition(deathX, deathY, deathZ), deathDim
            //     ));
            // }

            // 死亡时也同步一次数据，确保客户端最新
            syncModData(player);
        }
    }

    // 处理玩家重生事件
    @SubscribeEvent
    public static void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (!(event.player instanceof EntityPlayerMP)) return;

        EntityPlayerMP player = (EntityPlayerMP) event.player;
        World world = player.world;

        if (!world.isRemote) { // 只在服务器端处理
            world.getMinecraftServer().addScheduledTask(() -> {
                // 获取重生位置
                double respawnX = player.posX;
                double respawnY = player.posY;
                double respawnZ = player.posZ;
                int respawnDim = player.dimension;

                // 存储到自定义 NBT
                NBTTagCompound modData = getOrCreateModTag(player);
                NBTTagCompound respawnData = new NBTTagCompound();
                respawnData.setDouble("respawnX", respawnX);
                respawnData.setDouble("respawnY", respawnY);
                respawnData.setDouble("respawnZ", respawnZ);
                respawnData.setInteger("respawnDim", respawnDim);
                modData.setTag(RESPAWN_TAG, respawnData);

                // 显示消息 (此处的 sendMessage 方法调用将被删除)
                // if (DimensionManager.isDimensionRegistered(respawnDim)) { // 修正此处
                //     String dimensionName = DimensionManager.getProvider(respawnDim).getDimensionType().getName();
                // } else {
                // }

                // 显示死亡位置（如果存在） (此处的 sendMessage 方法调用将被删除)
                NBTTagCompound deathData = modData.getCompoundTag(DEATH_TAG);
                if (deathData.hasKey("deathDim")) {
                    double deathX = deathData.getDouble("deathX");
                    double deathY = deathData.getDouble("deathY");
                    double deathZ = deathData.getDouble("deathZ");
                    int deathDim = deathData.getInteger("deathDim");
                    // if (DimensionManager.isDimensionRegistered(deathDim)) { // 修正此处
                    //     String deathDimensionName = DimensionManager.getProvider(deathDim).getDimensionType().getName();
                    // } else {
                    // }
                }

                // 重生时也同步一次数据，确保客户端最新
                syncModData(player);
            });
        }
    }

    // 处理玩家下线事件，将数据保存到玩家的持久化 NBT
    @SubscribeEvent
    public static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.player instanceof EntityPlayerMP) {
            EntityPlayerMP player = (EntityPlayerMP) event.player;
            savePlayerData(player);
        }
    }

    // 从玩家的持久化 NBT 中获取或创建我们自定义的 NBTTagCompound
    private static NBTTagCompound getOrCreateModTag(EntityPlayer player) {
        // 获取玩家的持久化 NBT 数据
        NBTTagCompound persistentData = player.getEntityData().getCompoundTag(EntityPlayer.PERSISTED_NBT_TAG);

        // 如果没有我们自定义的 Mod 数据标签，则创建一个
        if (!persistentData.hasKey(MOD_DATA_TAG)) {
            persistentData.setTag(MOD_DATA_TAG, new NBTTagCompound());
        }
        return persistentData.getCompoundTag(MOD_DATA_TAG);
    }

    // 将我们自定义的数据保存到玩家的持久化 NBT
    private static void savePlayerData(EntityPlayerMP player) {
        // 获取玩家的 EntityData
        NBTTagCompound entityData = player.getEntityData();
        // 获取或创建 PERSISTED_NBT_TAG
        NBTTagCompound persistentData = entityData.getCompoundTag(EntityPlayer.PERSISTED_NBT_TAG);

        // 获取或更新我们的自定义 MOD_DATA_TAG
        NBTTagCompound modData = getOrCreateModTag(player); // 这一步已经更新了 persistentData 内部的 MOD_DATA_TAG

        // 关键一步：将更新后的 persistentData 重新设置回 EntityData
        // 这确保了对 persistentData 的修改会被 Minecraft 的保存机制捕获
        entityData.setTag(EntityPlayer.PERSISTED_NBT_TAG, persistentData);
    }

    // 加载并同步玩家数据到客户端
    private static void loadAndSyncPlayerData(EntityPlayerMP player) {
        // 在服务器端获取我们自定义的 NBT 数据
        NBTTagCompound modData = getOrCreateModTag(player);
        // 将这个 NBT 数据同步到客户端
        CHANNEL.sendTo(new PacketSyncPersistentNBT(modData), player);
    }

    // 同步我们自定义的 Mod 数据到客户端
    private static void syncModData(EntityPlayerMP player) {
        // 在服务器端获取我们自定义的 NBT 数据
        NBTTagCompound modData = getOrCreateModTag(player);
        // 将这个 NBT 数据同步到客户端
        CHANNEL.sendTo(new PacketSyncPersistentNBT(modData), player);
    }

    // 格式化坐标
    private static String formatPosition(double x, double y, double z) {
        return String.format("(%.1f, %.1f, %.1f)", x, y, z);
    }

    // 网络数据包：同步我们自定义的 Mod NBT 数据
    public static class PacketSyncPersistentNBT implements IMessage {
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

        // 客户端处理器
        public static class Handler implements IMessageHandler<PacketSyncPersistentNBT, IMessage> {
            @Override
            public IMessage onMessage(PacketSyncPersistentNBT message, MessageContext ctx) {
                if (ctx.side == Side.CLIENT && message.nbt != null) {
                    // 在客户端主线程执行
                    net.minecraft.client.Minecraft.getMinecraft().addScheduledTask(() -> {
                        EntityPlayer player = net.minecraft.client.Minecraft.getMinecraft().player;
                        if (player != null) {
                            // 将接收到的 NBT 数据存储到客户端玩家的 EntityData 中
                            // 注意：这里我们是将整个 MOD_DATA_TAG 替换掉，而不是 PERSISTED_NBT_TAG
                            // 因为 PERSISTED_NBT_TAG 是由服务器管理的主要持久化数据
                            // 客户端只需要我们自定义的那部分数据
                            NBTTagCompound clientPersistentData = player.getEntityData().getCompoundTag(EntityPlayer.PERSISTED_NBT_TAG);
                            clientPersistentData.setTag(MOD_DATA_TAG, message.nbt);

                            // 如果 persistentData 本身不存在，需要创建一下
                            if (!player.getEntityData().hasKey(EntityPlayer.PERSISTED_NBT_TAG)) {
                                player.getEntityData().setTag(EntityPlayer.PERSISTED_NBT_TAG, clientPersistentData);
                            }
                        }
                    });
                }
                return null;
            }
        }
    }
}