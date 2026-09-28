package com.walhay.gregifiedenergistics.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.walhay.gregifiedenergistics.api.capability.InfiniteItemStackHandler;
import com.walhay.gregifiedenergistics.api.gui.BufferedItemWidget;
import io.netty.buffer.Unpooled;
import java.util.function.Consumer;
import net.minecraft.init.Bootstrap;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketBuffer;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class BufferedItemWidgetTest {

	@BeforeAll
	static void bootstrap() {
		Bootstrap.register();
	}

	@Test
	void synchronizesFullCountsAndClearsTheClientWhenAPatternShrinks() {
		InfiniteItemStackHandler inventory = new InfiniteItemStackHandler(3);
		BufferedItemWidget client = new BufferedItemWidget(new InfiniteItemStackHandler(0), 2, 0, 0);
		BufferedItemWidget server = connectedWidget(inventory, 2, client);

		for (int count : new int[] {1, 64, 128, 256, 40_000, Integer.MAX_VALUE}) {
			inventory.setStackInSlot(2, new ItemStack(Items.DIAMOND, count));
			server.detectAndSendChanges();
			ItemStack received = (ItemStack) client.getIngredientOverMouse(1, 1);
			assertEquals(Items.DIAMOND, received.getItem());
			assertEquals(count, received.getCount());
			assertEquals(count, inventory.getStackInSlot(2).getCount());
		}

		inventory.setSize(1);
		server.detectAndSendChanges();
		assertNull(client.getIngredientOverMouse(1, 1));
		inventory.setSize(0);
		server.detectAndSendChanges();
		assertNull(client.getIngredientOverMouse(1, 1));
	}

	@Test
	void sendsItemChangesEvenWhenTheCountStaysTheSame() {
		InfiniteItemStackHandler inventory = new InfiniteItemStackHandler(1);
		BufferedItemWidget client = new BufferedItemWidget(new InfiniteItemStackHandler(0), 0, 0, 0);
		BufferedItemWidget server = connectedWidget(inventory, 0, client);
		inventory.setStackInSlot(0, new ItemStack(Items.DIAMOND, 1024));
		server.detectAndSendChanges();
		inventory.setStackInSlot(0, new ItemStack(Items.EMERALD, 1024));
		server.detectAndSendChanges();
		ItemStack received = (ItemStack) client.getIngredientOverMouse(1, 1);
		assertEquals(Items.EMERALD, received.getItem());
		assertEquals(1024, received.getCount());
	}

	private static BufferedItemWidget connectedWidget(
			InfiniteItemStackHandler inventory, int slot, BufferedItemWidget client) {
		return new BufferedItemWidget(inventory, slot, 0, 0) {
			@Override
			protected void writeUpdateInfo(int id, Consumer<PacketBuffer> writer) {
				PacketBuffer packet = new PacketBuffer(Unpooled.buffer());
				try {
					writer.accept(packet);
					client.readUpdateInfo(id, packet);
				} finally {
					packet.release();
				}
			}
		};
	}
}
