package com.abyess.Hollow;

import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.entity.RenderLiving;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.client.renderer.entity.layers.LayerSlimeGel;
import net.minecraft.util.ResourceLocation;

// 正确的自定义渲染器类
public class RenderHollow extends RenderLiving<EntityHollow> {
    private static final ResourceLocation TEXTURE = new ResourceLocation("curseofabyss", "textures/entity/hollow.png");

    public RenderHollow(RenderManager renderManager) {
        // 这里传入你的自定义模型和阴影尺寸
        super(renderManager, new ModelHollow(), 0.5F);


    }

    @Override
    protected ResourceLocation getEntityTexture(EntityHollow entity) {
        return TEXTURE;
    }

    @Override
    public void doRender(EntityHollow entity, double x, double y, double z, float yaw, float partialTicks) {
        // 应用史莱姆挤压效果
        this.shadowSize = 0.25F * entity.getSlimeSize(); // 动态调整阴影大小
        super.doRender(entity, x, y, z, yaw, partialTicks);
    }

    @Override
    protected void preRenderCallback(EntityHollow entity, float partialTickTime) {



        // 挤压动画逻辑保持原样
        float squish = entity.getSquishFactor() + (entity.getPrevSquishFactor() - entity.getSquishFactor()) * partialTickTime;
        float scaleFactor = 1.0F / (squish * 0.5F + 1.0F);
        GlStateManager.scale(scaleFactor, 1.0F / scaleFactor, scaleFactor);
    }



}