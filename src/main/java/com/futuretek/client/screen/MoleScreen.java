package com.futuretek.client.screen;

import com.futuretek.mining.MiningMole;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;

/** Interfaccia della talpa mineraria: raggio, Avvia/Ferma, carburante, stato (SOLO client). */
public class MoleScreen extends AbstractContainerScreen<MiningMole.MoleMenu> {
    private Button toggle;

    public MoleScreen(MiningMole.MoleMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = 176;
        imageHeight = 184;
        inventoryLabelY = 91;
    }

    private void press(int id) {
        if (minecraft != null && minecraft.gameMode != null) {
            minecraft.gameMode.handleInventoryButtonClick(menu.containerId, id);
        }
    }

    @Override
    protected void init() {
        super.init();
        addRenderableWidget(Button.builder(Component.literal("-"), b -> press(MiningMole.MoleMenu.BUTTON_LESS))
                .bounds(leftPos + 8, topPos + 16, 14, 14).build());
        addRenderableWidget(Button.builder(Component.literal("+"), b -> press(MiningMole.MoleMenu.BUTTON_MORE))
                .bounds(leftPos + 70, topPos + 16, 14, 14).build());
        toggle = addRenderableWidget(Button.builder(Component.translatable("gui.futuretek.mole_start"),
                b -> press(MiningMole.MoleMenu.BUTTON_TOGGLE)).bounds(leftPos + 88, topPos + 16, 56, 14).build());
    }

    private boolean running() {
        MiningMole.State s = menu.getState();
        return s == MiningMole.State.MINING || s == MiningMole.State.RETURNING || s == MiningMole.State.RESUMING
                || s == MiningMole.State.AT_HOME;
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        toggle.setMessage(Component.translatable(running() ? "gui.futuretek.mole_stop" : "gui.futuretek.mole_start"));
    }

    @Override
    protected void renderBg(GuiGraphics g, float partialTick, int mouseX, int mouseY) {
        int x = leftPos;
        int y = topPos;
        g.fill(x, y, x + imageWidth, y + imageHeight, 0xFFB9B2A6);
        g.fill(x, y, x + imageWidth - 1, y + 1, 0xFFFFFFFF);
        g.fill(x, y, x + 1, y + imageHeight - 1, 0xFFFFFFFF);
        g.fill(x + 1, y + imageHeight - 1, x + imageWidth, y + imageHeight, 0xFF4E4A42);
        g.fill(x + imageWidth - 1, y + 1, x + imageWidth, y + imageHeight, 0xFF4E4A42);
        for (Slot slot : menu.slots) {
            MachineScreen.drawSlot(g, x + slot.x - 1, y + slot.y - 1, 18, 18);
        }
        // barra del carburante sotto lo slot del combustibile
        MachineScreen.drawSlot(g, x + 151, y + 37, 18, 6);
        int f = MachineScreen.scaled(menu.getFuel(), MiningMole.MAX_FUEL, 16);
        g.fill(x + 152, y + 38, x + 152 + f, y + 42, 0xFFE0A030);
    }

    @Override
    protected void renderLabels(GuiGraphics g, int mouseX, int mouseY) {
        super.renderLabels(g, mouseX, mouseY);
        int side = menu.getRadius() * 2 + 1;
        g.drawCenteredString(font, side + "×" + side, 46, 19, 0xFFFFFFFF);
        g.drawString(font, Component.translatable("gui.futuretek.mole_state." + menu.getState().name().toLowerCase()),
                8, 34, 0xFF303030, false);
        g.drawString(font, Component.translatable("gui.futuretek.mole_info", menu.getDepth(), menu.getMined(), menu.getFuel()),
                8, 102 - 60, 0xFF505050, false);
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);
        renderTooltip(g, mouseX, mouseY);
        if (mouseX >= leftPos + 151 && mouseX < leftPos + 170 && mouseY >= topPos + 17 && mouseY < topPos + 44
                && !menu.slots.get(MiningMole.FUEL_SLOT).hasItem()) {
            g.renderTooltip(font, Component.translatable("gui.futuretek.mole_fuel"), mouseX, mouseY);
        }
    }
}
