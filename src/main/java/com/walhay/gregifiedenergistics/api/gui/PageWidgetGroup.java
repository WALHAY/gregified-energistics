package com.walhay.gregifiedenergistics.api.gui;

import gregtech.api.gui.INativeWidget;
import gregtech.api.gui.Widget;
import gregtech.api.gui.widgets.WidgetGroup;
import net.minecraft.network.PacketBuffer;
import org.lwjgl.input.Keyboard;

/** Container pages with fixed native slot IDs and a selection private to each viewer. */
public class PageWidgetGroup extends WidgetGroup {

	private int selectedPage;

	public PageWidgetGroup(int width, int height) {
		super(0, 0, width, height);
	}

	public void addPage(WidgetGroup page) {
		addWidget(page);
		setPageEnabled(page, widgets.size() - 1 == selectedPage);
	}

	private void setPageEnabled(WidgetGroup page, boolean enabled) {
		page.setVisible(enabled);
		page.setActive(enabled);
		for (INativeWidget slot : page.getNativeWidgets()) {
			if (slot instanceof Widget widget) {
				widget.setVisible(enabled);
				widget.setActive(enabled);
			}
		}
	}

	private void setPage(int page) {
		if (page < 0 || page >= widgets.size()) return;
		selectedPage = page;
		for (int i = 0; i < widgets.size(); i++) setPageEnabled((WidgetGroup) widgets.get(i), i == page);
	}

	public void selectPage(int page) {
		if (page < 0 || page >= widgets.size()) return;
		setPage(page);
		if (isRemote()) writeClientAction(2, buffer -> buffer.writeVarInt(page));
		else writeUpdateInfo(2, buffer -> buffer.writeVarInt(page));
	}

	@Override
	public boolean keyTyped(char charTyped, int keyCode) {
		if (selectedPage != 0 && keyCode == Keyboard.KEY_ESCAPE) {
			selectPage(0);
			return true;
		}
		return super.keyTyped(charTyped, keyCode);
	}

	@Override
	public void handleClientAction(int id, PacketBuffer buffer) {
		if (id == 2) {
			selectPage(buffer.readVarInt());
		} else if (id == 1) {
			buffer.markReaderIndex();
			int page = buffer.readVarInt();
			buffer.resetReaderIndex();
			if (page == selectedPage) super.handleClientAction(id, buffer);
		}
	}

	@Override
	public void readUpdateInfo(int id, PacketBuffer buffer) {
		if (id == 2) setPage(buffer.readVarInt());
		else super.readUpdateInfo(id, buffer);
	}
}
