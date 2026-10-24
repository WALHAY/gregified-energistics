package com.walhay.gregifiedenergistics.api;

import static org.junit.jupiter.api.Assertions.*;

import com.walhay.gregifiedenergistics.api.capability.InfiniteItemStackHandler;
import net.minecraft.init.Bootstrap;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraftforge.items.ItemStackHandler;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class InfiniteItemStackHandlerTest {
	@BeforeAll
	static void bootstrap() {
		Bootstrap.register();
	}

	@Test
	void persistsFullCountsAndItemTags() {
		for (int count : new int[] {1, 64, 127, 128, 256, 40_000, Integer.MAX_VALUE}) {
			InfiniteItemStackHandler inventory = new InfiniteItemStackHandler(2);
			ItemStack original = new ItemStack(Items.DIAMOND, count);
			original.setStackDisplayName("Buffered diamond");
			inventory.setStackInSlot(1, original);
			InfiniteItemStackHandler restored = new InfiniteItemStackHandler(0);
			restored.deserializeNBT(inventory.serializeNBT());
			assertEquals(2, restored.getSlots());
			assertTrue(restored.getStackInSlot(0).isEmpty());
			assertTrue(ItemStack.areItemStacksEqual(original, restored.getStackInSlot(1)));
		}
	}

	@Test
	void readsLegacyVanillaInventories() {
		ItemStackHandler legacy = new ItemStackHandler(1);
		legacy.setStackInSlot(0, new ItemStack(Items.EMERALD, 64));
		InfiniteItemStackHandler restored = new InfiniteItemStackHandler(0);
		restored.deserializeNBT(legacy.serializeNBT());
		assertEquals(64, restored.getStackInSlot(0).getCount());
	}

	@Test
	void simulationDoesNotExposeTheStoredStack() {
		InfiniteItemStackHandler inventory = new InfiniteItemStackHandler(1);
		inventory.setStackInSlot(0, new ItemStack(Items.DIAMOND, 1024));
		ItemStack simulated = inventory.extractItem(0, Integer.MAX_VALUE, true);
		simulated.shrink(1000);
		assertEquals(1024, inventory.getStackInSlot(0).getCount());
		assertTrue(inventory.extractItem(0, -1, false).isEmpty());
		assertEquals(1024, inventory.getStackInSlot(0).getCount());
	}

	@Test
	void insertionStopsAtTheIntegerLimitWithoutOverflow() {
		InfiniteItemStackHandler inventory = new InfiniteItemStackHandler(1);
		inventory.setStackInSlot(0, new ItemStack(Items.DIAMOND, Integer.MAX_VALUE - 1));
		ItemStack remainder = inventory.insertItem(0, new ItemStack(Items.DIAMOND, 64), false);
		assertEquals(Integer.MAX_VALUE, inventory.getStackInSlot(0).getCount());
		assertEquals(63, remainder.getCount());
	}
}
