package com.abyess.proxy;

import com.abyess.Hollow.EntityHollow;
import com.abyess.Hollow.RenderHollow;
import com.abyess.items.Scapegoat.ItemScapegoat;
import com.abyess.items.Starcompass.ItemStarCompass;
import com.abyess.tracker.FogRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.model.ModelBakery;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.client.renderer.entity.Render;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.item.Item;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.event.ModelRegistryEvent;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.client.registry.IRenderFactory;
import net.minecraftforge.fml.client.registry.RenderingRegistry;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;


@Mod.EventBusSubscriber(Side.CLIENT)
public class ClientProxy extends CommonProxy {


    @Override
    public void preInit(FMLPreInitializationEvent event) {
        super.preInit(event);
     //   MinecraftForge.EVENT_BUS.register(new ArmorColorExtractor());
        MinecraftForge.EVENT_BUS.register(new FogRenderer());
        RenderingRegistry.registerEntityRenderingHandler(EntityHollow.class, new IRenderFactory<EntityHollow>() {
            @Override
            public Render<? super EntityHollow> createRenderFor(RenderManager manager) {
                return new RenderHollow(manager);
            }
        });
    }
    // 修改后的ModelRegistryEvent处理
    @SubscribeEvent
    public static void registerModels(ModelRegistryEvent event) {
        // 注册替罪羊的模型
        registerSimpleItemModel(ItemScapegoat.INSTANCE);

        // 注册星之指南针的模型变种和默认模型
        ModelBakery.registerItemVariants(
                ItemStarCompass.INSTANCE,
                new ResourceLocation("curseofabyss", "star_compass"),
                new ResourceLocation("curseofabyss", "star_compass_base"),
                new ResourceLocation("curseofabyss", "star_compass_needle")
        );

        ModelLoader.setCustomModelResourceLocation(
                ItemStarCompass.INSTANCE,
                0,
                new ModelResourceLocation("curseofabyss:star_compass", "inventory")
        );
    }

    // 标准物品模型注册方法
    private static void registerSimpleItemModel(Item item) {
        ModelLoader.setCustomModelResourceLocation(
                item,
                0,
                new ModelResourceLocation(item.getRegistryName(), "inventory")
        );
    }






}