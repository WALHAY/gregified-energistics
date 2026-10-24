package com.walhay.gregifiedenergistics.api.gui;

import gregtech.api.gui.GuiTextures;
import gregtech.api.gui.IRenderContext;
import gregtech.api.gui.widgets.ToggleButtonWidget;
import gregtech.api.util.function.BooleanConsumer;
import java.util.function.BooleanSupplier;
import net.minecraft.item.ItemStack;

public class ItemToggleButtonWidget extends ToggleButtonWidget {

	private final ItemStack disabledIcon;
	private final ItemStack enabledIcon;

	public ItemToggleButtonWidget(
			int x,
			int y,
			ItemStack disabledIcon,
			ItemStack enabledIcon,
			BooleanSupplier getter,
			BooleanConsumer setter) {
		super(x, y, 18, 18, GuiTextures.TOGGLE_BUTTON_BACK, getter, setter);
		this.disabledIcon = disabledIcon;
		this.enabledIcon = enabledIcon;
	}

	@Override
	public void drawInBackground(int mouseX, int mouseY, float partialTicks, IRenderContext context) {
		super.drawInBackground(mouseX, mouseY, partialTicks, context);
		drawItemStack(isPressed ? enabledIcon : disabledIcon, getPosition().x + 1, getPosition().y + 1, null);
	}
}
