package com.walhay.gregifiedenergistics.api.metatileentity;

import appeng.api.networking.crafting.ICraftingPatternDetails;
import appeng.api.networking.crafting.ICraftingProvider;
import appeng.api.networking.crafting.ICraftingProviderHelper;
import appeng.api.networking.events.MENetworkCraftingPatternChange;
import appeng.api.storage.IStorageChannel;
import appeng.api.storage.data.IAEStack;
import appeng.me.GridAccessException;
import appeng.util.Platform;
import gregtech.common.metatileentities.multi.multiblockpart.appeng.MetaTileEntityAEHostablePart;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.util.ResourceLocation;

public abstract class MetaTileEntityCraftingProvider<T extends IAEStack<T>> extends MetaTileEntityAEHostablePart<T>
		implements ICraftingProviderAccessor {
	public MetaTileEntityCraftingProvider(
			ResourceLocation metaTileEntityId,
			int tier,
			boolean isExportHatch,
			Class<? extends IStorageChannel<T>> storageChannel) {
		super(metaTileEntityId, tier, isExportHatch, storageChannel);
	}

	@Override
	public boolean isBusy() {
		return true;
	}

	@Override
	public boolean pushPattern(ICraftingPatternDetails pattern, InventoryCrafting inventory) {
		return false;
	}

	@Override
	public void provideCrafting(ICraftingProviderHelper provider) {}

	public ICraftingProvider getCraftingProvider() {
		return (ICraftingProvider) getHolder();
	}

	// notify grid network when patterns should be recalculated
	public void notifyPatternChange() {
		if (Platform.isServer() && getProxy() != null) {
			try {
				getProxy()
						.getGrid()
						.postEvent(new MENetworkCraftingPatternChange(
								getCraftingProvider(), getProxy().getNode()));
			} catch (GridAccessException ignored) {
			}
		}
	}
}
