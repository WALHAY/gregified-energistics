package com.walhay.gregifiedenergistics.api.mui;

import com.cleanroommc.modularui.widgets.slot.ModularSlot;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraftforge.items.IItemHandler;

/** ItemSlot */
public class DynamicItemSlot extends ModularSlot {

	public DynamicItemSlot(IItemHandler itemHandler, int index) {
		super(itemHandler, index);
	}

	private boolean isValidHandler() {
		return getItemHandler() != null && getItemHandler().getSlots() > 0;
	}

	@Override
	public ItemStack getStack() {
		if (isValidHandler()) return super.getStack();

		return ItemStack.EMPTY;
	}

	@Override
	public boolean canTakeStack(EntityPlayer playerIn) {
		return isValidHandler() && super.canTakeStack(playerIn);
	}
}
