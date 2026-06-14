package com.walhay.gregifiedenergistics.mixins.gtceu.metatileentities;

import com.walhay.gregifiedenergistics.api.capability.SegmentItemHandlerList;
import com.walhay.gregifiedenergistics.mixins.interfaces.IShitController;
import gregtech.api.capability.impl.ItemHandlerList;
import gregtech.api.metatileentity.multiblock.MultiblockAbility;
import gregtech.api.metatileentity.multiblock.RecipeMapMultiblockController;
import net.minecraftforge.items.IItemHandlerModifiable;
import org.spongepowered.asm.mixin.Implements;
import org.spongepowered.asm.mixin.Interface;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** RecipeMapMultiblockControllerMixin */
@Mixin(value = RecipeMapMultiblockController.class, remap = false)
@Implements(@Interface(iface = IShitController.class, prefix = "ishit$"))
public class RecipeMapMultiblockControllerMixin implements IShitController {
	@Shadow
	protected IItemHandlerModifiable inputInventory;

	@Override
	public void updateInputs() {
		var self = (RecipeMapMultiblockController) (Object) this;
		this.inputInventory = new ItemHandlerList(self.getAbilities(MultiblockAbility.IMPORT_ITEMS));
	}

	@Inject(method = "initializeAbilities", at = @At("TAIL"))
	public void createSegmentHandlerList(CallbackInfo ci) {
		var self = (RecipeMapMultiblockController) (Object) this;
		this.inputInventory = new SegmentItemHandlerList(self.getAbilities(MultiblockAbility.IMPORT_ITEMS));
	}
}
