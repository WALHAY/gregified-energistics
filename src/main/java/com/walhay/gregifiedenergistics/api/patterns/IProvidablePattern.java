package com.walhay.gregifiedenergistics.api.patterns;

import appeng.api.networking.crafting.ICraftingMedium;
import appeng.api.networking.crafting.ICraftingPatternDetails;
import appeng.api.networking.crafting.ICraftingProviderHelper;
import java.util.Collection;
import java.util.Collections;

public interface IProvidablePattern extends ICraftingPatternDetails {

	void providePatterns(ICraftingMedium medium, ICraftingProviderHelper helper);

	default Collection<IProvidablePattern> getProvidedPatterns() {
		return Collections.singleton(this);
	}
}
