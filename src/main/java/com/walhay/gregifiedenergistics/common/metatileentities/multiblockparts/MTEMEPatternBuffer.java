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
import com.glodblock.github.common.item.fake.FakeFluids;
import com.glodblock.github.common.item.fake.FakeItemRegister;
import com.walhay.gregifiedenergistics.api.capability.AbstractPatternItemHandler;
import com.walhay.gregifiedenergistics.api.capability.InfiniteItemStackHandler;
import com.walhay.gregifiedenergistics.api.capability.PatternBufferDualHandler;
import com.walhay.gregifiedenergistics.api.gui.CircuitSlotWidget;
import com.walhay.gregifiedenergistics.api.gui.GregifiedEnergisticsGuiTextures;
import com.walhay.gregifiedenergistics.api.gui.PageWidgetGroup;
import com.walhay.gregifiedenergistics.api.metatileentity.MetaTileEntityCraftingProvider;
import com.walhay.gregifiedenergistics.api.patterns.implementations.GhostCircuitPatternWrapper;
import com.walhay.gregifiedenergistics.api.util.FluidCraftingUtils;
import com.walhay.gregifiedenergistics.common.gui.PatternBufferContentsWidget;
import gregtech.api.capability.IMultipleTankHandler;
import gregtech.api.capability.impl.FluidTankList;
import gregtech.api.capability.impl.GhostCircuitItemStackHandler;
import gregtech.api.capability.impl.ItemHandlerList;
import gregtech.api.gui.GuiTextures;
import gregtech.api.gui.IRenderContext;
import gregtech.api.gui.ModularUI;
import gregtech.api.gui.widgets.ClickButtonWidget;
import gregtech.api.gui.widgets.LabelWidget;
import gregtech.api.gui.widgets.SlotWidget;
import gregtech.api.gui.widgets.ToggleButtonWidget;
import gregtech.api.gui.widgets.WidgetGroup;
import gregtech.api.metatileentity.MetaTileEntity;
import gregtech.api.metatileentity.interfaces.IGregTechTileEntity;
import gregtech.api.metatileentity.multiblock.AbilityInstances;
import gregtech.api.metatileentity.multiblock.IMultiblockAbilityPart;
import gregtech.api.metatileentity.multiblock.MultiblockAbility;
import gregtech.api.metatileentity.multiblock.MultiblockControllerBase;
import gregtech.api.util.GTTransferUtils;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;
import net.minecraft.block.Block;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;
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
	public void addInformation(
			ItemStack stack, @Nullable World player, @NotNull List<String> tooltip, boolean advanced) {
		super.addInformation(stack, player, tooltip, advanced);
		tooltip.add(I18n.format("gregifiedenergistics.machine.me_pattern_buffer.tooltip"));
	}

	@Override
	public IItemHandlerModifiable getImportItems() {
		return new ItemHandlerList(patternHandler.getContainers().stream()
				.map(PatternContainer::dualInventory)
				.collect(Collectors.toList()));
	}

	@Override
	public void addToMultiBlock(MultiblockControllerBase controller) {
		super.addToMultiBlock(controller);
		for (var container : patternHandler) {
			container.notifyController(controller);
		}
	}

	@Override
	public void removeFromMultiBlock(MultiblockControllerBase controller) {
		super.removeFromMultiBlock(controller);
		for (var container : patternHandler) {
			container.clearControllerNotifications(controller);
		}
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
			if (getWorld() != null && !getWorld().isRemote) markDirty();
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

			if (stack.isEmpty()) continue;

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
	protected ModularUI createUI(EntityPlayer player) {
		PageWidgetGroup pages = new PageWidgetGroup(212, 138);
		WidgetGroup patterns = new WidgetGroup(0, 0, 212, 138);
		patterns.addWidget(new LabelWidget(7, 7, "gregifiedenergistics.gui.patterns_grid"));
		for (int slot = 0; slot < patternHandler.getSlots(); slot++) {
			int index = slot;
			patterns.addWidget(
					new SlotWidget(patternHandler, index, 25 + index % 9 * 18, 24 + index / 9 * 18) {
						private boolean hovered;

						@Override
						public void drawInBackground(
								int mouseX, int mouseY, float partialTicks, IRenderContext context) {
							hovered = isMouseOverElement(mouseX, mouseY);
							super.drawInBackground(mouseX, mouseY, partialTicks, context);
						}

						@Override
						public boolean keyTyped(char typedChar, int keyCode) {
							if (hovered && keyCode == Keyboard.KEY_B) {
								pages.selectPage(index + 1);
								return true;
							}
							return false;
						}
					}.setBackgroundTexture(GuiTextures.SLOT, GregifiedEnergisticsGuiTextures.PATTERN_OVERLAY)
							.setChangeListener(() -> patternHandler.onContentsChanged(index)));
		}
		patterns.addWidget(new LabelWidget(7, 108, "gregifiedenergistics.gui.buffer_hint"));
		pages.addPage(patterns);
		for (int index = 0; index < patternHandler.getSlots(); index++) {
			PatternContainer container = patternHandler.getContainers().get(index);
			WidgetGroup detail = new WidgetGroup(0, 0, 212, 138);
			detail.addWidget(
					new LabelWidget(7, 9, "gregifiedenergistics.gui.buffer_contents", new Object[] {index + 1}));
			detail.addWidget(new CircuitSlotWidget(container.circuitInventory, 164, 5));
			detail.addWidget(new ClickButtonWidget(187, 5, 18, 18, "<", click -> pages.selectPage(0))
					.setTooltipText("gregifiedenergistics.gui.back"));
			detail.addWidget(new PatternBufferContentsWidget(
					container.itemInventory,
					() -> container.fluidInventory,
					() -> container.revision,
					7,
					28,
					198,
					106));
			pages.addPage(detail);
		}
		return ModularUI.builder(GuiTextures.BACKGROUND, 212, 228)
				.widget(pages)
				.bindPlayerInventory(player.inventory, GuiTextures.SLOT, 7, 145)
				.widget(new ToggleButtonWidget(
								187,
								203,
								18,
								18,
								GuiTextures.BUTTON_POWER,
								this::isWorkingEnabled,
								this::setWorkingEnabled)
						.setTooltipText("gregifiedenergistics.gui.working"))
				.build(getHolder(), player);
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
		data.setBoolean("WorkingEnabled", isWorkingEnabled);

		return data;
	}

	@Override
	public void readFromNBT(NBTTagCompound data) {
		super.readFromNBT(data);
		if (data.hasKey("WorkingEnabled")) isWorkingEnabled = data.getBoolean("WorkingEnabled");
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

		private final ItemStackHandler itemInventory;
		private final GhostCircuitItemStackHandler circuitInventory;
		private final PatternBufferDualHandler dualHandler;
		private FluidTankList fluidInventory;

		private int revision;

		public PatternContainer() {
			this.itemInventory = new InfiniteItemStackHandler(0) {
				@Override
				protected void onContentsChanged(int slot) {
					super.onContentsChanged(slot);
					PatternContainer.this.dualHandler.onContentsChanged();
				}
			};

			this.circuitInventory = new GhostCircuitItemStackHandler(MTEMEPatternBuffer.this) {
				@Override
				public void setCircuitValue(int config) {
					super.setCircuitValue(config);

					PatternContainer.this.dualHandler.onContentsChanged();
				}
			};

			this.fluidInventory = new FluidTankList(false);

			this.dualHandler = new PatternBufferDualHandler(itemInventory, circuitInventory, fluidInventory);
			this.circuitInventory.addNotifiableMetaTileEntity(MTEMEPatternBuffer.this);
		}

		private void notifyController(MultiblockControllerBase controller) {
			this.dualHandler.addToNotifiedList(controller, this.dualHandler, false);
		}

		private void clearControllerNotifications(MultiblockControllerBase controller) {
			controller.getNotifiedItemInputList().remove(this.dualHandler);
			controller.getNotifiedFluidInputList().remove(this.dualHandler);
		}

		public void setPattern(ICraftingPatternDetails pattern) {

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

			List<FluidTank> fluidTanks = new ArrayList<>(fluids);
			for (int i = 0; i < fluids; ++i) {
				fluidTanks.add(new FluidTank(Integer.MAX_VALUE) {
					@Override
					protected void onContentsChanged() {
						super.onContentsChanged();
						PatternContainer.this.dualHandler.onContentsChanged();
					}
				});
			}

			this.fluidInventory = new FluidTankList(false, fluidTanks);
			this.dualHandler.setFluidDelegate(fluidInventory);
			this.dualHandler.onContentsChanged();
			revision++;
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
	}
}
