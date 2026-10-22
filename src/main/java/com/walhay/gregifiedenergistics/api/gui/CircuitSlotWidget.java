package com.walhay.gregifiedenergistics.api.gui;

import gregtech.api.capability.impl.GhostCircuitItemStackHandler;
import gregtech.api.gui.GuiTextures;
import gregtech.api.gui.widgets.GhostCircuitSlotWidget;
import gregtech.api.util.LocalizationUtils;
import net.minecraft.network.PacketBuffer;

public class CircuitSlotWidget extends GhostCircuitSlotWidget {

	private final GhostCircuitItemStackHandler inventory;

	public CircuitSlotWidget(GhostCircuitItemStackHandler inventory, int x, int y) {
		super(inventory, 0, x, y);
		this.inventory = inventory;
		setBackgroundTexture(GuiTextures.SLOT, GuiTextures.INT_CIRCUIT_OVERLAY);
	}

	@Override
	public void handleClientAction(int id, PacketBuffer buffer) {
		if (id == 3) {
			buffer.markReaderIndex();
			int value = buffer.readVarInt();
			buffer.resetReaderIndex();
			if (value < -1 || value > 32) return;
		}
		super.handleClientAction(id, buffer);
		if (id >= 1 && id <= 3) gui.holder.markAsDirty();
	}

	@Override
	public void drawInForeground(int mouseX, int mouseY) {
		String value = inventory.hasCircuitValue()
				? Integer.toString(inventory.getCircuitValue())
				: LocalizationUtils.format("gregtech.gui.configurator_slot.no_value");
		setTooltipText("gregtech.gui.configurator_slot.tooltip", value);
		super.drawInForeground(mouseX, mouseY);
	}
}
