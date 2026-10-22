package com.walhay.gregifiedenergistics.mixins.gtceu.metatileentities;

import com.walhay.gregifiedenergistics.api.util.PatternBufferFluidInputs;
import gregtech.api.capability.IMultipleTankHandler;
import gregtech.api.capability.impl.AbstractRecipeLogic;
import gregtech.api.capability.impl.MultiblockRecipeLogic;
import gregtech.api.metatileentity.MetaTileEntity;
import gregtech.api.recipes.Recipe;
import gregtech.api.recipes.RecipeMap;
import java.util.Collections;
import net.minecraftforge.items.IItemHandlerModifiable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** Uses only the selected pattern container's fluids when the controller is in distinct mode. */
@Mixin(value = MultiblockRecipeLogic.class, remap = false)
public abstract class MultiblockRecipeLogicMixin extends AbstractRecipeLogic {
	private MultiblockRecipeLogicMixin(MetaTileEntity tileEntity, RecipeMap<?> recipeMap) {
		super(tileEntity, recipeMap);
	}

	@Shadow
	protected IItemHandlerModifiable currentDistinctInputBus;

	@Shadow
	protected abstract IMultipleTankHandler getInputTank();

	@Redirect(
			method = "trySearchNewRecipeDistinct",
			at =
					@At(
							value = "INVOKE",
							target =
									"Lgregtech/api/capability/impl/MultiblockRecipeLogic;findRecipe(JLnet/minecraftforge/items/IItemHandlerModifiable;Lgregtech/api/capability/IMultipleTankHandler;)Lgregtech/api/recipes/Recipe;"))
	private Recipe findPatternRecipe(
			MultiblockRecipeLogic logic, long maxVoltage, IItemHandlerModifiable items, IMultipleTankHandler fluids) {
		return findRecipe(
				maxVoltage, items, PatternBufferFluidInputs.combine(fluids, Collections.singletonList(items)));
	}

	@Redirect(
			method = "checkPreviousRecipeDistinct",
			at =
					@At(
							value = "INVOKE",
							target =
									"Lgregtech/api/capability/impl/MultiblockRecipeLogic;getInputTank()Lgregtech/api/capability/IMultipleTankHandler;"))
	private IMultipleTankHandler getPreviousPatternFluids(MultiblockRecipeLogic logic, IItemHandlerModifiable items) {
		return PatternBufferFluidInputs.combine(getInputTank(), Collections.singletonList(items));
	}

	@Redirect(
			method = "prepareRecipeDistinct",
			at =
					@At(
							value = "INVOKE",
							target =
									"Lgregtech/api/capability/impl/MultiblockRecipeLogic;getInputTank()Lgregtech/api/capability/IMultipleTankHandler;"))
	private IMultipleTankHandler getSelectedPatternFluids(MultiblockRecipeLogic logic) {
		return PatternBufferFluidInputs.combine(getInputTank(), Collections.singletonList(currentDistinctInputBus));
	}

	@Redirect(
			method = "prepareRecipeDistinct",
			at =
					@At(
							value = "INVOKE",
							target =
									"Lgregtech/api/capability/impl/MultiblockRecipeLogic;setupAndConsumeRecipeInputs(Lgregtech/api/recipes/Recipe;Lnet/minecraftforge/items/IItemHandlerModifiable;)Z"))
	private boolean consumePatternInputs(MultiblockRecipeLogic logic, Recipe recipe, IItemHandlerModifiable items) {
		return setupAndConsumeRecipeInputs(
				recipe, items, PatternBufferFluidInputs.combine(getInputTank(), Collections.singletonList(items)));
	}
}
