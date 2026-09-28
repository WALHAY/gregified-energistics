package com.walhay.gregifiedenergistics;

import com.walhay.gregifiedenergistics.common.CommonProxy;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventHandler;
import net.minecraftforge.fml.common.SidedProxy;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Mod(
		modid = GregifiedEnergisticsMod.MOD_ID,
		name = GregifiedEnergisticsMod.NAME,
		version = GregifiedEnergisticsMod.VERSION,
		dependencies = "required-after:gregtech@[2.8,2.9);required-after:appliedenergistics2;after:jei@[4.15.0,);")
public class GregifiedEnergisticsMod {
	public static final String MOD_ID = Tags.MOD_ID;
	public static final String NAME = Tags.MOD_NAME;
	public static final String VERSION = Tags.VERSION;

	@Mod.Instance
	public static GregifiedEnergisticsMod INSTANCE;

	public static final Logger LOGGER = LogManager.getLogger(MOD_ID);

	@SidedProxy(
			modId = GregifiedEnergisticsMod.MOD_ID,
			clientSide = "com.walhay.gregifiedenergistics.client.ClientProxy",
			serverSide = "com.walhay.gregifiedenergistics.common.CommonProxy")
	public static CommonProxy proxy;

	@EventHandler
	public void preInit(FMLPreInitializationEvent event) {
		proxy.preInit(event);
	}

	public static ResourceLocation gregifiedEnergisticsId(String path) {
		return new ResourceLocation(GregifiedEnergisticsMod.MOD_ID, path);
	}
}
