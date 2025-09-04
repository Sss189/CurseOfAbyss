package com.abyess.api;

import com.abyess.Network.PacketSyncLayerStatus;
import com.abyess.debuff.TurnIntoHollow;
import com.abyess.items.Scapegoat.CustomScapegoatManager;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.text.TextComponentString;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.ArrayList;
import java.util.List;

public class AbyessAPI {

    private static List<PacketSyncLayerStatus.LayerStatus> cachedLayerStatuses = new ArrayList<>();

    public static void updateCachedLayerStatuses(List<PacketSyncLayerStatus.LayerStatus> statuses) {
        if (statuses != null) {
            cachedLayerStatuses = new ArrayList<>(statuses);
        } else {
            cachedLayerStatuses.clear();
        }
    }
//       minieffect
    public static boolean isInCursedLayerWithValue(EntityPlayer player) {
        if (player == null) {
            return false;
        }

        int currentDimensionId = player.dimension;
        boolean foundCursedLayer = false;

        for (PacketSyncLayerStatus.LayerStatus status : cachedLayerStatuses) {
            if (status.dimensionId == currentDimensionId && status.value > 0) {
                foundCursedLayer = true;
                break;
            }
        }

        return foundCursedLayer;
    }

    public static boolean hasScapegoatItem(EntityPlayer player) {
        if (player == null) {
            return false;
        }

        boolean hasScapegoat = CustomScapegoatManager.hasAnyScapegoatItemInInventory(player);
        return hasScapegoat;
    }

//miniefffect

    @SideOnly(Side.CLIENT)
    public static boolean isPlayerHollow(EntityPlayer player) {
        return TurnIntoHollow.isPlayerHollow(player);
    }




}