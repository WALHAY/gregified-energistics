package com.walhay.gregifiedenergistics.common.mui;

import appeng.api.networking.crafting.ICraftingPatternDetails;
import appeng.items.misc.ItemEncodedPattern;
import com.cleanroommc.modularui.api.drawable.IDrawable;
import com.cleanroommc.modularui.drawable.ItemDrawable;
import com.cleanroommc.modularui.screen.viewport.ModularGuiContext;
import com.cleanroommc.modularui.theme.WidgetThemeEntry;
import com.cleanroommc.modularui.widgets.slot.ItemSlot;
import com.google.common.collect.Lists;
import gregtech.api.recipes.RecipeMaps;
import gregtech.api.recipes.machines.IResearchRecipeMap;
import gregtech.api.util.AssemblyLineManager;
import net.minecraft.item.ItemStack;

/** DataStickSlot */
public class DataStickSlot extends ItemSlot {

	private ItemDrawable patternOverlay;
	private int tick = -1;

	public DataStickSlot() {
		super();
		this.patternOverlay = new ItemDrawable();
		overlay(this.patternOverlay.asIcon().size(16).margin(1));
		onUpdateListener(w -> this.patternOverlay.setItem(getOutputItem()));
	}

	@Override
	public void draw(ModularGuiContext context, WidgetThemeEntry<?> widgetTheme) {
		if (isBelowMouse()) {
			overlay(IDrawable.NONE);
			super.draw(context, widgetTheme);
		} else {
			overlay(this.patternOverlay.asIcon().size(16).margin(1));
		}
	}

	@Override
	public void onUpdate() {
		super.onUpdate();
		this.tick++;
	}

	private int getTicks() {
		return this.tick;
	}

	private ItemStack getOutputItem() {
		var stack = getSlot().getStack();

		if (stack == null || stack.isEmpty()) {
			return ItemStack.EMPTY;
		}

		int select = getTicks() / 20;
		if (stack.getItem() instanceof ItemEncodedPattern encodedPattern) {
			ICraftingPatternDetails pattern = encodedPattern.getPatternForItem(stack, null);

			var outputs = pattern.getCondensedOutputs();

			if (outputs.length <= 0) {
				return ItemStack.EMPTY;
			}

			return outputs[select % outputs.length].createItemStack();
		} else if (AssemblyLineManager.isStackDataItem(stack, true)) {
			if (!AssemblyLineManager.hasResearchTag(stack)) {
				return ItemStack.EMPTY;
			}

			var reseachId = AssemblyLineManager.readResearchId(stack);

			var recipeMap = (IResearchRecipeMap) RecipeMaps.ASSEMBLY_LINE_RECIPES;

			if (recipeMap != null) {
				var recipes = recipeMap.getDataStickEntry(reseachId);

				int recipeIndex = select / recipes.size();
				var recipesArr = Lists.newArrayList(recipes);

				var currentRecipe = recipesArr.get(recipeIndex % recipesArr.size());
				var outputs = currentRecipe.getOutputs();

				return outputs.get(select % outputs.size());
			}
		}

		return ItemStack.EMPTY;
	}
}
