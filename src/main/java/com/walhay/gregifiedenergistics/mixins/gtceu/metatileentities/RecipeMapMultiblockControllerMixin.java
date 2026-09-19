package com.walhay.gregifiedenergistics.mixins.gtceu.metatileentities;

import com.walhay.gregifiedenergistics.api.capability.PatternBufferDualHandler;
import com.walhay.gregifiedenergistics.api.capability.SegmentItemHandlerList;
import gregtech.api.capability.IMultipleTankHandler;
import gregtech.api.capability.impl.ItemHandlerList;
import gregtech.api.metatileentity.multiblock.RecipeMapMultiblockController;
import java.util.Collection;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.IItemHandlerModifiable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** RecipeMapMultiblockControllerMixin */
@Mixin(value = RecipeMapMultiblockController.class, remap = false)
public abstract class RecipeMapMultiblockControllerMixin {
	@Shadow
	protected IItemHandlerModifiable inputInventory;

	@Shadow
	protected IMultipleTankHandler extendedFluidInputs;

	@Shadow
	protected IMultipleTankHandler inputFluidInventory;

	@Shadow
	protected abstract IMultipleTankHandler extendedImportFluidList(IMultipleTankHandler fluids);

	@Inject(method = "initializeAbilities", at = @At("TAIL"))
	public void createSegmentHandlerList(CallbackInfo ci) {
		if (this.inputInventory instanceof ItemHandlerList list) {
			Collection<IItemHandler> handlers = list.getBackingHandlers();

			boolean found = handlers.stream().anyMatch(handler -> handler instanceof PatternBufferDualHandler);
			if (found) {
				this.inputInventory = new SegmentItemHandlerList(handlers);
			}
		}
	}

	@Inject(method = "getInputFluidInventory", at = @At("HEAD"))
	private void refreshPatternBufferFluids(CallbackInfoReturnable<IMultipleTankHandler> cir) {
		// Pattern-buffer tanks are created when patterns change, after the controller initially forms.
		if (this.inputInventory instanceof SegmentItemHandlerList) {
			this.extendedFluidInputs = this.extendedImportFluidList(this.inputFluidInventory);
		}
	}
}
