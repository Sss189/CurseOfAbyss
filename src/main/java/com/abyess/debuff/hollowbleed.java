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
public class hollowbleed {




        private static final Map<EntityPlayer, int[]> activeEffects = new HashMap<>();



        public hollowbleed() {
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

            // 调整参数
            double spreadRange = 0.2 * Math.sqrt(intensity); // 范围优化
            int particleCount = 2 + intensity * 2; // 适当减少基础数量

            // REDSTONE粒子生成
            for (int i = 0; i < particleCount; i++) {
                double angle = rand.nextDouble() * Math.PI * 2;
                double radius = rand.nextDouble() * spreadRange;
                double x = player.posX + Math.cos(angle) * radius;
                double z = player.posZ + Math.sin(angle) * radius;
                double y = player.posY + 0.1 + (rand.nextDouble() - 0.5) * 0.15;

                worldServer.spawnParticle(
                        EnumParticleTypes.REDSTONE,
                        x, y, z,
                        1,
                        0,
                        -0.02 * intensity, // 下坠速度
                        0,
                        0.0,
                        (255 << 16) | (0 << 8) | 0
                );
            }

            // DRIP_LAVA调整
            if (intensity > 2) {
                int dripCount = intensity * 2;
                for (int i = 0; i < dripCount; i++) {
                    double x = player.posX + (rand.nextDouble() - 0.5) * 0.4; // 缩小水平扩散
                    double y = player.posY + 0.05 + rand.nextDouble() * 0.25; // 更低的高度区间
                    double z = player.posZ + (rand.nextDouble() - 0.5) * 0.4;

                    worldServer.spawnParticle(
                            EnumParticleTypes.DRIP_LAVA,
                            x, y, z,
                            1,
                            (rand.nextDouble() - 0.5) * 0.01, // 减少水平初速度
                            -0.2, // 加快下坠速度
                            (rand.nextDouble() - 0.5) * 0.01,
                            0.0
                    );
                }
            }
        }
    }



