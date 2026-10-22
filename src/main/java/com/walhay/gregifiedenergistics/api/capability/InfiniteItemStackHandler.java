package com.walhay.gregifiedenergistics.api.capability;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraftforge.common.util.Constants.NBT;
import net.minecraftforge.items.ItemHandlerHelper;
import net.minecraftforge.items.ItemStackHandler;

/** InfiniteStackHandler */
public class InfiniteItemStackHandler extends ItemStackHandler {

	public InfiniteItemStackHandler(int slots) {
		super(slots);
	}

	@Override
	protected int getStackLimit(int slot, ItemStack stack) {
		return Integer.MAX_VALUE;
	}

	@Override
	public int getSlotLimit(int slot) {
		return Integer.MAX_VALUE;
	}

	@Override
	public ItemStack getStackInSlot(int slot) {
		if (getSlots() == 0) return ItemStack.EMPTY;

		return this.stacks.get(slot);
	}

	@Override
	public NBTTagCompound serializeNBT() {
		NBTTagCompound data = super.serializeNBT();
		NBTTagList items = data.getTagList("Items", NBT.TAG_COMPOUND);
		for (int i = 0; i < items.tagCount(); i++) {
			NBTTagCompound entry = items.getCompoundTagAt(i);
			entry.setInteger("StoredCount", stacks.get(entry.getInteger("Slot")).getCount());
			entry.setByte("Count", (byte) 1);
		}
		return data;
	}

	@Override
	public void deserializeNBT(NBTTagCompound data) {
		// Keep compatibility with inventories saved before full counts were introduced.
		super.deserializeNBT(data);
		NBTTagList items = data.getTagList("Items", NBT.TAG_COMPOUND);
		for (int i = 0; i < items.tagCount(); i++) {
			NBTTagCompound entry = items.getCompoundTagAt(i);
			int slot = entry.getInteger("Slot");
			if (slot >= 0 && slot < getSlots() && entry.hasKey("StoredCount", NBT.TAG_INT)) {
				stacks.get(slot).setCount(Math.max(0, entry.getInteger("StoredCount")));
			}
		}
	}

	@Override
	public ItemStack extractItem(int slot, int amount, boolean simulate) {
		if (amount <= 0) {
			return ItemStack.EMPTY;
		}

		validateSlotIndex(slot);

		ItemStack existing = this.stacks.get(slot);

		if (existing.isEmpty()) {
			return ItemStack.EMPTY;
		}

		if (existing.getCount() <= amount) {
			if (!simulate) {
				this.stacks.set(slot, ItemStack.EMPTY);
				onContentsChanged(slot);
			}
			return existing.copy();
		} else {
			if (!simulate) {
				this.stacks.set(slot, ItemHandlerHelper.copyStackWithSize(existing, existing.getCount() - amount));
				onContentsChanged(slot);
			}

			return ItemHandlerHelper.copyStackWithSize(existing, amount);
		}
	}
}
