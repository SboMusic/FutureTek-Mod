package com.futuretek.client.screen;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import com.futuretek.logistics.TerminalClientData;
import com.futuretek.logistics.TerminalPayloads;
import com.futuretek.menu.AutoCrafterMenu;
import com.futuretek.menu.FilterMenu;
import com.futuretek.menu.TerminalMenu;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;

/** Interfacce della logistica (SOLO client). */
public final class LogisticsScreens {
    private LogisticsScreens() {
    }

    private static void panel(GuiGraphics g, int x, int y, int w, int h) {
        g.fill(x, y, x + w, y + h, 0xFFB8C6C6);
        g.fill(x, y, x + w - 1, y + 1, 0xFFFFFFFF);
        g.fill(x, y, x + 1, y + h - 1, 0xFFFFFFFF);
        g.fill(x + 1, y + h - 1, x + w, y + h, 0xFF485A5A);
        g.fill(x + w - 1, y + 1, x + w, y + h, 0xFF485A5A);
    }

    /** Base: pannello + sfondo degli slot */
    abstract static class Simple<T extends AbstractContainerMenu> extends AbstractContainerScreen<T> {
        Simple(T menu, Inventory inventory, Component title, int height) {
            super(menu, inventory, title);
            imageWidth = 176;
            imageHeight = height;
            inventoryLabelY = imageHeight - 94;
        }

        @Override
        protected void renderBg(GuiGraphics g, float partialTick, int mouseX, int mouseY) {
            panel(g, leftPos, topPos, imageWidth, imageHeight);
            for (Slot slot : menu.slots) {
                MachineScreen.drawSlot(g, leftPos + slot.x - 1, topPos + slot.y - 1, 18, 18);
            }
        }

        @Override
        public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
            super.render(g, mouseX, mouseY, partialTick);
            renderTooltip(g, mouseX, mouseY);
        }
    }

    public static class Filter extends Simple<FilterMenu> {
        public Filter(FilterMenu menu, Inventory inventory, Component title) {
            super(menu, inventory, title, 166);
        }

        @Override
        protected void renderLabels(GuiGraphics g, int mouseX, int mouseY) {
            super.renderLabels(g, mouseX, mouseY);
            g.drawString(font, Component.translatable("gui.futuretek.filter_hint"), 8, 22, 0xFF404040, false);
        }
    }

    /** Tubo analizzatore: quanti oggetti ricorda, filtro extra, "Lascia 1" e "Dimentica" */
    public static class Analyzer extends Simple<com.futuretek.logistics.AnalyzerMenu> {
        private net.minecraft.client.gui.components.Button keep;

        public Analyzer(com.futuretek.logistics.AnalyzerMenu menu, Inventory inventory, Component title) {
            super(menu, inventory, title, 166);
        }

        private void press(int id) {
            if (minecraft != null && minecraft.gameMode != null) {
                minecraft.gameMode.handleInventoryButtonClick(menu.containerId, id);
            }
        }

        @Override
        protected void init() {
            super.init();
            keep = addRenderableWidget(net.minecraft.client.gui.components.Button.builder(Component.empty(),
                    b -> press(com.futuretek.logistics.AnalyzerConduitBlockEntity.BUTTON_KEEP_ONE))
                    .bounds(leftPos + 8, topPos + 55, 90, 13)
                    .tooltip(net.minecraft.client.gui.components.Tooltip.create(Component.translatable("gui.futuretek.keep_one.hint")))
                    .build());
            addRenderableWidget(net.minecraft.client.gui.components.Button.builder(
                    Component.translatable("gui.futuretek.forget"),
                    b -> press(com.futuretek.logistics.AnalyzerConduitBlockEntity.BUTTON_FORGET))
                    .bounds(leftPos + 102, topPos + 55, 66, 13)
                    .tooltip(net.minecraft.client.gui.components.Tooltip.create(Component.translatable("gui.futuretek.forget.hint")))
                    .build());
        }

        @Override
        protected void containerTick() {
            super.containerTick();
            if (keep != null) {
                keep.setMessage(Component.translatable(menu.keepsOne() ? "gui.futuretek.keep_one.on" : "gui.futuretek.keep_one.off"));
            }
        }

        @Override
        protected void renderLabels(GuiGraphics g, int mouseX, int mouseY) {
            super.renderLabels(g, mouseX, mouseY);
            g.drawString(font, Component.translatable("gui.futuretek.analyzer_memory", menu.memorySize()), 8, 22, 0xFF404040, false);
        }
    }

    public static class AutoCrafter extends Simple<AutoCrafterMenu> {
        public AutoCrafter(AutoCrafterMenu menu, Inventory inventory, Component title) {
            super(menu, inventory, title, 166);
        }

        private void press(int id) {
            if (minecraft != null && minecraft.gameMode != null) {
                minecraft.gameMode.handleInventoryButtonClick(menu.containerId, id);
            }
        }

        @Override
        protected void init() {
            super.init();
            int x = leftPos + 102;
            int y = topPos + 16;
            addRenderableWidget(net.minecraft.client.gui.components.Button.builder(Component.literal("+1"),
                    b -> press(AutoCrafterMenu.BUTTON_CRAFT_1)).bounds(x, y, 30, 12).build());
            addRenderableWidget(net.minecraft.client.gui.components.Button.builder(Component.literal("+16"),
                    b -> press(AutoCrafterMenu.BUTTON_CRAFT_16)).bounds(x + 32, y, 34, 12).build());
            addRenderableWidget(net.minecraft.client.gui.components.Button.builder(Component.literal("-"),
                    b -> press(AutoCrafterMenu.BUTTON_STOCK_DOWN)).bounds(x, y + 40, 14, 12).build());
            addRenderableWidget(net.minecraft.client.gui.components.Button.builder(Component.literal("+"),
                    b -> press(AutoCrafterMenu.BUTTON_STOCK_UP)).bounds(x + 16, y + 40, 14, 12).build());
            addRenderableWidget(net.minecraft.client.gui.components.Button.builder(Component.literal("0"),
                    b -> press(AutoCrafterMenu.BUTTON_STOCK_OFF)).bounds(x + 32, y + 40, 14, 12)
                    .tooltip(net.minecraft.client.gui.components.Tooltip.create(Component.translatable("gui.futuretek.stock_off")))
                    .build());
        }

        @Override
        protected void renderLabels(GuiGraphics g, int mouseX, int mouseY) {
            super.renderLabels(g, mouseX, mouseY);
            g.drawString(font, "→", 88, 35, 0xFF404040, false);
            g.drawString(font, Component.translatable("gui.futuretek.queued", menu.getQueued()), 102, 30, 0xFF404040, false);
            g.drawString(font, menu.getStock() > 0 ? Component.translatable("gui.futuretek.stock", menu.getStock())
                    : Component.translatable("gui.futuretek.stock_none"), 102, 46, 0xFF404040, false);
        }
    }

    /** Arnia tecnica: regina, fuco, barra del ciclo e prodotti */
    public static class Apiary extends Simple<com.futuretek.bee.ApiaryMenu> {
        public Apiary(com.futuretek.bee.ApiaryMenu menu, Inventory inventory, Component title) {
            super(menu, inventory, title, 166);
        }

        @Override
        protected void renderBg(GuiGraphics g, float partialTick, int mouseX, int mouseY) {
            super.renderBg(g, partialTick, mouseX, mouseY);
            int x = leftPos + 50;
            int y = topPos + 36;
            MachineScreen.drawSlot(g, x - 1, y - 1, 38, 8);
            int done = MachineScreen.scaled(menu.getProgress(), com.futuretek.bee.ApiaryBlockEntity.CYCLE_TICKS, 36);
            g.fill(x, y, x + done, y + 6, 0xFFE8C04A);
        }

        @Override
        protected void renderLabels(GuiGraphics g, int mouseX, int mouseY) {
            super.renderLabels(g, mouseX, mouseY);
            g.drawString(font, Component.translatable("gui.futuretek.queen"), 46, 22, 0xFF404040, false);
            g.drawString(font, Component.translatable("gui.futuretek.drone"), 46, 52, 0xFF404040, false);
        }
    }

    /** Terminale di rete: griglia scorrevole con ricerca */
    public static class Terminal extends Simple<TerminalMenu> {
        private static final int COLS = 9;
        private static final int ROWS = 6;
        private static final int GRID_X = 8;
        private static final int GRID_Y = 18;
        private EditBox search;
        private int scroll;

        public Terminal(TerminalMenu menu, Inventory inventory, Component title) {
            super(menu, inventory, title, 222);
            inventoryLabelY = TerminalMenu.INVENTORY_TOP - 11;
        }

        @Override
        protected void init() {
            super.init();
            search = new EditBox(font, leftPos + 88, topPos + 5, 80, 10, Component.empty());
            search.setBordered(false);
            search.setTextColor(0xFFFFFF);
            search.setHint(Component.translatable("gui.futuretek.search").withStyle(ChatFormatting.GRAY));
            addRenderableWidget(search);
        }

        /** Voci visibili: oggetti presenti + oggetti fabbricabili (quantita' 0), filtrati dalla ricerca */
        private List<TerminalPayloads.Entry> visible() {
            String query = search == null ? "" : search.getValue().toLowerCase(Locale.ROOT);
            List<TerminalPayloads.Entry> list = new ArrayList<>();
            for (TerminalPayloads.Entry e : TerminalClientData.contents.entries()) {
                if (e.item().getHoverName().getString().toLowerCase(Locale.ROOT).contains(query)) {
                    list.add(e);
                }
            }
            for (ItemStack craftable : TerminalClientData.contents.craftables()) {
                boolean present = list.stream().anyMatch(e -> ItemStack.isSameItemSameComponents(e.item(), craftable));
                if (!present && craftable.getHoverName().getString().toLowerCase(Locale.ROOT).contains(query)) {
                    list.add(new TerminalPayloads.Entry(craftable, 0));
                }
            }
            return list;
        }

        private boolean isCraftable(ItemStack stack) {
            return TerminalClientData.contents.craftables().stream().anyMatch(c -> ItemStack.isSameItemSameComponents(c, stack));
        }

        private static String shortCount(long n) {
            if (n >= 1_000_000) {
                return (n / 100_000) / 10.0 + "M";
            }
            if (n >= 10_000) {
                return n / 1000 + "k";
            }
            return Long.toString(n);
        }

        @Override
        protected void renderBg(GuiGraphics g, float partialTick, int mouseX, int mouseY) {
            super.renderBg(g, partialTick, mouseX, mouseY);
            g.fill(leftPos + 86, topPos + 3, leftPos + 170, topPos + 15, 0xFF202828);
            List<TerminalPayloads.Entry> list = visible();
            int maxScroll = Math.max(0, (list.size() + COLS - 1) / COLS - ROWS);
            scroll = Math.min(scroll, maxScroll);
            for (int i = 0; i < COLS * ROWS; i++) {
                int x = leftPos + GRID_X + (i % COLS) * 18;
                int y = topPos + GRID_Y + (i / COLS) * 18;
                MachineScreen.drawSlot(g, x - 1, y - 1, 18, 18);
                int index = scroll * COLS + i;
                if (index < list.size()) {
                    TerminalPayloads.Entry e = list.get(index);
                    g.renderItem(e.item(), x, y);
                    String text = e.count() == 0 ? "+" : shortCount(e.count());
                    g.pose().pushPose();
                    g.pose().translate(0, 0, 200);
                    g.pose().scale(0.5F, 0.5F, 1);
                    g.drawString(font, text, (x + 16) * 2 - font.width(text), (y + 12) * 2,
                            e.count() == 0 ? 0xFF7CFC7C : 0xFFFFFFFF, true);
                    g.pose().popPose();
                }
            }
        }

        private int gridIndex(double mouseX, double mouseY) {
            double rx = mouseX - leftPos - GRID_X;
            double ry = mouseY - topPos - GRID_Y;
            if (rx < 0 || ry < 0 || rx >= COLS * 18 || ry >= ROWS * 18) {
                return -1;
            }
            return scroll * COLS + (int) (ry / 18) * COLS + (int) (rx / 18);
        }

        @Override
        public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
            super.render(g, mouseX, mouseY, partialTick);
            int index = gridIndex(mouseX, mouseY);
            List<TerminalPayloads.Entry> list = visible();
            if (index >= 0 && index < list.size()) {
                TerminalPayloads.Entry e = list.get(index);
                List<Component> lines = new ArrayList<>(getTooltipFromContainerItem(e.item()));
                lines.add(Component.translatable("gui.futuretek.in_network", e.count()).withStyle(ChatFormatting.GRAY));
                if (isCraftable(e.item())) {
                    lines.add(Component.translatable("gui.futuretek.craft_hint").withStyle(ChatFormatting.GREEN));
                }
                g.renderComponentTooltip(font, lines, mouseX, mouseY);
            }
        }

        @Override
        public boolean mouseClicked(double mouseX, double mouseY, int button) {
            int index = gridIndex(mouseX, mouseY);
            List<TerminalPayloads.Entry> list = visible();
            if (index >= 0 && index < list.size()) {
                TerminalPayloads.Entry e = list.get(index);
                boolean craft = e.count() == 0 || (button == 2) || (Screen.hasControlDown() && isCraftable(e.item()));
                int amount;
                if (craft) {
                    amount = Screen.hasShiftDown() ? 64 : 1;
                } else {
                    amount = button == 1 ? 1 : e.item().getMaxStackSize();
                }
                PacketDistributor.sendToServer(new TerminalPayloads.Request(e.item(), amount, craft));
                return true;
            }
            return super.mouseClicked(mouseX, mouseY, button);
        }

        @Override
        public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
            scroll = Math.max(0, scroll - (int) Math.signum(scrollY));
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
}
