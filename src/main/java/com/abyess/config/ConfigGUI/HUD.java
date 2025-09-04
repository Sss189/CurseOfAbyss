package com.abyess.config.ConfigGUI;

import com.abyess.config.ModConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiTextField;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class HUD {

    /**
     * HUD 主菜单界面。
     * 允许玩家选择配置 Curse HUD
     */
    public static class HudMainMenuGUI extends GuiScreen {
        private GuiScreen parentScreen;

        public HudMainMenuGUI(GuiScreen parent) {
            this.parentScreen = parent;
        }

        @Override
        public void initGui() {
            this.buttonList.clear();

            // Curse HUD Display Settings Button
            this.buttonList.add(new GuiButton(0, this.width / 2 - 100, this.height / 4 + 48, 200, 20, "Curse HUD"));



            // Back button
            this.buttonList.add(new GuiButton(1, this.width / 2 - 100, this.height / 4 + 96, 200, 20, "Back"));
        }

        @Override
        protected void actionPerformed(GuiButton button) throws IOException {
            if (button.id == 0) {
                Minecraft.getMinecraft().displayGuiScreen(new CurseHudDisplayConfigGUI(this));
//            } else if (button.id == 1) {
//                Minecraft.getMinecraft().displayGuiScreen(new StatusBarHudGUI(this));
            } else if (button.id == 1) {
                Minecraft.getMinecraft().displayGuiScreen(parentScreen);
            }
        }

        @Override
        public void drawScreen(int mouseX, int mouseY, float partialTicks) {
            this.drawDefaultBackground();
            this.drawCenteredString(this.fontRenderer, "HUD Settings", this.width / 2, 20, 0xFFFFFF);
            super.drawScreen(mouseX, mouseY, partialTicks);
        }
    }

    /**
     * Curse HUD 的详细配置界面。
     * 允许玩家启用/禁用HUD，设置其X/Y百分比位置。
     */
    public static class CurseHudDisplayConfigGUI extends GuiScreen {
        private GuiScreen parentScreen;
        private ModConfig.HudConfig hudConfig;
        private GuiButton enabledButton;
        private GuiTextField xField;
        private GuiTextField yField;

        public CurseHudDisplayConfigGUI(GuiScreen parent) {
            this.parentScreen = parent;
            this.hudConfig = ModConfig.getConfigData().getHudConfig();
        }

        @Override
        public void initGui() {
            this.buttonList.clear();
            int yPos = 40; // Starting Y position for the first element

            // Curse HUD Enabled/Disabled button
            enabledButton = new GuiButton(0, width / 2 - 100, yPos, 200, 20,
                    "Curse HUD Enabled: " + (hudConfig.isEnabled() ? "Yes" : "No"));
            this.buttonList.add(enabledButton);
            yPos += 30;

            // Curse HUD X Percentage input field
            xField = new GuiTextField(1, fontRenderer, width / 2 - 100, yPos, 200, 20);
            // ✨ 从 ModConfig 获取 0.0-1.0 的值，转换为 1-100% 显示
            xField.setText(String.valueOf(Math.round(hudConfig.getXPercentage() * 100)));
            xField.setEnableBackgroundDrawing(true);
            xField.setMaxStringLength(3); // 允许输入 1-100
            yPos += 30;

            // Curse HUD Y Percentage input field
            yField = new GuiTextField(2, fontRenderer, width / 2 - 100, yPos, 200, 20);
            // ✨ 从 ModConfig 获取 0.0-1.0 的值，转换为 1-100% 显示
            yField.setText(String.valueOf(Math.round(hudConfig.getYPercentage() * 100)));
            yField.setEnableBackgroundDrawing(true);
            yField.setMaxStringLength(3); // 允许输入 1-100
            yPos += 30;

            // Save and Back button
            this.buttonList.add(new GuiButton(3, width / 2 - 100, this.height - 30, 200, 20, "Save & Back"));
        }

        @Override
        protected void actionPerformed(GuiButton button) throws IOException {
            if (button.id == 0) { // Curse HUD Enabled/Disabled button
                hudConfig.setEnabled(!hudConfig.isEnabled());
                enabledButton.displayString = "Curse HUD Enabled: " + (hudConfig.isEnabled() ? "Yes" : "No");
            } else if (button.id == 3) { // Save and Back
                saveChanges();
                Minecraft.getMinecraft().displayGuiScreen(parentScreen);
            }
        }

        /**
         * 保存 Curse HUD 配置界面上的更改。
         * 主要处理 X/Y 百分比位置的解析和保存。
         */
        public void saveChanges() {
            // 保存 X 百分比位置
            try {
                // ✨ 从用户输入获取 1-100 的值，转换为 0.0-1.0 保存
                float xPercentageInput = Float.parseFloat(xField.getText());
                if (xPercentageInput >= 0.0f && xPercentageInput <= 100.0f) { // 验证 0-100
                    hudConfig.setXPercentage(xPercentageInput / 100.0f); // 转换为 0.0-1.0
                } else {
                    System.err.println("Invalid X percentage input. Value must be between 0 and 100. Keeping previous value.");
                }
            } catch (NumberFormatException e) {
                System.err.println("Invalid X percentage input. Keeping previous value.");
            }

            // 保存 Y 百分比位置
            try {
                // ✨ 从用户输入获取 1-100 的值，转换为 0.0-1.0 保存
                float yPercentageInput = Float.parseFloat(yField.getText());
                if (yPercentageInput >= 0.0f && yPercentageInput <= 100.0f) { // 验证 0-100
                    hudConfig.setYPercentage(yPercentageInput / 100.0f); // 转换为 0.0-1.0
                } else {
                    System.err.println("Invalid Y percentage input. Value must be between 0 and 100. Keeping previous value.");
                }
            } catch (NumberFormatException e) {
                System.err.println("Invalid Y percentage input. Keeping previous value.");
            }

            ModConfig.saveConfig(); // 保存到文件
        }

        @Override
        public void drawScreen(int mouseX, int mouseY, float partialTicks) {
            this.drawDefaultBackground();
            this.drawCenteredString(this.fontRenderer, "Curse HUD Settings", this.width / 2, 20, 0xFFFFFF);

            // ✨ 更新文本标签以指示输入为百分比 (%)
            this.drawString(fontRenderer, "Curse HUD X(%):", width / 2 - 185, xField.y + 6, 0xAAAAAA);
            this.drawString(fontRenderer, "Curse HUD Y(%):", width / 2 - 185, yField.y + 6, 0xAAAAAA);

            xField.drawTextBox();
            yField.drawTextBox();

            super.drawScreen(mouseX, mouseY, partialTicks);
        }

        @Override
        protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
            super.mouseClicked(mouseX, mouseY, mouseButton);
            xField.mouseClicked(mouseX, mouseY, mouseButton);
            yField.mouseClicked(mouseX, mouseY, mouseButton);
        }

        @Override
        protected void keyTyped(char typedChar, int keyCode) throws IOException {
            super.keyTyped(typedChar, keyCode);

            if (Character.isDigit(typedChar) || keyCode == 14) { // 14 是退格键的键码
                xField.textboxKeyTyped(typedChar, keyCode);
                yField.textboxKeyTyped(typedChar, keyCode);
            }
        }
    }


}