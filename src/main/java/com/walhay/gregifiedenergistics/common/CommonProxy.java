package com.walhay.gregifiedenergistics.common;

import com.walhay.gregifiedenergistics.GregifiedEnergisticsMod;
import com.walhay.gregifiedenergistics.api.capability.GregifiedEnergisticsCapabilities;
import com.walhay.gregifiedenergistics.common.metatileentities.GregifiedEnergisticsMetaTileEntities;
import com.walhay.gregifiedenergistics.loader.recipes.GregifiedEnergisticsRecipes;
import net.minecraft.item.crafting.IRecipe;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

@Mod.EventBusSubscriber(modid = GregifiedEnergisticsMod.MOD_ID)
public class CommonProxy {

	public void preInit(FMLPreInitializationEvent event) {
		GregifiedEnergisticsMetaTileEntities.init();
		GregifiedEnergisticsCapabilities.register();
	}

	@SubscribeEvent
	public static void registerRecipes(RegistryEvent.Register<IRecipe> event) {
		GregifiedEnergisticsRecipes.load();
	}
}
