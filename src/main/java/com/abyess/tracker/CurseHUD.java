package com.abyess.tracker;

import com.abyess.Network.PacketSyncLayerStatus;
import com.abyess.config.ModConfig; // 确保导入 ModConfig
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

@SideOnly(Side.CLIENT)
public class CurseHUD {
    private static final List<PacketSyncLayerStatus.LayerStatus> currentStatuses = new ArrayList<>();
    private static final Minecraft mc = Minecraft.getMinecraft();

    /**
     * 更新当前显示的层级状态列表。
     * 这个方法会在服务端同步数据到客户端时被调用。
     * @param statuses 最新的层级状态列表
     */
    public static void updateStatuses(List<PacketSyncLayerStatus.LayerStatus> statuses) {
        currentStatuses.clear();
        currentStatuses.addAll(statuses);
    }

    /**
     * 订阅游戏内叠加层渲染事件，用于绘制诅咒HUD。
     * 这个方法会在游戏每次渲染HUD时触发。
     * @param event 渲染游戏叠加层事件
     */
    @SubscribeEvent
    public void onRenderGameOverlay(RenderGameOverlayEvent.Post event) {
        // 确保只在绘制文本叠加层时执行，避免重复绘制或影响其他元素
        if (event.getType() != RenderGameOverlayEvent.ElementType.TEXT) return;

        // 如果玩家不存在或当前没有需要显示的层级状态，则不进行绘制
        if (mc.player == null || currentStatuses.isEmpty()) return;

        // 获取HUD的配置数据
        ModConfig.HudConfig hudConfig = ModConfig.getConfigData().getHudConfig();

        // 检查HUD是否全局启用
        if (!hudConfig.isEnabled()) {
            return;
        }

        // 获取当前屏幕的缩放分辨率，用于计算实际像素坐标
        ScaledResolution res = new ScaledResolution(mc);
        int screenWidth = res.getScaledWidth();
        int screenHeight = res.getScaledHeight();

        // 根据配置中的百分比计算HUD的起始X和Y像素坐标
        // 例如，0.05f * screenWidth 意味着距离左边5%的位置
        int x = (int) (hudConfig.getXPercentage() * screenWidth);
        int y = (int) (hudConfig.getYPercentage() * screenHeight);

        // 使用临时变量来更新Y坐标，以便每个层级信息都能独立绘制在新的一行
        int currentY = y;

        // 遍历所有当前活跃的层级状态
        for (PacketSyncLayerStatus.LayerStatus status : currentStatuses) {
            // 检查该特定层级是否在HUD上启用
            if (!status.hudEnabled) {
                continue; // 如果未启用，则跳过该层级
            }

            // 解析层级名称的颜色，如果解析失败则默认为白色
            int color;
            try {
                color = Color.decode(status.color).getRGB();
            } catch (Exception e) {
                color = 0xFFFFFF; // 默认白色
            }

            // 绘制层级名称（如“Edge of the Abyss”）
            // 使用drawStringWithShadow以获得更好的可见性（带阴影的文字）
            mc.fontRenderer.drawStringWithShadow(status.layerName, x, currentY, color);

            // ✨根据该层级的 'showTimeDistance' 配置决定是否显示时间/距离阈值
            if (status.showTimeDistance) {
                String statusText;
                // 根据层级是否连续触发来格式化显示文本
                if (status.isContinuous) {
                    // 连续层级通常显示时间（秒）
                    statusText = String.format("%ds / %ds", Math.round(status.value), status.maxValue);
                } else {
                    // 非连续层级通常显示距离或特定数值
                    statusText = String.format("%d / %d", Math.round(status.value), status.maxValue);
                }

                // 计算层级名称的宽度，以便将状态信息放置在其右侧
                int nameWidth = mc.fontRenderer.getStringWidth(status.layerName);

                // 绘制状态信息（例如“10s / 100s”或“50 / 200”）
                // 放置在层级名称右侧，并添加少量间距 (+5)
                mc.fontRenderer.drawStringWithShadow(statusText, x + nameWidth + 5, currentY, 0xAAAAAA); // 浅灰色文本
            }

            // 增加行间距，为下一个层级的信息留出空间
            currentY += 10;
        }
    }
}