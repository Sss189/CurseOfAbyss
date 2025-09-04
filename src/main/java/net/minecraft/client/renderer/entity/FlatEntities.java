package net.minecraft.client.renderer.entity;

import com.abyess.config.ModConfig; // 确保您的 ModConfig 存在且路径正确
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraftforge.client.event.RenderLivingEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;


@EventBusSubscriber
public class FlatEntities
{
    // === 效果控制字段和参数 ===
    private static int effectTicksRemaining = 0;
    private static boolean flatteningEnabled = true;
    private static final float BASE_SIZE = 0.2F;
    private static final float MIN_SCALE = 1F;
    private static final float MAX_SCALE = 100.0F;

    // === 效果控制方法 ===
    public static void applyEffect(int durationTicks) {
        effectTicksRemaining = durationTicks;
    }
    public static void stopEffect() {
        effectTicksRemaining = 0;
    }
    public static void setFlatteningEnabled(boolean enabled) {
        flatteningEnabled = enabled;
    }
    public static int getRemainingTicks() {
        return effectTicksRemaining;
    }

    // === 客户端 Tick 事件 ===
    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (effectTicksRemaining > 0) {
            effectTicksRemaining--;
        }
    }

    // === 核心：渲染事件处理 ===
    @SubscribeEvent
    public static void onRenderLivingPre(RenderLivingEvent.Pre<EntityLivingBase> event)
    {
        boolean applyFlattening = flatteningEnabled && effectTicksRemaining > 0;
        boolean applyScaling = ModConfig.getConfigData().isEnable2DEntityDistanceScaling() && effectTicksRemaining > 0;

        if (!applyFlattening && !applyScaling) {
            return;
        }

        EntityLivingBase entity = event.getEntity();
        RenderLivingBase<EntityLivingBase> renderer = event.getRenderer();
        float partialTicks = event.getPartialRenderTick();
        double x = event.getX();
        double y = event.getY();
        double z = event.getZ();

        event.setCanceled(true);

        GlStateManager.pushMatrix();
        GlStateManager.disableCull();
        renderer.mainModel.swingProgress = renderer.getSwingProgress(entity, partialTicks);
        boolean shouldSit = entity.isRiding() && (entity.getRidingEntity() != null && entity.getRidingEntity().shouldRiderSit());
        renderer.mainModel.isRiding = shouldSit;
        renderer.mainModel.isChild = entity.isChild();

        try
        {
            float f = renderer.interpolateRotation(entity.prevRenderYawOffset, entity.renderYawOffset, partialTicks);
            float f1 = renderer.interpolateRotation(entity.prevRotationYawHead, entity.rotationYawHead, partialTicks);
            float f2 = f1 - f;

            if(shouldSit && entity.getRidingEntity() instanceof EntityLivingBase)
            {
                EntityLivingBase entitylivingbase = (EntityLivingBase)entity.getRidingEntity();
                f = renderer.interpolateRotation(entitylivingbase.prevRenderYawOffset, entitylivingbase.renderYawOffset, partialTicks);
                f2 = f1 - f;
                float f3 = MathHelper.wrapDegrees(f2);
                if(f3 < -85.0F) f3 = -85.0F;
                if(f3 >= 85.0F) f3 = 85.0F;
                f = f1 - f3;
                if(f3 * f3 > 2500.0F) f += f3 * 0.2F;
                f2 = f1 - f;
            }

            float f7 = entity.prevRotationPitch + (entity.rotationPitch - entity.prevRotationPitch) * partialTicks;
            renderer.renderLivingAt(entity, x, y, z);
            float f8 = renderer.handleRotationFloat(entity, partialTicks);

            GlStateManager.pushMatrix();

            if (applyFlattening) {
                prepareFlatRender(x, z, f);
            } else {
                renderer.applyRotations(entity, f8, f, partialTicks);
            }

            float sizeFactor = 1.0F;
            if (applyScaling) {
                Entity viewEntity = Minecraft.getMinecraft().getRenderManager().renderViewEntity;
                if (viewEntity == null) viewEntity = Minecraft.getMinecraft().player;

                if (viewEntity != null) {
                    Vec3d eyePos = viewEntity.getPositionEyes(partialTicks);
                    Vec3d center = new Vec3d(entity.posX, entity.posY + entity.getEyeHeight() * 0.7, entity.posZ);
                    Vec3d camLook = viewEntity.getLook(partialTicks).normalize();
                    double projDist = center.subtract(eyePos).dotProduct(camLook);
                    double effDist = Math.max(Math.abs(projDist), 0.1);
                    sizeFactor = (float)(BASE_SIZE * effDist);
                    sizeFactor = MathHelper.clamp(sizeFactor, MIN_SCALE, MAX_SCALE);
                }
            }
            GlStateManager.scale(sizeFactor, sizeFactor, sizeFactor);

            float f4 = renderer.prepareScale(entity, partialTicks);
            float f5 = 0.0F, f6 = 0.0F;

            if(!entity.isRiding())
            {
                f5 = entity.prevLimbSwingAmount + (entity.limbSwingAmount - entity.prevLimbSwingAmount) * partialTicks;
                f6 = entity.limbSwing - entity.limbSwingAmount * (1.0F - partialTicks);
                if(entity.isChild()) f6 *= 3.0F;
                if(f5 > 1.0F) f5 = 1.0F;
                f2 = f1 - f;
            }

            GlStateManager.enableAlpha();
            renderer.mainModel.setLivingAnimations(entity, f6, f5, partialTicks);
            renderer.mainModel.setRotationAngles(f6, f5, f8, f2, f7, f4, entity);

            if(renderer.renderOutlines)
            {
                boolean flag1 = renderer.setScoreTeamColor(entity);
                GlStateManager.enableColorMaterial();
                GlStateManager.enableOutlineMode(renderer.getTeamColor(entity));
                if(!renderer.renderMarker) renderer.renderModel(entity, f6, f5, f8, f2, f7, f4);
                if(!(entity instanceof EntityPlayer) || !((EntityPlayer)entity).isSpectator()) renderer.renderLayers(entity, f6, f5, partialTicks, f8, f2, f7, f4);
                GlStateManager.disableOutlineMode();
                GlStateManager.disableColorMaterial();
                if(flag1) renderer.unsetScoreTeamColor();
            }
            else
            {
                boolean flag = renderer.setDoRenderBrightness(entity, partialTicks);
                renderer.renderModel(entity, f6, f5, f8, f2, f7, f4);
                if(flag) renderer.unsetBrightness();
                GlStateManager.depthMask(true);
                if(!(entity instanceof EntityPlayer) || !((EntityPlayer)entity).isSpectator()) renderer.renderLayers(entity, f6, f5, partialTicks, f8, f2, f7, f4);
            }

            GlStateManager.disableRescaleNormal();
            GlStateManager.popMatrix();
        }
        catch(Exception exception) {
            exception.printStackTrace();
        }

        GlStateManager.setActiveTexture(OpenGlHelper.lightmapTexUnit);
        GlStateManager.enableTexture2D();
        GlStateManager.setActiveTexture(OpenGlHelper.defaultTexUnit);
        GlStateManager.enableCull();
        GlStateManager.popMatrix();

        if(!renderer.renderOutlines) renderer.renderName(entity, x, y, z);

        net.minecraftforge.common.MinecraftForge.EVENT_BUS.post(new net.minecraftforge.client.event.RenderLivingEvent.Post<EntityLivingBase>(entity, renderer, partialTicks, x, y, z));
    }

    /**
     * 【最终修正版】扁平化渲染辅助方法。
     * 解决了“不朝向玩家”的问题。
     */
    public static void prepareFlatRender(double x, double z, float f)
    {
        // 1. 计算广告牌角度，即从实体指向玩家的角度
        double angle1 = Math.atan2(z, x) / 3.141592653589793D * 180.0D;

        // 2. 计算精灵图角度，即实体自身想朝向的方向与玩家方向的角度差，并量化到45度
        double angle2 = Math.floor((f - angle1) / 45.0D) * 45.0D;

        // 3. 应用变换
        // 【关键修正】增加 180 度修正，以抵消模型默认面向-Z轴的问题，确保模型正面朝向玩家
        GlStateManager.rotate(180.0F - (float)angle1, 0.0F, 1.0F, 0.0F);

        // 将实体压扁成纸片
        GlStateManager.scale(0.02F, 1.0F, 1.0F);

        // 将压扁后的纸片旋转到对应的精灵图角度
        GlStateManager.rotate((float)angle2, 0.0F, 1.0F, 0.0F);
    }
}