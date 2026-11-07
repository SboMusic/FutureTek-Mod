package com.futuretek.client.screen;

import com.futuretek.power.CharcoalKiln;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;

/** Interfaccia della carbonaia: tronchi sopra, carbonella sotto, barra di cottura in mezzo (SOLO client). */
public class KilnScreen extends AbstractContainerScreen<CharcoalKiln.KilnMenu> {
    public KilnScreen(CharcoalKiln.KilnMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = 176;
        imageHeight = 198;
        inventoryLabelY = 105;
    }

    @Override
    protected void renderBg(GuiGraphics g, float partialTick, int mouseX, int mouseY) {
        int x = leftPos;
        int y = topPos;
        g.fill(x, y, x + imageWidth, y + imageHeight, 0xFFC9BBAA);
        g.fill(x, y, x + imageWidth - 1, y + 1, 0xFFFFFFFF);
        g.fill(x, y, x + 1, y + imageHeight - 1, 0xFFFFFFFF);
        g.fill(x + 1, y + imageHeight - 1, x + imageWidth, y + imageHeight, 0xFF5A4A3A);
        g.fill(x + imageWidth - 1, y + 1, x + imageWidth, y + imageHeight, 0xFF5A4A3A);
        for (Slot slot : menu.slots) {
            MachineScreen.drawSlot(g, x + slot.x - 1, y + slot.y - 1, 18, 18);
        }
        // barra di cottura con "fiamma"
        MachineScreen.drawSlot(g, x + 7, y + 55, 162, 7);
        int done = MachineScreen.scaled(menu.getProgress(), CharcoalKiln.CYCLE, 160);
        g.fill(x + 8, y + 56, x + 8 + done, y + 61, 0xFFE07A2A);
    }

    @Override
    protected void renderLabels(GuiGraphics g, int mouseX, int mouseY) {
        super.renderLabels(g, mouseX, mouseY);
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);
        renderTooltip(g, mouseX, mouseY);
        if (mouseX >= leftPos + 7 && mouseX < leftPos + 169 && mouseY >= topPos + 55 && mouseY < topPos + 62) {
            g.renderTooltip(font, Component.translatable("gui.futuretek.kiln_hint", CharcoalKiln.PER_CYCLE,
                    CharcoalKiln.CYCLE / 20), mouseX, mouseY);
        }
    }
}
