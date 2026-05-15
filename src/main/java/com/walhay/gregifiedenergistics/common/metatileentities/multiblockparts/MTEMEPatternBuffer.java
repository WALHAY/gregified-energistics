package com.walhay.gregifiedenergistics.common.metatileentities.multiblockparts;

import static gregtech.api.GTValues.ZPM;
import static gregtech.api.metatileentity.multiblock.MultiblockAbility.IMPORT_FLUIDS;
import static gregtech.api.metatileentity.multiblock.MultiblockAbility.IMPORT_ITEMS;

import appeng.api.AEApi;
import appeng.api.config.Actionable;
import appeng.api.networking.crafting.ICraftingPatternDetails;
import appeng.api.networking.crafting.ICraftingProviderHelper;
import appeng.api.networking.storage.IStorageGrid;
import appeng.api.storage.IMEInventory;
import appeng.api.storage.channels.IFluidStorageChannel;
import appeng.api.storage.channels.IItemStorageChannel;
import appeng.api.storage.data.IAEFluidStack;
import appeng.api.storage.data.IAEItemStack;
import appeng.fluids.util.AEFluidStack;
import appeng.items.misc.ItemEncodedPattern;
import appeng.util.item.AEItemStack;
import com.cleanroommc.modularui.factory.PosGuiData;
import com.cleanroommc.modularui.screen.ModularPanel;
import com.cleanroommc.modularui.screen.UISettings;
import com.cleanroommc.modularui.value.sync.PanelSyncManager;
import com.cleanroommc.modularui.widgets.SlotGroupWidget;
import com.cleanroommc.modularui.widgets.slot.ItemSlot;
import com.glodblock.github.common.item.fake.FakeFluids;
import com.glodblock.github.common.item.fake.FakeItemRegister;
import com.walhay.gregifiedenergistics.api.capability.AbstractPatternItemHandler;
import com.walhay.gregifiedenergistics.api.metatileentity.MetaTileEntityCraftingProvider;
import gregtech.api.capability.IMultipleTankHandler;
import gregtech.api.capability.impl.FluidTankList;
import gregtech.api.capability.impl.ItemHandlerList;
import gregtech.api.capability.impl.NotifiableFluidTank;
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
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.util.INBTSerializable;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidTank;
import net.minecraftforge.items.IItemHandlerModifiable;
import net.minecraftforge.items.ItemStackHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/** MTEMEPatternBuffer */
public class MTEMEPatternBuffer extends MetaTileEntityCraftingProvider<IAEItemStack>
		implements IMultiblockAbilityPart<IItemHandlerModifiable> {
	private boolean isWorkingEnabled = true;

	private final PatternHandler patternHandler;

	public MTEMEPatternBuffer(ResourceLocation metaTileEntityId) {
		super(metaTileEntityId, ZPM, false, IItemStorageChannel.class);
		this.patternHandler = new PatternHandler(36);
	}

	@Override
	protected IItemHandlerModifiable createImportItemHandler() {
		if (patternHandler == null || patternHandler.getContainers() == null)
			return new ItemHandlerList(Collections.emptyList());
		return new ItemHandlerList(patternHandler.getContainers().stream()
				.map(PatternContainer::inventory)
				.filter(Objects::nonNull)
				.collect(Collectors.toList()));
	}

	@Override
	public void clearMachineInventory(@NotNull List<@NotNull ItemStack> itemBuffer) {
		for (var container : patternHandler.containers) {
			container.refundItems();
			container.refundFluids();
		}

		super.clearMachineInventory(itemBuffer);
	}

	@Override
	public boolean isWorkingEnabled() {
		return isWorkingEnabled;
	}

	@Override
	public void setWorkingEnabled(boolean isWorkingEnabled) {
		if (this.isWorkingEnabled != isWorkingEnabled) {
			this.isWorkingEnabled = isWorkingEnabled;
			notifyPatternChange();
		}
	}

	@Override
	public void provideCrafting(ICraftingProviderHelper provider) {
		if (isAttachedToMultiBlock() && isWorkingEnabled()) {
			for (var pattern : patternHandler.getPatterns()) {
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
		if (!isWorkingEnabled() || !patternHandler.contains(pattern)) return false;

		var container = patternHandler.getContainer(pattern);
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
				.key('I', index -> new ItemSlot().slot(patternHandler, index))
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

	@Override
	public @NotNull List<MultiblockAbility<?>> getAbilities() {
		return Arrays.asList(IMPORT_ITEMS, IMPORT_FLUIDS);
	}

	@Override
	public void registerAbilities(@NotNull AbilityInstances ability) {
		for (var container : patternHandler) {
			ability.add(
					ability.isKey(IMPORT_ITEMS)
							? container.inventory()
							: container.fluidInventory().getFluidTanks());
		}
	}

	@Override
	public NBTTagCompound writeToNBT(NBTTagCompound data) {
		super.writeToNBT(data);
		data.setTag("PatternInventory", patternHandler.serializeNBT());
		return data;
	}

	@Override
	public void readFromNBT(NBTTagCompound data) {
		super.readFromNBT(data);
		if (data.hasKey("PatternInventory")) {
			patternHandler.deserializeNBT(data.getCompoundTag("PatternInventory"));
		}
	}

	/** PatternBuffer */
	private class PatternHandler extends AbstractPatternItemHandler implements Iterable<PatternContainer> {

		private final PatternContainer[] containers;
		private final Object2ObjectOpenHashMap<ICraftingPatternDetails, PatternContainer> patternToContainer;

		public PatternHandler(int size) {
			super(size);
			this.containers = new PatternContainer[size];
			for (int i = 0; i < size; ++i) {
				this.containers[i] = new PatternContainer();
			}
			this.patternToContainer = new Object2ObjectOpenHashMap<>(size);
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

		private PatternContainer getFreeContainer() {
			return getContainers().stream()
					.filter(p -> p != null && p.pattern == null)
					.findFirst()
					.orElse(null);
		}

		private void attachContainerToPattern(ICraftingPatternDetails pattern) {
			if (pattern == null) return;

			var container = getFreeContainer();
			if (container == null) return;

			patternToContainer.put(pattern, container);
			container.setPattern(pattern);
		}

		@Override
		protected void onLoad() {
			super.onLoad();
			for (int i = 0; i < getSlots(); ++i) {
				var pattern = getPatternDetails(i);

				attachContainerToPattern(pattern);
			}
		}

		@Override
		protected ICraftingPatternDetails getPatternFromStack(ItemStack stack) {
			if (stack.getItem() instanceof ItemEncodedPattern encodedPattern) {
				return encodedPattern.getPatternForItem(stack, getWorld());
			}
			return null;
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
					patternToContainer.remove(oldPattern, container);
				}
				// TODO: refund old pattern container
				return;
			}

			attachContainerToPattern(pattern);
		}

		@Override
		protected void onPatternUpdate() {
			notifyPatternChange();
		}

		@Override
		public Iterator<PatternContainer> iterator() {
			return getContainers().iterator();
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
		private PatternBufferStackHandler inventory;
		private FluidTankList fluidInventory;

		public PatternContainer() {
			this.inventory = new PatternBufferStackHandler();
			List<FluidTank> tanks = new ArrayList<>();
			for (int i = 0; i < 9; ++i) {
				tanks.add(new NotifiableFluidTank(Integer.MAX_VALUE, MTEMEPatternBuffer.this, false));
			}
			this.fluidInventory = new FluidTankList(false, tanks);
		}

		public void setPattern(ICraftingPatternDetails pattern) {
			this.pattern = pattern;
			this.inventory.setPattern(pattern);
		}

		@NotNull public IItemHandlerModifiable inventory() {
			return inventory;
		}

		@NotNull public IMultipleTankHandler fluidInventory() {
			return fluidInventory;
		}

		@Nullable public ICraftingPatternDetails getPattern() {
			return pattern;
		}

		public void insertInventory(InventoryCrafting inventory) {
			if (inventory == null || inventory.isEmpty()) return;

			for (int i = 0; i < inventory.getSizeInventory(); ++i) {
				var stack = inventory.getStackInSlot(i);

				if (stack == null || stack.isEmpty()) continue;

				if (FakeFluids.isFluidFakeItem(stack)) {
					FluidStack fluidStack = FakeItemRegister.getStack(stack);
					if (fluidStack == null) continue;
					insertFluid(fluidStack);
					continue;
				}

				insertItem(stack);
			}
		}

		public void insertItem(ItemStack stack) {
			var result = GTTransferUtils.insertItem(inventory, stack, true);
			if (!result.isEmpty()) return;

			GTTransferUtils.insertItem(inventory, stack, false);
		}

		public void insertFluid(FluidStack stack) {
			if (stack == null) return;

			fluidInventory.fill(stack, true);
		}

		public void refundItems() {
			var node = getProxy().getNode();
			if (node == null) return;

			var grid = node.getGrid();
			if (grid == null) return;

			IStorageGrid storage = grid.getCache(IStorageGrid.class);
			if (storage == null) return;

			IMEInventory<IAEItemStack> meInventory = storage.getInventory(getStorageChannel());
			if (meInventory == null) return;

			for (int i = 0; i < inventory.size(); ++i) {
				var stack = inventory.getStackInSlot(i);

				if (stack == null || stack.isEmpty()) continue;

				IAEItemStack remainder = meInventory.injectItems(
						AEItemStack.fromItemStack(stack), Actionable.MODULATE, getActionSource());
				if (remainder == null) continue;

				inventory.setStackInSlot(i, remainder.asItemStackRepresentation());
			}
		}

		public void refundFluids() {
			var node = getProxy().getNode();
			if (node == null) return;

			var grid = node.getGrid();
			if (grid == null) return;

			IFluidStorageChannel fluidChannel =
					AEApi.instance().storage().getStorageChannel(IFluidStorageChannel.class);

			IStorageGrid storage = grid.getCache(IStorageGrid.class);
			if (storage == null) return;

			IMEInventory<IAEFluidStack> meInventory = storage.getInventory(fluidChannel);
			if (meInventory == null) return;

			for (var tank : fluidInventory.getFluidTanks()) {
				var fluid = tank.getFluid();

				if (fluid == null || fluid.amount <= 0) continue;

				IAEFluidStack aeFluid = AEFluidStack.fromFluidStack(fluid);

				IAEFluidStack remainder = meInventory.injectItems(aeFluid, Actionable.MODULATE, getActionSource());
				if (remainder == null) continue;

				int toDrain = fluid.amount - (int) remainder.getStackSize();

				tank.drain(toDrain, true);
			}
		}

		@Override
		public NBTTagCompound serializeNBT() {
			var nbt = new NBTTagCompound();

			nbt.setTag("Inventory", inventory.serializeNBT());
			nbt.setTag("FluidInventory", fluidInventory.serializeNBT());

			return nbt;
		}

		@Override
		public void deserializeNBT(NBTTagCompound nbt) {
			if (nbt.hasKey("Inventory")) {
				inventory.deserializeNBT(nbt.getCompoundTag("Inventory"));
			}
			if (nbt.hasKey("FluidInventory")) {
				fluidInventory.deserializeNBT(nbt.getCompoundTag("FluidInventory"));
			}
		}
	}

	/** Storage with unlimited space */
	private class PatternBufferStackHandler extends NotifiableItemStackHandler {

		private PatternBufferStackHandler() {
			super(MTEMEPatternBuffer.this, 0, MTEMEPatternBuffer.this.getController(), false);
		}

		public void setPattern(ICraftingPatternDetails pattern) {
			var size =
					pattern == null || pattern.getCondensedInputs() == null ? 0 : pattern.getCondensedOutputs().length;

			setSize(size);
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
