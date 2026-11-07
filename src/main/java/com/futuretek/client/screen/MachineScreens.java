package com.futuretek.client.screen;

import com.futuretek.energy.EnergyUtil;
import com.futuretek.menu.BatteryBoxMenu;
import com.futuretek.menu.CombustionGeneratorMenu;
import com.futuretek.menu.GeothermalGeneratorMenu;
import com.futuretek.menu.ProcessingMachineMenu;
import com.futuretek.menu.ReactorMenu;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/** Le interfacce delle macchine della Fase 2 (SOLO client). */
public final class MachineScreens {
    private MachineScreens() {
    }

    /** Generatore a combustione: slot del combustibile + fiamma che si consuma */
    public static class CombustionGenerator extends MachineScreen<CombustionGeneratorMenu> {
        public CombustionGenerator(CombustionGeneratorMenu menu, Inventory inventory, Component title) {
            super(menu, inventory, title);
        }

        @Override
        protected void drawExtras(GuiGraphics g, int x, int y) {
            // fiamma: 14x14 sopra lo slot, si abbassa mentre il combustibile brucia
            int fx = x + 81;
            int fy = y + 34;
            g.fill(fx, fy, fx + 14, fy + 14, 0xFF4A4A4A);
            int flame = scaled(menu.getBurnTime(), menu.getBurnTimeTotal(), 14);
            g.fill(fx, fy + 14 - flame, fx + 14, fy + 14, 0xFFFF9A1F);
            if (flame > 3) {
                g.fill(fx + 4, fy + 14 - flame + 3, fx + 10, fy + 14, 0xFFFFE066);
            }
        }
    }

    /** Generatore geotermico: serbatoio di lava */
    public static class GeothermalGenerator extends MachineScreen<GeothermalGeneratorMenu> {
        private static final int TANK_X = 80;
        private static final int TANK_Y = 17;
        private static final int TANK_W = 16;
        private static final int TANK_H = 52;

        public GeothermalGenerator(GeothermalGeneratorMenu menu, Inventory inventory, Component title) {
            super(menu, inventory, title);
        }

        @Override
        protected void drawExtras(GuiGraphics g, int x, int y) {
            drawSlot(g, x + TANK_X - 1, y + TANK_Y - 1, TANK_W + 2, TANK_H + 2);
            g.fill(x + TANK_X, y + TANK_Y, x + TANK_X + TANK_W, y + TANK_Y + TANK_H, 0xFF2A1A12);
            int filled = scaled(menu.getLava(), menu.getTankCapacity(), TANK_H);
            g.fill(x + TANK_X, y + TANK_Y + TANK_H - filled, x + TANK_X + TANK_W, y + TANK_Y + TANK_H, 0xFFE0661A);
        }

        @Override
        protected void renderExtraTooltips(GuiGraphics g, int mouseX, int mouseY) {
            if (isHovering(TANK_X, TANK_Y, TANK_W, TANK_H, mouseX, mouseY)) {
                g.renderTooltip(font, Component.translatable("gui.futuretek.lava",
                        EnergyUtil.format(menu.getLava()), EnergyUtil.format(menu.getTankCapacity())), mouseX, mouseY);
            }
        }
    }

    /** Macchine di lavorazione: ingresso -> freccia di avanzamento -> prodotti; potenziamenti sotto */
    public static class ProcessingMachine extends MachineScreen<ProcessingMachineMenu> {
        public ProcessingMachine(ProcessingMachineMenu menu, Inventory inventory, Component title) {
            super(menu, inventory, title);
        }

        @Override
        protected void drawExtras(GuiGraphics g, int x, int y) {
            // freccia: fusto + punta, si riempie da sinistra a destra
            int ax = x + 66;
            int ay = y + 30;
            g.fill(ax, ay + 2, ax + 18, ay + 6, 0xFF8B8B8B);
            g.fill(ax + 18, ay, ax + 22, ay + 8, 0xFF8B8B8B);
            int done = scaled(menu.getProgress(), menu.getMaxProgress(), 22);
            if (done > 0) {
                g.fill(ax, ay + 2, ax + Math.min(done, 18), ay + 6, 0xFF3CC24A);
                if (done > 18) {
                    g.fill(ax + 18, ay, ax + done, ay + 8, 0xFF3CC24A);
                }
            }
        }

        @Override
        protected void renderLabels(GuiGraphics g, int mouseX, int mouseY) {
            super.renderLabels(g, mouseX, mouseY);
            g.drawString(font, Component.translatable("gui.futuretek.upgrades"), 8, 59, 0xFF404040, false);
        }

        private static final int TANK_X = 12;
        private static final int TANK_Y = 17;
        private static final int TANK_W = 12;
        private static final int TANK_H = 34;

        @Override
        protected void renderBg(GuiGraphics g, float partialTick, int mouseX, int mouseY) {
            super.renderBg(g, partialTick, mouseX, mouseY);
            if (menu.usesWater()) {
                int x = leftPos;
                int y = topPos;
                drawSlot(g, x + TANK_X - 1, y + TANK_Y - 1, TANK_W + 2, TANK_H + 2);
                g.fill(x + TANK_X, y + TANK_Y, x + TANK_X + TANK_W, y + TANK_Y + TANK_H, 0xFF1A2230);
                int filled = scaled(menu.getWater(), com.futuretek.blockentity.ProcessingMachineBlockEntity.TANK_CAPACITY, TANK_H);
                g.fill(x + TANK_X, y + TANK_Y + TANK_H - filled, x + TANK_X + TANK_W, y + TANK_Y + TANK_H, 0xFF3A6FE0);
            }
        }

        @Override
        protected void renderExtraTooltips(GuiGraphics g, int mouseX, int mouseY) {
            if (menu.usesWater() && isHovering(TANK_X, TANK_Y, TANK_W, TANK_H, mouseX, mouseY)) {
                g.renderTooltip(font, Component.translatable("gui.futuretek.water",
                        EnergyUtil.format(menu.getWater()),
                        EnergyUtil.format(com.futuretek.blockentity.ProcessingMachineBlockEntity.TANK_CAPACITY)), mouseX, mouseY);
            }
        }
    }

    /** Reattore: griglia 6 righe x (3 + camere) colonne, barra del calore, produzione e stato */
    public static class Reactor extends MachineScreen<ReactorMenu> {
        public Reactor(ReactorMenu menu, Inventory inventory, Component title) {
            super(menu, inventory, title);
            imageWidth = 194;
            imageHeight = 222;
            inventoryLabelY = 10_000; // niente scritta "Inventario": c'e' la barra del calore
            energyX = 174;
            energyY = 18;
            energyW = 12;
            energyH = 106;
        }

        @Override
        protected void drawExtras(GuiGraphics g, int x, int y) {
            // caselle che arriveranno con le camere: riquadri scuri vuoti
            for (int col = menu.getColumns(); col < 9; col++) {
                for (int row = 0; row < 6; row++) {
                    g.fill(x + 8 + col * 18, y + 18 + row * 18, x + 24 + col * 18, y + 34 + row * 18, 0xFFA8A8A8);
                }
            }
            drawSlot(g, x + 7, y + 127, 164, 8);
            g.fill(x + 8, y + 128, x + 170, y + 134, 0xFF2A2A2A);
            int filled = scaled(menu.getHeat(), Math.max(1, menu.getMaxHeat()), 162);
            int pct = menu.getMaxHeat() > 0 ? menu.getHeat() * 100 / menu.getMaxHeat() : 0;
            int color = pct < 50 ? 0xFF3CC24A : pct < 70 ? 0xFFE8B030 : 0xFFE83020;
            g.fill(x + 8, y + 128, x + 8 + filled, y + 134, color);
        }

        @Override
        protected void renderLabels(GuiGraphics g, int mouseX, int mouseY) {
            g.drawString(font, title, 8, 6, 0xFF404040, false);
            Component state = Component.translatable(menu.isRunning() ? "gui.futuretek.reactor_on" : "gui.futuretek.reactor_off");
            String text = menu.getOutput() + " TU/t";
            g.drawString(font, text, 186 - font.width(text), 6, 0xFF2A7A2A, false);
            g.drawString(font, state, 186 - font.width(text) - 6 - font.width(state), 6,
                    menu.isRunning() ? 0xFF2A7A2A : 0xFF7A2A2A, false);
        }

        @Override
        protected void renderExtraTooltips(GuiGraphics g, int mouseX, int mouseY) {
            if (isHovering(8, 128, 162, 6, mouseX, mouseY)) {
                g.renderTooltip(font, Component.translatable("gui.futuretek.heat",
                        EnergyUtil.format(menu.getHeat()), EnergyUtil.format(menu.getMaxHeat())), mouseX, mouseY);
            }
            if (menu.getColumns() < 9 && isHovering(8 + menu.getColumns() * 18, 18, (9 - menu.getColumns()) * 18, 108, mouseX, mouseY)) {
                g.renderTooltip(font, Component.translatable("gui.futuretek.reactor_more_columns"), mouseX, mouseY);
            }
        }
    }

    /** Banco accumulatore: slot di ricarica/scarica e indicatore della carica */
    public static class BatteryBox extends MachineScreen<BatteryBoxMenu> {
        public BatteryBox(BatteryBoxMenu menu, Inventory inventory, Component title) {
            super(menu, inventory, title);
        }

        @Override
        protected void renderLabels(GuiGraphics g, int mouseX, int mouseY) {
            super.renderLabels(g, mouseX, mouseY);
            g.drawString(font, Component.translatable("gui.futuretek.charge"), 76, 21, 0xFF404040, false);
            g.drawString(font, Component.translatable("gui.futuretek.discharge"), 76, 57, 0xFF404040, false);
            String text = EnergyUtil.format(menu.getEnergy()) + " TU";
            g.drawString(font, text, 76, 39, 0xFF404040, false);
        }
    }
}
