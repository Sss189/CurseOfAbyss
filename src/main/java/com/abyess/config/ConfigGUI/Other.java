package com.abyess.config.ConfigGUI;

import com.abyess.config.ModConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiTextField;
import org.lwjgl.input.Keyboard;

import java.io.IOException;

public class Other {

    public static class OtherConfigGUI extends GuiScreen {
        private GuiScreen parentScreen;

        // Buttons for the new options
        private GuiButton noWayBackButton;
        private GuiButton customCompass3DLockButton;
        private GuiButton disableInventoryInHollowButton;
        private GuiButton enable2DEntityDistanceScalingButton; // NEW: 2D Entity Distance Scaling toggle button

        // GuiTextField for Hollow Jump Power
        private GuiTextField hollowJumpPowerField;
        private GuiButton saveJumpPowerButton; // Button to apply the jump power from the text field

        // Variables for displaying jump power validation hints
        private String jumpPowerHint = "";
        private long hintDisplayTime = 0;
        private static final long HINT_DURATION_MS = 3000; // Hint display duration 3 seconds

        public OtherConfigGUI(GuiScreen parent) {
            this.parentScreen = parent;
        }

        @Override
        public void initGui() {
            this.buttonList.clear(); // Clear existing buttons
            Keyboard.enableRepeatEvents(true); // Enable repeating events for text field input

            int startY = this.height / 4 + 24; // Starting Y position for buttons
            int buttonWidth = 200;
            int buttonHeight = 20;
            int spacing = 24; // Space between buttons

            // "No Way Back" Button (ID 0)
            this.noWayBackButton = new GuiButton(0, this.width / 2 - 100, startY, buttonWidth, buttonHeight, "");
            this.buttonList.add(this.noWayBackButton);

            // "3D Compass Lock" Button (ID 1)
            this.customCompass3DLockButton = new GuiButton(1, this.width / 2 - 100, startY + spacing, buttonWidth, buttonHeight, "");
            this.buttonList.add(this.customCompass3DLockButton);

            // "Disable Inventory in Hollow" Button (ID 4)
            this.disableInventoryInHollowButton = new GuiButton(4, this.width / 2 - 100, startY + (spacing * 2), buttonWidth, buttonHeight, "");
            this.buttonList.add(this.disableInventoryInHollowButton);

            // "2D Entity Distance Scaling" Button (ID 7)
            this.enable2DEntityDistanceScalingButton = new GuiButton(7, this.width / 2 - 100, startY + (spacing * 3), buttonWidth, buttonHeight, "");
            this.buttonList.add(this.enable2DEntityDistanceScalingButton);

            // Hollow Jump Power Text Field and Save Button
            // Calculate positions for text, field, and button on one line
            String jumpPowerLabel = "Hollow Jump Power:";
            int labelWidth = this.fontRenderer.getStringWidth(jumpPowerLabel);
            int totalControlWidth = labelWidth + 5 + 60 + 5 + 60; // Label + gap + fieldWidth + gap + saveButtonWidth (adjust as needed)

            int baseX = this.width / 2 - totalControlWidth / 2; // Center the whole line of controls
            int fieldY = startY + (spacing * 4); // Adjusted Y position due to new button above

            // Hollow Jump Power Text Field (ID 5)
            int fieldX = baseX + labelWidth + 5; // Position after label + gap
            int fieldWidth = 60; // Smaller width for just the number
            this.hollowJumpPowerField = new GuiTextField(5, this.fontRenderer, fieldX, fieldY, fieldWidth, buttonHeight);
            this.hollowJumpPowerField.setMaxStringLength(5); // e.g., "1.5" or "0.8"
            this.hollowJumpPowerField.setEnableBackgroundDrawing(true);
            this.hollowJumpPowerField.setVisible(true);
            this.hollowJumpPowerField.setTextColor(0xFFFFFF); // White text

            // Save Jump Power Button (Apply button) (ID 6)
            int saveButtonWidth = 60; // Make this button shorter
            int saveButtonX = fieldX + fieldWidth + 5; // Position after field + gap
            this.saveJumpPowerButton = new GuiButton(6, saveButtonX, fieldY, saveButtonWidth, buttonHeight, "Apply");
            this.buttonList.add(this.saveJumpPowerButton);

            // Back Button (ID 2) - Adjusted Y position to be consistently near the bottom
            this.buttonList.add(new GuiButton(2, this.width / 2 - 100, this.height - 28, buttonWidth, buttonHeight, "Back"));

            updateButtonText(); // Set initial button text based on config
        }

        @Override
        public void onGuiClosed() {
            super.onGuiClosed();
            Keyboard.enableRepeatEvents(false); // Disable repeating events when GUI is closed
        }

        // Helper method to update button text and text field based on current config state
        private void updateButtonText() {
            ModConfig.ConfigData config = ModConfig.getConfigData(); // Get current config data

            boolean noWayBack = config.isNoWayBack();
            this.noWayBackButton.displayString = "No Way Back: " + (noWayBack ? "ON" : "OFF");

            boolean custom3DLock = config.isCustomCompass3DLock();
            this.customCompass3DLockButton.displayString = "Custom Compass 3D Lock: " + (custom3DLock ? "ON" : "OFF");

            boolean disableInventory = config.isDisableInventoryInHollow();
            this.disableInventoryInHollowButton.displayString = "Disable Inventory in Hollow: " + (disableInventory ? "ON" : "OFF");

            boolean enable2DScaling = config.isEnable2DEntityDistanceScaling();
            this.enable2DEntityDistanceScalingButton.displayString = "2D Entity Distance Scaling: " + (enable2DScaling ? "ON" : "OFF");

            // Set text field to current jump power
            this.hollowJumpPowerField.setText(String.format("%.1f", config.getHollowJumpPower()));
        }

        @Override
        protected void actionPerformed(GuiButton button) throws IOException {
            ModConfig.ConfigData config = ModConfig.getConfigData();

            if (button.id == 0) { // No Way Back button
                config.setNoWayBack(!config.isNoWayBack()); // Toggle the value
            } else if (button.id == 1) { // 3D Compass Lock button
                config.setCustomCompass3DLock(!config.isCustomCompass3DLock()); // Toggle the value
            } else if (button.id == 4) { // Disable Inventory in Hollow button
                config.setDisableInventoryInHollow(!config.isDisableInventoryInHollow()); // Toggle the value
            } else if (button.id == 7) { // 2D Entity Distance Scaling button
                config.setEnable2DEntityDistanceScaling(!config.isEnable2DEntityDistanceScaling()); // Toggle the value
            } else if (button.id == 6) { // Save Jump Power Button (Apply button)
                try {
                    float newJumpPower = Float.parseFloat(this.hollowJumpPowerField.getText());
                    // Validate jump power value, e.g., restrict between 0.0F and 5.0F
                    if (newJumpPower >= 0.0F && newJumpPower <= 5.0F) {
                        config.setHollowJumpPower(newJumpPower);
                        jumpPowerHint = "Jump Power Saved!";
                    } else {
                        jumpPowerHint = "Invalid Jump Power! Value must be between 0.0 and 5.0.";
                    }
                } catch (NumberFormatException e) {
                    jumpPowerHint = "Please enter a valid number!";
                }
                hintDisplayTime = System.currentTimeMillis(); // Record when the hint started displaying
            } else if (button.id == 2) { // Back button
                ModConfig.saveConfig(); // Save configuration when going back
                Minecraft.getMinecraft().displayGuiScreen(this.parentScreen);
                return;
            }

            ModConfig.saveConfig(); // Save config immediately after toggling or applying
            updateButtonText(); // Update button and text field text to reflect new state
        }

        @Override
        protected void keyTyped(char typedChar, int keyCode) throws IOException {
            // Allow GuiTextField to handle key input if it's focused
            if (this.hollowJumpPowerField.isFocused()) {
                this.hollowJumpPowerField.textboxKeyTyped(typedChar, keyCode);
                // Clear previous hint upon new input
                jumpPowerHint = "";
            }

            // Close GUI on escape key, but only if text field is not focused
            if (keyCode == Keyboard.KEY_ESCAPE && !this.hollowJumpPowerField.isFocused()) {
                ModConfig.saveConfig(); // Save config on exit
                Minecraft.getMinecraft().displayGuiScreen(this.parentScreen);
            }
        }

        @Override
        protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
            super.mouseClicked(mouseX, mouseY, mouseButton); // Handle button clicks

            // Allow GuiTextField to handle mouse clicks for focus
            this.hollowJumpPowerField.mouseClicked(mouseX, mouseY, mouseButton);
        }

        @Override
        public void drawScreen(int mouseX, int mouseY, float partialTicks) {
            this.drawDefaultBackground();
            this.drawCenteredString(this.fontRenderer, "Other Configurations", this.width / 2, 20, 0xFFFFFF); // English title

            // Draw the text field label on the same line as the field
            String jumpPowerLabel = "Hollow Jump Power:"; // English label
            int labelWidth = this.fontRenderer.getStringWidth(jumpPowerLabel);
            int labelX = this.hollowJumpPowerField.x - labelWidth - 5; // Position label to the left of the field
            this.drawString(this.fontRenderer, jumpPowerLabel, labelX, this.hollowJumpPowerField.y + (this.hollowJumpPowerField.height - this.fontRenderer.FONT_HEIGHT) / 2, 0xFFFFFF); // Vertically center with field

            // Draw the text field itself
            this.hollowJumpPowerField.drawTextBox();

            // Draw jump power validation hint
            if (!jumpPowerHint.isEmpty() && System.currentTimeMillis() - hintDisplayTime < HINT_DURATION_MS) {
                this.drawCenteredString(this.fontRenderer, jumpPowerHint, this.width / 2,
                        this.hollowJumpPowerField.y + this.hollowJumpPowerField.height + 5,
                        0xFFFF00); // Yellow hint text
            }

            super.drawScreen(mouseX, mouseY, partialTicks);
        }
    }
}
