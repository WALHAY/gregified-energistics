package com.walhay.gregifiedenergistics.api;

import static org.junit.jupiter.api.Assertions.*;

import com.walhay.gregifiedenergistics.api.capability.InfiniteItemStackHandler;
import com.walhay.gregifiedenergistics.api.util.PatternBufferTransfer;
import gregtech.api.capability.impl.FluidTankList;
import net.minecraft.init.Bootstrap;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidTank;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class PatternBufferPersistenceTest {
	@BeforeAll
	static void bootstrap() {
		Bootstrap.register();
	}

	@Test
	void removingAPatternPreservesUnrefundedInputs() {
		InfiniteItemStackHandler items = new InfiniteItemStackHandler(3);
		items.setStackInSlot(2, new ItemStack(Items.DIAMOND, 40_000));
		FluidTankList fluids =
				new FluidTankList(false, new FluidTank(new FluidStack(FluidRegistry.WATER, 12_000), Integer.MAX_VALUE));
		PatternBufferTransfer.resizeItems(items, 0);
		fluids = PatternBufferTransfer.resizeFluids(fluids, 0, () -> {});
		assertEquals(3, items.getSlots());
		assertEquals(40_000, items.getStackInSlot(2).getCount());
		assertEquals(1, fluids.getTanks());
		assertEquals(12_000, fluids.getTankAt(0).getFluidAmount());
	}

	@Test
	void resizingForANewPatternAndReloadingPreservesItemsAndFluids() {
		InfiniteItemStackHandler items = new InfiniteItemStackHandler(1);
		items.setStackInSlot(0, new ItemStack(Items.DIAMOND, 40_000));
		FluidTankList fluids =
				new FluidTankList(false, new FluidTank(new FluidStack(FluidRegistry.WATER, 12_000), Integer.MAX_VALUE));
		PatternBufferTransfer.resizeItems(items, 4);
		fluids = PatternBufferTransfer.resizeFluids(fluids, 2, () -> {});
		assertEquals(40_000, items.getStackInSlot(0).getCount());
		assertTrue(items.getStackInSlot(3).isEmpty());
		assertEquals(12_000, fluids.getTankAt(0).getFluidAmount());
		NBTTagCompound saved = fluids.serializeNBT();
		FluidTankList restored = PatternBufferTransfer.resizeFluids(
				new FluidTankList(false), saved.getTagList("Tanks", 10).tagCount(), () -> {});
		restored.deserializeNBT(saved);
		assertEquals(2, restored.getTanks());
		assertEquals(12_000, restored.getTankAt(0).getFluidAmount());
		assertNull(restored.getTankAt(1).getFluid());
	}
}
