package com.walhay.gregifiedenergistics.api;

import static org.junit.jupiter.api.Assertions.*;

import com.walhay.gregifiedenergistics.api.capability.InfiniteItemStackHandler;
import com.walhay.gregifiedenergistics.api.capability.PatternBufferDualHandler;
import com.walhay.gregifiedenergistics.api.util.PatternBufferFluidInputs;
import gregtech.api.capability.IMultipleTankHandler;
import gregtech.api.capability.impl.FluidTankList;
import gregtech.api.capability.impl.GhostCircuitItemStackHandler;
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

class PatternBufferDualHandlerTest {
	@BeforeAll
	static void bootstrap() {
		Bootstrap.register();
	}

	@Test
	void resizingItemsKeepsTheCircuitSlotAfterTheCurrentItemSlots() {
		InfiniteItemStackHandler items = new InfiniteItemStackHandler(0);
		GhostCircuitItemStackHandler circuit = new GhostCircuitItemStackHandler(null);
		PatternBufferDualHandler buffer = new PatternBufferDualHandler(items, circuit, new FluidTankList(false));
		assertEquals(1, buffer.getSlots());
		items.setSize(3);
		items.setStackInSlot(2, new ItemStack(Items.DIAMOND, 40_000));
		buffer.onResize();
		assertEquals(4, buffer.getSlots());
		assertEquals(40_000, buffer.getStackInSlot(2).getCount());
		assertSame(circuit.getStackInSlot(0), buffer.getStackInSlot(3));
		assertTrue(buffer.getStackInSlot(4).isEmpty());
	}

	@Test
	void replacingFluidTanksMakesNewContentsVisibleToTheController() {
		PatternBufferDualHandler buffer = bufferWithWater(1_000);
		FluidTankList replacement = new FluidTankList(
				false, new FluidTank(new FluidStack(FluidRegistry.LAVA, 2_000), 4_000), new FluidTank(4_000));
		buffer.setFluidDelegate(replacement);
		IMultipleTankHandler inputs =
				PatternBufferFluidInputs.combine(new FluidTankList(true), Collections.singletonList(buffer));
		assertEquals(2, inputs.getTanks());
		assertEquals(2, buffer.getTankProperties().length);
		assertNull(inputs.drain(new FluidStack(FluidRegistry.WATER, 1_000), false));
		assertEquals(2_000, inputs.drain(new FluidStack(FluidRegistry.LAVA, 2_000), true).amount);
		assertNull(replacement.getTankAt(0).getFluid());
	}

	@Test
	void distinctInputsConsumeOnlyTheSelectedContainersFluids() {
		PatternBufferDualHandler first = bufferWithWater(1_000);
		PatternBufferDualHandler second = bufferWithWater(2_000);
		FluidTank hatch = new FluidTank(new FluidStack(FluidRegistry.LAVA, 500), 4_000);
		FluidTankList hatches = new FluidTankList(true, hatch);
		IMultipleTankHandler selected = PatternBufferFluidInputs.combine(hatches, Collections.singletonList(first));
		assertEquals(2, selected.getTanks());
		assertEquals(1_000, selected.drain(new FluidStack(FluidRegistry.WATER, 3_000), true).amount);
		assertEquals(2_000, second.getTankAt(0).getFluidAmount());
		assertEquals(500, selected.drain(new FluidStack(FluidRegistry.LAVA, 500), true).amount);
		assertEquals(0, hatch.getFluidAmount());
		IMultipleTankHandler combined = PatternBufferFluidInputs.combine(hatches, Arrays.asList(first, second));
		assertEquals(2_000, combined.drain(new FluidStack(FluidRegistry.WATER, 3_000), true).amount);
	}

	@Test
	void simulatedTransfersDoNotNotifyButActualItemAndFluidChangesDo() {
		int[] changes = {0};
		PatternBufferDualHandler buffer =
				new PatternBufferDualHandler(
						new InfiniteItemStackHandler(1),
						new GhostCircuitItemStackHandler(null),
						new FluidTankList(false, new FluidTank(4_000))) {
					@Override
					public void onContentsChanged() {
						changes[0]++;
					}
				};
		buffer.insertItem(0, new ItemStack(Items.DIAMOND, 100), true);
		buffer.fill(new FluidStack(FluidRegistry.WATER, 1_000), false);
		assertEquals(0, changes[0]);
		buffer.insertItem(0, new ItemStack(Items.DIAMOND, 100), false);
		buffer.fill(new FluidStack(FluidRegistry.WATER, 1_000), true);
		assertEquals(2, changes[0]);
		buffer.extractItem(0, 10, true);
		buffer.drain(100, false);
		assertEquals(2, changes[0]);
		buffer.extractItem(0, 10, false);
		buffer.drain(100, true);
		assertEquals(4, changes[0]);
	}

	private static PatternBufferDualHandler bufferWithWater(int amount) {
		return new PatternBufferDualHandler(
				new InfiniteItemStackHandler(0),
				new GhostCircuitItemStackHandler(null),
				new FluidTankList(false, new FluidTank(new FluidStack(FluidRegistry.WATER, amount), 4_000)));
	}
}
