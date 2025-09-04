package com.abyess.config.ConfigGUI;


import com.abyess.config.ModConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiTextField;

import java.io.IOException;

public class Item {

    public static class ItemMenuGUI extends GuiScreen {
        private GuiScreen parentScreen;

        public ItemMenuGUI(GuiScreen parent) {
            this.parentScreen = parent;
        }

        @Override
        public void initGui() {
            this.buttonList.clear();
            this.buttonList.add(new GuiButton(0, this.width / 2 - 100, this.height / 4 + 48, 200, 20, "CustomStarCompass"));
            this.buttonList.add(new GuiButton(1, this.width / 2 - 100, this.height / 4 + 72, 200, 20, "CustomScapegoat"));
            this.buttonList.add(new GuiButton(2, this.width / 2 - 100, this.height / 4 + 96, 200, 20, "Back"));
        }

        @Override
        protected void actionPerformed(GuiButton button) throws IOException {
            if (button.id == 0) {
                Minecraft.getMinecraft().displayGuiScreen(new StarCompassConfigListGUI(this));
            } else if (button.id == 1) {
                Minecraft.getMinecraft().displayGuiScreen(new ScapegoatConfigListGUI(this));
            } else if (button.id == 2) {
                Minecraft.getMinecraft().displayGuiScreen(parentScreen);
            }
        }

        @Override
        public void drawScreen(int mouseX, int mouseY, float partialTicks) {
            this.drawDefaultBackground();
            this.drawCenteredString(this.fontRenderer, "Custom Item Configuration", this.width / 2, 20, 0xFFFFFF);
            super.drawScreen(mouseX, mouseY, partialTicks);
        }
    }

    // NEW: Star Compass List GUI
    public static class StarCompassConfigListGUI extends GuiScreen {
        private GuiScreen parentScreen;

        public StarCompassConfigListGUI(GuiScreen parent) {
            this.parentScreen = parent;
        }

        @Override
        public void initGui() {
            this.buttonList.clear();
            int yPos = 40;

            for (int i = 0; i < ModConfig.getConfigData().getCustomCompasses().size(); i++) {
                ModConfig.CustomCompassConfig compass = ModConfig.getConfigData().getCustomCompasses().get(i);
                String buttonText = "Compass: " + compass.getItemId().substring(compass.getItemId().lastIndexOf(":") + 1);
                this.buttonList.add(new GuiButton(100 + i, this.width / 2 - 100, yPos, 150, 20, buttonText));
                this.buttonList.add(new GuiButton(200 + i, this.width / 2 + 55, yPos, 45, 20, "Delete"));
                yPos += 24;
            }

            this.buttonList.add(new GuiButton(0, this.width / 2 - 100, yPos, 98, 20, "Add Compass"));
            this.buttonList.add(new GuiButton(1, this.width / 2 + 2, yPos, 98, 20, "Back"));
        }

        @Override
        protected void actionPerformed(GuiButton button) throws IOException {
            if (button.id == 0) { // Add Compass
                ModConfig.CustomCompassConfig newCompass = new ModConfig.CustomCompassConfig("minecraft:compass", new int[]{0, 64, 0}, 0);
                ModConfig.getConfigData().getCustomCompasses().add(newCompass);
                ModConfig.saveConfig();
                initGui(); // Refresh
            } else if (button.id == 1) { // Back
                Minecraft.getMinecraft().displayGuiScreen(parentScreen);
            } else if (button.id >= 100 && button.id < 200) { // Edit Compass
                int index = button.id - 100;
                if (index < ModConfig.getConfigData().getCustomCompasses().size()) {
                    ModConfig.CustomCompassConfig compass = ModConfig.getConfigData().getCustomCompasses().get(index);
                    Minecraft.getMinecraft().displayGuiScreen(new StarCompassConfigGUI(this, compass));
                }
            } else if (button.id >= 200) { // Delete Compass
                int index = button.id - 200;
                if (index < ModConfig.getConfigData().getCustomCompasses().size()) {
                    ModConfig.getConfigData().getCustomCompasses().remove(index);
                    ModConfig.saveConfig();
                    initGui(); // Refresh
                }
            }
        }

        @Override
        public void drawScreen(int mouseX, int mouseY, float partialTicks) {
            this.drawDefaultBackground();
            this.drawCenteredString(this.fontRenderer, "Star Compass Configurations", this.width / 2, 20, 0xFFFFFF);
            super.drawScreen(mouseX, mouseY, partialTicks);
        }
    }

    // NEW: Star Compass Configuration GUI
    public static class StarCompassConfigGUI extends GuiScreen {
        private GuiScreen parentScreen;
        private ModConfig.CustomCompassConfig compass;
        private GuiTextField itemIdField;
        private GuiTextField targetXField;
        private GuiTextField targetYField;
        private GuiTextField targetZField;
        private GuiTextField targetDimField;
        // private GuiButton enabledButton; // 1. 删除 enabledButton 成员变量

        public StarCompassConfigGUI(GuiScreen parent, ModConfig.CustomCompassConfig compass) {
            this.parentScreen = parent;
            this.compass = compass;
        }

        @Override
        public void initGui() {
            this.buttonList.clear();

            itemIdField = new GuiTextField(0, fontRenderer, width / 2 - 100, 40, 200, 20);
            itemIdField.setText(compass.getItemId());

            targetXField = new GuiTextField(1, fontRenderer, width / 2 - 100, 70, 60, 20);
            targetXField.setText(String.valueOf(compass.getTargetPos()[0]));
            targetYField = new GuiTextField(2, fontRenderer, width / 2 - 30, 70, 60, 20);
            targetYField.setText(String.valueOf(compass.getTargetPos()[1]));
            targetZField = new GuiTextField(3, fontRenderer, width / 2 + 40, 70, 60, 20);
            targetZField.setText(String.valueOf(compass.getTargetPos()[2]));

            targetDimField = new GuiTextField(4, fontRenderer, width / 2 - 100, 100, 200, 20);
            targetDimField.setText(String.valueOf(compass.getTargetDim()));



            this.buttonList.add(new GuiButton(6, width / 2 - 100, 130, 200, 20, "Save & Back")); // Y 从 160 改为 130
        }

        @Override
        protected void actionPerformed(GuiButton button) throws IOException {

            if (button.id == 6) { // Save & Back (现在 ID 5 不再存在，所以 Save & Back 的 ID 依然是 6)
                saveChanges();
                Minecraft.getMinecraft().displayGuiScreen(parentScreen);
            }
        }

        public void saveChanges() {
            compass.setItemId(itemIdField.getText());
            try {
                int x = Integer.parseInt(targetXField.getText());
                int y = Integer.parseInt(targetYField.getText());
                int z = Integer.parseInt(targetZField.getText());
                compass.setTargetPos(new int[]{x, y, z});
            } catch (NumberFormatException e) {
                // Keep original value
            }
            try {
                compass.setTargetDim(Integer.parseInt(targetDimField.getText()));
            } catch (NumberFormatException e) {
                // Keep original value
            }
            // compass.isEnabled() 的状态不再通过 GUI 控制，它将保持在 ModConfig.CustomCompassConfig 中设置的值
            ModConfig.saveConfig();
        }

        @Override
        public void drawScreen(int mouseX, int mouseY, float partialTicks) {
            this.drawDefaultBackground();
            this.drawCenteredString(this.fontRenderer, "Star Compass Configuration", this.width / 2, 20, 0xFFFFFF);

            this.drawString(fontRenderer, "Item ID:", width / 2 - 154, 46, 0xAAAAAA);
            this.drawString(fontRenderer, "Target(X Y Z):", width / 2 - 175, 76, 0xAAAAAA);
            this.drawString(fontRenderer, "Target DimID:", width / 2 - 170, 106, 0xAAAAAA);
            // 5. 删除绘制 Enabled 标签的逻辑
            // this.drawString(fontRenderer, "Enabled:", width / 2 - 154, 136, 0xAAAAAA); // 原来是 136, 现在这个标签也应该移除

            itemIdField.drawTextBox();
            targetXField.drawTextBox();
            targetYField.drawTextBox();
            targetZField.drawTextBox();
            targetDimField.drawTextBox();

            super.drawScreen(mouseX, mouseY, partialTicks);
        }

        @Override
        protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
            super.mouseClicked(mouseX, mouseY, mouseButton);
            itemIdField.mouseClicked(mouseX, mouseY, mouseButton);
            targetXField.mouseClicked(mouseX, mouseY, mouseButton);
            targetYField.mouseClicked(mouseX, mouseY, mouseButton);
            targetZField.mouseClicked(mouseX, mouseY, mouseButton);
            targetDimField.mouseClicked(mouseX, mouseY, mouseButton);
        }

        @Override
        protected void keyTyped(char typedChar, int keyCode) throws IOException {
            super.keyTyped(typedChar, keyCode);
            itemIdField.textboxKeyTyped(typedChar, keyCode);
            targetXField.textboxKeyTyped(typedChar, keyCode);
            targetYField.textboxKeyTyped(typedChar, keyCode);
            targetZField.textboxKeyTyped(typedChar, keyCode);
            targetDimField.textboxKeyTyped(typedChar, keyCode);
        }
    }

    // NEW: Scapegoat Item List GUI
    public static class ScapegoatConfigListGUI extends GuiScreen {
        private GuiScreen parentScreen;

        public ScapegoatConfigListGUI(GuiScreen parent) {
            this.parentScreen = parent;
        }

        @Override
        public void initGui() {
            this.buttonList.clear();
            int yPos = 40;

            for (int i = 0; i < ModConfig.getConfigData().getCustomScapegoatItems().size(); i++) {
                ModConfig.CustomScapegoatItem item = ModConfig.getConfigData().getCustomScapegoatItems().get(i);
                String buttonText = "Item: " + item.getItemId().substring(item.getItemId().lastIndexOf(":") + 1) + " (Dim: " + item.getDimensionId() + ")";
                this.buttonList.add(new GuiButton(100 + i, this.width / 2 - 100, yPos, 150, 20, buttonText));
                this.buttonList.add(new GuiButton(200 + i, this.width / 2 + 55, yPos, 45, 20, "Delete"));
                yPos += 24;
            }

            this.buttonList.add(new GuiButton(0, this.width / 2 - 100, yPos, 98, 20, "Add Item"));
            this.buttonList.add(new GuiButton(1, this.width / 2 + 2, yPos, 98, 20, "Back"));
        }

        @Override
        protected void actionPerformed(GuiButton button) throws IOException {
            if (button.id == 0) { // Add Item
                ModConfig.CustomScapegoatItem newItem = new ModConfig.CustomScapegoatItem("minecraft:stick", 0);
                ModConfig.getConfigData().getCustomScapegoatItems().add(newItem);
                ModConfig.saveConfig();
                initGui(); // Refresh
            } else if (button.id == 1) { // Back
                Minecraft.getMinecraft().displayGuiScreen(parentScreen);
            } else if (button.id >= 100 && button.id < 200) { // Edit Item
                int index = button.id - 100;
                if (index < ModConfig.getConfigData().getCustomScapegoatItems().size()) {
                    ModConfig.CustomScapegoatItem item = ModConfig.getConfigData().getCustomScapegoatItems().get(index);
                    Minecraft.getMinecraft().displayGuiScreen(new ScapegoatConfigGUI(this, item));
                }
            } else if (button.id >= 200) { // Delete Item
                int index = button.id - 200;
                if (index < ModConfig.getConfigData().getCustomScapegoatItems().size()) {
                    ModConfig.getConfigData().getCustomScapegoatItems().remove(index);
                    ModConfig.saveConfig();
                    initGui(); // Refresh
                }
            }
        }

        @Override
        public void drawScreen(int mouseX, int mouseY, float partialTicks) {
            this.drawDefaultBackground();
            this.drawCenteredString(this.fontRenderer, "Scapegoat Item Configurations", this.width / 2, 20, 0xFFFFFF);
            super.drawScreen(mouseX, mouseY, partialTicks);
        }
    }

    // NEW: Scapegoat Item Configuration GUI
    public static class ScapegoatConfigGUI extends GuiScreen {
        private GuiScreen parentScreen;
        private ModConfig.CustomScapegoatItem item;
        private GuiTextField itemIdField;
        private GuiTextField dimensionIdField;

        public ScapegoatConfigGUI(GuiScreen parent, ModConfig.CustomScapegoatItem item) {
            this.parentScreen = parent;
            this.item = item;
        }

        @Override
        public void initGui() {
            this.buttonList.clear();

            itemIdField = new GuiTextField(0, fontRenderer, width / 2 - 100, 40, 200, 20);
            itemIdField.setText(item.getItemId());

            dimensionIdField = new GuiTextField(1, fontRenderer, width / 2 - 100, 70, 200, 20);
            dimensionIdField.setText(String.valueOf(item.getDimensionId()));

            this.buttonList.add(new GuiButton(2, width / 2 - 100, this.height - 30, 200, 20, "Save & Back"));
        }

        @Override
        protected void actionPerformed(GuiButton button) throws IOException {
            if (button.id == 2) { // Save & Back
                saveChanges();
                Minecraft.getMinecraft().displayGuiScreen(parentScreen);
            }
        }

        public void saveChanges() {
            item.setItemId(itemIdField.getText());
            try {
                item.setDimensionId(Integer.parseInt(dimensionIdField.getText()));
            } catch (NumberFormatException e) {
                // Keep original value
            }
            ModConfig.saveConfig();
        }

        @Override
        public void drawScreen(int mouseX, int mouseY, float partialTicks) {
            this.drawDefaultBackground();
            this.drawCenteredString(this.fontRenderer, "Scapegoat Item Configuration", this.width / 2, 20, 0xFFFFFF);

            this.drawString(fontRenderer, "Item ID:", width / 2 - 154, 46, 0xAAAAAA);
            this.drawString(fontRenderer, "Dimension ID:", width / 2 - 165, 76, 0xAAAAAA);

            itemIdField.drawTextBox();
            dimensionIdField.drawTextBox();

            super.drawScreen(mouseX, mouseY, partialTicks);
        }

        @Override
        protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
            super.mouseClicked(mouseX, mouseY, mouseButton);
            itemIdField.mouseClicked(mouseX, mouseY, mouseButton);
            dimensionIdField.mouseClicked(mouseX, mouseY, mouseButton);
        }

        @Override
        protected void keyTyped(char typedChar, int keyCode) throws IOException {
            super.keyTyped(typedChar, keyCode);
            itemIdField.textboxKeyTyped(typedChar, keyCode);
            dimensionIdField.textboxKeyTyped(typedChar, keyCode);
        }
    }
}
