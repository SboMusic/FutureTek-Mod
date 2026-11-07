package com.futuretek.client.screen;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import com.futuretek.energy.EnergyUtil;
import com.futuretek.essence.EssenceManager;
import com.futuretek.essence.EssenceValues;
import com.futuretek.essence.SynthesisRequestPayload;
import com.futuretek.menu.ReplicatorMenu;
import com.futuretek.menu.SynthesisTableMenu;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;

/** Interfacce della trasmutazione (SOLO client). */
public final class EssenceScreens {
    private EssenceScreens() {
    }

    private static void panel(GuiGraphics g, int x, int y, int w, int h) {
        g.fill(x, y, x + w, y + h, 0xFFC6B8D6);
        g.fill(x, y, x + w - 1, y + 1, 0xFFFFFFFF);
        g.fill(x, y, x + 1, y + h - 1, 0xFFFFFFFF);
        g.fill(x + 1, y + h - 1, x + w, y + h, 0xFF55486A);
        g.fill(x + w - 1, y + 1, x + w, y + h, 0xFF55486A);
    }

    /** Tavolo di Sintesi (e Tavoletta portatile): griglia con ricerca e pagine */
    public static class SynthesisTable extends AbstractContainerScreen<SynthesisTableMenu> {
        private static final int GRID_X = 8;
        private static final int GRID_Y = 30;
        private static final int COLS = 8;
        private static final int ROWS = 5;
        private static final int PAGE_SIZE = COLS * ROWS;
        private EditBox search;
        private int page;

        public SynthesisTable(SynthesisTableMenu menu, Inventory inventory, Component title) {
            super(menu, inventory, title);
            imageWidth = 176;
            imageHeight = 204;
            inventoryLabelY = SynthesisTableMenu.INVENTORY_Y - 11;
        }

        @Override
        protected void init() {
            super.init();
            search = new EditBox(font, leftPos + 10, topPos + 18, 132, 10, Component.empty());
            search.setBordered(false);
            search.setTextColor(0xFFFFFF);
            search.setHint(Component.translatable("gui.futuretek.search").withStyle(ChatFormatting.GRAY));
            search.setResponder(text -> page = 0);
            addRenderableWidget(search);
        }

        /** Oggetti conosciuti che corrispondono alla ricerca */
        private List<Item> filtered() {
            String query = search == null ? "" : search.getValue().toLowerCase(Locale.ROOT).trim();
            List<Item> all = menu.knownItems();
            if (query.isEmpty()) {
                return all;
            }
            List<Item> out = new ArrayList<>();
            for (Item item : all) {
                String name = new ItemStack(item).getHoverName().getString().toLowerCase(Locale.ROOT);
                String id = BuiltInRegistries.ITEM.getKey(item).getPath();
                if (name.contains(query) || id.contains(query)) {
                    out.add(item);
                }
            }
            return out;
        }

        private int pageCount(List<Item> list) {
            return Math.max(1, (list.size() + PAGE_SIZE - 1) / PAGE_SIZE);
        }

        private List<Item> pageItems() {
            List<Item> all = filtered();
            page = Math.min(page, pageCount(all) - 1);
            int from = Math.min(all.size(), page * PAGE_SIZE);
            int to = Math.min(all.size(), from + PAGE_SIZE);
            return all.subList(from, to);
        }

        @Override
        protected void renderBg(GuiGraphics g, float partialTick, int mouseX, int mouseY) {
            panel(g, leftPos, topPos, imageWidth, imageHeight);
            g.fill(leftPos + 8, topPos + 16, leftPos + 144, topPos + 28, 0xFF2A1A3A);
            for (Slot slot : menu.slots) {
                MachineScreen.drawSlot(g, leftPos + slot.x - 1, topPos + slot.y - 1, 18, 18);
            }
            // griglia degli oggetti conosciuti
            List<Item> items = pageItems();
            long essence = EssenceManager.get(Minecraft.getInstance().player).essence();
            for (int i = 0; i < PAGE_SIZE; i++) {
                int x = leftPos + GRID_X + (i % COLS) * 18;
                int y = topPos + GRID_Y + (i / COLS) * 18;
                MachineScreen.drawSlot(g, x - 1, y - 1, 18, 18);
                if (i < items.size()) {
                    Item item = items.get(i);
                    if (EssenceValues.get(true, item) > essence) {
                        g.fill(x, y, x + 16, y + 16, 0x60FF3030); // troppo costoso: sfondo rosso
                    }
                    g.renderItem(new ItemStack(item), x, y);
                }
            }
            // pulsanti pagina
            button(g, leftPos + 152, topPos + 54, "<");
            button(g, leftPos + 152, topPos + 70, ">");
        }

        private void button(GuiGraphics g, int x, int y, String text) {
            MachineScreen.drawSlot(g, x, y, 16, 14);
            g.drawString(font, text, x + 5, y + 3, 0xFF202020, false);
        }

        @Override
        protected void renderLabels(GuiGraphics g, int mouseX, int mouseY) {
            long essence = EssenceManager.get(Minecraft.getInstance().player).essence();
            g.drawString(font, title, 8, 5, 0xFF3A2A50, false);
            String text = EnergyUtil.format(essence) + " ESS";
            g.drawString(font, text, imageWidth - 8 - font.width(text), 5, 0xFF6A2AA0, false);
            List<Item> list = filtered();
            g.drawString(font, (page + 1) + "/" + pageCount(list), 150, 88, 0xFF3A2A50, false);
            g.drawString(font, String.valueOf(menu.knownItems().size()), 150, 100, 0xFF6A2AA0, false);
            g.drawString(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, 0xFF3A2A50, false);
        }

        @Override
        public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
            super.render(g, mouseX, mouseY, partialTick);
            renderTooltip(g, mouseX, mouseY);
            int index = gridIndex(mouseX, mouseY);
            List<Item> items = pageItems();
            if (index >= 0 && index < items.size()) {
                ItemStack stack = new ItemStack(items.get(index));
                List<Component> lines = new ArrayList<>(getTooltipFromContainerItem(stack));
                lines.add(Component.translatable("tooltip.futuretek.essence_cost",
                        EnergyUtil.format(EssenceValues.get(true, stack.getItem()))).withStyle(ChatFormatting.LIGHT_PURPLE));
                lines.add(Component.translatable("tooltip.futuretek.synthesis_click").withStyle(ChatFormatting.DARK_GRAY));
                g.renderComponentTooltip(font, lines, mouseX, mouseY);
            } else if (isOver(mouseX, mouseY, SynthesisTableMenu.DISSOLVE_X, SynthesisTableMenu.DISSOLVE_Y, 16, 16)
                    && !menu.slots.get(0).hasItem()) {
                g.renderTooltip(font, Component.translatable("tooltip.futuretek.dissolve_slot"), mouseX, mouseY);
            } else if (isOver(mouseX, mouseY, 150, 98, 24, 10)) {
                g.renderTooltip(font, Component.translatable("tooltip.futuretek.known_count"), mouseX, mouseY);
            }
        }

        private boolean isOver(double mx, double my, int x, int y, int w, int h) {
            return mx >= leftPos + x && mx < leftPos + x + w && my >= topPos + y && my < topPos + y + h;
        }

        private int gridIndex(double mouseX, double mouseY) {
            double rx = mouseX - leftPos - GRID_X;
            double ry = mouseY - topPos - GRID_Y;
            if (rx < 0 || ry < 0 || rx >= COLS * 18 || ry >= ROWS * 18) {
                return -1;
            }
            return (int) (ry / 18) * COLS + (int) (rx / 18);
        }

        @Override
        public boolean mouseClicked(double mouseX, double mouseY, int button) {
            int index = gridIndex(mouseX, mouseY);
            List<Item> items = pageItems();
            if (index >= 0 && index < items.size()) {
                PacketDistributor.sendToServer(new SynthesisRequestPayload(
                        BuiltInRegistries.ITEM.getKey(items.get(index)), Screen.hasShiftDown()));
                return true;
            }
            if (isOver(mouseX, mouseY, 152, 54, 16, 14)) {
                page = Math.max(0, page - 1);
                return true;
            }
            if (isOver(mouseX, mouseY, 152, 70, 16, 14)) {
                page = Math.min(pageCount(filtered()) - 1, page + 1);
                return true;
            }
            return super.mouseClicked(mouseX, mouseY, button);
        }

        @Override
        public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
            page = Math.max(0, Math.min(pageCount(filtered()) - 1, page - (int) Math.signum(scrollY)));
            return true;
        }

        @Override
        public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
            if (search.isFocused() && keyCode != 256) { // 256 = ESC
                search.keyPressed(keyCode, scanCode, modifiers);
                return true;
            }
            return super.keyPressed(keyCode, scanCode, modifiers);
        }
    }

    /** Replicatore */
    public static class Replicator extends AbstractContainerScreen<ReplicatorMenu> {
        public Replicator(ReplicatorMenu menu, Inventory inventory, Component title) {
            super(menu, inventory, title);
            imageWidth = 176;
            imageHeight = 166;
            inventoryLabelY = imageHeight - 94;
        }

        @Override
        protected void renderBg(GuiGraphics g, float partialTick, int mouseX, int mouseY) {
            panel(g, leftPos, topPos, imageWidth, imageHeight);
            for (Slot slot : menu.slots) {
                MachineScreen.drawSlot(g, leftPos + slot.x - 1, topPos + slot.y - 1, 18, 18);
            }
            // barra: Essenza accumulata rispetto al costo del modello
            int x = leftPos + 116;
            int y = topPos + 17;
            MachineScreen.drawSlot(g, x - 1, y - 1, 8, 54);
            g.fill(x, y, x + 6, y + 52, 0xFF2A1A3A);
            int filled = MachineScreen.scaled(Math.min(menu.getBuffer(), Math.max(1, menu.getTemplateValue())),
                    Math.max(1, menu.getTemplateValue()), 52);
            g.fill(x, y + 52 - filled, x + 6, y + 52, 0xFFB05AFF);
        }

        @Override
        protected void renderLabels(GuiGraphics g, int mouseX, int mouseY) {
            super.renderLabels(g, mouseX, mouseY);
            g.drawString(font, Component.translatable("gui.futuretek.template"), 14, 56, 0xFF3A2A50, false);
            String text = EnergyUtil.format(menu.getBuffer()) + " ESS";
            g.drawString(font, text, imageWidth - 8 - font.width(text), 6, 0xFF6A2AA0, false);
        }

        @Override
        public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
            super.render(g, mouseX, mouseY, partialTick);
            renderTooltip(g, mouseX, mouseY);
        }
    }
}
