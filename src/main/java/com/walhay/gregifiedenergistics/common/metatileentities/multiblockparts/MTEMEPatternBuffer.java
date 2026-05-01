package com.walhay.gregifiedenergistics.common.metatileentities.multiblockparts;

import static gregtech.api.GTValues.ZPM;

import appeng.api.networking.crafting.ICraftingPatternDetails;
import appeng.api.networking.crafting.ICraftingProviderHelper;
import appeng.api.storage.channels.IItemStorageChannel;
import appeng.api.storage.data.IAEItemStack;
import appeng.items.misc.ItemEncodedPattern;
import com.cleanroommc.modularui.factory.PosGuiData;
import com.cleanroommc.modularui.screen.ModularPanel;
import com.cleanroommc.modularui.screen.UISettings;
import com.cleanroommc.modularui.value.sync.PanelSyncManager;
import com.cleanroommc.modularui.widgets.SlotGroupWidget;
import com.cleanroommc.modularui.widgets.slot.ItemSlot;
import com.walhay.gregifiedenergistics.api.capability.AbstractPatternItemHandler;
import com.walhay.gregifiedenergistics.api.metatileentity.MetaTileEntityCraftingProvider;
import gregtech.api.capability.impl.FluidTankList;
import gregtech.api.capability.impl.ItemHandlerList;
import gregtech.api.capability.impl.NotifiableItemStackHandler;
import gregtech.api.metatileentity.MetaTileEntity;
import gregtech.api.metatileentity.interfaces.IGregTechTileEntity;
import gregtech.api.metatileentity.multiblock.AbilityInstances;
import gregtech.api.metatileentity.multiblock.IMultiblockAbilityPart;
import gregtech.api.metatileentity.multiblock.MultiblockAbility;
import gregtech.api.metatileentity.multiblock.RecipeMapMultiblockController;
import gregtech.api.mui.GTGuis;
import gregtech.api.util.GTLog;
import gregtech.api.util.GTTransferUtils;
import it.unimi.dsi.fastutil.objects.Object2ObjectArrayMap;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.util.INBTSerializable;
import net.minecraftforge.fluids.FluidTank;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.IItemHandlerModifiable;
import net.minecraftforge.items.ItemStackHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/** MTEMEPatternBuffer */
public class MTEMEPatternBuffer extends MetaTileEntityCraftingProvider<IAEItemStack>
		implements IMultiblockAbilityPart<IItemHandlerModifiable> {
	private boolean isWorkingEnabled = true;

	private PatternContainer[] containers;
	private final PatternBuffer patternInventory = new PatternBuffer(36);

	public MTEMEPatternBuffer(ResourceLocation metaTileEntityId) {
		super(metaTileEntityId, ZPM, false, IItemStorageChannel.class);
	}

	@Override
	public void clearMachineInventory(@NotNull List<@NotNull ItemStack> itemBuffer) {
		super.clearMachineInventory(itemBuffer);
		patternInventory.refundAll();
		for (int i = 0; i < patternInventory.getSlots(); ++i) {
			itemBuffer.add(patternInventory.getStackInSlot(i));
		}

		for (var container : patternInventory.getContainers()) {
			if (container == null) continue;
			var inventory = container.inventory();
			for (int j = 0; j < inventory.getSlots(); ++j) {
				itemBuffer.add(inventory.getStackInSlot(j));
			}
		}
	}

	@Override
	public IFluidHandler getFluidInventory() {
		return new FluidTankList(true);
	}

	@Override
	protected IItemHandlerModifiable createImportItemHandler() {
		if (containers == null) {
			this.containers = new PatternContainer[36];
			for (int i = 0; i < containers.length; ++i) {
				this.containers[i] = new PatternContainer();
			}
		}

		List<IItemHandler> handlers =
				Arrays.stream(containers).map(PatternContainer::inventory).collect(Collectors.toList());

		return new ItemHandlerList(handlers);
	}

	@Override
	public boolean isWorkingEnabled() {
		return isWorkingEnabled;
	}

	@Override
	public void setWorkingEnabled(boolean isWorkingEnabled) {
		this.isWorkingEnabled = isWorkingEnabled;
		notifyPatternChange();
	}

	@Override
	public void provideCrafting(ICraftingProviderHelper provider) {
		if (isAttachedToMultiBlock() && isWorkingEnabled()) {
			for (var pattern : patternInventory.getPatterns()) {
				if (pattern == null) continue;

				provider.addCraftingOption(getCraftingProvider(), pattern);
			}
		}
	}

	@Override
	public boolean isBusy() {
		if (getController() == null) return true;

		if (getController() instanceof RecipeMapMultiblockController controller && !controller.isWorkingEnabled())
			return true;

		return false;
	}

	@Override
	public boolean pushPattern(ICraftingPatternDetails pattern, InventoryCrafting inventory) {
		if (!isWorkingEnabled() || !patternInventory.contains(pattern)) return false;

		var container = patternInventory.getContainer(pattern);
		GTLog.logger.info("Insert into container: " + container);
		if (container != null) {
			container.insertInventory(inventory);
		}

		return true;
	}

	@Override
	public boolean usesMui2() {
		return true;
	}

	@Override
	public ModularPanel buildUI(PosGuiData guiData, PanelSyncManager panelSyncManager, UISettings settings) {
		var panel = GTGuis.createPanel(this, 176, 200);

		panel.child(SlotGroupWidget.builder()
				.matrix("IIIIIIIII", "IIIIIIIII", "IIIIIIIII", "IIIIIIIII")
				.key('I', index -> new ItemSlot().slot(patternInventory, index))
				.build()
				.horizontalCenter()
				.top(7));

		panel.bindPlayerInventory();

		return panel;
	}

	@Override
	public MetaTileEntity createMetaTileEntity(IGregTechTileEntity arg0) {
		return new MTEMEPatternBuffer(metaTileEntityId);
	}

	public MultiblockAbility<IItemHandlerModifiable> getAbility() {
		return MultiblockAbility.IMPORT_ITEMS;
	}

	@Override
	public void registerAbilities(@NotNull AbilityInstances ability) {
		ability.add(this.importItems);
	}

	@Override
	public NBTTagCompound writeToNBT(NBTTagCompound data) {
		super.writeToNBT(data);
		data.setTag("PatternInventory", patternInventory.serializeNBT());
		return data;
	}

	@Override
	public void readFromNBT(NBTTagCompound data) {
		super.readFromNBT(data);
		if (data.hasKey("PatternInventory")) {
			patternInventory.deserializeNBT(data.getCompoundTag("PatternInventory"));
		}
	}

	@Override
	public void notifyPatternChange() {
		super.notifyPatternChange();
	}

	/** PatternBuffer */
	private class PatternBuffer extends AbstractPatternItemHandler {

		private final Object2ObjectArrayMap<ICraftingPatternDetails, PatternContainer> patternToContainer;

		public PatternBuffer(int size) {
			super(size);
			this.patternToContainer = new Object2ObjectArrayMap<>(size);
		}

		public boolean contains(ICraftingPatternDetails pattern) {
			var patterns = getPatterns();
			return patterns != null && patterns.contains(pattern);
		}

		public PatternContainer getContainer(ICraftingPatternDetails pattern) {
			return patternToContainer.get(pattern);
		}

		public Collection<PatternContainer> getContainers() {
			return Arrays.asList(containers);
		}

		public void refundAll() {}

		@Override
		protected void onLoad() {
			super.onLoad();
			for (int i = 0; i < getSlots(); ++i) {
				var pattern = getPatternDetails(i);

				if (pattern == null) continue;

				assignContainerToPattern(pattern);
			}
		}

		@Override
		protected ICraftingPatternDetails getPatternFromStack(ItemStack stack) {
			if (stack.getItem() instanceof ItemEncodedPattern encodedPattern) {
				return encodedPattern.getPatternForItem(stack, getWorld());
			}
			return null;
		}

		private PatternContainer getFreeContainer() {
			return Arrays.stream(containers)
					.filter(p -> p.pattern == null)
					.findFirst()
					.orElse(null);
		}

		private void assignContainerToPattern(ICraftingPatternDetails pattern) {
			if (pattern == null) return;

			var container = getFreeContainer();
			if (container == null) return;

			patternToContainer.put(pattern, container);
			container.setPattern(pattern);
		}

		@Override
		protected void onContentsChanged(int slot) {
			var oldPattern = getPatternDetails(slot);
			super.onContentsChanged(slot);
			var pattern = getPatternDetails(slot);

			if (pattern == null) {
				var container = getContainer(oldPattern);
				if (container != null) {
					container.setPattern(null);
					container.refundItems();
					patternToContainer.remove(oldPattern, container);
				}
				// refund old pattern container
				return;
			}

			assignContainerToPattern(pattern);
		}

		@Override
		protected void onPatternUpdate() {
			notifyPatternChange();
		}
	}

	/**
	 * PatternContainer Storage for all items/fluids accepted from pattern
	 *
	 * <p>Works as proxy for {@link ItemStackHandler} and {@link FluidTank}
	 *
	 * <p>TODO: Add Fluids
	 */
	private class PatternContainer implements INBTSerializable<NBTTagCompound> {

		private ICraftingPatternDetails pattern;
		private PatternBufferStackHandler items;
		// private List<FluidStack> fluids;

		public PatternContainer() {
			this(null);
		}

		public PatternContainer(ICraftingPatternDetails pattern) {
			this.items = new PatternBufferStackHandler(pattern);
			this.pattern = pattern;
		}

		public void setPattern(ICraftingPatternDetails pattern) {
			this.pattern = pattern;
		}

		public IItemHandlerModifiable inventory() {
			return items;
		}

		@Nullable public ICraftingPatternDetails getPattern() {
			return pattern;
		}

		public void insertInventory(InventoryCrafting inventory) {
			if (inventory == null || inventory.isEmpty()) return;

			for (int i = 0; i < inventory.getSizeInventory(); ++i) {
				var stack = inventory.getStackInSlot(i);

				if (stack == null || stack.isEmpty()) continue;

				GTLog.logger.info("Insert into container: " + stack.getDisplayName());
				insertItem(stack);
			}
		}

		public void insertItem(ItemStack stack) {
			GTLog.logger.info("Trying to insert simulate");
			var result = GTTransferUtils.insertItem(items, stack, true);
			GTLog.logger.info("Result: " + result);
			if (!result.isEmpty()) return;

			var res = GTTransferUtils.insertItem(items, stack, false);
			GTLog.logger.info("Successfully inserted simulate: " + stack + "\nRemaining real: " + res);
		}

		public void refundItems() {}

		@Override
		public NBTTagCompound serializeNBT() {
			var nbt = new NBTTagCompound();

			return nbt;
		}

		@Override
		public void deserializeNBT(NBTTagCompound nbt) {}
	}

	/** Storage with unlimited space */
	private class PatternBufferStackHandler extends NotifiableItemStackHandler {

		private PatternBufferStackHandler(ICraftingPatternDetails pattern) {
			super(MTEMEPatternBuffer.this, 16, MTEMEPatternBuffer.this.getController(), false);
		}

		@Override
		public int getSlotLimit(int slot) {
			return Integer.MAX_VALUE;
		}

		@Override
		protected int getStackLimit(int slot, ItemStack stack) {
			return getSlotLimit(slot);
		}
	}
}
