package com.walhay.gregifiedenergistics.api.capability;

import gregtech.api.capability.DualHandler;
import gregtech.api.capability.IMultipleTankHandler;
import gregtech.api.capability.impl.GhostCircuitItemStackHandler;
import gregtech.api.capability.impl.ItemHandlerList;
import java.util.Arrays;
import java.util.List;
import net.minecraft.item.ItemStack;
import net.minecraftforge.items.IItemHandlerModifiable;
import org.jetbrains.annotations.NotNull;

/** DualHandlerPattern */
public class PatternBufferDualHandler extends DualHandler {

	private final IItemHandlerModifiable itemInventory;
	private final GhostCircuitItemStackHandler circuitInventory;

	@Override
	public @NotNull ItemStack getStackInSlot(int slot) {
		if (slot < 0 || slot >= getSlots()) return ItemStack.EMPTY;

		return super.getStackInSlot(slot);
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

		List<IMultipleTankHandler.ITankEntry> entries = super.getFluidTanks();
		entries.clear();
		for (var entry : fluidTank.getFluidTanks()) {
			entries.add(new DualEntry(this, entry));
		}
	}

	public void onResize() {
		this.itemDelegate = new ItemHandlerList(Arrays.asList(itemInventory, circuitInventory));
	}
}
