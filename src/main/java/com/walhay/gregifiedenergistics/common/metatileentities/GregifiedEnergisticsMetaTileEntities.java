package com.walhay.gregifiedenergistics.common.metatileentities;

import static gregtech.common.metatileentities.MetaTileEntities.registerMetaTileEntity;

import com.walhay.gregifiedenergistics.GregifiedEnergisticsMod;
import com.walhay.gregifiedenergistics.common.metatileentities.multiblockparts.MTEMEAssemblyLineBus;
import com.walhay.gregifiedenergistics.common.metatileentities.multiblockparts.MTEMEAssemblyLineOpticalBus;
import com.walhay.gregifiedenergistics.common.metatileentities.multiblockparts.MTEMEPatternBuffer;
import net.minecraft.util.ResourceLocation;

public class GregifiedEnergisticsMetaTileEntities {
	private static int id = 11000;

	public static MTEMEAssemblyLineBus ME_ASSEMBLY_LINE_BUS;
	public static MTEMEAssemblyLineOpticalBus ME_ASSEMBLY_LINE_OPTICAL_BUS;
	public static MTEMEPatternBuffer ME_PATTERN_BUFFER;

	public static void init() {
		ME_ASSEMBLY_LINE_BUS =
				registerMetaTileEntity(autoId(), new MTEMEAssemblyLineBus(location("me_assembly_line_bus")));
		ME_ASSEMBLY_LINE_OPTICAL_BUS = registerMetaTileEntity(
				autoId(), new MTEMEAssemblyLineOpticalBus(location("me_assembly_line_optical_bus")));
		ME_PATTERN_BUFFER = registerMetaTileEntity(autoId(), new MTEMEPatternBuffer(location("me_pattern_buffer")));
	}

	private static ResourceLocation location(String location) {
		return new ResourceLocation(GregifiedEnergisticsMod.MOD_ID, location);
	}

	private static int autoId() {
		return id++;
	}
}
