package com.walhay.gregifiedenergistics.api.gui;

import gregtech.api.gui.GuiTextures;
import gregtech.api.gui.IRenderContext;
import gregtech.api.gui.Widget;
import gregtech.api.gui.ingredient.IIngredientSlot;
import gregtech.api.util.Position;
import gregtech.api.util.Size;
import gregtech.api.util.TextFormattingUtil;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketBuffer;
import net.minecraftforge.items.IItemHandlerModifiable;
import net.minecraftforge.items.ItemHandlerHelper;
import net.minecraftforge.items.wrapper.PlayerMainInvWrapper;

/** An interactive buffer slot which preserves counts larger than a vanilla inventory stack. */
public class BufferedItemWidget extends Widget implements IIngredientSlot {

	private final IItemHandlerModifiable inventory;
	private final int slot;
	private ItemStack displayedStack = ItemStack.EMPTY;

	public BufferedItemWidget(IItemHandlerModifiable inventory, int slot, int x, int y) {
		super(new Position(x, y), new Size(18, 18));
		this.inventory = inventory;
		this.slot = slot;
	}

	@Override
	public void detectAndSendChanges() {
		ItemStack stack = slot < inventory.getSlots() ? inventory.getStackInSlot(slot) : ItemStack.EMPTY;
		if (!ItemStack.areItemStacksEqual(stack, displayedStack)) {
			displayedStack = stack.copy();
			writeUpdateInfo(0, buffer -> {
				// Vanilla's item packet stores Count in a byte. Send the full count separately.
				buffer.writeItemStack(ItemHandlerHelper.copyStackWithSize(stack, 1));
				buffer.writeVarInt(stack.getCount());
			});
		}
	}

	@Override
	public void readUpdateInfo(int id, PacketBuffer buffer) {
		if (id != 0) return;
		try {
			displayedStack = buffer.readItemStack();
			displayedStack.setCount(buffer.readVarInt());
		} catch (IOException e) {
			throw new IllegalArgumentException("Invalid buffer item update", e);
		}
	}

	@Override
	public void drawInBackground(int mouseX, int mouseY, float partialTicks, IRenderContext context) {
		int x = getPosition().x;
		int y = getPosition().y;
		GuiTextures.SLOT.draw(x, y, 18, 18);
		if (!displayedStack.isEmpty()) {
			drawItemStack(
					displayedStack,
					x + 1,
					y + 1,
					displayedStack.getCount() == 1
							? ""
							: TextFormattingUtil.formatLongToCompactString(displayedStack.getCount(), 4));
		}
		if (isMouseOverElement(mouseX, mouseY)) drawSelectionOverlay(x + 1, y + 1, 16, 16);
	}

	@Override
	public void drawInForeground(int mouseX, int mouseY) {
		if (!displayedStack.isEmpty() && isMouseOverElement(mouseX, mouseY)) {
			List<String> tooltip = new ArrayList<>(getItemToolTip(displayedStack));
			tooltip.add(String.format("%,d", displayedStack.getCount()));
			drawHoveringText(displayedStack, tooltip, 300, mouseX, mouseY);
		}
	}

	@Override
	public Object getIngredientOverMouse(int mouseX, int mouseY) {
		return isMouseOverElement(mouseX, mouseY) && !displayedStack.isEmpty() ? displayedStack : null;
	}

	@Override
	public boolean mouseClicked(int mouseX, int mouseY, int button) {
		if (!isMouseOverElement(mouseX, mouseY) || button < 0 || button > 1) return false;
		ClickData data = new ClickData(button, isShiftDown(), isCtrlDown());
		writeClientAction(0, data::writeToBuf);
		return true;
	}

	@Override
	public void handleClientAction(int id, PacketBuffer buffer) {
		if (id != 0 || slot < 0 || slot >= inventory.getSlots()) return;
		ClickData click = ClickData.readFromBuf(buffer);
		if (click.button < 0 || click.button > 1) return;
		InventoryPlayer playerInventory = gui.entityPlayer.inventory;
		ItemStack stored = inventory.getStackInSlot(slot);
		ItemStack held = playerInventory.getItemStack();
		if (click.isShiftClick) {
			if (!stored.isEmpty()) {
				ItemStack extracted = inventory
						.extractItem(slot, stored.getMaxStackSize(), true)
						.copy();
				ItemStack remainder = ItemHandlerHelper.insertItemStacked(
						new PlayerMainInvWrapper(playerInventory), extracted, false);
				inventory.extractItem(slot, extracted.getCount() - remainder.getCount(), false);
			}
		} else if (held.isEmpty()) {
			if (!stored.isEmpty()) {
				int amount = click.button == 0
						? stored.getMaxStackSize()
						: (int) Math.min(stored.getMaxStackSize(), (stored.getCount() + 1L) / 2);
				playerInventory.setItemStack(inventory.extractItem(slot, amount, false));
			}
		} else if (stored.isEmpty() || ItemHandlerHelper.canItemStacksStack(stored, held)) {
			int amount = click.button == 0 ? held.getCount() : 1;
			ItemStack remainder = inventory.insertItem(slot, ItemHandlerHelper.copyStackWithSize(held, amount), false);
			held.shrink(amount - remainder.getCount());
			playerInventory.setItemStack(held.isEmpty() ? ItemStack.EMPTY : held);
		} else if (click.button == 0
				&& stored.getCount() <= stored.getMaxStackSize()
				&& held.getCount() <= inventory.getSlotLimit(slot)
				&& inventory.isItemValid(slot, held)) {
			inventory.setStackInSlot(slot, held.copy());
			playerInventory.setItemStack(stored.copy());
		}
		playerInventory.markDirty();
		gui.holder.markAsDirty();
		uiAccess.sendHeldItemUpdate();
	}
}
