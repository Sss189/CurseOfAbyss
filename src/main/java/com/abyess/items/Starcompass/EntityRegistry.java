package com.abyess.items.Starcompass;

import net.minecraft.util.ResourceLocation;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.registry.EntityEntry;
import net.minecraftforge.fml.common.registry.EntityEntryBuilder;

@Mod.EventBusSubscriber
public class EntityRegistry {
    private static final String MODID = "curseofabyss"; // 必须与@Mod注解中的modid完全一致
    private static final ResourceLocation ENTITY_ID = new ResourceLocation(MODID, "star_compass_item");

    @SubscribeEvent
    public static void registerEntities(RegistryEvent.Register<EntityEntry> event) {
        // 必须使用.name()设置带有命名空间的名称
        EntityEntry entry = EntityEntryBuilder.create()
                .entity(EntityStarCompassItem.class)
                .id(ENTITY_ID, 1)  // 第一个参数必须与.name()参数匹配
                .name(ENTITY_ID.toString()) // 关键修复点：必须为"modid:entityname"格式
                .tracker(64, 20, true)
                .build();

        event.getRegistry().register(entry);
    }
}