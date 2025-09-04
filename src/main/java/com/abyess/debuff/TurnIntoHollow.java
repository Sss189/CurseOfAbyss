
        package com.abyess.debuff;

import com.abyess.Hollow.EntityHollow;
import com.abyess.Hollow.ModelHollow;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.inventory.GuiInventory;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.entity.RenderLivingBase;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.SoundEvents;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraftforge.client.event.GuiOpenEvent;
import net.minecraftforge.client.event.InputUpdateEvent;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.client.event.RenderHandEvent;
import net.minecraftforge.client.event.RenderPlayerEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import net.minecraftforge.event.world.WorldEvent;

import com.abyess.config.ModConfig;

public class TurnIntoHollow {

    // ====================== Basic Configuration ====================== //
    private static final ResourceLocation HOLLOW_TEXTURE =
            new ResourceLocation("curseofabyss", "textures/entity/hollow.png");
    private static int activeUntilTick = 0;          // Timer based on game ticks
    private static final CustomHollowRenderer HOLLOW_RENDERER =
            new CustomHollowRenderer(Minecraft.getMinecraft().getRenderManager());

    // ====================== Squish System Parameters ====================== //
    private static final float JUMP_SQUISH = 0.7F;
    private static final float LAND_SQUISH = 0.6F;
    private static final float SQUISH_RECOVERY = 1.6F; // Unit: seconds -> converted to game ticks
    private static final float MIN_SQUISH = 0.6F;
    private static float squishFactor = 1.0F;
    private static int squishStartTick = 0;        // Squish timer based on game ticks
    private static boolean wasOnGround = false;

    // ====================== Movement Control Parameters ====================== //
    private static final float HORIZONTAL_FACTOR = 0.4F;

    // ====================== Initialization ====================== //
    public static void init() {
        MinecraftForge.EVENT_BUS.register(TurnIntoHollow.class);
    }

    // ====================== Additional Configuration (Unchanged) ====================== //
    private static int bleedingDurationSeconds = 9; // Duration of bleeding effect in seconds

    // ====================== Custom Renderer (Unchanged) ====================== //
    private static class CustomHollowRenderer extends RenderLivingBase<EntityHollow> {
        public CustomHollowRenderer(RenderManager renderManager) {
            super(renderManager, new ModelHollow(), 0.25F);
            this.shadowSize = 0.3F;
        }

        @Override
        protected void preRenderCallback(EntityHollow entity, float partialTicks) {
            float horizontalScale = 1.0F + (1.0F - squishFactor) * 0.6F;
            GlStateManager.scale(horizontalScale, squishFactor, horizontalScale);
            ((ModelHollow) this.mainModel).setForLevelModel(true);
        }

        @Override
        protected boolean canRenderName(EntityHollow entity) {
            return false;
        }

        @Override
        protected ResourceLocation getEntityTexture(EntityHollow entity) {
            return HOLLOW_TEXTURE;
        }
    }

    // ====================== Main Render Logic (Unchanged) ====================== //
    @SubscribeEvent
    @SideOnly(Side.CLIENT)
    public static void onPlayerRender(RenderPlayerEvent.Pre event) {
        EntityPlayer player = event.getEntityPlayer();
        if (!shouldProcess(player)) return;

        event.setCanceled(true);
        GlStateManager.pushMatrix();

        RenderManager rm = Minecraft.getMinecraft().getRenderManager();
        double x = interpolate(player.prevPosX, player.posX, event.getPartialRenderTick());
        double y = interpolate(player.prevPosY, player.posY, event.getPartialRenderTick());
        double z = interpolate(player.prevPosZ, player.posZ, event.getPartialRenderTick());

        GlStateManager.translate(
                x - rm.viewerPosX,
                y - rm.viewerPosY,
                z - rm.viewerPosZ
        );
        GlStateManager.rotate(180 - player.renderYawOffset, 0, 1, 0);

        if (player.isSneaking()) {
            GlStateManager.translate(0, 0.15F, 0);
        }

        HOLLOW_RENDERER.doRender(
                createDummyHollow(player, event.getPartialRenderTick()),
                0, 0, 0,
                interpolateRotation(player.prevRotationYawHead, player.rotationYawHead, event.getPartialRenderTick()),
                event.getPartialRenderTick()
        );

        GlStateManager.popMatrix();
    }

    // ====================== Squish Animation System (Unchanged) ====================== //
    private static void handleSquishAnimation(EntityPlayer player) {
        World world = player.world;
        boolean isJumping = Minecraft.getMinecraft().gameSettings.keyBindJump.isKeyDown();
        boolean isOnGround = player.onGround;

        if (isJumping && isOnGround) {
            squishFactor = JUMP_SQUISH;
            squishStartTick = (int) world.getTotalWorldTime();
        }

        if (!wasOnGround && isOnGround) {
            squishFactor = LAND_SQUISH;
            squishStartTick = (int) world.getTotalWorldTime();
        }
        wasOnGround = isOnGround;

        updateSquishFactor(world);
    }

    private static void updateSquishFactor(World world) {
        int currentTick = (int) world.getTotalWorldTime();
        int elapsedTicks = currentTick - squishStartTick;
        float progress = MathHelper.clamp(elapsedTicks / (SQUISH_RECOVERY * 20), 0, 1);
        float ease = MathHelper.sin(progress * (float) Math.PI / 2);
        squishFactor = squishFactor + (1.0F - squishFactor) * ease;
        squishFactor = Math.max(squishFactor, MIN_SQUISH);
    }

    // ====================== Movement Control System ====================== //
    @SubscribeEvent
    @SideOnly(Side.CLIENT)
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.START) return;

        EntityPlayer player = event.player;

        if (player.isDead || player.getHealth() <= 0.0F) {
            activeUntilTick = 0;
            squishFactor = 1.0F;
            float defaultEyeHeight = player.getDefaultEyeHeight();
            if (player.eyeHeight != defaultEyeHeight) {
                player.eyeHeight = defaultEyeHeight;
            }
            player.capabilities.setPlayerWalkSpeed(0.1F);
            return;
        }

        boolean isHollow = shouldProcess(player);

        if (isHollow) {
            handleSquishAnimation(player);

            // Close inventory if configured to be disabled in Hollow form
            if (ModConfig.getConfigData().isDisableInventoryInHollow() && player.openContainer != player.inventoryContainer) {
                player.closeScreen();
            }

            if (player.onGround) {
                player.motionX = 0;
                player.motionZ = 0;
            }

            player.capabilities.isFlying = false;
            player.capabilities.setPlayerWalkSpeed(0);
            player.setSprinting(false);

            handleSlimeJump(player); // Uses JUMP_POWER from config

            player.eyeHeight = 0.4375F;
        } else {
            float defaultEyeHeight = player.getDefaultEyeHeight();
            if (player.eyeHeight != defaultEyeHeight) {
                player.eyeHeight = defaultEyeHeight;
            }
            player.capabilities.setPlayerWalkSpeed(0.1F);
        }
    }


private static void handleSlimeJump(EntityPlayer player) {
    if (player.onGround && Minecraft.getMinecraft().gameSettings.keyBindJump.isKeyDown()) {
        Vec3d look = player.getLookVec().normalize();
        float jumpPower = ModConfig.getConfigData().getHollowJumpPower();

        // 直接设置玩家的跳跃速度，不再调用 player.jump()
        player.motionY = jumpPower; // 使用 jumpPower 即可，乘数可以根据实际效果调整
        player.motionX = look.x * HORIZONTAL_FACTOR * 1.5F;
        player.motionZ = look.z * HORIZONTAL_FACTOR * 1.5F;

        // 确保玩家离开地面，防止立即再次触发 onGround
        player.onGround = false; // 模拟跳跃发生

        spawnSlimeParticles(player);
        player.playSound(SoundEvents.ENTITY_SLIME_JUMP, 0.8F,
                1.0F + (player.world.rand.nextFloat() - 0.5F) * 0.2F);
    }
}
    // ====================== Helper Methods (Unchanged) ====================== //
    @SideOnly(Side.CLIENT)
    private static boolean shouldProcess(EntityPlayer player) {
        World world = Minecraft.getMinecraft().world;
        return player == Minecraft.getMinecraft().player
                && world != null
                && world.getTotalWorldTime() <= activeUntilTick;
    }
  //  -------------API
    @SideOnly(Side.CLIENT)
    public static boolean isPlayerHollow(EntityPlayer player) {
        return player != null && shouldProcess(player);
    }

    private static EntityHollow createDummyHollow(EntityPlayer player, float partialTicks) {
        EntityHollow dummy = new EntityHollow(player.world);
        dummy.setPosition(player.posX, player.posY, player.posZ);
        dummy.limbSwing = player.limbSwing;
        dummy.limbSwingAmount = player.limbSwingAmount;
        dummy.onGround = player.onGround;
        dummy.motionX = player.motionX;
        dummy.motionY = player.motionY;
        dummy.motionZ = player.motionZ;
        dummy.rotationYaw = interpolateRotation(player.prevRotationYaw, player.rotationYaw, partialTicks);
        dummy.rotationYawHead = interpolateRotation(player.prevRotationYawHead, player.rotationYawHead, partialTicks);
        dummy.rotationPitch = interpolateRotation(player.prevRotationPitch, player.rotationPitch, partialTicks);
        return dummy;
    }

    private static void spawnSlimeParticles(EntityPlayer player) {
        if (player.world.isRemote) {
            Vec3d pos = player.getPositionVector().addVector(0, 0.1, 0);
            for (int i = 0; i < 15; ++i) {
                player.world.spawnParticle(
                        EnumParticleTypes.REDSTONE,
                        pos.x + (player.world.rand.nextDouble() - 0.5) * 0.8,
                        pos.y,
                        pos.z + (player.world.rand.nextDouble() - 0.5) * 0.8,
                        0, 0.1 + player.world.rand.nextDouble() * 0.2, 0
                );
            }
        }
    }

    // ====================== Input Control (Unchanged) ====================== //
    @SubscribeEvent
    @SideOnly(Side.CLIENT)
    public static void onInput(InputUpdateEvent event) {
        if (shouldProcess(event.getEntityPlayer())) {
            if (event.getEntityPlayer().onGround) {
                event.getMovementInput().moveStrafe = 0;
                event.getMovementInput().moveForward = 0;
            }
            event.getMovementInput().jump = false;
        }
    }

    // ====================== GUI Control ====================== //
    @SubscribeEvent
    @SideOnly(Side.CLIENT)
    public static void onRenderHand(RenderHandEvent event) {
        if (shouldProcess(Minecraft.getMinecraft().player)) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    @SideOnly(Side.CLIENT)
    public static void onGuiOpen(GuiOpenEvent event) {
        if (shouldProcess(Minecraft.getMinecraft().player)
                && event.getGui() instanceof GuiInventory
                && ModConfig.getConfigData().isDisableInventoryInHollow()) { // Now checks config setting
            event.setCanceled(true);
        }
    }


    // ====================== World Event Handling (Unchanged) ====================== //
    @SubscribeEvent
    @SideOnly(Side.CLIENT)
    public static void onWorldUnload(WorldEvent.Unload event) {
        activeUntilTick = 0;
        squishFactor = 1.0F;
    }

    // ====================== Math Utilities (Unchanged) ====================== //
    private static double interpolate(double prev, double current, float partial) {
        return prev + (current - prev) * partial;
    }

    private static float interpolateRotation(float prev, float current, float partial) {
        return prev + (current - prev) * partial;
    }

    // ====================== External Control Interface (Unchanged) ====================== //
    public static void activateHollowModel(int durationTicks, boolean enableBleedingEffect) {
        World world = Minecraft.getMinecraft().world;
        if (world != null) {
            activeUntilTick = (int) world.getTotalWorldTime() + durationTicks;
        }

        if (enableBleedingEffect) {
            EntityPlayer player = Minecraft.getMinecraft().player;
            if (player != null) {
                // Assuming hollowbleed is a defined class or method somewhere else
                 hollowbleed.applyBleedingEffect(player, bleedingDurationSeconds, 3);
            }
        }
    }
}
