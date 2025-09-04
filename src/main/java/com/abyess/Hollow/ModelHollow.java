package com.abyess.Hollow;

import net.minecraft.client.Minecraft;
import net.minecraft.client.model.ModelBase;
import net.minecraft.client.model.ModelBox;
import net.minecraft.client.model.ModelRenderer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.monster.EntitySlime;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.MathHelper;

public class ModelHollow extends ModelBase {
    private final ModelRenderer body;

    public ModelHollow() {
        textureWidth = 128;
        textureHeight = 128;
        this.boxList.clear();

        body = new ModelRenderer(this);
        body.setRotationPoint(0.0F, 25.0F, 0.0F); // 基准点从 24 改为 16（Y轴下沉8单位）


        body.cubeList.add(new ModelBox(body, 0, 0, -8.0F, -11.0F, -6.0F, 12, 9, 12, 0.0F, false));
        body.cubeList.add(new ModelBox(body, 0, 35, -7.0F, -12.0F, -5.0F, 10, 1, 10, 0.0F, false));
        body.cubeList.add(new ModelBox(body, 48, 19, -5.0F, -15.0F, 4.0F, 1, 1, 1, 0.0F, false));
        body.cubeList.add(new ModelBox(body, 48, 17, 0.0F, -15.0F, 4.0F, 1, 1, 1, 0.0F, false));
        body.cubeList.add(new ModelBox(body, 47, 15, -1.0F, -14.0F, 4.0F, 3, 1, 1, 0.0F, false));
        body.cubeList.add(new ModelBox(body, 47, 13, -6.0F, -14.0F, 4.0F, 3, 1, 1, 0.0F, false));
        body.cubeList.add(new ModelBox(body, 48, 11, -7.0F, -13.0F, 4.0F, 4, 1, 1, 0.0F, false));
        body.cubeList.add(new ModelBox(body, 22, 46, -1.0F, -13.0F, 4.0F, 4, 1, 1, 0.0F, false));
        body.cubeList.add(new ModelBox(body, 48, 0, -7.0F, -10.0F, 6.0F, 10, 7, 1, 0.0F, false));
        body.cubeList.add(new ModelBox(body, 40, 44, 4.0F, -10.0F, -5.0F, 1, 7, 10, 0.0F, false));
        body.cubeList.add(new ModelBox(body, 0, 46, -7.0F, -10.0F, -7.0F, 10, 7, 1, 0.0F, false));
        body.cubeList.add(new ModelBox(body, 0, 24, -7.0F, -2.0F, -5.0F, 10, 1, 10, 0.0F, false));
        body.cubeList.add(new ModelBox(body, 40, 24, -9.0F, -10.0F, -5.0F, 1, 7, 10, 0.0F, false));
        body.cubeList.add(new ModelBox(body, 30, 45, -12.0F, -6.0F, -5.0F, 3, 3, 3, 0.0F, false));
        body.cubeList.add(new ModelBox(body, 30, 45, -12.0F, -6.0F, 2.0F, 3, 3, 3, 0.0F, false));
        body.cubeList.add(new ModelBox(body, 30, 45, 5.0F, -6.0F, -5.0F, 3, 3, 3, 0.0F, false));
        body.cubeList.add(new ModelBox(body, 30, 45, 5.0F, -6.0F, 2.0F, 3, 3, 3, 0.0F, false));

        this.boxList.add(body);
    }
    private boolean isLevelModelRendering = false; // 新增标志位

    public void setForLevelModel(boolean isLevelModel) {
        this.isLevelModelRendering = isLevelModel;
    }
    // 保留原有动画和渲染逻辑
    @Override
    public void render(Entity entity, float limbSwing, float limbSwingAmount,
                       float ageInTicks, float headYaw, float headPitch, float scale) {
        EntitySlime slime = (EntitySlime) entity;
        float squish = slime.squishFactor + (slime.prevSquishFactor - slime.squishFactor) * ageInTicks;

        GlStateManager.pushMatrix();
        if (squish != 0.0F) {
            float scaleFactor = 1.0F / (squish * 0.5F + 1.0F);
            GlStateManager.translate(0.0F, -0.5F * scaleFactor, 0.0F);
            GlStateManager.scale(scaleFactor, 1.0F / scaleFactor, scaleFactor);
        }
// 根据模式动态补偿
        if (!isLevelModelRendering) {
            GlStateManager.translate(0.0F, 0.5F, 0.0F); // 仅非LevelmodelRenderer时补偿
        }

        body.render(scale);
        GlStateManager.popMatrix();
    }


    }
