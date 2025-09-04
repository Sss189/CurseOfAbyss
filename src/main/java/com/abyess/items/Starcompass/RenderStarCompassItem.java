package com.abyess.items.Starcompass;

import com.abyess.items.Starcompass.EntityStarCompassItem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderItem;
import net.minecraft.client.renderer.block.model.ItemCameraTransforms;
import net.minecraft.client.renderer.entity.Render;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;

public class RenderStarCompassItem extends Render<EntityStarCompassItem> {
    private final RenderItem itemRenderer;

    public RenderStarCompassItem(RenderManager manager) {
        super(manager);
        this.itemRenderer = Minecraft.getMinecraft().getRenderItem();
    }


@Override
public void doRender(EntityStarCompassItem entity, double x, double y, double z, float entityYaw, float partialTicks) {
    ItemStack stack = entity.getItem();

// 计算上下悬浮的效果（使用正弦波）
    float bobbing = (float) Math.sin((entity.ticksExisted + partialTicks) * 0.1F) * 0.1F;

// 计算目标偏航角（原来的旋转逻辑保留）
    float renderYaw = calculateTargetYaw(entity, partialTicks);

    GlStateManager.pushMatrix();
// 定位到实体位置：基础Y高度 + 悬浮动效
    GlStateManager.translate(x, y + 0.65D + bobbing, z);
// 使用原本的旋转逻辑：将计算后的偏航角应用于渲染
    GlStateManager.rotate(180.0F - renderYaw, 0.0F, 1.0F, 0.0F);

// 渲染物品
    this.itemRenderer.renderItem(stack, ItemCameraTransforms.TransformType.FIXED);

    GlStateManager.popMatrix();
}
    // 新增：计算目标偏航角
    private float calculateTargetYaw(EntityStarCompassItem entity, float partialTicks) {
        ItemStack stack = entity.getItem();
        if (stack.getItem() instanceof ItemStarCompass) {
            BlockPos target = ((ItemStarCompass) stack.getItem()).getTargetPosition(stack);
            World world = entity.world; // 获取当前世界

            // 检查目标是否存在且当前在目标维度
            if (target != null && ItemStarCompass.isInTargetDimension(world, stack)) {
                // 计算水平方向向量
                double dx = target.getX() + 0.5 - entity.posX;
                double dz = target.getZ() + 0.5 - entity.posZ;

                // 计算偏航角（使用MathHelper的atan2实现）
                float yaw = (float) Math.toDegrees(MathHelper.atan2(dz, dx));
                yaw = MathHelper.wrapDegrees(yaw - 90.0F); // 转换为Minecraft坐标系
                return yaw;
            } else {
                // 添加空闲动画（当目标不存在或不在目标维度时）
                long time = System.currentTimeMillis();
                return (time * 0.05F) % 360; // 与IDLE_ANIM_SPEED_Y同步
            }
        }
        return 0.0F;
    }

    private BlockPos getTargetPosition(ItemStack stack) {
        // 复用原有NBT读取逻辑
        if (stack.hasTagCompound() && stack.getTagCompound().hasKey("TargetPos")) {
            return BlockPos.fromLong(stack.getTagCompound().getLong("TargetPos"));
        }
        return null;
    }

    @Override
    protected ResourceLocation getEntityTexture(EntityStarCompassItem entity) {
        return TextureMap.LOCATION_BLOCKS_TEXTURE;
    }
}
