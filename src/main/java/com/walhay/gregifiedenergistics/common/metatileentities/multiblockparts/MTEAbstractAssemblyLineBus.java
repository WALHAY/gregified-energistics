package com.walhay.gregifiedenergistics.common.metatileentities.multiblockparts;

import static com.walhay.gregifiedenergistics.api.mui.GregifiedEnergisticsGuiTextures.BLOCKING_MODE;
import static com.walhay.gregifiedenergistics.api.patterns.substitutions.SubstitutionStorage.STORAGE_TAG;
import static com.walhay.gregifiedenergistics.api.util.BlockingMode.BLOCKING_MODE_TAG;
import static gregtech.api.mui.GTGuiTextures.BUTTON_POWER;

import appeng.api.AEApi;
import appeng.api.config.Actionable;
import appeng.api.networking.crafting.ICraftingPatternDetails;
import appeng.api.networking.crafting.ICraftingProviderHelper;
import appeng.api.networking.events.MENetworkCraftingPatternChange;
import appeng.api.storage.channels.IFluidStorageChannel;
import appeng.api.storage.data.IAEFluidStack;
import appeng.me.GridAccessException;
import appeng.util.InventoryAdaptor;
import appeng.util.Platform;
import appeng.util.inv.AdaptorItemHandler;
import codechicken.lib.raytracer.CuboidRayTraceResult;
import codechicken.lib.render.CCRenderState;
import codechicken.lib.render.pipeline.IVertexOperation;
import codechicken.lib.vec.Matrix4;
import com.cleanroommc.modularui.api.drawable.IKey;
import com.cleanroommc.modularui.drawable.GuiTextures;
import com.cleanroommc.modularui.drawable.ItemDrawable;
import com.cleanroommc.modularui.factory.PosGuiData;
import com.cleanroommc.modularui.screen.ModularPanel;
import com.cleanroommc.modularui.screen.UISettings;
import com.cleanroommc.modularui.value.sync.BooleanSyncValue;
import com.cleanroommc.modularui.value.sync.EnumSyncValue;
import com.cleanroommc.modularui.value.sync.PanelSyncManager;
import com.cleanroommc.modularui.widget.Widget;
import com.cleanroommc.modularui.widgets.CycleButtonWidget;
import com.cleanroommc.modularui.widgets.ListWidget;
import com.cleanroommc.modularui.widgets.PageButton;
import com.cleanroommc.modularui.widgets.PagedWidget;
import com.cleanroommc.modularui.widgets.ToggleButton;
import com.cleanroommc.modularui.widgets.layout.Flow;
import com.cleanroommc.modularui.widgets.layout.Grid;
import com.cleanroommc.modularui.widgets.slot.ItemSlot;
import com.google.common.collect.Lists;
import com.walhay.gregifiedenergistics.api.metatileentity.MetaTileEntityCraftingProvider;
import com.walhay.gregifiedenergistics.api.mui.GregifiedEnergisticsGuiTextures;
import com.walhay.gregifiedenergistics.api.patterns.AbstractPatternHelper;
import com.walhay.gregifiedenergistics.api.patterns.ISubstitutionNotifiable;
import com.walhay.gregifiedenergistics.api.patterns.ISubstitutionStorage;
import com.walhay.gregifiedenergistics.api.patterns.substitutions.SubstitutionStorage;
import com.walhay.gregifiedenergistics.api.util.BlockingMode;
import com.walhay.gregifiedenergistics.client.render.GregifiedEnergisticsTextures;
import com.walhay.gregifiedenergistics.common.mui.SubstitutionSlotWidget;
import gregtech.api.capability.GregtechDataCodes;
import gregtech.api.capability.GregtechTileCapabilities;
import gregtech.api.capability.impl.GhostCircuitItemStackHandler;
import gregtech.api.capability.impl.MultiblockRecipeLogic;
import gregtech.api.capability.impl.NotifiableItemStackHandler;
import gregtech.api.metatileentity.multiblock.AbilityInstances;
import gregtech.api.metatileentity.multiblock.IMultiblockAbilityPart;
import gregtech.api.metatileentity.multiblock.MultiblockAbility;
import gregtech.api.metatileentity.multiblock.RecipeMapMultiblockController;
import gregtech.api.mui.GTGuis;
import gregtech.client.renderer.texture.cube.SimpleOverlayRenderer;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.Constants.NBT;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.IFluidTank;
import net.minecraftforge.items.IItemHandlerModifiable;
import org.jetbrains.annotations.NotNull;

public abstract class MTEAbstractAssemblyLineBus extends MetaTileEntityCraftingProvider<IAEFluidStack>
		implements IMultiblockAbilityPart<IItemHandlerModifiable>, ISubstitutionNotifiable {

	public static final String WORKING_ENABLED_TAG = "WorkingEnabled";
	public static final String USE_FLUID_TAG = "FluidMode";
	public static final String FLUID_TO_SEND_TAG = "FluidToSend";
	public static final String ITEM_TO_SEND_TAG = "ItemToSend";
	public static final String SEND_SLOT_TAG = "Slot";

	private Int2ObjectOpenHashMap<ItemStack> waitingToSend;
	private Int2ObjectOpenHashMap<FluidStack> fluidWaitingToSend;
	private BlockingMode blockingMode = BlockingMode.NO_BLOCKING;
	private boolean useFluids = true;
	private boolean workingEnabled = true;
	protected final ISubstitutionStorage substitutionStorage = new SubstitutionStorage(this);

	/* ###########################
	###     MTE METHODS     ###
	########################### */

	public MTEAbstractAssemblyLineBus(ResourceLocation metaTileEntityId, int tier) {
		super(metaTileEntityId, tier, false, IFluidStorageChannel.class);
	}

	@Override
	public void update() {
		super.update();
		if (getWorld().isRemote) return;

		if (isWorkingEnabled() && shouldSyncME() && updateMEStatus()) {
			if (hasItemsToSend()) pushItemsOut();

			if (hasFluidsToSend()) pushFluidsOut();
		}
	}

	@Override
	protected IItemHandlerModifiable createImportItemHandler() {
		return new NotifiableItemStackHandler(this, 1, getController(), false);
	}

	@Override
	public void clearMachineInventory(@NotNull List<@NotNull ItemStack> itemBuffer) {
		super.clearMachineInventory(itemBuffer);
		clearInventory(itemBuffer, importItems);
		if (hasItemsToSend()) {
			itemBuffer.addAll(waitingToSend.values());

			waitingToSend = null;
		}
	}

	@Override
	public void addToolUsages(ItemStack stack, World world, List<String> tooltip, boolean advanced) {
		super.addToolUsages(stack, world, tooltip, advanced);
		tooltip.add(I18n.format("gregifiedenergistics.tool_action.memory_card.copy_substitution"));
	}

	@Override
	public ModularPanel buildUI(PosGuiData guiData, PanelSyncManager sync, UISettings settings) {
		sync.syncValue("working_enabled", new BooleanSyncValue(this::isWorkingEnabled, this::setWorkingEnabled));
		sync.syncValue("fluid_mode", new BooleanSyncValue(this::getUsingFluids, this::setUsingFluids));
		sync.syncValue(
				"blocking_mode", new EnumSyncValue<>(BlockingMode.class, this::getBlockingMode, this::setBlockingMode));

		ModularPanel panel = GTGuis.createPanel(this, 176, 200);

		var controller = new PagedWidget.Controller();

		var tabs = Flow.row()
				.name("tab row")
				.widthRel(1f)
				.leftRel(0.5f)
				.margin(3, 0)
				.coverChildrenHeight()
				.topRel(0f, 3, 1f);

		var paged = new PagedWidget<>();

		int pageCounter = 0;

		var patternList = createPatternList(panel, sync);
		if (patternList != null) {
			tabs.child(new PageButton(pageCounter++, controller)
					.tab(GuiTextures.TAB_TOP, 0)
					.addTooltipLine(IKey.lang("gregtech.machine.workbench.tab.workbench"))
					.overlay(new ItemDrawable(AEApi.instance()
									.definitions()
									.items()
									.encodedPattern()
									.maybeItem()
									.get())
							.asIcon()
							.size(16)));
		}

		var substitutionList = createSubstitutionList(panel, sync);
		if (substitutionList != null) {
			tabs.child(new PageButton(pageCounter++, controller)
					.tab(GuiTextures.TAB_TOP, 0)
					.addTooltipLine(IKey.lang("gregtech.machine.workbench.tab.item_list"))
					.addTooltipLine(
							IKey.lang("gregtech.machine.workbench.storage_note").style(TextFormatting.DARK_GRAY))
					.overlay(new ItemDrawable(AEApi.instance()
									.definitions()
									.items()
									.memoryCard()
									.maybeItem()
									.get())
							.asIcon()
							.size(16)));
		}

		if (patternList != null) {
			paged.addPage(patternList);
		}

		if (substitutionList != null) {
			paged.addPage(substitutionList);
		}

		return panel.child(new ItemSlot().slot(importItems, 0).pos(7, 7).size(18))
				.child(Flow.column()
						.width(16)
						.left(-18)
						.top(0)
						.childPadding(2)
						.child(new ToggleButton()
								.syncHandler("working_enabled")
								.size(16)
								.overlay(false, BUTTON_POWER[0])
								.overlay(true, BUTTON_POWER[1]))
						.child(new ToggleButton()
								.syncHandler("fluid_mode")
								.size(16)
								.overlay(false, new ItemDrawable(Items.BUCKET))
								.overlay(true, new ItemDrawable(Items.WATER_BUCKET)))
						.child(new CycleButtonWidget()
								.syncHandler("blocking_mode")
								.size(16)
								.stateOverlay(0, BLOCKING_MODE[0])
								.stateOverlay(1, BLOCKING_MODE[1])
								.stateOverlay(2, BLOCKING_MODE[2])))
				.child(tabs)
				.child(paged.top(28).widthRel(0.9f).controller(controller));
	}

	public Widget<?> createPatternList(ModularPanel panel, PanelSyncManager syncHandler) {
		return null;
	}

	public Widget<?> createSubstitutionList(ModularPanel panel, PanelSyncManager syncHandler) {
		return Flow.column()
				.left(7)
				.widthRel(0.9f)
				.child(IKey.lang("gregifiedenergistics.gui.substitution_list").asWidget())
				.child(new ListWidget<>()
						.widthRel(0.9f)
						.child(new Grid()
								.minElementMargin(0)
								.minColWidth(18)
								.minRowHeight(18)
								.coverChildren()
								.mapTo(9, Lists.newArrayList(substitutionStorage.getOptions()), (index, name) ->
										(Widget<?>) new SubstitutionSlotWidget().storage(substitutionStorage, name))));
	}

	@Override
	@SuppressWarnings("UnstableApiUsage")
	public boolean usesMui2() {
		return true;
	}

	@Override
	public boolean onRightClick(
			EntityPlayer player, EnumHand hand, EnumFacing facing, CuboidRayTraceResult traceResult) {
		ItemStack heldItem = player.getHeldItem(hand);

		if (AEApi.instance().definitions().items().memoryCard().isSameAs(heldItem)) {
			if (player.isSneaking()) {
				var nbt = new NBTTagCompound();
				heldItem.writeToNBT(nbt);

				nbt.setTag(STORAGE_TAG, substitutionStorage.serializeNBT());

				heldItem.setTagCompound(nbt);
				player.sendStatusMessage(
						new TextComponentString(I18n.format("gregifiedenergistics.machine.me.copy_settings")), true);
			} else {
				var tag = heldItem.getSubCompound(STORAGE_TAG);
				if (tag != null) {
					substitutionStorage.deserializeNBT(tag);

					notifySubstitutionChange();
					player.sendStatusMessage(
							new TextComponentString(I18n.format("gregifiedenergistics.machine.me.paste_settings")),
							true);
				}
			}
			return true;
		}
		return super.onRightClick(player, hand, facing, traceResult);
	}

	@Override
	public NBTTagCompound writeToNBT(NBTTagCompound data) {
		super.writeToNBT(data);
		data.setBoolean(WORKING_ENABLED_TAG, workingEnabled);
		data.setString(BLOCKING_MODE_TAG, blockingMode.toString());
		data.setBoolean(USE_FLUID_TAG, useFluids);

		if (hasItemsToSend()) {
			NBTTagList itemList = new NBTTagList();
			for (var entry : waitingToSend.entrySet()) {
				NBTTagCompound tag = new NBTTagCompound();

				entry.getValue().writeToNBT(tag);
				tag.setInteger(SEND_SLOT_TAG, entry.getKey());

				itemList.appendTag(tag);
			}
			data.setTag(ITEM_TO_SEND_TAG, itemList);
		}

		if (hasFluidsToSend()) {
			NBTTagList fluidList = new NBTTagList();
			for (var entry : fluidWaitingToSend.entrySet()) {
				NBTTagCompound tag = new NBTTagCompound();

				entry.getValue().writeToNBT(tag);
				tag.setInteger(SEND_SLOT_TAG, entry.getKey());

				fluidList.appendTag(tag);
			}
			data.setTag(FLUID_TO_SEND_TAG, fluidList);
		}

		return data;
	}

	@Override
	public void readFromNBT(NBTTagCompound data) {
		super.readFromNBT(data);
		workingEnabled = data.getBoolean(WORKING_ENABLED_TAG);
		blockingMode = BlockingMode.valueOf(data.getString(BLOCKING_MODE_TAG));
		useFluids = data.getBoolean(USE_FLUID_TAG);

		if (data.hasKey(ITEM_TO_SEND_TAG)) {
			NBTTagList itemList = data.getTagList(ITEM_TO_SEND_TAG, NBT.TAG_COMPOUND);

			for (int i = 0; i < itemList.tagCount(); ++i) {
				NBTTagCompound tag = itemList.getCompoundTagAt(i);

				int slot = tag.getInteger(SEND_SLOT_TAG);
				ItemStack stack = new ItemStack(tag);

				waitingToSend.put(slot, stack);
			}
		}

		if (data.hasKey(FLUID_TO_SEND_TAG)) {
			NBTTagList fluidList = data.getTagList(FLUID_TO_SEND_TAG, NBT.TAG_COMPOUND);

			for (int i = 0; i < fluidList.tagCount(); ++i) {
				NBTTagCompound tag = fluidList.getCompoundTagAt(i);

				int slot = tag.getInteger(SEND_SLOT_TAG);
				FluidStack stack = FluidStack.loadFluidStackFromNBT(tag);

				fluidWaitingToSend.put(slot, stack);
			}
		}
	}

	@Override
	public void writeInitialSyncData(PacketBuffer buf) {
		super.writeInitialSyncData(buf);
		buf.writeBoolean(workingEnabled);
		buf.writeEnumValue(blockingMode);
		buf.writeBoolean(useFluids);
	}

	@Override
	public void receiveInitialSyncData(PacketBuffer buf) {
		super.receiveInitialSyncData(buf);
		workingEnabled = buf.readBoolean();
		blockingMode = buf.readEnumValue(BlockingMode.class);
		useFluids = buf.readBoolean();
	}

	@Override
	public void receiveCustomData(int descriptor, PacketBuffer buf) {
		super.receiveCustomData(descriptor, buf);
		if (descriptor == GregtechDataCodes.WORKING_ENABLED) {
			this.workingEnabled = buf.readBoolean();
			scheduleRenderUpdate();
		}
	}

	@Override
	public MultiblockAbility<IItemHandlerModifiable> getAbility() {
		return MultiblockAbility.IMPORT_ITEMS;
	}

	@Override
	public void registerAbilities(@NotNull AbilityInstances abilityInstances) {
		abilityInstances.add(importItems);
	}

	@Override
	public <T> T getCapability(Capability<T> capability, EnumFacing facing) {
		if (capability == GregtechTileCapabilities.CAPABILITY_CONTROLLABLE) {
			return GregtechTileCapabilities.CAPABILITY_CONTROLLABLE.cast(this);
		}
		return super.getCapability(capability, facing);
	}

	protected SimpleOverlayRenderer getOverlay() {
		if (isOnline) {
			if (isWorkingEnabled()) {
				return GregifiedEnergisticsTextures.ME_AL_HATCH_CONNECTOR_ACTIVE;
			}
			return GregifiedEnergisticsTextures.ME_AL_HATCH_CONNECTOR_WAITING;
		}
		return GregifiedEnergisticsTextures.ME_AL_HATCH_CONNECTOR_INACTIVE;
	}

	@Override
	public void renderMetaTileEntity(CCRenderState renderState, Matrix4 translation, IVertexOperation[] pipeline) {
		super.renderMetaTileEntity(renderState, translation, pipeline);
		getOverlay().renderSided(getFrontFacing(), renderState, translation, pipeline);
	}

	// provide crafting patterns to the network
	@Override
	public void provideCrafting(ICraftingProviderHelper craftingHelper) {
		if (isAttachedToMultiBlock() && isWorkingEnabled()) {
			for (ICraftingPatternDetails details : getPatterns()) {
				if (details instanceof AbstractPatternHelper helper) {
					helper.providePatterns(getCraftingProvider(), craftingHelper);
				} else if (details != null) {
					craftingHelper.addCraftingOption(getCraftingProvider(), details);
				}
			}
		}
	}

	// check if hatch can be used for pushing pattern right now
	@Override
	public boolean isBusy() {
		if (!isAttachedToMultiBlock() || hasItemsToSend() || hasFluidsToSend()) {
			return true;
		}

		if (blockingMode == BlockingMode.CRAFTING_BLOCKING_MODE) {
			if (getController() != null && getController() instanceof RecipeMapMultiblockController controller) {
				MultiblockRecipeLogic workable = controller.getRecipeMapWorkable();
				if (workable.getProgressPercent() < 0.95 && workable.getProgress() > 0) {
					return true;
				}
			}
		}

		if (blockingMode.isBlockingEnabled()) {
			return getController().getAbilities(MultiblockAbility.IMPORT_ITEMS).stream()
					.map(AdaptorItemHandler::new)
					.anyMatch(InventoryAdaptor::containsItems);
		}

		return false;
	}

	// try to push pattern to import buses
	@Override
	public boolean pushPattern(ICraftingPatternDetails pattern, InventoryCrafting inventoryCrafting) {
		if (hasItemsToSend()
				|| (useFluids && hasFluidsToSend())
				|| getProxy() == null
				|| !getProxy().isActive()) {
			return false;
		}

		IAEFluidStack[] fluids = null;
		if (pattern instanceof AbstractPatternHelper patternHelper) {
			fluids = patternHelper.getFluidInputs();
		}

		if (acceptsItems(inventoryCrafting) && (!useFluids || acceptsFluids(fluids))) {
			int slot = 0;
			for (int i = 0; i < inventoryCrafting.getSizeInventory(); ++i) {
				ItemStack stack = inventoryCrafting.getStackInSlot(i);
				if (!stack.isEmpty()) {
					addToSendList(slot++, stack);
				}
			}

			if (useFluids && fluids != null) {
				slot = 0;
				for (IAEFluidStack fluidStack : fluids) {
					FluidStack stack = fluidStack.getFluidStack();
					if (stack != null && stack.amount != 0) {
						addToSendList(slot++, stack);
					}
				}
				pushFluidsOut();
			}
			pushItemsOut();
			return true;
		}

		return false;
	}

	// check if buses is able to accept items from pattern
	private boolean acceptsItems(final InventoryCrafting inventoryCrafting) {
		List<InventoryAdaptor> inventoryAdaptors = getController().getAbilities(MultiblockAbility.IMPORT_ITEMS).stream()
				.filter(i -> !(i instanceof GhostCircuitItemStackHandler))
				.map(AdaptorItemHandler::new)
				.collect(Collectors.toList());
		Iterator<InventoryAdaptor> it = inventoryAdaptors.iterator();

		for (int i = 0; i < inventoryCrafting.getSizeInventory(); ++i) {
			ItemStack stack = inventoryCrafting.getStackInSlot(i);
			if (stack.isEmpty()) {
				continue;
			}

			if (!it.hasNext()) {
				return false;
			}

			if (!it.next().simulateAdd(stack).isEmpty()) {
				return false;
			}
		}

		return true;
	}

	private boolean containsFluids(IAEFluidStack[] fluids) {
		if (getProxy() == null) return false;

		for (IAEFluidStack fluidStack : fluids) {
			try {
				IAEFluidStack find = getProxy()
						.getStorage()
						.getInventory(getStorageChannel())
						.getStorageList()
						.findPrecise(fluidStack);
				if (find == null || find.getStackSize() < fluidStack.getStackSize()) {
					return false;
				}
			} catch (GridAccessException e) {
				return false;
			}
		}
		return true;
	}

	private boolean acceptsFluids(IAEFluidStack[] inputs) {
		if (inputs == null || inputs.length == 0) {
			return true;
		}

		if (getProxy() == null || !containsFluids(inputs)) {
			return false;
		}

		List<IFluidTank> inputHandlers = getController().getAbilities(MultiblockAbility.IMPORT_FLUIDS);

		Iterator<IFluidTank> it = inputHandlers.iterator();

		for (IAEFluidStack fluidStack : inputs) {
			if (fluidStack == null || fluidStack.getStackSize() == 0) {
				continue;
			}

			if (!it.hasNext()) {
				return false;
			}

			if (it.next().fill(fluidStack.getFluidStack(), false) < fluidStack.getStackSize()) {
				return false;
			}

			try {
				IAEFluidStack result = getProxy()
						.getStorage()
						.getInventory(getStorageChannel())
						.extractItems(fluidStack, Actionable.SIMULATE, getActionSource());
				if (!result.equals(fluidStack)) {
					return false;
				}
			} catch (GridAccessException e) {
				return false;
			}
		}
		return true;
	}

	// Push items from auto-crafting to input buses
	private void pushItemsOut() {
		List<InventoryAdaptor> inventoryAdaptors = getController().getAbilities(MultiblockAbility.IMPORT_ITEMS).stream()
				.filter(i -> !(i instanceof GhostCircuitItemStackHandler))
				.map(AdaptorItemHandler::new)
				.collect(Collectors.toList());

		Iterator<Map.Entry<Integer, ItemStack>> it = waitingToSend.entrySet().iterator();

		while (it.hasNext()) {
			Map.Entry<Integer, ItemStack> entry = it.next();

			int index = entry.getKey();
			ItemStack stack = entry.getValue();

			if (stack.isEmpty()) {
				continue;
			}

			if (index <= inventoryAdaptors.size()) {
				ItemStack result = inventoryAdaptors.get(index).addItems(stack);
				if (result.isEmpty()) {
					it.remove();
				} else {
					stack.setCount(result.getCount());
				}
			}
		}

		if (waitingToSend.isEmpty()) {
			waitingToSend = null;
		}
	}

	private void pushFluidsOut() {
		if (getProxy() == null) return;

		List<IFluidTank> inputHandlers = getController().getAbilities(MultiblockAbility.IMPORT_FLUIDS);

		Iterator<Map.Entry<Integer, FluidStack>> it =
				fluidWaitingToSend.entrySet().iterator();

		while (it.hasNext()) {
			Map.Entry<Integer, FluidStack> entry = it.next();

			int slot = entry.getKey();
			FluidStack stack = entry.getValue();

			if (stack == null || stack.amount == 0) {
				continue;
			}

			if (slot < inputHandlers.size()) {
				try {
					IAEFluidStack extracted = getProxy()
							.getStorage()
							.getInventory(getStorageChannel())
							.extractItems(
									getStorageChannel().createStack(stack), Actionable.MODULATE, getActionSource());
					int filled = inputHandlers.get(slot).fill(extracted.getFluidStack(), true);
					if (filled == stack.amount) {
						it.remove();
					} else {
						stack.amount -= filled;
					}
				} catch (GridAccessException e) {
					// :3
				}
			}
		}

		if (fluidWaitingToSend.isEmpty()) {
			fluidWaitingToSend = null;
		}
	}

	// notify grid network when patterns should be recalculated
	public void notifyPatternChange() {
		if (Platform.isServer() && getProxy() != null) {
			try {
				getProxy()
						.getGrid()
						.postEvent(new MENetworkCraftingPatternChange(
								getCraftingProvider(), getProxy().getNode()));
			} catch (GridAccessException ignored) {
			}
		}
	}

	@Override
	public void notifySubstitutionChange() {
		for (ICraftingPatternDetails details : getPatterns()) {
			if (details instanceof AbstractPatternHelper helper) {
				helper.injectSubstitutions(substitutionStorage);
			}
		}

		notifyPatternChange();
	}

	/* ###################################
	###    GETTER/SETTER METHODS    ###
	################################### */

	public abstract Collection<? extends ICraftingPatternDetails> getPatterns();

	private void addToSendList(int slot, ItemStack stack) {
		if (waitingToSend == null) {
			waitingToSend = new Int2ObjectOpenHashMap<>();
		}
		waitingToSend.put(slot, stack);
	}

	public boolean hasItemsToSend() {
		return this.waitingToSend != null && !this.waitingToSend.isEmpty();
	}

	private void addToSendList(int slot, FluidStack stack) {
		if (fluidWaitingToSend == null) {
			fluidWaitingToSend = new Int2ObjectOpenHashMap<>();
		}
		fluidWaitingToSend.put(slot, stack);
	}

	public boolean hasFluidsToSend() {
		return this.fluidWaitingToSend != null && !this.fluidWaitingToSend.isEmpty();
	}

	public void setBlockingMode(BlockingMode blockingMode) {
		this.blockingMode = blockingMode;
	}

	public BlockingMode getBlockingMode() {
		return blockingMode;
	}

	public void setUsingFluids(boolean useFluids) {
		this.useFluids = useFluids;
	}

	public boolean getUsingFluids() {
		return useFluids;
	}

	@Override
	public void setWorkingEnabled(boolean workingEnabled) {
		this.workingEnabled = workingEnabled;
		World world = getWorld();
		if (world != null && !world.isRemote) {
			writeCustomData(GregtechDataCodes.WORKING_ENABLED, buf -> buf.writeBoolean(workingEnabled));
			notifyPatternChange();
		}
	}

	@Override
	public boolean isWorkingEnabled() {
		return workingEnabled;
	}
}
