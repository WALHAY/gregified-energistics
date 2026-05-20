package com.walhay.gregifiedenergistics.api.capability;

import gregtech.api.capability.IMultipleTankHandler;
import gregtech.api.capability.impl.GhostCircuitItemStackHandler;
import gregtech.api.capability.impl.ItemHandlerList;
import gregtech.api.capability.impl.NotifiableItemStackHandler;
import java.util.Arrays;
import java.util.List;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.common.util.INBTSerializable;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidTankProperties;
import net.minecraftforge.items.IItemHandlerModifiable;
import org.jetbrains.annotations.NotNull;

/** PatternBufferDualDelegate */
public class PatternBufferDualDelegate
		implements IItemHandlerModifiable, IMultipleTankHandler, INBTSerializable<NBTTagCompound> {

	private final GhostCircuitItemStackHandler ghostCircuit;
	private final IItemHandlerModifiable itemDelegate;
	private IMultipleTankHandler fluidDelegate;

	private ItemHandlerList inventory;

	public PatternBufferDualDelegate(
			IItemHandlerModifiable itemDelegate,
			GhostCircuitItemStackHandler ghostCircuit,
			IMultipleTankHandler fluidDelegate) {
		this.ghostCircuit = ghostCircuit;
		this.itemDelegate = itemDelegate;
		this.inventory = new ItemHandlerList(Arrays.asList(itemDelegate, ghostCircuit));
		this.fluidDelegate = fluidDelegate;
	}

	@Override
	public int getSlots() {
		return inventory.getSlots();
	}

	@Override
	public ItemStack getStackInSlot(int slot) {
		return inventory.getStackInSlot(slot);
	}

	@Override
	public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
		return inventory.insertItem(slot, stack, simulate);
	}

	@Override
	public ItemStack extractItem(int slot, int amount, boolean simulate) {
		return inventory.extractItem(slot, amount, simulate);
	}

	@Override
	public int getSlotLimit(int slot) {
		return inventory.getSlotLimit(slot);
	}

	@Override
	public void setStackInSlot(int slot, ItemStack stack) {
		inventory.setStackInSlot(slot, stack);
	}

	@Override
	public IFluidTankProperties[] getTankProperties() {
		return fluidDelegate.getTankProperties();
	}

	@Override
	public int fill(FluidStack resource, boolean doFill) {
		return fluidDelegate.fill(resource, doFill);
	}

	@Override
	public FluidStack drain(FluidStack resource, boolean doDrain) {
		return fluidDelegate.drain(resource, doDrain);
	}

	@Override
	public FluidStack drain(int maxDrain, boolean doDrain) {
		return fluidDelegate.drain(maxDrain, doDrain);
	}

	@Override
	public NBTTagCompound serializeNBT() {
		var nbt = new NBTTagCompound();
		return nbt;
	}

	@Override
	public void deserializeNBT(NBTTagCompound nbt) {}

	@Override
	public boolean allowSameFluidFill() {
		return fluidDelegate.allowSameFluidFill();
	}

	@Override
	public @NotNull List<ITankEntry> getFluidTanks() {
		return fluidDelegate.getFluidTanks();
	}

	@Override
	public @NotNull ITankEntry getTankAt(int index) {
		return fluidDelegate.getTankAt(index);
	}

	@Override
	public int getTanks() {
		return fluidDelegate.getTanks();
	}

	public void setFluidDelegate(IMultipleTankHandler fluidDelegate) {
		this.fluidDelegate = fluidDelegate;
	}

	public void setSize(int slots) {
		if (itemDelegate instanceof NotifiableItemStackHandler handler) {
			handler.setSize(slots);
			inventory = new ItemHandlerList(Arrays.asList(itemDelegate, ghostCircuit));
		}
	}
}
