package com.walhay.gregifiedenergistics.common.metatileentities.multiblockparts;

import appeng.api.networking.crafting.ICraftingPatternDetails;
import appeng.api.networking.crafting.ICraftingProviderHelper;
import appeng.api.storage.channels.IItemStorageChannel;
import appeng.api.storage.data.IAEItemStack;
import appeng.items.misc.ItemEncodedPattern;
import codechicken.lib.raytracer.CuboidRayTraceResult;
import com.walhay.gregifiedenergistics.api.capability.AbstractPatternItemHandler;
import com.walhay.gregifiedenergistics.api.gui.CircuitSlotWidget;
import com.walhay.gregifiedenergistics.api.gui.GregifiedEnergisticsGuiTextures;
import com.walhay.gregifiedenergistics.api.metatileentity.MetaTileEntityCraftingProvider;
import gregtech.api.GTValues;
import gregtech.api.capability.GregtechDataCodes;
import gregtech.api.capability.IGhostSlotConfigurable;
import gregtech.api.capability.impl.GhostCircuitItemStackHandler;
import gregtech.api.capability.impl.ItemHandlerList;
import gregtech.api.capability.impl.NotifiableItemStackHandler;
import gregtech.api.gui.GuiTextures;
import gregtech.api.gui.ModularUI;
import gregtech.api.gui.resources.TextureArea;
import gregtech.api.gui.widgets.SlotWidget;
import gregtech.api.gui.widgets.ToggleButtonWidget;
import gregtech.api.items.itemhandlers.GTItemStackHandler;
import gregtech.api.metatileentity.MetaTileEntity;
import gregtech.api.metatileentity.interfaces.IGregTechTileEntity;
import gregtech.api.metatileentity.multiblock.AbilityInstances;
import gregtech.api.metatileentity.multiblock.IMultiblockAbilityPart;
import gregtech.api.metatileentity.multiblock.MultiblockAbility;
import gregtech.api.metatileentity.multiblock.MultiblockControllerBase;
import gregtech.api.util.GTHashMaps;
import gregtech.api.util.GTTransferUtils;
import gregtech.common.metatileentities.multi.multiblockpart.MetaTileEntityItemBus;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import java.io.IOException;
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
	private final SinglePatternHandler patternHandler;
	private ItemHandlerList actualImportItems;
	private boolean workingEnabled = true;
	private boolean autoCollapse = false;

	public MTEMEPatternProvider(ResourceLocation metaTileEntityId, int tier) {
		super(metaTileEntityId, tier, false, IItemStorageChannel.class);
		this.patternHandler = new SinglePatternHandler();
	}

	@Override
	public void setWorkingEnabled(boolean workingEnabled) {
		this.workingEnabled = workingEnabled;
		var world = getWorld();
		if (world != null && !world.isRemote) {
			writeCustomData(GregtechDataCodes.WORKING_ENABLED, buf -> buf.writeBoolean(workingEnabled));
			markDirty();
		}
		notifyPatternChange();
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

		if (isAutoCollapse()) {
			if (!isAttachedToMultiBlock() || this.getNotifiedItemInputList().contains(importItems)) {
				collapseInventorySlotContents(importItems);
			}
		}
	}

	@Override
	public MetaTileEntity createMetaTileEntity(IGregTechTileEntity holder) {
		return new MTEMEPatternProvider(metaTileEntityId, getTier());
	}

	@Override
	public void provideCrafting(ICraftingProviderHelper provider) {
		if (isWorkingEnabled()) {
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
		if (!isWorkingEnabled() || !patternHandler.getPatterns().contains(pattern)) return false;

		int size = inventory.getSizeInventory();
		List<ItemStack> items = new ArrayList<>(size);

		for (int i = 0; i < size; ++i) {
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
	public void clearMachineInventory(@NotNull List<@NotNull ItemStack> itemBuffer) {
		super.clearMachineInventory(itemBuffer);
		clearInventory(itemBuffer, patternHandler);
	}

	@Override
	public void writeInitialSyncData(PacketBuffer buf) {
		super.writeInitialSyncData(buf);
		buf.writeBoolean(workingEnabled);
		buf.writeBoolean(autoCollapse);
		buf.writeCompoundTag(patternHandler.serializeNBT());
	}

	@Override
	public void receiveInitialSyncData(PacketBuffer buf) {
		super.receiveInitialSyncData(buf);
		this.workingEnabled = buf.readBoolean();
		this.autoCollapse = buf.readBoolean();
		try {
			NBTTagCompound patterns = buf.readCompoundTag();
			if (patterns != null) patternHandler.deserializeNBT(patterns);
		} catch (IOException e) {
			throw new IllegalArgumentException("Invalid pattern provider sync data", e);
		}
	}

	@Override
	public NBTTagCompound writeToNBT(NBTTagCompound data) {
		super.writeToNBT(data);
		data.setBoolean("WorkingEnabled", workingEnabled);
		data.setBoolean("AutoCollapse", autoCollapse);
		data.setTag("PatternInventory", this.patternHandler.serializeNBT());
		this.circuitInventory.write(data);
		return data;
	}

	@Override
	public void readFromNBT(NBTTagCompound data) {
		super.readFromNBT(data);
		if (data.hasKey("WorkingEnabled")) {
			this.workingEnabled = data.getBoolean("WorkingEnabled");
		}
		if (data.hasKey("AutoCollapse")) {
			this.autoCollapse = data.getBoolean("AutoCollapse");
		}
		if (data.hasKey("PatternInventory")) {
			this.patternHandler.deserializeNBT(data.getCompoundTag("PatternInventory"));
		}
		if (this.circuitInventory != null) {
			this.circuitInventory.read(data);
		}
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
	protected ModularUI createUI(EntityPlayer player) {
		int rowSize = (int) Math.sqrt(getInventorySize());
		int backgroundWidth = Math.max(199, rowSize * 18 + 14);
		int backgroundHeight = 18 + 18 * rowSize + 94;
		ModularUI.Builder builder = ModularUI.builder(GuiTextures.BACKGROUND, backgroundWidth, backgroundHeight)
				.label(7, 5, getMetaFullName())
				.bindPlayerInventory(player.inventory, GuiTextures.SLOT, 7, backgroundHeight - 83);
		int startX = (backgroundWidth - rowSize * 18) / 2;
		for (int index = 0; index < importItems.getSlots(); index++) {
			int slot = index;
			builder.widget(new SlotWidget(importItems, slot, startX + index % rowSize * 18, 18 + index / rowSize * 18)
					.setBackgroundTexture(GuiTextures.SLOT)
					.setChangeListener(() -> {
						if (importItems instanceof GTItemStackHandler handler) handler.onContentsChanged(slot);
					}));
		}
		int buttonX = backgroundWidth - 25;
		int buttonY = backgroundHeight - 84;
		return builder.widget(new SlotWidget(patternHandler, 0, buttonX, buttonY)
						.setBackgroundTexture(GuiTextures.SLOT, GregifiedEnergisticsGuiTextures.PATTERN_OVERLAY)
						.setChangeListener(() -> patternHandler.onContentsChanged(0)))
				.widget(new CircuitSlotWidget(circuitInventory, buttonX, buttonY + 18))
				.widget(new ToggleButtonWidget(
								buttonX,
								buttonY + 36,
								18,
								18,
								TextureArea.fullImage("textures/gui/widget/button_auto_collapse_overlay.png"),
								this::isAutoCollapse,
								this::setAutoCollapse)
						.shouldUseBaseBackground()
						.setTooltipText("gregtech.gui.item_auto_collapse.tooltip"))
				.image(buttonX, buttonY + 54, 18, 6, GuiTextures.BUTTON_POWER_DETAIL)
				.widget(new ToggleButtonWidget(
								buttonX,
								buttonY + 59,
								18,
								18,
								GuiTextures.BUTTON_POWER,
								this::isWorkingEnabled,
								this::setWorkingEnabled)
						.setTooltipText("gregifiedenergistics.gui.working"))
				.build(getHolder(), player);
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
		tooltip.add(I18n.format("gregifiedenergistics.machine.me_pattern_provider.tooltip"));
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
			super(MTEMEPatternProvider.this, 1);
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
