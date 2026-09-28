package com.walhay.gregifiedenergistics.common.gui;

import com.walhay.gregifiedenergistics.api.patterns.ISubstitutionStorage;
import gregtech.api.gui.GuiTextures;
import gregtech.api.gui.IRenderContext;
import gregtech.api.gui.Widget;
import gregtech.api.unification.OreDictUnifier;
import gregtech.api.util.LocalizationUtils;
import gregtech.api.util.Position;
import gregtech.api.util.Size;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.math.MathHelper;
import org.lwjgl.input.Keyboard;

/** A paged substitution grid and selector, synchronized through the GTCEu container. */
public class SubstitutionListWidget extends Widget {

	private final ISubstitutionStorage storage;
	private final Map<String, List<ItemStack>> variants = new HashMap<>();
	private Map<String, Integer> options = new TreeMap<>();
	private List<String> names = new ArrayList<>();
	private boolean initialSync;
	private String selectedName;
	private int page;

	public SubstitutionListWidget(ISubstitutionStorage storage, int x, int y, int width, int height) {
		super(new Position(x, y), new Size(width, height));
		this.storage = storage;
	}

	private List<ItemStack> variants(String name) {
		return variants.computeIfAbsent(name, key -> new ArrayList<>(OreDictUnifier.getAllWithOreDictionaryName(key)));
	}

	private int columns() {
		return Math.max(1, Math.min(9, getSize().width / 18));
	}

	private int pageSize() {
		return columns() * Math.max(1, (getSize().height - 22) / 18);
	}

	private int entryCount() {
		return selectedName == null ? names.size() : variants(selectedName).size();
	}

	private int pageCount() {
		return Math.max(1, (entryCount() + pageSize() - 1) / pageSize());
	}

	private ItemStack selectedStack(String name) {
		List<ItemStack> items = variants(name);
		if (items.isEmpty()) return ItemStack.EMPTY;
		return items.get(MathHelper.clamp(options.getOrDefault(name, 0), 0, items.size() - 1));
	}

	private ItemStack stackAt(int index) {
		return selectedName == null
				? selectedStack(names.get(index))
				: variants(selectedName).get(index);
	}

	private int hoveredEntry(int mouseX, int mouseY) {
		int x = mouseX - getPosition().x;
		int y = mouseY - getPosition().y - 22;
		if (x < 0 || x >= columns() * 18 || y < 0 || y >= pageSize() / columns() * 18) return -1;
		int index = page * pageSize() + y / 18 * columns() + x / 18;
		return index < entryCount() ? index : -1;
	}

	@Override
	public void detectAndSendChanges() {
		Map<String, Integer> current = new TreeMap<>();
		for (String name : storage.getOptions()) current.put(name, storage.getOption(name));
		if (!initialSync || !current.equals(options)) {
			initialSync = true;
			options = current;
			writeUpdateInfo(0, buffer -> {
				buffer.writeVarInt(current.size());
				current.forEach((name, option) -> {
					buffer.writeString(name);
					buffer.writeVarInt(option);
				});
			});
		}
	}

	@Override
	public void readUpdateInfo(int id, PacketBuffer buffer) {
		if (id != 0) return;
		Map<String, Integer> current = new TreeMap<>();
		int count = buffer.readVarInt();
		for (int i = 0; i < count; i++) current.put(buffer.readString(256), buffer.readVarInt());
		options = current;
		names = new ArrayList<>(current.keySet());
		if (selectedName != null && !current.containsKey(selectedName)) selectedName = null;
		page = MathHelper.clamp(page, 0, pageCount() - 1);
	}

	@Override
	public void drawInBackground(int mouseX, int mouseY, float partialTicks, IRenderContext context) {
		int x = getPosition().x;
		int y = getPosition().y;
		if (selectedName != null) {
			drawButton(x, y, "<");
			drawItemStack(selectedStack(selectedName), x + 23, y + 1, null);
		}
		drawText((page + 1) + "/" + pageCount(), x + 65, y + 5, 1, 0xFF404040);
		drawButton(x + getSize().width - 40, y, "<");
		drawButton(x + getSize().width - 18, y, ">");
		for (int cell = 0; cell < pageSize() && page * pageSize() + cell < entryCount(); cell++) {
			int index = page * pageSize() + cell;
			int cellX = x + cell % columns() * 18;
			int cellY = y + 22 + cell / columns() * 18;
			GuiTextures.SLOT.draw(cellX, cellY, 18, 18);
			drawItemStack(stackAt(index), cellX + 1, cellY + 1, null);
			if (selectedName != null && options.getOrDefault(selectedName, 0) == index)
				drawBorder(cellX, cellY, 18, 18, 0xFF55AA55, 1);
		}
	}

	private void drawButton(int x, int y, String text) {
		GuiTextures.VANILLA_BUTTON.drawSubArea(x, y, 18, 18, 0, 0, 1, 0.5);
		drawText(text, x + 6, y + 5, 1, 0xFFFFFFFF);
	}

	@Override
	public void drawInForeground(int mouseX, int mouseY) {
		int index = hoveredEntry(mouseX, mouseY);
		if (index < 0) return;
		ItemStack stack = stackAt(index);
		List<String> tooltip = new ArrayList<>(getItemToolTip(stack));
		if (selectedName == null) {
			tooltip.add(names.get(index));
			tooltip.add(LocalizationUtils.format("gregifiedenergistics.gui.substitution_hint"));
		}
		drawHoveringText(stack, tooltip, 300, mouseX, mouseY);
	}

	@Override
	public boolean mouseClicked(int mouseX, int mouseY, int button) {
		if (!isMouseOverElement(mouseX, mouseY)) return false;
		int x = mouseX - getPosition().x;
		int y = mouseY - getPosition().y;
		if (y < 18 && button == 0) {
			if (selectedName != null && x < 18) {
				selectedName = null;
				page = 0;
			} else if (x >= getSize().width - 18) {
				page = Math.min(page + 1, pageCount() - 1);
			} else if (x >= getSize().width - 40 && x < getSize().width - 22) {
				page = Math.max(page - 1, 0);
			}
			return true;
		}
		int index = hoveredEntry(mouseX, mouseY);
		if (index < 0 || button > 1) return true;
		if (selectedName != null) {
			select(selectedName, index);
		} else {
			String name = names.get(index);
			if (button == 0 && isShiftDown()) {
				selectedName = name;
				page = 0;
			} else {
				select(name, options.getOrDefault(name, 0) + (button == 0 ? 1 : -1));
			}
		}
		return true;
	}

	private void select(String name, int option) {
		int count = variants(name).size();
		if (count == 0) return;
		int value = MathHelper.clamp(option, 0, count - 1);
		writeClientAction(0, buffer -> {
			buffer.writeString(name);
			buffer.writeVarInt(value);
		});
	}

	@Override
	public void handleClientAction(int id, PacketBuffer buffer) {
		if (id != 0) return;
		String name = buffer.readString(256);
		int option = buffer.readVarInt();
		if (!storage.getOptions().contains(name)
				|| option < 0
				|| option >= variants(name).size()) return;
		storage.setOption(name, option);
		gui.holder.markAsDirty();
	}

	@Override
	public boolean mouseWheelMove(int mouseX, int mouseY, int wheelDelta) {
		if (!isMouseOverElement(mouseX, mouseY) || wheelDelta == 0) return false;
		int index = hoveredEntry(mouseX, mouseY);
		if (selectedName == null && index >= 0) {
			String name = names.get(index);
			select(name, options.getOrDefault(name, 0) + Integer.signum(wheelDelta));
		} else {
			page = MathHelper.clamp(page - Integer.signum(wheelDelta), 0, pageCount() - 1);
		}
		return true;
	}

	@Override
	public boolean keyTyped(char charTyped, int keyCode) {
		if (selectedName != null && keyCode == Keyboard.KEY_ESCAPE) {
			selectedName = null;
			page = 0;
			return true;
		}
		return false;
	}
}
