package com.walhay.gregifiedenergistics.common.gui;

import com.walhay.gregifiedenergistics.api.gui.BufferedItemWidget;
import gregtech.api.capability.impl.FluidTankList;
import gregtech.api.gui.GuiTextures;
import gregtech.api.gui.widgets.LabelWidget;
import gregtech.api.gui.widgets.ScrollableListWidget;
import gregtech.api.gui.widgets.TankWidget;
import gregtech.api.gui.widgets.WidgetGroup;
import java.util.function.Consumer;
import java.util.function.IntSupplier;
import java.util.function.Supplier;
import net.minecraft.network.PacketBuffer;
import net.minecraftforge.fluids.FluidTank;
import net.minecraftforge.fluids.IFluidTank;
import net.minecraftforge.items.IItemHandlerModifiable;

/** Rebuilds the buffer view when its pattern changes, without changing any native inventory slots. */
public class PatternBufferContentsWidget extends WidgetGroup {

	private final IItemHandlerModifiable items;
	private final Supplier<FluidTankList> fluids;
	private final IntSupplier patternRevision;
	private int revision = -1;
	private int itemCount = -1;
	private int fluidCount = -1;

	public PatternBufferContentsWidget(
			IItemHandlerModifiable items,
			Supplier<FluidTankList> fluids,
			IntSupplier patternRevision,
			int x,
			int y,
			int width,
			int height) {
		super(x, y, width, height);
		this.items = items;
		this.fluids = fluids;
		this.patternRevision = patternRevision;
	}

	@Override
	public void detectAndSendChanges() {
		if (revision != patternRevision.getAsInt()
				|| itemCount != items.getSlots()
				|| fluidCount != fluids.get().getTanks()) {
			revision = patternRevision.getAsInt();
			itemCount = items.getSlots();
			fluidCount = fluids.get().getTanks();
			writeUpdateInfo(2, buffer -> {
				buffer.writeVarInt(revision);
				buffer.writeVarInt(itemCount);
				buffer.writeVarInt(fluidCount);
			});
			rebuild();
		}
		super.detectAndSendChanges();
	}

	@Override
	public void readUpdateInfo(int id, PacketBuffer buffer) {
		if (id == 2) {
			revision = buffer.readVarInt();
			itemCount = buffer.readVarInt();
			fluidCount = buffer.readVarInt();
			rebuild();
		} else {
			super.readUpdateInfo(id, buffer);
		}
	}

	private void rebuild() {
		clearAllWidgets();
		if (itemCount == 0 && fluidCount == 0) {
			addWidget(new LabelWidget(0, 0, "gregifiedenergistics.gui.buffer_empty"));
			return;
		}
		if (itemCount > 0) addWidget(new LabelWidget(0, 0, "gregifiedenergistics.gui.buffer_items"));
		if (fluidCount > 0) addWidget(new LabelWidget(92, 0, "gregifiedenergistics.gui.buffer_fluids"));
		ScrollableListWidget list = new ScrollableListWidget(0, 13, getSize().width, getSize().height - 13);
		int rows = (Math.max(itemCount, fluidCount) + 3) / 4;
		for (int rowIndex = 0; rowIndex < rows; rowIndex++) {
			WidgetGroup row = new WidgetGroup(0, 0, 164, 18);
			for (int column = 0; column < 4; column++) {
				int index = rowIndex * 4 + column;
				if (index < itemCount) row.addWidget(new BufferedItemWidget(items, index, column * 18, 0));
				if (index < fluidCount) {
					IFluidTank tank = isRemote()
							? new FluidTank(Integer.MAX_VALUE)
							: fluids.get().getTankAt(index);
					row.addWidget(new TankWidget(tank, 92 + column * 18, 0, 18, 18)
							.setBackgroundTexture(GuiTextures.FLUID_SLOT)
							.setAlwaysShowFull(true)
							.setContainerClicking(true, true));
				}
			}
			list.addWidget(row);
		}
		addWidget(list);
	}

	@Override
	protected void writeClientAction(int id, Consumer<PacketBuffer> writer) {
		super.writeClientAction(id, buffer -> {
			buffer.writeVarInt(revision);
			writer.accept(buffer);
		});
	}

	@Override
	public void handleClientAction(int id, PacketBuffer buffer) {
		if (id != 1) return;
		int clientRevision = buffer.readVarInt();
		// Ignore clicks sent before another player replaced or removed the pattern.
		if (clientRevision != revision || revision != patternRevision.getAsInt()) return;
		super.handleClientAction(id, buffer);
	}
}
