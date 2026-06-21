package com.walhay.gregifiedenergistics.common.metatileentities.multiblockparts;

import appeng.api.networking.crafting.ICraftingPatternDetails;
import appeng.api.networking.crafting.ICraftingProviderHelper;
import appeng.api.storage.channels.IItemStorageChannel;
import appeng.api.storage.data.IAEItemStack;
import appeng.items.misc.ItemEncodedPattern;
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
import gregtech.api.capability.impl.GhostCircuitItemStackHandler;
import gregtech.api.capability.impl.NotifiableItemStackHandler;
import gregtech.api.items.itemhandlers.GTItemStackHandler;
import gregtech.api.metatileentity.MetaTileEntity;
import gregtech.api.metatileentity.interfaces.IGregTechTileEntity;
import gregtech.api.metatileentity.multiblock.AbilityInstances;
import gregtech.api.metatileentity.multiblock.IMultiblockAbilityPart;
import gregtech.api.metatileentity.multiblock.MultiblockAbility;
import gregtech.api.mui.GTGuiTextures;
import gregtech.api.mui.GTGuis;
import gregtech.api.mui.widget.GhostCircuitSlotWidget;
import gregtech.api.util.GTTransferUtils;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.items.IItemHandlerModifiable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/** MTEMEPatternProvider */
public class MTEMEPatternProvider extends MetaTileEntityCraftingProvider<IAEItemStack>
		implements IMultiblockAbilityPart<IItemHandlerModifiable> {

	private boolean workingEnabled = true;
	private final GhostCircuitItemStackHandler circuitInventory;
	private final SinglePatternHandler patternInventory;
	private boolean autoCollapse = true;

	public MTEMEPatternProvider(ResourceLocation metaTileEntityId, int tier) {
		super(metaTileEntityId, tier, false, IItemStorageChannel.class);
		this.circuitInventory = new GhostCircuitItemStackHandler(this);
		this.patternInventory = new SinglePatternHandler();
	}

	@Override
	public boolean isWorkingEnabled() {
		return workingEnabled;
	}

	@Override
	public void setWorkingEnabled(boolean isWorkingEnabled) {
		if (this.workingEnabled != isWorkingEnabled) {
			this.workingEnabled = isWorkingEnabled;
			notifyPatternChange();
		}
	}

	@Override
	public MetaTileEntity createMetaTileEntity(IGregTechTileEntity handler) {
		return new MTEMEPatternProvider(metaTileEntityId, getTier());
	}

	@Override
	public boolean usesMui2() {
		return true;
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
	protected IItemHandlerModifiable createImportItemHandler() {
		return new NotifiableItemStackHandler(this, getInventorySize(), getController(), false);
	}

	@Override
	public @Nullable MultiblockAbility<IItemHandlerModifiable> getAbility() {
		return MultiblockAbility.IMPORT_ITEMS;
	}

	@Override
	public void registerAbilities(@NotNull AbilityInstances ability) {
		ability.add(importItems);
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

	protected int getInventorySize() {
		int sizeRoot = 1 + Math.min(GTValues.UHV, getTier());
		return sizeRoot * sizeRoot;
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
