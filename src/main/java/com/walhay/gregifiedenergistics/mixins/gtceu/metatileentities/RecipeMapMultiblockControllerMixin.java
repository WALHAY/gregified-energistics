package com.walhay.gregifiedenergistics.mixins.gtceu.metatileentities;

import com.walhay.gregifiedenergistics.api.capability.PatternBufferDualHandler;
import com.walhay.gregifiedenergistics.api.capability.SegmentItemHandlerList;
import com.walhay.gregifiedenergistics.api.util.PatternBufferFluidInputs;
import gregtech.api.capability.IMultipleTankHandler;
import gregtech.api.capability.impl.FluidTankList;
import gregtech.api.metatileentity.multiblock.MultiblockAbility;
import gregtech.api.metatileentity.multiblock.RecipeMapMultiblockController;
import java.util.ArrayList;
import java.util.List;
import net.minecraftforge.items.IItemHandlerModifiable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Keeps dynamic pattern inputs visible to the GTCEu 2.8.10 controller. */
@Mixin(value = RecipeMapMultiblockController.class, remap = false)
public abstract class RecipeMapMultiblockControllerMixin {
	@Shadow
	protected IItemHandlerModifiable inputInventory;

	@Shadow
	protected IMultipleTankHandler inputFluidInventory;

	@Shadow
	protected abstract boolean allowSameFluidFillForOutputs();

	@Inject(method = "initializeAbilities", at = @At("TAIL"))
	private void createSegmentHandlerList(CallbackInfo ci) {
		var controller = (RecipeMapMultiblockController) (Object) this;
		List<IItemHandlerModifiable> handlers = controller.getAbilities(MultiblockAbility.IMPORT_ITEMS);
		if (handlers.stream().anyMatch(handler -> handler instanceof PatternBufferDualHandler)) {
			this.inputInventory = new SegmentItemHandlerList(new ArrayList<>(handlers));
		}
	}

	@Inject(method = "getInputFluidInventory", at = @At("HEAD"))
	private void refreshPatternBufferFluids(CallbackInfoReturnable<IMultipleTankHandler> cir) {
		if (this.inputInventory instanceof SegmentItemHandlerList) {
			var controller = (RecipeMapMultiblockController) (Object) this;
			IMultipleTankHandler fluids = new FluidTankList(
					allowSameFluidFillForOutputs(), controller.getAbilities(MultiblockAbility.IMPORT_FLUIDS));
			this.inputFluidInventory = controller.isDistinct()
					? fluids
					: PatternBufferFluidInputs.combine(fluids, controller.getAbilities(MultiblockAbility.IMPORT_ITEMS));
		}
	}
}
