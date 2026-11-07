package com.futuretek.client.screen;

import com.futuretek.storage.Storage;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;

/** Interfaccia dei bauli FutureTek, con i pulsanti "Deposita tutto" e "Ordina" (SOLO client). */
public class StorageScreen extends AbstractContainerScreen<Storage.ChestMenu> {
    public StorageScreen(Storage.ChestMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = Storage.ChestMenu.width(menu.getTier());
        imageHeight = Storage.ChestMenu.height(menu.getTier());
        inventoryLabelX = Storage.ChestMenu.inventoryLeft(menu.getTier()) - 1;
        inventoryLabelY = Storage.ChestMenu.inventoryTop(menu.getTier()) - 11;
    }

    @Override
    protected void init() {
        super.init();
        addRenderableWidget(Button.builder(Component.translatable("gui.futuretek.deposit_all"),
                        b -> press(Storage.ChestMenu.BUTTON_DEPOSIT))
                .bounds(leftPos + imageWidth - 92, topPos + 4, 50, 12)
                .tooltip(Tooltip.create(Component.translatable("gui.futuretek.deposit_all_tip"))).build());
        addRenderableWidget(Button.builder(Component.translatable("gui.futuretek.sort"),
                        b -> press(Storage.ChestMenu.BUTTON_SORT))
                .bounds(leftPos + imageWidth - 40, topPos + 4, 34, 12)
                .tooltip(Tooltip.create(Component.translatable("gui.futuretek.sort_tip"))).build());
    }

    private void press(int id) {
        if (minecraft != null && minecraft.gameMode != null) {
            minecraft.gameMode.handleInventoryButtonClick(menu.containerId, id);
        }
    }

    @Override
    protected void renderBg(GuiGraphics g, float partialTick, int mouseX, int mouseY) {
        int x = leftPos;
        int y = topPos;
        int w = imageWidth;
        int h = imageHeight;
        g.fill(x, y, x + w, y + h, 0xFFC6C6C6);
        g.fill(x, y, x + w - 1, y + 1, 0xFFFFFFFF);
        g.fill(x, y, x + 1, y + h - 1, 0xFFFFFFFF);
        g.fill(x + 1, y + h - 1, x + w, y + h, 0xFF555555);
        g.fill(x + w - 1, y + 1, x + w, y + h, 0xFF555555);
        for (Slot slot : menu.slots) {
            MachineScreen.drawSlot(g, x + slot.x - 1, y + slot.y - 1, 18, 18);
        }
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);
        renderTooltip(g, mouseX, mouseY);
    }
}
