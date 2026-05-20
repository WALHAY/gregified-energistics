package com.walhay.gregifiedenergistics.api.util;

import appeng.api.storage.data.IAEItemStack;
import com.glodblock.github.common.item.fake.FakeFluids;
import com.glodblock.github.common.item.fake.FakeItemRegister;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import net.minecraftforge.fluids.FluidStack;

/** FluidCraftingUtils */
public class FluidCraftingUtils {

	public static List<FluidStack> getFluids(IAEItemStack... items) {
		return Arrays.stream(items)
				.map(IAEItemStack::asItemStackRepresentation)
				.filter(FakeFluids::isFluidFakeItem)
				.map(FakeItemRegister::<FluidStack>getStack)
				.collect(Collectors.toList());
	}
}
