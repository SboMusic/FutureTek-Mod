package com.futuretek.client.screen;

import java.util.ArrayList;
import java.util.List;

import com.futuretek.automation.SlotMachines;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/** Interfaccia generica delle macchine delle Fasi 12-14 (SOLO client) */
public class SlotMachineScreen extends MachineScreen<SlotMachines.Menu> {
    private final List<Button> buttons = new ArrayList<>();

    public SlotMachineScreen(SlotMachines.Menu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
    }

    private String key(String what) {
        return "gui.futuretek." + menu.machineId + "." + what;
    }

    private Object[] values() {
        return new Object[]{menu.value(0), menu.value(1), menu.value(2)};
    }

    @Override
    protected void init() {
        super.init();
        buttons.clear();
        int w = menu.buttons == 0 ? 0 : (140 - (menu.buttons - 1) * 2) / menu.buttons;
        for (int i = 0; i < menu.buttons; i++) {
            int id = i;
            Button.Builder b = Button.builder(Component.empty(), btn -> {
                if (minecraft != null && minecraft.gameMode != null) {
                    minecraft.gameMode.handleInventoryButtonClick(menu.containerId, id);
                }
            }).bounds(leftPos + 8 + i * (w + 2), topPos + 56, w, 14);
            if (I18n.exists(key("button" + i + ".hint"))) {
                b.tooltip(Tooltip.create(Component.translatable(key("button" + i + ".hint"))));
            }
            buttons.add(addRenderableWidget(b.build()));
        }
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        for (int i = 0; i < buttons.size(); i++) {
            buttons.get(i).setMessage(Component.translatable(key("button" + i), values()));
        }
    }

    @Override
    protected void renderLabels(GuiGraphics g, int mouseX, int mouseY) {
        super.renderLabels(g, mouseX, mouseY);
        String status = key("status" + menu.status());
        if (I18n.exists(status)) {
            int y = menu.rows() <= 1 ? 40 : 58;
            if (menu.slotCount == 0) {
                y = 22;
            }
            g.drawString(font, Component.translatable(status, values()), 8, y, 0xFF404040, false);
        }
    }
}
