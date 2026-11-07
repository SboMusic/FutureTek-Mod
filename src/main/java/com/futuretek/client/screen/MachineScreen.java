package com.futuretek.client.screen;

import com.futuretek.energy.EnergyUtil;
import com.futuretek.menu.MachineMenu;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;

/**
 * Base grafica delle interfacce delle macchine (SOLO client).
 * Disegna tutto con rettangoli colorati: pannello, slot e barra dell'energia.
 * Cosi' non servono immagini, e ogni nuova macchina ha subito un'interfaccia coerente.
 */
public abstract class MachineScreen<T extends MachineMenu> extends AbstractContainerScreen<T> {
    // colori in formato 0xAARRGGBB
    protected static final int PANEL = 0xFFC6C6C6;
    protected static final int LIGHT = 0xFFFFFFFF;
    protected static final int DARK = 0xFF555555;
    protected static final int SLOT_BG = 0xFF8B8B8B;
    protected static final int SLOT_DARK = 0xFF373737;
    protected static final int ENERGY_FULL = 0xFFE8402A;
    protected static final int ENERGY_EMPTY = 0xFF2B1B1B;

    /** Posizione e dimensione della barra dell'energia (relative al pannello) */
    protected int energyX = 152;
    protected int energyY = 17;
    protected int energyW = 14;
    protected int energyH = 52;

    protected MachineScreen(T menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = 176;
        imageHeight = 166;
        inventoryLabelY = imageHeight - 94;
    }

    @Override
    protected void renderBg(GuiGraphics g, float partialTick, int mouseX, int mouseY) {
        int x = leftPos;
        int y = topPos;
        // pannello con bordo in rilievo
        g.fill(x, y, x + imageWidth, y + imageHeight, PANEL);
        g.fill(x, y, x + imageWidth - 1, y + 1, LIGHT);
        g.fill(x, y, x + 1, y + imageHeight - 1, LIGHT);
        g.fill(x + 1, y + imageHeight - 1, x + imageWidth, y + imageHeight, DARK);
        g.fill(x + imageWidth - 1, y + 1, x + imageWidth, y + imageHeight, DARK);
        // uno sfondo per ogni slot
        for (Slot slot : menu.slots) {
            if (slot.isActive()) {
                drawSlot(g, x + slot.x - 1, y + slot.y - 1, 18, 18);
            }
        }
        // barra dell'energia
        drawSlot(g, x + energyX - 1, y + energyY - 1, energyW + 2, energyH + 2);
        g.fill(x + energyX, y + energyY, x + energyX + energyW, y + energyY + energyH, ENERGY_EMPTY);
        int filled = scaled(menu.getEnergy(), menu.getCapacity(), energyH);
        g.fill(x + energyX, y + energyY + energyH - filled, x + energyX + energyW, y + energyY + energyH, ENERGY_FULL);
        drawExtras(g, x, y);
    }

    /** Per le macchine che vogliono disegnare altro (fiamme, serbatoi, frecce...) */
    protected void drawExtras(GuiGraphics g, int x, int y) {
    }

    /** Riquadro "incassato" come gli slot di Minecraft */
    public static void drawSlot(GuiGraphics g, int x, int y, int w, int h) {
        g.fill(x, y, x + w, y + h, SLOT_BG);
        g.fill(x, y, x + w - 1, y + 1, SLOT_DARK);
        g.fill(x, y, x + 1, y + h - 1, SLOT_DARK);
        g.fill(x + 1, y + h - 1, x + w, y + h, LIGHT);
        g.fill(x + w - 1, y + 1, x + w, y + h, LIGHT);
    }

    public static int scaled(long value, long max, int size) {
        if (max <= 0 || value <= 0) {
            return 0;
        }
        return (int) Math.min(size, value * size / max);
    }

    protected boolean isHovering(int relX, int relY, int w, int h, double mouseX, double mouseY) {
        return mouseX >= leftPos + relX && mouseX < leftPos + relX + w
                && mouseY >= topPos + relY && mouseY < topPos + relY + h;
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);
        renderTooltip(g, mouseX, mouseY);
        if (isHovering(energyX, energyY, energyW, energyH, mouseX, mouseY)) {
            g.renderTooltip(font, Component.translatable("gui.futuretek.energy",
                    EnergyUtil.format(menu.getEnergy()), EnergyUtil.format(menu.getCapacity())), mouseX, mouseY);
        }
        renderExtraTooltips(g, mouseX, mouseY);
    }

    protected void renderExtraTooltips(GuiGraphics g, int mouseX, int mouseY) {
    }
}
