package com.futuretek.client.screen;

import java.util.List;
import java.util.Map;

import com.futuretek.automation.Enchanting;
import com.futuretek.automation.NetherMachines;
import com.futuretek.automation.SlotMachines;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.enchantment.Enchantment;

/**
 * Incantatore elettrico (SOLO client). Caselle: oggetto, lapislazzuli, archivio.
 * Senza archivio: 3 pulsanti di incanto a caso (gradi I/II/III).
 * Con l'archivio: scegli l'incantesimo con le frecce, il livello con - e +, poi "Incanta".
 */
public class EnchanterScreen extends MachineScreen<SlotMachines.Menu> {
    private Button[] random;
    private Button prev, next, down, up, apply;

    public EnchanterScreen(SlotMachines.Menu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
    }

    private void press(int id) {
        if (minecraft != null && minecraft.gameMode != null) {
            minecraft.gameMode.handleInventoryButtonClick(menu.containerId, id);
        }
    }

    @Override
    protected void init() {
        super.init();
        random = new Button[3];
        for (int i = 0; i < 3; i++) {
            int id = i;
            random[i] = addRenderableWidget(Button.builder(Component.translatable("gui.futuretek.electric_enchanter.button" + i),
                    b -> press(id)).bounds(leftPos + 8 + i * 47, topPos + 56, 45, 14)
                    .tooltip(Tooltip.create(Component.translatable("gui.futuretek.electric_enchanter.button" + i + ".hint"))).build());
        }
        prev = addRenderableWidget(Button.builder(Component.literal("<"), b -> press(NetherMachines.Enchanter.PREV))
                .bounds(leftPos + 8, topPos + 38, 14, 14).build());
        next = addRenderableWidget(Button.builder(Component.literal(">"), b -> press(NetherMachines.Enchanter.NEXT))
                .bounds(leftPos + 134, topPos + 38, 14, 14).build());
        down = addRenderableWidget(Button.builder(Component.literal("-"), b -> press(NetherMachines.Enchanter.LEVEL_DOWN))
                .bounds(leftPos + 8, topPos + 56, 14, 14).build());
        up = addRenderableWidget(Button.builder(Component.literal("+"), b -> press(NetherMachines.Enchanter.LEVEL_UP))
                .bounds(leftPos + 24, topPos + 56, 14, 14).build());
        apply = addRenderableWidget(Button.builder(Component.empty(), b -> press(NetherMachines.Enchanter.APPLY))
                .bounds(leftPos + 42, topPos + 56, 106, 14).build());
    }

    private boolean hasArchive() {
        return menu.getSlot(2).getItem().getItem() instanceof Enchanting.Archive;
    }

    private List<Map.Entry<Holder.Reference<Enchantment>, Integer>> choices() {
        if (Minecraft.getInstance().level == null) {
            return List.of();
        }
        return Enchanting.usable(Minecraft.getInstance().level.registryAccess(), menu.getSlot(2).getItem(),
                menu.getSlot(0).getItem());
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        boolean archive = hasArchive();
        for (Button b : random) {
            b.visible = !archive;
        }
        prev.visible = next.visible = down.visible = up.visible = apply.visible = archive;
        int[] c = Enchanting.cost(Math.max(1, menu.value(2)));
        apply.setMessage(Component.translatable("gui.futuretek.enchanter_apply", c[0], c[1], c[2] / 1000));
        apply.active = !choices().isEmpty();
    }

    @Override
    protected void renderLabels(GuiGraphics g, int mouseX, int mouseY) {
        super.renderLabels(g, mouseX, mouseY);
        g.drawString(font, Component.translatable("gui.futuretek.energy_k", menu.value(0)), 70, 22, 0xFF404040, false);
        if (!hasArchive()) {
            g.drawString(font, Component.translatable("gui.futuretek.enchanter_no_archive"), 8, 42, 0xFF606060, false);
            return;
        }
        var list = choices();
        Component text;
        if (list.isEmpty()) {
            text = Component.translatable(menu.getSlot(0).getItem().isEmpty() ? "gui.futuretek.enchanter_put_item"
                    : "gui.futuretek.enchanter_nothing");
        } else {
            var e = list.get(Math.floorMod(menu.value(1), list.size()));
            int lvl = Math.max(1, Math.min(menu.value(2), e.getValue()));
            text = Enchantment.getFullname(e.getKey(), lvl);
        }
        int w = font.width(text);
        g.drawString(font, text, 78 - w / 2, 41, 0xFF5A2A8A, false);
    }

}
