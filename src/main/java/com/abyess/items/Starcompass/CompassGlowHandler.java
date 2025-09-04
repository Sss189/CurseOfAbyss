package com.abyess.items.Starcompass;

import com.abyess.config.ModConfig;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.nbt.NBTTagEnd;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraft.util.text.TextComponentString;

@Mod.EventBusSubscriber(modid = "abyess")
public class CompassGlowHandler {

    // 1) 缓存好“空的 enchList”和 HideFlags
    private static final NBTTagList GLOW_ENCH_LIST = new NBTTagList();
    private static final int HIDE_FLAG_BIT = 1;
    static {
        // 往列表里塞一个空 Compound，使 isItemEnchanted() == true
        GLOW_ENCH_LIST.appendTag(new NBTTagCompound());
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.side != Side.SERVER || event.phase != TickEvent.Phase.END) return;
        // 新增：配置里没有罗盘就直接退
        if (ModConfig.getConfigData().getCustomCompasses().isEmpty()) return;


        EntityPlayer player = event.player;
        int ticks = player.ticksExisted;



        // 实际扫描只做每 5 tick 一次
        if (ticks % 5 != 0) {
            return;
        }

        int currentDim = player.world.provider.getDimension();

        for (ModConfig.CustomCompassConfig cfg : ModConfig.getConfigData().getCustomCompasses()) {

            boolean inTargetDim = (currentDim == cfg.getTargetDim());

            scanAndApply(player, player.getHeldItemMainhand(), cfg, inTargetDim);
            scanAndApply(player, player.getHeldItemOffhand(),  cfg, inTargetDim);
            for (int slot = 0; slot < player.inventory.getSizeInventory(); slot++) {
                scanAndApply(player, player.inventory.getStackInSlot(slot), cfg, inTargetDim);
            }
        }
    }

    private static void scanAndApply(EntityPlayer player,
                                     ItemStack stack,
                                     ModConfig.CustomCompassConfig cfg,
                                     boolean inTargetDim) {
        if (stack.isEmpty()) return;
        if (!stack.getItem().getRegistryName().toString().equals(cfg.getItemId())) return;

        NBTTagCompound root = stack.hasTagCompound()
                ? stack.getTagCompound()
                : new NBTTagCompound();
        boolean changed = false;

        if (inTargetDim) {
            // 如果还没加过发光
            if (!root.hasKey("ench", 9) || root.getTagList("ench", 10).tagCount() == 0) {
                // 直接复用静态的 GLOW_ENCH_LIST
                root.setTag("ench", GLOW_ENCH_LIST);
                changed = true;
            }
            // 打 HideFlags bit0
            int hide = root.getInteger("HideFlags");
            if ((hide & HIDE_FLAG_BIT) == 0) {
                root.setInteger("HideFlags", hide | HIDE_FLAG_BIT);
                changed = true;
            }

        } else {
            // 离开目标维度，撤销
            if (root.hasKey("ench", 9)) {
                root.removeTag("ench");
                changed = true;
            }
            if (root.hasKey("HideFlags")) {
                int hide = root.getInteger("HideFlags") & ~HIDE_FLAG_BIT;
                if (hide == 0) root.removeTag("HideFlags");
                else         root.setInteger("HideFlags", hide);
                changed = true;
            }
            // 没剩任何 Tag 了就清掉整个 NBT
            if (root.hasNoTags()) {
                stack.setTagCompound(null);
                player.inventory.markDirty();
                player.openContainer.detectAndSendChanges();
                return;
            }
        }

        if (changed) {
            stack.setTagCompound(root);
            // 强制同步
            player.inventory.markDirty();
            player.openContainer.detectAndSendChanges();
        }
    }
}