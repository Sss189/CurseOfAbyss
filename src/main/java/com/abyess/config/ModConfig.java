package com.abyess.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.relauncher.Side;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;

public class ModConfig {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String CONFIG_FILE_PATH = "config/abyess_curse_config.json";

    private static ConfigData configData;

    private static boolean configLoadError = false;

    public static void loadConfig() {
        File configFile = new File(CONFIG_FILE_PATH);
        configLoadError = false;

        if (!configFile.exists()) {
            configData = createDefaultConfig();
            saveConfig();
        } else {
            try (FileReader reader = new FileReader(configFile)) {
                configData = GSON.fromJson(reader, ConfigData.class);
                if (configData == null) {
                    configData = createDefaultConfig();
                    configLoadError = true;
                }
            } catch (Exception e) {
                configData = createDefaultConfig();
                configLoadError = true;
            }
            saveConfig();
        }
    }

    public static boolean hasConfigLoadError() {
        if (configData == null) {
            loadConfig();
        }
        return configLoadError;
    }

    public static void saveConfig() {
        File configFile = new File(CONFIG_FILE_PATH);
        File parentDir = configFile.getParentFile();
        if (parentDir != null && !parentDir.exists()) {
            parentDir.mkdirs();
        }

        try (FileWriter writer = new FileWriter(configFile)) {
            GSON.toJson(configData, writer);
        } catch (IOException e) {
            // Error handling, if necessary, without System.err.println
        }
    }

    public static ConfigData getConfigData() {
        if (configData == null) {
            loadConfig();
        }
        return configData;
    }

    private static ConfigData createDefaultConfig() {
        ConfigData defaultConfig = new ConfigData();

        defaultConfig.setHollowJumpPower(0.5F);
        defaultConfig.setNoWayBack(true);
        defaultConfig.setCustomCompass3DLock(true);
        defaultConfig.setDisableInventoryInHollow(true);
        defaultConfig.setEnable2DEntityDistanceScaling(true);

        defaultConfig.setHudConfig(new HudConfig());

        List<ModConfig.CustomCompassConfig> customCompasses = new ArrayList<>();
        customCompasses.add(new ModConfig.CustomCompassConfig(
                "minecraft:ender_eye",
                new int[]{1000, 65, 2000},
                0
        ));
        defaultConfig.setCustomCompasses(customCompasses);

        List<CustomScapegoatItem> customItems = new ArrayList<>();
        customItems.add(new CustomScapegoatItem("minecraft:nether_star", -1));
        defaultConfig.setCustomScapegoatItems(customItems);

        Map<Integer, DimensionConfig> dimensions = new HashMap<>();

        DimensionConfig overworld = new DimensionConfig();
        overworld.setEnabled(true);
        overworld.setLayers(Arrays.asList(
                new LayerConfig("Edge of the Abyss", true, true, new int[]{224, 249}, 10, 0, 1, "#B0C4DE",
                        Arrays.asList(new CurseConfig("potion", "minecraft:nausea", 10, 0, false)),
                        true, new float[]{1.0F, 1.0F, 1.0F}, 0.02f, true,
                        "EXP2", 0.0f, 1.0f
                ),
                new LayerConfig("Forest of Temptation", true, true, new int[]{203, 224}, 10, 0, 2, "#006400",
                        Arrays.asList(
                                new CurseConfig("potion", "minecraft:hunger", 5, 1, false),
                                new CurseConfig("potion", "minecraft:mining_fatigue", 5, 2, false),
                                new CurseConfig("potion", "minecraft:slowness", 5, 1, false),
                                new CurseConfig("potion", "minecraft:weakness", 5, 0, false),
                                new CurseConfig("filter", "overlappingBlur", 5, 0, false)
                        ),
                        true, new float[]{0.0F, 0.25F, 0.0F}, 0.03f, true,
                        "EXP2", 0.0f, 1.0f
                ),
                new LayerConfig("The Great Fault ", true, true, new int[]{135, 203}, 10, 0, 5, "#FFFFFF",
                        Arrays.asList(
                                new CurseConfig("potion", "minecraft:hunger", 8, 2, false),
                                new CurseConfig("potion", "minecraft:slowness", 8, 2, false),
                                new CurseConfig("potion", "minecraft:nausea", 8, 0, false),
                                new CurseConfig("filter", "inverted", 8, 0, false)
                        ),
                        false, new float[]{1.0F, 1.0F, 1.0F}, 0.03f, true,
                        "EXP2", 0.0f, 1.0f
                ),
                new LayerConfig("Goblet of Giants", true, true, new int[]{98, 135}, 10, 0, 10, "#90EE90",
                        Arrays.asList(
                                new CurseConfig("potion", "minecraft:wither", 8, 1, false),
                                new CurseConfig("filter", "red", 8, 0, true),
                                new CurseConfig("other", "bleeding", 8, 3, false)
                        ),
                        true, new float[]{0.565F, 0.933F, 0.565F}, 0.03f, true,
                        "EXP2", 0.0f, 1.0f
                ),
                new LayerConfig("Sea of Corpses", true, true, new int[]{40, 98}, 10, 0, 15, "#00008B",
                        Arrays.asList(
                                new CurseConfig("potion", "minecraft:blindness", 8, 0, false),
                                new CurseConfig("filter", "inverted", 8, 0, false),
                                new CurseConfig("other", "confusion", 8, 0, false)
                        ),
                        true, new float[]{0.294F, 0.0F, 0.51F}, 0.0f, true,
                        "LINEAR", 0.05f, 0.60f
                ),
                new LayerConfig("Capital of the Unreturned", true, true, new int[]{0, 40}, 10, 0, 50, "#FFD700",
                        Arrays.asList(
                                new CurseConfig("potion", "minecraft:wither", 9, 2, false),
                                new CurseConfig("other", "hollow", 20, 0, true),
                                new CurseConfig("filter", "red", 9, 0, true)
                        ),
                        true, new float[]{0.933F, 0.902F, 0.722F}, 0.03f, true,
                        "EXP2", 0.0f, 1.0f
                )
        ));
        dimensions.put(0, overworld);
        defaultConfig.setDimensions(dimensions);

        return defaultConfig;
    }

    public static int hexStringToInt(String hexColor) {
        if (hexColor == null || hexColor.isEmpty()) {
            return 0xFFFFFFFF;
        }

        String cleanHex = hexColor.startsWith("#") ? hexColor.substring(1) : hexColor;

        try {
            if (cleanHex.length() == 6) {
                cleanHex = "FF" + cleanHex;
            } else if (cleanHex.length() != 8) {
                return 0xFFFFFFFF;
            }
            return (int) Long.parseLong(cleanHex, 16);
        } catch (NumberFormatException e) {
            return 0xFFFFFFFF;
        }
    }


    public static class ConfigData {
        private HudConfig hudConfig;
        private float hollowJumpPower;
        private List<CustomCompassConfig> customCompasses;
        private boolean noWayBack;
        private boolean customCompass3DLock;
        private boolean disableInventoryInHollow;
        private boolean enable2DEntityDistanceScaling;

        private List<CustomScapegoatItem> customScapegoatItems;

        private Map<Integer, DimensionConfig> dimensions;

        public ConfigData() {
            this.dimensions = new HashMap<>();
            this.customScapegoatItems = new ArrayList<>();
            this.customCompasses = new ArrayList<>();

            this.hudConfig = new HudConfig();
            this.hollowJumpPower = 0.5F;
            this.noWayBack = true;
            this.customCompass3DLock = true;
            this.disableInventoryInHollow = true;
            this.enable2DEntityDistanceScaling = true;
        }

        public Map<Integer, DimensionConfig> getDimensions() { return dimensions; }
        public void setDimensions(Map<Integer, DimensionConfig> dimensions) { this.dimensions = dimensions; }

        public List<CustomScapegoatItem> getCustomScapegoatItems() { return customScapegoatItems; }
        public void setCustomScapegoatItems(List<CustomScapegoatItem> customScapegoatItems) { this.customScapegoatItems = customScapegoatItems; }

        public HudConfig getHudConfig() { return hudConfig; }
        public void setHudConfig(HudConfig hudConfig) { this.hudConfig = hudConfig; }

        public float getHollowJumpPower() { return hollowJumpPower; }
        public void setHollowJumpPower(float hollowJumpPower) { this.hollowJumpPower = hollowJumpPower; }

        public List<CustomCompassConfig> getCustomCompasses() { return customCompasses; }
        public void setCustomCompasses(List<CustomCompassConfig> customCompasses) { this.customCompasses = customCompasses; }

        public boolean isNoWayBack() { return noWayBack; }
        public void setNoWayBack(boolean noWayBack) { this.noWayBack = noWayBack; }

        public boolean isCustomCompass3DLock() { return customCompass3DLock; }
        public void setCustomCompass3DLock(boolean customCompass3DLock) { this.customCompass3DLock = customCompass3DLock; }

        public boolean isDisableInventoryInHollow() { return disableInventoryInHollow; }
        public void setDisableInventoryInHollow(boolean disableInventoryInHollow) { this.disableInventoryInHollow = disableInventoryInHollow; }

        public boolean isEnable2DEntityDistanceScaling() { return enable2DEntityDistanceScaling; }
        public void setEnable2DEntityDistanceScaling(boolean enable2DEntityDistanceScaling) { this.enable2DEntityDistanceScaling = enable2DEntityDistanceScaling; }
    }

    public static class DimensionConfig {
        private boolean enabled;
        private List<LayerConfig> layers;

        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }
        public List<LayerConfig> getLayers() { return layers; }
        public void setLayers(List<LayerConfig> layers) { this.layers = layers; }
    }

    public static class LayerConfig {
        private String name;
        private boolean enabled;
        private boolean hudEnabled;
        private int[] range;
        private int threshold;
        private int continuousTriggerInterval;
        private int durabilityCost;
        private String color;

        private List<CurseConfig> curses;

        private boolean showTimeDistance;

        private boolean fogEnabled;
        private float[] fogColorRGB;
        private float fogDensity;
        private String fogType = "EXP2";
        private float fogStart = 0.0F;
        private float fogEnd = 1.0F;


        public LayerConfig() {
            this.name = "New Layer";
            this.enabled = true;
            this.hudEnabled = true;
            this.range = new int[]{0, 64};
            this.threshold = 50;
            this.continuousTriggerInterval = 10;
            this.durabilityCost = 1;
            this.color = "#FFFFFF";
            this.curses = new ArrayList<>();
            this.showTimeDistance = true;
            this.fogEnabled = false;
            this.fogColorRGB = new float[]{1.0F, 1.0F, 1.0F};
            this.fogDensity = 0.05f;
            this.fogType = "EXP2";
            this.fogStart = 0.0f;
            this.fogEnd = 1.0f;
        }

        public LayerConfig(String name, boolean enabled, boolean hudEnabled, int[] range, int threshold,
                           int continuousTriggerInterval, int durabilityCost, String color,
                           List<CurseConfig> curses, boolean fogEnabled, float[] fogColorRGB,
                           float fogDensity, boolean showTimeDistance,
                           String fogType, float fogStart, float fogEnd) {
            this.name = name;
            this.enabled = enabled;
            this.hudEnabled = hudEnabled;
            this.range = range;
            this.threshold = threshold;
            this.continuousTriggerInterval = continuousTriggerInterval;
            this.durabilityCost = durabilityCost;
            this.color = color;
            this.curses = curses;
            this.fogEnabled = fogEnabled;
            this.fogColorRGB = fogColorRGB;
            this.fogDensity = fogDensity;
            this.showTimeDistance = showTimeDistance;
            this.fogType = fogType;
            this.fogStart = fogStart;
            this.fogEnd = fogEnd;
        }

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }
        public boolean isHudEnabled() { return hudEnabled; }
        public void setHudEnabled(boolean hudEnabled) { this.hudEnabled = hudEnabled; }
        public int[] getRange() { return range; }
        public void setRange(int[] range) { this.range = range; }
        public int getThreshold() { return threshold; }
        public void setThreshold(int threshold) { this.threshold = threshold; }
        public int getContinuousTriggerInterval() { return continuousTriggerInterval; }
        public void setContinuousTriggerInterval(int continuousTriggerInterval) { this.continuousTriggerInterval = continuousTriggerInterval; }
        public int getDurabilityCost() { return durabilityCost; }
        public void setDurabilityCost(int durabilityCost) { this.durabilityCost = durabilityCost; }
        public String getColor() { return color; }
        public void setColor(String color) { this.color = color; }
        public List<CurseConfig> getCurses() { return curses; }
        public void setCurses(List<CurseConfig> curses) { this.curses = curses; }

        public boolean isShowTimeDistance() { return showTimeDistance; }
        public void setShowTimeDistance(boolean showTimeDistance) { this.showTimeDistance = showTimeDistance; }

        public boolean isFogEnabled() { return fogEnabled; }
        public void setFogEnabled(boolean fogEnabled) { this.fogEnabled = fogEnabled; }
        public float[] getFogColorRGB() { return fogColorRGB; }
        public void setFogColorRGB(float[] fogColorRGB) { this.fogColorRGB = fogColorRGB; }
        public float getFogDensity() { return fogDensity; }
        public void setFogDensity(float fogDensity) { this.fogDensity = fogDensity; }

        public String getFogType() { return fogType; }
        public void setFogType(String fogType) { this.fogType = fogType; }
        public float getFogStart() { return fogStart; }
        public void setFogStart(float fogStart) { this.fogStart = fogStart; }
        public float getFogEnd() { return fogEnd; }
        public void setFogEnd(float fogEnd) { this.fogEnd = fogEnd; }

        public boolean isContinuous() { return this.continuousTriggerInterval > 0; }
    }

    public static class CurseConfig {
        private String type;
        private String name;
        private int duration;
        private int amplifier;
        private boolean isBooleanValue;
        private String command;

        public CurseConfig() {
            this.type = "potion";
            this.name = "regeneration";
            this.duration = 5;
            this.amplifier = 1;
            this.isBooleanValue = false;
            this.command = "";
        }
        public CurseConfig(String type, String name, int duration, int amplifier, boolean isBooleanValue) {
            this(type, name, duration, amplifier, isBooleanValue, "");
        }
        public CurseConfig(String type, String name, int duration, int amplifier, boolean isBooleanValue, String command) {
            this.type = type;
            this.name = name;
            this.duration = duration;
            this.amplifier = amplifier;
            this.isBooleanValue = isBooleanValue;
            this.command = command;
        }

        public String getType() { return type; }
        public void setType(String type) { this.type = type; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public int getDuration() { return duration; }
        public void setDuration(int duration) { this.duration = duration; }
        public int getAmplifier() { return amplifier; }
        public void setAmplifier(int amplifier) { this.amplifier = amplifier; }
        public boolean isBooleanValue() { return isBooleanValue; }
        public void setBooleanValue(boolean isBooleanValue) { this.isBooleanValue = isBooleanValue; }
        public String getCommand() { return command; }
        public void setCommand(String command) { this.command = command; }
    }

    public static class CustomScapegoatItem {
        private String itemId;
        private int dimensionId;

        public CustomScapegoatItem() {}
        public CustomScapegoatItem(String itemId, int dimensionId) {
            this.itemId = itemId;
            this.dimensionId = dimensionId;
        }
        public String getItemId() { return itemId; }
        public void setItemId(String itemId) { this.itemId = itemId; }
        public int getDimensionId() { return dimensionId; }
        public void setDimensionId(int dimensionId) { this.dimensionId = dimensionId; }
    }

    public static class CustomCompassConfig {
        private String itemId;
        private int[] targetPos;
        private int targetDim;

        public CustomCompassConfig() {}
        public CustomCompassConfig(String itemId, int[] targetPos, int targetDim) {
            this.itemId = itemId;
            this.targetPos = targetPos;
            this.targetDim = targetDim;
        }

        public String getItemId() { return itemId; }
        public void setItemId(String itemId) { this.itemId = itemId; }
        public int[] getTargetPos() { return targetPos; }
        public void setTargetPos(int[] targetPos) { this.targetPos = targetPos; }
        public int getTargetDim() { return targetDim; }
        public void setTargetDim(int targetDim) { this.targetDim = targetDim; }
    }

    public static class HudConfig {
        private boolean enabled;
        private float xPercentage;
        private float yPercentage;


        public HudConfig() {
            this.enabled = true;
            this.xPercentage = 0.05f;
            this.yPercentage = 0.05f;

        }

        public HudConfig(boolean enabled, float xPercentage, float yPercentage, boolean enableAutoArmorColoring, boolean enableStatusBarHUD) {
            this.enabled = enabled;
            this.xPercentage = xPercentage;
            this.yPercentage = yPercentage;

        }

        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }
        public float getXPercentage() { return xPercentage; }
        public void setXPercentage(float xPercentage) { this.xPercentage = xPercentage; }
        public float getYPercentage() { return yPercentage; }
        public void setYPercentage(float yPercentage) { this.yPercentage = yPercentage; }

    }
}