package com.walhay.gregifiedenergistics.api.capability;

import gregtech.api.capability.DualHandler;
import gregtech.api.capability.IMultipleTankHandler;
import gregtech.api.capability.impl.GhostCircuitItemStackHandler;
import gregtech.api.capability.impl.ItemHandlerList;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.List;
import net.minecraftforge.items.IItemHandlerModifiable;
import org.jetbrains.annotations.NotNull;

/** DualHandlerPattern */
public class PatternBufferDualHandler extends DualHandler {

	private final IItemHandlerModifiable itemInventory;
	private final GhostCircuitItemStackHandler circuitInventory;

	private static final Field UNWRAPPED;

	static {
		try {
			UNWRAPPED = DualHandler.class.getDeclaredField("unwrapped");
			UNWRAPPED.setAccessible(true);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	public PatternBufferDualHandler(
			@NotNull IItemHandlerModifiable itemDelegate,
			GhostCircuitItemStackHandler circuitInventory,
			@NotNull IMultipleTankHandler fluidTank) {
		super(new ItemHandlerList(Arrays.asList(itemDelegate, circuitInventory)), fluidTank, false);
		this.itemInventory = itemDelegate;
		this.circuitInventory = circuitInventory;
	}

	public void setFluidDelegate(IMultipleTankHandler fluidTank) {
		this.fluidDelegate = fluidTank;

		try {
			@SuppressWarnings("unchecked")
			List<IMultipleTankHandler.ITankEntry> unwrapped =
					(List<IMultipleTankHandler.ITankEntry>) UNWRAPPED.get(this);

			unwrapped.clear();

			for (var entry : this.fluidDelegate.getFluidTanks()) {
				unwrapped.add(entry instanceof DualEntry dualEntry ? dualEntry : new DualEntry(this, entry));
			}
		} catch (Exception e) {
			new RuntimeException(e);
		}
	}

	public void onResize() {
		this.itemDelegate = new ItemHandlerList(Arrays.asList(itemInventory, circuitInventory));
	}
}
