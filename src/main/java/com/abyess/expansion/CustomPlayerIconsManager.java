//package com.abyess.expansion;
//
//import net.minecraft.client.Minecraft;
//import net.minecraft.client.renderer.texture.DynamicTexture;
//import net.minecraft.util.ResourceLocation;
//import net.minecraftforge.fml.common.Mod;
//import net.minecraftforge.fml.relauncher.Side;
//import net.minecraftforge.fml.relauncher.SideOnly;
//
//import javax.imageio.ImageIO;
//import java.awt.image.BufferedImage;
//import java.io.File;
//import java.io.IOException;
//import java.util.Collections;
//import java.util.HashMap;
//import java.util.Map;
//import java.util.Set;
//
//// 导入 ModConfig
//import com.abyess.config.ModConfig;
//
//// 如果这个类只处理客户端逻辑且没有其他 Forge 事件订阅，你可以选择移除 @Mod.EventBusSubscriber
//// 如果有其他客户端事件，就保留
//@Mod.EventBusSubscriber(modid = "curseofabyss", value = Side.CLIENT)
//@SideOnly(Side.CLIENT)
//public class CustomPlayerIconsManager {
//
//    // NBT_PLAYER_ICON_KEY 已不再直接用于持久化，可以移除或作为常量保留，但本类不再使用
//    // public static final String NBT_PLAYER_ICON_KEY = "CurseOfAbyss_SelectedHudIcon";
//
//    // 存储自定义图标的Map: key为文件名(不带.png), value为对应的ResourceLocation
//    private static final Map<String, ResourceLocation> customIcons = new HashMap<>();
//    // 存储配置文件中 player_icons 目录的 File 对象
//    private static File playerIconsDirectory;
//
//    // playerSelectedIcon 不再是一个需要直接更新的静态变量，而是直接从 ModConfig 读取
//    // private static String playerSelectedIcon = "0"; // 可以移除此行
//
//    /**
//     * 设置自定义图标的根目录。
//     * 在 Mod 的 preInit 阶段（客户端侧）调用。
//     * @param dir 玩家自定义图标的文件夹路径 (例如: config/curseofabyss/player_icons)
//     */
//    public static void setPlayerIconsDirectory(File dir) {
//        playerIconsDirectory = dir;
//        System.out.println("[Curse of Abyss] 自定义玩家头像目录设置为: " + dir.getAbsolutePath());
//    }
//
//    /**
//     * 加载 player_icons 目录下的所有 .png 文件作为动态纹理。
//     * 应该在客户端的 PostInit 阶段或进入世界时调用。
//     */
//    public static void loadCustomIcons() {
//        if (playerIconsDirectory == null) {
//            System.err.println("[Curse of Abyss] 自定义玩家头像目录未设置！");
//            return;
//        }
//        if (!playerIconsDirectory.exists() || !playerIconsDirectory.isDirectory()) {
//            System.err.println("[Curse of Abyss] 自定义玩家头像目录不存在或不是一个目录: " + playerIconsDirectory.getAbsolutePath());
//            return;
//        }
//
//        customIcons.clear(); // 清除之前加载的图标，避免重复
//
//        File[] iconFiles = playerIconsDirectory.listFiles((dir, name) -> name.endsWith(".png"));
//
//        if (iconFiles != null) {
//            System.out.println("[Curse of Abyss] 从以下路径加载自定义玩家头像: " + playerIconsDirectory.getAbsolutePath());
//            for (File file : iconFiles) {
//                String fileName = file.getName();
//                String iconId = fileName.substring(0, fileName.lastIndexOf(".")); // 获取不带后缀的文件名作为ID
//
//                try {
//                    // 读取 PNG 图片
//                    BufferedImage bufferedImage = ImageIO.read(file);
//
//                    // 检查图片尺寸，确保是 14x14
//                    if (bufferedImage.getWidth() != 14 || bufferedImage.getHeight() != 14) {
//                        System.err.println("[Curse of Abyss] 跳过自定义头像 '" + fileName + "': 图片尺寸必须是 14x14 像素。实际尺寸 " + bufferedImage.getWidth() + "x" + bufferedImage.getHeight());
//                        continue; // 跳过不符合尺寸的图片
//                    }
//
//                    // 创建 DynamicTexture
//                    DynamicTexture dynamicTexture = new DynamicTexture(bufferedImage);
//
//                    // 为 DynamicTexture 生成一个唯一的 ResourceLocation
//                    ResourceLocation dynamicLoc = Minecraft.getMinecraft().getTextureManager().getDynamicTextureLocation("curseofabyss_icon_" + iconId, dynamicTexture);
//
//                    customIcons.put(iconId, dynamicLoc);
//                    System.out.println("[Curse of Abyss] 成功加载自定义头像: '" + iconId + "'");
//
//                } catch (IOException e) {
//                    System.err.println("[Curse of Abyss] 加载自定义头像文件失败: " + fileName + " - " + e.getMessage());
//                } catch (Exception e) {
//                    System.err.println("[Curse of Abyss] 加载自定义头像时发生意外错误: " + fileName + " - " + e.getMessage());
//                    e.printStackTrace(); // 打印堆栈跟踪以便调试
//                }
//            }
//            System.out.println("[Curse of Abyss] 完成加载自定义玩家头像。总计加载: " + customIcons.size());
//        } else {
//            System.out.println("[Curse of Abyss] 在 " + playerIconsDirectory.getAbsolutePath() + " 中未找到 .png 文件。");
//        }
//    }
//
//    /**
//     * 根据图标ID获取对应的ResourceLocation。
//     * @param iconId 图标的ID (文件名，不带.png后缀)
//     * @return 对应的ResourceLocation，如果不存在则返回 null
//     */
//    public static ResourceLocation getCustomIcon(String iconId) {
//        return customIcons.get(iconId);
//    }
//
//    /**
//     * 检查是否存在指定ID的自定义图标。
//     * @param iconId 图标的ID
//     * @return 如果存在则返回 true，否则返回 false
//     */
//    public static boolean hasCustomIcon(String iconId) {
//        return customIcons.containsKey(iconId);
//    }
//
//    /**
//     * 获取所有已加载的自定义图标的ID列表。
//     * 用于命令的Tab补全。
//     * @return 不可修改的图标ID集合
//     */
//    public static Set<String> getAllCustomIconNames() {
//        return Collections.unmodifiableSet(customIcons.keySet());
//    }
//
//    /**
//     * 设置玩家选择的HUD图标ID。
//     * 这个方法会被命令调用。它会更新 ModConfig。
//     * @param iconId 图标ID
//     */
//    public static void setPlayerSelectedIcon(String iconId) {
//        if (iconId == null || iconId.isEmpty()) {
//            iconId = "0"; // 默认设置为 "0" (史蒂夫/玩家皮肤)
//        }
//        ModConfig.setPlayerSelectedIconId(iconId); // 更新并保存到配置文件
//        System.out.println("[Curse of Abyss] 玩家头像设置为: " + iconId + " (已保存到配置)");
//    }
//
//    /**
//     * 获取玩家当前选择的HUD图标ID。
//     * 这个方法会被 HUD 渲染逻辑调用。
//     * @return 当前选择的图标ID字符串
//     */
//    public static String getPlayerSelectedIcon() {
//        return ModConfig.getPlayerSelectedIconId(); // 从配置中获取
//    }
//
//}