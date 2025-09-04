package com.abyess.render;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import org.lwjgl.opengl.GL11;

/**
 * 反色滤镜渲染器
 * 用于在游戏界面中应用反色效果。
 */
public class InvertedColorFilter {

    private static boolean isFilterActive = false; // 表示反色滤镜是否激活
    private static int effectEndTick = 0;          // 反色滤镜结束的游戏刻时间
    private static int durationTicks = 0;          // 滤镜持续时间（以游戏刻为单位）

    /**
     * 构造函数，注册到事件总线
     */
    public InvertedColorFilter() {
        MinecraftForge.EVENT_BUS.register(this);
    }

    /**
     * 激活反色滤镜
     * @param durationInTicks 持续时间（单位：游戏刻）
     */
    public static void activateFilter(int durationInTicks) {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.world != null) { // 确保世界对象存在
            isFilterActive = true;
            durationTicks = durationInTicks;
            // effectEndTick 将在渲染时初始化
        }
    }

    /**
     * 在游戏界面渲染后应用反色滤镜
     * @param event 渲染事件
     */

    @SubscribeEvent(priority = EventPriority.LOW)
    public void onRenderGameOverlay(RenderGameOverlayEvent.Post event) {
        if (!isFilterActive || event.getType() != RenderGameOverlayEvent.ElementType.ALL) return;

        Minecraft mc = Minecraft.getMinecraft();
        if (mc.world == null || mc.player == null) { // 确保世界和玩家对象存在
            isFilterActive = false;
            return;
        }

        // 检测玩家是否死亡
        if (mc.player.isDead || mc.player.getHealth() <= 0.0F) {
            // 玩家死亡时禁用滤镜
            isFilterActive = false;
            effectEndTick = 0;
            durationTicks = 0;
            return;
        }

        // 延迟初始化结束刻
        if (effectEndTick == 0 && durationTicks > 0) {
            effectEndTick = (int) mc.world.getTotalWorldTime() + durationTicks;
        }

        // 检查滤镜是否已经到期
        if (mc.world.getTotalWorldTime() >= effectEndTick) {
            isFilterActive = false;
            effectEndTick = 0;
            durationTicks = 0;
            return;
        }

        // 获取分辨率
        ScaledResolution sr = new ScaledResolution(mc);
        int w = sr.getScaledWidth();
        int h = sr.getScaledHeight();

        // 保存所有 OpenGL 状态
        GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);
        GL11.glPushMatrix();

        // 应用反色效果的 OpenGL 设置
        GL11.glDisable(GL11.GL_TEXTURE_2D);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_ONE_MINUS_DST_COLOR, GL11.GL_ZERO);

        // 绘制全屏反色效果
        GL11.glColor4f(1.0f, 1.0f, 1.0f, 1.0f);
        GL11.glBegin(GL11.GL_QUADS);
        GL11.glVertex2f(0, 0);
        GL11.glVertex2f(0, h);
        GL11.glVertex2f(w, h);
        GL11.glVertex2f(w, 0);
        GL11.glEnd();

        // 恢复 OpenGL 状态
        GL11.glPopMatrix();
        GL11.glPopAttrib();
    }
}