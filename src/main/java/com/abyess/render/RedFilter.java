package com.abyess.render;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.lwjgl.opengl.GL11;

@SideOnly(Side.CLIENT)
public class RedFilter {

    private static boolean isActive = false;
    private static boolean isDecayEnabled = false; // 是否启用衰减
    private static int startTick;         // 游戏刻计时起点
    private static int durationTicks;     // 总持续时间
    private static final float MAX_ALPHA = 0.90f; // 最大透明度

    public RedFilter() {
        MinecraftForge.EVENT_BUS.register(this);
    }

    /**
     * 启动红色滤镜效果。
     *
     * @param durationTicks 持续时间（以游戏刻为单位）。
     * @param decayEnabled  是否启用衰减。
     */
    public static void activate(int durationTicks, boolean decayEnabled) {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.world != null) {  // 检查世界是否存在
            isActive = true;
            isDecayEnabled = decayEnabled; // 设置衰减开关
            RedFilter.durationTicks = durationTicks;
            startTick = (int) mc.world.getTotalWorldTime();
        }
    }

    /**
     * 获取当前滤镜的透明度。
     *
     * @return 当前透明度值。
     */
    private float getCurrentAlpha() {
        if (!isActive) return 0.0f;
        Minecraft mc = Minecraft.getMinecraft();

        // 检查关键状态
        if (mc.world == null || mc.player == null) {
            resetEffect();
            return 0.0f;
        }

        // 检查玩家是否死亡
        if (mc.player.isDead) {
            resetEffect(); // 玩家死亡，停止效果
            return 0.0f;
        }

        int currentTick = (int) mc.world.getTotalWorldTime();
        int elapsed = currentTick - startTick;

        if (elapsed >= durationTicks) {
            resetEffect();
            return 0.0f;
        }

        if (isDecayEnabled) {
            float progress = elapsed / (float) durationTicks;
            return MAX_ALPHA * (1.0f - progress);  // 线性渐变透明度
        } else {
            return MAX_ALPHA; // 如果没有衰减，始终保持最大透明度
        }
    }

    /**
     * 在游戏渲染界面时绘制红色滤镜。
     *
     * @param event 渲染事件。
     */
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onRenderOverlay(RenderGameOverlayEvent.Post event) {
        if (event.getType() != RenderGameOverlayEvent.ElementType.ALL) return;
        renderRedOverlay();
    }

    /**
     * 绘制红色滤镜覆盖层。
     */
    private void renderRedOverlay() {
        float alpha = getCurrentAlpha();
        if (alpha <= 0.0f) return;

        Minecraft mc = Minecraft.getMinecraft();
        ScaledResolution res = new ScaledResolution(mc);

        GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);
        GL11.glDisable(GL11.GL_DEPTH_TEST);
        GL11.glDisable(GL11.GL_TEXTURE_2D);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);

        // 红色滤镜的颜色设置
        GL11.glColor4f(0.7f, 0.05f, 0.05f, alpha * 0.9f); // 调整红色值，变暗

        Tessellator tess = Tessellator.getInstance();
        BufferBuilder buf = tess.getBuffer();
        buf.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION);
        buf.pos(0, res.getScaledHeight(), 0).endVertex();
        buf.pos(res.getScaledWidth(), res.getScaledHeight(), 0).endVertex();
        buf.pos(res.getScaledWidth(), 0, 0).endVertex();
        buf.pos(0, 0, 0).endVertex();
        tess.draw();

        GL11.glPopAttrib();
    }

    /**
     * 重置红色滤镜效果。
     */
    private static void resetEffect() {
        isActive = false;
        isDecayEnabled = false; // 重置衰减开关
        startTick = 0;
        durationTicks = 0;
    }
}