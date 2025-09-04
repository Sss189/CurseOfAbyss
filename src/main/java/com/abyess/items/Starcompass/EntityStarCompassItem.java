package com.abyess.items.Starcompass;

import net.minecraft.entity.MoverType;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;

public class EntityStarCompassItem extends EntityItem {
    public EntityStarCompassItem(World world) {
        super(world);
    }

    public EntityStarCompassItem(World world, double x, double y, double z) {
        super(world, x, y, z);
        // 移除 setNoDespawn() 和 motion 清零
    }

    public EntityStarCompassItem(World world, double x, double y, double z, ItemStack stack) {
        super(world, x, y, z, stack); // 保留父类初始化逻辑
    }
    @Override
    public void onCollideWithPlayer(EntityPlayer player) {
        // 在物品被捡起前清除NBT
        if (!this.world.isRemote && !this.isDead) {
            ItemStack stack = this.getItem();
            if (stack.getItem() instanceof ItemStarCompass) {
                clearNbtAngles(stack);

                // 关键修复：确保服务端更新物品
                if (!player.world.isRemote) {
                    player.inventory.markDirty();
                }
            }
        }
        super.onCollideWithPlayer(player);
    }
    // 新增：清除NBT角度数据的通用方法
    private void clearNbtAngles(ItemStack stack) {
        if (stack.hasTagCompound()) {
            NBTTagCompound nbt = stack.getTagCompound();
            nbt.removeTag("CompassYaw");
            nbt.removeTag("CompassPitch");
            if (nbt.hasNoTags()) { // 如果NBT为空则移除
                stack.setTagCompound(null);
            }
        }
    }




    @Override
    public void onUpdate() {
        super.onUpdate();

        // 仅客户端计算
        if (!world.isRemote) return;

        ItemStack stack = this.getItem();
        if (stack.getItem() instanceof ItemStarCompass) {


            BlockPos target = ((ItemStarCompass) stack.getItem()).getTargetPosition(stack);
            if (target != null && ItemStarCompass.isInTargetDimension(world, stack)) {
                // 计算实体中心高度
                double centerY = this.posY + this.height / 2.0;

                // 计算方向向量
                double dx = target.getX() + 0.5 - this.posX;
                double dy = target.getY() + 0.5 - centerY;
                double dz = target.getZ() + 0.5 - this.posZ;
                double dist = MathHelper.sqrt(dx*dx + dy*dy + dz*dz);

                if (dist > 0.001) {
                    // 计算偏航角（水平方向）
                    float yaw = (float) Math.toDegrees(Math.atan2(dz, dx)) - 90.0F;
                    yaw = MathHelper.wrapDegrees(yaw + 180.0F); // 转换为 0~360 度

                    // 计算俯仰角（垂直方向）
                    float pitch = (float) -Math.toDegrees(Math.asin(dy / dist));

                    // 写入NBT
                    if (!stack.hasTagCompound()) {
                        stack.setTagCompound(new NBTTagCompound());
                    }
                    NBTTagCompound nbt = stack.getTagCompound();
                    nbt.setFloat("CompassYaw", yaw);
                    nbt.setFloat("CompassPitch", pitch);

                    // 强制同步到实体（关键！）
                    this.setItem(stack); // 更新实体的物品堆栈
                }
            }
        }

        // 锁定实体旋转
        this.rotationYaw = 0;
        this.prevRotationYaw = 0;
        this.rotationPitch = 0;
        this.prevRotationPitch = 0;
    }
}
