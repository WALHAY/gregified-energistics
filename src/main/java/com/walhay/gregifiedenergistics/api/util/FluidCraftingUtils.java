package com.walhay.gregifiedenergistics.api.util;

import appeng.api.storage.data.IAEItemStack;
import com.glodblock.github.common.item.fake.FakeFluids;
import com.glodblock.github.common.item.fake.FakeItemRegister;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import net.minecraftforge.fluids.FluidStack;

/** FluidCraftingUtils */
public class FluidCraftingUtils {

	public static List<FluidStack> getFluids(IAEItemStack... items) {
		if (items == null) return Collections.emptyList();
		return Arrays.stream(items)
				.filter(Objects::nonNull)
				.filter(stack -> FakeFluids.isFluidFakeItem(stack.asItemStackRepresentation()))
				.map(FakeItemRegister::<FluidStack>getStack)
				.filter(Objects::nonNull)
				.collect(Collectors.toList());
	}
}
