package com.futuretek.automation;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;

import javax.annotation.Nullable;

import com.futuretek.registry.ModBlockEntities;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.EnchantmentTags;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Sistema degli incantesimi "a ricerca":
 *  - ARCHIVIO DEGLI INCANTESIMI: un oggetto che ricorda quali incantesimi conosci e fino a che livello;
 *  - RICERCATORE DI INCANTESIMI: con energia e tempo scopre da solo un incantesimo nuovo (o un livello in piu');
 *    se gli dai un libro incantato (o un oggetto incantato) lo legge e salva SUBITO quegli incantesimi;
 *  - l'INCANTATORE ELETTRICO, con l'archivio dentro, ti fa scegliere l'incantesimo e il livello esatti.
 * Le maledizioni non si ricercano.
 */
public final class Enchanting {
    private Enchanting() {
    }

    // =====================================================================
    // Archivio
    // =====================================================================
    public static class Archive extends Item {
        public Archive(Properties props) {
            super(props.stacksTo(1));
        }

        /** Incantesimi conosciuti: id -> livello massimo (in ordine alfabetico) */
        public static TreeMap<ResourceLocation, Integer> known(ItemStack stack) {
            TreeMap<ResourceLocation, Integer> map = new TreeMap<>();
            CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getCompound("Known");
            for (String k : tag.getAllKeys()) {
                ResourceLocation id = ResourceLocation.tryParse(k);
                if (id != null) {
                    map.put(id, tag.getInt(k));
                }
            }
            return map;
        }

        /** Salva un incantesimo (tiene il livello piu' alto). true = era nuovo o piu' alto */
        public static boolean learn(ItemStack stack, ResourceLocation id, int level) {
            TreeMap<ResourceLocation, Integer> map = known(stack);
            if (map.getOrDefault(id, 0) >= level) {
                return false;
            }
            map.put(id, level);
            CompoundTag known = new CompoundTag();
            map.forEach((k, v) -> known.putInt(k.toString(), v));
            CompoundTag root = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
            root.put("Known", known);
            stack.set(DataComponents.CUSTOM_DATA, CustomData.of(root));
            return true;
        }

        @Override
        public boolean isFoil(ItemStack stack) {
            return !known(stack).isEmpty();
        }

        @Override
        public void appendHoverText(ItemStack stack, TooltipContext ctx, List<Component> tooltip, TooltipFlag flag) {
            TreeMap<ResourceLocation, Integer> map = known(stack);
            tooltip.add(Component.translatable("tooltip.futuretek.enchant_archive.count", map.size()).withStyle(ChatFormatting.AQUA));
            HolderLookup.Provider regs = ctx.registries();
            int shown = 0;
            for (var e : map.entrySet()) {
                if (shown++ >= 12) {
                    tooltip.add(Component.literal("…").withStyle(ChatFormatting.DARK_GRAY));
                    break;
                }
                Optional<Holder.Reference<Enchantment>> h = regs == null ? Optional.empty()
                        : holder(regs, e.getKey());
                tooltip.add(h.map(x -> Enchantment.getFullname(x, e.getValue()).copy().withStyle(ChatFormatting.GRAY))
                        .orElse(Component.literal(e.getKey() + " " + e.getValue()).withStyle(ChatFormatting.GRAY)));
            }
            com.futuretek.alchemy.AlchemyItem.addDescription(this, tooltip);
        }
    }

    static Optional<Holder.Reference<Enchantment>> holder(HolderLookup.Provider regs, ResourceLocation id) {
        return regs.lookup(Registries.ENCHANTMENT).flatMap(r -> r.get(ResourceKey.create(Registries.ENCHANTMENT, id)));
    }

    /** Per l'incantatore: gli incantesimi dell'archivio che si possono mettere su quell'oggetto (stesso ordine su client e server) */
    public static List<Map.Entry<Holder.Reference<Enchantment>, Integer>> usable(HolderLookup.Provider regs, ItemStack archive,
            ItemStack item) {
        List<Map.Entry<Holder.Reference<Enchantment>, Integer>> list = new ArrayList<>();
        if (item.isEmpty() || !(archive.getItem() instanceof Archive)) {
            return list;
        }
        boolean book = item.is(Items.BOOK) || item.is(Items.ENCHANTED_BOOK);
        for (var e : Archive.known(archive).entrySet()) {
            holder(regs, e.getKey()).ifPresent(h -> {
                if (book || h.value().canEnchant(item)) {
                    list.add(Map.entry(h, e.getValue()));
                }
            });
        }
        return list;
    }

    // =====================================================================
    // Ricercatore
    // =====================================================================
    public static class Researcher extends SlotMachines.Entity {
        public static final int TIME = 1200;          // 1 minuto per ogni scoperta
        public static final int COST_PER_TICK = 50;   // 60.000 TU per scoperta
        public static final int SCAN_COST = 1000;
        private int progress;

        public Researcher(BlockPos pos, BlockState state) {
            super(ModBlockEntities.ENCHANT_RESEARCHER.get(), pos, state, 100_000, 512, 3);
        }

        @Override
        protected boolean accepts(int slot, ItemStack stack) {
            return switch (slot) {
                case 0 -> stack.getItem() instanceof Archive;
                case 1 -> stack.is(Items.ENCHANTED_BOOK) || stack.isEnchanted();
                default -> false;
            };
        }

        @Override
        public void serverTick() {
            ItemStack archive = items.getStackInSlot(0);
            if (!(archive.getItem() instanceof Archive)) {
                status = 0;
                v1 = 0;
                setLit(false);
                return;
            }
            v2 = Archive.known(archive).size();
            // 1) legge il libro (o l'oggetto) incantato: subito
            ItemStack input = items.getStackInSlot(1);
            if (!input.isEmpty() && items.getStackInSlot(2).isEmpty() && energy.getEnergyStored() >= SCAN_COST) {
                ItemEnchantments ench = input.is(Items.ENCHANTED_BOOK)
                        ? input.getOrDefault(DataComponents.STORED_ENCHANTMENTS, ItemEnchantments.EMPTY)
                        : input.getOrDefault(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY);
                ItemStack copy = archive.copy();
                for (var e : ench.entrySet()) {
                    e.getKey().unwrapKey().ifPresent(k -> Archive.learn(copy, k.location(), e.getIntValue()));
                }
                items.setStackInSlot(0, copy);
                items.setStackInSlot(1, ItemStack.EMPTY);
                items.setStackInSlot(2, input.is(Items.ENCHANTED_BOOK) ? new ItemStack(Items.BOOK) : input);
                energy.consume(SCAN_COST);
                status = 4;
                level.playSound(null, worldPosition, SoundEvents.BOOK_PAGE_TURN, SoundSource.BLOCKS, 1.0F, 1.0F);
                return;
            }
            // 2) ricerca col tempo
            List<Holder.Reference<Enchantment>> todo = new ArrayList<>();
            TreeMap<ResourceLocation, Integer> known = Archive.known(archive);
            level.registryAccess().registryOrThrow(Registries.ENCHANTMENT).holders().forEach(h -> {
                if (!h.is(EnchantmentTags.CURSE) && known.getOrDefault(h.key().location(), 0) < h.value().getMaxLevel()) {
                    todo.add(h);
                }
            });
            if (todo.isEmpty()) {
                status = 3;
                setLit(false);
                return;
            }
            if (energy.getEnergyStored() < COST_PER_TICK) {
                status = 2;
                setLit(false);
                return;
            }
            energy.consume(COST_PER_TICK);
            progress++;
            status = 1;
            v1 = progress * 100 / TIME;
            setLit(true);
            if (progress >= TIME) {
                progress = 0;
                Holder.Reference<Enchantment> found = todo.get(level.random.nextInt(todo.size()));
                ItemStack copy = archive.copy();
                Archive.learn(copy, found.key().location(), known.getOrDefault(found.key().location(), 0) + 1);
                items.setStackInSlot(0, copy);
                level.playSound(null, worldPosition, SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.BLOCKS, 1.0F, 1.4F);
            }
        }

        @Override
        protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
            super.saveAdditional(tag, registries);
            tag.putInt("Progress", progress);
        }

        @Override
        protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
            super.loadAdditional(tag, registries);
            progress = tag.getInt("Progress");
        }
    }

    /** Costo dell'incanto scelto: livelli di esperienza, lapislazzuli, energia */
    public static int[] cost(int level) {
        return new int[]{3 * level, level, 40_000 * level};
    }

    /** Mette l'incantesimo (o ne alza il livello); i libri diventano libri incantati. null = non compatibile */
    @Nullable
    public static ItemStack apply(ItemStack item, Holder<Enchantment> ench, int level) {
        boolean book = item.is(Items.BOOK) || item.is(Items.ENCHANTED_BOOK);
        ItemStack out = item.is(Items.BOOK) ? new ItemStack(Items.ENCHANTED_BOOK) : item.copy();
        var component = book ? DataComponents.STORED_ENCHANTMENTS : DataComponents.ENCHANTMENTS;
        ItemEnchantments current = out.getOrDefault(component, ItemEnchantments.EMPTY);
        for (Holder<Enchantment> other : current.keySet()) {
            if (!other.equals(ench) && !Enchantment.areCompatible(other, ench)) {
                return null;
            }
        }
        if (current.getLevel(ench) >= level) {
            return null;
        }
        ItemEnchantments.Mutable m = new ItemEnchantments.Mutable(current);
        m.set(ench, level);
        out.set(component, m.toImmutable());
        return out;
    }

}
