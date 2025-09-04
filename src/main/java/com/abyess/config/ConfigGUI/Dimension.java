package com.abyess.config.ConfigGUI;


import com.abyess.config.ModConfig;
import it.unimi.dsi.fastutil.ints.IntSortedSet;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiTextField;
import net.minecraftforge.common.DimensionManager;
import org.lwjgl.input.Mouse;

import java.io.IOException;
import java.util.*;

public class Dimension {
    public static class DimensionListGUI extends GuiScreen {
        private GuiScreen parentScreen;
        private GuiTextField dimensionIdField;
        private Map<GuiButton, Integer> editButtons = new HashMap<>();
        private Map<GuiButton, Integer> deleteButtons = new HashMap<>();

        private boolean dropdownOpen = false;
        private List<String> dropdownOptions = new ArrayList<>();
        private int dropdownScroll = 0;
        private static final int DROPDOWN_ITEM_HEIGHT = 20;
        private static final int DROPDOWN_VISIBLE_ITEMS = 5;

        private GuiButton selectDimensionButton;

        public DimensionListGUI(GuiScreen parent) {
            this.parentScreen = parent;
        }

        @Override
        public void initGui() {
            this.buttonList.clear();
            this.editButtons.clear();
            this.deleteButtons.clear();

            dimensionIdField = new GuiTextField(0, this.fontRenderer, this.width / 2 - 100, 40, 150, 20);
            dimensionIdField.setMaxStringLength(10);
            dimensionIdField.setText("");

            selectDimensionButton = new GuiButton(1, this.width / 2 + 55, 40, 45, 20, "Select");
            this.buttonList.add(selectDimensionButton);

            this.buttonList.add(new GuiButton(0, this.width / 2 + 105, 40, 45, 20, "Add"));

            int yPos = 70;
            for (Integer dimId : ModConfig.getConfigData().getDimensions().keySet()) {
                String dimName = getDimensionName(dimId);
                GuiButton editButton = new GuiButton(100 + this.buttonList.size(), this.width / 2 - 100, yPos, 150, 20, dimName);
                GuiButton deleteButton = new GuiButton(100 + this.buttonList.size() + 1, this.width / 2 + 55, yPos, 45, 20, "Delete");

                this.buttonList.add(editButton);
                this.buttonList.add(deleteButton);

                editButtons.put(editButton, dimId);
                deleteButtons.put(deleteButton, dimId);

                yPos += 24;
            }

            this.buttonList.add(new GuiButton(99, this.width / 2 - 100, this.height - 30, 200, 20, "Back"));

            populateDropdownOptions();
        }

        private void populateDropdownOptions() {
            dropdownOptions.clear();

            Map<net.minecraft.world.DimensionType, IntSortedSet> registeredDimsMap = DimensionManager.getRegisteredDimensions();

            for (IntSortedSet dimIdSet : registeredDimsMap.values()) {
                for (int dimId : dimIdSet) {
                    String dimName = getDimensionName(dimId);
                    dropdownOptions.add(dimName + " (" + dimId + ")");
                }
            }
            Collections.sort(dropdownOptions);
        }

        @Override
        protected void actionPerformed(GuiButton button) throws IOException {
            if (button.id == 0) { // Add button
                try {
                    int dimId = Integer.parseInt(dimensionIdField.getText());
                    if (!ModConfig.getConfigData().getDimensions().containsKey(dimId)) {
                        ModConfig.DimensionConfig newDim = new ModConfig.DimensionConfig();
                        newDim.setEnabled(true);
                        newDim.setLayers(new ArrayList<>());
                        ModConfig.getConfigData().getDimensions().put(dimId, newDim);
                        ModConfig.saveConfig();
                        initGui();
                    }
                } catch (NumberFormatException e) {
                    // Invalid input, optionally display a message
                }
            } else if (button.id == 99) { // Back button
                Minecraft.getMinecraft().displayGuiScreen(parentScreen);
            } else if (button.id == selectDimensionButton.id) { // Select Dimension button
                dropdownOpen = !dropdownOpen;
                dropdownScroll = 0;
            }
            else if (editButtons.containsKey(button)) {
                int dimId = editButtons.get(button);
                ModConfig.DimensionConfig config = ModConfig.getConfigData().getDimensions().get(dimId);
                if (config != null) {
                    Minecraft.getMinecraft().displayGuiScreen(new DimensionConfigGUI(this, dimId, config));
                }
            } else if (deleteButtons.containsKey(button)) {
                int dimId = deleteButtons.get(button);
                ModConfig.getConfigData().getDimensions().remove(dimId);
                ModConfig.saveConfig();
                initGui();
            }
        }

        @Override
        public void drawScreen(int mouseX, int mouseY, float partialTicks) {
            this.drawDefaultBackground();
            this.drawCenteredString(this.fontRenderer, "Dimension List", this.width / 2, 20, 0xFFFFFF);
            this.drawString(fontRenderer, "Dimension ID:", this.width / 2 - 166, 46, 0xAAAAAA);

            dimensionIdField.drawTextBox();
            super.drawScreen(mouseX, mouseY, partialTicks);

            // 如果下拉菜单是打开状态，则绘制下拉菜单
            if (dropdownOpen && !dropdownOptions.isEmpty()) {
                // --- 关键修改点：调整 dropdownX 和 dropdownWidth ---
                // 让下拉菜单从 dimensionIdField 的起始 X 坐标开始
                int dropdownX = dimensionIdField.x;
                // 让下拉菜单的宽度与 dimensionIdField 的宽度相同
                int dropdownWidth = dimensionIdField.width;
                // Y 坐标仍在 selectDimensionButton 下方
                int dropdownY = selectDimensionButton.y + selectDimensionButton.height;

                int listHeight = Math.min(DROPDOWN_VISIBLE_ITEMS, dropdownOptions.size()) * DROPDOWN_ITEM_HEIGHT;

                // 绘制背景
                drawRect(dropdownX, dropdownY,
                        dropdownX + dropdownWidth, // 确保 X2 使用正确的宽度
                        dropdownY + listHeight,
                        0xFF000000); // 黑色全透明背景
                drawRect(dropdownX, dropdownY,
                        dropdownX + dropdownWidth, // 确保 X2 使用正确的宽度
                        dropdownY + listHeight,
                        0xAA222222); // 半透明深灰色背景

                // 绘制下拉列表中的项目
                int maxItems = Math.min(dropdownOptions.size() - dropdownScroll, DROPDOWN_VISIBLE_ITEMS);
                for (int i = 0; i < maxItems; i++) {
                    int index = i + dropdownScroll;
                    String option = dropdownOptions.get(index);
                    int itemY = dropdownY + i * DROPDOWN_ITEM_HEIGHT;

                    // 鼠标悬停高亮
                    if (mouseX >= dropdownX && mouseX < dropdownX + dropdownWidth &&
                            mouseY >= itemY && mouseY < itemY + DROPDOWN_ITEM_HEIGHT) {
                        drawRect(dropdownX, itemY,
                                dropdownX + dropdownWidth,
                                itemY + DROPDOWN_ITEM_HEIGHT,
                                0xAA555555); // 悬停高亮颜色
                    }

                    // 绘制文本
                    fontRenderer.drawString(option,
                            dropdownX + 5,
                            itemY + (DROPDOWN_ITEM_HEIGHT - 8) / 2, // 垂直居中
                            0xFFFFFF);
                }

                // 绘制滚动条
                if (dropdownOptions.size() > DROPDOWN_VISIBLE_ITEMS) {
                    // 滚动条的 X 坐标也应该与下拉菜单的右边缘对齐
                    int scrollbarX = dropdownX + dropdownWidth - 5;
                    int scrollbarHeight = (int) ((float) DROPDOWN_VISIBLE_ITEMS / dropdownOptions.size() * listHeight);
                    int scrollbarTop = dropdownY + (int) ((float) dropdownScroll / (dropdownOptions.size() - DROPDOWN_VISIBLE_ITEMS) * (listHeight - scrollbarHeight));

                    drawRect(scrollbarX, scrollbarTop, // 使用正确的 X 坐标
                            dropdownX + dropdownWidth, // 滚动条宽度 5 像素
                            scrollbarTop + scrollbarHeight,
                            0xFFFFFFFF); // 白色滚动条
                }
            }
        }

        @Override
        protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
            // 先处理自定义下拉列表的点击
            if (dropdownOpen) {
                // --- 关键修改点：调整 dropdownX 和 dropdownWidth ---
                int dropdownX = dimensionIdField.x;
                int dropdownWidth = dimensionIdField.width;
                int dropdownY = selectDimensionButton.y + selectDimensionButton.height;
                int listHeight = Math.min(DROPDOWN_VISIBLE_ITEMS, dropdownOptions.size()) * DROPDOWN_ITEM_HEIGHT;

                // 检查是否点击了下拉列表中的项
                if (mouseX >= dropdownX && mouseX <= dropdownX + dropdownWidth &&
                        mouseY >= dropdownY && mouseY <= dropdownY + listHeight) {

                    int clickedIndex = (mouseY - dropdownY) / DROPDOWN_ITEM_HEIGHT + dropdownScroll;
                    if (clickedIndex >= 0 && clickedIndex < dropdownOptions.size()) {
                        String selectedOption = dropdownOptions.get(clickedIndex);
                        int startIndex = selectedOption.lastIndexOf('(') + 1;
                        int endIndex = selectedOption.lastIndexOf(')');
                        if (startIndex > 0 && endIndex > startIndex) {
                            try {
                                int dimId = Integer.parseInt(selectedOption.substring(startIndex, endIndex));
                                dimensionIdField.setText(String.valueOf(dimId));
                            } catch (NumberFormatException e) {
                                // Should not happen
                            }
                        }
                        dropdownOpen = false;
                        return;
                    }
                } else {
                    dropdownOpen = false;
                }
            }

            dimensionIdField.mouseClicked(mouseX, mouseY, mouseButton);
            super.mouseClicked(mouseX, mouseY, mouseButton);
        }

        @Override
        public void handleMouseInput() throws IOException {
            super.handleMouseInput();

            if (dropdownOpen) {
                int scroll = Mouse.getEventDWheel();
                if (scroll != 0) {
                    scroll = scroll > 0 ? -1 : 1;
                    dropdownScroll = Math.max(0, Math.min(dropdownOptions.size() - DROPDOWN_VISIBLE_ITEMS,
                            dropdownScroll + scroll));
                }
            }
        }

        @Override
        protected void keyTyped(char typedChar, int keyCode) throws IOException {
            if (dropdownOpen && keyCode == 1) {
                dropdownOpen = false;
                return;
            }

            super.keyTyped(typedChar, keyCode);
            dimensionIdField.textboxKeyTyped(typedChar, keyCode);
        }

        private String getDimensionName(int dimId) {
            switch (dimId) {
                case 0: return "Overworld";
                case -1: return "Nether";
                case 1: return "The End";
                case 7: return "Twilight Forest";
                default: return "Dimension " + dimId;
            }
        }
    }


    // Dimension configuration interface
    public static class DimensionConfigGUI extends GuiScreen {
        private GuiScreen parentScreen;
        private int dimensionId;
        private ModConfig.DimensionConfig config;
        private GuiButton enableButton;

        public DimensionConfigGUI(GuiScreen parent, int dimId, ModConfig.DimensionConfig config) {
            this.parentScreen = parent;
            this.dimensionId = dimId;
            this.config = config;
        }

        @Override
        public void initGui() {
            this.buttonList.clear();

            enableButton = new GuiButton(0, this.width / 2 - 100, 40, 200, 20,
                    "Enabled: " + (config.isEnabled() ? "Yes" : "No"));
            this.buttonList.add(enableButton);

            int yPos = 70;
            for (int i = 0; i < config.getLayers().size(); i++) {
                ModConfig.LayerConfig layer = config.getLayers().get(i);
                this.buttonList.add(new GuiButton(10 + i, this.width / 2 - 100, yPos, 150, 20, layer.getName()));
                this.buttonList.add(new GuiButton(30 + i, this.width / 2 + 55, yPos, 45, 20, "Delete"));
                yPos += 24;
            }

            this.buttonList.add(new GuiButton(1, this.width / 2 - 100, yPos, 98, 20, "Add Layer"));
            this.buttonList.add(new GuiButton(2, this.width / 2 + 2, yPos, 98, 20, "Back"));
        }

        @Override
        protected void actionPerformed(GuiButton button) throws IOException {
            if (button.id == 0) {
                config.setEnabled(!config.isEnabled());
                enableButton.displayString = "Enabled: " + (config.isEnabled() ? "Yes" : "No");
                ModConfig.saveConfig();
            } else if (button.id == 1) {
                ModConfig.LayerConfig newLayer = createDefaultLayer();
                config.getLayers().add(newLayer);
                ModConfig.saveConfig();
                initGui();
            } else if (button.id == 2) {
                Minecraft.getMinecraft().displayGuiScreen(parentScreen);
            } else if (button.id >= 10 && button.id < 30) {
                int layerIndex = button.id - 10;
                if (layerIndex < config.getLayers().size()) {
                    ModConfig.LayerConfig layer = config.getLayers().get(layerIndex);
                    Minecraft.getMinecraft().displayGuiScreen(new Layer.LayerConfigGUI(this, layer));
                }
            } else if (button.id >= 30) {
                int layerIndex = button.id - 30;
                if (layerIndex < config.getLayers().size()) {
                    config.getLayers().remove(layerIndex);
                    ModConfig.saveConfig();
                    initGui();
                }
            }
        }

        private ModConfig.LayerConfig createDefaultLayer() {
            ModConfig.LayerConfig layer = new ModConfig.LayerConfig();
            layer.setName("New Layer");
            layer.setEnabled(true);
            layer.setHudEnabled(true);
            layer.setRange(new int[]{0, 64});
            layer.setThreshold(10);
            layer.setContinuousTriggerInterval(10);
            layer.setDurabilityCost(1);
            layer.setColor("#FFFFFF");
            layer.setCurses(new ArrayList<>());
            return layer;
        }

        @Override
        public void drawScreen(int mouseX, int mouseY, float partialTicks) {
            this.drawDefaultBackground();
            String dimName = "Dimension " + dimensionId;
            switch (dimensionId) {
                case 0: dimName = "Overworld"; break;
                case -1: dimName = "Nether"; break;
                case 1: dimName = "The End"; break;
                case 7: dimName = "Twilight Forest"; break;
            }
            this.drawCenteredString(this.fontRenderer, dimName + " Configuration", this.width / 2, 20, 0xFFFFFF);
            super.drawScreen(mouseX, mouseY, partialTicks);
        }
    }

}
