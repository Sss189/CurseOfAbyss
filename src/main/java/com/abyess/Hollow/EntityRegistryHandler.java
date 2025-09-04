package com.abyess.Hollow;



import com.abyess.main;
import net.minecraft.entity.EnumCreatureType;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.biome.Biome;
import net.minecraftforge.common.BiomeDictionary;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.registry.EntityRegistry;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

public class EntityRegistryHandler {

    // 在 Mod 主类中（如 ModMain.java）
    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        // 注册实体，参数：实体类、实体名称、网络ID、Mod实例、追踪范围、更新间隔、是否发送速度更新
        // 正确参数顺序（1.12.2）
        EntityRegistry.registerModEntity(
                new ResourceLocation("curseofabyss", "hollow"), // registryName
                EntityHollow.class, // entityClass
                "hollow", // entityName（必须与命令/summon一致）
                120, // networkID
                main.instance, // mod实例
                64, // trackingRange
                1, // updateFrequency
                true // sendsVelocityUpdates
        );



    }
///summon curseofabyss:hollow ~ ~ ~
}