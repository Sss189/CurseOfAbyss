package com.abyess.tracker;

import com.abyess.Network.NetworkHandler;
import com.abyess.Network.PacketApplyFlatEffect;
import com.abyess.config.ModConfig;
import com.abyess.debuff.BleedEffectHandler;
import com.abyess.debuff.ConfusionDebuffHandler;
import com.abyess.debuff.TurnIntoHollow;
import com.abyess.render.InvertedColorFilter;
import com.abyess.render.OverlappingBlurFilter;
import com.abyess.render.RedFilter;
import com.abyess.shaders.CustomShaderLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.FlatEntities;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.MobEffects;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;
import net.minecraft.server.MinecraftServer;

import java.util.HashMap;
import java.util.Map;

public class CurseApplication {

    @FunctionalInterface
    public interface FilterEffect {
        void start(int duration, boolean isDecayEnabled);
    }

    public static class FilterRegistry {
        private static final Map<String, FilterEffect> filters = new HashMap<>();

        static {
            filters.put("inverted", (duration, isDecayEnabled) -> InvertedColorFilter.activateFilter(duration));
            filters.put("overlappingBlur", OverlappingBlurFilter::startEffect);
            filters.put("red", RedFilter::activate);
        }

        public static FilterEffect getFilter(String id) {
            return filters.get(id);
        }
    }

    public static void applyCurse(EntityPlayer player, ModConfig.CurseConfig curseConfig) {
        switch (curseConfig.getType()) {
            case "potion":
                applyPotionCurse(player, curseConfig);
                break;
            case "filter":
                applyFilterCurse(player, curseConfig);
                break;
            case "other":
                applyOtherCurse(player, curseConfig);
                break;
            case "super_secret_setting":
                applySuperSecretSetting(player, curseConfig);
                break;
            default:
                throw new IllegalArgumentException("Unknown curse type: " + curseConfig.getType());
        }
    }

    private static void applyPotionCurse(EntityPlayer player, ModConfig.CurseConfig curseConfig) {
        Potion potion = Potion.getPotionFromResourceLocation(curseConfig.getName());
        if (potion == null) {
            System.err.println("Could not find potion effect: " + curseConfig.getName());
            return;
        }

        int duration = curseConfig.getDuration() * 20;
        int amplifier = curseConfig.getAmplifier();

        player.addPotionEffect(new PotionEffect(
                potion,
                duration,
                amplifier,
                false,
                false
        ));
    }

    private static void applyFilterCurse(EntityPlayer player, ModConfig.CurseConfig curseConfig) {
        String filterId = curseConfig.getName();
        int duration = curseConfig.getDuration() * 20;
        boolean isDecayEnabled = curseConfig.isBooleanValue();

        FilterEffect filter = FilterRegistry.getFilter(filterId);
        if (filter != null) {
            filter.start(duration, isDecayEnabled);
        } else {
            System.err.println("Unknown filter ID: " + filterId);
        }
    }


    private static void applyOtherCurse(EntityPlayer player, ModConfig.CurseConfig curseConfig) {
        String effectId = curseConfig.getName();

        switch (effectId.toLowerCase()) {
            case "hollow":
                int hollowDuration = curseConfig.getDuration() * 20;
                boolean enableBleedingEffect = curseConfig.isBooleanValue();
                TurnIntoHollow.activateHollowModel(hollowDuration, enableBleedingEffect);
                break;

            case "confusion":
                int confusionDuration = curseConfig.getDuration();
                ConfusionDebuffHandler.applyConfusion(player, confusionDuration);
                break;

            case "bleeding":
                int bleedingDuration = curseConfig.getDuration();
                int intensity = curseConfig.getAmplifier();
                BleedEffectHandler.applyBleedingEffect(player, bleedingDuration, intensity);
                break;

            case "2dentities":
                int flatDuration = curseConfig.getDuration() * 20;
                if (!player.world.isRemote && player instanceof EntityPlayerMP) {
                    NetworkHandler.sendFlatEffectPacket((EntityPlayerMP) player, flatDuration);
                }
                break;

            case "custom_command":
                String command = curseConfig.getCommand();
                if (command != null && !command.isEmpty()) {
                    if (!player.world.isRemote && player instanceof EntityPlayerMP) {
                        // Server-side execution
                        MinecraftServer server = player.getServer();
                        if (server != null) {
                            if (command.startsWith("/")) {
                                command = command.substring(1);
                            }
                            server.getCommandManager().executeCommand(player, command);
                        }
                    } else if (player.world.isRemote) {
                        // Client-side execution
                        // For a client-side player, use Minecraft.getMinecraft().player.sendChatMessage()
                        // This will send the command to the server for processing.
                        Minecraft.getMinecraft().player.sendChatMessage(command);
                    }
                }
                break;
            default:
                System.out.println("Applying custom curse: " + effectId);
                break;
        }
    }


    private static void applySuperSecretSetting(EntityPlayer player, ModConfig.CurseConfig curseConfig) {
        String shaderId = curseConfig.getName();
        int duration = curseConfig.getDuration() * 20;

        if (shaderId == null || shaderId.isEmpty()) {
            System.err.println("[CurseApplication] No shader ID specified, cannot apply super secret setting.");
            return;
        }

        try {
            CustomShaderLoader.INSTANCE.activateShader(shaderId, duration);

        } catch (Exception e) {
            System.err.println("[CurseApplication] Error activating super secret setting: " + shaderId);
            e.printStackTrace();
        }
    }
}