package com.abyess.shaders;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.shader.Shader;
import net.minecraft.client.shader.ShaderGroup;
import net.minecraft.util.ResourceLocation;
// import net.minecraft.util.text.TextComponentString; // 移除 TextComponentString 导入，因为它不再使用

import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.relauncher.ReflectionHelper;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CustomShaderLoader {
    public static final CustomShaderLoader INSTANCE = new CustomShaderLoader();

    // 着色器名称到资源位置的映射（核心着色器程序）
    private static final Map<String, ResourceLocation> SHADER_PROGRAM_MAP = new HashMap<>();
    static {
        // 基础着色器程序
        SHADER_PROGRAM_MAP.put("blit", new ResourceLocation("curseofabyss", "shaders/program/blit"));
        SHADER_PROGRAM_MAP.put("antialias", new ResourceLocation("curseofabyss", "shaders/program/antialias"));
        SHADER_PROGRAM_MAP.put("color_convolve", new ResourceLocation("curseofabyss", "shaders/program/color_convolve"));
        SHADER_PROGRAM_MAP.put("notch", new ResourceLocation("curseofabyss", "shaders/program/notch"));
        SHADER_PROGRAM_MAP.put("phosphor", new ResourceLocation("curseofabyss", "shaders/program/phosphor"));
        SHADER_PROGRAM_MAP.put("rotscale", new ResourceLocation("curseofabyss", "shaders/program/rotscale"));
        SHADER_PROGRAM_MAP.put("deconverge", new ResourceLocation("curseofabyss", "shaders/program/deconverge"));
        SHADER_PROGRAM_MAP.put("ntsc_decode", new ResourceLocation("curseofabyss", "shaders/program/ntsc_decode"));
        SHADER_PROGRAM_MAP.put("scan_pincushion", new ResourceLocation("curseofabyss", "shaders/program/scan_pincushion"));
        SHADER_PROGRAM_MAP.put("downscale", new ResourceLocation("curseofabyss", "shaders/program/downscale"));
        SHADER_PROGRAM_MAP.put("ntsc_encode", new ResourceLocation("curseofabyss", "shaders/program/ntsc_encode"));
        SHADER_PROGRAM_MAP.put("sobel", new ResourceLocation("curseofabyss", "shaders/program/sobel"));
        SHADER_PROGRAM_MAP.put("outline", new ResourceLocation("curseofabyss", "shaders/program/outline"));
        SHADER_PROGRAM_MAP.put("entity_outline", new ResourceLocation("curseofabyss", "shaders/program/entity_outline"));
        SHADER_PROGRAM_MAP.put("spider", new ResourceLocation("curseofabyss", "shaders/program/spider"));
        SHADER_PROGRAM_MAP.put("entity_sobel", new ResourceLocation("curseofabyss", "shaders/program/entity_sobel"));
        SHADER_PROGRAM_MAP.put("outline_combine", new ResourceLocation("curseofabyss", "shaders/program/outline_combine"));
        SHADER_PROGRAM_MAP.put("spiderclip", new ResourceLocation("curseofabyss", "shaders/program/spiderclip"));
        SHADER_PROGRAM_MAP.put("flip", new ResourceLocation("curseofabyss", "shaders/program/flip"));
        SHADER_PROGRAM_MAP.put("wobble", new ResourceLocation("curseofabyss", "shaders/program/wobble"));
        SHADER_PROGRAM_MAP.put("outline_soft", new ResourceLocation("curseofabyss", "shaders/program/outline_soft"));
        SHADER_PROGRAM_MAP.put("fxaa", new ResourceLocation("curseofabyss", "shaders/program/fxaa"));
        SHADER_PROGRAM_MAP.put("outline_watercolor", new ResourceLocation("curseofabyss", "shaders/program/outline_watercolor"));
        SHADER_PROGRAM_MAP.put("invert", new ResourceLocation("curseofabyss", "shaders/program/invert"));
        SHADER_PROGRAM_MAP.put("overlay", new ResourceLocation("curseofabyss", "shaders/program/overlay"));

        System.out.println("[ShaderLoader] Loaded " + SHADER_PROGRAM_MAP.size() + " shader programs");
    }

    // 着色器组注册表 (名称 -> JSON资源路径)
    private final Map<String, ResourceLocation> registeredShaders = new HashMap<>();
    // 当前激活的着色器组
    private ShaderGroup activeShaderGroup;
    private String currentShaderName;
    private int shaderTimer = 0;
    private boolean needsReload = true;
    private boolean shaderSupported = false;

    // private int debugMessageTimer = 0; // 移除用于调试消息的计时器

    private CustomShaderLoader() {
        registerAllShaders();
    }

    // 注册所有着色器
    private void registerAllShaders() {
        registerShader("pencil", new ResourceLocation("curseofabyss", "shaders/post/pencil.json"));
        registerShader("antialias", new ResourceLocation("curseofabyss", "shaders/post/antialias.json"));
        registerShader("blobs", new ResourceLocation("curseofabyss", "shaders/post/blobs.json"));
        registerShader("blobs2", new ResourceLocation("curseofabyss", "shaders/post/blobs2.json"));
        registerShader("blur", new ResourceLocation("curseofabyss", "shaders/post/blur.json"));
        registerShader("bumpy", new ResourceLocation("curseofabyss", "shaders/post/bumpy.json"));
        registerShader("color_convolve", new ResourceLocation("curseofabyss", "shaders/post/color_convolve.json"));
        registerShader("creeper", new ResourceLocation("curseofabyss", "shaders/post/creeper.json"));
        registerShader("deconverge", new ResourceLocation("curseofabyss", "shaders/post/deconverge.json"));
        registerShader("desaturate", new ResourceLocation("curseofabyss", "shaders/post/desaturate.json"));
        registerShader("entity_outline", new ResourceLocation("curseofabyss", "shaders/post/entity_outline.json"));
        registerShader("flip", new ResourceLocation("curseofabyss", "shaders/post/flip.json"));
        registerShader("fxaa", new ResourceLocation("curseofabyss", "shaders/post/fxaa.json"));
        registerShader("green", new ResourceLocation("curseofabyss", "shaders/post/green.json"));
        registerShader("invert", new ResourceLocation("curseofabyss", "shaders/post/invert.json"));
        registerShader("ntsc", new ResourceLocation("curseofabyss", "shaders/post/ntsc.json"));
        registerShader("outline", new ResourceLocation("curseofabyss", "shaders/post/outline.json"));
        registerShader("phosphor", new ResourceLocation("curseofabyss", "shaders/post/phosphor.json"));
        registerShader("scan_pincushion", new ResourceLocation("curseofabyss", "shaders/post/scan_pincushion.json"));
        registerShader("sobel", new ResourceLocation("curseofabyss", "shaders/post/sobel.json"));
        registerShader("spider", new ResourceLocation("curseofabyss", "shaders/post/spider.json"));
        registerShader("wobble", new ResourceLocation("curseofabyss", "shaders/post/wobble.json"));
        registerShader("art", new ResourceLocation("curseofabyss", "shaders/post/art.json"));
        registerShader("bits", new ResourceLocation("curseofabyss", "shaders/post/bits.json"));
        registerShader("notch", new ResourceLocation("curseofabyss", "shaders/post/notch.json"));
    }

    // 注册新着色器
    public void registerShader(String name, ResourceLocation location) {
        registeredShaders.put(name, location);
        System.out.println("[ShaderLoader] Registered shader: " + name);
    }

    public void init() {
        Minecraft mc = Minecraft.getMinecraft();
        shaderSupported = OpenGlHelper.shadersSupported;

        if (shaderSupported) {
            MinecraftForge.EVENT_BUS.register(this);
            System.out.println("[ShaderLoader] Initialized with shader support. Total shaders: " + registeredShaders.size());
        } else {
            System.out.println("[ShaderLoader] Shaders not supported on this system");
        }
    }

    // 切换着色器
    public boolean setActiveShader(String shaderName) {
        if (!registeredShaders.containsKey(shaderName)) {
            System.err.println("[ShaderLoader] Unknown shader: " + shaderName);
            return false;
        }

        // 只有当着色器名称发生变化时，才设置 needsReload 为 true
        // 否则，如果已经是当前着色器且无需重载，直接返回 true
        if (shaderName.equals(currentShaderName)) {
            if (!needsReload) { // 如果已经激活且无需重载，直接返回
                return true;
            }
        } else {
            needsReload = true; // 着色器名称不同，需要重载
        }

        currentShaderName = shaderName;
        // needsReload = true; // 仅在名称不同时设置
        System.out.println("[ShaderLoader] Setting active shader: " + shaderName);
        return true;
    }

    private void reloadShader() {
        if (!shaderSupported || currentShaderName == null) return;

        ResourceLocation shaderLocation = registeredShaders.get(currentShaderName);
        if (shaderLocation == null) return;

        Minecraft mc = Minecraft.getMinecraft();
        try {
            // 卸载旧着色器
            if (activeShaderGroup != null) {
                activeShaderGroup.deleteShaderGroup();
                activeShaderGroup = null;
            }

            // 加载新着色器
            activeShaderGroup = new ShaderGroup(
                    mc.getTextureManager(),
                    mc.getResourceManager(),
                    mc.getFramebuffer(),
                    shaderLocation
            );

            // 修复着色器路径
            fixShaderPaths();

            // 初始帧缓冲
            updateFramebufferSize();

            needsReload = false;
            System.out.println("[ShaderLoader] Shader loaded: " + currentShaderName);
        } catch (Exception e) {
            System.err.println("[ShaderLoader] Failed to load shader: " + shaderLocation);
            e.printStackTrace();
            needsReload = true;
        }
    }

    // 修复着色器路径
    private void fixShaderPaths() {
        if (activeShaderGroup == null) return;

        try {
            Field listShadersField = ReflectionHelper.findField(ShaderGroup.class, "listShaders", "field_148031_d");
            @SuppressWarnings("unchecked")
            List<Shader> shaders = (List<Shader>) listShadersField.get(activeShaderGroup);

            for (Shader shader : shaders) {
                Field shaderNameField = ReflectionHelper.findField(Shader.class, "shaderName", "field_148000_a");
                String programName = (String) shaderNameField.get(shader);

                // 如果映射中没有这个程序，尝试动态添加
                if (!SHADER_PROGRAM_MAP.containsKey(programName)) {
                    ResourceLocation location = new ResourceLocation("curseofabyss", "shaders/program/" + programName);
                    SHADER_PROGRAM_MAP.put(programName, location);
                    System.out.println("[ShaderLoader] Dynamically registered program: " + programName);
                }

                if (SHADER_PROGRAM_MAP.containsKey(programName)) {
                    Field programField = ReflectionHelper.findField(Shader.class, "program", "field_148001_b");
                    programField.set(shader, SHADER_PROGRAM_MAP.get(programName));
                } else {
                    System.err.println("[ShaderLoader] Program not found: " + programName);
                }
            }
        } catch (Exception e) {
            System.err.println("[ShaderLoader] Failed to fix shader paths");
            e.printStackTrace();
        }
    }

    private void updateFramebufferSize() {
        if (activeShaderGroup != null) {
            Minecraft mc = Minecraft.getMinecraft();
            activeShaderGroup.createBindFramebuffers(mc.displayWidth, mc.displayHeight);
        }
    }

    /**
     * onRenderTick 事件：负责在每一帧的渲染循环中调用着色器进行渲染。
     * 不负责计时器更新，只根据 shaderTimer 的状态判断是否渲染。
     */
    @SubscribeEvent
    @SideOnly(Side.CLIENT)
    public void onRenderTick(TickEvent.RenderTickEvent event) {
        // 只在渲染阶段结束时执行，且着色器支持
        if (!shaderSupported || event.phase != TickEvent.Phase.END) return;

        Minecraft mc = Minecraft.getMinecraft();

        // 只有当着色器激活（计时器 > 0）、着色器组已加载且玩家存在时才渲染
        if (shaderTimer <= 0 || activeShaderGroup == null ||
                mc.world == null || mc.player == null) {
            // 如果着色器应该不活跃但 activeShaderGroup 仍然存在，则清理它以避免资源泄露
            if (activeShaderGroup != null) {
                activeShaderGroup.deleteShaderGroup();
                activeShaderGroup = null;
                currentShaderName = null;
                System.out.println("[ShaderLoader] Shader group cleaned up due to timer expiry.");
            }
            return;
        }

        try {
            activeShaderGroup.render(mc.getRenderPartialTicks()); // 仅仅调用渲染方法
        } catch (Exception e) {
            System.err.println("[ShaderLoader] Error rendering shader: " + currentShaderName);
            e.printStackTrace();
            // 渲染出错时，强制停止着色器，以免持续报错
            shaderTimer = 0;
            // debugMessageTimer = 0; // 移除此行
            if (activeShaderGroup != null) {
                activeShaderGroup.deleteShaderGroup();
                activeShaderGroup = null;
                currentShaderName = null;
            }
        }
    }

    /**
     * onClientTick 事件：负责着色器计时器的更新。
     * 此事件与游戏逻辑刻同步 (20 ticks/second)。
     */
    @SubscribeEvent
    @SideOnly(Side.CLIENT)
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return; // 只在Tick结束时处理

        Minecraft mc = Minecraft.getMinecraft();

        // 只有当客户端有世界和玩家时才进行计时
        if (mc.world == null || mc.player == null) {
            // 如果玩家离开世界，重置所有计时器和着色器状态
            if (shaderTimer != 0 || activeShaderGroup != null) { // 移除 debugMessageTimer 检查
                shaderTimer = 0;
                // debugMessageTimer = 0; // 移除此行
                if (activeShaderGroup != null) {
                    activeShaderGroup.deleteShaderGroup();
                    activeShaderGroup = null;
                    currentShaderName = null;
                }
                needsReload = true; // 确保下次进入世界时重新加载
            }
            return;
        }

        // 重试加载机制 (通常在客户端有玩家时才会加载)
        if (needsReload && mc.getRenderViewEntity() != null) {
            reloadShader();
        }

        // 只有当着色器激活（计时器 > 0）且已加载时才更新计时
        if (shaderTimer <= 0 || activeShaderGroup == null) {
            // 如果计时器已结束，不需要额外处理 debugMessageTimer
            return;
        }

        try {
            shaderTimer--; // 着色器计时器每游戏刻减一

            // 移除调试信息逻辑块
            /*
            debugMessageTimer++;
            if (debugMessageTimer >= 20) { // 每20刻（1秒）发送一次消息
                int remainingSeconds = (int) Math.ceil(shaderTimer / 20.0); // 计算剩余秒数
                if (mc.player != null) {
                    mc.player.sendMessage(new TextComponentString("§b[诅咒之渊着色器] §f着色器 §e" + currentShaderName + " §f剩余时间: §a" + remainingSeconds + " §f秒。"));
                }
                debugMessageTimer = 0; // 重置调试计时器
            }
            */

        } catch (Exception e) {
            System.err.println("[ShaderLoader] Error updating shader timer: " + currentShaderName); // 修改日志信息
            e.printStackTrace();
            shaderTimer = 0; // 发生错误时，停止计时
            // debugMessageTimer = 0; // 移除此行
            // 如果 ClientTick 发生错误，也清理着色器组
            if (activeShaderGroup != null) {
                activeShaderGroup.deleteShaderGroup();
                activeShaderGroup = null;
                currentShaderName = null;
            }
        }
    }


    // 外部控制API
    public void activateShader(String shaderName, int duration) {
        if (setActiveShader(shaderName)) {
         //   shaderTimer = Math.max(shaderTimer, duration);
            shaderTimer = duration;
       //     System.out.println("[ShaderLoader] Activated shader: " + shaderName + " for " + duration + " ticks");
            // 移除激活时立即发送一次调试信息，并重置计时器
            /*
            Minecraft mc = Minecraft.getMinecraft();
            if (mc.player != null) {
                int initialSeconds = (int) Math.ceil(duration / 20.0);
                mc.player.sendMessage(new TextComponentString("§b[诅咒之渊着色器] §f着色器 §e" + shaderName + " §f已激活，初始持续时间: §a" + initialSeconds + " §f秒。"));
            }
            debugMessageTimer = 0;
            */
        }
    }

    public void deactivateShader() {
        shaderTimer = 0;
        // debugMessageTimer = 0; // 移除此行
        System.out.println("[ShaderLoader] Shader deactivated");
        // 移除停用时发送调试信息
        /*
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.player != null) {
            mc.player.sendMessage(new TextComponentString("§b[诅咒之渊着色器] §f着色器已停用。"));
        }
        */
    }

    public boolean isShaderActive() {
        return shaderTimer > 0;
    }

    public String getActiveShaderName() {
        return isShaderActive() ? currentShaderName : null;
    }

    public void reload() {
        needsReload = true;
    }

    // 获取所有注册的着色器名称
    public String[] getAvailableShaders() {
        return registeredShaders.keySet().toArray(new String[0]);
    }
}