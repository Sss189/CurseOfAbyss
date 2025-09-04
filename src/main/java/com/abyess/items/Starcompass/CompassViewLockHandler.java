package com.abyess.items.Starcompass;

import com.abyess.config.ModConfig; // Import ModConfig
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraftforge.client.event.InputUpdateEvent;
import net.minecraftforge.client.event.MouseEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public class CompassViewLockHandler {

    // 配置参数
    private static final float LERP_SPEED = 15.0f;
    private static final double MIN_HORIZONTAL_DISTANCE_SQ = 0.01;
    private static final double UNLOCK_DISTANCE_SQ = 1.0 * 1.0;

    // 状态控制
    private static float savedMouseSensitivity = -1.0f;
    private static boolean isViewLocked = false;

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

        Minecraft mc = Minecraft.getMinecraft();
        EntityPlayerSP player = mc.player;
        if (player == null) return;

        boolean shouldLock = false;
        ItemStack compass = getHeldCompass(player);

        if (!compass.isEmpty()) {
            BlockPos target = null;
            int targetDim = Integer.MIN_VALUE;
            boolean isCustom = false;

            if (compass.getItem() == ItemStarCompass.INSTANCE) {
                target = ItemStarCompass.getTargetPosition(compass);
                targetDim = ItemStarCompass.getTargetDimension(compass);
            } else if (ItemStarCompass.isCustomCompass(compass)) {
                target = ItemStarCompass.getCustomCompassTarget(compass);
                targetDim = ItemStarCompass.getCustomCompassDimension(compass);
                isCustom = true;
            }

            if (target != null && targetDim != Integer.MIN_VALUE) {
                if (player.world.provider.getDimension() == targetDim) {
                    double dx = (target.getX() + 0.5) - player.posX;
                    double dz = (target.getZ() + 0.5) - player.posZ;
                    double distanceSq = dx * dx + dz * dz;

                    shouldLock = distanceSq >= UNLOCK_DISTANCE_SQ;

                    if (shouldLock) {
                        // 在这里计算 dy，然后传递给 updatePlayerView
                        double dy = (target.getY() + 0.5) - (player.posY + player.getEyeHeight()); // 正确的眼睛高度差

                        // NEW: Conditionally apply 3D lock based on config and compass type
                        if (isCustom && ModConfig.getConfigData().isCustomCompass3DLock()) {
                            // Apply 3D lock for custom compasses if enabled
                            updatePlayerView(player, target, dx, dy, dz, true); // 传递修正后的 dy
                        } else if (!isCustom) {
                            // Apply original horizontal lock for regular compasses
                            updatePlayerView(player, target, dx, dy, dz, false); // 传递修正后的 dy
                        }
                    }
                }
            }
        }
        isViewLocked = shouldLock;

        // 灵敏度控制
        if (isViewLocked) {
            if (savedMouseSensitivity < 0) {
                savedMouseSensitivity = mc.gameSettings.mouseSensitivity;
                mc.gameSettings.mouseSensitivity = 0.0F;
            }
        } else {
            if (savedMouseSensitivity >= 0) {
                mc.gameSettings.mouseSensitivity = savedMouseSensitivity;
                savedMouseSensitivity = -1;
            }
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onMouseInput(MouseEvent event) {
        if (!isViewLocked) return;

        // 仅阻止鼠标移动
        if (event.getDx() != 0 || event.getDy() != 0) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onInputUpdate(InputUpdateEvent event) {
        // 检查 "noWayBack" 配置
        if (!ModConfig.getConfigData().isNoWayBack()) { // 使用正确的 getter
            return; // If "noWayBack" is disabled, do nothing
        }

        if (!isViewLocked) return;

        // 阻止后退移动
        if (event.getMovementInput().moveForward < 0) {
            event.getMovementInput().moveForward = 0;
        }
    }

    // Fully restored original view update logic
    private static void updatePlayerView(EntityPlayerSP player, BlockPos target, double dx, double dy, double dz, boolean lock3D) { // 接收 dy 参数
        if ((dx * dx + dz * dz) < MIN_HORIZONTAL_DISTANCE_SQ) return;

        float targetYaw = (float) Math.toDegrees(Math.atan2(dz, dx)) - 90.0f;
        targetYaw = MathHelper.wrapDegrees(targetYaw);

        float deltaTime = 0.05f;
        player.rotationYaw = lerpAngleSmooth(player.rotationYaw, targetYaw, LERP_SPEED, deltaTime);

        if (lock3D) {
            // Calculate pitch for 3D lock
            // 关键修改：反转 dy 的符号，或者在 atan2 内部反转，取决于哪个效果更好
            // 尝试直接反转 dy 的符号
            float targetPitch = (float) Math.toDegrees(Math.atan2(-dy, Math.sqrt(dx * dx + dz * dz))); // <-- 尝试在这里对 dy 取反
            targetPitch = MathHelper.wrapDegrees(targetPitch);
            player.rotationPitch = lerpAngleSmooth(player.rotationPitch, targetPitch, LERP_SPEED, deltaTime);
        } else {
            // Pitch fixed at 0 for original logic (horizontal lock)
            player.rotationPitch = lerpAngleSmooth(player.rotationPitch, 0.0f, LERP_SPEED, deltaTime);
        }
        player.rotationYawHead = player.rotationYaw;
    }

    private static float lerpAngleSmooth(float current, float target, float speed, float deltaTime) {
        float delta = MathHelper.wrapDegrees(target - current);
        float factor = 1.0f - (float) Math.exp(-speed * deltaTime);
        return current + delta * MathHelper.clamp(factor, 0.0f, 1.0f);
    }

    // 获取手持罗盘（支持原版和自定义）
    private static ItemStack getHeldCompass(EntityPlayerSP player) {
        ItemStack mainHand = player.getHeldItem(EnumHand.MAIN_HAND);
        ItemStack offHand = player.getHeldItem(EnumHand.OFF_HAND);

        // 检查主手是否为罗盘
        if (!mainHand.isEmpty()) {
            if (mainHand.getItem() == ItemStarCompass.INSTANCE ||
                    ItemStarCompass.isCustomCompass(mainHand)) {
                return mainHand;
            }
        }

        // 检查副手是否为罗盘
        if (!offHand.isEmpty()) {
            if (offHand.getItem() == ItemStarCompass.INSTANCE ||
                    ItemStarCompass.isCustomCompass(offHand)) {
                return offHand;
            }
        }

        return ItemStack.EMPTY;
    }
}