package com.abyess.items.Scapegoat;

import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import net.minecraft.client.resources.I18n; // 导入 I18n 类
// Removed TextComponentString, TextComponentTranslation, ITextComponent as they are no longer needed without debug messages

import javax.annotation.Nullable;
import java.util.List;

public class ItemScapegoat extends Item {

    public static final ItemScapegoat INSTANCE = new ItemScapegoat();

    public ItemScapegoat() {
        setMaxStackSize(1);
        setMaxDamage(100);
        setNoRepair();
        setRegistryName("curseofabyss", "scapegoat");
        setUnlocalizedName("curseofabyss.scapegoat"); // 非本地化名称作为语言文件的键
        setCreativeTab(CreativeTabs.TOOLS);
    }

    /**
     * 核心方法：累积伤害（公开以便自定义管理器调用）
     */
    public static void accumulateDamage(EntityPlayer player, ItemStack stack, float amount) {
        NBTTagCompound nbt = stack.getTagCompound();
        if (nbt == null) {
            nbt = new NBTTagCompound();
            stack.setTagCompound(nbt);
        }

        float pending = nbt.getFloat("pendingDamage");
        pending += amount;

        int damageIncrement = (int) pending;
        if (damageIncrement > 0) {
            int newDamage = stack.getItemDamage() + damageIncrement;
            stack.setItemDamage(newDamage);
            pending -= damageIncrement;

            if (stack.getItemDamage() >= stack.getMaxDamage()) {
                handleItemBreak(player, stack);
            }
        }

        nbt.setFloat("pendingDamage", pending);
    }

    private static void handleItemBreak(EntityPlayer player, ItemStack stack) {
        boolean wasLast = stack.getCount() == 1;
        stack.shrink(1);

        if (!player.world.isRemote && wasLast) {
            ItemStack mutton = new ItemStack(Items.COOKED_MUTTON);
            if (!player.addItemStackToInventory(mutton)) {
                player.dropItem(mutton, false);
            }
        }
    }

    @Override
    public boolean shouldCauseReequipAnimation(ItemStack oldStack, ItemStack newStack, boolean slotChanged) {
        return !ItemStack.areItemStackTagsEqual(oldStack, newStack);
    }

    @Override
    public NBTTagCompound getNBTShareTag(ItemStack stack) {
        NBTTagCompound tag = stack.getTagCompound();
        return tag != null ? tag.copy() : new NBTTagCompound();
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void addInformation(ItemStack stack, @Nullable World world, List<String> tooltip, ITooltipFlag flag) {
        int maxDamage = stack.getMaxDamage();
        int currentDamage = stack.getItemDamage();
        int remaining = maxDamage - currentDamage;
        double percent = (remaining / (double) maxDamage) * 100.0;

        String statusKey;

        if (percent <= 25) {
            statusKey = "item.curseofabyss.scapegoat.tooltip.status_low_durability";
        } else if (percent <= 50) {
            statusKey = "item.curseofabyss.scapegoat.tooltip.status_medium_durability";
        } else {
            statusKey = "item.curseofabyss.scapegoat.tooltip.status_high_durability";
        }

        tooltip.add(I18n.format(statusKey, remaining, maxDamage)); // Pass remaining and maxDamage for formatting in the lang file
    }
}