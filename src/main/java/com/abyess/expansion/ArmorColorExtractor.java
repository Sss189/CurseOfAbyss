//package com.abyess.expansion;
//
//import net.minecraft.client.Minecraft;
//import net.minecraft.item.Item;
//import net.minecraft.item.ItemArmor;
//import net.minecraft.item.ItemStack;
//import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
//import net.minecraftforge.fml.common.gameevent.TickEvent;
//import net.minecraftforge.fml.relauncher.Side;
//import net.minecraftforge.fml.relauncher.SideOnly;
//import net.minecraftforge.fml.common.registry.ForgeRegistries;
//
//import java.awt.Color;
//import java.awt.image.BufferedImage;
//import java.nio.ByteBuffer;
//import java.util.HashMap;
//import java.util.Map;
//
//import org.lwjgl.BufferUtils;
//import org.lwjgl.opengl.GL11;
//import org.lwjgl.opengl.GL14;
//import org.lwjgl.opengl.GL30;
//
//import com.google.gson.Gson;
//import com.google.gson.GsonBuilder;
//import com.google.gson.JsonObject;
//
//import java.io.File;
//import java.io.FileWriter;
//import java.io.IOException;
//import java.nio.file.Path;
//import java.nio.file.Paths;
//
//// 确保导入 ModConfig
//import com.abyess.config.ModConfig;
//
///**
// * 客户端工具类，通过将盔甲图标渲染到 FBO 并读取像素来提取其主色调。
// * 旨在游戏启动且渲染环境稳定后运行一次。
// *
// * 改进：
// * 1. 使用加权众数逻辑来选择主色。
// * 2. 过滤掉透明、纯黑和近黑色的像素。
// * 3. 对未被过滤的像素根据亮度和饱和度应用动态权重。
// * 4. 增强中心区域像素的权重，以减少边缘/阴影的影响。
// * 5. 初始颜色提取后，对头盔和靴子应用后处理步骤：
// * 如果存在对应的胸甲颜色，则使用该颜色以保持套装内的一致性。
// * 6. 如果只找到透明或近黑色的像素，则回退到纯黑色。
// * 7. 新增：添加了启动延迟，以避免因渲染引擎未完全准备就绪而导致的竞态条件。
// * 8. 新增：如果 ModConfig 中存在该盔甲的自定义颜色，则跳过其自动颜色提取。
// */
//@SideOnly(Side.CLIENT)
//public class ArmorColorExtractor {
//
//    // 存储提取的盔甲颜色：物品注册名 -> ARGB 颜色。
//    private static final Map<String, Integer> extractedArmorColors = new HashMap<>();
//    // 确保颜色提取只运行一次的标志。
//    private static boolean colorsExtracted = false;
//
//    // --- 新增：用于解决竞态条件的延迟逻辑 ---
//    // 用于在世界加载后稍微延迟提取过程的计数器。
//    private static int tickDelayCounter = 0;
//    private static final int REQUIRED_TICKS_DELAY = 20;
//
//    // OpenGL FBO (帧缓冲对象) 相关 ID
//    private static int FBO_ID = 0;
//    private static int TEXTURE_ID = 0;
//    private static int DEPTH_RENDER_BUFFER_ID = 0;
//    // 用于将盔甲图标渲染到 FBO 的尺寸。
//    private static final int ICON_RENDER_SIZE = 32;
//
//    public ArmorColorExtractor() {
//        // System.out.println("[Curse of Abyss - ArmorColorExtractor]: Instance created.");
//    }
//
//    /**
//     * 检索所有已提取的盔甲颜色映射。
//     * @return 从盔甲注册名到其平均颜色的映射。
//     */
//    public static Map<String, Integer> getExtractedArmorColors() {
//        return extractedArmorColors;
//    }
//
//    /**
//     * 检查盔甲颜色是否已成功提取。
//     * @return 如果颜色已提取则为 true，否则为 false。
//     */
//    public static boolean hasExtractedColors() {
//        return colorsExtracted;
//    }
//
//    /**
//     * 客户端 Tick 事件监听器。在每个客户端 Tick 开始时检查是否需要执行颜色提取。
//     * @param event 客户端 Tick 事件。
//     */
//    @SubscribeEvent
//    public void onClientTick(TickEvent.ClientTickEvent event) {
//        // 确保此逻辑只运行一次。
//        if (event.phase == TickEvent.Phase.START && !colorsExtracted) {
//            Minecraft mc = Minecraft.getMinecraft();
//
//            // *** 新增的性能优化检查 ***
//            // 如果自动着色未启用，则直接退出，不执行颜色提取。
//            if (!ModConfig.getConfigData().getHudConfig().isEnableAutoArmorColoring()) {
//                // System.out.println("[Curse of Abyss - ArmorColorExtractor]: Auto Armor Coloring is disabled. Skipping color extraction for performance.");
//                colorsExtracted = true; // 标记为已完成，防止下次 Tick 再次尝试。
//                return;
//            }
//
//            // 检查游戏是否处于可以开始我们流程的状态（客户端、物品渲染器、世界均存在，且当前无 GUI）。
//            if (mc != null && mc.getRenderItem() != null && mc.world != null && mc.currentScreen == null) {
//
//                // 增加我们的延迟计数器。
//                if (tickDelayCounter <= REQUIRED_TICKS_DELAY) {
//                    tickDelayCounter++;
//                    return; // 等待直到延迟结束。
//                }
//
//                // --- 延迟已经结束，现在我们可以安全地运行提取逻辑了 ---
//                System.out.println("[Curse of Abyss - ArmorColorExtractor]: Starting 2D GUI icon color extraction (via FBO simulation)...");
//                colorsExtracted = true; // 尽早设置此标志以防止重复进入。
//
//                initFBO();
//                if (FBO_ID != 0) {
//                    extractAllArmorColorsViaFBO();
//                    // 初始提取后，应用胸甲颜色以保持一致性。
//                    // 注意：这里仍然会检查是否有自定义颜色，以避免覆盖用户设置。
//                    applyChestplateColorsToHeadAndFeet();
//                    // 在这里调用保存到 JSON 的方法
//                    saveColorsToJson();
//                } else {
//                    System.err.println("[Curse of Abyss - ArmorColorExtractor]: ERROR: Failed to initialize FBO. GUI icon colors may not be extracted for all armor.");
//                }
//                cleanupFBO();
//                System.out.println("[Curse of Abyss - ArmorColorExtractor]: GUI icon color extraction finished. Extracted " + extractedArmorColors.size() + " colors.");
//            } else {
//                // 如果玩家离开世界或打开GUI，重置计数器。
//                tickDelayCounter = 0;
//            }
//        }
//    }
//
//    /**
//     * 初始化帧缓冲对象 (FBO) 及其附件（纹理和深度渲染缓冲）。
//     */
//    private static void initFBO() {
//        FBO_ID = GL30.glGenFramebuffers();
//        GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, FBO_ID);
//
//        TEXTURE_ID = GL11.glGenTextures();
//        GL11.glBindTexture(GL11.GL_TEXTURE_2D, TEXTURE_ID);
//        GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL11.GL_RGBA, ICON_RENDER_SIZE, ICON_RENDER_SIZE, 0, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, (ByteBuffer) null);
//        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR);
//        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);
//        GL30.glFramebufferTexture2D(GL30.GL_FRAMEBUFFER, GL30.GL_COLOR_ATTACHMENT0, GL11.GL_TEXTURE_2D, TEXTURE_ID, 0);
//
//        DEPTH_RENDER_BUFFER_ID = GL30.glGenRenderbuffers();
//        GL30.glBindRenderbuffer(GL30.GL_RENDERBUFFER, DEPTH_RENDER_BUFFER_ID);
//        GL30.glRenderbufferStorage(GL30.GL_RENDERBUFFER, GL14.GL_DEPTH_COMPONENT24, ICON_RENDER_SIZE, ICON_RENDER_SIZE);
//        GL30.glFramebufferRenderbuffer(GL30.GL_FRAMEBUFFER, GL30.GL_DEPTH_ATTACHMENT, GL30.GL_RENDERBUFFER, DEPTH_RENDER_BUFFER_ID);
//
//        int fboStatus = GL30.glCheckFramebufferStatus(GL30.GL_FRAMEBUFFER);
//        if (fboStatus != GL30.GL_FRAMEBUFFER_COMPLETE) {
//            System.err.println("[Curse of Abyss - ArmorColorExtractor]: FBO creation failed with status: " + fboStatus);
//            FBO_ID = 0; // 标记 FBO 创建失败
//        }
//
//        GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, 0); // 恢复默认 FBO
//    }
//
//    /**
//     * 清理 FBO 及其关联的 OpenGL 资源。
//     */
//    private static void cleanupFBO() {
//        if (FBO_ID != 0) {
//            GL30.glDeleteFramebuffers(FBO_ID);
//            GL11.glDeleteTextures(TEXTURE_ID);
//            GL30.glDeleteRenderbuffers(DEPTH_RENDER_BUFFER_ID);
//            FBO_ID = 0;
//            TEXTURE_ID = 0;
//            DEPTH_RENDER_BUFFER_ID = 0;
//        }
//    }
//
//    /**
//     * 辅助方法：计算给定 ARGB 颜色值的亮度 (0-1)。
//     * 使用 HSB (HSV) 颜色空间的 'B' (Brightness) 或 'V' (Value) 分量。
//     */
//    private static float calculateBrightnessHSB(int argb) {
//        int r = (argb >> 16) & 0xFF;
//        int g = (argb >> 8) & 0xFF;
//        int b = (argb >> 0) & 0xFF;
//        float[] hsb = Color.RGBtoHSB(r, g, b, null);
//        return hsb[2]; // 返回亮度 (Value) 分量，范围 0.0 - 1.0
//    }
//
//    /**
//     * 辅助方法：计算给定 ARGB 颜色值的饱和度 (0-1)。
//     * 使用 HSB (HSV) 颜色空间的 'S' (Saturation) 分量。
//     */
//    private static float calculateSaturationHSB(int argb) {
//        int r = (argb >> 16) & 0xFF;
//        int g = (argb >> 8) & 0xFF;
//        int b = (argb >> 0) & 0xFF;
//        float[] hsb = Color.RGBtoHSB(r, g, b, null);
//        return hsb[1]; // 返回饱和度 (Saturation) 分量，范围 0.0 - 1.0
//    }
//
//    /**
//     * 遍历所有盔甲物品，将它们渲染到 FBO，并提取其主色。
//     * 策略：过滤掉透明、纯黑和近黑色的像素。
//     * 对剩余像素，根据亮度、饱和度和位置（中心区域）应用权重。
//     * 计算加权众数（按权重计最常见的颜色），并在找不到有效像素时回退。
//     * **修改：现在即使 ModConfig 中存在该盔甲的自定义颜色，也会进行自动颜色提取。**
//     */
//    private static void extractAllArmorColorsViaFBO() {
//        Minecraft mc = Minecraft.getMinecraft();
//
//        // 存储当前的 FBO 绑定，以便稍后恢复。
//        int currentFBO = GL11.glGetInteger(GL30.GL_FRAMEBUFFER_BINDING);
//
//        // 启用标准的 GUI 物品光照和深度测试。
//        net.minecraft.client.renderer.RenderHelper.enableGUIStandardItemLighting();
//        GL11.glEnable(GL11.GL_DEPTH_TEST);
//
//        // --- 像素过滤阈值 ---
//        final int ALPHA_THRESHOLD = 5; // Alpha <= 此值的像素被视为透明。
//        // RGB 分量必须全部 <= 此值才被视为“近黑色”。
//        final int NEAR_BLACK_RGB_THRESHOLD = 15;
//
//        // 如果只找到黑色/透明像素，则使用的回退颜色。
//        final int FALLBACK_TO_BLACK = 0xFF000000; // 纯黑，完全不透明。
//
//        // --- 权重因子（用于未被过滤的像素） ---
//        final double BRIGHTNESS_WEIGHT_FACTOR = 2.5; // 强调更亮的颜色。
//        final double SATURATION_WEIGHT_FACTOR = 1.5; // 强调更鲜艳的颜色。
//
//        // 用于应用额外权重的核心区域尺寸。
//        final int CORE_REGION_SIZE = 16;
//        // 为核心区域像素提供的额外权重提升。
//        final double CORE_REGION_WEIGHT_BOOST = 1.0;
//
//        try {
//            GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, FBO_ID);
//            GL11.glViewport(0, 0, ICON_RENDER_SIZE, ICON_RENDER_SIZE);
//
//            // 删除了获取 customColors 的代码，因为现在不再跳过提取逻辑。
//            // Map<String, String> customColors = ModConfig.getCustomArmorColors();
//
//            for (Item item : ForgeRegistries.ITEMS) {
//                if (item instanceof ItemArmor) {
//                    ItemStack itemStack = new ItemStack(item);
//                    String registryName = item.getRegistryName().toString();
//
//                    // *** 核心修改点：删除了跳过自定义颜色盔甲的逻辑 ***
//                    // if (customColors.containsKey(registryName)) {
//                    //     // System.out.println("[Curse of Abyss - ArmorColorExtractor]: Skipping extraction for " + registryName + " (custom color found).");
//                    //     continue; // 跳过此盔甲的颜色提取
//                    // }
//
//                    GL11.glClear(GL11.GL_COLOR_BUFFER_BIT | GL11.GL_DEPTH_BUFFER_BIT);
//                    mc.getRenderItem().renderItemAndEffectIntoGUI(itemStack, 0, 0);
//
//                    ByteBuffer buffer = BufferUtils.createByteBuffer(ICON_RENDER_SIZE * ICON_RENDER_SIZE * 4);
//                    GL11.glReadPixels(0, 0, ICON_RENDER_SIZE, ICON_RENDER_SIZE, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, buffer);
//
//                    BufferedImage image = new BufferedImage(ICON_RENDER_SIZE, ICON_RENDER_SIZE, BufferedImage.TYPE_INT_ARGB);
//                    for (int x = 0; x < ICON_RENDER_SIZE; x++) {
//                        for (int y = 0; y < ICON_RENDER_SIZE; y++) {
//                            // OpenGL 从下往上读取像素，所以需要翻转 Y 轴。
//                            int i = (x + (ICON_RENDER_SIZE - y - 1) * ICON_RENDER_SIZE) * 4;
//                            int r = buffer.get(i) & 0xFF;
//                            int g = buffer.get(i + 1) & 0xFF;
//                            int b = buffer.get(i + 2) & 0xFF;
//                            int a = buffer.get(i + 3) & 0xFF;
//                            image.setRGB(x, y, (a << 24) | (r << 16) | (g << 8) | b);
//                        }
//                    }
//
//                    Map<Integer, Double> weightedColorCounts = new HashMap<>();
//
//                    // 计算中心区域的边界。
//                    int coreStartX = (ICON_RENDER_SIZE - CORE_REGION_SIZE) / 2;
//                    int coreEndX = coreStartX + CORE_REGION_SIZE;
//                    int coreStartY = (ICON_RENDER_SIZE - CORE_REGION_SIZE) / 2;
//                    int coreEndY = coreStartY + CORE_REGION_SIZE;
//
//                    for (int x = 0; x < image.getWidth(); x++) {
//                        for (int y = 0; y < image.getHeight(); y++) {
//                            int pixel = image.getRGB(x, y);
//                            int alpha = (pixel >> 24) & 0xFF;
//                            int r = (pixel >> 16) & 0xFF;
//                            int g = (pixel >> 8) & 0xFF;
//                            int b = (pixel >> 0) & 0xFF;
//
//                            // 过滤掉透明像素。
//                            if (alpha <= ALPHA_THRESHOLD) {
//                                continue;
//                            }
//
//                            // 过滤掉纯黑和近黑色的像素。
//                            if (r <= NEAR_BLACK_RGB_THRESHOLD && g <= NEAR_BLACK_RGB_THRESHOLD && b <= NEAR_BLACK_RGB_THRESHOLD) {
//                                continue;
//                            }
//
//                            int rgbPixel = pixel & 0xFFFFFF;
//                            float currentBrightness = calculateBrightnessHSB(pixel);
//                            float currentSaturation = calculateSaturationHSB(pixel);
//
//                            // 根据亮度和饱和度计算基础权重。
//                            double weight = 1.0; // 基础权重。
//                            weight += (currentBrightness * BRIGHTNESS_WEIGHT_FACTOR);
//                            weight += (currentSaturation * SATURATION_WEIGHT_FACTOR);
//
//                            // 为中心区域的像素应用额外权重。
//                            if (x >= coreStartX && x < coreEndX && y >= coreStartY && y < coreEndY) {
//                                weight += CORE_REGION_WEIGHT_BOOST;
//                            }
//
//                            // 累加此颜色的权重值。
//                            weightedColorCounts.put(rgbPixel, weightedColorCounts.getOrDefault(rgbPixel, 0.0) + weight);
//                        }
//                    }
//
//                    int mainColor;
//                    if (!weightedColorCounts.isEmpty()) {
//                        // 找到了有效像素；选择加权众数。
//                        double maxWeightedCount = -1.0;
//                        int dominantRgb = weightedColorCounts.keySet().iterator().next(); // 使用第一个有效颜色初始化
//                        for (Map.Entry<Integer, Double> entry : weightedColorCounts.entrySet()) {
//                            if (entry.getValue() > maxWeightedCount) {
//                                maxWeightedCount = entry.getValue();
//                                dominantRgb = entry.getKey();
//                            }
//                        }
//                        mainColor = (0xFF << 24) | dominantRgb;
//                    } else {
//                        // 没有找到有效的（非透明、非黑、非近黑）像素。
//                        mainColor = FALLBACK_TO_BLACK; // 默认使用纯黑色。
//                    }
//
//                    extractedArmorColors.put(registryName, mainColor);
//                }
//            }
//        } finally {
//            // 禁用 GUI 物品光照和深度测试。
//            net.minecraft.client.renderer.RenderHelper.disableStandardItemLighting();
//            GL11.glDisable(GL11.GL_DEPTH_TEST);
//
//            // 恢复原始的 FBO 绑定（通常是默认的帧缓冲）。
//            GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, currentFBO);
//            // 将视口恢复到主屏幕尺寸。
//            if (mc != null) {
//                GL11.glViewport(0, 0, mc.displayWidth, mc.displayHeight);
//            }
//        }
//    }
//
//    /**
//     * 后处理方法：如果头盔或靴子有对应的胸甲，
//     * 它们的颜色将被设置为胸甲的颜色以保持一致性。
//     * 注意：此方法只应用于通过 FBO 提取的颜色。如果 ModConfig 中存在自定义颜色，
//     * 它们将优先于此处的任何自动调整。
//     */
//    private static void applyChestplateColorsToHeadAndFeet() {
//        System.out.println("[Curse of Abyss - ArmorColorExtractor]: Applying chestplate colors to helmets and boots for consistency...");
//
//        Map<String, String> customColors = ModConfig.getCustomArmorColors(); // 再次获取自定义颜色，用于检查
//
//        // 遍历键集的副本以避免 ConcurrentModificationException。
//        for (String registryName : new HashMap<>(extractedArmorColors).keySet()) {
//            // 检查物品是否是胸甲。
//            if (registryName.contains("_chestplate")) {
//                String baseName = registryName.replace("_chestplate", "");
//                Integer chestplateColor = extractedArmorColors.get(registryName);
//
//                if (chestplateColor != null) {
//                    // 尝试为对应的头盔设置颜色。
//                    String helmetName = baseName + "_helmet";
//                    // 只有当头盔没有自定义颜色时才应用胸甲颜色
//                    if (extractedArmorColors.containsKey(helmetName) && !customColors.containsKey(helmetName)) {
//                        extractedArmorColors.put(helmetName, chestplateColor);
//                    }
//
//                    // 尝试为对应的靴子设置颜色。
//                    String bootsName = baseName + "_boots";
//                    // 只有当靴子没有自定义颜色时才应用胸甲颜色
//                    if (extractedArmorColors.containsKey(bootsName) && !customColors.containsKey(bootsName)) {
//                        extractedArmorColors.put(bootsName, chestplateColor);
//                    }
//                }
//            }
//        }
//        System.out.println("[Curse of Abyss - ArmorColorExtractor]: Chestplate color application finished.");
//    }
//
//    /**
//     * 将提取的盔甲颜色保存到 JSON 文件。
//     * 文件路径：.minecraft/config/curseofabyss/armor_coloring/armor_colors.json
//     * 此方法现在只保存通过自动提取获得的颜色，不包括用户通过 ModConfig 设置的颜色。
//     */
//    private static void saveColorsToJson() {
//        Gson gson = new GsonBuilder().setPrettyPrinting().create();
//        JsonObject root = new JsonObject();
//        JsonObject autoExtractedArmorColors = new JsonObject(); // 改名为 autoExtractedArmorColors 更准确
//
//        // 遍历 extractedArmorColors 中已有的颜色
//        for (Map.Entry<String, Integer> entry : extractedArmorColors.entrySet()) {
//            String registryName = entry.getKey();
//            int colorInt = entry.getValue();
//            // 将 ARGB 整数转换为 #AARRGGBB 格式的字符串
//            String hexColor = String.format("#%08X", colorInt);
//            autoExtractedArmorColors.addProperty(registryName, hexColor);
//        }
//
//        root.add("autoExtractedArmorColors", autoExtractedArmorColors); // 写入 JSON 时也使用新名称
//
//        // 获取 Minecraft 配置目录
//        File configDir = new File(Minecraft.getMinecraft().mcDataDir, "config");
//        File modConfigDir = new File(configDir, "curseofabyss");
//        File armorColoringDir = new File(modConfigDir, "armor_coloring");
//
//        // 确保文件夹存在
//        if (!armorColoringDir.exists()) {
//            if (!armorColoringDir.mkdirs()) {
//                System.err.println("[Curse of Abyss - ArmorColorExtractor]: Failed to create directory: " + armorColoringDir.getAbsolutePath());
//                return;
//            }
//        }
//
//        File outputFile = new File(armorColoringDir, "armor_colors_auto.json"); // 建议将文件名改为 armor_colors_auto.json
//        // 以区分自动提取的和用户自定义的颜色
//
//        try (FileWriter writer = new FileWriter(outputFile)) {
//            gson.toJson(root, writer);
//            System.out.println("[Curse of Abyss - ArmorColorExtractor]: Successfully saved auto-extracted armor colors to " + outputFile.getAbsolutePath());
//        } catch (IOException e) {
//            System.err.println("[Curse of Abyss - ArmorColorExtractor]: Error saving auto-extracted armor colors to JSON: " + e.getMessage());
//            e.printStackTrace();
//        }
//    }
//}