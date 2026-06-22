package com.walhay.gregifiedenergistics.common.metatileentities.multiblockparts;

import appeng.api.networking.crafting.ICraftingPatternDetails;
import appeng.api.networking.crafting.ICraftingProviderHelper;
import appeng.api.storage.channels.IItemStorageChannel;
import appeng.api.storage.data.IAEItemStack;
import appeng.items.misc.ItemEncodedPattern;
import codechicken.lib.raytracer.CuboidRayTraceResult;
import com.cleanroommc.modularui.api.drawable.IKey;
import com.cleanroommc.modularui.factory.PosGuiData;
import com.cleanroommc.modularui.screen.ModularPanel;
import com.cleanroommc.modularui.screen.UISettings;
import com.cleanroommc.modularui.value.sync.BooleanSyncValue;
import com.cleanroommc.modularui.value.sync.PanelSyncManager;
import com.cleanroommc.modularui.value.sync.SyncHandlers;
import com.cleanroommc.modularui.widgets.SlotGroupWidget;
import com.cleanroommc.modularui.widgets.ToggleButton;
import com.cleanroommc.modularui.widgets.layout.Flow;
import com.cleanroommc.modularui.widgets.layout.Grid;
import com.cleanroommc.modularui.widgets.slot.ItemSlot;
import com.walhay.gregifiedenergistics.api.capability.AbstractPatternItemHandler;
import com.walhay.gregifiedenergistics.api.metatileentity.MetaTileEntityCraftingProvider;
import gregtech.api.GTValues;
import gregtech.api.capability.GregtechDataCodes;
import gregtech.api.capability.IGhostSlotConfigurable;
import gregtech.api.capability.impl.GhostCircuitItemStackHandler;
import gregtech.api.capability.impl.ItemHandlerList;
import gregtech.api.capability.impl.NotifiableItemStackHandler;
import gregtech.api.items.itemhandlers.GTItemStackHandler;
import gregtech.api.metatileentity.MetaTileEntity;
import gregtech.api.metatileentity.interfaces.IGregTechTileEntity;
import gregtech.api.metatileentity.multiblock.AbilityInstances;
import gregtech.api.metatileentity.multiblock.IMultiblockAbilityPart;
import gregtech.api.metatileentity.multiblock.MultiblockAbility;
import gregtech.api.metatileentity.multiblock.MultiblockControllerBase;
import gregtech.api.mui.GTGuiTextures;
import gregtech.api.mui.GTGuis;
import gregtech.api.mui.widget.GhostCircuitSlotWidget;
import gregtech.api.util.GTHashMaps;
import gregtech.api.util.GTTransferUtils;
import gregtech.common.metatileentities.multi.multiblockpart.MetaTileEntityItemBus;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.world.World;
import net.minecraftforge.items.IItemHandlerModifiable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/** MTEMEPatternProvider */
public class MTEMEPatternProvider extends MetaTileEntityCraftingProvider<IAEItemStack>
		implements IMultiblockAbilityPart<IItemHandlerModifiable>, IGhostSlotConfigurable {

	private GhostCircuitItemStackHandler circuitInventory;
	private final SinglePatternHandler patternInventory;
	private ItemHandlerList actualImportItems;
	private boolean workingEnabled;
	private boolean autoCollapse;

	public MTEMEPatternProvider(ResourceLocation metaTileEntityId, int tier) {
		super(metaTileEntityId, tier, false, IItemStorageChannel.class);
		this.patternInventory = new SinglePatternHandler();
	}

	@Override
	public void setWorkingEnabled(boolean workingEnabled) {
		this.workingEnabled = workingEnabled;
		var world = getWorld();
		if (world != null && !world.isRemote) {
			writeCustomData(GregtechDataCodes.WORKING_ENABLED, buf -> buf.writeBoolean(workingEnabled));
			notifyPatternChange();
		}
	}

	@Override
	public boolean isWorkingEnabled() {
		return workingEnabled;
	}

	@Override
	public void update() {
		super.update();
		if (getWorld().isRemote || getOffsetTimer() % 5 != 0) {
			return;
		}

		if (isWorkingEnabled()) {
			pullItemsFromNearbyHandlers(getFrontFacing());
		}

		if (isAutoCollapse()) {
			if (!isAttachedToMultiBlock() || this.getNotifiedItemInputList().contains(importItems)) {
				collapseInventorySlotContents(importItems);
			}
		}
	}

	@Override
	public MetaTileEntity createMetaTileEntity(IGregTechTileEntity handler) {
		return new MTEMEPatternProvider(metaTileEntityId, getTier());
	}

	@Override
	public void provideCrafting(ICraftingProviderHelper provider) {
		if (isWorkingEnabled()) {
			for (var pattern : patternInventory.getPatterns()) {
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
		List<ItemStack> items = new ArrayList<>(inventory.getSizeInventory());

		for (int i = 0; i < inventory.getSizeInventory(); ++i) {
			items.add(inventory.getStackInSlot(i));
		}

		if (!GTTransferUtils.addItemsToItemHandler(importItems, true, items)) {
			return false;
		}

		GTTransferUtils.addItemsToItemHandler(importItems, false, items);
		return true;
	}

	@Override
	protected void initializeInventory() {
		super.initializeInventory();
		this.circuitInventory = new GhostCircuitItemStackHandler(this);
		this.circuitInventory.addNotifiableMetaTileEntity(this);
		this.actualImportItems = new ItemHandlerList(Arrays.asList(importItems, circuitInventory));
	}

	protected int getInventorySize() {
		int sizeRoot = 1 + Math.min(GTValues.UHV, getTier());
		return sizeRoot * sizeRoot;
	}

	@Override
	protected IItemHandlerModifiable createImportItemHandler() {
		return new NotifiableItemStackHandler(this, getInventorySize(), getController(), false);
	}

	@Override
	public IItemHandlerModifiable getImportItems() {
		return actualImportItems;
	}

	@Override
	public void addToMultiBlock(MultiblockControllerBase controller) {
		super.addToMultiBlock(controller);
		this.circuitInventory.addNotifiableMetaTileEntity(controller);
		this.circuitInventory.addToNotifiedList(this, this.circuitInventory, false);
	}

	@Override
	public void removeFromMultiBlock(MultiblockControllerBase controller) {
		super.removeFromMultiBlock(controller);
		this.circuitInventory.removeNotifiableMetaTileEntity(controller);
	}

	@Override
	public @Nullable MultiblockAbility<IItemHandlerModifiable> getAbility() {
		return MultiblockAbility.IMPORT_ITEMS;
	}

	@Override
	public void registerAbilities(@NotNull AbilityInstances ability) {
		ability.add(actualImportItems);
	}

	@Override
	public void writeInitialSyncData(PacketBuffer buf) {
		super.writeInitialSyncData(buf);
		buf.writeBoolean(workingEnabled);
		buf.writeBoolean(autoCollapse);
	}

	@Override
	public void receiveInitialSyncData(PacketBuffer buf) {
		super.receiveInitialSyncData(buf);
		this.workingEnabled = buf.readBoolean();
		this.autoCollapse = buf.readBoolean();
	}

	@Override
	public NBTTagCompound writeToNBT(NBTTagCompound data) {
		super.writeToNBT(data);
		data.setBoolean("workingEnabled", workingEnabled);
		data.setBoolean("autoCollapse", autoCollapse);
		this.circuitInventory.write(data);
		return data;
	}

	@Override
	public void readFromNBT(NBTTagCompound data) {
		super.readFromNBT(data);
		if (data.hasKey("workingEnabled")) {
			this.workingEnabled = data.getBoolean("workingEnabled");
		}
		if (data.hasKey("autoCollapse")) {
			this.autoCollapse = data.getBoolean("autoCollapse");
		}
		this.circuitInventory.read(data);
	}

	@Override
	public void receiveCustomData(int dataId, PacketBuffer buf) {
		super.receiveCustomData(dataId, buf);
		if (dataId == GregtechDataCodes.TOGGLE_COLLAPSE_ITEMS) {
			this.autoCollapse = buf.readBoolean();
		} else if (dataId == GregtechDataCodes.WORKING_ENABLED) {
			this.workingEnabled = buf.readBoolean();
		}
	}

	@Override
	public boolean usesMui2() {
		return true;
	}

	@Override
	public ModularPanel buildUI(PosGuiData guiData, PanelSyncManager panelSyncManager, UISettings settings) {
		int rowSize = (int) Math.sqrt(getInventorySize());
		panelSyncManager.registerSlotGroup("item_inv", rowSize);

		int backgroundWidth = Math.max(
				9 * 18 + 18 + 14 + 5, // Player Inv width
				rowSize * 18 + 14); // Bus Inv width
		int backgroundHeight = 18 + 18 * rowSize + 94;

		BooleanSyncValue workingStateValue = new BooleanSyncValue(() -> workingEnabled, val -> workingEnabled = val);
		BooleanSyncValue collapseStateValue = new BooleanSyncValue(() -> autoCollapse, val -> autoCollapse = val);

		return GTGuis.createPanel(this, backgroundWidth, backgroundHeight)
				.child(IKey.lang(getMetaFullName()).asWidget().pos(5, 5))
				.child(SlotGroupWidget.playerInventory(false).left(7).bottom(7))
				.child(new Grid()
						.top(18)
						.height(rowSize * 18)
						.minElementMargin(0, 0)
						.minColWidth(18)
						.minRowHeight(18)
						.horizontalCenter()
						.mapTo(rowSize, rowSize * rowSize, index -> new ItemSlot()
								.slot(SyncHandlers.itemSlot(importItems, index)
										.slotGroup("item_inv")
										.changeListener((newItem, onlyAmountChanged, client, init) -> {
											if (onlyAmountChanged
													&& importItems instanceof GTItemStackHandler gtHandler) {
												gtHandler.onContentsChanged(index);
											}
										})
										.accessibility(!isExportHatch, true))))
				.child(Flow.column()
						.pos(backgroundWidth - 7 - 18, backgroundHeight - 18 * 4 - 7 - 5)
						.width(18)
						.height(18 * 5 + 5)
						.child(GTGuiTextures.getLogo(getUITheme())
								.asWidget()
								.size(17)
								.top(18 * 3 + 5))
						.child(new ToggleButton()
								.top(18 * 3)
								.value(workingStateValue)
								.overlay(GTGuiTextures.BUTTON_ITEM_OUTPUT)
								.tooltipAutoUpdate(true)
								.tooltipBuilder(t -> t.addLine(
										isExportHatch
												? (workingStateValue.getBoolValue()
														? IKey.lang("gregtech.gui.item_auto_output.tooltip.enabled")
														: IKey.lang("gregtech.gui.item_auto_output.tooltip.disabled"))
												: (workingStateValue.getBoolValue()
														? IKey.lang("gregtech.gui.item_auto_input.tooltip.enabled")
														: IKey.lang("gregtech.gui.item_auto_input.tooltip.disabled")))))
						.child(new ToggleButton()
								.top(18 * 2)
								.value(collapseStateValue)
								.overlay(GTGuiTextures.BUTTON_AUTO_COLLAPSE)
								.tooltipAutoUpdate(true)
								.tooltipBuilder(t -> t.addLine(
										collapseStateValue.getBoolValue()
												? IKey.lang("gregtech.gui.item_auto_collapse.tooltip.enabled")
												: IKey.lang("gregtech.gui.item_auto_collapse.tooltip.disabled"))))
						.child(new GhostCircuitSlotWidget()
								.slot(circuitInventory, 0)
								.background(GTGuiTextures.SLOT, GTGuiTextures.INT_CIRCUIT_OVERLAY))
						.child(new ItemSlot().slot(patternInventory, 0).top(18).background(GTGuiTextures.SLOT)));
	}

	/** Exact copy of {@link MetaTileEntityItemBus#collapseInventorySlotContents(IItemHandlerModifiable)} */
	private static void collapseInventorySlotContents(IItemHandlerModifiable inventory) {
		// Gather a snapshot of the provided inventory
		Object2IntMap<ItemStack> inventoryContents = GTHashMaps.fromItemHandler(inventory, true);

		List<ItemStack> inventoryItemContents = new ArrayList<>();

		// Populate the list of item stacks in the inventory with apportioned item stacks, for easy replacement
		for (Object2IntMap.Entry<ItemStack> e : inventoryContents.object2IntEntrySet()) {
			ItemStack stack = e.getKey();
			int count = e.getIntValue();
			int maxStackSize = stack.getMaxStackSize();
			while (count >= maxStackSize) {
				ItemStack copy = stack.copy();
				copy.setCount(maxStackSize);
				inventoryItemContents.add(copy);
				count -= maxStackSize;
			}
			if (count > 0) {
				ItemStack copy = stack.copy();
				copy.setCount(count);
				inventoryItemContents.add(copy);
			}
		}

		for (int i = 0; i < inventory.getSlots(); i++) {
			ItemStack stackToMove;
			// Ensure that we are not exceeding the List size when attempting to populate items
			if (i >= inventoryItemContents.size()) {
				stackToMove = ItemStack.EMPTY;
			} else {
				stackToMove = inventoryItemContents.get(i);
			}

			// Populate the slots
			inventory.setStackInSlot(i, stackToMove);
		}
	}

	@Override
	public boolean onScrewdriverClick(
			EntityPlayer playerIn, EnumHand hand, EnumFacing facing, CuboidRayTraceResult hitResult) {
		setAutoCollapse(!this.autoCollapse);

		if (!getWorld().isRemote) {
			if (this.autoCollapse) {
				playerIn.sendStatusMessage(new TextComponentTranslation("gregtech.bus.collapse_true"), true);
			} else {
				playerIn.sendStatusMessage(new TextComponentTranslation("gregtech.bus.collapse_false"), true);
			}
		}
		return true;
	}

	public boolean isAutoCollapse() {
		return autoCollapse;
	}

	public void setAutoCollapse(boolean inverted) {
		autoCollapse = inverted;
		if (!getWorld().isRemote) {
			if (autoCollapse) {
				addNotifiedInput(super.getImportItems());
			}
			writeCustomData(
					GregtechDataCodes.TOGGLE_COLLAPSE_ITEMS, packetBuffer -> packetBuffer.writeBoolean(autoCollapse));
			notifyBlockUpdate();
			markDirty();
		}
	}

	@Override
	public boolean hasGhostCircuitInventory() {
		return true;
	}

	@Override
	public void setGhostCircuitConfig(int config) {
		if (this.circuitInventory == null || this.circuitInventory.getCircuitValue() == config) {
			return;
		}
		this.circuitInventory.setCircuitValue(config);
		if (!getWorld().isRemote) {
			markDirty();
		}
	}

	@Override
	public void addInformation(
			ItemStack stack, @Nullable World player, @NotNull List<String> tooltip, boolean advanced) {
		tooltip.add(I18n.format("gregtech.machine.item_bus.import.tooltip"));
		tooltip.add(I18n.format("gregtech.universal.tooltip.item_storage_capacity", getInventorySize()));
		tooltip.add(I18n.format("gregtech.universal.enabled"));
	}

	@Override
	public void addToolUsages(ItemStack stack, @Nullable World world, List<String> tooltip, boolean advanced) {
		tooltip.add(I18n.format("gregtech.tool_action.screwdriver.access_covers"));
		tooltip.add(I18n.format("gregtech.tool_action.screwdriver.auto_collapse"));
		tooltip.add(I18n.format("gregtech.tool_action.wrench.set_facing"));
		super.addToolUsages(stack, world, tooltip, advanced);
	}

	private class SinglePatternHandler extends AbstractPatternItemHandler {

		public SinglePatternHandler() {
			super(1);
		}

		@Override
		protected ICraftingPatternDetails getPatternFromStack(ItemStack stack) {
			if (stack.getItem() instanceof ItemEncodedPattern encodedPattern) {
				return encodedPattern.getPatternForItem(stack, getWorld());
			}
			return null;
		}

		@Override
		protected void onPatternUpdate() {
			notifyPatternChange();
		}
	}
}
