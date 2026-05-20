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
import com.glodblock.github.common.item.fake.FakeFluids;
import com.glodblock.github.common.item.fake.FakeItemRegister;
import com.walhay.gregifiedenergistics.api.capability.AbstractPatternItemHandler;
import com.walhay.gregifiedenergistics.api.capability.PatternBufferDualDelegate;
import com.walhay.gregifiedenergistics.api.metatileentity.MetaTileEntityCraftingProvider;
import com.walhay.gregifiedenergistics.api.util.FluidCraftingUtils;
import gregtech.api.capability.IMultipleTankHandler;
import gregtech.api.capability.impl.FluidTankList;
import gregtech.api.capability.impl.GhostCircuitItemStackHandler;
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
import gregtech.api.mui.widget.GhostCircuitSlotWidget;
import gregtech.api.util.GTTransferUtils;
import gregtech.common.mui.widget.GTFluidSlot;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;
import net.minecraft.block.Block;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.util.Constants;
import net.minecraftforge.common.util.INBTSerializable;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidTank;
import net.minecraftforge.items.IItemHandlerModifiable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.input.Keyboard;

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
				.map(PatternContainer::dualInventory)
				.collect(Collectors.toList()));
	}

	@Override
	public void clearMachineInventory(@NotNull List<@NotNull ItemStack> itemBuffer) {
		for (var container : patternHandler.containers) {
			container.refundItems(false);
			container.refundFluids();
		}

		super.clearMachineInventory(itemBuffer);
		clearInventory(itemBuffer, patternHandler);
		for (var container : patternHandler.getContainers()) {
			clearInventory(itemBuffer, container.inventory());
		}
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
											"buffer_contents#" + index, true, (parent, player) -> patternHandler
													.getContainers()
													.get(index)
													.buildUI(panelSyncManager, index));

									@Override
									public @NotNull Result onKeyPressed(char typedChar, int keyCode) {
										if (keyCode == Keyboard.KEY_B) {
											if (contentsPanel != null) {
												contentsPanel.togglePanel();
												return Result.SUCCESS;
											}
										}
										return super.onKeyPressed(typedChar, keyCode);
									}
								}.slot(patternHandler, index)
										.tooltipBuilder(rt ->
												rt.addLine(IKey.lang("gregifiedenergistics.gui.buffer_contents_open"))))
						.build()
						.horizontalCenter()
						.top(7))
				.bindPlayerInventory();
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
		patternHandler.getContainers().stream()
				.map(PatternContainer::dualInventory)
				.forEach(ability::add);
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

		public List<PatternContainer> getContainers() {
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

		private void detachContainerFromPattern(int slot, @Nullable ICraftingPatternDetails pattern) {
			if (slot < 0 || slot >= containers.length) return;

			var container = containers[slot];
			if (pattern != null) {
				patternToContainer.remove(pattern, container);
			}

			container.refundItems(true);
			container.refundFluids();

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
			var previous = getPatternDetails(slot);
			super.onContentsChanged(slot);
			var current = getPatternDetails(slot);

			if (previous == current) return;

			detachContainerFromPattern(slot, previous);

			if (current != null) {
				attachContainerToPattern(slot, current);
			}
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
	 * PatternContainer
	 *
	 * <p>Medium for operations on {@link IItemHandlerModifiable} and {@link IMultipleTankHandler}.
	 *
	 * <p>Implements {@link INBTSerializable} for serializing container content.
	 */
	private class PatternContainer implements INBTSerializable<NBTTagCompound> {

		private ICraftingPatternDetails pattern;

		private NotifiableItemStackHandler itemInventory;
		private FluidTankList fluidInventory;
		private GhostCircuitItemStackHandler ghostCircuit;

		private MetaTileEntity controllerToNotify;

		private PatternBufferDualDelegate dualHandler;

		public PatternContainer() {
			this.itemInventory =
					new NotifiableItemStackHandler(
							MTEMEPatternBuffer.this, 0, MTEMEPatternBuffer.this.getController(), false) {
						@Override
						public int getSlotLimit(int slot) {
							return Integer.MAX_VALUE;
						}

						@Override
						protected int getStackLimit(int slot, ItemStack stack) {
							return getSlotLimit(slot);
						}
					};
			this.fluidInventory = new FluidTankList(false);
			this.ghostCircuit = new GhostCircuitItemStackHandler(MTEMEPatternBuffer.this.getController());

			this.dualHandler = new PatternBufferDualDelegate(itemInventory, ghostCircuit, fluidInventory);
		}

		public void setPattern(ICraftingPatternDetails pattern) {
			this.pattern = pattern;

			int inputs = 0;
			int fluids = 0;
			if (pattern != null && pattern.getCondensedInputs() != null) {
				fluids = FluidCraftingUtils.getFluids(pattern.getCondensedInputs())
						.size();
				inputs = pattern.getCondensedInputs().length - fluids;
			}

			this.dualHandler.setSize(inputs);

			List<FluidTank> tanks = new ArrayList<>(fluids);
			for (int i = 0; i < fluids; ++i) {
				tanks.add(new NotifiableFluidTank(Integer.MAX_VALUE, MTEMEPatternBuffer.this.getController(), false));
			}

			this.fluidInventory = new FluidTankList(false, tanks);
			this.dualHandler.setFluidDelegate(this.fluidInventory);

			setControllerToNotify(MTEMEPatternBuffer.this.getController());
		}

		public void setControllerToNotify(@Nullable MetaTileEntity controller) {
			if (this.controllerToNotify == controller) return;

			if (this.controllerToNotify != null) {
				itemInventory.removeNotifiableMetaTileEntity(this.controllerToNotify);
				ghostCircuit.removeNotifiableMetaTileEntity(this.controllerToNotify);
				for (var tank : dualHandler.getFluidTanks()) {
					if (tank instanceof NotifiableFluidTank notifiableTank) {
						notifiableTank.removeNotifiableMetaTileEntity(this.controllerToNotify);
					}
				}
			}

			this.controllerToNotify = controller;
			if (controller != null) {
				itemInventory.addNotifiableMetaTileEntity(controller);
				ghostCircuit.addNotifiableMetaTileEntity(controller);
				for (var tank : dualHandler.getFluidTanks()) {
					if (tank instanceof NotifiableFluidTank notifiableTank) {
						notifiableTank.addNotifiableMetaTileEntity(controller);
					}
				}
			}
		}

		@Nullable public ICraftingPatternDetails getPattern() {
			return pattern;
		}

		public IItemHandlerModifiable inventory() {
			return itemInventory;
		}

		public GhostCircuitItemStackHandler ghostCircuitInventory() {
			return ghostCircuit;
		}

		public IMultipleTankHandler fluidInventory() {
			return fluidInventory;
		}

		public PatternBufferDualDelegate dualInventory() {
			return dualHandler;
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
			var result = GTTransferUtils.insertItem(itemInventory, stack, true);
			if (!result.isEmpty()) return;

			GTTransferUtils.insertItem(itemInventory, stack, false);
		}

		public void insertFluid(FluidStack stack) {
			if (stack == null || stack.amount <= 0) return;

			int accepted = fluidInventory.fill(stack.copy(), false);
			if (accepted < stack.amount) return;

			fluidInventory.fill(stack, true);
		}

		public void refundItems(boolean drop) {
			var node = getProxy().getNode();
			if (node == null) return;

			var grid = node.getGrid();
			if (grid == null) return;

			IStorageGrid storage = grid.getCache(IStorageGrid.class);
			if (storage == null) return;

			IMEInventory<IAEItemStack> meInventory = storage.getInventory(getStorageChannel());
			if (meInventory == null) return;

			for (int i = 0; i < itemInventory.getSlots(); ++i) {
				var stack = itemInventory.getStackInSlot(i);

				if (stack == null || stack.isEmpty()) continue;

				IAEItemStack remainder = meInventory.injectItems(
						AEItemStack.fromItemStack(stack), Actionable.MODULATE, getActionSource());
				if (remainder == null || remainder.getStackSize() <= 0) {
					itemInventory.setStackInSlot(i, ItemStack.EMPTY);
				} else {
					if (!drop) {
						itemInventory.setStackInSlot(i, remainder.asItemStackRepresentation());
						continue;
					}

					itemInventory.setStackInSlot(i, ItemStack.EMPTY);
					Block.spawnAsEntity(getWorld(), getPos(), remainder.asItemStackRepresentation());
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

			for (var tank : fluidInventory.getFluidTanks()) {
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
			var data = new NBTTagCompound();
			data.setTag("Items", itemInventory.serializeNBT());
			data.setTag("Fluids", fluidInventory.serializeNBT());
			var ghostData = new NBTTagCompound();
			ghostCircuit.write(ghostData);
			data.setTag("GhostCircuit", ghostData);
			return data;
		}

		@Override
		public void deserializeNBT(NBTTagCompound data) {
			if (data.hasKey("Items", Constants.NBT.TAG_COMPOUND)) {
				var inventory = data.getCompoundTag("Items");
				dualHandler.setSize(inventory.getInteger("Size"));
				itemInventory.deserializeNBT(inventory);
			}
			if (data.hasKey("Fluids", Constants.NBT.TAG_COMPOUND)) {
				fluidInventory.deserializeNBT(data.getCompoundTag("Fluids"));
			}
			if (data.hasKey("GhostCircuit", Constants.NBT.TAG_COMPOUND)) {
				ghostCircuit.read(data.getCompoundTag("GhostCircuit"));
			}
		}

		public PopupPanel buildUI(PanelSyncManager syncManager, int slot) {
			int slots = inventory().getSlots();
			int slotsPerRow = (int) Math.ceil(Math.sqrt(slots));

			int tanks = fluidInventory().getTanks();
			int tanksPerRow = (int) Math.ceil(Math.sqrt(tanks));

			int width = 50 + Math.max(20, slotsPerRow * 18 + 22 + tanksPerRow * 18);
			int height = 22 + Math.max(20, 18 * Math.max(slotsPerRow, tanksPerRow));

			return (PopupPanel) GTGuis.createPopupPanel("buffer_contents#" + slot, width, height)
					.child(IKey.lang("gregifiedenergistics.gui.buffer_contents", slot)
							.asWidget()
							.horizontalCenter())
					.child(Flow.row()
							.coverChildrenWidth()
							.horizontalCenter()
							.childPadding(8)
							.child(new Grid()
									.minColWidth(18)
									.minRowHeight(18)
									.coverChildren()
									.mapTo(slotsPerRow, slots, index -> new ItemSlot().slot(inventory(), index)))
							.child(new Grid()
									.minColWidth(18)
									.minRowHeight(18)
									.coverChildren()
									.mapTo(
											tanksPerRow,
											fluidInventory().getFluidTanks(),
											(index, tank) -> new GTFluidSlot().syncHandler(tank)))
							.child(new GhostCircuitSlotWidget().slot(ghostCircuit, 0)));
		}
	}
}
