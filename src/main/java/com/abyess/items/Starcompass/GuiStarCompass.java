package com.abyess.items.Starcompass;

import com.abyess.Network.NetworkHandler;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.world.World;
import org.lwjgl.input.Keyboard;

import java.io.IOException;

// 导入 DeathRespawnNotifier 中的 MOD_DATA_TAG 常量
import static com.abyess.items.Starcompass.DeathRespawnNotifier.MOD_DATA_TAG;

public class GuiStarCompass extends GuiScreen {
    private GuiTextField xInput;
    private GuiTextField yInput;
    private GuiTextField zInput;
    private GuiTextField dimInput;

    private GuiButton currentPositionButton;
    private GuiButton deathPointButton;
    private GuiButton respawnPointButton;
    private GuiButton confirmButton;
    private GuiButton escapeButton;
    private int inputY;
    private final EntityPlayer player;
    private final World world;
    private final EnumHand hand;

    private final BlockPos initialTarget;
    private final int initialDim;
    private boolean showDimensionWarning = false;

    public GuiStarCompass(EntityPlayer player, World world, EnumHand hand,
                          BlockPos initialTarget, int initialDim) {
        this.player = player;
        this.world = world;
        this.hand = hand;
        this.initialTarget = initialTarget;
        this.initialDim = initialDim;

        // 仅在维度已设置且与玩家维度不匹配时显示警告
        this.showDimensionWarning = (initialDim != Integer.MIN_VALUE) &&
                (initialDim != player.dimension);
    }

    @Override
    public void initGui() {
        this.buttonList.clear();

        int guiWidth = 300;
        int guiHeight = 180;
        int centerX = (this.width - guiWidth) / 2;
        int centerY = (this.height - guiHeight) / 2;

        int inputWidth = 50;
        int inputSpacing = 25;

        // 额外的右移量，从 6 改为 11 (原先 6 + 额外 5)
        int offsetX = 11;

        int totalInputWidth = 4 * inputWidth + 3 * inputSpacing;

        // DimID 相关的元素需要额外右移 offsetX
        int startX = (this.width - totalInputWidth) / 2 + 5;
        this.inputY = centerY + 30;

        xInput = new GuiTextField(0, this.fontRenderer, startX, inputY, inputWidth, 20);
        yInput = new GuiTextField(1, this.fontRenderer, startX + inputWidth + inputSpacing, inputY, inputWidth, 20);
        zInput = new GuiTextField(2, this.fontRenderer, startX + (inputWidth + inputSpacing) * 2, inputY, inputWidth, 20);
        // 修正 DimID 输入框的 X 坐标，加上额外的偏移量
        dimInput = new GuiTextField(3, this.fontRenderer, startX + (inputWidth + inputSpacing) * 3 - 3 + offsetX, inputY, inputWidth, 20);


        // 设置文本输入框属性
        xInput.setEnableBackgroundDrawing(true);
        xInput.setMaxStringLength(32);
        xInput.setFocused(true);
        xInput.setText("");

        yInput.setEnableBackgroundDrawing(true);
        yInput.setMaxStringLength(32);
        yInput.setFocused(false);
        yInput.setText("");

        zInput.setEnableBackgroundDrawing(true);
        zInput.setMaxStringLength(32);
        zInput.setFocused(false);
        zInput.setText("");

        dimInput.setEnableBackgroundDrawing(true);
        dimInput.setMaxStringLength(32);
        dimInput.setFocused(false);
        dimInput.setText("");

        // 设置初始值
        if (initialTarget != null && !initialTarget.equals(BlockPos.ORIGIN)) {
            xInput.setText(String.valueOf(initialTarget.getX()));
            yInput.setText(String.valueOf(initialTarget.getY()));
            zInput.setText(String.valueOf(initialTarget.getZ()));
        }

        dimInput.setText(initialDim != Integer.MIN_VALUE ?
                String.valueOf(initialDim) : "");

        int buttonWidth = (int)(260 * 0.7);
        int buttonX = (this.width - buttonWidth) / 2;
        int buttonY = inputY + 30;

        currentPositionButton = new GuiButton(4, buttonX, buttonY, buttonWidth, 20, "Set to Current Position");
        this.buttonList.add(currentPositionButton);

        deathPointButton = new GuiButton(1, buttonX, buttonY + 25, buttonWidth, 20, "Set to Last Death Point");
        this.buttonList.add(deathPointButton);

        respawnPointButton = new GuiButton(2, buttonX, buttonY + 50, buttonWidth, 20, "Set to Last Respawn Point");
        this.buttonList.add(respawnPointButton);

        confirmButton = new GuiButton(3, buttonX, buttonY + 75, buttonWidth, 20, "Confirm");
        this.buttonList.add(confirmButton);

        escapeButton = new GuiButton(5, buttonX, buttonY + 100, buttonWidth, 20, "ESC (Close)");
        this.buttonList.add(escapeButton);
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        this.drawDefaultBackground();
        this.drawCenteredString(this.fontRenderer, "Star Compass GUI", this.width / 2, 15, 0xFFFFFF);

        int labelY = inputY + 5;

        this.drawString(fontRenderer, "X:", xInput.x - fontRenderer.getStringWidth("X:") - 2, labelY, 0xFFFFFF);
        this.drawString(fontRenderer, "Y:", yInput.x - fontRenderer.getStringWidth("Y:") - 2, labelY, 0xFFFFFF);
        this.drawString(fontRenderer, "Z:", zInput.x - fontRenderer.getStringWidth("Z:") - 2, labelY, 0xFFFFFF);
        // 修正 DimID 标签的 X 坐标，它应该与 dimInput 的 X 坐标相关联
        this.drawString(fontRenderer, "DimID:", dimInput.x - fontRenderer.getStringWidth("DimID:") - 2, labelY, 0xFFFFFF);

        xInput.drawTextBox();
        yInput.drawTextBox();
        zInput.drawTextBox();
        dimInput.drawTextBox();

        // 将警告消息放到 DimID 输入框上方
        if (showDimensionWarning) {
            String warningText = "§cNot in this dimension!";
            int warningWidth = fontRenderer.getStringWidth(warningText);
            // 警告消息的 X 坐标与 dimInput 居中对齐
            int warningX = dimInput.x + (dimInput.width - warningWidth) / 2;
            // 警告消息的 Y 坐标在 inputY (输入框顶部) 上方，留出一些间距
            int warningY = inputY - fontRenderer.FONT_HEIGHT - 5; // 字体高度 + 5像素间距
            this.drawString(fontRenderer, warningText, warningX, warningY, 0xFFFFFF);
        }

        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    @Override
    protected void actionPerformed(GuiButton button) throws IOException {
        if (button == currentPositionButton) {
            BlockPos pos = player.getPosition();
            xInput.setText(String.valueOf(pos.getX()));
            yInput.setText(String.valueOf(pos.getY()));
            zInput.setText(String.valueOf(pos.getZ()));

            dimInput.setText(String.valueOf(player.dimension));
            showDimensionWarning = false;
        }
        else if (button == deathPointButton) {
            NBTTagCompound playerPersistentData = player.getEntityData().getCompoundTag(EntityPlayer.PERSISTED_NBT_TAG);
            NBTTagCompound modData = playerPersistentData.getCompoundTag(MOD_DATA_TAG);

            if (modData.hasKey("DeathData")) {
                NBTTagCompound deathData = modData.getCompoundTag("DeathData");
                double deathX = deathData.getDouble("deathX");
                double deathY = deathData.getDouble("deathY");
                double deathZ = deathData.getDouble("deathZ");
                int deathDim = deathData.getInteger("deathDim");

                xInput.setText(String.valueOf(Math.round(deathX)));
                yInput.setText(String.valueOf(Math.round(deathY)));
                zInput.setText(String.valueOf(Math.round(deathZ)));

                dimInput.setText(String.valueOf(deathDim));

                showDimensionWarning = (deathDim != player.dimension);
            } else {
                player.sendMessage(new TextComponentString("§cNo death point recorded"));
            }
        }
        else if (button == respawnPointButton) {
            NBTTagCompound playerPersistentData = player.getEntityData().getCompoundTag(EntityPlayer.PERSISTED_NBT_TAG);
            NBTTagCompound modData = playerPersistentData.getCompoundTag(MOD_DATA_TAG);

            if (modData.hasKey("RespawnData")) {
                NBTTagCompound respawnData = modData.getCompoundTag("RespawnData");
                double respawnX = respawnData.getDouble("respawnX");
                double respawnY = respawnData.getDouble("respawnY");
                double respawnZ = respawnData.getDouble("respawnZ");
                int respawnDim = respawnData.getInteger("respawnDim");

                xInput.setText(String.valueOf(Math.round(respawnX)));
                yInput.setText(String.valueOf(Math.round(respawnY)));
                zInput.setText(String.valueOf(Math.round(respawnZ)));

                dimInput.setText(String.valueOf(respawnDim));

                showDimensionWarning = (respawnDim != player.dimension);
            } else {
                player.sendMessage(new TextComponentString("§cNo respawn point recorded"));
            }
        }
        else if (button == confirmButton) {
            try {
                int x = Integer.parseInt(xInput.getText().trim());
                int y = Integer.parseInt(yInput.getText().trim());
                int z = Integer.parseInt(zInput.getText().trim());
                BlockPos target = new BlockPos(x, y, z);

                int dim = Integer.MIN_VALUE;
                String dimText = dimInput.getText().trim();
                if (!dimText.isEmpty()) {
                    dim = Integer.parseInt(dimText);
                }

                showDimensionWarning = (dim != Integer.MIN_VALUE) &&
                        (dim != player.dimension);

                if (world.isRemote) {
                    NetworkHandler.sendCompassUpdate(player, hand, target, dim);
                }

                player.sendMessage(new TextComponentString("§aTarget position set successfully!"));
                if (showDimensionWarning) {
                    player.sendMessage(new TextComponentString("§cWarning: Not in target dimension!"));
                }

                this.mc.displayGuiScreen(null);

            } catch (NumberFormatException e) {
                player.sendMessage(new TextComponentString("§cInvalid input! Please enter integers."));
            }
        }
        else if (button == escapeButton) {
            this.mc.displayGuiScreen(null);
        }
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        super.keyTyped(typedChar, keyCode);

        if (xInput.isFocused()) {
            xInput.textboxKeyTyped(typedChar, keyCode);
        } else if (yInput.isFocused()) {
            yInput.textboxKeyTyped(typedChar, keyCode);
        } else if (zInput.isFocused()) {
            zInput.textboxKeyTyped(typedChar, keyCode);
        } else if (dimInput.isFocused()) {
            dimInput.textboxKeyTyped(typedChar, keyCode);

            try {
                String dimText = dimInput.getText().trim();
                if (!dimText.isEmpty()) {
                    int dim = Integer.parseInt(dimText);
                    showDimensionWarning = (dim != player.dimension);
                } else {
                    showDimensionWarning = false;
                }
            } catch (NumberFormatException e) {
                showDimensionWarning = false;
            }
        }

        if (keyCode == Keyboard.KEY_ESCAPE) {
            this.mc.displayGuiScreen(null);
        } else if (keyCode == Keyboard.KEY_TAB) {
            if (xInput.isFocused()) {
                yInput.setFocused(true);
                xInput.setFocused(false);
            } else if (yInput.isFocused()) {
                zInput.setFocused(true);
                yInput.setFocused(false);
            } else if (zInput.isFocused()) {
                dimInput.setFocused(true);
                zInput.setFocused(false);
            } else if (dimInput.isFocused()) {
                xInput.setFocused(true);
                dimInput.setFocused(false);
            }
        } else if (keyCode == Keyboard.KEY_RETURN || keyCode == Keyboard.KEY_NUMPADENTER) {
            try {
                actionPerformed(confirmButton);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        super.mouseClicked(mouseX, mouseY, mouseButton);
        xInput.mouseClicked(mouseX, mouseY, mouseButton);
        yInput.mouseClicked(mouseX, mouseY, mouseButton);
        zInput.mouseClicked(mouseX, mouseY, mouseButton);
        dimInput.mouseClicked(mouseX, mouseY, mouseButton);
    }

    @Override
    public void updateScreen() {
        super.updateScreen();
        xInput.updateCursorCounter();
        yInput.updateCursorCounter();
        zInput.updateCursorCounter();
        dimInput.updateCursorCounter();
    }
}