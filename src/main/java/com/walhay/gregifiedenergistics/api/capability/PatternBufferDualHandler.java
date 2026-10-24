package com.walhay.gregifiedenergistics.api.capability;

import gregtech.api.capability.IMultipleTankHandler;
import gregtech.api.capability.INotifiableHandler;
import gregtech.api.capability.impl.GhostCircuitItemStackHandler;
import gregtech.api.metatileentity.MetaTileEntity;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidTankProperties;
import net.minecraftforge.items.IItemHandlerModifiable;
import org.jetbrains.annotations.NotNull;

/** Dynamic pattern inputs using the item and fluid interfaces available in GTCEu 2.8.10. */
public class PatternBufferDualHandler extends SegmentItemHandlerList
		implements IMultipleTankHandler, INotifiableHandler {
	private IMultipleTankHandler fluidDelegate;
	private final List<MetaTileEntity> notifiableEntities = new ArrayList<>();

	public PatternBufferDualHandler(
			@NotNull IItemHandlerModifiable itemDelegate,
			GhostCircuitItemStackHandler circuitInventory,
			@NotNull IMultipleTankHandler fluidTank) {
		super(itemDelegate, circuitInventory);
		this.fluidDelegate = fluidTank;
	}

	public void setFluidDelegate(IMultipleTankHandler fluidTank) {
		for (MetaTileEntity entity : notifiableEntities) {
			entity.getNotifiedFluidInputList().remove(fluidDelegate);
		}
		this.fluidDelegate = fluidTank;
		onContentsChanged();
	}

	public IMultipleTankHandler getFluidDelegate() {
		return fluidDelegate;
	}

	public void onResize() {
		// SegmentItemHandlerList reads the current slot counts on every access.
		onContentsChanged();
	}

	public void onContentsChanged() {
		for (MetaTileEntity entity : notifiableEntities) {
			addToNotifiedList(entity, this, false);
			// GTCEu 2.8.10 classifies a combined handler as items only. Notify fluids separately.
			addToNotifiedList(entity, fluidDelegate, false);
		}
	}

	@Override
	public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
		int count = stack.getCount();
		ItemStack remainder = super.insertItem(slot, stack, simulate);
		if (!simulate && remainder.getCount() != count) onContentsChanged();
		return remainder;
	}

	@Override
	public ItemStack extractItem(int slot, int amount, boolean simulate) {
		ItemStack extracted = super.extractItem(slot, amount, simulate);
		if (!simulate && !extracted.isEmpty()) onContentsChanged();
		return extracted;
	}

	@Override
	public void setStackInSlot(int slot, ItemStack stack) {
		ItemStack previous = getStackInSlot(slot).copy();
		super.setStackInSlot(slot, stack);
		if (!ItemStack.areItemStacksEqual(previous, getStackInSlot(slot))) onContentsChanged();
	}

	@Override
	public void addNotifiableMetaTileEntity(MetaTileEntity entity) {
		if (entity != null && !notifiableEntities.contains(entity)) notifiableEntities.add(entity);
	}

	@Override
	public void removeNotifiableMetaTileEntity(MetaTileEntity entity) {
		notifiableEntities.remove(entity);
	}

	@Override
	public @NotNull List<MultiFluidTankEntry> getFluidTanks() {
		return fluidDelegate.getFluidTanks();
	}

	@Override
	public int getTanks() {
		return fluidDelegate.getTanks();
	}

	@Override
	public @NotNull MultiFluidTankEntry getTankAt(int index) {
		return fluidDelegate.getTankAt(index);
	}

	@Override
	public boolean allowSameFluidFill() {
		return fluidDelegate.allowSameFluidFill();
	}

	@Override
	public IFluidTankProperties[] getTankProperties() {
		return fluidDelegate.getTankProperties();
	}

	@Override
	public int fill(FluidStack resource, boolean doFill) {
		int filled = fluidDelegate.fill(resource, doFill);
		if (doFill && filled > 0) onContentsChanged();
		return filled;
	}

	@Override
	public FluidStack drain(FluidStack resource, boolean doDrain) {
		FluidStack drained = fluidDelegate.drain(resource, doDrain);
		if (doDrain && drained != null && drained.amount > 0) onContentsChanged();
		return drained;
	}

	@Override
	public FluidStack drain(int maxDrain, boolean doDrain) {
		FluidStack drained = fluidDelegate.drain(maxDrain, doDrain);
		if (doDrain && drained != null && drained.amount > 0) onContentsChanged();
		return drained;
	}
}
