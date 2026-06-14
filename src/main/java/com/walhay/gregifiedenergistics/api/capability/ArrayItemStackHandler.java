package com.walhay.gregifiedenergistics.api.capability;

import gregtech.api.capability.INotifiableHandler;
import gregtech.api.metatileentity.MetaTileEntity;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.item.ItemStack;
import net.minecraftforge.items.IItemHandlerModifiable;

/** ArrayItemStackHandler */
public class ArrayItemStackHandler implements IItemHandlerModifiable, INotifiableHandler {

	private final List<ItemStack> stacks;
	private final List<MetaTileEntity> mtes = new ArrayList<>();

	public ArrayItemStackHandler(List<ItemStack> stacks) {
		this.stacks = stacks;
	}

	protected void validateSlotIndex(int slot) {
		if (slot < 0 || slot >= stacks.size())
			throw new RuntimeException("Slot " + slot + " not in valid range - [0," + stacks.size() + ")");
	}

	@Override
	public int getSlots() {
		return stacks.size();
	}

	@Override
	public ItemStack getStackInSlot(int slot) {
		validateSlotIndex(slot);

		return stacks.get(slot);
	}

	@Override
	public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
		// validateSlotIndex(slot);
		//
		// if(!simulate) {
		// 	stacks.add(slot, stack);
		// }
		// return ItemStack.EMPTY;
		throw new UnsupportedOperationException("Insertion is not supported");
	}

	@Override
	public ItemStack extractItem(int slot, int amount, boolean simulate) {
		validateSlotIndex(slot);

		var stack = stacks.get(slot);
		int count = stack.getCount();

		if (count > amount) {
			var newStack = stack.copy();
			newStack.setCount(amount);
			if (!simulate) {
				stack.setCount(count - amount);
			}
			return newStack;
		}

		return stack.copy();
	}

	@Override
	public int getSlotLimit(int slot) {
		return Integer.MAX_VALUE;
	}

	@Override
	public void addNotifiableMetaTileEntity(MetaTileEntity mte) {
		if (mte != null && !mtes.contains(mte)) {
			mtes.add(mte);
		}
	}

	@Override
	public void removeNotifiableMetaTileEntity(MetaTileEntity mte) {
		mtes.remove(mte);
	}

	@Override
	public void setStackInSlot(int slot, ItemStack stack) {
		stacks.set(slot, stack);
	}

	protected void onContentsChanged() {
		for (var mte : mtes) {
			addToNotifiedList(mte, this, false);
		}
	}
}
