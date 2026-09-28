package com.walhay.gregifiedenergistics.common.metatileentities.multiblockparts;

import static com.walhay.gregifiedenergistics.api.gui.GregifiedEnergisticsGuiTextures.BLOCKING_MODE;
import static com.walhay.gregifiedenergistics.api.patterns.substitutions.SubstitutionStorage.STORAGE_TAG;
import static com.walhay.gregifiedenergistics.api.util.BlockingMode.BLOCKING_MODE_TAG;

import appeng.api.AEApi;
import appeng.api.config.Actionable;
import appeng.api.networking.crafting.ICraftingPatternDetails;
import appeng.api.networking.crafting.ICraftingProviderHelper;
import appeng.api.storage.channels.IFluidStorageChannel;
import appeng.api.storage.data.IAEFluidStack;
import appeng.me.GridAccessException;
import appeng.util.InventoryAdaptor;
import appeng.util.inv.AdaptorItemHandler;
import codechicken.lib.raytracer.CuboidRayTraceResult;
import codechicken.lib.render.CCRenderState;
import codechicken.lib.render.pipeline.IVertexOperation;
import codechicken.lib.vec.Matrix4;
import com.walhay.gregifiedenergistics.api.gui.ItemToggleButtonWidget;
import com.walhay.gregifiedenergistics.api.metatileentity.MetaTileEntityCraftingProvider;
import com.walhay.gregifiedenergistics.api.patterns.AbstractPatternHelper;
import com.walhay.gregifiedenergistics.api.patterns.ISubstitutionNotifiable;
import com.walhay.gregifiedenergistics.api.patterns.ISubstitutionStorage;
import com.walhay.gregifiedenergistics.api.patterns.substitutions.SubstitutionStorage;
import com.walhay.gregifiedenergistics.api.util.BlockingMode;
import com.walhay.gregifiedenergistics.client.render.GregifiedEnergisticsTextures;
import com.walhay.gregifiedenergistics.common.gui.SubstitutionListWidget;
import gregtech.api.capability.GregtechDataCodes;
import gregtech.api.capability.GregtechTileCapabilities;
import gregtech.api.capability.impl.GhostCircuitItemStackHandler;
import gregtech.api.capability.impl.MultiblockRecipeLogic;
import gregtech.api.capability.impl.NotifiableItemStackHandler;
import gregtech.api.gui.GuiTextures;
import gregtech.api.gui.ModularUI;
import gregtech.api.gui.widgets.ImageCycleButtonWidget;
import gregtech.api.gui.widgets.ImageWidget;
import gregtech.api.gui.widgets.LabelWidget;
import gregtech.api.gui.widgets.SlotWidget;
import gregtech.api.gui.widgets.TabGroup;
import gregtech.api.gui.widgets.ToggleButtonWidget;
import gregtech.api.gui.widgets.WidgetGroup;
import gregtech.api.gui.widgets.tab.ItemTabInfo;
import gregtech.api.metatileentity.multiblock.AbilityInstances;
import gregtech.api.metatileentity.multiblock.IMultiblockAbilityPart;
import gregtech.api.metatileentity.multiblock.MultiblockAbility;
import gregtech.api.metatileentity.multiblock.RecipeMapMultiblockController;
import gregtech.api.util.Position;
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
	protected ModularUI createUI(EntityPlayer player) {
		TabGroup<WidgetGroup> tabs = new TabGroup<>(TabGroup.TabLocation.HORIZONTAL_TOP_LEFT, Position.ORIGIN);
		WidgetGroup patterns = createPatternList();
		if (patterns != null) {
			tabs.addTab(
					new ItemTabInfo(
							"gregifiedenergistics.gui.pattern_list",
							new ItemStack(AEApi.instance()
									.definitions()
									.items()
									.encodedPattern()
									.maybeItem()
									.orElse(Items.AIR))),
					patterns);
		}
		WidgetGroup substitutions = new WidgetGroup(0, 0, 199, 109);
		substitutions.addWidget(new LabelWidget(7, 7, "gregifiedenergistics.gui.substitution_list"));
		substitutions.addWidget(new SubstitutionListWidget(substitutionStorage, 7, 20, 185, 84));
		tabs.addTab(
				new ItemTabInfo(
						"gregifiedenergistics.gui.substitutions_grid",
						new ItemStack(AEApi.instance()
								.definitions()
								.items()
								.memoryCard()
								.maybeItem()
								.orElse(Items.AIR))),
				substitutions);

		return ModularUI.builder(GuiTextures.BACKGROUND, 199, 200)
				.widget(tabs)
				.widget(createButtonBar())
				.bindPlayerInventory(player.inventory, GuiTextures.SLOT, 7, 117)
				.build(getHolder(), player);
	}

	private WidgetGroup createButtonBar() {
		WidgetGroup buttons = new WidgetGroup(174, 116, 18, 77);
		buttons.addWidget(new SlotWidget(importItems, 0, 0, 0)
				.setBackgroundTexture(GuiTextures.SLOT)
				.setTooltipText("gregifiedenergistics.gui.item_slot"));
		buttons.addWidget(new ImageCycleButtonWidget(
						0,
						18,
						18,
						18,
						BLOCKING_MODE,
						BlockingMode.values().length,
						() -> getBlockingMode().ordinal(),
						option -> {
							if (option >= 0 && option < BlockingMode.values().length)
								setBlockingMode(BlockingMode.values()[option]);
						})
				.shouldUseBaseBackground()
				.setTooltipHoverString(option -> switch (BlockingMode.values()[option]) {
					case NO_BLOCKING -> "gregifiedenergistics.gui.no_blocking";
					case BLOCKING_MODE -> "gregifiedenergistics.gui.blocking_mode";
					case CRAFTING_BLOCKING_MODE -> "gregifiedenergistics.gui.crafting_blocking_mode";
				}));
		buttons.addWidget(new ItemToggleButtonWidget(
						0,
						36,
						new ItemStack(Items.BUCKET),
						new ItemStack(Items.WATER_BUCKET),
						this::getUsingFluids,
						this::setUsingFluids)
				.setTooltipText("gregifiedenergistics.gui.fluid_mode"));
		buttons.addWidget(new ImageWidget(0, 54, 18, 6, GuiTextures.BUTTON_POWER_DETAIL));
		buttons.addWidget(new ToggleButtonWidget(
						0, 59, 18, 18, GuiTextures.BUTTON_POWER, this::isWorkingEnabled, this::setWorkingEnabled)
				.setTooltipText("gregifiedenergistics.gui.working"));
		return buttons;
	}

	protected WidgetGroup createPatternList() {
		return null;
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
		if (getWorld() != null && !getWorld().isRemote) markDirty();
	}

	public BlockingMode getBlockingMode() {
		return blockingMode;
	}

	public void setUsingFluids(boolean useFluids) {
		this.useFluids = useFluids;
		if (getWorld() != null && !getWorld().isRemote) markDirty();
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
			markDirty();
		}
	}

	@Override
	public boolean isWorkingEnabled() {
		return workingEnabled;
	}
}
