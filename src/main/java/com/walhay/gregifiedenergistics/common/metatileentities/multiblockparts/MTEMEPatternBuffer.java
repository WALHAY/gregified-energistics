package com.walhay.gregifiedenergistics.common.metatileentities.multiblockparts;

import static gregtech.api.GTValues.ZPM;
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
import com.cleanroommc.modularui.api.IPanelHandler;
import com.cleanroommc.modularui.api.drawable.IKey;
import com.cleanroommc.modularui.factory.PosGuiData;
import com.cleanroommc.modularui.screen.ModularPanel;
import com.cleanroommc.modularui.screen.UISettings;
import com.cleanroommc.modularui.value.sync.PanelSyncManager;
import com.cleanroommc.modularui.widgets.SlotGroupWidget;
import com.cleanroommc.modularui.widgets.layout.Flow;
import com.cleanroommc.modularui.widgets.layout.Grid;
import com.cleanroommc.modularui.widgets.slot.ItemSlot;
import com.cleanroommc.modularui.widgets.slot.PhantomItemSlot;
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
import gregtech.api.metatileentity.multiblock.MultiblockControllerBase;
import gregtech.api.metatileentity.multiblock.RecipeMapMultiblockController;
import gregtech.api.mui.GTGuis;
import gregtech.api.mui.GTGuis.PopupPanel;
import gregtech.api.mui.sync.GTFluidSyncHandler;
import gregtech.api.util.GTTransferUtils;
import gregtech.common.mui.widget.GTFluidSlot;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.util.Constants;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidTank;
import net.minecraftforge.fluids.capability.IFluidTankProperties;
import net.minecraftforge.items.IItemHandler;
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
		return new ItemHandlerList((List<? extends IItemHandler>) patternHandler.getContainers());
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
		if (container == null) return false;

		container.insertInventory(inventory);

		return true;
	}

	@Override
	public void addToMultiBlock(MultiblockControllerBase controller) {
		super.addToMultiBlock(controller);
		patternHandler.syncControllerNotification();
	}

	@Override
	public void removeFromMultiBlock(MultiblockControllerBase controller) {
		super.removeFromMultiBlock(controller);
		patternHandler.syncControllerNotification();
	}

	@Override
	public boolean usesMui2() {
		return true;
	}

	@Override
	public ModularPanel buildUI(PosGuiData guiData, PanelSyncManager panelSyncManager, UISettings settings) {
		return GTGuis.createPanel(this, 176, 200)
				.child(SlotGroupWidget.builder()
						.matrix("IIIIIIIII", "IIIIIIIII", "IIIIIIIII", "IIIIIIIII")
						.key(
								'I',
								index -> new ItemSlot() {
									private IPanelHandler contentsPanel = panelSyncManager.syncedPanel(
											"contents_panel_" + index,
											true,
											(parent, player) -> contentsPanel(panelSyncManager, index));

									@Override
									public @NotNull Result onMousePressed(int mouseButton) {
										if (mouseButton == 1 && GuiScreen.isCtrlKeyDown()) {
											if (contentsPanel != null) {
												contentsPanel.togglePanel();
												return Result.SUCCESS;
											}
										}
										return super.onMousePressed(mouseButton);
									}
								}.slot(patternHandler, index))
						.build()
						.horizontalCenter()
						.top(7))
				.bindPlayerInventory();
	}

	protected PopupPanel contentsPanel(PanelSyncManager syncManager, int slot) {
		var container = patternHandler.containers[slot];
		if (container == null) return null;

		int slots = container.getSlots();
		int slotsPerRow = (int) Math.ceil(Math.sqrt(slots));

		int tanks = container.getTanks();
		int tanksPerRow = (int) Math.ceil(Math.sqrt(tanks));

		int width = 22 + Math.max(20, slotsPerRow * 18 + 22 + tanksPerRow * 18);
		int height = 22 + Math.max(20, 18 * Math.max(slotsPerRow, tanksPerRow));

		return (PopupPanel) GTGuis.createPopupPanel("contents_panel_" + slot, width, height)
				.child(IKey.lang("gregifiedenergistics.gui.buffer_contents", slot)
						.asWidget()
						.horizontalCenter())
				.child(Flow.row()
						.coverChildren()
						.horizontalCenter()
						.childPadding(8)
						.child(new Grid()
								.minColWidth(18)
								.minRowHeight(18)
								.coverChildren()
								.mapTo(slotsPerRow, slots, index -> new PhantomItemSlot().slot(container, index)))
						.child(new Grid()
								.minColWidth(18)
								.minRowHeight(18)
								.coverChildren()
								.mapTo(tanksPerRow, container.getFluidTanks(), (index, tank) -> new GTFluidSlot()
										.syncHandler(new GTFluidSyncHandler(tank).phantom(true)))));
	}

	@Override
	public MetaTileEntity createMetaTileEntity(IGregTechTileEntity tileEntity) {
		return new MTEMEPatternBuffer(metaTileEntityId);
	}

	@Override
	public @NotNull List<MultiblockAbility<?>> getAbilities() {
		return Arrays.asList(IMPORT_ITEMS);
	}

	@Override
	public void registerAbilities(@NotNull AbilityInstances ability) {
		ability.addAll(patternHandler.getContainers());
	}

	@Override
	public NBTTagCompound writeToNBT(NBTTagCompound data) {
		super.writeToNBT(data);
		data.setTag("PatternInventory", patternHandler.serializeNBT());
		data.setTag("PatternContainers", patternHandler.serializeContainersNBT());
		return data;
	}

	@Override
	public void readFromNBT(NBTTagCompound data) {
		super.readFromNBT(data);
		if (data.hasKey("PatternInventory")) {
			patternHandler.deserializeNBT(data.getCompoundTag("PatternInventory"));
		}
		if (data.hasKey("PatternContainers", Constants.NBT.TAG_LIST)) {
			patternHandler.deserializeContainersNBT(data.getTagList("PatternContainers", Constants.NBT.TAG_COMPOUND));
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
			return patternToContainer.containsKey(pattern);
		}

		public PatternContainer getContainer(ICraftingPatternDetails pattern) {
			return patternToContainer.get(pattern);
		}

		public Collection<PatternContainer> getContainers() {
			return Arrays.asList(containers);
		}

		private void attachContainerToPattern(int slot, @Nullable ICraftingPatternDetails pattern) {
			if (slot < 0 || slot >= containers.length) return;

			var container = containers[slot];
			container.setPattern(pattern);
			if (pattern != null) {
				patternToContainer.put(pattern, container);
			}
		}

		private void detachContainerFromPattern(
				int slot, @Nullable ICraftingPatternDetails oldPattern, boolean refundContents) {
			if (slot < 0 || slot >= containers.length) return;

			var container = containers[slot];
			if (oldPattern != null) {
				patternToContainer.remove(oldPattern, container);
			}

			if (refundContents) {
				container.refundItems();
				container.refundFluids();
			}

			container.setPattern(null);
		}

		@Override
		protected void onLoad() {
			super.onLoad();
			patternToContainer.clear();
			for (int i = 0; i < getSlots(); ++i) {
				var pattern = getPatternDetails(i);
				attachContainerToPattern(i, pattern);
			}
			syncControllerNotification();
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
			var hasChanged = oldPattern != pattern;
			var controller = MTEMEPatternBuffer.this.getController();
			if (controller != null) controller.invalidateStructure();

			if (!hasChanged) return;

			detachContainerFromPattern(slot, oldPattern, true);

			if (pattern != null) {
				attachContainerToPattern(slot, pattern);
			}
			syncControllerNotification();
		}

		@Override
		protected void onPatternUpdate() {
			notifyPatternChange();
		}

		@Override
		public Iterator<PatternContainer> iterator() {
			return getContainers().iterator();
		}

		public NBTTagList serializeContainersNBT() {
			var list = new NBTTagList();
			for (int i = 0; i < containers.length; ++i) {
				var entry = new NBTTagCompound();
				entry.setInteger("Slot", i);
				entry.setTag("Data", containers[i].serializeNBT());
				list.appendTag(entry);
			}
			return list;
		}

		public void deserializeContainersNBT(NBTTagList list) {
			for (int i = 0; i < containers.length; ++i) {
				attachContainerToPattern(i, getPatternDetails(i));
			}

			for (int i = 0; i < list.tagCount(); ++i) {
				var entry = list.getCompoundTagAt(i);
				if (!entry.hasKey("Slot", Constants.NBT.TAG_INT)) continue;
				int slot = entry.getInteger("Slot");
				if (slot < 0 || slot >= containers.length) continue;
				if (!entry.hasKey("Data", Constants.NBT.TAG_COMPOUND)) continue;

				containers[slot].deserializeNBT(entry.getCompoundTag("Data"));
			}
			syncControllerNotification();
		}

		public void syncControllerNotification() {
			var controller = MTEMEPatternBuffer.this.getController();
			for (var container : containers) {
				container.setControllerToNotify(controller);
			}
		}
	}

	/**
	 * PatternContainer Storage for all items/fluids accepted from pattern
	 *
	 * <p>Works as proxy for {@link ItemStackHandler} and {@link FluidTank}
	 *
	 * <p>TODO: Add Fluids
	 */
	private class PatternContainer extends NotifiableItemStackHandler implements IMultipleTankHandler {

		private ICraftingPatternDetails pattern;
		private FluidTankList fluidInventory;
		private MetaTileEntity controllerToNotify;

		public PatternContainer() {
			super(MTEMEPatternBuffer.this, 0, MTEMEPatternBuffer.this, false);
			this.fluidInventory = new FluidTankList(false);
		}

		public void setPattern(ICraftingPatternDetails pattern) {
			this.pattern = pattern;

			var size =
					pattern == null || pattern.getCondensedInputs() == null ? 0 : pattern.getCondensedInputs().length;

			setSize(size);

			List<FluidTank> tanks = new ArrayList<>();
			if (pattern != null && pattern.getCondensedInputs() != null) {
				for (var stack : pattern.getCondensedInputs()) {
					if (FakeFluids.isFluidFakeItem(stack.createItemStack())) {
						tanks.add(new NotifiableFluidTank(Integer.MAX_VALUE, MTEMEPatternBuffer.this, false));
					}
				}
			}
			this.fluidInventory = new FluidTankList(false, tanks);
			setControllerToNotify(MTEMEPatternBuffer.this.getController());
		}

		public void setControllerToNotify(@Nullable MetaTileEntity controller) {
			if (this.controllerToNotify == controller) return;

			if (this.controllerToNotify != null) {
				removeNotifiableMetaTileEntity(this.controllerToNotify);
				for (var tank : getFluidTanks()) {
					if (tank instanceof NotifiableFluidTank notifiableTank) {
						notifiableTank.removeNotifiableMetaTileEntity(this.controllerToNotify);
					}
				}
			}

			this.controllerToNotify = controller;
			if (controller != null) {
				addNotifiableMetaTileEntity(controller);
				for (var tank : getFluidTanks()) {
					if (tank instanceof NotifiableFluidTank notifiableTank) {
						notifiableTank.addNotifiableMetaTileEntity(controller);
					}
				}
			}
		}

		@Nullable public ICraftingPatternDetails getPattern() {
			return pattern;
		}

		@Override
		public int getSlotLimit(int slot) {
			return Integer.MAX_VALUE;
		}

		@Override
		protected int getStackLimit(int slot, ItemStack stack) {
			return getSlotLimit(slot);
		}

		public void insertInventory(InventoryCrafting inventory) {
			if (inventory == null || inventory.isEmpty()) return;

			for (int i = 0; i < inventory.getSizeInventory(); ++i) {
				var stack = inventory.getStackInSlot(i);

				if (stack == null || stack.isEmpty()) continue;

				if (FakeFluids.isFluidFakeItem(stack)) {
					FluidStack fluidStack = FakeItemRegister.getStack(stack);
					if (fluidStack == null) continue;
					insertFluid(fluidStack.copy());
					continue;
				}

				insertItem(stack.copy());
			}
		}

		public void insertItem(ItemStack stack) {
			var result = GTTransferUtils.insertItem(this, stack, true);
			if (!result.isEmpty()) return;

			GTTransferUtils.insertItem(this, stack, false);
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

			for (int i = 0; i < size(); ++i) {
				var stack = getStackInSlot(i);

				if (stack == null || stack.isEmpty()) continue;

				IAEItemStack remainder = meInventory.injectItems(
						AEItemStack.fromItemStack(stack), Actionable.MODULATE, getActionSource());
				if (remainder == null || remainder.getStackSize() <= 0) {
					setStackInSlot(i, ItemStack.EMPTY);
				} else {
					setStackInSlot(i, remainder.asItemStackRepresentation());
				}
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

			for (var tank : getFluidTanks()) {
				var fluid = tank.getFluid();

				if (fluid == null || fluid.amount <= 0) continue;

				IAEFluidStack aeFluid = AEFluidStack.fromFluidStack(fluid);

				IAEFluidStack remainder = meInventory.injectItems(aeFluid, Actionable.MODULATE, getActionSource());
				int toDrain = fluid.amount;
				if (remainder != null && remainder.getStackSize() > 0) {
					toDrain = fluid.amount - (int) remainder.getStackSize();
				}
				if (toDrain <= 0) continue;

				tank.drain(toDrain, true);
			}
		}

		@Override
		public NBTTagCompound serializeNBT() {
			var nbt = super.serializeNBT();

			nbt.setTag("FluidInventory", fluidInventory.serializeNBT());

			return nbt;
		}

		@Override
		public void deserializeNBT(NBTTagCompound nbt) {
			super.deserializeNBT(nbt);

			if (nbt.hasKey("FluidInventory")) {
				fluidInventory.deserializeNBT(nbt.getCompoundTag("FluidInventory"));
			}
		}

		@Override
		public IFluidTankProperties[] getTankProperties() {
			return fluidInventory.getTankProperties();
		}

		@Override
		public int fill(FluidStack resource, boolean doFill) {
			return fluidInventory.fill(resource, doFill);
		}

		@Override
		public FluidStack drain(FluidStack resource, boolean doDrain) {
			return fluidInventory.drain(resource, doDrain);
		}

		@Override
		public FluidStack drain(int maxDrain, boolean doDrain) {
			return fluidInventory.drain(maxDrain, doDrain);
		}

		@Override
		public boolean allowSameFluidFill() {
			return false;
		}

		@Override
		public @NotNull List<ITankEntry> getFluidTanks() {
			return fluidInventory.getFluidTanks();
		}

		@Override
		public @NotNull ITankEntry getTankAt(int index) {
			return fluidInventory.getTankAt(index);
		}

		@Override
		public int getTanks() {
			return fluidInventory.getTanks();
		}
	}
}
