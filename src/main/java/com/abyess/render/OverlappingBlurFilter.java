package com.abyess.render;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import org.lwjgl.opengl.GL11;

import java.util.Random;

public class OverlappingBlurFilter {

    private static boolean isEffectActive = false;
    private static boolean isDecayEnabled = false; // 是否启用衰减
    private static long effectEndTick = 0;  // 效果结束时间（以游戏 tick 为单位）
    private static int durationTicks = 0;  // 效果持续时间
    private static final Random random = new Random();

    // 构造函数注册事件总线
    public OverlappingBlurFilter() {
        MinecraftForge.EVENT_BUS.register(this);
    }

    /**
     * 启动滤镜效果
     *
     * @param durationInTicks 持续时间（以 ticks 为单位）
     * @param decayEnabled    是否启用衰减
     */
    public static void startEffect(int durationInTicks, boolean decayEnabled) {
        isEffectActive = true;
        isDecayEnabled = decayEnabled; // 设置衰减开关
        durationTicks = durationInTicks;  // 存储原始持续时间
        effectEndTick = 0; // 每次启动时清零，重新计算
    }

    @SubscribeEvent
    public void onRenderOverlay(RenderGameOverlayEvent.Post event) {
        if (!isEffectActive || event.getType() != RenderGameOverlayEvent.ElementType.ALL) return;

        Minecraft mc = Minecraft.getMinecraft();
        if (mc.world == null || mc.player == null) {
            resetEffect();
            return;
        }

        // 检测玩家是否死亡
        if (mc.player.isDead || mc.player.getHealth() <= 0.0F) {
            resetEffect();
            return;
        }

        // 初始化结束时间
        if (effectEndTick == 0 && durationTicks > 0) {
            effectEndTick = mc.world.getTotalWorldTime() + durationTicks;
        }

        // 检查效果是否超时
        long currentTime = mc.world.getTotalWorldTime();
        if (currentTime >= effectEndTick) {
            resetEffect();
            return;
        }

        // 计算剩余时间百分比（增加最低值限制，避免过早消失）
        float remainingFraction = Math.max(0.1f, (float) (effectEndTick - currentTime) / durationTicks);

        // 保存当前 OpenGL 状态
        GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);
        GL11.glPushMatrix();

        // 模糊效果实现
        ScaledResolution sr = new ScaledResolution(mc);
        int w = sr.getScaledWidth();
        int h = sr.getScaledHeight();

        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);

        // 根据衰减调整模糊强度（透明度和位移减少更平滑）
        float alpha = 0.3f * (float) Math.sqrt(remainingFraction); // 平滑衰减透明度
        GL11.glColor4f(1, 1, 1, alpha);

        // 绑定游戏帧缓冲纹理
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, mc.getFramebuffer().framebufferTexture);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);

        // 多重绘制产生模糊效果
        for (int i = 0; i < 4; i++) {
            GL11.glPushMatrix();
            float offsetX = (random.nextFloat() - 0.5f) * 8f * (float) Math.sqrt(remainingFraction); // 衰减位移
            float offsetY = (random.nextFloat() - 0.5f) * 8f * (float) Math.sqrt(remainingFraction); // 衰减位移
            GL11.glTranslatef(offsetX, offsetY, 0);

            GL11.glBegin(GL11.GL_QUADS);
            GL11.glTexCoord2f(0, 0);
            GL11.glVertex2f(0, 0);
            GL11.glTexCoord2f(0, 1);
            GL11.glVertex2f(0, h);
            GL11.glTexCoord2f(1, 1);
            GL11.glVertex2f(w, h);
            GL11.glTexCoord2f(1, 0);
            GL11.glVertex2f(w, 0);
            GL11.glEnd();

            GL11.glPopMatrix();
        }

        // 恢复状态
        GL11.glPopMatrix();
        GL11.glPopAttrib();
        GL11.glColor4f(1, 1, 1, 1);
        GL11.glDisable(GL11.GL_BLEND);
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, 0);
    }

    /**
     * 重置滤镜效果
     */
    private static void resetEffect() {
        isEffectActive = false;
        isDecayEnabled = false; // 重置衰减开关
        effectEndTick = 0;
        durationTicks = 0;
    }
}