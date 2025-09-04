package com.abyess.debuff;



import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.math.MathHelper;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.relauncher.Side;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

public class ConfusionDebuffHandler {
    private static final Map<EntityPlayer, int[]> activeDebuffs = new HashMap<>();
    private static final float BASE_INTENSITY = 1.5f; // 增大强度

    public ConfusionDebuffHandler() {
        MinecraftForge.EVENT_BUS.register(this);
    }

    public static void applyConfusion(EntityPlayer player, int durationSeconds) {
        if (player == null || player.world.isRemote) return;
        int totalTicks = durationSeconds * 20;
        activeDebuffs.put(player, new int[]{totalTicks, totalTicks});
     //   System.out.println("[SERVER] 混乱效果应用于: " + player.getName());
    }

    @SubscribeEvent
    public void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase == TickEvent.Phase.END && event.side == Side.SERVER) { // 改为END阶段
            EntityPlayer player = event.player;
            if (activeDebuffs.containsKey(player)) {
                player.moveForward = 0;
                player.moveStrafing = 0;
                handleServerTick(player);
            }
        }
    }

    private void handleServerTick(EntityPlayer player) {
        if (player.isDead || player.isRiding()) {
            activeDebuffs.remove(player);
            return;
        }

        int[] ticksData = activeDebuffs.get(player);
        int remainingTicks = ticksData[0];
        if (remainingTicks > 0) {
            Random rand = player.getRNG();

            double dx = rand.nextGaussian();
            double dz = rand.nextGaussian();
            double length = Math.sqrt(dx*dx + dz*dz);
            if (length != 0) {
                dx /= length;
                dz /= length;
            }

            double force = BASE_INTENSITY * 0.15; // 增大作用力
            player.motionX += dx * force;
            player.motionZ += dz * force;
            player.velocityChanged = true; // 确保同步

            double maxSpeed = 0.8; // 提高速度上限
            double currentSpeed = Math.sqrt(player.motionX*player.motionX + player.motionZ*player.motionZ);
            if (currentSpeed > maxSpeed) {
                player.motionX *= maxSpeed / currentSpeed;
                player.motionZ *= maxSpeed / currentSpeed;
                player.velocityChanged = true;
            }

            ticksData[0]--;
        } else {
            activeDebuffs.remove(player);
        //    System.out.println("[SERVER] 混乱效果移除: " + player.getName());
        }
    }

}