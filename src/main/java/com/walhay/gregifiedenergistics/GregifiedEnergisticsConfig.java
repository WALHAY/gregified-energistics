package com.walhay.gregifiedenergistics;

import net.minecraftforge.common.config.Config;

@Config(modid = GregifiedEnergisticsMod.MOD_ID)
public class GregifiedEnergisticsConfig {

	@Config.Name("Multiblock parts config")
	public static MachineConfig machineConfig = new MachineConfig();

	@Config.Name("Gui Config")
	public static GuiConfig guiConfig = new GuiConfig();

	public static class MachineConfig {

		@Config.Name("Pattern Handler Size")
		@Config.Comment({"Slots amoun in ME Assembly Line Bus", "Default: 36"})
		@Config.RangeInt(min = 1)
		public int patternHandlerSize = 36;
	}

	public static class GuiConfig {

		@Config.Name(value = "Slots per line in substitution grid")
		@Config.Comment("Default: 8")
		@Config.RangeInt(min = 1, max = 16)
		public int substitutionSlotsPerLine = 8;

		@Config.Name(value = "Slots per line in pattern grid")
		@Config.Comment({"0 = detect automatically", "Default: 9"})
		@Config.RangeInt(min = 0, max = 16)
		public int patternSlotsPerLine = 9;
	}
}
