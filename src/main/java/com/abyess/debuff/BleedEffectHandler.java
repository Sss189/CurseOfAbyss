package com.abyess.debuff;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.relauncher.Side;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

public class BleedEffectHandler {
    private static final Map<EntityPlayer, int[]> activeEffects = new HashMap<>();



    public BleedEffectHandler() {
        // 注册事件
        MinecraftForge.EVENT_BUS.register(this);
    }


    public static void applyBleedingEffect(EntityPlayer player, int durationSeconds, int intensity) {
        activeEffects.put(player, new int[]{
                durationSeconds * 20,
                MathHelper.clamp(intensity, 1, 5)
        });
    }

    // 改为在服务端处理
    @SubscribeEvent
    public void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase == TickEvent.Phase.END && event.side == Side.SERVER) {
            EntityPlayer player = event.player;

            if (activeEffects.containsKey(player)) {
                int[] params = activeEffects.get(player);
                int remainingTicks = params[0];
                int intensity = params[1];

                if (remainingTicks > 0) {
                    spawnParticles(player, intensity); // 服务端生成粒子
                    activeEffects.put(player, new int[]{remainingTicks - 1, intensity});
                } else {
                    activeEffects.remove(player);
                }
            }
        }
    }

    private static void spawnParticles(EntityPlayer player, int intensity) {
        World world = player.world;
        if (!(world instanceof WorldServer)) return;
        WorldServer worldServer = (WorldServer) world;
        Random rand = world.rand;

        // 保持原有扩散参数不变
        double horizontalSpread = 0.3 * intensity;
        double verticalSpread = player.height * 0.8;

        // 调整粒子数量（原数量的1/3）
        int particleCount = 1 + intensity;  // 原为 3 + intensity*3

        // REDSTONE粒子生成
        for (int i = 0; i < particleCount; i++) {
            // 保持位置计算方式不变
            double x = player.posX + (rand.nextDouble() - 0.5) * horizontalSpread;
            double y = player.posY + rand.nextDouble() * verticalSpread;
            double z = player.posZ + (rand.nextDouble() - 0.5) * horizontalSpread;

            worldServer.spawnParticle(
                    EnumParticleTypes.REDSTONE,
                    x, y, z,
                    1,
                    0,
                    -0.03 * intensity,
                    0,
                    0.0,
                    (255 << 16) | (0 << 8) | 0
            );
        }

        // DRIP_LAVA调整（触发条件改为强度>2）
        if (intensity > 2) {  // 原为 intensity > 1
            // 数量调整为强度值本身（原为 intensity*3）
            int dripCount = intensity;

            for (int i = 0; i < dripCount; i++) {
                // 保持位置计算方式不变
                double x = player.posX + (rand.nextDouble() - 0.5) * 0.6;
                double y = player.posY + rand.nextDouble() * verticalSpread;
                double z = player.posZ + (rand.nextDouble() - 0.5) * 0.6;

                worldServer.spawnParticle(
                        EnumParticleTypes.DRIP_LAVA,
                        x, y, z,
                        1,
                        (rand.nextDouble() - 0.5) * 0.02,
                        -0.25 - rand.nextDouble() * 0.15,
                        (rand.nextDouble() - 0.5) * 0.02,
                        0.0
                );
            }
        }
    }
}