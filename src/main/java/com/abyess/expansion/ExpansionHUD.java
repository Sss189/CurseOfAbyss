//
//
//package com.abyess.expansion;
//
//import com.abyess.items.Scapegoat.CustomScapegoatManager;
//import net.minecraft.client.Minecraft;
//import net.minecraft.client.entity.AbstractClientPlayer;
//import net.minecraft.client.gui.Gui;
//import net.minecraft.client.gui.ScaledResolution;
//import net.minecraft.client.renderer.GlStateManager;
//import net.minecraft.client.renderer.RenderHelper;
//import net.minecraft.item.ItemStack;
//import net.minecraft.util.ResourceLocation;
//import net.minecraft.block.material.Material;
//import net.minecraftforge.client.event.RenderGameOverlayEvent;
//import net.minecraftforge.fml.common.Mod;
//import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
//import net.minecraftforge.fml.relauncher.Side;
//import net.minecraftforge.fml.relauncher.SideOnly;
//
//import javax.imageio.ImageIO;
//import java.awt.image.BufferedImage;
//import java.io.File;
//import java.io.FileInputStream;
//import java.io.IOException;
//import java.util.HashMap;
//import java.util.Map;
//import java.util.concurrent.ConcurrentHashMap; // Use ConcurrentHashMap for thread-safety
//
//import com.abyess.debuff.TurnIntoHollow;
//import com.abyess.expansion.ArmorColorExtractor;
//import com.abyess.expansion.CustomPlayerIconsManager;
//import com.abyess.config.ModConfig;
//import net.minecraft.world.GameType;
//import net.minecraft.entity.SharedMonsterAttributes;
//import net.minecraft.client.renderer.texture.DynamicTexture;
//
//@Mod.EventBusSubscriber(modid = "curseofabyss", value = Side.CLIENT)
//@SideOnly(Side.CLIENT)
//public class ExpansionHUD {
//
//    // ========== 常量定义 ==========
//    private static final Minecraft mc = Minecraft.getMinecraft();
//    // 修改为ConcurrentHashMap以应对多线程访问（尽管通常在主线程渲染，但以防万一）
//    private static final Map<String, ResourceLocation> customArmorTextures = new ConcurrentHashMap<>();
//    private static final Map<String, Integer> customArmorTextureHeights = new ConcurrentHashMap<>();
//    // 跟踪是否已尝试加载某个特定的文件名（包含metadata，不包含metadata，或modid默认）
//    private static final Map<String, Boolean> textureLoadAttemptedForFilename = new ConcurrentHashMap<>();
//
//    // 盔甲纹理映射
//    private static final Map<String, Map<String, ResourceLocation>> armorTextures = new HashMap<>();
//    private static final Map<String, ResourceLocation> armorTintTextures = new HashMap<>();
//    private static final Map<String, ResourceLocation> scapegoatDurabilityTextures = new HashMap<>();
//
//    // 资源位置
//    private static final ResourceLocation EMPTY_HEAD_FRAME_TEXTURE = new ResourceLocation("curseofabyss", "textures/hud/hud_icon_empty.png");
//    private static final ResourceLocation STEVE_HEAD_ICON_TEXTURE = new ResourceLocation("curseofabyss", "textures/hud/hud_icon_steve.png");
//    private static final ResourceLocation HOLLOW_HEAD_ICON_TEXTURE = new ResourceLocation("curseofabyss", "textures/hud/hud_icon_hollow.png");
//    private static final ResourceLocation GENERAL_ARMOR_TEXTURE = new ResourceLocation("curseofabyss", "textures/hud/hud_general.png");
//
//
//    // 尺寸常量
//    private static final int SCAPEGOAT_ICON_SIZE = 16;
//    private static final int ARMOR_ICON_WIDTH = 9;
//    private static final int MAX_ARMOR_ICON_HEIGHT = 9; // Max height for default armor icons
//    private static final int CUSTOM_DURABILITY_BAR_WIDTH = 16;
//    private static final int CUSTOM_DURABILITY_BAR_HEIGHT = 3;
//    private static final int HEAD_FRAME_SIZE = 14;
//    private static final int PLAYER_HEAD_SIZE = 8;
//    private static final int HEAD_FRAME_PADDING = 3;
//    private static final int HEAD_FRAME_Y_OFFSET_FROM_BASELINE = 2;
//    private static final int BASE_HUD_Y_GENERAL = 59;
//    private static final int BASE_HUD_Y_ARMOR = 40;
//
//    // 新增：hud_general.png 的尺寸
//    private static final int ARMOR_ICON_WIDTH_GENERAL = 9;
//    private static final int ARMOR_ICON_HEIGHT_GENERAL = 12;
//
//    // 盔甲类型常量
//    private static final String HELMET_TYPE = "helm";
//    private static final String CHESTPLATE_TYPE = "chestplate";
//    private static final String LEGGINGS_TYPE = "leggings";
//    private static final String BOOTS_TYPE = "boots";
//
//    // 默认盔甲高度
//    private static final int HELMET_HEIGHT = 7;
//    private static final int CHESTPLATE_HEIGHT = 9;
//    private static final int LEGGINGS_HEIGHT = 9;
//    private static final int BOOTS_HEIGHT = 7;
//
//    // Y偏移量配置
//    private static final int[][] Y_OFFSETS = {
//            {0, -1, -1, 0},     // 有替罪羊 - 基底
//            {-10, -11, -11, -10},// 有替罪羊 - 抬升1
//            {-20, -20, -20, -20},// 有替罪羊 - 抬升2
//            {0, -1, -1, 0},      // 无替罪羊 - 基底
//            {-10, -12, -12, -10},// 无替罪羊 - 抬升1
//            {-20, -18, -18, -20} // 无替罪羊 - 抬升2
//    };
//
//    // X偏移量配置
//    private static final int[][] X_OFFSETS = {
//            {-28, -18, 9, 19},   // 有替罪羊 - 基底
//            {-28, -18, 9, 19},   // 有替罪羊 - 抬升1
//            {-28, -18, 9, 19},   // 有替罪羊 - 抬升2
//            {-27, -17, 8, 18},   // 无替罪羊 - 基底
//            {-20, -10, 1, 11},   // 无替罪羊 - 抬升1
//            {-20, -10, 1, 11}    // 无替罪羊 - 抬升2
//    };
//
//    static {
//        // 初始化纹理
//        initializeTextures();
//    }
//
//    private static void initializeTextures() {
//        String modid = "curseofabyss";
//        String[] armorTypes = {HELMET_TYPE, CHESTPLATE_TYPE, LEGGINGS_TYPE, BOOTS_TYPE};
//        String[] durabilityRanges = {"0_9", "10_19", "20_34", "35_49", "50_64", "65_79", "80_89", "90_100"};
//
//        for (String type : armorTypes) {
//            Map<String, ResourceLocation> typeTextures = new HashMap<>();
//            for (String range : durabilityRanges) {
//                String path = "textures/hud/hud_armor_" + type + "_" + range + ".png";
//                typeTextures.put(range, new ResourceLocation(modid, path));
//            }
//            armorTextures.put(type, typeTextures);
//
//            // 初始化tint纹理
//            String tintPath = "textures/hud/hud_armor_" + type + "_tint.png";
//            armorTintTextures.put(type, new ResourceLocation(modid, tintPath));
//        }
//
//        // 初始化替罪羊耐久纹理
//        String[] scapegoatDurabilityRanges = {
//                "0_4", "5_9", "10_14", "15_19", "20_24", "25_29", "30_39",
//                "40_49", "50_59", "60_69", "70_79", "80_84", "85_89", "90_94", "95_100"
//        };
//        for (String range : scapegoatDurabilityRanges) {
//            String path = "textures/hud/hud_scapegoat_" + range + ".png";
//            scapegoatDurabilityTextures.put(range, new ResourceLocation(modid, path));
//        }
//    }
//
//    // ========== 辅助方法 ==========
//
//    private static String getDurabilityRange(float durabilityPercent) {
//        if (durabilityPercent >= 90.0F) return "90_100";
//        if (durabilityPercent >= 80.0F) return "80_89";
//        if (durabilityPercent >= 65.0F) return "65_79";
//        if (durabilityPercent >= 50.0F) return "50_64";
//        if (durabilityPercent >= 35.0F) return "35_49";
//        if (durabilityPercent >= 20.0F) return "20_34";
//        if (durabilityPercent >= 10.0F) return "10_19";
//        return "0_9";
//    }
//
//    private static String getDurabilityRangeScapegoat(float durabilityPercent) {
//        if (durabilityPercent >= 95.0F) return "95_100";
//        if (durabilityPercent >= 90.0F) return "90_94";
//        if (durabilityPercent >= 85.0F) return "85_89";
//        if (durabilityPercent >= 80.0F) return "80_84";
//        if (durabilityPercent >= 70.0F) return "70_79";
//        if (durabilityPercent >= 60.0F) return "60_69";
//        if (durabilityPercent >= 50.0F) return "50_59";
//        if (durabilityPercent >= 40.0F) return "40_49";
//        if (durabilityPercent >= 30.0F) return "30_39";
//        if (durabilityPercent >= 25.0F) return "25_29";
//        if (durabilityPercent >= 20.0F) return "20_24";
//        if (durabilityPercent >= 15.0F) return "15_19";
//        if (durabilityPercent >= 10.0F) return "10_14";
//        if (durabilityPercent >= 5.0F) return "5_9";
//        return "0_4";
//    }
//
//    private static void applyArmorColor(ItemStack armorStack, boolean autoColoringEnabled) {
//        if (armorStack == null || armorStack.isEmpty() || armorStack.getItem().getRegistryName() == null) {
//            GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
//            return;
//        }
//
//        String registryName = armorStack.getItem().getRegistryName().toString();
//        int displayColor = 0xFFFFFFFF;
//
//        String customHexColor = ModConfig.getCustomArmorColors().get(registryName);
//        if (customHexColor != null && !customHexColor.isEmpty()) {
//            displayColor = ModConfig.hexStringToInt(customHexColor);
//        } else if (autoColoringEnabled) {
//            Integer extractedColor = ArmorColorExtractor.getExtractedArmorColors().get(registryName);
//            if (extractedColor != null) {
//                displayColor = extractedColor;
//            }
//        }
//
//        float r = ((displayColor >> 16) & 0xFF) / 255.0F;
//        float g = ((displayColor >> 8) & 0xFF) / 255.0F;
//        float b = (displayColor & 0xFF) / 255.0F;
//        float a = ((displayColor >> 24) & 0xFF) / 255.0F;
//        GlStateManager.color(r, g, b, a);
//    }
//
//    /**
//     * 根据优先级获取自定义盔甲纹理。
//     * 优先级：modid__itemid__metadata.png > modid__itemid.png > modid__.png
//     * @param armorStack 盔甲物品堆。
//     * @return 对应的ResourceLocation，如果没有则返回null。
//     */
//    private static ResourceLocation getCustomArmorTexture(ItemStack armorStack) {
//        if (armorStack == null || armorStack.isEmpty() || armorStack.getItem().getRegistryName() == null) return null;
//        String registryName = armorStack.getItem().getRegistryName().toString();
//        String modId = getModIdFromRegistryName(registryName);
//
//        // 1. 尝试 modid__itemid__metadata.png
//        // Important: Use registryName.replace(":", "__") for filename, but registryName for map key
//        String identifierWithMetaKey = registryName + "__" + armorStack.getMetadata();
//        if (customArmorTextures.containsKey(identifierWithMetaKey)) {
//            return customArmorTextures.get(identifierWithMetaKey);
//        }
//
//        // 2. 尝试 modid__itemid.png
//        // Important: Use registryName for map key
//        if (customArmorTextures.containsKey(registryName)) {
//            return customArmorTextures.get(registryName);
//        }
//
//        // 3. 尝试 modid__.png (Mod ID 默认纹理)
//        if (modId != null && customArmorTextures.containsKey(modId + "__default")) { // Use a distinct key for modid default
//            return customArmorTextures.get(modId + "__default");
//        }
//        return null;
//    }
//
//    /**
//     * 根据优先级获取自定义盔甲纹理的高度。
//     * 优先级：modid__itemid__metadata.png > modid__itemid.png > modid__.png
//     * @param armorStack 盔甲物品堆。
//     * @return 对应的纹理高度，如果没有则返回0。
//     */
//    private static int getCustomArmorTextureHeight(ItemStack armorStack) {
//        if (armorStack == null || armorStack.isEmpty() || armorStack.getItem().getRegistryName() == null) return 0;
//        String registryName = armorStack.getItem().getRegistryName().toString();
//        String modId = getModIdFromRegistryName(registryName);
//
//        // 1. 尝试 modid__itemid__metadata.png
//        String identifierWithMetaKey = registryName + "__" + armorStack.getMetadata();
//        Integer heightWithMeta = customArmorTextureHeights.get(identifierWithMetaKey);
//        if (heightWithMeta != null && heightWithMeta > 0) {
//            return heightWithMeta;
//        }
//
//        // 2. 尝试 modid__itemid.png
//        Integer height = customArmorTextureHeights.get(registryName);
//        if (height != null && height > 0) {
//            return height;
//        }
//
//        // 3. 尝试 modid__.png (Mod ID 默认纹理)
//        if (modId != null) {
//            Integer modIdHeight = customArmorTextureHeights.get(modId + "__default"); // Use a distinct key for modid default
//            if (modIdHeight != null && modIdHeight > 0) {
//                return modIdHeight;
//            }
//        }
//        return 0;
//    }
//
//    /**
//     * 从 registryName (例如 "modid:itemid") 中提取 Mod ID。
//     * @param registryName 物品的注册名称。
//     * @return Mod ID，如果格式不正确则返回 null。
//     */
//    private static String getModIdFromRegistryName(String registryName) {
//        int colonIndex = registryName.indexOf(':');
//        if (colonIndex != -1) {
//            return registryName.substring(0, colonIndex);
//        }
//        return null;
//    }
//
//    /**
//     * 尝试加载指定物品的自定义纹理，支持带metadata、不带metadata以及modid默认纹理。
//     * 纹理文件命名约定：
//     * 1. `modid__itemid__metadata.png` (最高优先级)
//     * 2. `modid__itemid.png` (次优先级)
//     * 3. `modid__.png` (Mod ID 默认，最低优先级)
//     * @param armorStack 物品堆。
//     */
//    private static void loadCustomArmorTexture(ItemStack armorStack) {
//        if (armorStack == null || armorStack.isEmpty() || armorStack.getItem().getRegistryName() == null) return;
//        String registryName = armorStack.getItem().getRegistryName().toString();
//        String modId = getModIdFromRegistryName(registryName);
//
//        File configDir = new File(Minecraft.getMinecraft().mcDataDir, "config");
//        File modConfigDir = new File(configDir, "curseofabyss");
//        File armorTexturesDir = new File(modConfigDir, "armor_coloring");
//
//        // --- 1. 尝试加载 modid__itemid__metadata.png (最高优先级) ---
//        String filenameWithMeta = registryName.replace(":", "__") + "__" + armorStack.getMetadata();
//        String identifierWithMetaKey = registryName + "__" + armorStack.getMetadata(); // Key for map (registryName:metadata)
//        if (!textureLoadAttemptedForFilename.containsKey(filenameWithMeta)) {
//            textureLoadAttemptedForFilename.put(filenameWithMeta, true);
//            File specificTextureFileWithMeta = new File(armorTexturesDir, filenameWithMeta + ".png");
//
//            if (specificTextureFileWithMeta.exists() && specificTextureFileWithMeta.isFile()) {
//                try (FileInputStream fis = new FileInputStream(specificTextureFileWithMeta)) {
//                    BufferedImage image = ImageIO.read(fis);
//                    if (image != null) {
//                        int height = image.getHeight();
//                        DynamicTexture dynamicTexture = new DynamicTexture(image);
//                        ResourceLocation location = Minecraft.getMinecraft().getTextureManager().getDynamicTextureLocation(
//                                "custom_armor_" + filenameWithMeta, dynamicTexture);
//                        customArmorTextures.put(identifierWithMetaKey, location);
//                        customArmorTextureHeights.put(identifierWithMetaKey, height);
//                    }
//                } catch (IOException e) {
//                    System.err.println("[ExpansionHUD] Error loading specific custom armor texture with metadata for " + filenameWithMeta + ": " + e.getMessage());
//                }
//            }
//        }
//
//        // --- 2. 尝试加载 modid__itemid.png (次优先级) ---
//        String filenameWithoutMeta = registryName.replace(":", "__");
//        String identifierWithoutMetaKey = registryName; // Key for map (registryName)
//        if (!textureLoadAttemptedForFilename.containsKey(filenameWithoutMeta)) {
//            textureLoadAttemptedForFilename.put(filenameWithoutMeta, true);
//            File specificTextureFile = new File(armorTexturesDir, filenameWithoutMeta + ".png");
//
//            if (specificTextureFile.exists() && specificTextureFile.isFile()) {
//                try (FileInputStream fis = new FileInputStream(specificTextureFile)) {
//                    BufferedImage image = ImageIO.read(fis);
//                    if (image != null) {
//                        int height = image.getHeight();
//                        DynamicTexture dynamicTexture = new DynamicTexture(image);
//                        ResourceLocation location = Minecraft.getMinecraft().getTextureManager().getDynamicTextureLocation(
//                                "custom_armor_" + filenameWithoutMeta, dynamicTexture);
//                        customArmorTextures.put(identifierWithoutMetaKey, location);
//                        customArmorTextureHeights.put(identifierWithoutMetaKey, height);
//                    }
//                } catch (IOException e) {
//                    System.err.println("[ExpansionHUD] Error loading specific custom armor texture for " + filenameWithoutMeta + ": " + e.getMessage());
//                }
//            }
//        }
//
//        // --- 3. 尝试加载 modid__.png (最低优先级) ---
//        if (modId != null) {
//            String modIdFilename = modId + "__";
//            String modIdDefaultKey = modId + "__default"; // Key for map (modid__default)
//            if (!textureLoadAttemptedForFilename.containsKey(modIdFilename)) {
//                textureLoadAttemptedForFilename.put(modIdFilename, true);
//                File modIdTextureFile = new File(armorTexturesDir, modIdFilename + ".png");
//
//                if (modIdTextureFile.exists() && modIdTextureFile.isFile()) {
//                    try (FileInputStream fis = new FileInputStream(modIdTextureFile)) {
//                        BufferedImage image = ImageIO.read(fis);
//                        if (image != null) {
//                            int height = image.getHeight();
//                            DynamicTexture dynamicTexture = new DynamicTexture(image);
//                            ResourceLocation location = Minecraft.getMinecraft().getTextureManager().getDynamicTextureLocation(
//                                    "custom_armor_" + modIdFilename, dynamicTexture);
//                            customArmorTextures.put(modIdDefaultKey, location);
//                            customArmorTextureHeights.put(modIdDefaultKey, height);
//                        }
//                    } catch (IOException e) {
//                        System.err.println("[ExpansionHUD] Error loading Mod ID default armor texture for " + modIdFilename + ": " + e.getMessage());
//                    }
//                }
//            }
//        }
//    }
//
//
//    private static void renderArmorPiece(ItemStack stack, String armorType, int armorBaseY,
//                                         int yOffset, int xPos, int defaultHeight,
//                                         boolean autoColoringEnabled) {
//        if (stack == null || stack.isEmpty()) return;
//
//        // 检查是否有耐久度
//        boolean hasDurability = stack.isItemStackDamageable() && stack.getMaxDamage() > 0;
//        float durability = hasDurability ?
//                (float) (stack.getMaxDamage() - stack.getItemDamage()) / (float) stack.getMaxDamage() * 100.0F : 100.0F;
//
//        // 获取自定义纹理及其高度
//        ResourceLocation customTexture = getCustomArmorTexture(stack);
//        int customHeight = (customTexture != null) ? getCustomArmorTextureHeight(stack) : 0;
//
//        // 首先渲染耐久条 (如果存在)
//        if (hasDurability) {
//            ResourceLocation durabilityTexture = armorTextures.get(armorType).get(getDurabilityRange(durability));
//            if (durabilityTexture != null) {
//                mc.getTextureManager().bindTexture(durabilityTexture);
//                int yPosBar = armorBaseY - CUSTOM_DURABILITY_BAR_HEIGHT + yOffset;
//                Gui.drawModalRectWithCustomSizedTexture(
//                        xPos, yPosBar,
//                        0, defaultHeight - CUSTOM_DURABILITY_BAR_HEIGHT,
//                        ARMOR_ICON_WIDTH, CUSTOM_DURABILITY_BAR_HEIGHT,
//                        ARMOR_ICON_WIDTH, defaultHeight
//                );
//            }
//        }
//
//        // 渲染主纹理
//        if (customTexture != null && customHeight > 0) {
//            // Calculate position for custom texture, applying a 2-pixel upward offset if durability bar is present
//            int yPosBody = armorBaseY + yOffset - customHeight;
//            if (hasDurability) {
//                yPosBody -= 2; // Lift custom texture by 2 pixels to avoid covering the durability bar
//            }
//
//            // Render custom texture
//            mc.getTextureManager().bindTexture(customTexture);
//            GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F); // Custom textures should not be tinted by armor color
//            Gui.drawModalRectWithCustomSizedTexture(
//                    xPos, yPosBody,
//                    0, 0,
//                    ARMOR_ICON_WIDTH, customHeight,
//                    ARMOR_ICON_WIDTH, customHeight
//            );
//        } else if (!hasDurability) {
//            // 如果没有自定义纹理且物品无耐久度，使用 hud_general.png
//            mc.getTextureManager().bindTexture(GENERAL_ARMOR_TEXTURE);
//            GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F); // hud_general.png 不受染色影响
//            int yPosGeneral = armorBaseY + yOffset - ARMOR_ICON_HEIGHT_GENERAL;
//            Gui.drawModalRectWithCustomSizedTexture(
//                    xPos, yPosGeneral,
//                    0, 0,
//                    ARMOR_ICON_WIDTH_GENERAL, ARMOR_ICON_HEIGHT_GENERAL,
//                    ARMOR_ICON_WIDTH_GENERAL, ARMOR_ICON_HEIGHT_GENERAL
//            );
//        } else {
//            // 回退到原始渲染 (有耐久度，无自定义纹理)
//            ResourceLocation tintTexture = armorTintTextures.get(armorType);
//            ResourceLocation shadingTexture = armorTextures.get(armorType).get(getDurabilityRange(durability));
//
//            if (tintTexture == null || shadingTexture == null) return;
//
//            int yPosBody = armorBaseY - defaultHeight + yOffset;
//
//            // Render tint layer
//            mc.getTextureManager().bindTexture(tintTexture);
//            applyArmorColor(stack, autoColoringEnabled);
//            Gui.drawModalRectWithCustomSizedTexture(
//                    xPos, yPosBody,
//                    0, 0,
//                    ARMOR_ICON_WIDTH, defaultHeight - CUSTOM_DURABILITY_BAR_HEIGHT,
//                    ARMOR_ICON_WIDTH, defaultHeight
//            );
//
//            // Render shading layer
//            GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
//            mc.getTextureManager().bindTexture(shadingTexture);
//            Gui.drawModalRectWithCustomSizedTexture(
//                    xPos, yPosBody,
//                    0, 0,
//                    ARMOR_ICON_WIDTH, defaultHeight - CUSTOM_DURABILITY_BAR_HEIGHT,
//                    ARMOR_ICON_WIDTH, defaultHeight
//            );
//        }
//    }
//
//    // ========== 主渲染方法 ==========
//
//    @SubscribeEvent
//    public static void onRenderGameOverlay(RenderGameOverlayEvent.Post event) {
//        if (!ModConfig.getConfigData().getHudConfig().isEnableStatusBarHUD()) return;
//        if (event.getType() != RenderGameOverlayEvent.ElementType.EXPERIENCE || mc.player == null) return;
//
//        GameType gameType = mc.playerController.getCurrentGameType();
//        if (gameType != GameType.SURVIVAL && gameType != GameType.ADVENTURE) return;
//
//        ScaledResolution scaledResolution = new ScaledResolution(mc);
//        int width = scaledResolution.getScaledWidth();
//        int height = scaledResolution.getScaledHeight();
//
//        GlStateManager.pushMatrix();
//        GlStateManager.enableBlend();
//        GlStateManager.tryBlendFuncSeparate(
//                GlStateManager.SourceFactor.SRC_ALPHA,
//                GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA,
//                GlStateManager.SourceFactor.ONE,
//                GlStateManager.DestFactor.ZERO
//        );
//        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
//        GlStateManager.disableDepth();
//
//        // 预加载盔甲纹理
//        for (int i = 0; i < 4; i++) {
//            ItemStack stack = mc.player.inventory.armorInventory.get(i);
//            if (!stack.isEmpty()) {
//                loadCustomArmorTexture(stack);
//            }
//        }
//
//        // 获取游戏状态
//        boolean autoColoringEnabled = ModConfig.getConfigData().getHudConfig().isEnableAutoArmorColoring();
//        int currentArmorValue = mc.player.getTotalArmorValue();
//        float absorptionAmount = mc.player.getAbsorptionAmount();
//        double armorToughness = mc.player.getEntityAttribute(SharedMonsterAttributes.ARMOR_TOUGHNESS).getAttributeValue();
//        boolean hasWaterBreathing = mc.player.isInsideOfMaterial(Material.WATER) && mc.player.getAir() > 0;
//
//        // 检查替罪羊物品
//        ItemStack firstScapegoatItem = null;
//        for (int i = 0; i < mc.player.inventory.getSizeInventory(); i++) {
//            ItemStack stack = mc.player.inventory.getStackInSlot(i);
//            if (!stack.isEmpty() && CustomScapegoatManager.isScapegoatItem(mc.player, stack)) {
//                firstScapegoatItem = stack;
//                break;
//            }
//        }
//        boolean hasScapegoat = firstScapegoatItem != null;
//
//        // 计算抬升等级
//        int leftLiftLevel = (currentArmorValue > 0 && absorptionAmount > 0.0F) ? 2 :
//                (currentArmorValue > 0 || absorptionAmount > 0.0F) ? 1 : 0;
//
//        int rightLiftLevel = (hasWaterBreathing && armorToughness > 0.0D) ? 2 :
//                (hasWaterBreathing || armorToughness > 0.0D) ? 1 : 0;
//
//        // 计算位置
//        int armorBaseY = height - BASE_HUD_Y_ARMOR;
//        int originalBaselineY = height - BASE_HUD_Y_GENERAL + 6;
//
//        int leftYOffsetArrayIndex = hasScapegoat ? leftLiftLevel : 3 + leftLiftLevel;
//        int rightYOffsetArrayIndex = hasScapegoat ? rightLiftLevel : 3 + rightLiftLevel;
//        int leftXOffsetArrayIndex = hasScapegoat ? leftLiftLevel : 3 + leftLiftLevel;
//        int rightXOffsetArrayIndex = hasScapegoat ? rightLiftLevel : 3 + rightLiftLevel;
//
//        // 获取Y偏移
//        int helmYOffset = Y_OFFSETS[leftYOffsetArrayIndex][0];
//        int chestYOffset = Y_OFFSETS[leftYOffsetArrayIndex][1];
//        int legsYOffset = Y_OFFSETS[rightYOffsetArrayIndex][2];
//        int bootsYOffset = Y_OFFSETS[rightYOffsetArrayIndex][3];
//
//        if (hasScapegoat) {
//            int scapegoatCenterX = width / 2;
//
//            // 渲染左侧盔甲（头盔和胸甲）
//            renderArmorPiece(
//                    mc.player.inventory.armorInventory.get(3), // Helmet
//                    HELMET_TYPE,
//                    armorBaseY,
//                    helmYOffset,
//                    scapegoatCenterX + X_OFFSETS[leftXOffsetArrayIndex][0],
//                    HELMET_HEIGHT,
//                    autoColoringEnabled
//            );
//
//            renderArmorPiece(
//                    mc.player.inventory.armorInventory.get(2), // Chestplate
//                    CHESTPLATE_TYPE,
//                    armorBaseY,
//                    chestYOffset,
//                    scapegoatCenterX + X_OFFSETS[leftXOffsetArrayIndex][1],
//                    CHESTPLATE_HEIGHT,
//                    autoColoringEnabled
//            );
//
//            // 渲染替罪羊
//            renderScapegoat(firstScapegoatItem, scapegoatCenterX, originalBaselineY);
//
//            // 渲染右侧盔甲（腿甲和靴子）
//            renderArmorPiece(
//                    mc.player.inventory.armorInventory.get(1), // Leggings
//                    LEGGINGS_TYPE,
//                    armorBaseY,
//                    legsYOffset,
//                    scapegoatCenterX + X_OFFSETS[rightXOffsetArrayIndex][2],
//                    LEGGINGS_HEIGHT,
//                    autoColoringEnabled
//            );
//
//            renderArmorPiece(
//                    mc.player.inventory.armorInventory.get(0), // Boots
//                    BOOTS_TYPE,
//                    armorBaseY,
//                    bootsYOffset,
//                    scapegoatCenterX + X_OFFSETS[rightXOffsetArrayIndex][3],
//                    BOOTS_HEIGHT,
//                    autoColoringEnabled
//            );
//        } else {
//            int screenCenterX = width / 2;
//
//            // 渲染所有盔甲
//            renderArmorPiece(
//                    mc.player.inventory.armorInventory.get(3), // Helmet
//                    HELMET_TYPE,
//                    armorBaseY,
//                    helmYOffset,
//                    screenCenterX + X_OFFSETS[leftXOffsetArrayIndex][0],
//                    HELMET_HEIGHT,
//                    autoColoringEnabled
//            );
//
//            renderArmorPiece(
//                    mc.player.inventory.armorInventory.get(2), // Chestplate
//                    CHESTPLATE_TYPE,
//                    armorBaseY,
//                    chestYOffset,
//                    screenCenterX + X_OFFSETS[leftXOffsetArrayIndex][1],
//                    CHESTPLATE_HEIGHT,
//                    autoColoringEnabled
//            );
//
//            renderArmorPiece(
//                    mc.player.inventory.armorInventory.get(1), // Leggings
//                    LEGGINGS_TYPE,
//                    armorBaseY,
//                    legsYOffset,
//                    screenCenterX + X_OFFSETS[rightXOffsetArrayIndex][2],
//                    LEGGINGS_HEIGHT,
//                    autoColoringEnabled
//            );
//
//            renderArmorPiece(
//                    mc.player.inventory.armorInventory.get(0), // Boots
//                    BOOTS_TYPE,
//                    armorBaseY,
//                    bootsYOffset,
//                    screenCenterX + X_OFFSETS[rightXOffsetArrayIndex][3],
//                    BOOTS_HEIGHT,
//                    autoColoringEnabled
//            );
//        }
//
//        // 渲染玩家头像
//        renderPlayerHead(width, height, originalBaselineY);
//
//        GlStateManager.enableDepth();
//        GlStateManager.disableBlend();
//        GlStateManager.popMatrix();
//    }
//
//    /**
//     * 渲染替罪羊物品
//     */
//    private static void renderScapegoat(ItemStack scapegoatItem, int centerX, int baselineY) {
//        if (scapegoatItem == null || scapegoatItem.isEmpty()) return;
//
//        GlStateManager.pushMatrix();
//        RenderHelper.enableGUIStandardItemLighting();
//        GlStateManager.enableDepth();
//        GlStateManager.translate(0F, 0F, -500.0F);
//
//        int barY = baselineY - CUSTOM_DURABILITY_BAR_HEIGHT + 1;
//        int iconY = barY - SCAPEGOAT_ICON_SIZE + 4; // +3+1 简化
//
//        // 渲染物品
//        mc.getRenderItem().renderItemAndEffectIntoGUI(scapegoatItem, centerX - (SCAPEGOAT_ICON_SIZE / 2), iconY);
//        GlStateManager.disableDepth();
//
//        // 渲染耐久条
//        if (scapegoatItem.isItemStackDamageable() && scapegoatItem.getMaxDamage() > 0) {
//            float durabilityPercent = (float) (scapegoatItem.getMaxDamage() - scapegoatItem.getItemDamage()) /
//                    (float) scapegoatItem.getMaxDamage() * 100.0F;
//            String range = getDurabilityRangeScapegoat(durabilityPercent);
//            ResourceLocation durabilityTexture = scapegoatDurabilityTextures.get(range);
//
//            if (durabilityTexture != null) {
//                mc.getTextureManager().bindTexture(durabilityTexture);
//                int barX = centerX - (SCAPEGOAT_ICON_SIZE / 2) + (SCAPEGOAT_ICON_SIZE - CUSTOM_DURABILITY_BAR_WIDTH) / 2;
//                Gui.drawModalRectWithCustomSizedTexture(
//                        barX, barY,
//                        0, 0,
//                        CUSTOM_DURABILITY_BAR_WIDTH, CUSTOM_DURABILITY_BAR_HEIGHT,
//                        CUSTOM_DURABILITY_BAR_WIDTH, CUSTOM_DURABILITY_BAR_HEIGHT
//                );
//            }
//        }
//
//        RenderHelper.disableStandardItemLighting();
//        GlStateManager.popMatrix();
//    }
//
//    /**
//     * 渲染玩家头像
//     */
//    private static void renderPlayerHead(int width, int height, int baselineY) {
//        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
//        int headFrameX = (width / 2) - (HEAD_FRAME_SIZE / 2);
//        int headFrameY = baselineY + HEAD_FRAME_Y_OFFSET_FROM_BASELINE;
//        GlStateManager.enableDepth();
//
//        if (TurnIntoHollow.shouldProcess(mc.player)) {
//            mc.getTextureManager().bindTexture(HOLLOW_HEAD_ICON_TEXTURE);
//            Gui.drawModalRectWithCustomSizedTexture(
//                    headFrameX, headFrameY,
//                    0, 0,
//                    HEAD_FRAME_SIZE, HEAD_FRAME_SIZE,
//                    HEAD_FRAME_SIZE, HEAD_FRAME_SIZE
//            );
//        } else {
//            String selectedIconId = CustomPlayerIconsManager.getPlayerSelectedIcon();
//            ResourceLocation customIconLocation = !"0".equals(selectedIconId) ?
//                    CustomPlayerIconsManager.getCustomIcon(selectedIconId) : null;
//
//            if (customIconLocation != null) {
//                mc.getTextureManager().bindTexture(customIconLocation);
//                Gui.drawModalRectWithCustomSizedTexture(
//                        headFrameX, headFrameY,
//                        0, 0,
//                        HEAD_FRAME_SIZE, HEAD_FRAME_SIZE,
//                        HEAD_FRAME_SIZE, HEAD_FRAME_SIZE
//                );
//            } else {
//                // 渲染默认头像
//                renderDefaultPlayerHead(headFrameX, headFrameY);
//            }
//        }
//    }
//
//    /**
//     * 渲染默认玩家头像（无自定义图标时）
//     */
//    private static void renderDefaultPlayerHead(int frameX, int frameY) {
//        mc.getTextureManager().bindTexture(EMPTY_HEAD_FRAME_TEXTURE);
//        Gui.drawModalRectWithCustomSizedTexture(
//                frameX, frameY,
//                0, 0,
//                HEAD_FRAME_SIZE, HEAD_FRAME_SIZE,
//                HEAD_FRAME_SIZE, HEAD_FRAME_SIZE
//        );
//
//        int headX = frameX + HEAD_FRAME_PADDING;
//        int headY = frameY + HEAD_FRAME_PADDING;
//
//        if (mc.player instanceof AbstractClientPlayer) {
//            AbstractClientPlayer player = (AbstractClientPlayer) mc.player;
//            if (player.hasSkin()) {
//                mc.getTextureManager().bindTexture(player.getLocationSkin());
//                Gui.drawModalRectWithCustomSizedTexture(headX, headY, 8.0F, 8.0F, PLAYER_HEAD_SIZE, PLAYER_HEAD_SIZE, 64.0F, 64.0F);
//                Gui.drawModalRectWithCustomSizedTexture(headX, headY, 40.0F, 8.0F, PLAYER_HEAD_SIZE, PLAYER_HEAD_SIZE, 64.0F, 64.0F);
//                return;
//            }
//        }
//
//        // 使用Steve头像作为回退
//        mc.getTextureManager().bindTexture(STEVE_HEAD_ICON_TEXTURE);
//        Gui.drawModalRectWithCustomSizedTexture(headX, headY, 0, 0, PLAYER_HEAD_SIZE, PLAYER_HEAD_SIZE, PLAYER_HEAD_SIZE, PLAYER_HEAD_SIZE);
//    }
//}