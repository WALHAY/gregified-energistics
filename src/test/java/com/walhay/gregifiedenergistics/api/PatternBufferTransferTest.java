package com.walhay.gregifiedenergistics.api;

import static org.junit.jupiter.api.Assertions.*;

import com.walhay.gregifiedenergistics.api.capability.InfiniteItemStackHandler;
import com.walhay.gregifiedenergistics.api.util.PatternBufferTransfer;
import gregtech.api.capability.impl.FluidTankList;
import java.util.Arrays;
import java.util.Collections;
import net.minecraft.init.Bootstrap;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidTank;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class PatternBufferTransferTest {
	@BeforeAll
	static void bootstrap() {
		Bootstrap.register();
	}

	@Test
	void rejectsTheWholeRecipeWhenDifferentItemsCompeteForTheLastSlot() {
		InfiniteItemStackHandler items = new InfiniteItemStackHandler(1);
		ItemStack diamond = new ItemStack(Items.DIAMOND, 64);
		ItemStack emerald = new ItemStack(Items.EMERALD, 64);
		assertFalse(PatternBufferTransfer.insertInputs(
				items, new FluidTankList(false), Arrays.asList(diamond, emerald), Collections.emptyList()));
		assertTrue(items.getStackInSlot(0).isEmpty());
		assertEquals(64, diamond.getCount());
		assertEquals(64, emerald.getCount());
	}

	@Test
	void cumulativeItemCountsCannotOverflowAndFailurePreservesBothInventories() {
		InfiniteItemStackHandler items = new InfiniteItemStackHandler(1);
		items.setStackInSlot(0, new ItemStack(Items.DIAMOND, Integer.MAX_VALUE - 10));
		FluidTank tank = new FluidTank(1000);
		assertFalse(PatternBufferTransfer.insertInputs(
				items,
				new FluidTankList(false, tank),
				Arrays.asList(new ItemStack(Items.DIAMOND, 6), new ItemStack(Items.DIAMOND, 6)),
				Collections.singletonList(new FluidStack(FluidRegistry.WATER, 1000))));
		assertEquals(Integer.MAX_VALUE - 10, items.getStackInSlot(0).getCount());
		assertNull(tank.getFluid());
	}

	@Test
	void cumulativeFluidInputsCannotOverfillTheTankOrConsumeItems() {
		InfiniteItemStackHandler items = new InfiniteItemStackHandler(1);
		FluidTank tank = new FluidTank(1000);
		assertFalse(PatternBufferTransfer.insertInputs(
				items,
				new FluidTankList(false, tank),
				Collections.singletonList(new ItemStack(Items.DIAMOND, 64)),
				Arrays.asList(new FluidStack(FluidRegistry.WATER, 600), new FluidStack(FluidRegistry.WATER, 600))));
		assertTrue(items.getStackInSlot(0).isEmpty());
		assertNull(tank.getFluid());
	}

	@Test
	void commitsMixedInputsWithoutAliasingTheCraftingInventory() {
		InfiniteItemStackHandler items = new InfiniteItemStackHandler(2);
		FluidTank tank = new FluidTank(2000);
		ItemStack input = new ItemStack(Items.DIAMOND, 40_000);
		assertTrue(PatternBufferTransfer.insertInputs(
				items,
				new FluidTankList(false, tank),
				Collections.singletonList(input),
				Collections.singletonList(new FluidStack(FluidRegistry.WATER, 1000))));
		input.setCount(1);
		assertEquals(40_000, items.getStackInSlot(0).getCount());
		assertEquals(1000, tank.getFluidAmount());
		assertEquals(FluidRegistry.WATER, tank.getFluid().getFluid());
	}
}
