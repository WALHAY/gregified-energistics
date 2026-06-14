package com.walhay.gregifiedenergistics.api;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.walhay.gregifiedenergistics.api.capability.SegmentItemHandlerList;
import java.util.LinkedList;
import java.util.Random;
import net.minecraft.init.Bootstrap;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fml.common.registry.ForgeRegistries;
import net.minecraftforge.items.IItemHandlerModifiable;
import net.minecraftforge.items.ItemStackHandler;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class SegmentItemHandlerListTest {

	private SegmentItemHandlerList handlerList;
	private LinkedList<ItemStackHandler> handlers;
	private static Random random = new Random(42);

	@BeforeAll
	public static void bootstrap() {
		Bootstrap.register();
	}

	@BeforeEach
	public void prepareTest() {
		this.handlers = new LinkedList<>();

		ItemStackHandler h1 = new ItemStackHandler(5);
		ItemStackHandler h2 = new ItemStackHandler(9);
		ItemStackHandler h3 = new ItemStackHandler(63);

		this.handlers.add(h1);
		this.handlers.add(h2);
		this.handlers.add(h3);

		fillWithRandomItems(h1, h2, h3);

		this.handlerList = new SegmentItemHandlerList(h1, h2, h3);
	}

	private void fillWithRandomItems(IItemHandlerModifiable... handlers) {
		Item[] items = ForgeRegistries.ITEMS.getValuesCollection().toArray(new Item[0]);

		for (IItemHandlerModifiable handler : handlers) {
			for (int i = 0; i < handler.getSlots(); ++i) {
				Item randomItem = items[random.nextInt(items.length)];
				int randomSize = 1 + random.nextInt(handler.getSlotLimit(i));

				handler.setStackInSlot(i, new ItemStack(randomItem, randomSize));
			}
		}
	}

	private void checkContents() {
		int totalSlots = this.handlers.stream()
				.mapToInt(IItemHandlerModifiable::getSlots)
				.sum();

		assertEquals(totalSlots, this.handlerList.getSlots());

		int currentIndex = 0;
		int currentHandler = 0;
		for (int i = 0; i < this.handlerList.getSlots(); ++i) {
			if (currentIndex >= this.handlers.get(currentHandler).getSlots()) {
				currentIndex = 0;
				currentHandler++;
			}

			ItemStack expected = this.handlers.get(currentHandler).getStackInSlot(currentIndex++);

			ItemStack actual = this.handlerList.getStackInSlot(i);

			assertEquals(expected, actual);
		}
	}

	private void performHandlerResize(int handlerIndex, int diff) {
		ItemStackHandler changeHandler = this.handlers.get(handlerIndex);

		int currentSize = changeHandler.getSlots();

		int newSize = Math.max(0, currentSize + diff);
		changeHandler.setSize(newSize);
		if (newSize == 0) handlers.remove(handlerIndex);
		this.handlerList.onHandlerChange(changeHandler);

		checkContents();
	}

	@Test
	public void handlerStaticTest() {
		checkContents();
	}

	@Test
	public void handlerDynamicTest() {
		ItemStackHandler handler = new ItemStackHandler(16);

		fillWithRandomItems(handler);

		this.handlers.add(handler);
		this.handlerList.addHandler(handler);

		checkContents();
	}

	@Test
	public void startHandlerResizeTest() {
		performHandlerResize(0, 10);
		performHandlerResize(0, -10);
		performHandlerResize(0, 15);
		performHandlerResize(0, 25);
		performHandlerResize(0, -40);
	}

	@Test
	public void midHandlerResizeTest() {
		performHandlerResize(2, 10);
		performHandlerResize(2, -10);
		performHandlerResize(2, 15);
		performHandlerResize(2, 25);
		performHandlerResize(2, -40);
	}

	@Test
	public void randomHandlerResizeTest() {
		performHandlerResize(1, 10);
		performHandlerResize(2, -3);
		performHandlerResize(2, 15);
		performHandlerResize(2, 23);
		performHandlerResize(0, 40);
		performHandlerResize(0, -17);
		performHandlerResize(1, -40);
	}

	@Test
	public void zeroResizeTest() {
		performHandlerResize(2, -handlers.get(2).getSlots());
		performHandlerResize(0, -handlers.get(0).getSlots());
		performHandlerResize(0, -handlers.get(0).getSlots());
	}
}
