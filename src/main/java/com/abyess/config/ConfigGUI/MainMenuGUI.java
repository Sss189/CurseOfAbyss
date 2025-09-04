package com.abyess.config.ConfigGUI;

import com.abyess.config.ModConfig; // 导入 ModConfig
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;

import java.io.IOException;

public class MainMenuGUI extends GuiScreen {
    private GuiScreen parentScreen;

    public MainMenuGUI(GuiScreen parent) {
        this.parentScreen = parent;
    }

    @Override
    public void initGui() {
        // Original y-coordinate comments are based on the request's provided code.
        // The y-coordinates are adjusted to make space for the new "Other" button.

        // Curse Configuration Button
        this.buttonList.add(new GuiButton(0, this.width / 2 - 100, this.height / 4 + 36, 200, 20, "Curse Configuration"));

        // Items Button
        this.buttonList.add(new GuiButton(1, this.width / 2 - 100, this.height / 4 + 60, 200, 20, "CustomItems"));

        // HUD Button - This will now open CurseHudSelectionGUI
        this.buttonList.add(new GuiButton(2, this.width / 2 - 100, this.height / 4 + 84, 200, 20, "HUD Settings")); // Changed text for clarity

        // NEW: Other Button
        // Placed below HUD, so it shifts the "Back" button down
        this.buttonList.add(new GuiButton(3, this.width / 2 - 100, this.height / 4 + 108, 200, 20, "Other"));

        // Back Button (adjusted ID and position)
        this.buttonList.add(new GuiButton(4, this.width / 2 - 100, this.height / 4 + 132, 200, 20, "Back"));
    }

    @Override
    protected void actionPerformed(GuiButton button) throws IOException {
        if (button.id == 0) {
            Minecraft.getMinecraft().displayGuiScreen(new Dimension.DimensionListGUI(this));
        } else if (button.id == 1) {
            Minecraft.getMinecraft().displayGuiScreen(new Item.ItemMenuGUI(this));
        } else if (button.id == 2) {
            // Now opens the CurseHudSelectionGUI
            Minecraft.getMinecraft().displayGuiScreen(new HUD.HudMainMenuGUI(this));
        } else if (button.id == 3) { // NEW: Handle Other button click
            Minecraft.getMinecraft().displayGuiScreen(new Other.OtherConfigGUI(this)); // Open the new OtherConfigGUI
        } else if (button.id == 4) { // Adjusted Back button ID
            Minecraft.getMinecraft().displayGuiScreen(parentScreen);
        }
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        this.drawDefaultBackground();
        // Y-coordinate for "Configuration System"
        int titleY = 20;
        this.drawCenteredString(this.fontRenderer, "Configuration System", this.width / 2, titleY, 0xFFFFFF);

        // 获取配置加载错误状态
        boolean hasError = ModConfig.hasConfigLoadError();

        // 提示消息的颜色和内容
        int messageColor = hasError ? 0xFFFF0000 : 0xFFAAAA00; // 红色 (FF0000) 或 黄色 (AAAA00)
        String line1 = hasError ? "Config file error or outdated format!" : "Sometimes after a game configuration update,";
        String line2 = hasError ? "Please delete 'abyess_curse_config.json' to regenerate." : "you need to delete the old config file to generate a new one.";
        String line3 = hasError ? "(You can also manually edit the JSON file.)" : "(You can also manually edit the JSON file.)"; // 新增针对错误的提示

        // 计算 Y 坐标
        int warningY = titleY + this.fontRenderer.FONT_HEIGHT + 10; // 标题底部 + 间距
        int warningLine2Y = warningY + this.fontRenderer.FONT_HEIGHT;
        int warningLine3Y = warningLine2Y + this.fontRenderer.FONT_HEIGHT;

        // 绘制提示信息
        this.drawCenteredString(this.fontRenderer, line1, this.width / 2, warningY, messageColor);
        this.drawCenteredString(this.fontRenderer, line2, this.width / 2, warningLine2Y, messageColor);
        this.drawCenteredString(this.fontRenderer, line3, this.width / 2, warningLine3Y, messageColor);

        super.drawScreen(mouseX, mouseY, partialTicks);
    }
}