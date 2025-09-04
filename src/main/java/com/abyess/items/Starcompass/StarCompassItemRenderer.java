package com.abyess.items.Starcompass;



import com.abyess.items.Starcompass.ItemStarCompass;

import net.minecraft.client.Minecraft;

import net.minecraft.client.renderer.GLAllocation;

import net.minecraft.client.renderer.GlStateManager;

import net.minecraft.client.renderer.block.model.IBakedModel;

import net.minecraft.client.renderer.block.model.ModelManager;

import net.minecraft.client.renderer.block.model.ModelResourceLocation;

import net.minecraft.client.renderer.tileentity.TileEntityItemStackRenderer;

import net.minecraft.entity.Entity;

import net.minecraft.entity.item.EntityItem;

import net.minecraft.item.ItemStack;

import net.minecraft.nbt.NBTTagCompound;

import net.minecraft.util.math.AxisAlignedBB;

import net.minecraft.util.math.BlockPos;

import net.minecraft.util.math.MathHelper;

import net.minecraft.world.World;

import org.lwjgl.opengl.GL11;



public class StarCompassItemRenderer extends TileEntityItemStackRenderer {





    private float partialTicks;

// 模型缓存

    private IBakedModel baseModel;

    private IBakedModel needleModel;



// 新增字段存储渲染时的实体上下文

    private Entity contextEntity;



    @Override

    public void renderByItem(ItemStack stack, float partialTicks) {

// 提前进行有效性检查

        if (!isValidStack(stack)) {

            super.renderByItem(stack, partialTicks);

            return;

        }



// 存储插值时间

        this.partialTicks = partialTicks;







// 开始自定义渲染流程

        GlStateManager.pushMatrix();

        GlStateManager.pushAttrib();

        try {

            prepareRenderState();

            renderBaseModel(stack);

            render3DNeedle(stack); // 内部调用 calculate3DRotations

        } finally {
            GlStateManager.color(1F, 1F, 1.0F, 1F);
            GlStateManager.popAttrib();

            GlStateManager.popMatrix();

        }

    }



    private boolean isValidStack(ItemStack stack) {

        if (stack.isEmpty() || stack.getItem() != ItemStarCompass.INSTANCE) {

            super.renderByItem(stack);

            return false;

        }

        return true;

    }



    private void prepareRenderState() {



        GlStateManager.enableRescaleNormal();

        GlStateManager.enableDepth();

        GlStateManager.enableBlend();

        GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);

        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);

    }





    private void render3DNeedle(ItemStack stack) {

        float[] rotations = calculate3DRotations(stack);

        GlStateManager.pushMatrix();

        try {

            apply3DTransformation(rotations[0], rotations[1],stack);

            renderNeedleModel(stack);







            renderTransparentSphere();



        } finally {

            GlStateManager.popMatrix();

        }

    }





    private void renderTransparentSphere() {

        GlStateManager.pushMatrix();

        GlStateManager.pushAttrib();



        try {



            GlStateManager.translate(0.5F, 0.5F, 0.5F);

            GlStateManager.scale(0.42F, 0.42F, 0.42F);



// 深度测试配置优化

            GlStateManager.enableDepth();

            GlStateManager.depthFunc(GL11.GL_LEQUAL);

            GlStateManager.depthMask(false); // 禁止写入深度缓冲



// 混合模式调整

            GlStateManager.enableBlend();

            GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);

            GlStateManager.disableAlpha();

            GlStateManager.disableLighting(); // 禁用光照计算



// 调整颜色参数（增加可见性）

            GlStateManager.color(0.2F, 0.5F, 1.0F, 0.3F);

            GlStateManager.disableTexture2D();



            drawSphere(1.0F, 16, 16);



        } finally {

// 恢复渲染状态

            GlStateManager.enableLighting();

            GlStateManager.enableAlpha();

            GlStateManager.enableTexture2D();

            GlStateManager.depthMask(true);
            GlStateManager.color(1F, 1F, 1.0F, 1F);
            GlStateManager.popAttrib();

            GlStateManager.popMatrix();

        }

    }





// 预计算顶点数据的显示列表（优化关键）

    private static int sphereDisplayList = -1;



// 初始化时预编译显示列表

    public static void compileSphereDisplayList() {

        if (sphereDisplayList == -1) {

            sphereDisplayList = GLAllocation.generateDisplayLists(1);

            GL11.glNewList(sphereDisplayList, GL11.GL_COMPILE);



// 优化后的参数设置

            final int slices = 24; // 经线细分（保持水平方向细节）

            final int stacks = 16; // 纬线细分（减少垂直方向细节）

            final float radius = 1.0f;



// 法线计算优化（关键改进）

            GL11.glBegin(GL11.GL_TRIANGLE_STRIP);

            for (int i = 0; i < stacks; ++i) {

                double phi = Math.PI * i / stacks;

                double nextPhi = Math.PI * (i + 1) / stacks;



// 预计算公共三角函数值

                double cosPhi = Math.cos(phi);

                double sinPhi = Math.sin(phi);

                double cosNextPhi = Math.cos(nextPhi);

                double sinNextPhi = Math.sin(nextPhi);



                for (int j = 0; j <= slices; ++j) {

                    double theta = 2 * Math.PI * j / slices;

                    double cosTheta = Math.cos(theta);

                    double sinTheta = Math.sin(theta);



// 当前顶点法线（直接使用顶点坐标）

                    double nx = cosTheta * sinPhi;

                    double ny = cosPhi;

                    double nz = sinTheta * sinPhi;

                    GL11.glNormal3d(nx, ny, nz);

                    GL11.glVertex3d(nx * radius, ny * radius, nz * radius);



// 下一层顶点法线

                    nx = cosTheta * sinNextPhi;

                    ny = cosNextPhi;

                    nz = sinTheta * sinNextPhi;

                    GL11.glNormal3d(nx, ny, nz);

                    GL11.glVertex3d(nx * radius, ny * radius, nz * radius);

                }

            }

            GL11.glEnd();

            GL11.glEndList();

        }

    }



    private void drawSphere(float radius, int slices, int stacks) {

// 使用预编译显示列表

        if (sphereDisplayList == -1) compileSphereDisplayList();

        GL11.glCallList(sphereDisplayList);

    }





    /**

     * 修改点：仅在NBT数据指示掉落物状态（存在 "CompassYaw"）时应用外壳旋转，

     * 同时利用线性插值平滑旋转过渡。

     */

    private void renderBaseModel(ItemStack stack) {

        loadModelsIfNeeded();

        GlStateManager.pushMatrix();

        World world = Minecraft.getMinecraft().world;

// 将模型平移到中心后进行缩放

        GlStateManager.translate(0.5F, 0.5F, 0.5F);

        GlStateManager.scale(0.7F, 0.7F, 0.7F);

        BlockPos target = getTargetPosition(stack);

        boolean isDropped = stack.hasTagCompound() && stack.getTagCompound().hasKey("CompassYaw");

        if (isDropped||target == null||!ItemStarCompass.isInTargetDimension(world, stack)) {

// 平滑旋转逻辑

            float rotationSpeed = 1.8F; // 降低旋转速度

            float rawRotation;

            if (this.contextEntity instanceof EntityItem) {

                EntityItem entityItem = (EntityItem) this.contextEntity;

// 使用插值后的tick计数

                float interpolatedTicks = entityItem.ticksExisted + partialTicks;

// 应用角度回绕并添加正弦波动

                rawRotation = MathHelper.wrapDegrees(interpolatedTicks * rotationSpeed);

                rawRotation += MathHelper.sin(interpolatedTicks * 0.1F) * 2.5F; // 添加细微波动

            } else {

// 世界时间为基础的备用方案

                long worldTime = Minecraft.getMinecraft().world.getTotalWorldTime();

                rawRotation = MathHelper.wrapDegrees((worldTime + partialTicks) * rotationSpeed);

            }



// 应用平滑旋转

            GlStateManager.rotate(rawRotation, 0.0F, 1.0F, 0.0F);

        } else {

// 原有玩家手持逻辑保持不变

            Entity viewer = Minecraft.getMinecraft().getRenderViewEntity();

            if (viewer != null) {

                float interpolatedYaw = viewer.prevRotationYaw + (viewer.rotationYaw - viewer.prevRotationYaw) * partialTicks;

                float interpolatedPitch = viewer.prevRotationPitch + (viewer.rotationPitch - viewer.prevRotationPitch) * partialTicks;



// GlStateManager.rotate(180.0F, 0.0F, 0.0F, 1.0F);

                GlStateManager.rotate(interpolatedYaw, 0.0F, 1.0F, 0.0F);

                GlStateManager.rotate(interpolatedPitch, 1.0F, 0.0F, 0.0F);

            }

        }



        Minecraft.getMinecraft().getRenderItem().renderItem(stack, baseModel);

        GlStateManager.popMatrix();

    }





    private void renderNeedleModel(ItemStack stack) {

        GlStateManager.pushMatrix();



// 调整模型中心点并缩放

        GlStateManager.translate(0.5F, 0.5F, 0.5F);

        GlStateManager.scale(0.3F, 0.3F, 0.3F);





// 启用深度测试并渲染模型

        GlStateManager.enableDepth();

        GlStateManager.depthFunc(GL11.GL_LEQUAL);

        Minecraft.getMinecraft().getRenderItem().renderItem(stack, needleModel);



// 调试坐标轴（可选）

// renderDebugAxes();



        GlStateManager.popMatrix();

    }

// 新增调试坐标轴渲染方法

// private void renderDebugAxes() {

// GlStateManager.pushMatrix();

// GlStateManager.pushAttrib();

//

// try {

// // 设置渲染参数

// GlStateManager.disableTexture2D();

// GlStateManager.disableLighting();

// GL11.glLineWidth(2.0F); // 设置线宽

//

// // 坐标轴长度

// final float axisLength = 1.5F;

//

// // 绘制X轴（红色）

// GlStateManager.color(1.0F, 0.0F, 0.0F, 1.0F);

// GL11.glBegin(GL11.GL_LINES);

// GL11.glVertex3f(0.0F, 0.0F, 0.0F);

// GL11.glVertex3f(axisLength, 0.0F, 0.0F);

// GL11.glEnd();

//

// // 绘制Y轴（绿色）

// GlStateManager.color(0.0F, 1.0F, 0.0F, 1.0F);

// GL11.glBegin(GL11.GL_LINES);

// GL11.glVertex3f(0.0F, 0.0F, 0.0F);

// GL11.glVertex3f(0.0F, axisLength, 0.0F);

// GL11.glEnd();

//

// // 绘制Z轴（蓝色）

// GlStateManager.color(0.0F, 0.0F, 1.0F, 1.0F);

// GL11.glBegin(GL11.GL_LINES);

// GL11.glVertex3f(0.0F, 0.0F, 0.0F);

// GL11.glVertex3f(0.0F, 0.0F, axisLength);

// GL11.glEnd();

//

// } finally {

// // 恢复渲染状态

// GlStateManager.enableTexture2D();

// GlStateManager.enableLighting();

// GlStateManager.popAttrib();

// GlStateManager.popMatrix();

// }

// }





    public float[] calculate3DRotations(ItemStack stack) {

        World world = Minecraft.getMinecraft().world;

        Entity viewer = Minecraft.getMinecraft().getRenderViewEntity();

        BlockPos target = getTargetPosition(stack);



// 添加维度检查：如果不在目标维度，返回空闲动画角度

        if (world == null || viewer == null || target == null ||

                !ItemStarCompass.isInTargetDimension(world, stack)) {

            return getIdleAnimationAngles();

        }



// 优先检查NBT中的角度值

        NBTTagCompound nbt = stack.getTagCompound();

        if (nbt != null && nbt.hasKey("CompassYaw") && nbt.hasKey("CompassPitch")) {

            return new float[] {

                    MathHelper.wrapDegrees(nbt.getFloat("CompassYaw")),

                    nbt.getFloat("CompassPitch")

            };

        }



// 实时计算目标方向（玩家手持时）

// 计算玩家眼睛位置到目标的向量

        double playerX = viewer.lastTickPosX + (viewer.posX - viewer.lastTickPosX) * partialTicks;

        double playerY = viewer.lastTickPosY + (viewer.posY - viewer.lastTickPosY) * partialTicks + viewer.getEyeHeight();

        double playerZ = viewer.lastTickPosZ + (viewer.posZ - viewer.lastTickPosZ) * partialTicks;



        double dx = target.getX() + 0.5 - playerX;

        double dy = target.getY() + 0.5 - playerY;

        double dz = target.getZ() + 0.5 - playerZ;



        double horizontalDist = MathHelper.sqrt(dx * dx + dz * dz);

        float yaw = (float) Math.toDegrees(Math.atan2(dz, dx)) - 90.0F;

        yaw = MathHelper.wrapDegrees(yaw + 180.0F); // 转换为0-360度



        float pitch = (float) -Math.toDegrees(Math.asin(dy / MathHelper.sqrt(dx * dx + dy * dy + dz * dz)));



        return new float[] { yaw, pitch };

    }





    private void apply3DTransformation(float targetYaw, float targetPitch, ItemStack stack) {

        GlStateManager.translate(0.5F, 0.5F, 0.5F);

        BlockPos target = getTargetPosition(stack);



// 初始朝向修正（关键调整）

        GlStateManager.rotate(90.0F, 1.0F, 0.0F, 0.0F); // 模型Z轴→Y轴正方向

        GlStateManager.rotate(180.0F, 0.0F, 0.0F, 1.0F); // 原有修正



// 先应用俯仰角，再应用偏航角（顺序调整）

        GlStateManager.rotate(targetPitch, 1.0F, 0.0F, 0.0F); // X轴旋转

        GlStateManager.rotate(targetYaw, 0.0F, 1.0F, 0.0F); // Y轴旋转



        GlStateManager.translate(-0.5F, -0.5F, -0.5F);

    }







// 使用ThreadLocal保证线程安全



// 简化版伪随机噪声（仅用于演示）

// 简化版伪随机噪声（仅用于演示）



    private final float BASE_NOISE_SPEED = 0.12f; // 原基础速度

    private final float SPEED_FACTOR = 0.9f; // 70% 速度



    private float[] getIdleAnimationAngles() {

        World world = Minecraft.getMinecraft().world;

        if (world == null) {

            return new float[] {0.0F, 0.0F}; // 默认静止角度

        }



// 基于游戏时间计算动画进度

        float totalTime = (world.getTotalWorldTime() + partialTicks);

        float speedAdjustedTime = totalTime * BASE_NOISE_SPEED * SPEED_FACTOR;



        float idleYaw = (pseudoNoise(speedAdjustedTime) * 120.0f - 60.0f);

        float idlePitch = (pseudoNoise(speedAdjustedTime + 100.0f) * 90.0f - 45.0f);



        return new float[] { idleYaw, idlePitch };

    }



// 移除noiseSeed字段，修改伪随机函数为静态方法

    private static float pseudoNoise(float time) {

        int tInt = (int) time;

        float tFrac = time - tInt;



        float a = (float) Math.sin(tInt * 127.1f + tInt * 311.7f) * 43758.5453f % 1.0f;

        float b = (float) Math.sin((tInt + 1) * 127.1f + (tInt + 1) * 311.7f) * 43758.5453f % 1.0f;

        return a + (b - a) * tFrac;

    }





    private BlockPos getTargetPosition(ItemStack stack) {

        if (!stack.hasTagCompound())

            return null;



        NBTTagCompound nbt = stack.getTagCompound();

        String NBT_TARGET_KEY = "TargetPos";

// 仅当存在手动设置的目标时返回，忽略默认配置

        return nbt.hasKey(NBT_TARGET_KEY) ? BlockPos.fromLong(nbt.getLong(NBT_TARGET_KEY)) : null;

    }



    private void loadModelsIfNeeded() {

        if (baseModel == null || needleModel == null) {

            ModelManager modelManager = Minecraft.getMinecraft().getRenderItem().getItemModelMesher().getModelManager();

            baseModel = modelManager.getModel(new ModelResourceLocation("curseofabyss:star_compass_base", "inventory"));

            needleModel = modelManager.getModel(new ModelResourceLocation("curseofabyss:star_compass_needle", "inventory"));

        }

    }



// 单例模式访问

    public static final StarCompassItemRenderer INSTANCE = new StarCompassItemRenderer();









}