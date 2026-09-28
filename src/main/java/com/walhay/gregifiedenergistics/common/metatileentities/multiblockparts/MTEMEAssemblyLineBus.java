package com.walhay.gregifiedenergistics.common.metatileentities.multiblockparts;

import static gregtech.api.GTValues.LuV;

import appeng.api.networking.crafting.ICraftingPatternDetails;
import appeng.items.misc.ItemEncodedPattern;
import com.walhay.gregifiedenergistics.GregifiedEnergisticsConfig;
import com.walhay.gregifiedenergistics.api.capability.AbstractPatternItemHandler;
import com.walhay.gregifiedenergistics.api.gui.GregifiedEnergisticsGuiTextures;
import com.walhay.gregifiedenergistics.api.patterns.implementations.DataStickPatternHelper;
import gregtech.api.gui.GuiTextures;
import gregtech.api.gui.widgets.LabelWidget;
import gregtech.api.gui.widgets.ScrollableListWidget;
import gregtech.api.gui.widgets.SlotWidget;
import gregtech.api.gui.widgets.WidgetGroup;
import gregtech.api.metatileentity.MetaTileEntity;
import gregtech.api.metatileentity.interfaces.IGregTechTileEntity;
import gregtech.api.util.AssemblyLineManager;
import java.io.IOException;
import java.util.Collection;
import java.util.List;
import javax.annotation.Nonnull;
import net.minecraft.client.resources.I18n;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;
import org.jetbrains.annotations.NotNull;

public class MTEMEAssemblyLineBus extends MTEAbstractAssemblyLineBus {

	public static final String PATTERN_INVENTORY_TAG = "PatternInventory";

	private final DataStickHandler patternHandler =
			new DataStickHandler(GregifiedEnergisticsConfig.machineConfig.patternHandlerSize);

	public MTEMEAssemblyLineBus(ResourceLocation metaTileEntityId) {
		super(metaTileEntityId, LuV);
	}

	@Override
	public void addInformation(ItemStack stack, World world, @NotNull List<String> tooltip, boolean advanced) {
		super.addInformation(stack, world, tooltip, advanced);
		tooltip.add(I18n.format("gregifiedenergistics.machine.me_assembly_line_bus.datastick"));
		tooltip.add(I18n.format("gregifiedenergistics.machine.me_assembly_line_bus.pattern_slots"));
		tooltip.add(I18n.format("gregifiedenergistics.machine.me_assembly_line_bus.fluid_mode"));
	}

	@Override
	public MetaTileEntity createMetaTileEntity(IGregTechTileEntity metaTileEntity) {
		return new MTEMEAssemblyLineBus(metaTileEntityId);
	}

	@Override
	protected WidgetGroup createPatternList() {
		WidgetGroup page = new WidgetGroup(0, 0, 199, 109);
		page.addWidget(new LabelWidget(7, 7, "gregifiedenergistics.gui.pattern_list"));
		ScrollableListWidget list = new ScrollableListWidget(58, 22, 83, 82);
		for (int firstSlot = 0; firstSlot < patternHandler.getSlots(); firstSlot += 4) {
			WidgetGroup row = new WidgetGroup(0, 0, 72, 18);
			for (int column = 0; column < 4 && firstSlot + column < patternHandler.getSlots(); column++) {
				int index = firstSlot + column;
				row.addWidget(new SlotWidget(patternHandler, index, column * 18, 0)
						.setBackgroundTexture(GuiTextures.SLOT, GregifiedEnergisticsGuiTextures.PATTERN_OVERLAY)
						.setChangeListener(() -> patternHandler.onContentsChanged(index)));
			}
			list.addWidget(row);
		}
		page.addWidget(list);
		return page;
	}

	@Override
	public NBTTagCompound writeToNBT(NBTTagCompound data) {
		super.writeToNBT(data);
		data.setTag(PATTERN_INVENTORY_TAG, patternHandler.serializeNBT());
		return data;
	}

	@Override
	public void readFromNBT(NBTTagCompound data) {
		super.readFromNBT(data);
		patternHandler.deserializeNBT(data.getCompoundTag(PATTERN_INVENTORY_TAG));
	}

	@Override
	public void writeInitialSyncData(PacketBuffer buf) {
		super.writeInitialSyncData(buf);
		buf.writeCompoundTag(patternHandler.serializeNBT());
	}

	@Override
	public void receiveInitialSyncData(PacketBuffer buf) {
		super.receiveInitialSyncData(buf);
		try {
			NBTTagCompound nbt = buf.readCompoundTag();

			if (nbt == null) return;

			patternHandler.deserializeNBT(nbt);
		} catch (IOException ignored) {
			// :#
		}
	}

	@Override
	public void clearMachineInventory(@NotNull List<@NotNull ItemStack> itemBuffer) {
		super.clearMachineInventory(itemBuffer);
		clearInventory(itemBuffer, patternHandler);
	}

	@Override
	public Collection<? extends ICraftingPatternDetails> getPatterns() {
		return patternHandler.getPatterns();
	}

	class DataStickHandler extends AbstractPatternItemHandler {

		public DataStickHandler(int size) {
			super(MTEMEAssemblyLineBus.this, size);
		}

		@Override
		protected ICraftingPatternDetails getPatternFromStack(ItemStack stack) {
			if (stack.isEmpty()) return null;

			if (AssemblyLineManager.isStackDataItem(stack, true)) {
				DataStickPatternHelper helper = new DataStickPatternHelper(stack);
				helper.injectSubstitutions(substitutionStorage);
				return helper;
			} else if (stack.getItem() instanceof ItemEncodedPattern itemPattern) {
				return itemPattern.getPatternForItem(stack, getWorld());
			}
			return null;
		}

		@Override
		protected void onPatternUpdate() {
			notifyPatternChange();
		}

		@Override
		public boolean isItemValid(int slot, @Nonnull ItemStack stack) {
			return super.isItemValid(slot, stack) || AssemblyLineManager.isStackDataItem(stack, true);
		}
	}
}
