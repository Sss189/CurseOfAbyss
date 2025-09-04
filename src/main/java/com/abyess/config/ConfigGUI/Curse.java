package com.abyess.config.ConfigGUI;


import com.abyess.config.ModConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.client.resources.I18n;
import net.minecraft.potion.Potion;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.input.Mouse;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

public class Curse {

    public static class CurseConfigGUI extends GuiScreen {
        private static final Map<String, List<String>> CURSE_OPTIONS = new HashMap<>();
        static {
            CURSE_OPTIONS.put("filter", Arrays.asList(
                    "inverted", "overlappingBlur", "red"
            ));
            CURSE_OPTIONS.put("other", Arrays.asList(
                    "hollow", "confusion", "bleeding", "2dentities", "custom_command"
            ));
            CURSE_OPTIONS.put("super_secret_setting", Arrays.asList(
                    "antialias", "art", "bits", "blobs", "blobs2", "blur", "bumpy",
                    "color_convolve", "creeper", "deconverge", "desaturate",
                    "flip", "fxaa", "green", "invert", "notch", "ntsc", "outline",
                    "pencil", "phosphor", "scan_pincushion", "sobel", "spider", "wobble"
            ));
        }

        private GuiScreen parentScreen;
        private ModConfig.CurseConfig curse;
        private GuiTextField durationField;
        private GuiTextField amplifierField;
        private GuiButton typeButton;
        private GuiButton effectButton;
        private GuiButton booleanValueButton;
        private GuiTextField commandField;
        private GuiButton saveButton;
        private GuiButton deleteButton;

        private boolean dropdownOpen = false;
        private List<String> dropdownOptions = new ArrayList<>();
        private int dropdownScroll = 0;
        private static final int DROPDOWN_ITEM_HEIGHT = 20;
        private static final int DROPDOWN_VISIBLE_ITEMS = 5;

        private static final String[] CURSE_TYPES = {"potion", "filter", "other", "super_secret_setting"};
        private int currentTypeIndex = 0;
        private String selectedEffect = "";

        public CurseConfigGUI(GuiScreen parent, ModConfig.CurseConfig curse) {
            this.parentScreen = parent;
            this.curse = curse;

            for (int i = 0; i < CURSE_TYPES.length; i++) {
                if (CURSE_TYPES[i].equals(curse.getType())) {
                    currentTypeIndex = i;
                    break;
                }
            }
            selectedEffect = curse.getName();
        }

        @Override
        public void initGui() {
            this.buttonList.clear();

            if (curse == null) {
                Minecraft.getMinecraft().displayGuiScreen(parentScreen);
                return;
            }

            typeButton = new GuiButton(10, width / 2 - 100, 40, 200, 20, "Type: " + CURSE_TYPES[currentTypeIndex]);
            this.buttonList.add(typeButton);

            effectButton = new GuiButton(11, width / 2 - 100, 70, 200, 20, getEffectButtonText());
            this.buttonList.add(effectButton);

            // Initialize durationField
            durationField = new GuiTextField(1, fontRenderer, width / 2 - 100, 100, 200, 20);
            durationField.setText(String.valueOf(curse.getDuration()));

            // Initialize amplifierField
            amplifierField = new GuiTextField(2, fontRenderer, width / 2 - 100, 130, 200, 20);
            amplifierField.setText(String.valueOf(curse.getAmplifier()));

            // Initialize booleanValueButton
            booleanValueButton = new GuiButton(12, width / 2 - 100, 130, 200, 20,
                    getBooleanValueButtonText());
            this.buttonList.add(booleanValueButton);

            // Initialize commandField
            commandField = new GuiTextField(3, fontRenderer, width / 2 - 100, 130, 200, 20);
            commandField.setText(curse.getCommand());

            saveButton = new GuiButton(0, width / 2 - 100, 0, 98, 20, "Save & Back");
            this.buttonList.add(saveButton);
            deleteButton = new GuiButton(1, width / 2 + 2, 0, 98, 20, "Delete Curse");
            this.buttonList.add(deleteButton);

            updateFieldVisibilityAndPositions(); // Call to set initial visibility and positions
        }

        private String getEffectButtonText() {
            if (selectedEffect.isEmpty()) {
                return "Select Effect";
            }

            String displayText = selectedEffect;

            if ("potion".equals(CURSE_TYPES[currentTypeIndex])) {
                Potion potion = Potion.getPotionFromResourceLocation(selectedEffect);
                if (potion != null) {
                    String localizedName = I18n.format(potion.getName());
                    ResourceLocation potionRL = Potion.REGISTRY.getNameForObject(potion);
                    if (potionRL != null) {
                        String namespace = potionRL.getResourceDomain();
                        String source = "Vanilla";
                        if (!"minecraft".equals(namespace)) {
                            source = "Mod: " + namespace;
                        }
                        displayText = localizedName + " (" + source + ")";
                    } else {
                        displayText = localizedName;
                    }
                }
            } else if ("custom_command".equals(selectedEffect) && "other".equals(CURSE_TYPES[currentTypeIndex])) {
                return "Custom Command";
            }

            if (displayText.length() > 20) {
                return displayText.substring(0, 17) + "...";
            }
            return displayText;
        }

        private String getBooleanValueButtonText() {
            if ("other".equals(CURSE_TYPES[currentTypeIndex]) && "hollow".equals(selectedEffect)) {
                return "Enable Bleeding Effect: " + (curse.isBooleanValue() ? "Yes" : "No");
            }
            return "Decay: " + (curse.isBooleanValue() ? "Enabled" : "Disabled");
        }

        private boolean isDurationVisible() {
            // Duration should be hidden if custom_command is selected
            return !("other".equals(CURSE_TYPES[currentTypeIndex]) && "custom_command".equals(selectedEffect));
        }

        private boolean isAmplifierVisible() {
            return "potion".equals(CURSE_TYPES[currentTypeIndex]) ||
                    ("other".equals(CURSE_TYPES[currentTypeIndex]) && "bleeding".equals(selectedEffect));
        }

        private boolean isCommandFieldVisible() {
            return "other".equals(CURSE_TYPES[currentTypeIndex]) && "custom_command".equals(selectedEffect);
        }

        private void updateFieldVisibilityAndPositions() {
            boolean showDuration = isDurationVisible();
            boolean showAmplifier = isAmplifierVisible();
            boolean showBoolean = ("filter".equals(CURSE_TYPES[currentTypeIndex]) && !"inverted".equals(selectedEffect)) ||
                    ("other".equals(CURSE_TYPES[currentTypeIndex]) && "hollow".equals(selectedEffect));
            boolean showCommand = isCommandFieldVisible();

            durationField.setVisible(showDuration);
            amplifierField.setVisible(showAmplifier);
            booleanValueButton.visible = showBoolean;
            commandField.setVisible(showCommand);

            int currentY = 100; // Starting Y for durationField
            int nextFieldY = currentY;

            if (showDuration) {
                durationField.y = currentY;
                nextFieldY += 30;
            }

            // Adjust Y positions for amplifierField, booleanValueButton, and commandField
            // They will share the same Y if they are the next visible field
            if (showAmplifier) {
                amplifierField.y = nextFieldY;
                nextFieldY += 30;
            }
            if (showBoolean) {
                booleanValueButton.y = nextFieldY;
                nextFieldY += 30;
            }
            if (showCommand) {
                commandField.y = nextFieldY;
                nextFieldY += 30;
            }

            // Adjust Save/Delete buttons based on where the last input field ended
            saveButton.y = nextFieldY;
            deleteButton.y = nextFieldY;
        }

        @Override
        protected void actionPerformed(GuiButton button) throws IOException {
            if (button.id == 10) { // Type Button
                currentTypeIndex = (currentTypeIndex + 1) % CURSE_TYPES.length;
                typeButton.displayString = "Type: " + CURSE_TYPES[currentTypeIndex];

                selectedEffect = "";
                effectButton.displayString = "Select Effect";

                dropdownOpen = false;
                updateFieldVisibilityAndPositions(); // Update all fields' visibility and positions
                booleanValueButton.displayString = getBooleanValueButtonText(); // Update text
            } else if (button.id == 11) { // Effect Button
                dropdownOpen = !dropdownOpen;

                if (dropdownOpen) {
                    if ("potion".equals(CURSE_TYPES[currentTypeIndex])) {
                        dropdownOptions = Potion.REGISTRY.getKeys().stream()
                                .map(ResourceLocation::toString)
                                .sorted()
                                .collect(Collectors.toList());
                    } else {
                        dropdownOptions = new ArrayList<>(CURSE_OPTIONS.getOrDefault(CURSE_TYPES[currentTypeIndex], Collections.emptyList()));
                        Collections.sort(dropdownOptions);
                    }
                    dropdownScroll = 0;
                }

            } else if (button.id == 12) { // Boolean Value Button
                curse.setBooleanValue(!curse.isBooleanValue());
                booleanValueButton.displayString = getBooleanValueButtonText();

            } else if (button.id == 0) { // Save & Back Button
                saveChanges();
                Minecraft.getMinecraft().displayGuiScreen(parentScreen);
            } else if (button.id == 1) { // Delete Curse Button
                if (parentScreen instanceof Layer.CurseListGUI) {
                    Layer.CurseListGUI curseListGUI = (Layer.CurseListGUI) parentScreen;
                    if (curseListGUI.getLayer().getCurses().remove(curse)) {
                        ModConfig.saveConfig();
                        Minecraft.getMinecraft().displayGuiScreen(new Layer.CurseListGUI(curseListGUI.parentScreen, curseListGUI.getLayer()));
                    }
                }
            }
        }

        @Override
        protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
            if (dropdownOpen) {
                int dropdownTop = effectButton.y + effectButton.height;
                int dropdownHeight = Math.min(DROPDOWN_VISIBLE_ITEMS, dropdownOptions.size()) * DROPDOWN_ITEM_HEIGHT;

                if (mouseX >= effectButton.x && mouseX <= effectButton.x + effectButton.width &&
                        mouseY >= dropdownTop && mouseY <= dropdownTop + dropdownHeight) {

                    int clickedIndex = (mouseY - dropdownTop) / DROPDOWN_ITEM_HEIGHT + dropdownScroll;
                    if (clickedIndex < dropdownOptions.size()) {
                        selectedEffect = dropdownOptions.get(clickedIndex);
                        effectButton.displayString = getEffectButtonText();

                        updateFieldVisibilityAndPositions(); // Update all fields' visibility and positions
                        booleanValueButton.displayString = getBooleanValueButtonText();

                        dropdownOpen = false;
                    }
                    return;
                } else {
                    dropdownOpen = false;
                }
            }

            super.mouseClicked(mouseX, mouseY, mouseButton);

            if (isDurationVisible()) { // Only process if visible
                durationField.mouseClicked(mouseX, mouseY, mouseButton);
            }
            if (isAmplifierVisible()) { // Only process if visible
                amplifierField.mouseClicked(mouseX, mouseY, mouseButton);
            }
            if (isCommandFieldVisible()) { // Only process if visible
                commandField.mouseClicked(mouseX, mouseY, mouseButton);
            }
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

        public void saveChanges() {
            curse.setType(CURSE_TYPES[currentTypeIndex]);
            curse.setName(selectedEffect);

            // Only save duration if it's visible
            if (isDurationVisible()) {
                try {
                    curse.setDuration(Integer.parseInt(durationField.getText()));
                } catch (NumberFormatException e) {
                    // Keep original value if parsing fails
                }
            } else {
                curse.setDuration(0); // Set to 0 if not visible to avoid stale data
            }


            if (isAmplifierVisible()) {
                try {
                    curse.setAmplifier(Integer.parseInt(amplifierField.getText()));
                } catch (NumberFormatException e) {
                    // Keep original value if parsing fails
                }
            } else {
                curse.setAmplifier(0);
            }

            // Save command field value
            if (isCommandFieldVisible()) {
                curse.setCommand(commandField.getText());
            } else {
                curse.setCommand("");
            }

            ModConfig.saveConfig();
        }

        @Override
        public void drawScreen(int mouseX, int mouseY, float partialTicks) {
            this.drawDefaultBackground();
            this.drawCenteredString(this.fontRenderer, "Curse Configuration", this.width / 2, 20, 0xFFFFFF);

            // Draw Duration label and field only if visible
            if (isDurationVisible()) {
                this.drawString(fontRenderer, "Duration(s):", width / 2 - 163, durationField.y + 6, 0xAAAAAA);
                durationField.drawTextBox();
            }


            // Adjust label for amplifierField or commandField based on their visibility
            if (isAmplifierVisible()) {
                this.drawString(fontRenderer, "Amplifier:", width / 2 - 154, amplifierField.y + 6, 0xAAAAAA);
                amplifierField.drawTextBox();
            } else if (isCommandFieldVisible()) {
                this.drawString(fontRenderer, "Command:", width / 2 - 154, commandField.y + 6, 0xAAAAAA);
                commandField.drawTextBox();
            }

            super.drawScreen(mouseX, mouseY, partialTicks);

            if (dropdownOpen && !dropdownOptions.isEmpty()) {
                int dropdownTop = effectButton.y + effectButton.height;
                int dropdownHeight = Math.min(DROPDOWN_VISIBLE_ITEMS, dropdownOptions.size()) * DROPDOWN_ITEM_HEIGHT;

                drawRect(effectButton.x, dropdownTop,
                        effectButton.x + effectButton.width,
                        dropdownTop + dropdownHeight,
                        0xFF000000);

                drawRect(effectButton.x, dropdownTop,
                        effectButton.x + effectButton.width,
                        dropdownTop + dropdownHeight,
                        0xAA222222);

                int maxItems = Math.min(dropdownOptions.size() - dropdownScroll, DROPDOWN_VISIBLE_ITEMS);
                for (int i = 0; i < maxItems; i++) {
                    int index = i + dropdownScroll;
                    String option = dropdownOptions.get(index);
                    int yPos = dropdownTop + i * DROPDOWN_ITEM_HEIGHT;

                    if (option.equals(selectedEffect)) {
                        drawRect(effectButton.x, yPos,
                                effectButton.x + effectButton.width,
                                yPos + DROPDOWN_ITEM_HEIGHT,
                                0xAA555555);
                    }

                    String displayText = option;
                    if ("potion".equals(CURSE_TYPES[currentTypeIndex])) {
                        Potion potion = Potion.getPotionFromResourceLocation(option);
                        if (potion != null) {
                            displayText = I18n.format(potion.getName());
                            ResourceLocation potionRL = Potion.REGISTRY.getNameForObject(potion);
                            if (potionRL != null) {
                                String namespace = potionRL.getResourceDomain();
                                String source = "Vanilla";
                                if (!"minecraft".equals(namespace)) {
                                    source = "Mod: " + namespace;
                                }
                                displayText += " (" + source + ")";
                            }
                        }
                    } else if ("custom_command".equals(option) && "other".equals(CURSE_TYPES[currentTypeIndex])) {
                        displayText = "Custom Command";
                    }

                    if (displayText.length() > 30) {
                        displayText = displayText.substring(0, 27) + "...";
                    }
                    fontRenderer.drawString(displayText,
                            effectButton.x + 5,
                            yPos + (DROPDOWN_ITEM_HEIGHT - 8) / 2,
                            0xFFFFFF);
                }

                if (dropdownOptions.size() > DROPDOWN_VISIBLE_ITEMS) {
                    int scrollbarHeight = (int) ((float) DROPDOWN_VISIBLE_ITEMS / dropdownOptions.size() * dropdownHeight);
                    int scrollbarTop = dropdownTop + (int) ((float) dropdownScroll / (dropdownOptions.size() - DROPDOWN_VISIBLE_ITEMS) * dropdownHeight);

                    drawRect(effectButton.x + effectButton.width - 5, scrollbarTop,
                            effectButton.x + effectButton.width,
                            scrollbarTop + scrollbarHeight,
                            0xFFFFFFFF);
                }
            }
        }

        @Override
        protected void keyTyped(char typedChar, int keyCode) throws IOException {
            if (dropdownOpen && keyCode == 1) { // 1 is ESC key code
                dropdownOpen = false;
                return;
            }

            super.keyTyped(typedChar, keyCode);

            if (isDurationVisible()) {
                durationField.textboxKeyTyped(typedChar, keyCode);
            }
            if (isAmplifierVisible()) {
                amplifierField.textboxKeyTyped(typedChar, keyCode);
            }
            if (isCommandFieldVisible()) {
                commandField.textboxKeyTyped(typedChar, keyCode);
            }
        }
    }
}
