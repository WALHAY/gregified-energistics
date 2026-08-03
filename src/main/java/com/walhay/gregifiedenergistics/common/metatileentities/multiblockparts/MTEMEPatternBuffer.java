package com.walhay.gregifiedenergistics.common.metatileentities.multiblockparts;

import static gregtech.api.GTValues.ZPM;

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
import com.cleanroommc.modularui.factory.PosGuiData;
import com.cleanroommc.modularui.screen.ModularPanel;
import com.cleanroommc.modularui.screen.RichTooltip;
import com.cleanroommc.modularui.screen.UISettings;
import com.cleanroommc.modularui.value.sync.DynamicSyncHandler;
import com.cleanroommc.modularui.value.sync.FluidSlotSyncHandler;
import com.cleanroommc.modularui.value.sync.ItemSlotSH;
import com.cleanroommc.modularui.value.sync.PanelSyncManager;
import com.cleanroommc.modularui.value.sync.SyncHandlers;
import com.cleanroommc.modularui.widget.Widget;
import com.cleanroommc.modularui.widgets.DynamicSyncedWidget;
import com.cleanroommc.modularui.widgets.SlotGroupWidget;
import com.cleanroommc.modularui.widgets.layout.Flow;
import com.cleanroommc.modularui.widgets.layout.Grid;
import com.cleanroommc.modularui.widgets.slot.FluidSlot;
import com.cleanroommc.modularui.widgets.slot.ItemSlot;
import com.cleanroommc.modularui.widgets.slot.ModularSlot;
import com.glodblock.github.common.item.fake.FakeFluids;
import com.glodblock.github.common.item.fake.FakeItemRegister;
import com.walhay.gregifiedenergistics.api.capability.AbstractPatternItemHandler;
import com.walhay.gregifiedenergistics.api.capability.InfiniteItemStackHandler;
import com.walhay.gregifiedenergistics.api.capability.PatternBufferDualHandler;
import com.walhay.gregifiedenergistics.api.capability.SegmentItemHandlerList;
import com.walhay.gregifiedenergistics.api.metatileentity.MetaTileEntityCraftingProvider;
import com.walhay.gregifiedenergistics.api.mui.GregifiedEnergisticsGuiTextures;
import com.walhay.gregifiedenergistics.api.patterns.implementations.GhostCircuitPatternWrapper;
import com.walhay.gregifiedenergistics.api.util.FluidCraftingUtils;
import gregtech.api.capability.IMultipleTankHandler;
import gregtech.api.capability.impl.FluidTankList;
import gregtech.api.capability.impl.GhostCircuitItemStackHandler;
import gregtech.api.capability.impl.ItemHandlerList;
import gregtech.api.metatileentity.MetaTileEntity;
import gregtech.api.metatileentity.interfaces.IGregTechTileEntity;
import gregtech.api.metatileentity.multiblock.AbilityInstances;
import gregtech.api.metatileentity.multiblock.IMultiblockAbilityPart;
import gregtech.api.metatileentity.multiblock.MultiblockAbility;
import gregtech.api.metatileentity.multiblock.RecipeMapMultiblockController;
import gregtech.api.mui.GTGuiTextures;
import gregtech.api.mui.GTGuis;
import gregtech.api.mui.GTGuis.PopupPanel;
import gregtech.api.util.GTTransferUtils;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;
import net.minecraft.block.Block;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.util.Constants;
import net.minecraftforge.common.util.Constants.NBT;
import net.minecraftforge.common.util.INBTSerializable;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidTank;
import net.minecraftforge.items.IItemHandlerModifiable;
import net.minecraftforge.items.ItemStackHandler;
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
	public IItemHandlerModifiable getImportItems() {
		return new ItemHandlerList(patternHandler.getContainers().stream()
				.map(PatternContainer::dualInventory)
				.collect(Collectors.toList()));
	}

	@Override
	public void clearMachineInventory(@NotNull List<@NotNull ItemStack> itemBuffer) {
		super.clearMachineInventory(itemBuffer);
		clearInventory(itemBuffer, patternHandler);
		for (var container : patternHandler) {
			clearInventory(itemBuffer, container.inventory());
		}
	}

	@Override
	public void onRemoval() {
		super.onRemoval();
		for (var container : patternHandler) {
			container.refundItems(false);
			container.refundFluids();
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
		return false;
	}

	@Override
	public boolean pushPattern(ICraftingPatternDetails pattern, InventoryCrafting inventory) {
		if (!isWorkingEnabled() || !patternHandler.contains(pattern)) return false;

		var container = patternHandler.getContainer(pattern);
		if (container == null) return false;

		for (int i = 0; i < inventory.getSizeInventory(); ++i) {
			var stack = inventory.getStackInSlot(i);

			if (stack == null || stack.isEmpty()) continue;

			if (FakeFluids.isFluidFakeItem(stack)) {
				FluidStack fluidStack = FakeItemRegister.getStack(stack);
				if (fluidStack == null) continue;

				container.insertFluid(fluidStack);
				continue;
			}

			container.insertItem(stack);
		}

		return true;
	}

	@Override
	public boolean usesMui2() {
		return true;
	}

	@Override
	public ModularPanel buildUI(PosGuiData guiData, PanelSyncManager syncManager, UISettings settings) {
		syncManager.registerSlotGroup("pattern_inv", 9);

		return GTGuis.createPanel(this, 176, 166)
				.child(SlotGroupWidget.builder()
						.slotGroup("pattern_inv")
						.matrix("IIIIIIIII", "IIIIIIIII", "IIIIIIIII", "IIIIIIIII")
						.key(
								'I',
								index -> new ItemSlot() {
									private final IPanelHandler panel =
											syncManager.syncedPanel("buffer#" + index, true, (sh, ph) -> patternHandler
													.getContainers()
													.get(index)
													.buildUI(sh, index));

									@Override
									public @NotNull Result onKeyPressed(char typedChar, int keyCode) {
										if (keyCode == Keyboard.KEY_B) {
											panel.togglePanel();
											return Result.SUCCESS;
										}
										return Result.ACCEPT;
									}

									@Override
									public void buildTooltip(ItemStack stack, RichTooltip tooltip) {
										super.buildTooltip(stack, tooltip);
										PatternContainer container =
												patternHandler.getContainers().get(index);

										for (int i = 0;
												i < container.inventory().getSlots();
												++i) {
											tooltip.addFromItem(
													container.inventory().getStackInSlot(i));
										}
									}
								}.slot(SyncHandlers.itemSlot(patternHandler, index)
												.changeListener((newItem, onlyAmountChanged, client, init) ->
														patternHandler.onContentsChanged(index)))
										.background(
												GTGuiTextures.SLOT,
												GregifiedEnergisticsGuiTextures.PATTERN_OVERLAY
														.asIcon()
														.size(16)))
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
	public @Nullable MultiblockAbility<IItemHandlerModifiable> getAbility() {
		return MultiblockAbility.IMPORT_ITEMS;
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
			super(MTEMEPatternBuffer.this, size);
			this.containers = new PatternContainer[size];
			for (int i = 0; i < size; ++i) {
				this.containers[i] = new PatternContainer(i);
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

		private void attachPatternToContainer(int slot, @Nullable ICraftingPatternDetails pattern) {
			validateSlotIndex(slot);

			var container = containers[slot];
			container.setPattern(pattern);
			if (pattern != null) {
				patternToContainer.put(pattern, container);
			}
		}

		private void detachPatternFromContainer(int slot, @Nullable ICraftingPatternDetails pattern) {
			validateSlotIndex(slot);

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

				attachPatternToContainer(i, pattern);
			}
		}

		@Override
		protected ICraftingPatternDetails getPatternFromStack(ItemStack stack) {
			if (stack.getItem() instanceof ItemEncodedPattern encodedPattern) {
				return GhostCircuitPatternWrapper.wrap(encodedPattern.getPatternForItem(stack, getWorld()));
			}
			return null;
		}

		@Override
		public void onContentsChanged(int slot) {
			var previous = getPatternDetails(slot);
			super.onContentsChanged(slot);
			var current = getPatternDetails(slot);

			if (previous == current) return;

			detachPatternFromContainer(slot, previous);

			if (current != null) {
				attachPatternToContainer(slot, current);
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

		@Override
		public NBTTagCompound serializeNBT() {
			var nbt = super.serializeNBT();

			var list = new NBTTagList();
			for (int i = 0; i < containers.length; ++i) {
				var entry = new NBTTagCompound();

				entry.setInteger("Slot", i);
				entry.setTag("Data", containers[i].serializeNBT());

				list.appendTag(entry);
			}

			nbt.setTag("Containers", list);
			return nbt;
		}

		@Override
		public void deserializeNBT(NBTTagCompound nbt) {
			super.deserializeNBT(nbt);

			var list = nbt.getTagList("Containers", NBT.TAG_COMPOUND);
			for (int i = 0; i < list.tagCount(); ++i) {
				var entry = list.getCompoundTagAt(i);
				if (!entry.hasKey("Slot", Constants.NBT.TAG_INT)) continue;

				int slot = entry.getInteger("Slot");
				if (slot < 0 || slot >= containers.length) continue;

				if (!entry.hasKey("Data", Constants.NBT.TAG_COMPOUND)) continue;

				containers[slot].deserializeNBT(entry.getCompoundTag("Data"));
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

		private ItemStackHandler itemInventory;
		private GhostCircuitItemStackHandler circuitInventory;
		private PatternBufferDualHandler dualHandler;
		private FluidTankList fluidInventory;

		private DynamicSyncHandler dsh;

		public PatternContainer(int index) {
			this.dsh = new DynamicSyncHandler();
			this.itemInventory = new InfiniteItemStackHandler(0);

			this.circuitInventory = new GhostCircuitItemStackHandler(MTEMEPatternBuffer.this) {
				@Override
				public void setCircuitValue(int config) {
					super.setCircuitValue(config);

					PatternContainer.this.dualHandler.onContentsChanged();
				}
			};

			this.fluidInventory = new FluidTankList(false);

			this.dualHandler = new PatternBufferDualHandler(itemInventory, circuitInventory, fluidInventory);
		}

		public void setPattern(ICraftingPatternDetails pattern) {
			this.pattern = pattern;

			int fluids = 0;
			int items = 0;
			int circuitValue = -1;
			if (pattern != null && pattern.getCondensedInputs() != null) {
				fluids = FluidCraftingUtils.getFluids(pattern.getCondensedInputs())
						.size();

				items = pattern.getCondensedInputs().length - fluids;
			}

			if (pattern instanceof GhostCircuitPatternWrapper wrapper) {
				circuitValue = wrapper.getCircuitValue();
			}

			this.circuitInventory.setCircuitValue(circuitValue);
			this.itemInventory.setSize(items);
			this.dualHandler.onResize();

			if (getController() instanceof RecipeMapMultiblockController controller) {
				if (controller.getInputInventory() instanceof SegmentItemHandlerList seg) {
					seg.onHandlerChange(dualHandler);
				}
			}

			List<FluidTank> fluidTanks = new ArrayList<>(fluids);
			for (int i = 0; i < fluids; ++i) {
				fluidTanks.add(new FluidTank(Integer.MAX_VALUE));
			}

			this.fluidInventory = new FluidTankList(false, fluidTanks);
			this.dualHandler.setFluidDelegate(fluidInventory);

			this.dsh.notifyUpdate(buf -> buf.writeInt(itemInventory.getSlots()).writeInt(fluidInventory.getTanks()));
		}

		@Nullable public ICraftingPatternDetails getPattern() {
			return pattern;
		}

		public IMultipleTankHandler fluidInventory() {
			return fluidInventory;
		}

		public IItemHandlerModifiable inventory() {
			return itemInventory;
		}

		public IItemHandlerModifiable dualInventory() {
			return dualHandler;
		}

		public void insertItem(ItemStack stack) {
			if (stack == null || stack.isEmpty()) return;

			GTTransferUtils.insertItem(dualHandler, stack, false);
		}

		public void insertFluid(FluidStack stack) {
			if (stack == null || stack.amount <= 0) return;

			int accepted = fluidInventory.fill(stack, false);
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
			circuitInventory.write(ghostData);
			data.setTag("CircuitInventory", ghostData);
			return data;
		}

		@Override
		public void deserializeNBT(NBTTagCompound data) {
			if (data.hasKey("Items", Constants.NBT.TAG_COMPOUND)) {
				var inventory = data.getCompoundTag("Items");
				itemInventory.setSize(inventory.getInteger("Size"));
				dualHandler.onResize();
				itemInventory.deserializeNBT(inventory);
			}
			if (data.hasKey("Fluids", Constants.NBT.TAG_COMPOUND)) {
				fluidInventory.deserializeNBT(data.getCompoundTag("Fluids"));
			}
			if (data.hasKey("CircuitInventory", Constants.NBT.TAG_COMPOUND)) {
				circuitInventory.read(data.getCompoundTag("CircuitInventory"));
			}
		}

		protected Widget<?> contentBuffer(PanelSyncManager syncManager, PacketBuffer buffer) {
			int slots = buffer.readInt();
			int fluids = buffer.readInt();

			Flow row = Flow.row();

			if (slots > 0) {
				row.child(new Grid()
						.minColWidth(18)
						.minRowHeight(18)
						.coverChildren()
						.mapTo(6, slots, slotIndex -> {
							ModularSlot ms = new ModularSlot(itemInventory, slotIndex);

							ItemSlotSH itemSyncHandler = syncManager.getOrCreateSyncHandler(
									"buffer_slot", slotIndex, ItemSlotSH.class, () -> new ItemSlotSH(ms));

							return new ItemSlot().syncHandler(itemSyncHandler).background(GTGuiTextures.SLOT);
						}));
			}

			if (fluids > 0) {
				row.child(new Grid()
						.minColWidth(18)
						.minRowHeight(18)
						.coverChildren()
						.mapTo(6, fluidInventory.getFluidTanks(), (fluidIndex, tank) -> {
							FluidSlotSyncHandler fsh = syncManager.getOrCreateSyncHandler(
									"fluid_slot",
									fluidIndex,
									FluidSlotSyncHandler.class,
									() -> new FluidSlotSyncHandler(tank));

							return new FluidSlot().syncHandler(fsh);
						}));
			}

			return row;
		}

		public PopupPanel buildUI(PanelSyncManager syncManager, int index) {
			this.dsh.widgetProvider(this::contentBuffer);
			this.dsh.notifyUpdate(buf -> buf.writeInt(itemInventory.getSlots()).writeInt(fluidInventory.getTanks()));

			return (PopupPanel) GTGuis.createPopupPanel("buffer#" + index, 180, 140)
					.child(new DynamicSyncedWidget<>().pos(6, 6).syncHandler(this.dsh));
		}
	}
}
