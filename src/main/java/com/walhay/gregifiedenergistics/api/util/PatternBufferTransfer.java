package com.walhay.gregifiedenergistics.api.util;

import com.walhay.gregifiedenergistics.api.capability.InfiniteItemStackHandler;
import gregtech.api.capability.impl.FluidTankList;
import gregtech.api.util.GTTransferUtils;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidTank;

/** Checks the entire recipe on independent inventories before accepting any resources. */
public final class PatternBufferTransfer {
	private PatternBufferTransfer() {}

	public static int resizeItems(InfiniteItemStackHandler items, int slots) {
		List<ItemStack> previous = new ArrayList<>(items.getSlots());
		for (int slot = 0; slot < items.getSlots(); slot++) {
			ItemStack stack = items.getStackInSlot(slot).copy();
			previous.add(stack);
			if (!stack.isEmpty()) slots = Math.max(slots, slot + 1);
		}
		items.setSize(slots);
		for (int slot = 0; slot < Math.min(slots, previous.size()); slot++)
			items.setStackInSlot(slot, previous.get(slot));
		return slots;
	}

	public static FluidTankList resizeFluids(FluidTankList previous, int count, Runnable onContentsChanged) {
		for (int index = 0; index < previous.getTanks(); index++) {
			if (previous.getTankAt(index).getFluidAmount() > 0) count = Math.max(count, index + 1);
		}
		List<FluidTank> tanks = new ArrayList<>(count);
		for (int index = 0; index < count; index++) {
			FluidTank tank = new FluidTank(Integer.MAX_VALUE) {
				@Override
				protected void onContentsChanged() {
					onContentsChanged.run();
				}
			};
			if (index < previous.getTanks() && previous.getTankAt(index).getFluid() != null) {
				tank.setFluid(previous.getTankAt(index).getFluid().copy());
			}
			tanks.add(tank);
		}
		return new FluidTankList(false, tanks);
	}

	public static boolean insertInputs(
			InfiniteItemStackHandler items,
			FluidTankList fluids,
			List<ItemStack> itemInputs,
			List<FluidStack> fluidInputs) {
		InfiniteItemStackHandler stagedItems = new InfiniteItemStackHandler(items.getSlots());
		for (int slot = 0; slot < items.getSlots(); slot++) {
			stagedItems.setStackInSlot(slot, items.getStackInSlot(slot).copy());
		}
		List<FluidTank> tanks = new ArrayList<>(fluids.getTanks());
		for (var entry : fluids.getFluidTanks()) {
			FluidTank tank = new FluidTank(entry.getCapacity());
			if (entry.getFluid() != null) tank.setFluid(entry.getFluid().copy());
			tanks.add(tank);
		}
		FluidTankList stagedFluids = new FluidTankList(fluids.allowSameFluidFill(), tanks);
		for (ItemStack input : itemInputs) {
			if (!GTTransferUtils.insertItem(stagedItems, input.copy(), false).isEmpty()) return false;
		}
		for (FluidStack input : fluidInputs) {
			if (input == null || input.amount <= 0 || stagedFluids.fill(input.copy(), true) != input.amount)
				return false;
		}
		for (int slot = 0; slot < items.getSlots(); slot++) {
			ItemStack staged = stagedItems.getStackInSlot(slot);
			if (!ItemStack.areItemStacksEqual(items.getStackInSlot(slot), staged)) items.setStackInSlot(slot, staged);
		}
		for (FluidStack input : fluidInputs) fluids.fill(input.copy(), true);
		return true;
	}
}
