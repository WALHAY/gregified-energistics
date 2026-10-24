package com.walhay.gregifiedenergistics.api.util;

import com.walhay.gregifiedenergistics.api.capability.PatternBufferDualHandler;
import gregtech.api.capability.IMultipleTankHandler;
import gregtech.api.capability.impl.FluidTankList;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import net.minecraftforge.fluids.IFluidTank;
import net.minecraftforge.items.IItemHandlerModifiable;

/** Adds current buffer tanks to the normal fluid hatches, preserving each tank's fill policy. */
public final class PatternBufferFluidInputs {
	private PatternBufferFluidInputs() {}

	public static IMultipleTankHandler combine(
			IMultipleTankHandler fluids, Collection<? extends IItemHandlerModifiable> handlers) {
		List<IFluidTank> tanks = new ArrayList<>(fluids.getFluidTanks());
		for (var handler : handlers) {
			if (handler instanceof PatternBufferDualHandler buffer) tanks.addAll(buffer.getFluidTanks());
		}
		return new FluidTankList(fluids.allowSameFluidFill(), tanks);
	}
}
