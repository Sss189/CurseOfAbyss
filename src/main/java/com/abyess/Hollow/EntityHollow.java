package com.abyess.Hollow;

import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.EntityAIBase;
import net.minecraft.entity.monster.EntitySlime;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.world.World;

public class EntityHollow extends EntitySlime {

    public EntityHollow(World world) {
        super(world);
        // 强制设置为最小尺寸（原版1为最小）
        this.setSlimeSize(1, true);
        this.setSize(1.5F, 1.5F);    // 调整碰撞箱与模型尺寸匹配

        // 设置显示名字为 “Unfortunate Author”，且总是显示（客户端渲染用）
//        this.setCustomNameTag("§cUnfortunate Author");
//        this.setAlwaysRenderNameTag(true);

        // 清除原有AI任务
        this.tasks.taskEntries.clear();
        this.targetTasks.taskEntries.clear();

        // 初始化自定义AI
        this.initEntityAI();
    }

    /**
     * 重写 initEntityAI 方法，清空所有已有任务，
     * 并添加自定义 AI：每两秒跳跃一次
     */
    @Override
    protected void initEntityAI() {
        // 确保清空所有任务
        this.tasks.taskEntries.clear();
        this.targetTasks.taskEntries.clear();

        // 添加自定义 AI 任务，优先级设为1（数值越小优先级越高）
        this.tasks.addTask(1, new EntityAIJumpEveryTwoSeconds(this));
    }

    @Override
    public int getSlimeSize() {
        return 1; // 始终返回最小尺寸
    }

    @Override
    protected void dropFewItems(boolean wasRecentlyHit, int lootingModifier) {
        // 留空，不进行物品掉落
    }

    @Override
    public int getExperiencePoints(EntityPlayer player) {
        return 0; // 无经验掉落
    }

    @Override
    public void setSlimeSize(int size, boolean resetHealth) {
        // 防止尺寸被外部修改，总是固定为1
        super.setSlimeSize(1, resetHealth);
    }

    public void setSquishFactors(float current, float previous) {
        this.squishFactor = current;
        this.prevSquishFactor = previous;
    }

    public float getSquishFactor() {
        return this.squishFactor;
    }

    public float getPrevSquishFactor() {
        return this.prevSquishFactor;
    }

    /**
     * 重写 onUpdate 保留特效动画（仅在客户端使用）以及调用父类逻辑。
     * 原有的弹跳逻辑已由自定义 AI 处理，因此不在此处重复调用 jump()。
     */
    @Override
    public void onUpdate() {
        // 客户端仅更新挤压动画
        if (world.isRemote) {
            this.prevSquishFactor = this.squishFactor;
            this.squishFactor += (this.squishFactor - this.prevSquishFactor) * 0.5F;
        }
        super.onUpdate();
    }



    // 保留修正后的红石粒子逻辑（颜色参数已修正）
    @Override
    public void jump() {
        super.jump();

        if (this.world.isRemote) {
            for (int i = 0; i < 15; i++) {
                double offsetX = (this.rand.nextDouble() - 0.5D) * this.width;
                double offsetZ = (this.rand.nextDouble() - 0.5D) * this.width;
                this.world.spawnParticle(
                        EnumParticleTypes.REDSTONE,
                        this.posX + offsetX,
                        this.posY + 0.1D,
                        this.posZ + offsetZ,
                        1.0D, 0.0D, 0.0D // 红色粒子
                );
            }
        }
    }
    // 禁用原版史莱姆粒子
    @Override
    protected EnumParticleTypes getParticleType() {
        return EnumParticleTypes.REDSTONE; // 避免返回 null
    }
    /**
     * 重写实体属性，包括血量和移动速度
     */
    @Override
    protected void applyEntityAttributes() {
        super.applyEntityAttributes();
        // 设置最大生命值为5
        this.getEntityAttribute(SharedMonsterAttributes.MAX_HEALTH).setBaseValue(5.0D);
        // 设置移动速度
        this.getEntityAttribute(SharedMonsterAttributes.MOVEMENT_SPEED).setBaseValue(0.3D);
    }

    /**
     * 内部自定义AI任务：
     * 每40 tick（约2秒）执行一次跳跃（如果实体在地面）
     */
    private static class EntityAIJumpEveryTwoSeconds extends EntityAIBase {

        private final EntityHollow entity;
        // 用于累计tick数
        private int tickDelay;

        public EntityAIJumpEveryTwoSeconds(EntityHollow entity) {
            this.entity = entity;
            this.tickDelay = 0;
            // 设为0表示此任务与其他任务不互斥
            this.setMutexBits(0);
        }

        /**
         * 此任务始终执行
         */
        @Override
        public boolean shouldExecute() {
            return true;
        }

        /**
         * 每 tick 被调用一次
         */
        @Override
        public void updateTask() {
            tickDelay++;
            if (tickDelay >= 40) { // 40 tick ~= 2 秒
                tickDelay = 0;
                if (entity.onGround) {
                    entity.jump();
                }
            }
        }
    }
}