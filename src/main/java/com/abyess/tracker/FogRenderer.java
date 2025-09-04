package com.abyess.tracker;

import com.abyess.config.ModConfig;
import com.abyess.Network.PacketSyncLayerStatus;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.client.event.EntityViewRenderEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.lwjgl.opengl.GL11;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.lang.reflect.Field;

// 导入 Minecraft 客户端选项，用于获取渲染距离
import net.minecraft.client.Minecraft;
// 移除对 TextComponentString 的导入，因为不再发送游戏内消息
// import net.minecraft.util.text.TextComponentString;

@SideOnly(Side.CLIENT)
public class FogRenderer {

    public enum FogControlMode {
        DISABLED,
        CONFIG_BASED,
        MANUAL_OVERRIDE
    }

    private static List<ModConfig.LayerConfig> currentActiveConfigLayers = new ArrayList<>();
    private static FogControlMode currentFogControlMode = FogControlMode.CONFIG_BASED;

    // 手动覆盖参数
    private static float manualFogRed = 1.0F;
    private static float manualFogGreen = 1.0F;
    private static float manualFogBlue = 1.0F;
    private static float manualFogDensity = 0.05F; // 用于EXP2
    private static String manualFogType = "EXP2"; // 存储手动雾效类型："EXP2" 或 "LINEAR"
    private static float manualFogStartPercent = 0.0F; // 用于LINEAR，存储百分比
    private static float manualFogEndPercent = 1.0F;   // 用于LINEAR，存储百分比

    // 混合雾效参数 - 已更新以支持类型和线性参数
    private static float blendedFogRed = 1.0F;
    private static float blendedFogGreen = 1.0F;
    private static float blendedFogBlue = 1.0F;
    private static float blendedFogDensity = 0.0F;
    private static boolean isBlendedFogActive = false;
    private static String blendedFogType = "EXP2"; // 新增：存储混合后的雾效类型
    private static float blendedFogStart = 0.0F;   // 新增：存储混合后的线性雾开始百分比
    private static float blendedFogEnd = 1.0F;     // 新增：存储混合后的线性雾结束百分比

    // 原版雾效状态缓存
    private static int originalFogMode = GL11.GL_EXP;
    private static float originalFogDensity = 1.0f;
    private static float originalFogStart = 0.0f;
    private static float originalFogEnd = 1.0f;
    private static float originalFogRed = 0.0f;
    private static float originalFogGreen = 0.0f;
    private static float originalFogBlue = 0.0f;

    // 反射访问GlStateManager的fogState
    private static Object fogState = null;
    private static Field fogModeField = null;
    private static Field fogDensityField = null;
    private static Field fogStartField = null;
    private static Field fogEndField = null;

    static {
        try {
            // 获取GlStateManager的fogState字段
            Field fogStateField = GlStateManager.class.getDeclaredField("fogState");
            fogStateField.setAccessible(true);
            fogState = fogStateField.get(null);

            // 获取fogState内部的字段
            fogModeField = fogState.getClass().getDeclaredField("mode");
            fogModeField.setAccessible(true);

            fogDensityField = fogState.getClass().getDeclaredField("density");
            fogDensityField.setAccessible(true);

            fogStartField = fogState.getClass().getDeclaredField("start");
            fogStartField.setAccessible(true);

            fogEndField = fogState.getClass().getDeclaredField("end");
            fogEndField.setAccessible(true);

        } catch (Exception e) {
            System.err.println("[FogRenderer] Failed to access GlStateManager fogState: " + e.getMessage());
        }
    }

    public static void setFogControlMode(FogControlMode mode) {
        currentFogControlMode = mode;

        if (mode != FogControlMode.CONFIG_BASED) {
            currentActiveConfigLayers.clear();
            isBlendedFogActive = false;
            // 当不是 CONFIG_BASED 模式时，重置混合雾效参数，避免残留
            blendedFogType = "EXP2";
            blendedFogDensity = 0.0F;
            blendedFogStart = 0.0F;
            blendedFogEnd = 1.0F;
        }
        // 移除游戏内消息
        // if (Minecraft.getMinecraft().player != null) {
        //     Minecraft.getMinecraft().player.sendMessage(new TextComponentString("§b[Fog]§f 雾效模式: §e" + mode.name()));
        // }
    }

    /**
     * 设置手动指数雾 (EXP2) 参数
     * @param r 红色分量 (0.0-1.0)
     * @param g 绿色分量 (0.0-1.0)
     * @param b 蓝色分量 (0.0-1.0)
     * @param density 雾密度 (0.0-1.0)
     */
    public static void setManualFogExp2(float r, float g, float b, float density) {
        manualFogRed = Math.max(0.0F, Math.min(1.0F, r));
        manualFogGreen = Math.max(0.0F, Math.min(1.0F, g));
        manualFogBlue = Math.max(0.0F, Math.min(1.0F, b));
        manualFogDensity = Math.max(0.0F, density);
        manualFogType = "EXP2";

        setFogControlMode(FogControlMode.MANUAL_OVERRIDE);
        // 移除游戏内消息
        // if (Minecraft.getMinecraft().player != null) {
        //     Minecraft.getMinecraft().player.sendMessage(new TextComponentString(
        //         "§b[Fog]§f 手动EXP2雾: 颜色[" + String.format("%.2f", r) + "," + String.format("%.2f", g) + "," + String.format("%.2f", b) + "], 密度=" + String.format("%.3f", density)
        //     ));
        // }
    }

    /**
     * 设置手动线性雾 (LINEAR) 参数，使用渲染距离的百分比。
     * @param r 红色分量 (0.0-1.0)
     * @param g 绿色分量 (0.0-1.0)
     * @param b 蓝色分量 (0.0-1.0)
     * @param startPercent 雾开始距离的渲染距离百分比 (0.0-1.0)
     * @param endPercent 雾结束距离的渲染距离百分比 (0.0-1.0)
     */
    public static void setManualFogLinear(float r, float g, float b, float startPercent, float endPercent) {
        manualFogRed = Math.max(0.0F, Math.min(1.0F, r));
        manualFogGreen = Math.max(0.0F, Math.min(1.0F, g));
        manualFogBlue = Math.max(0.0F, Math.min(1.0F, b));
        manualFogType = "LINEAR";
        manualFogStartPercent = Math.max(0.0F, Math.min(1.0F, startPercent)); // 确保在 0-1 之间
        manualFogEndPercent = Math.max(0.0F, Math.min(1.0F, endPercent));     // 确保在 0-1 之间

        setFogControlMode(FogControlMode.MANUAL_OVERRIDE);
        // 移除游戏内消息
        // if (Minecraft.getMinecraft().player != null) {
        //     Minecraft.getMinecraft().player.sendMessage(new TextComponentString(
        //         "§b[Fog]§f 手动LINEAR雾: 颜色[" + String.format("%.2f", r) + "," + String.format("%.2f", g) + "," + String.format("%.2f", b) + "], 起始百分比=" + String.format("%.2f", startPercent) + ", 结束百分比=" + String.format("%.2f", endPercent)
        //     ));
        // }
    }

    /**
     * 获取当前的渲染距离（以方块为单位）。
     * Minecraft 的渲染距离是 chunk 为单位的，1 chunk = 16 方块。
     * @return 当前渲染距离（方块数）
     */
    private static float getCurrentRenderDistance() {
        // Minecraft.getMinecraft().gameSettings.renderDistanceChunks 获取的是 chunk 数
        // 每个 chunk 是 16x16x16 方块
        return Minecraft.getMinecraft().gameSettings.renderDistanceChunks * 16.0F;
    }

    /**
     * 更新激活的雾效层
     */
    public static void updateActiveFogLayers(List<PacketSyncLayerStatus.LayerStatus> layerStatuses) {
        // 只有在 CONFIG_BASED 模式下才处理层配置
        if (currentFogControlMode != FogControlMode.CONFIG_BASED) {
            currentActiveConfigLayers.clear();
            isBlendedFogActive = false;
            return;
        }

        ModConfig.ConfigData configData = ModConfig.getConfigData();
        if (configData == null) {
            currentActiveConfigLayers.clear();
            isBlendedFogActive = false;
            return;
        }

        List<ModConfig.LayerConfig> matchingConfigLayers = new ArrayList<>();
        Map<Integer, ModConfig.DimensionConfig> dimConfigs = configData.getDimensions();

        for (PacketSyncLayerStatus.LayerStatus status : layerStatuses) {
            ModConfig.DimensionConfig dimConfig = dimConfigs.get(status.dimensionId);

            if (dimConfig != null && dimConfig.isEnabled()) {
                List<ModConfig.LayerConfig> layers = dimConfig.getLayers();
                if (layers != null) {
                    for (ModConfig.LayerConfig configLayer : layers) {
                        // 检查层名称是否匹配，并且该层的雾效是否启用
                        if (configLayer.getName().equals(status.layerName) && configLayer.isFogEnabled()) {
                            matchingConfigLayers.add(configLayer);
                            break;
                        }
                    }
                }
            }
        }

        // 更新当前活跃的配置层列表
        currentActiveConfigLayers = matchingConfigLayers;

        // 计算混合雾效参数
        calculateBlendedFogParameters();
    }

    /**
     * 计算混合雾效参数
     * 已修复：现在会根据活跃层的类型决定混合雾效类型，并处理线性雾的参数。
     */
    private static void calculateBlendedFogParameters() {
        if (currentActiveConfigLayers.isEmpty()) {
            isBlendedFogActive = false;
            blendedFogDensity = 0.0F;
            blendedFogRed = 1.0F;
            blendedFogGreen = 1.0F;
            blendedFogBlue = 1.0F;
            blendedFogType = "EXP2"; // 默认回指数雾
            blendedFogStart = 0.0F;
            blendedFogEnd = 1.0F;
            // 移除游戏内消息
            // if (Minecraft.getMinecraft().player != null) {
            //     Minecraft.getMinecraft().player.sendMessage(new TextComponentString("§b[Fog]§f 无活跃雾效层，恢复默认。"));
            // }
            return;
        }

        isBlendedFogActive = true;
        float maxDensity = 0.0F;
        float totalWeightedRed = 0.0F;
        float totalWeightedGreen = 0.0F;
        float totalWeightedBlue = 0.0F;
        float totalDensitySum = 0.0F;

        // 新增：用于判断是否存在指数雾效层，指数雾优先
        boolean anyExponentialFogActive = false; // 改为检查指数雾
        float minLinearStart = 1.0F; // 线性开始的最小百分比 (最靠近玩家)
        float maxLinearEnd = 0.0F;   // 线性结束的最大百分比 (最远离玩家)

        for (ModConfig.LayerConfig layer : currentActiveConfigLayers) {
            float layerDensity = layer.getFogDensity();
            float[] layerColor = layer.getFogColorRGB();

            // 颜色和密度的混合（与之前类似，取最大密度，颜色加权平均）
            if (layerDensity > maxDensity) {
                maxDensity = layerDensity;
            }

            if (layerColor != null && layerColor.length >= 3) {
                totalWeightedRed += layerColor[0] * layerDensity;
                totalWeightedGreen += layerColor[1] * layerDensity;
                totalWeightedBlue += layerColor[2] * layerDensity;
                totalDensitySum += layerDensity;
            }

            // 处理雾效类型和参数
            if ("EXP2".equalsIgnoreCase(layer.getFogType())) {
                anyExponentialFogActive = true; // 只要有一个是指数雾，就标记为true
            } else if ("LINEAR".equalsIgnoreCase(layer.getFogType())) {
                // 对于线性雾，取所有激活层中最小的 start 和最大的 end，以覆盖更广的范围
                if (layer.getFogStart() < minLinearStart) {
                    minLinearStart = layer.getFogStart();
                }
                if (layer.getFogEnd() > maxLinearEnd) {
                    maxLinearEnd = layer.getFogEnd();
                }
            }
        }

        // 确定最终的混合雾效类型和参数
        // **修改：指数雾优先**
        if (anyExponentialFogActive) { // 如果存在任何一个指数雾层，则最终应用指数雾
            blendedFogType = "EXP2";
            blendedFogDensity = maxDensity; // 沿用之前取最大密度
            blendedFogStart = 0.0F; // 线性参数不相关，重置为默认值
            blendedFogEnd = 1.0F;
        } else { // 只有所有活跃层都是线性雾时，才应用线性雾
            blendedFogType = "LINEAR";
            blendedFogStart = minLinearStart;
            blendedFogEnd = maxLinearEnd;
            blendedFogDensity = 0.0F; // 线性雾不使用密度
        }

        // 计算混合颜色
        if (totalDensitySum > 0) {
            blendedFogRed = totalWeightedRed / totalDensitySum;
            blendedFogGreen = totalWeightedGreen / totalDensitySum;
            blendedFogBlue = totalWeightedBlue / totalDensitySum;
        } else {
            // 如果所有活跃层密度都为0，则默认白色
            blendedFogRed = 1.0F;
            blendedFogGreen = 1.0F;
            blendedFogBlue = 1.0F;
        }

        // 移除游戏内消息
        // if (Minecraft.getMinecraft().player != null) {
        //     String debugMsg;
        //     if (blendedFogType.equals("EXP2")) {
        //         debugMsg = String.format("§b[Fog]§f 混合雾: 类型§eEXP2§f, 颜色[%.2f,%.2f,%.2f], 密度=%.3f",
        //                                  blendedFogRed, blendedFogGreen, blendedFogBlue, blendedFogDensity);
        //     } else {
        //         debugMsg = String.format("§b[Fog]§f 混合雾: 类型§aLINEAR§f, 颜色[%.2f,%.2f,%.2f], 起始百分比=%.2f, 结束百分比=%.2f",
        //                                  blendedFogRed, blendedFogGreen, blendedFogBlue, blendedFogStart, blendedFogEnd);
        //     }
        //     Minecraft.getMinecraft().player.sendMessage(new TextComponentString(debugMsg));
        // }
    }

    /**
     * 保存当前原版雾效状态
     */
    private static void saveOriginalFogState() {
        if (fogState == null) return;

        try {
            originalFogMode = fogModeField.getInt(fogState);
            originalFogDensity = fogDensityField.getFloat(fogState);
            originalFogStart = fogStartField.getFloat(fogState);
            originalFogEnd = fogEndField.getFloat(fogState);

        } catch (Exception e) {
            System.err.println("[FogRenderer] Failed to save original fog state: " + e.getMessage());
        }
    }

    /**
     * 恢复原版雾效状态
     */
    private static void restoreOriginalFogState() {
        if (fogState == null) return;

        try {
            // 恢复OpenGL状态
            GL11.glFogi(GL11.GL_FOG_MODE, originalFogMode);
            GL11.glFogf(GL11.GL_FOG_DENSITY, originalFogDensity);
            GL11.glFogf(GL11.GL_FOG_START, originalFogStart);
            GL11.glFogf(GL11.GL_FOG_END, originalFogEnd);

            // 恢复GlStateManager内部状态
            fogModeField.setInt(fogState, originalFogMode);
            fogDensityField.setFloat(fogState, originalFogDensity);
            fogStartField.setFloat(fogState, originalFogStart);
            fogEndField.setFloat(fogState, originalFogEnd);

        } catch (Exception e) {
            System.err.println("[FogRenderer] Failed to restore original fog state: " + e.getMessage());
        }
    }

    /**
     * 使用高优先级保存原始状态
     */
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onPreRenderFog(EntityViewRenderEvent.RenderFogEvent event) {
        if (!(event.getEntity() instanceof EntityPlayer)) {
            return;
        }

        // 在渲染前保存原版状态
        saveOriginalFogState();
    }

    /**
     * 使用低优先级应用雾效覆盖
     * 已修复：CONFIG_BASED 模式现在会根据 blendedFogType 应用正确的雾效类型和参数。
     */
    @SubscribeEvent(priority = EventPriority.LOW)
    public void onRenderFogDensity(EntityViewRenderEvent.RenderFogEvent event) {
        if (!(event.getEntity() instanceof EntityPlayer)) {
            return;
        }

        switch (currentFogControlMode) {
            case DISABLED:
                // 恢复原版雾效
                restoreOriginalFogState();
                break;

            case MANUAL_OVERRIDE:
                // 根据 manualFogType 应用不同的雾效
                if ("EXP2".equalsIgnoreCase(manualFogType)) {
                    GlStateManager.setFog(GlStateManager.FogMode.EXP2);
                    GlStateManager.setFogDensity(manualFogDensity);
                } else if ("LINEAR".equalsIgnoreCase(manualFogType)) {
                    // 获取当前渲染距离
                    float renderDistance = getCurrentRenderDistance();
                    // 根据百分比计算 START 和 END
                    float calculatedStart = renderDistance * manualFogStartPercent;
                    float calculatedEnd = renderDistance * manualFogEndPercent;

                    GlStateManager.setFog(GlStateManager.FogMode.LINEAR);
                    GlStateManager.setFogStart(calculatedStart);
                    GlStateManager.setFogEnd(calculatedEnd);

                    // 确保 Start 不大于 End，并且至少有一个最小间隔
                    if (calculatedStart >= calculatedEnd) {
                        calculatedEnd = calculatedStart + 1.0F; // 避免渲染问题
                    }

                    // 移除游戏内消息
                    // if (Minecraft.getMinecraft().player != null) {
                    //     Minecraft.getMinecraft().player.sendMessage(new TextComponentString(
                    //         String.format("§b[Fog]§f 手动LINEAR: 视距=%.0f, 计算起始=%.1f, 结束=%.1f",
                    //             renderDistance, calculatedStart, calculatedEnd)
                    //     ));
                    // }
                }
                break;

            case CONFIG_BASED:
                if (isBlendedFogActive) {
                    // 根据 blendedFogType 应用不同的雾效
                    if ("EXP2".equalsIgnoreCase(blendedFogType)) {
                        GlStateManager.setFog(GlStateManager.FogMode.EXP2);
                        GlStateManager.setFogDensity(blendedFogDensity);
                    } else if ("LINEAR".equalsIgnoreCase(blendedFogType)) {
                        float renderDistance = getCurrentRenderDistance();
                        // 这里使用混合后的线性雾参数 (blendedFogStart, blendedFogEnd)
                        float calculatedStart = renderDistance * blendedFogStart;
                        float calculatedEnd = renderDistance * blendedFogEnd;

                        GlStateManager.setFog(GlStateManager.FogMode.LINEAR);
                        GlStateManager.setFogStart(calculatedStart);
                        GlStateManager.setFogEnd(calculatedEnd);

                        if (calculatedStart >= calculatedEnd) {
                            calculatedEnd = calculatedStart + 1.0F; // 确保结束距离大于开始距离
                        }

                        // 移除游戏内消息
                        // if (Minecraft.getMinecraft().player != null) {
                        //     Minecraft.getMinecraft().player.sendMessage(new TextComponentString(
                        //         String.format("§b[Fog]§f 配置LINEAR: 视距=%.0f, 计算起始=%.1f, 结束=%.1f",
                        //             renderDistance, calculatedStart, calculatedEnd)
                        //     ));
                        // }
                    }
                } else {
                    // 恢复原版雾效
                    restoreOriginalFogState();
                }
                break;
        }
    }

    /**
     * 使用高优先级保存原始颜色
     */
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onPreRenderFogColor(EntityViewRenderEvent.FogColors event) {
        if (!(event.getEntity() instanceof EntityPlayer)) {
            return;
        }

        // 保存原版颜色
        originalFogRed = event.getRed();
        originalFogGreen = event.getGreen();
        originalFogBlue = event.getBlue();
    }

    /**
     * 使用低优先级覆盖雾效颜色
     */
    @SubscribeEvent(priority = EventPriority.LOW)
    public void onRenderFogColor(EntityViewRenderEvent.FogColors event) {
        if (!(event.getEntity() instanceof EntityPlayer)) {
            return;
        }

        switch (currentFogControlMode) {
            case DISABLED:
                // 恢复原版颜色
                event.setRed(originalFogRed);
                event.setGreen(originalFogGreen);
                event.setBlue(originalFogBlue);
                break;

            case MANUAL_OVERRIDE:
                event.setRed(manualFogRed);
                event.setGreen(manualFogGreen);
                event.setBlue(manualFogBlue);
                break;

            case CONFIG_BASED:
                if (isBlendedFogActive) {
                    event.setRed(blendedFogRed);
                    event.setGreen(blendedFogGreen);
                    event.setBlue(blendedFogBlue);
                } else {
                    // 恢复原版颜色
                    event.setRed(originalFogRed);
                    event.setGreen(originalFogGreen);
                    event.setBlue(originalFogBlue);
                }
                break;
        }
    }

    public static void forceDisableCustomFog() {
        setFogControlMode(FogControlMode.DISABLED);
        // 移除游戏内消息
        // if (Minecraft.getMinecraft().player != null) {
        //     Minecraft.getMinecraft().player.sendMessage(new TextComponentString("§b[Fog]§f 自定义雾效已禁用。"));
        // }
    }

    public static FogControlMode getCurrentFogControlMode() {
        return currentFogControlMode;
    }

    public static boolean isCustomFogActive() {
        if (currentFogControlMode == FogControlMode.DISABLED) {
            return false;
        }
        if (currentFogControlMode == FogControlMode.MANUAL_OVERRIDE) {
            return true;
        }
        // CONFIG_BASED 模式下只有当 isBlendedFogActive 为 true 时才算活跃
        return isBlendedFogActive;
    }
}