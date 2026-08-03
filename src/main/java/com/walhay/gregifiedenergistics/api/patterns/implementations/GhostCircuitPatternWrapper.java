package com.walhay.gregifiedenergistics.api.patterns.implementations;

import appeng.api.networking.crafting.ICraftingMedium;
import appeng.api.networking.crafting.ICraftingPatternDetails;
import appeng.api.networking.crafting.ICraftingProviderHelper;
import appeng.api.storage.data.IAEItemStack;
import com.walhay.gregifiedenergistics.api.patterns.AbstractPatternHelper;
import com.walhay.gregifiedenergistics.api.patterns.IProvidablePattern;
import gregtech.api.recipes.ingredients.IntCircuitIngredient;
import java.util.Arrays;
import java.util.Collection;
import java.util.Optional;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
import org.jetbrains.annotations.NotNull;

/** GhostCircuitPatternHelper */
public class GhostCircuitPatternWrapper extends AbstractPatternHelper {

	@NotNull private final ICraftingPatternDetails pattern;

	private final int option;

	private GhostCircuitPatternWrapper(@NotNull ICraftingPatternDetails pattern) {
		this.pattern = pattern;
		Optional<ItemStack> stack = Arrays.stream(pattern.getInputs())
				.map(IAEItemStack::getDefinition)
				.filter(IntCircuitIngredient::isIntegratedCircuit)
				.findFirst();
		this.option = stack.isPresent() ? IntCircuitIngredient.getCircuitConfiguration(stack.get()) : -1;
	}

	public static ICraftingPatternDetails wrap(ICraftingPatternDetails pattern) {
		if (pattern != null && hasCircuit(pattern.getInputs())) {
			return new GhostCircuitPatternWrapper(pattern);
		}
		return pattern;
	}

	private static boolean hasCircuit(IAEItemStack[] inputs) {
		if (inputs == null || inputs.length == 0) return false;

		return Arrays.stream(inputs)
				.map(IAEItemStack::getDefinition)
				.filter(IntCircuitIngredient::isIntegratedCircuit)
				.findFirst()
				.isPresent();
	}

	protected static IAEItemStack[] filterCircuits(IAEItemStack[] inputs) {
		return Arrays.stream(inputs)
				.filter(stack -> !IntCircuitIngredient.isIntegratedCircuit(stack.getDefinition()))
				.toArray(IAEItemStack[]::new);
	}

	public int getCircuitValue() {
		return option;
	}

	@Override
	public IAEItemStack[] getInputs() {
		return filterCircuits(pattern.getInputs());
	}

	@Override
	public IAEItemStack[] getCondensedInputs() {
		return filterCircuits(pattern.getCondensedInputs());
	}

	@Override
	public IAEItemStack[] getOutputs() {
		return pattern.getOutputs();
	}

	@Override
	public IAEItemStack[] getCondensedOutputs() {
		return pattern.getCondensedOutputs();
	}

	@Override
	public ItemStack getPattern() {
		return pattern.getPattern();
	}

	@Override
	public boolean canSubstitute() {
		return pattern.canSubstitute();
	}

	@Override
	public ItemStack getOutput(InventoryCrafting inventoryCrafting, World world) {
		return pattern.getOutput(inventoryCrafting, world);
	}

	@Override
	public int getPriority() {
		return pattern.getPriority();
	}

	@Override
	public boolean isCraftable() {
		return pattern.isCraftable();
	}

	@Override
	public boolean isValidItemForSlot(int i, ItemStack itemStack, World world) {
		return pattern.isValidItemForSlot(i, itemStack, world);
	}

	@Override
	public void providePatterns(ICraftingMedium medium, ICraftingProviderHelper helper) {
		helper.addCraftingOption(medium, this);
	}

	@Override
	public void setPriority(int i) {
		pattern.setPriority(i);
	}

	@Override
	public Collection<IProvidablePattern> getProvidedPatterns() {
		return Arrays.asList(this);
	}

	@Override
	public IAEItemStack getPrimaryOutput() {
		return pattern.getPrimaryOutput();
	}
}
