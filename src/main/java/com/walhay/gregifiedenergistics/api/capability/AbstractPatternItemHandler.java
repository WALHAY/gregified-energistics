package com.walhay.gregifiedenergistics.api.capability;

import appeng.api.networking.crafting.ICraftingPatternDetails;
import appeng.items.misc.ItemEncodedPattern;
import com.google.common.base.Objects;
import gregtech.api.items.itemhandlers.GTItemStackHandler;
import gregtech.api.metatileentity.MetaTileEntity;
import java.util.Arrays;
import java.util.Collection;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import net.minecraft.item.ItemStack;

public abstract class AbstractPatternItemHandler extends GTItemStackHandler {

	private final ICraftingPatternDetails[] patterns;

	public AbstractPatternItemHandler(MetaTileEntity mte, int size) {
		super(mte, size);
		this.patterns = new ICraftingPatternDetails[size];
	}

	@Override
	public void onContentsChanged(int slot) {
		super.onContentsChanged(slot);
		ItemStack stack = getStackInSlot(slot);
		ICraftingPatternDetails pattern = patterns[slot];

		if (stack.isEmpty()) {
			if (pattern != null) {
				patterns[slot] = null;
				onPatternUpdate();
			}
			return;
		}

		if (pattern != null && ItemStack.areItemStacksEqual(pattern.getPattern(), stack)) return;

		ICraftingPatternDetails newPattern = getPatternFromStack(stack);

		if (Objects.equal(pattern, newPattern)) return;

		patterns[slot] = newPattern;
		onPatternUpdate();
	}

	@Nullable public ICraftingPatternDetails getPatternDetails(int slot) {
		validateSlotIndex(slot);
		return patterns[slot];
	}

	public Collection<ICraftingPatternDetails> getPatterns() {
		return Arrays.asList(patterns);
	}

	@Override
	public boolean isItemValid(int slot, @Nonnull ItemStack stack) {
		return stack.getItem() instanceof ItemEncodedPattern;
	}

	@Override
	protected int getStackLimit(int slot, @Nonnull ItemStack stack) {
		return 1;
	}

	protected abstract ICraftingPatternDetails getPatternFromStack(ItemStack stack);

	protected void onPatternUpdate() {}

	@Override
	protected void onLoad() {
		super.onLoad();
		for (int i = 0; i < getSlots(); ++i) {
			patterns[i] = getPatternFromStack(getStackInSlot(i));
		}
		onPatternUpdate();
	}
}
