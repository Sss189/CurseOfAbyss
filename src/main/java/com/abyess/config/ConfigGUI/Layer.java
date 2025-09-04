package com.abyess.config.ConfigGUI;

import com.abyess.config.ModConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiTextField;

import java.io.IOException;
import java.util.Arrays;

public class Layer {

    public static class LayerConfigGUI extends GuiScreen {
        private GuiScreen parentScreen;
        private ModConfig.LayerConfig layer;
        private GuiTextField nameField;
        private GuiTextField minYField;
        private GuiTextField maxYField;
        private GuiTextField thresholdField;
        private GuiTextField continuousIntervalField;
        private GuiTextField durabilityCostField;

        private GuiTextField colorField;

        // Replaced direct fog inputs with a button to open a new GUI
        private GuiButton fogSettingsButton; // <--- NEW BUTTON

        private GuiButton enableButton;
        private GuiButton hudButton;
        private GuiButton showTimeDistanceButton;
        private GuiButton manageCursesButton;
        private GuiButton saveAndBackButton;

        public LayerConfigGUI(GuiScreen parent, ModConfig.LayerConfig layer) {
            this.parentScreen = parent;
            this.layer = layer;
        }

        public ModConfig.LayerConfig getLayer() {
            return layer;
        }

        @Override
        public void initGui() {
            this.buttonList.clear();

            int fieldHalfWidth = 95;
            int startY = 40;
            int rowHeight = 30;

            // Text input fields (left side)
            nameField = new GuiTextField(0, fontRenderer, width / 2 - 100, startY, fieldHalfWidth, 20);
            nameField.setText(layer.getName());

            minYField = new GuiTextField(1, fontRenderer, width / 2 - 100, startY + rowHeight, fieldHalfWidth, 20);
            minYField.setText(String.valueOf(layer.getRange()[0]));

            maxYField = new GuiTextField(2, fontRenderer, width / 2 + 5, startY + rowHeight, fieldHalfWidth, 20);
            maxYField.setText(String.valueOf(layer.getRange()[1]));

            thresholdField = new GuiTextField(3, fontRenderer, width / 2 - 100, startY + 2 * rowHeight, fieldHalfWidth, 20);
            thresholdField.setText(String.valueOf(layer.getThreshold()));

            continuousIntervalField = new GuiTextField(4, fontRenderer, width / 2 - 100, startY + 3 * rowHeight, fieldHalfWidth, 20);
            continuousIntervalField.setText(String.valueOf(layer.getContinuousTriggerInterval() == 0 ? 10 : layer.getContinuousTriggerInterval()));

            durabilityCostField = new GuiTextField(5, fontRenderer, width / 2 - 100, startY + 4 * rowHeight, fieldHalfWidth, 20);
            durabilityCostField.setText(String.valueOf(layer.getDurabilityCost()));

            // Right side input fields and NEW Fog Settings Button
            colorField = new GuiTextField(6, fontRenderer, width / 2 + 5, startY, fieldHalfWidth, 20);
            colorField.setText(layer.getColor());

            // NEW Fog Settings button instead of direct inputs
            fogSettingsButton = new GuiButton(20, width / 2 + 5, startY + 2 * rowHeight, fieldHalfWidth, 20, "Fog Settings"); // <--- NEW BUTTON

            // Bottom buttons
            int buttonWidth = 100;
            int buttonHorizontalGap = 10;
            int totalButtonsWidth = (buttonWidth * 2) + buttonHorizontalGap;
            int bottomButtonY = this.height - 30;
            int topButtonY = bottomButtonY - (buttonWidth / 5) - 8; // Adjust for spacing

            // HUD and Enabled buttons
            enableButton = new GuiButton(0, width / 2 - (totalButtonsWidth / 2), topButtonY, buttonWidth, 20, "Enabled: " + (layer.isEnabled() ? "Yes" : "No"));
            hudButton = new GuiButton(1, width / 2 + (totalButtonsWidth / 2) - buttonWidth, topButtonY, buttonWidth, 20, "HUD: " + (layer.isHudEnabled() ? "Yes" : "No"));

            // Show Time Distance button (position adjusted in updateElementVisibilityAndPositions)
            showTimeDistanceButton = new GuiButton(2, hudButton.x + hudButton.width + 5, topButtonY, buttonWidth, 20, "Show Threshold: " + (layer.isShowTimeDistance() ? "Yes" : "No"));


            // Manage Curses button and Save & Back button
            manageCursesButton = new GuiButton(10, width / 2 - (totalButtonsWidth / 2), bottomButtonY, buttonWidth, 20, "Manage Curses");
            saveAndBackButton = new GuiButton(11, width / 2 + (totalButtonsWidth / 2) - buttonWidth, bottomButtonY, buttonWidth, 20, "Save & Back");


            this.buttonList.add(enableButton);
            this.buttonList.add(hudButton);
            this.buttonList.add(showTimeDistanceButton);
            this.buttonList.add(manageCursesButton);
            this.buttonList.add(saveAndBackButton);
            this.buttonList.add(fogSettingsButton); // Add the new fog settings button

            updateElementVisibilityAndPositions(); // Ensure all elements position and visibility are correct
        }

        private void updateElementVisibilityAndPositions() {
            if (thresholdField == null || continuousIntervalField == null || durabilityCostField == null) {
                return;
            }

            try {
                int threshold = Integer.parseInt(thresholdField.getText());
                continuousIntervalField.setVisible(threshold == 0);
                continuousIntervalField.setFocused(threshold == 0 && continuousIntervalField.isFocused());
            } catch (NumberFormatException e) {
                continuousIntervalField.setVisible(false);
                continuousIntervalField.setFocused(false);
            }

            // Recalculate bottom button positions
            int buttonWidth = 100;
            int buttonHorizontalGap = 10;
            int totalButtonsWidth = (buttonWidth * 2) + buttonHorizontalGap;
            int bottomButtonY = this.height - 30;
            int topButtonY = bottomButtonY - (buttonWidth / 5) - 8; // Enabled / HUD row


            manageCursesButton.width = buttonWidth;
            manageCursesButton.x = width / 2 - (totalButtonsWidth / 2);
            manageCursesButton.y = bottomButtonY;

            saveAndBackButton.width = buttonWidth;
            saveAndBackButton.x = width / 2 + (totalButtonsWidth / 2) - buttonWidth;
            saveAndBackButton.y = bottomButtonY;

            enableButton.width = buttonWidth;
            enableButton.x = width / 2 - (totalButtonsWidth / 2);
            enableButton.y = topButtonY;
            enableButton.displayString = "Enabled: " + (layer.isEnabled() ? "Yes" : "No");

            hudButton.width = buttonWidth;
            hudButton.x = width / 2 + (totalButtonsWidth / 2) - buttonWidth;
            hudButton.y = topButtonY;
            hudButton.displayString = "HUD: " + (layer.isHudEnabled() ? "Yes" : "No");

            // Show Time Distance button position and display text
            showTimeDistanceButton.width = buttonWidth;
            showTimeDistanceButton.x = hudButton.x + hudButton.width + 5;
            showTimeDistanceButton.y = topButtonY;
            showTimeDistanceButton.displayString = "Show Threshold: " + (layer.isShowTimeDistance() ? "Yes" : "No");

            // Control visibility of showTimeDistanceButton based on hudEnabled
            showTimeDistanceButton.visible = layer.isHudEnabled();
        }


        @Override
        protected void actionPerformed(GuiButton button) throws IOException {
            if (button.id == 0) { // Enabled button
                layer.setEnabled(!layer.isEnabled());
                enableButton.displayString = "Enabled: " + (layer.isEnabled() ? "Yes" : "No");
            } else if (button.id == 1) { // HUD button
                layer.setHudEnabled(!layer.isHudEnabled());
                hudButton.displayString = "HUD: " + (layer.isHudEnabled() ? "Yes" : "No");
                updateElementVisibilityAndPositions(); // HUD state change might affect showTimeDistance button visibility
            } else if (button.id == 2) { // Show Time Distance button
                layer.setShowTimeDistance(!layer.isShowTimeDistance());
                showTimeDistanceButton.displayString = "Show Threshold: " + (layer.isShowTimeDistance() ? "Yes" : "No");
            } else if (button.id == 10) { // Manage Curses button
                saveChanges();
                Minecraft.getMinecraft().displayGuiScreen(new CurseListGUI(this, layer));
            } else if (button.id == 11) { // Save & Back button
                saveChanges();
                Minecraft.getMinecraft().displayGuiScreen(parentScreen);
            } else if (button.id == 20) { // Fog Settings button <--- ACTION FOR NEW BUTTON
                saveChanges(); // Save current layer changes before opening new GUI
                Minecraft.getMinecraft().displayGuiScreen(new FogConfigGUI(this, layer)); // Open new Fog Config GUI
            }
        }

        public void saveChanges() {
            layer.setName(nameField.getText());
            layer.setColor(colorField.getText());

            try {
                int minY = Integer.parseInt(minYField.getText());
                int maxY = Integer.parseInt(maxYField.getText());
                layer.setRange(new int[]{minY, maxY});
            } catch (NumberFormatException e) {
                System.err.println("Invalid Y range input. Keeping previous value.");
            }
            try {
                int threshold = Integer.parseInt(thresholdField.getText());
                layer.setThreshold(threshold);

                if (threshold == 0) {
                    try {
                        layer.setContinuousTriggerInterval(Integer.parseInt(continuousIntervalField.getText()));
                    } catch (NumberFormatException e) {
                        layer.setContinuousTriggerInterval(0); // Default or error value
                        System.err.println("Invalid Continuous Interval input. Setting to 0.");
                    }
                } else {
                    layer.setContinuousTriggerInterval(0); // Reset if not continuous
                }

            } catch (NumberFormatException e) {
                layer.setThreshold(0); // Default or error value
                layer.setContinuousTriggerInterval(0); // Reset
                System.err.println("Invalid Threshold input. Setting to 0.");
            }

            try {
                layer.setDurabilityCost(Integer.parseInt(durabilityCostField.getText()));
            } catch (NumberFormatException e) {
                layer.setDurabilityCost(0); // Default or error value
                System.err.println("Invalid Durability Cost input. Setting to 0.");
            }

            // No direct fog saving here, handled by FogConfigGUI
            ModConfig.saveConfig();
        }

        @Override
        public void drawScreen(int mouseX, int mouseY, float partialTicks) {
            this.drawDefaultBackground();
            this.drawCenteredString(this.fontRenderer, "Layer Configuration: " + layer.getName(), this.width / 2, 20, 0xFFFFFF);

            int labelXOffsetLeft = -154; // Adjust label X offset
            int labelYAlign = 6; // Label Y alignment with text box

            // Left side text labels
            this.drawString(fontRenderer, "Name:", width / 2 + labelXOffsetLeft, nameField.y + labelYAlign, 0xAAAAAA);
            this.drawString(fontRenderer, "Y Range:", width / 2 + labelXOffsetLeft, minYField.y + labelYAlign, 0xAAAAAA);
            this.drawString(fontRenderer, "Threshold:", width / 2 + labelXOffsetLeft - 3, thresholdField.y + labelYAlign, 0xAAAAAA);
            if (continuousIntervalField.getVisible()) {
                this.drawString(fontRenderer, "Interval(s):", width / 2 + labelXOffsetLeft - 6, continuousIntervalField.y + labelYAlign, 0xAAAAAA);
            }
            this.drawString(fontRenderer, "Durability Cost:", width / 2 + labelXOffsetLeft - 25, durabilityCostField.y + labelYAlign, 0xAAAAAA);

            // Right side text label
            this.drawString(fontRenderer, "HUD Color (Hex):", colorField.x + colorField.width + 5, colorField.y + labelYAlign, 0xAAAAAA);

            // Draw text fields
            nameField.drawTextBox();
            minYField.drawTextBox();
            maxYField.drawTextBox();
            thresholdField.drawTextBox();
            continuousIntervalField.drawTextBox();
            durabilityCostField.drawTextBox();

            colorField.drawTextBox();

            super.drawScreen(mouseX, mouseY, partialTicks);
        }

        @Override
        protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
            super.mouseClicked(mouseX, mouseY, mouseButton);
            nameField.mouseClicked(mouseX, mouseY, mouseButton);
            minYField.mouseClicked(mouseX, mouseY, mouseButton);
            maxYField.mouseClicked(mouseX, mouseY, mouseButton);
            thresholdField.mouseClicked(mouseX, mouseY, mouseButton);
            continuousIntervalField.mouseClicked(mouseX, mouseY, mouseButton);
            durabilityCostField.mouseClicked(mouseX, mouseY, mouseButton);

            colorField.mouseClicked(mouseX, mouseY, mouseButton);
        }

        @Override
        protected void keyTyped(char typedChar, int keyCode) throws IOException {
            super.keyTyped(typedChar, keyCode);
            nameField.textboxKeyTyped(typedChar, keyCode);
            minYField.textboxKeyTyped(typedChar, keyCode);
            maxYField.textboxKeyTyped(typedChar, keyCode);

            if (thresholdField.textboxKeyTyped(typedChar, keyCode)) {
                updateElementVisibilityAndPositions();
            }
            continuousIntervalField.textboxKeyTyped(typedChar, keyCode);
            durabilityCostField.textboxKeyTyped(typedChar, keyCode);

            colorField.textboxKeyTyped(typedChar, keyCode);
        }
    }

    // --- NEW: FogConfigGUI Class ---
// --- NEW: FogConfigGUI Class ---
    public static class FogConfigGUI extends GuiScreen {
        private GuiScreen parentScreen;
        private ModConfig.LayerConfig layer;

        private GuiButton fogEnabledButton;
        private GuiButton fogTypeButton;
        private GuiTextField fogColorField;
        private GuiTextField fogParam1Field; // For Density (EXP2) or Start (LINEAR)
        private GuiTextField fogParam2Field; // For End (LINEAR)

        private GuiButton saveAndBackButton;

        public FogConfigGUI(GuiScreen parent, ModConfig.LayerConfig layer) {
            this.parentScreen = parent;
            this.layer = layer;
        }

        @Override
        public void initGui() {
            this.buttonList.clear();

            int startY = 40;
            int rowHeight = 30;
            int fieldWidth = 190;
            int buttonWidth = 95;
            int buttonGap = 5;

            // Row 1: Fog On/Off
            fogEnabledButton = new GuiButton(0, width / 2 - buttonWidth / 2, startY, buttonWidth, 20, "Fog: " + (layer.isFogEnabled() ? "On" : "Off"));
            this.buttonList.add(fogEnabledButton);

            // Row 2: Fog Type (Linear/Exponential)
            fogTypeButton = new GuiButton(1, width / 2 - buttonWidth / 2, startY + rowHeight, buttonWidth, 20, "Type: " + layer.getFogType());
            this.buttonList.add(fogTypeButton);

            // Row 3: Fog Color
            String currentFogColor = String.format("%.3f %.3f %.3f", layer.getFogColorRGB()[0], layer.getFogColorRGB()[1], layer.getFogColorRGB()[2]);
            fogColorField = new GuiTextField(2, fontRenderer, width / 2 - fieldWidth / 2, startY + 2 * rowHeight + 15, fieldWidth, 20); // Adjusted Y for label
            fogColorField.setText(currentFogColor);
            // No direct add to buttonList for GuiTextField, handled by drawScreen/mouseClicked/keyTyped

            // Row 4: Fog Parameters (Density or Start/End) - 这一行向下移动8像素
            int paramRowY = startY + 3 * rowHeight + 15 + 8; // 在原来的基础上再加8像素

            if ("LINEAR".equals(layer.getFogType())) {
                int halfFieldWidth = (fieldWidth - buttonGap) / 2;
                fogParam1Field = new GuiTextField(3, fontRenderer, width / 2 - fieldWidth / 2, paramRowY, halfFieldWidth, 20); // 使用 paramRowY
                fogParam1Field.setText(String.format("%.3f", layer.getFogStart()));

                fogParam2Field = new GuiTextField(4, fontRenderer, width / 2 - fieldWidth / 2 + halfFieldWidth + buttonGap, paramRowY, halfFieldWidth, 20); // 使用 paramRowY
                fogParam2Field.setText(String.format("%.3f", layer.getFogEnd()));
            } else { // EXP2
                fogParam1Field = new GuiTextField(3, fontRenderer, width / 2 - fieldWidth / 2, paramRowY, fieldWidth, 20); // 使用 paramRowY
                fogParam1Field.setText(String.format("%.3f", layer.getFogDensity()));
                fogParam2Field = null; // No second field for EXP2
            }

            // Save & Back button
            saveAndBackButton = new GuiButton(100, width / 2 - 75, height - 30, 150, 20, "Save & Back");
            this.buttonList.add(saveAndBackButton);

            updateElementVisibility();
        }

        private void updateElementVisibility() {
            boolean isFogEnabled = layer.isFogEnabled();
            fogTypeButton.visible = isFogEnabled;
            fogColorField.setVisible(isFogEnabled);
            fogParam1Field.setVisible(isFogEnabled);
            if (fogParam2Field != null) {
                fogParam2Field.setVisible(isFogEnabled && "LINEAR".equals(layer.getFogType()));
            }
        }

        @Override
        protected void actionPerformed(GuiButton button) throws IOException {
            if (button.id == 0) { // Fog Enabled/Off
                layer.setFogEnabled(!layer.isFogEnabled());
                fogEnabledButton.displayString = "Fog: " + (layer.isFogEnabled() ? "On" : "Off");
                updateElementVisibility();
                initGui(); // Re-initialize to correctly display/hide parameters
            } else if (button.id == 1) { // Fog Type
                if ("EXP2".equals(layer.getFogType())) {
                    layer.setFogType("LINEAR");
                } else {
                    layer.setFogType("EXP2");
                }
                fogTypeButton.displayString = "Type: " + layer.getFogType();
                initGui(); // Re-initialize to switch between density/distance fields
            } else if (button.id == 100) { // Save & Back
                saveChanges();
                Minecraft.getMinecraft().displayGuiScreen(parentScreen);
            }
        }

        public void saveChanges() {
            layer.setFogEnabled(fogEnabledButton.displayString.endsWith("On"));

            if (layer.isFogEnabled()) {
                // Save Fog Type
                layer.setFogType(fogTypeButton.displayString.replace("Type: ", ""));

                // Save Fog Color
                try {
                    String[] colorParts = fogColorField.getText().split(" ");
                    if (colorParts.length == 3) {
                        float r = Float.parseFloat(colorParts[0]);
                        float g = Float.parseFloat(colorParts[1]);
                        float b = Float.parseFloat(colorParts[2]);
                        layer.setFogColorRGB(new float[]{r, g, b});
                    } else {
                        System.err.println("Invalid fog color format. Expected 'R G B' floats.");
                    }
                } catch (NumberFormatException e) {
                    System.err.println("Failed to parse fog color floats. Error: " + e.getMessage());
                }

                // Save Fog Parameters based on Type
                if ("LINEAR".equals(layer.getFogType())) {
                    try {
                        layer.setFogStart(Float.parseFloat(fogParam1Field.getText()));
                    } catch (NumberFormatException e) {
                        System.err.println("Failed to parse fog start distance. Error: " + e.getMessage());
                    }
                    try {
                        layer.setFogEnd(Float.parseFloat(fogParam2Field.getText()));
                    } catch (NumberFormatException e) {
                        System.err.println("Failed to parse fog end distance. Error: " + e.getMessage());
                    }
                } else { // EXP2
                    try {
                        layer.setFogDensity(Float.parseFloat(fogParam1Field.getText()));
                    } catch (NumberFormatException e) {
                        System.err.println("Failed to parse fog density. Error: " + e.getMessage());
                    }
                }
            }
            ModConfig.saveConfig();
        }

        @Override
        public void drawScreen(int mouseX, int mouseY, float partialTicks) {
            this.drawDefaultBackground();
            this.drawCenteredString(this.fontRenderer, "Fog Settings for " + layer.getName(), this.width / 2, 20, 0xFFFFFF);

            int labelX = width / 2 - 95;
            if (layer.isFogEnabled()) {
                // 雾颜色提示保持不变
                this.drawString(fontRenderer, "Fog Color (R G B, 0.0-1.0):", labelX, fogColorField.y - 12, 0xAAAAAA);
                fogColorField.drawTextBox();

                if ("LINEAR".equals(layer.getFogType())) {
                    // 线性雾的起始和结束现在是百分比
                    this.drawString(fontRenderer, "Start (Percentage):", labelX, fogParam1Field.y - 12, 0xAAAAAA); // 更新为百分比提示
                    this.drawString(fontRenderer, "End (Percentage):", fogParam2Field.x, fogParam2Field.y - 12, 0xAAAAAA); // 更新为百分比提示
                    fogParam1Field.drawTextBox();
                    if (fogParam2Field != null) {
                        fogParam2Field.drawTextBox();
                    }
                } else { // EXP2
                    // 指数雾密度提示保持不变
                    this.drawString(fontRenderer, "Density (0.0-1.0):", labelX, fogParam1Field.y - 12, 0xAAAAAA);
                    fogParam1Field.drawTextBox();
                }
            }

            super.drawScreen(mouseX, mouseY, partialTicks);
        }

        @Override
        protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
            super.mouseClicked(mouseX, mouseY, mouseButton);
            if (layer.isFogEnabled()) {
                fogColorField.mouseClicked(mouseX, mouseY, mouseButton);
                fogParam1Field.mouseClicked(mouseX, mouseY, mouseButton);
                if (fogParam2Field != null) {
                    fogParam2Field.mouseClicked(mouseX, mouseY, mouseButton);
                }
            }
        }

        @Override
        protected void keyTyped(char typedChar, int keyCode) throws IOException {
            super.keyTyped(typedChar, keyCode);
            if (layer.isFogEnabled()) {
                fogColorField.textboxKeyTyped(typedChar, keyCode);
                fogParam1Field.textboxKeyTyped(typedChar, keyCode);
                if (fogParam2Field != null) {
                    fogParam2Field.textboxKeyTyped(typedChar, keyCode);
                }
            }
        }
    }

    // --- CurseListGUI (remains unchanged) ---
    public static class CurseListGUI extends GuiScreen {
        public GuiScreen parentScreen;
        private ModConfig.LayerConfig layer;

        public CurseListGUI(GuiScreen parent, ModConfig.LayerConfig layer) {
            this.parentScreen = parent;
            this.layer = layer;
        }

        public ModConfig.LayerConfig getLayer() {
            return layer;
        }

        @Override
        public void initGui() {
            this.buttonList.clear();

            int yPos = 40;
            for (int i = 0; i < layer.getCurses().size(); i++) {
                ModConfig.CurseConfig curse = layer.getCurses().get(i);
                this.buttonList.add(new GuiButton(10 + i, width / 2 - 100, yPos, 150, 20, curse.getName()));
                this.buttonList.add(new GuiButton(30 + i, width / 2 + 55, yPos, 45, 20, "Delete"));
                yPos += 24;
            }

            this.buttonList.add(new GuiButton(0, width / 2 - 100, yPos, 98, 20, "Add Curse"));
            this.buttonList.add(new GuiButton(1, width / 2 + 2, yPos, 98, 20, "Back"));
        }

        @Override
        protected void actionPerformed(GuiButton button) throws IOException {
            if (button.id == 0) {
                ModConfig.CurseConfig newCurse = new ModConfig.CurseConfig(
                        "potion",
                        "minecraft:regeneration",
                        5,
                        0,
                        false
                );
                layer.getCurses().add(newCurse);
                ModConfig.saveConfig();
                initGui();
            } else if (button.id == 1) {
                Minecraft.getMinecraft().displayGuiScreen(parentScreen);
            } else if (button.id >= 10 && button.id < 30) {
                int curseIndex = button.id - 10;
                if (curseIndex < layer.getCurses().size()) {
                    ModConfig.CurseConfig curse = layer.getCurses().get(curseIndex);
                    Minecraft.getMinecraft().displayGuiScreen(new Curse.CurseConfigGUI(this, curse));
                }
            } else if (button.id >= 30) {
                int curseIndex = button.id - 30;
                if (curseIndex < layer.getCurses().size()) {
                    layer.getCurses().remove(curseIndex);
                    ModConfig.saveConfig();
                    initGui();
                }
            }
        }

        @Override
        public void drawScreen(int mouseX, int mouseY, float partialTicks) {
            this.drawDefaultBackground();
            this.drawCenteredString(this.fontRenderer, "Curse List for " + layer.getName(), this.width / 2, 20, 0xFFFFFF);
            super.drawScreen(mouseX, mouseY, partialTicks);
        }
    }
}