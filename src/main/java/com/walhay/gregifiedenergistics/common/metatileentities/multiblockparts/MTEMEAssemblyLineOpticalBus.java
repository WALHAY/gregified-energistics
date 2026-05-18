package com.walhay.gregifiedenergistics.common.metatileentities.multiblockparts;

import static com.walhay.gregifiedenergistics.api.capability.GregifiedEnergisticsDataCodes.CHANGE_OPTICAL_SIDE;
import static gregtech.api.GTValues.ZPM;

import appeng.api.networking.crafting.ICraftingPatternDetails;
import codechicken.lib.raytracer.CuboidRayTraceResult;
import codechicken.lib.render.CCRenderState;
import codechicken.lib.render.pipeline.IVertexOperation;
import codechicken.lib.vec.Matrix4;
import com.walhay.gregifiedenergistics.api.capability.GregifiedEnergisticsCapabilities;
import com.walhay.gregifiedenergistics.api.capability.INetRecipeHandler;
import com.walhay.gregifiedenergistics.api.capability.IOpticalNetRecipeHandler;
import com.walhay.gregifiedenergistics.api.patterns.implementations.RecipePatternHelper;
import gregtech.api.metatileentity.MetaTileEntity;
import gregtech.api.metatileentity.interfaces.IGregTechTileEntity;
import gregtech.api.recipes.Recipe;
import gregtech.client.renderer.texture.Textures;
import gregtech.common.pipelike.optical.tile.TileEntityOpticalPipe;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.PacketBuffer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;
import net.minecraftforge.common.capabilities.Capability;
import org.jetbrains.annotations.NotNull;

public class MTEMEAssemblyLineOpticalBus extends MTEAbstractAssemblyLineBus implements IOpticalNetRecipeHandler {

	private static final String OPTICAL_FACING_TAG = "OpticalFacing";

	private final List<RecipePatternHelper> patterns = new ArrayList<>();
	private EnumFacing opticalFacing = EnumFacing.DOWN;

	public MTEMEAssemblyLineOpticalBus(ResourceLocation metaTileEntityId) {
		super(metaTileEntityId, ZPM);
	}

	@Override
	public MetaTileEntity createMetaTileEntity(IGregTechTileEntity metaTileEntity) {
		return new MTEMEAssemblyLineOpticalBus(metaTileEntityId);
	}

	@Override
	public void addInformation(ItemStack stack, World world, @NotNull List<String> tooltip, boolean advanced) {
		super.addInformation(stack, world, tooltip, advanced);
		tooltip.add(I18n.format("gregifiedenergistics.machine.me_assembly_line_optical_bus.optical_pipe"));
	}

	@Override
	public void addToolUsages(ItemStack stack, World world, List<String> tooltip, boolean advanced) {
		super.addToolUsages(stack, world, tooltip, advanced);
		tooltip.add(I18n.format("gregifiedenergistics.tool_action.screwdriwer.optical_facing"));
	}

	@Override
	public boolean onScrewdriverClick(
			EntityPlayer playerIn, EnumHand hand, EnumFacing facing, CuboidRayTraceResult hitResult) {
		if (opticalFacing != facing) {
			opticalFacing = facing;
			writeCustomData(CHANGE_OPTICAL_SIDE, buf -> buf.writeEnumValue(opticalFacing));
			updatePatternData();
			return true;
		}

		return super.onScrewdriverClick(playerIn, hand, facing, hitResult);
	}

	@Override
	public void setFrontFacing(EnumFacing frontFacing) {
		super.setFrontFacing(frontFacing);
		updatePatternData();
	}

	@Override
	public NBTTagCompound writeToNBT(NBTTagCompound data) {
		super.writeToNBT(data);
		data.setString(OPTICAL_FACING_TAG, opticalFacing.name());
		return data;
	}

	@Override
	public void readFromNBT(NBTTagCompound data) {
		super.readFromNBT(data);
		opticalFacing = EnumFacing.byName(data.getString(OPTICAL_FACING_TAG));
	}

	@Override
	public void writeInitialSyncData(PacketBuffer buf) {
		super.writeInitialSyncData(buf);
		buf.writeEnumValue(opticalFacing);
	}

	@Override
	public void receiveInitialSyncData(PacketBuffer buf) {
		super.receiveInitialSyncData(buf);
		opticalFacing = buf.readEnumValue(EnumFacing.class);
	}

	@Override
	public void receiveCustomData(int dataId, PacketBuffer buf) {
		super.receiveCustomData(dataId, buf);
		if (dataId == CHANGE_OPTICAL_SIDE) {
			opticalFacing = buf.readEnumValue(EnumFacing.class);
			scheduleRenderUpdate();
		}
	}

	@Override
	public <T> T getCapability(Capability<T> capability, EnumFacing facing) {
		if (facing == opticalFacing && capability == GregifiedEnergisticsCapabilities.CAPABILITY_RECIPE_HANDLER) {
			return GregifiedEnergisticsCapabilities.CAPABILITY_RECIPE_HANDLER.cast(this);
		}
		return super.getCapability(capability, facing);
	}

	@Override
	public void renderMetaTileEntity(CCRenderState renderState, Matrix4 translation, IVertexOperation[] pipeline) {
		super.renderMetaTileEntity(renderState, translation, pipeline);
		Textures.OPTICAL_DATA_ACCESS_HATCH.renderSided(opticalFacing, renderState, translation, pipeline);
	}

	private void updatePatternData() {
		if (!isAttachedToMultiBlock()) {
			clearPatternData();
			return;
		}

		TileEntity te = getWorld().getTileEntity(getPos().offset(opticalFacing));
		if (te instanceof TileEntityOpticalPipe) {
			INetRecipeHandler data = te.getCapability(
					GregifiedEnergisticsCapabilities.CAPABILITY_RECIPE_HANDLER, opticalFacing.getOpposite());

			if (data == null) {
				clearPatternData();
				return;
			}

			var recipes = data.getRecipes();
			if (recipes == null || recipes.isEmpty()) {
				clearPatternData();
				return;
			}

			patterns.clear();
			recipes.stream().map(RecipePatternHelper::new).forEach(pattern -> {
				pattern.injectSubstitutions(substitutionStorage);
				patterns.add(pattern);
			});

			notifyPatternChange();
			return;
		}

		clearPatternData();
	}

	private void clearPatternData() {
		if (patterns.isEmpty()) return;

		patterns.clear();
		notifyPatternChange();
	}

	@Override
	public Collection<? extends ICraftingPatternDetails> getPatterns() {
		return patterns;
	}

	@Override
	public void onRecipesUpdate(Collection<INetRecipeHandler> seen) {
		seen.add(this);
		updatePatternData();
	}

	@Override
	public Collection<Recipe> getRecipes(Collection<INetRecipeHandler> seen) {
		seen.add(this);
		return null;
	}

	@Override
	public boolean isTransmitter() {
		return false;
	}
}
