package com.walhay.gregifiedenergistics.common.mui;

import com.cleanroommc.modularui.api.IPanelHandler;
<<<<<<< HEAD
||||||| parent of f2ab357 (Substitution GUI almost done)
import com.cleanroommc.modularui.api.drawable.IDrawable.DrawableWidget;
=======
import com.cleanroommc.modularui.api.drawable.IDrawable;
>>>>>>> f2ab357 (Substitution GUI almost done)
import com.cleanroommc.modularui.api.drawable.IKey;
import com.cleanroommc.modularui.api.value.ISyncOrValue;
import com.cleanroommc.modularui.api.widget.Interactable;
import com.cleanroommc.modularui.drawable.ItemDrawable;
<<<<<<< HEAD
import com.cleanroommc.modularui.utils.Alignment;
import com.cleanroommc.modularui.value.sync.SyncHandler;
||||||| parent of f2ab357 (Substitution GUI almost done)
import com.cleanroommc.modularui.screen.ModularPanel;
=======
import com.cleanroommc.modularui.screen.ModularPanel;
import com.cleanroommc.modularui.value.sync.SyncHandler;
>>>>>>> f2ab357 (Substitution GUI almost done)
import com.cleanroommc.modularui.widget.Widget;
import com.cleanroommc.modularui.widgets.ButtonWidget;
import com.cleanroommc.modularui.widgets.layout.Grid;
<<<<<<< HEAD
import com.walhay.gregifiedenergistics.api.patterns.ISubstitutionStorage;
import gregtech.api.mui.GTGuiTextures;
import gregtech.api.mui.GTGuis;
||||||| parent of f2ab357 (Substitution GUI almost done)
=======
import com.walhay.gregifiedenergistics.api.patterns.ISubstitutionStorage;
import gregtech.api.mui.GTGuiTextures;
>>>>>>> f2ab357 (Substitution GUI almost done)
import gregtech.api.unification.OreDictUnifier;
import java.io.IOException;
import java.util.List;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketBuffer;
import org.jetbrains.annotations.NotNull;

public class SubstitutionSlotWidget extends Widget<SubstitutionSlotWidget> implements Interactable {

	private List<ItemStack> items;
	private SubstituionSyncHandler syncHandler;
	private String name;
	private IPanelHandler selectorPanel;
	private ItemDrawable itemPreview;

	public SubstitutionSlotWidget() {
		size(18);
		background(GTGuiTextures.SLOT);
		this.itemPreview = new ItemDrawable();
		overlay(itemPreview.asIcon().alignment(Alignment.CENTER));
		onUpdateListener(w -> itemPreview.setItem(items.get(syncHandler.getOption())));
	}

	public SubstitutionSlotWidget storage(ISubstitutionStorage storage, String name) {
		this.name = name;
		this.items = OreDictUnifier.getAllWithOreDictionaryName(name);
		this.syncHandler = new SubstituionSyncHandler(storage, name);
		setSyncOrValue(syncHandler);

		return this;
	}

	@Override
	public void onInit() {
		tooltip().setAutoUpdate(true);
		tooltip().tooltipBuilder(rt -> rt.addFromItem(items.get(syncHandler.getOption())));
	}

	@Override
	public boolean isValidSyncOrValue(@NotNull ISyncOrValue syncOrValue) {
		return syncHandler instanceof SubstituionSyncHandler;
	}

	@Override
	public SubstituionSyncHandler getSyncHandler() {
		return syncHandler;
	}

	private IPanelHandler selectorPanel() {
		if (selectorPanel == null) {
			selectorPanel = IPanelHandler.simple(
					getPanel(),
					(parentPanel, player) -> {
						int height = (int) Math.ceil(items.size() / 5.0f);

<<<<<<< HEAD
						var selectedItem = items.get(syncHandler.getOption());
						return GTGuis.createPopupPanel("selector_panel_" + name, 104, height * 18 + 50)
								.child(IKey.lang("gregifiedenergistics.gui.substitution")
										.asWidget()
										.pos(5, 5))
								.child(new Widget<>()
										.size(18)
										.top(19)
										.tooltip(rt -> rt.addFromItem(selectedItem))
										.horizontalCenter()
										.background(GTGuiTextures.SLOT)
										.overlay(itemPreview.asIcon().margin(1)))
								.child(new Grid()
										.horizontalCenter()
										.top(41)
										.minElementMargin(0)
										.minColWidth(18)
										.minRowHeight(18)
										.mapTo(
												Math.min(5, items.size()),
												items,
												(index, element) -> (Widget<?>) new ButtonWidget<>()
														.background(GTGuiTextures.SLOT)
														.overlay(new ItemDrawable(element)
																.asIcon()
																.margin(1))
														.tooltip(rt -> rt.addFromItem(element))
														.onMousePressed(
																button -> this.syncHandler.setSubstitution(index))));
					},
					true);
		}
		return selectorPanel;
	}

||||||| parent of f2ab357 (Substitution GUI almost done)
	private ModularPanel buildUI() {
		int sq = (int) Math.sqrt(items.size());

		var panel = ModularPanel.defaultPanel("selector_panel_" + oreDict, 18 * sq + 12, 18 * sq + 12);

		var grid = new Grid()
				.pos(6, 6)
				.height(sq * 18)
				.minElementMargin(0)
				.minColWidth(18)
				.minRowHeight(18)
				.mapTo(sq, items.size(), index -> {
					var widget = new DrawableWidget(new ItemDrawable(items.get(index)));
					widget.onUpdateListener(this::listener);
					return widget;
				});

		panel.child(IKey.lang(oreDict).asWidget());
		panel.child(grid);

		return panel;
	}

	private void listener(DrawableWidget widget) {
		GTLog.logger.info("Hello world");
	}

=======
	private ModularPanel buildUI() {
		int sq = (int) Math.sqrt(items.size());

		var panel = ModularPanel.defaultPanel("selector_panel_" + name, 18 * sq + 12, 18 * sq + 12);

		var grid = new Grid()
				.pos(6, 6)
				.height(sq * 18)
				.minElementMargin(0)
				.minColWidth(18)
				.minRowHeight(18)
				.mapTo(sq, items.size(), index -> {
					var widget = new ButtonWidget<>()
							.background(IDrawable.of(GTGuiTextures.SLOT, new ItemDrawable(items.get(index))))
							.onMousePressed(button -> this.syncHandler.setSubstitution(index));
					return widget;
				});

		panel.child(IKey.lang(name).asWidget());
		panel.child(grid);

		return panel;
	}

>>>>>>> f2ab357 (Substitution GUI almost done)
	@Override
	public @NotNull Result onMousePressed(int mouseButton) {
		selectorPanel().togglePanel();
		return Result.SUCCESS;
	}

	public class SubstituionSyncHandler extends SyncHandler {

		private final ISubstitutionStorage storage;
		private final String name;
		public static final int SET_SUBSTITUTION = 0;

		public SubstituionSyncHandler(ISubstitutionStorage storage, String name) {
			this.storage = storage;
			this.name = name;
		}

		@Override
		public void readOnClient(int id, PacketBuffer buf) throws IOException {
			if (id == SET_SUBSTITUTION) {
				setSubstitution(buf.readVarInt());
			}
		}

		@Override
		public void readOnServer(int id, PacketBuffer buf) throws IOException {
			if (id == SET_SUBSTITUTION) {
				setSubstitution(buf.readVarInt());
			}
		}

		public boolean setSubstitution(int option) {
			if (storage != null && getOption() != option) {
				storage.setOption(name, option);
				sync(SET_SUBSTITUTION, buf -> buf.writeVarInt(option));
				return true;
			}
			return false;
		}

		public int getOption() {
			return storage.getOption(name);
		}
	}
}
