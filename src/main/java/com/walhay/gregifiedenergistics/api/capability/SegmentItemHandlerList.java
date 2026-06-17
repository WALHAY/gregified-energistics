package com.walhay.gregifiedenergistics.api.capability;

import com.github.bsideup.jabel.Desugar;
import gregtech.api.capability.impl.ItemHandlerList;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.Objects;
import net.minecraft.item.ItemStack;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.IItemHandlerModifiable;

/**
 * SegmentItemHandlerList
 *
 * <p>Main idea of that class is to provide mutability of handler list unlike immutable {@link ItemHandlerList} which
 * caches handlers on construction.
 */
public class SegmentItemHandlerList implements IItemHandlerModifiable {

	private final LinkedList<IItemHandler> handlers = new LinkedList<>();
	private final IntList prefix = new IntArrayList();

	public SegmentItemHandlerList(IItemHandler... handlers) {
		for (IItemHandler handler : handlers) {
			if (handler == null) continue;

			addHandler(handler);
		}
	}

	public SegmentItemHandlerList(Collection<IItemHandler> handlers) {
		handlers.stream().filter(Objects::nonNull).forEach(this::addHandler);
	}

	private static int upperBound(IntList prefix, int x) {
		int lo = 0, hi = prefix.size();
		while (lo < hi) {
			int mid = (lo + hi) >>> 1;
			if (prefix.get(mid) <= x) {
				lo = mid + 1;
			} else {
				hi = mid;
			}
		}
		return lo;
	}

	public void addHandler(IItemHandler handler) {
		if (handlers.contains(handler)) return;

		handlers.add(handler);
		prefix.add(handler.getSlots() + (prefix.size() == 0 ? 0 : prefix.get(prefix.size() - 1)));
	}

	public void onHandlerChange(IItemHandlerModifiable handler) {
		int index = handlers.indexOf(handler);

		if (index == -1) return;

		int oldSize = prefix.get(index) - (index == 0 ? 0 : prefix.get(index - 1));
		int diff = handler.getSlots() - oldSize;
		if (diff == 0) return;

		for (int i = index; i < prefix.size(); ++i) {
			prefix.set(i, prefix.get(i) + diff);
		}

		if (handler.getSlots() == 0) {
			handlers.remove(index);
			prefix.remove(index);
		}
	}

	protected HandlerEntry getHandlerByGlobalIndex(int index) {
		int handlerIndex = upperBound(prefix, index);

		if (handlerIndex == -1) return new HandlerEntry(null, 0);

		return new HandlerEntry(handlers.get(handlerIndex), handlerIndex == 0 ? 0 : prefix.get(handlerIndex - 1));
	}

	@Override
	public int getSlots() {
		return prefix.isEmpty() ? 0 : prefix.get(prefix.size() - 1);
	}

	@Override
	public ItemStack getStackInSlot(int slot) {
		HandlerEntry entry = getHandlerByGlobalIndex(slot);

		return entry.handler == null ? null : entry.handler.getStackInSlot(slot - entry.index);
	}

	@Override
	public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
		HandlerEntry entry = getHandlerByGlobalIndex(slot);

		return entry.handler == null ? stack : entry.handler.insertItem(slot - entry.index, stack, simulate);
	}

	@Override
	public ItemStack extractItem(int slot, int amount, boolean simulate) {
		HandlerEntry entry = getHandlerByGlobalIndex(slot);

		return entry.handler == null
				? ItemStack.EMPTY
				: entry.handler.extractItem(slot - entry.index, amount, simulate);
	}

	@Override
	public int getSlotLimit(int slot) {
		HandlerEntry entry = getHandlerByGlobalIndex(slot);

		return entry.handler == null ? 0 : entry.handler.getSlotLimit(slot - entry.index);
	}

	@Override
	public void setStackInSlot(int slot, ItemStack stack) {
		HandlerEntry entry = getHandlerByGlobalIndex(slot);

		if (entry.handler != null && entry.handler instanceof IItemHandlerModifiable modifiable) {
			modifiable.setStackInSlot(slot - entry.index, stack);
		} else {
			entry.handler.extractItem(slot, Integer.MAX_VALUE, false);
			entry.handler.insertItem(slot, stack, false);
		}
	}

	@Desugar
	public record HandlerEntry(IItemHandler handler, int index) {}
}
