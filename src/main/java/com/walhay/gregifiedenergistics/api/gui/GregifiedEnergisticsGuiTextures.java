package com.walhay.gregifiedenergistics.api.gui;

import com.walhay.gregifiedenergistics.GregifiedEnergisticsMod;
import gregtech.api.gui.resources.TextureArea;

public final class GregifiedEnergisticsGuiTextures {

	public static final TextureArea BLOCKING_MODE = texture("blocking_mode");
	public static final TextureArea PATTERN_OVERLAY = texture("pattern_overlay");

	private GregifiedEnergisticsGuiTextures() {}

	private static TextureArea texture(String name) {
		return new TextureArea(
				GregifiedEnergisticsMod.gregifiedEnergisticsId("textures/gui/" + name + ".png"), 0, 0, 1, 1);
	}
}
