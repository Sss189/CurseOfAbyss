package com.abyess;

import com.abyess.Hollow.EntityRegistryHandler;
import com.abyess.Network.NetworkHandler;


import com.abyess.commands.CommandCurse;

import com.abyess.commands.CommandFogControl;
import com.abyess.config.ModConfig;
import com.abyess.debuff.BleedEffectHandler;
import com.abyess.debuff.ConfusionDebuffHandler;
import com.abyess.debuff.TurnIntoHollow;
import com.abyess.debuff.hollowbleed;

import com.abyess.items.Scapegoat.ItemScapegoat;

import com.abyess.items.Starcompass.CompassGlowHandler;
import com.abyess.items.Starcompass.EntityStarCompassItem;
import com.abyess.items.Starcompass.ItemStarCompass;
import com.abyess.items.Starcompass.RenderStarCompassItem;
import com.abyess.proxy.CommonProxy;
import com.abyess.render.*;
import com.abyess.shaders.CustomShaderLoader;
import com.abyess.tracker.CurseHUD;
import com.abyess.tracker.CurseTriggerHandler;

import com.abyess.tracker.FogRenderer;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.init.PotionTypes;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.potion.PotionUtils;

import net.minecraft.world.World;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.client.registry.RenderingRegistry;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.SidedProxy;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.event.FMLServerStartingEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.network.NetworkRegistry;
import net.minecraftforge.oredict.OreDictionary;


import java.io.File;

@Mod.EventBusSubscriber
@Mod(modid = "curseofabyss", version = "1.0", name = "Curse of Abyss", guiFactory = "com.abyess.config.ModGuiFactory" )
public class main {
    @Mod.Instance("curseofabyss")
    public static main instance;
    public static final String MODID = "curseofabyss";
    // 效果渲染器声明
    public static final InvertedColorFilter INVERTED_COLOR_FILTER_RENDERER = new InvertedColorFilter();
    public static final OverlappingBlurFilter CURSE2_EFFECT_RENDERER = new OverlappingBlurFilter();
    public static final RedFilter RED_FILTER_RENDERER = new RedFilter();



    @SidedProxy(
            clientSide = "com.abyess.proxy.ClientProxy",
            serverSide = "com.abyess.proxy.CommonProxy"
    )
    public static CommonProxy proxy;



    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        // 注册事件监听器
        MinecraftForge.EVENT_BUS.register(new CurseTriggerHandler());
        MinecraftForge.EVENT_BUS.register(new BleedEffectHandler());
        MinecraftForge.EVENT_BUS.register(new ConfusionDebuffHandler());
        MinecraftForge.EVENT_BUS.register(TurnIntoHollow.class);
        MinecraftForge.EVENT_BUS.register(new hollowbleed());
        CustomShaderLoader.INSTANCE.init();

        MinecraftForge.EVENT_BUS.register(new CurseHUD());

        // 其他初始化
        MinecraftForge.EVENT_BUS.register(ItemStarCompass.class);
        ItemStarCompass.init(); // 注册视角锁定处理器
    }

    @Mod.EventHandler
    public void onPostInit(FMLPostInitializationEvent event) {
        // 初始化触发器
        CurseTriggerHandler.initializeTriggers();

      //  CustomPlayerIconsManager.loadCustomIcons();
    }

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {

        ModConfig.loadConfig();

        proxy.preInit(event);
        // ==============================================================
        // 新增：创建自定义图标文件夹的逻辑
        // ==============================================================
        File modConfigDir = new File(event.getModConfigurationDirectory(), MODID);
        if (!modConfigDir.exists()) {
            modConfigDir.mkdirs();
        }




        new EntityRegistryHandler().preInit(event);
        RenderingRegistry.registerEntityRenderingHandler(EntityStarCompassItem.class, RenderStarCompassItem::new);

        // 注册药水到矿物词典
        ItemStack healingPotion1 = new ItemStack(Items.POTIONITEM);
        PotionUtils.addPotionToItemStack(healingPotion1, PotionTypes.HEALING);
        OreDictionary.registerOre("curse_scapegoat_potion", healingPotion1);

        ItemStack healingPotion2 = new ItemStack(Items.POTIONITEM);
        PotionUtils.addPotionToItemStack(healingPotion2, PotionTypes.STRONG_HEALING);
        OreDictionary.registerOre("curse_scapegoat_potion", healingPotion2);

        NetworkHandler.registerPackets();


        // 注册二维生物渲染事件处理器（仅客户端）
        if (event.getSide().isClient()) {
            MinecraftForge.EVENT_BUS.register(new net.minecraft.client.renderer.entity.FlatEntities());
        }
        // 加载配置

        // 先加载配置
        // 再注册事件
        MinecraftForge.EVENT_BUS.register(CompassGlowHandler.class);
    }
    // 添加服务器启动事件处理

    @SubscribeEvent
    public static void registerItems(RegistryEvent.Register<Item> event) {
        event.getRegistry().register(ItemScapegoat.INSTANCE); // 注册唯一实例
        event.getRegistry().register(ItemStarCompass.INSTANCE); // 使用静态实例
    }

    @Mod.EventHandler
    public void onServerStarting(FMLServerStartingEvent event) {
        // 注册你的命令
        event.registerServerCommand(new CommandCurse());
        event.registerServerCommand(new CommandFogControl());
    }


}