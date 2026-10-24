package com.walhay.gregifiedenergistics.client.render;

import com.walhay.gregifiedenergistics.GregifiedEnergisticsMod;
import gregtech.client.renderer.texture.cube.SimpleOverlayRenderer;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public class GregifiedEnergisticsTextures {

	public static SimpleOverlayRenderer ME_AL_HATCH_CONNECTOR_ACTIVE;
	public static SimpleOverlayRenderer ME_AL_HATCH_CONNECTOR_WAITING;
	public static SimpleOverlayRenderer ME_AL_HATCH_CONNECTOR_INACTIVE;

	public static SimpleOverlayRenderer ME_PATTERN_PROVIDER_ACTIVE;
	public static SimpleOverlayRenderer ME_PATTERN_PROVIDER;

	public static SimpleOverlayRenderer ME_PATTERN_BUFFER_ACTIVE;
	public static SimpleOverlayRenderer ME_PATTERN_BUFFER;

	public static void init() {
		ME_AL_HATCH_CONNECTOR_ACTIVE = new SimpleOverlayRenderer(path("me_assembly_line_hatch/me_connector_active"));
		ME_AL_HATCH_CONNECTOR_WAITING = new SimpleOverlayRenderer(path("me_assembly_line_hatch/me_connector_waiting"));
		ME_AL_HATCH_CONNECTOR_INACTIVE =
				new SimpleOverlayRenderer(path("me_assembly_line_hatch/me_connector_inactive"));

		ME_PATTERN_PROVIDER = new SimpleOverlayRenderer(path("me_pattern_provider/me_pattern_provider"));
		ME_PATTERN_PROVIDER_ACTIVE = new SimpleOverlayRenderer(path("me_pattern_provider/me_pattern_provider_active"));

		ME_PATTERN_BUFFER = new SimpleOverlayRenderer(path("me_pattern_buffer/me_pattern_buffer"));
		ME_PATTERN_BUFFER_ACTIVE = new SimpleOverlayRenderer(path("me_pattern_buffer/me_pattern_buffer_active"));
	}

	private static String path(String base) {
		return GregifiedEnergisticsMod.MOD_ID + ":" + base;
	}
}
