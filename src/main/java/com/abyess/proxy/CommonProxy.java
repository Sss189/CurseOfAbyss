package com.abyess.proxy;

import net.minecraft.item.Item;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;

public class CommonProxy {
    public void registerItemRenderer(Item item) {}

    public void preInit(FMLPreInitializationEvent event) {
        // 通用初始化代码（服务端和客户端都会执行）
    }

    public void init(FMLInitializationEvent event) {
        // 基础逻辑（可为空）
    }

}
