package com.futuretek.compat.jei;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import com.futuretek.FutureTek;
import com.futuretek.machine.ProcessingMachine;
import com.futuretek.recipe.ProcessingKind;
import com.futuretek.recipe.ProcessingRecipe;
import com.futuretek.registry.ModBlocks;

import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.constants.RecipeTypes;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.widgets.IRecipeExtrasBuilder;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.block.Block;

/**
 * Integrazione con JEI (Just Enough Items): mostra le ricette delle macchine di FutureTek.
 * Questa classe viene caricata SOLO se JEI e' installato (la trova JEI grazie a @JeiPlugin).
 */
@JeiPlugin
public class FutureTekJeiPlugin implements IModPlugin {
    private static final Map<ProcessingKind, RecipeType<RecipeHolder<ProcessingRecipe>>> TYPES = new EnumMap<>(ProcessingKind.class);

    static {
        for (ProcessingKind kind : ProcessingKind.values()) {
            TYPES.put(kind, RecipeType.createRecipeHolderType(ResourceLocation.fromNamespaceAndPath(FutureTek.MODID, kind.id)));
        }
    }

    @Override
    public ResourceLocation getPluginUid() {
        return ResourceLocation.fromNamespaceAndPath(FutureTek.MODID, "jei_plugin");
    }

    /** La macchina che usa un certo tipo di ricetta */
    private static Block machineFor(ProcessingKind kind) {
        for (ProcessingMachine machine : ProcessingMachine.values()) {
            if (machine.kind == kind) {
                return ModBlocks.PROCESSING.get(machine).get();
            }
        }
        return ModBlocks.PROCESSING.get(ProcessingMachine.CRUSHER).get();
    }

    private static ProcessingMachine machineEnum(ProcessingKind kind) {
        for (ProcessingMachine machine : ProcessingMachine.values()) {
            if (machine.kind == kind) {
                return machine;
            }
        }
        return ProcessingMachine.CRUSHER;
    }

    @Override
    public void registerCategories(IRecipeCategoryRegistration registration) {
        IGuiHelper gui = registration.getJeiHelpers().getGuiHelper();
        for (ProcessingKind kind : ProcessingKind.values()) {
            registration.addRecipeCategories(new MachineCategory(kind, gui));
        }
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        if (Minecraft.getInstance().level == null) {
            return;
        }
        var manager = Minecraft.getInstance().level.getRecipeManager();
        for (ProcessingKind kind : ProcessingKind.values()) {
            List<RecipeHolder<ProcessingRecipe>> recipes = new ArrayList<>(manager.getAllRecipesFor(kind.type()));
            registration.addRecipes(TYPES.get(kind), recipes);
        }
    }

    @Override
    public void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        // ogni macchina compare sulla scheda delle sue ricette (il forno elettrico e quello a induzione sulla fornace)
        for (ProcessingMachine machine : ProcessingMachine.values()) {
            var block = ModBlocks.PROCESSING.get(machine).get();
            if (machine.kind == null) {
                registration.addRecipeCatalysts(RecipeTypes.SMELTING, block);
            } else {
                registration.addRecipeCatalysts(TYPES.get(machine.kind), block);
            }
        }
        registration.addRecipeCatalysts(RecipeTypes.SMELTING, ModBlocks.IRON_FURNACE.get());
    }

    /** Una "scheda" di JEI per ogni tipo di macchina: ingresso -> freccia -> prodotto (+ prodotto extra) */
    private static class MachineCategory implements IRecipeCategory<RecipeHolder<ProcessingRecipe>> {
        private final ProcessingKind kind;
        private final IDrawable icon;

        MachineCategory(ProcessingKind kind, IGuiHelper gui) {
            this.kind = kind;
            this.icon = gui.createDrawableItemLike(machineFor(kind));
        }

        @Override
        public RecipeType<RecipeHolder<ProcessingRecipe>> getRecipeType() {
            return TYPES.get(kind);
        }

        @Override
        public Component getTitle() {
            return Component.translatable(machineFor(kind).getDescriptionId());
        }

        @Override
        public int getWidth() {
            return 130;
        }

        @Override
        public int getHeight() {
            return 40;
        }

        @Override
        public IDrawable getIcon() {
            return icon;
        }

        @Override
        public void setRecipe(IRecipeLayoutBuilder builder, RecipeHolder<ProcessingRecipe> holder, IFocusGroup focuses) {
            ProcessingRecipe recipe = holder.value();
            builder.addInputSlot(4, 4).setStandardSlotBackground()
                    .addItemStacks(List.of(recipe.getIngredient().getItems()));
            builder.addOutputSlot(64, 4).setOutputSlotBackground().addItemStack(recipe.getResult());
            recipe.getSecondary().ifPresent(extra -> builder.addOutputSlot(90, 4).setOutputSlotBackground()
                    .addItemStack(extra.item())
                    .addRichTooltipCallback((view, tooltip) -> tooltip.add(Component.translatable("jei.futuretek.chance",
                            Math.round(extra.chance() * 100)).withStyle(ChatFormatting.GOLD))));
        }

        @Override
        public void createRecipeExtras(IRecipeExtrasBuilder builder, RecipeHolder<ProcessingRecipe> holder, IFocusGroup focuses) {
            ProcessingMachine machine = machineEnum(kind);
            int time = holder.value().getTime() > 0 ? holder.value().getTime() : machine.time;
            builder.addAnimatedRecipeArrow(time).setPosition(30, 4);
            String info = String.format(java.util.Locale.ITALY, "%.1f s · %d TU/t", time / 20.0, machine.energyPerTick);
            Component text = machine.usesWater()
                    ? Component.translatable("jei.futuretek.info_water", info, machine.waterPerOperation)
                    : Component.literal(info);
            builder.addText(text, 4, 28, 126, 10).setColor(0xFF606060);
        }
    }
}
