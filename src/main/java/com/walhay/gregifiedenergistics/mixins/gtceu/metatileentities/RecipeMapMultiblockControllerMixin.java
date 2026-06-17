package com.walhay.gregifiedenergistics.mixins.gtceu.metatileentities;

import com.walhay.gregifiedenergistics.api.capability.PatternBufferDualHandler;
import com.walhay.gregifiedenergistics.api.capability.SegmentItemHandlerList;
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

/** RecipeMapMultiblockControllerMixin */
@Mixin(value = RecipeMapMultiblockController.class, remap = false)
public class RecipeMapMultiblockControllerMixin {
	@Shadow
	protected IItemHandlerModifiable inputInventory;

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
}
