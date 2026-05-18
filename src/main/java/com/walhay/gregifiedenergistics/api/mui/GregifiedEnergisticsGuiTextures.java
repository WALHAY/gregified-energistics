package com.walhay.gregifiedenergistics.api.mui;

import com.cleanroommc.modularui.drawable.ColorType;
import com.cleanroommc.modularui.drawable.UITexture;
import com.walhay.gregifiedenergistics.Tags;
import org.jetbrains.annotations.Nullable;

public class GregifiedEnergisticsGuiTextures {
	/*
	 * 1 = NO BLOCKING
	 * 2 = BLOCKING
	 * 3 = CRAFTING BLOCKING MODE
	 */
	public static final UITexture[] BLOCKING_MODE = slice("textures/gui/blocking_mode.png", 16, 48, 16, 16);

	@SuppressWarnings("SameParameterValue")
	private static UITexture[] slice(String path, int imageWidth, int imageHeight, int sliceWidth, int sliceHeight) {
		return slice(path, imageWidth, imageHeight, sliceWidth, sliceHeight, null);
	}

	@SuppressWarnings("SameParameterValue")
	private static UITexture[] slice(
			String path,
			int imageWidth,
			int imageHeight,
			int sliceWidth,
			int sliceHeight,
			@Nullable ColorType colorType) {
		if (imageWidth % sliceWidth != 0 || imageHeight % sliceHeight != 0)
			throw new IllegalArgumentException("Slice height and slice width must divide the image evenly!");

		int countX = imageWidth / sliceWidth;
		int countY = imageHeight / sliceHeight;
		UITexture[] slices = new UITexture[countX * countY];

		for (int indexX = 0; indexX < countX; indexX++) {
			for (int indexY = 0; indexY < countY; indexY++) {
				slices[(indexX * countX) + indexY] = UITexture.builder()
						.location(Tags.MOD_ID, path)
						.colorType(colorType)
						.imageSize(imageWidth, imageHeight)
						.xy(indexX * sliceWidth, indexY * sliceHeight, sliceWidth, sliceHeight)
						.build();
			}
		}
		return slices;
	}
}
