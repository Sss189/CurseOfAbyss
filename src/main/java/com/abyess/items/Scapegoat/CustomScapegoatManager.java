// CustomScapegoatManager.java

package com.abyess.items.Scapegoat;

import com.abyess.config.ModConfig;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.TextComponentString;
import net.minecraftforge.fml.common.registry.ForgeRegistries;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class CustomScapegoatManager {

    private static final Map<Integer, Set<String>> dimensionItemMap = new HashMap<>();

    public static void initialize() {
        dimensionItemMap.clear();
        List<ModConfig.CustomScapegoatItem> customItems = ModConfig.getConfigData().getCustomScapegoatItems();

        if (customItems != null && !customItems.isEmpty()) {
            for (ModConfig.CustomScapegoatItem item : customItems) {
                String itemId = item.getItemId();
                int dimensionId = item.getDimensionId();

                if (itemId == null || !itemId.contains(":")) {
                    continue;
                }

                Item targetItem = ForgeRegistries.ITEMS.getValue(new ResourceLocation(itemId));
                if (targetItem == null) {
                    continue;
                }

                dimensionItemMap
                        .computeIfAbsent(dimensionId, k -> new HashSet<>())
                        .add(itemId);
            }
        }
    }

    public static boolean isScapegoatItem(EntityPlayer player, ItemStack stack) {
        int currentDimension = player.dimension;

        ModConfig.DimensionConfig dimConfig = ModConfig.getConfigData().getDimensions().get(currentDimension);
        boolean dimensionHasConfiguredCurses = false;
        if (dimConfig != null && dimConfig.isEnabled()) {
            List<ModConfig.LayerConfig> layers = dimConfig.getLayers();
            if (layers != null && !layers.isEmpty()) {
                for (ModConfig.LayerConfig layer : layers) {
                    if (layer.isEnabled()) {
                        dimensionHasConfiguredCurses = true;
                        break;
                    }
                }
            }
        }

        if (!dimensionHasConfiguredCurses) {
            return false;
        }

        if (stack.getItem() instanceof ItemScapegoat) {
            return true;
        }

        if (!dimensionItemMap.containsKey(currentDimension)) {
            return false;
        }

        ResourceLocation itemId = stack.getItem().getRegistryName();
        if (itemId == null) return false;

        return dimensionItemMap.get(currentDimension).contains(itemId.toString());
    }

    /**
     * 判断玩家背包中是否存在任何替罪羊物品（包括自定义的和默认的）。
     * 此方法会遍历玩家背包，并使用 isScapegoatItem 方法判断每个物品。
     *
     * @param player 目标玩家。
     * @return 如果玩家背包中存在替罪羊物品，则返回 true；否则返回 false。
     */
    public static boolean hasAnyScapegoatItemInInventory(EntityPlayer player) {
        if (player == null) {
            return false;
        }

        for (int i = 0; i < player.inventory.getSizeInventory(); i++) {
            ItemStack stack = player.inventory.getStackInSlot(i);
            if (!stack.isEmpty()) {
                if (isScapegoatItem(player, stack)) { // 重用现有的 isScapegoatItem 逻辑
                    return true; // 找到一个就立即返回
                }
            }
        }
        return false; // 遍历结束，没有找到
    }


    public static boolean tryAbsorbCurse(EntityPlayer player, int layerDurabilityCost, String layerName) {
        for (int i = 0; i < player.inventory.getSizeInventory(); i++) {
            ItemStack stack = player.inventory.getStackInSlot(i);
            if (stack.isEmpty()) continue;

            if (isScapegoatItem(player, stack)) {
                if (stack.getItem() instanceof ItemScapegoat) {
                    ItemScapegoat.accumulateDamage(player, stack, layerDurabilityCost);
                    return true;
                }

                if (handleCustomScapegoat(player, stack, layerDurabilityCost, layerName)) {
                    return true;
                }
            }
        }
        return false;
    }

    private static boolean handleCustomScapegoat(EntityPlayer player, ItemStack stack,
                                                 int layerDurabilityCost, String layerName) {
        String itemName = stack.getDisplayName();

        if (!stack.isItemStackDamageable()) {
            return true;
        }

        int actualCost = layerDurabilityCost;
        int newDamage = stack.getItemDamage() + actualCost;

        if (newDamage >= stack.getMaxDamage()) {
            stack.shrink(1);
        } else {
            stack.setItemDamage(newDamage);
        }
        return true;
    }

    private static void notifyPlayer(EntityPlayer player, String message) {
        if (!player.world.isRemote) {
            player.sendMessage(new TextComponentString(message));
        }
    }

    public static Set<String> getScapegoatItemsForDimension(int dimension) {
        return dimensionItemMap.getOrDefault(dimension, new HashSet<>());
    }
}