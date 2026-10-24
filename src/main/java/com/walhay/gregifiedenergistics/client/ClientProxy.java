package com.walhay.gregifiedenergistics.client;

import com.walhay.gregifiedenergistics.client.render.GregifiedEnergisticsTextures;
import com.walhay.gregifiedenergistics.common.CommonProxy;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;

public class ClientProxy extends CommonProxy {

	@Override
	public void preInit(FMLPreInitializationEvent event) {
		super.preInit(event);
		GregifiedEnergisticsTextures.init();
	}
}
