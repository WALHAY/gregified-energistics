package com.walhay.gregifiedenergistics.api.capability;

import gregtech.api.capability.IMultipleTankHandler;
import gregtech.api.capability.INotifiableHandler;
import gregtech.api.capability.impl.GhostCircuitItemStackHandler;
import gregtech.api.metatileentity.MetaTileEntity;
import gregtech.api.util.ItemStackHashStrategy;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidTankProperties;
import net.minecraftforge.items.IItemHandlerModifiable;
import net.minecraftforge.items.ItemStackHandler;
import org.jetbrains.annotations.NotNull;

/** PatternBufferDualDelegate */
public class PatternBufferDualDelegate implements IItemHandlerModifiable, IMultipleTankHandler, INotifiableHandler {

	private final GhostCircuitItemStackHandler ghostCircuit;
	private final IItemHandlerModifiable itemDelegate;
	private IMultipleTankHandler fluidDelegate;
	private List<MetaTileEntity> notifiables;

	@NotNull private static final ItemStackHashStrategy strategy = ItemStackHashStrategy.comparingAll();

	// May be it will be worth to keep default ItemHandlerList as its backing handlers changes not quite often
	private SegmentItemHandlerList inventory;

	public PatternBufferDualDelegate(
			IItemHandlerModifiable itemDelegate,
			GhostCircuitItemStackHandler ghostCircuit,
			IMultipleTankHandler fluidDelegate) {
		this.notifiables = new ArrayList<>();

		this.ghostCircuit = ghostCircuit;
		this.itemDelegate = itemDelegate;
		this.inventory = new SegmentItemHandlerList(itemDelegate, ghostCircuit);
		this.fluidDelegate = fluidDelegate;
	}

	public void onContentsChanged(Object handler) {
		for (MetaTileEntity metaTileEntity : notifiables) {
			addToNotifiedList(metaTileEntity, handler, false);
		}
	}

	public void onContentsChanged() {
		onContentsChanged(this);
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
	public @NotNull ItemStack insertItem(int slot, @NotNull ItemStack stack, boolean simulate) {
		var remainder = itemDelegate.insertItem(slot, stack, simulate);
		if (!simulate && !strategy.equals(remainder, stack)) onContentsChanged();
		return remainder;
	}

	@Override
	public @NotNull ItemStack extractItem(int slot, int amount, boolean simulate) {
		var extracted = itemDelegate.extractItem(slot, amount, simulate);
		if (!simulate && !extracted.isEmpty()) onContentsChanged();
		return extracted;
	}

	@Override
	public int getSlotLimit(int slot) {
		return inventory.getSlotLimit(slot);
	}

	@Override
	public void setStackInSlot(int slot, @NotNull ItemStack stack) {
		var oldStack = itemDelegate.getStackInSlot(slot);
		itemDelegate.setStackInSlot(slot, stack);
		if (!strategy.equals(oldStack, stack)) onContentsChanged();
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
		var drained = fluidDelegate.drain(resource, doDrain);
		if (doDrain && drained != null) onContentsChanged();
		return drained;
	}

	@Override
	public FluidStack drain(int maxDrain, boolean doDrain) {
		var drained = fluidDelegate.drain(maxDrain, doDrain);
		if (doDrain && drained != null) onContentsChanged();
		return drained;
	}

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
		if (itemDelegate instanceof ItemStackHandler handler) {
			handler.setSize(slots);
			inventory.onHandlerChange(handler);
		}
	}

	@Override
	public void addNotifiableMetaTileEntity(MetaTileEntity mte) {
		if (mte == null || notifiables.contains(mte)) return;

		notifiables.add(mte);
	}

	@Override
	public void removeNotifiableMetaTileEntity(MetaTileEntity mte) {
		notifiables.remove(mte);
	}
}
