package com.abyess.items;

import com.abyess.items.Scapegoat.ItemScapegoat;
import com.abyess.items.Starcompass.ItemStarCompass;
import com.abyess.main;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.oredict.ShapedOreRecipe;

import java.util.ArrayList;
import java.util.List;

@Mod.EventBusSubscriber(modid = "curseofabyss")
public class ModRecipes {

    @SubscribeEvent
    public static void registerRecipes(RegistryEvent.Register<IRecipe> event) {
        ItemStack output =  new ItemStack(ItemScapegoat.INSTANCE);// 使用 ItemScapegoat.INSTANCE
        Object redstone = Items.REDSTONE;
        Object milk = Items.MILK_BUCKET;
        Object potion = "curse_scapegoat_potion"; // 使用新的矿典标签

        // 生成所有6种排列组合
        List<Object[]> permutations = new ArrayList<>();
        permutations.add(new Object[]{redstone, milk, potion});
        permutations.add(new Object[]{redstone, potion, milk});
        permutations.add(new Object[]{milk, redstone, potion});
        permutations.add(new Object[]{milk, potion, redstone});
        permutations.add(new Object[]{potion, redstone, milk});
        permutations.add(new Object[]{potion, milk, redstone});

        int recipeNum = 1;
        for (Object[] materials : permutations) {
            // 创建配方形状
            ShapedOreRecipe recipe = new ShapedOreRecipe(
                    null,
                    output,
                    "ABC",  // 第一行
                    "DED",  // 第二行
                    "FFF",  // 第三行
                    'A', materials[0], // 第一行第一个材料
                    'B', materials[1], // 第一行第二个材料
                    'C', materials[2], // 第一行第三个材料
                    'D', Items.IRON_INGOT, // 第二行和第三行的D
                    'E', Items.MUTTON,      // 第二行中间的E
                    'F', Items.IRON_INGOT   // 第三行的F
            );
            recipe.setRegistryName(new ResourceLocation("curseofabyss", "scapegoat_recipe_" + recipeNum++));
            event.getRegistry().register(recipe);
        }

        // 新增 ItemStarCompass 的合成配方
        ItemStack starCompassOutput = new ItemStack(ItemStarCompass.INSTANCE);
        ShapedOreRecipe starCompassRecipe = new ShapedOreRecipe(
                null,
                starCompassOutput,
                "GGG",   // 第一行全玻璃
                "ONO",   // 中间行：金锭-下界之星-金锭
                "GGG",   // 第三行全玻璃
                'G', "blockGlass",    // 使用矿典标签匹配任意玻璃块
                'O', "ingotGold",    // 矿典标签匹配任意金锭
                'N', Items.NETHER_STAR
        );
        starCompassRecipe.setRegistryName(new ResourceLocation("curseofabyss", "star_compass_recipe"));
        event.getRegistry().register(starCompassRecipe);

    }
}