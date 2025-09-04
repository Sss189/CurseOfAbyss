package com.abyess.config;


import com.abyess.config.ConfigGUI.MainMenuGUI;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraftforge.fml.client.IModGuiFactory;

import java.util.Set;

public class ModGuiFactory implements IModGuiFactory {

    @Override
    public void initialize(Minecraft minecraftInstance) {
        // 初始化代码（通常为空）
    }

    @Override
    public boolean hasConfigGui() {
        return true; // 表示本 Mod 有配置界面
    }

    @Override
    public GuiScreen createConfigGui(GuiScreen parentScreen) {
        // 返回配置主菜单
        return new MainMenuGUI(parentScreen);
    }

    @Override
    public Set<RuntimeOptionCategoryElement> runtimeGuiCategories() {
        return null; // 不需要返回任何内容
    }
}