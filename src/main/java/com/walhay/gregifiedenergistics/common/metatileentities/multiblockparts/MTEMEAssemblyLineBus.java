package com.walhay.gregifiedenergistics.common.metatileentities.multiblockparts;

import static gregtech.api.GTValues.LuV;

import appeng.api.networking.crafting.ICraftingPatternDetails;
import appeng.items.misc.ItemEncodedPattern;
import com.cleanroommc.modularui.api.drawable.IKey;
import com.cleanroommc.modularui.screen.ModularPanel;
import com.cleanroommc.modularui.value.sync.PanelSyncManager;
import com.cleanroommc.modularui.value.sync.SyncHandlers;
import com.cleanroommc.modularui.widget.Widget;
import com.cleanroommc.modularui.widgets.SlotGroupWidget;
import com.cleanroommc.modularui.widgets.layout.Flow;
import com.walhay.gregifiedenergistics.GregifiedEnergisticsConfig;
import com.walhay.gregifiedenergistics.api.capability.AbstractPatternItemHandler;
import com.walhay.gregifiedenergistics.api.patterns.implementations.DataStickPatternHelper;
import com.walhay.gregifiedenergistics.common.mui.DataStickSlot;
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
	public Widget<?> createPatternList(ModularPanel panel, PanelSyncManager syncHandler) {
		panel.bindPlayerInventory();

		return Flow.column()
				.name("pattern list")
				.left(7)
				.widthRel(0.9f)
				.coverChildrenHeight()
				.child(IKey.lang("gregifiedenergistics.gui.pattern_list").asWidget())
				.child(SlotGroupWidget.builder()
						.row("IIII")
						.row("IIII")
						.row("IIII")
						.row("IIII")
						.key('I', index -> new DataStickSlot().slot(SyncHandlers.itemSlot(patternHandler, index)))
						.build()
						.coverChildren());
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
			super(size);
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
