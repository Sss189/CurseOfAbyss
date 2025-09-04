package com.abyess.items.Starcompass;

import com.abyess.Network.NetworkHandler;
import com.abyess.config.ModConfig;
import net.minecraft.client.renderer.tileentity.TileEntityItemStackRenderer;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.item.EntityItemFrame;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.*;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentString; // Still needed for direct string messages, but we'll use I18n for messages that should be translatable
import net.minecraft.world.World;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.lwjgl.input.Keyboard;
import net.minecraft.client.resources.I18n; // Import I18n

import javax.annotation.Nullable;
import java.lang.reflect.Field;
import java.util.List;

public class ItemStarCompass extends Item {
    public static final ItemStarCompass INSTANCE = new ItemStarCompass();
    private static final String NBT_TARGET_KEY = "TargetPos";
    private static final String NBT_DIMENSION_KEY = "TargetDim"; // 维度存储键名

    public ItemStarCompass() {
        setCreativeTab(CreativeTabs.TOOLS);
        setRegistryName("star_compass");
        setUnlocalizedName("star_compass");
        setMaxStackSize(1);
    }

    // --- (Previous methods like getTargetDimension, initNBT, etc. remain unchanged) ---

    // 获取目标维度（返回Integer.MIN_VALUE表示未设置）
    public static int getTargetDimension(ItemStack stack) {
        if (stack.hasTagCompound()) {
            NBTTagCompound nbt = stack.getTagCompound();
            // 检查维度标签是否存在
            if (nbt.hasKey(NBT_DIMENSION_KEY, 3)) { // 3 表示整型
                return nbt.getInteger(NBT_DIMENSION_KEY);
            }
        }
        return Integer.MIN_VALUE; // 特殊值表示未设置
    }

    // 初始化NBT时处理维度
    private void initNBT(ItemStack stack) {
        if (!stack.hasTagCompound()) {
            stack.setTagCompound(new NBTTagCompound());
        }

        NBTTagCompound nbt = stack.getTagCompound();

        // 仅在无目标坐标且无维度时写入默认值
        if (!nbt.hasKey(NBT_TARGET_KEY, 99) && !nbt.hasKey(NBT_DIMENSION_KEY, 3)) {
            BlockPos defaultPos = getDefaultTarget();
            int defaultDim = getDefaultDimension();

            if (defaultPos != null && defaultDim != Integer.MIN_VALUE) {
                writeTargetToNBT(nbt, defaultPos);
                writeDimensionToNBT(nbt, defaultDim);
            }
        }
    }

    // 新增：维度写入NBT
    private void writeDimensionToNBT(NBTTagCompound nbt, int dim) {
        nbt.setInteger(NBT_DIMENSION_KEY, dim);
    }

    // 获取默认维度（返回Integer.MIN_VALUE表示无默认值）
    private static int getDefaultDimension() {
        // 不使用默认维度
        return Integer.MIN_VALUE;
    }

    // 更新坐标和维度
    public static void updatePosition(ItemStack stack, BlockPos pos, int dim) {
        if (!stack.hasTagCompound()) {
            stack.setTagCompound(new NBTTagCompound());
        }

        NBTTagCompound nbt = stack.getTagCompound();
        nbt.setLong(NBT_TARGET_KEY, pos.toLong());
        nbt.setInteger(NBT_DIMENSION_KEY, dim); // 存储维度
        stack.setTagCompound(nbt);
    }

    // 修改现有的 getTargetPosition 方法
    public static BlockPos getTargetPosition(ItemStack stack) {
        if (stack.hasTagCompound()) {
            NBTTagCompound nbt = stack.getTagCompound();
            if (nbt.hasKey("TargetPos", 99)) { // 99 表示长整型
                return BlockPos.fromLong(nbt.getLong("TargetPos"));
            }
        }
        return null;
    }

    @Override
    public ActionResult<ItemStack> onItemRightClick(World world, EntityPlayer player, EnumHand hand) {
        ItemStack stack = player.getHeldItem(hand);

        if (player.isSneaking()) {
            if (stack.isEmpty() || stack.getItem() != this) {
                return new ActionResult<>(EnumActionResult.FAIL, stack);
            }

            // 只在服务端处理
            if (!world.isRemote) {
                // 获取当前坐标数据
                BlockPos target = getTargetPosition(stack);
                int dim = getTargetDimension(stack); // 直接获取维度值

                // 发送打开GUI的指令到客户端
                NetworkHandler.sendOpenGuiPacket(
                        (EntityPlayerMP) player,
                        hand,
                        target != null ? target : BlockPos.ORIGIN,
                        dim // 直接传递维度值
                );
            }
            return new ActionResult<>(EnumActionResult.SUCCESS, stack);
        }
        return new ActionResult<>(EnumActionResult.PASS, stack);
    }

    // 在 ItemStarCompass 类中添加
    public static boolean isCustomCompass(ItemStack stack) {
        if (stack.isEmpty()) return false;

        List<ModConfig.CustomCompassConfig> customCompasses = ModConfig.getConfigData().getCustomCompasses();
        if (customCompasses == null || customCompasses.isEmpty()) return false;

        String itemId = stack.getItem().getRegistryName().toString();
        return customCompasses.stream()
                .anyMatch(config ->  config.getItemId().equals(itemId));
    }

    public static BlockPos getCustomCompassTarget(ItemStack stack) {
        if (stack.isEmpty()) return null;

        List<ModConfig.CustomCompassConfig> customCompasses = ModConfig.getConfigData().getCustomCompasses();
        if (customCompasses == null || customCompasses.isEmpty()) return null;

        String itemId = stack.getItem().getRegistryName().toString();
        for (ModConfig.CustomCompassConfig config : customCompasses) {
            if ( config.getItemId().equals(itemId)) {
                int[] pos = config.getTargetPos();
                return new BlockPos(pos[0], pos[1], pos[2]);
            }
        }
        return null;
    }

    public static int getCustomCompassDimension(ItemStack stack) {
        if (stack.isEmpty()) return Integer.MIN_VALUE;

        List<ModConfig.CustomCompassConfig> customCompasses = ModConfig.getConfigData().getCustomCompasses();
        if (customCompasses == null || customCompasses.isEmpty()) return Integer.MIN_VALUE;

        String itemId = stack.getItem().getRegistryName().toString();
        for (ModConfig.CustomCompassConfig config : customCompasses) {
            if ( config.getItemId().equals(itemId)) {
                return config.getTargetDim();
            }
        }
        return Integer.MIN_VALUE;
    }

    private static BlockPos getDefaultTarget() {
        return null; // 不使用默认坐标
    }

    // 坐标写入NBT
    private void writeTargetToNBT(NBTTagCompound nbt, BlockPos pos) {
        nbt.setLong(NBT_TARGET_KEY, pos.toLong());
    }

    /**
     * 检查当前维度是否与罗盘目标维度相同
     *
     * @param world 当前世界
     * @param stack 罗盘物品堆栈
     * @return 如果维度匹配返回true，否则false
     */
    public static boolean isInTargetDimension(World world, ItemStack stack) {
        int targetDim = getTargetDimension(stack);
        // 如果罗盘未设置维度或当前世界为null，返回false
        if (targetDim == Integer.MIN_VALUE || world == null) {
            return false;
        }
        // 比较当前维度与目标维度
        return world.provider.getDimension() == targetDim;
    }

    // 更新坐标
    public static void updatePosition(ItemStack stack, BlockPos pos) {
        if (!stack.hasTagCompound()) {
            stack.setTagCompound(new NBTTagCompound());
        }

        NBTTagCompound nbt = stack.getTagCompound();
        nbt.setLong(NBT_TARGET_KEY, pos.toLong());
        stack.setTagCompound(nbt);
    }

    // 物品创建时初始化
    @Override
    public void onCreated(ItemStack stack, World world, EntityPlayer player) {
        initNBT(stack);
    }

    // 创造模式物品栏
    @Override
    public void getSubItems(CreativeTabs tab, NonNullList<ItemStack> items) {
        if (isInCreativeTab(tab)) {
            ItemStack stack = new ItemStack(this);
            initNBT(stack);
            items.add(stack);
        }
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void addInformation(ItemStack stack, @Nullable World world, List<String> tooltip, ITooltipFlag flag) {
        // 实时检测SHIFT键状态
        boolean isShiftDown = Keyboard.isKeyDown(Keyboard.KEY_LSHIFT) || Keyboard.isKeyDown(Keyboard.KEY_RSHIFT);

        // 动态引文显示逻辑
        if (isShiftDown) {
            // Using I18n.format for translatable quote and hint
            tooltip.add(I18n.format("item.star_compass.tooltip.quote_long"));
        } else {
            // Using I18n.format for short translatable quote
            tooltip.add(I18n.format("item.star_compass.tooltip.quote_short"));
        }

        // 自适应坐标显示
        BlockPos target = getTargetPosition(stack);
        int dim = getTargetDimension(stack); // 获取维度ID

        if (target != null) {
            String coords;
            if (isShiftDown) {
                // In coordinates, use I18n.format with placeholders for X, Y, Z, Dim
                coords = I18n.format("item.star_compass.tooltip.coords_set",
                        target.getX(), target.getY(), target.getZ(), dim);
            } else {
                // Use I18n.format for the "Hold SHIFT" prompt
                coords = I18n.format("item.star_compass.tooltip.hold_shift_for_status");
            }
            tooltip.add(coords);
        }

        if (target == null && isShiftDown){
            // Use I18n.format for "No target set" message
            tooltip.add(I18n.format("item.star_compass.tooltip.no_target_set"));
        }
    }

    @Override
    public String getItemStackDisplayName(ItemStack stack) {
        // Use I18n.format for the item name
        return I18n.format(this.getUnlocalizedName() + ".name");
    }

    @SideOnly(Side.CLIENT)
    public static void registerRenderer() {
        TileEntityItemStackRenderer.instance = new StarCompassItemRenderer();
    }

    // 添加静态初始化块注册 TEISR
    static {
        // 仅在客户端注册
        if (FMLCommonHandler.instance().getSide() == Side.CLIENT) {
            registerTEISR();
        }
    }

    @SideOnly(Side.CLIENT)
    private static void registerTEISR() {
        try {
            Field teisrField = Item.class.getDeclaredField("teisr");
            teisrField.setAccessible(true);
            teisrField.set(ItemStarCompass.INSTANCE, StarCompassItemRenderer.INSTANCE);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public boolean hasCustomEntity(ItemStack stack) {
        return true;
    }

    // 关键修改：保留原版丢弃逻辑
    @Override
    public boolean onEntityItemUpdate(EntityItem entityItem) {
        return false; // 必须返回false以允许正常更新
    }

    // 注册事件处理器
    public static void init() {
        MinecraftForge.EVENT_BUS.register(new EventHandler());
        MinecraftForge.EVENT_BUS.register(CompassViewLockHandler.class);
    }

    // 替换 createEntity 方法
    @Override
    public Entity createEntity(World world, Entity originalEntity, ItemStack itemstack) {
        // 创建新物品实体（深度复制物品堆栈）
        ItemStack newStack = itemstack.copy();
        newStack.setCount(1); // 确保只丢出1个物品

        EntityStarCompassItem newEntity = new EntityStarCompassItem(
                world,
                originalEntity.posX,
                originalEntity.posY,
                originalEntity.posZ,
                newStack // 使用复制的堆栈
        );

        newEntity.copyLocationAndAnglesFrom(originalEntity);
        newEntity.motionX = originalEntity.motionX;
        newEntity.motionY = originalEntity.motionY;
        newEntity.motionZ = originalEntity.motionZ;
        newEntity.setPickupDelay(40);
        return newEntity;
    }

    // 在 ItemStarCompass 类中添加以下内容
    public static class EventHandler {
        @SubscribeEvent
        public void onInteractWithFrame(PlayerInteractEvent.EntityInteract event) {
            if (event.getTarget() instanceof EntityItemFrame) {
                EntityPlayer player = event.getEntityPlayer();
                ItemStack heldItem = player.getHeldItem(event.getHand());

                if (heldItem.getItem() == ItemStarCompass.INSTANCE) {
                    event.setCanceled(true);

                    if (!event.getWorld().isRemote) {
                        // Using I18n.format for the translatable message
                        player.sendMessage(new TextComponentString(I18n.format("item.star_compass.message.item_frame_interaction")));
                    }
                }
            }
        }
    }
}