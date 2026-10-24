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
		refreshHandlerSizes();
	}

	/**
	 * Item handlers used by the pattern buffer can change their slot count after this list is created. Rebuild the
	 * cached prefix before each operation so the aggregate always reflects the current handler sizes.
	 */
	private void refreshHandlerSizes() {
		int total = 0;
		int index = 0;
		for (IItemHandler handler : handlers) {
			total += handler.getSlots();
			if (index < prefix.size()) {
				prefix.set(index, total);
			} else {
				prefix.add(total);
			}
			index++;
		}
		while (prefix.size() > handlers.size()) {
			prefix.remove(prefix.size() - 1);
		}
	}

	protected HandlerEntry getHandlerByGlobalIndex(int index) {
		refreshHandlerSizes();
		if (index < 0 || index >= getSlots()) return new HandlerEntry(null, 0);

		int handlerIndex = upperBound(prefix, index);

		if (handlerIndex >= handlers.size()) return new HandlerEntry(null, 0);

		return new HandlerEntry(handlers.get(handlerIndex), handlerIndex == 0 ? 0 : prefix.get(handlerIndex - 1));
	}

	@Override
	public int getSlots() {
		refreshHandlerSizes();
		return prefix.isEmpty() ? 0 : prefix.get(prefix.size() - 1);
	}

	@Override
	public ItemStack getStackInSlot(int slot) {
		HandlerEntry entry = getHandlerByGlobalIndex(slot);

		return entry.handler == null ? ItemStack.EMPTY : entry.handler.getStackInSlot(slot - entry.index);
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
		} else if (entry.handler != null) {
			int localSlot = slot - entry.index;
			entry.handler.extractItem(localSlot, Integer.MAX_VALUE, false);
			entry.handler.insertItem(localSlot, stack, false);
		}
	}

	@Desugar
	public record HandlerEntry(IItemHandler handler, int index) {}
}
