package com.futuretek.recipe;

import java.util.Optional;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.common.crafting.SizedIngredient;

/**
 * Ricetta di una macchina di lavorazione (frantumatore, compressore, estrattore, pressa per fili).
 * Esempio di file JSON (in data/futuretek/recipe/):
 * <pre>
 * {
 *   "type": "futuretek:crushing",
 *   "ingredient": { "tag": "c:raw_materials/tin", "count": 1 },
 *   "result": { "id": "futuretek:tin_dust", "count": 2 },
 *   "secondary": { "item": { "id": "futuretek:tin_dust" }, "chance": 0.1 },   (facoltativo)
 *   "time": 300                                                              (facoltativo, in tick)
 * }
 * </pre>
 */
public class ProcessingRecipe implements Recipe<SingleRecipeInput> {
    /** Un prodotto extra che esce solo con una certa probabilita' (0.0 - 1.0) */
    public record ChanceOutput(ItemStack item, float chance) {
        public static final Codec<ChanceOutput> CODEC = RecordCodecBuilder.create(i -> i.group(
                ItemStack.CODEC.fieldOf("item").forGetter(ChanceOutput::item),
                Codec.floatRange(0.0F, 1.0F).fieldOf("chance").forGetter(ChanceOutput::chance)
        ).apply(i, ChanceOutput::new));

        public static final StreamCodec<RegistryFriendlyByteBuf, ChanceOutput> STREAM_CODEC = StreamCodec.composite(
                ItemStack.STREAM_CODEC, ChanceOutput::item,
                ByteBufCodecs.FLOAT, ChanceOutput::chance,
                ChanceOutput::new);
    }

    private final ProcessingKind kind;
    private final SizedIngredient ingredient;
    private final ItemStack result;
    private final Optional<ChanceOutput> secondary;
    /** Durata in tick (0 = durata predefinita della macchina) */
    private final int time;

    public ProcessingRecipe(ProcessingKind kind, SizedIngredient ingredient, ItemStack result,
            Optional<ChanceOutput> secondary, int time) {
        this.kind = kind;
        this.ingredient = ingredient;
        this.result = result;
        this.secondary = secondary;
        this.time = time;
    }

    public ProcessingKind getKind() {
        return kind;
    }

    public SizedIngredient getIngredient() {
        return ingredient;
    }

    public ItemStack getResult() {
        return result;
    }

    public Optional<ChanceOutput> getSecondary() {
        return secondary;
    }

    public int getTime() {
        return time;
    }

    /** La ricetta va bene se l'oggetto e' giusto E ce n'e' abbastanza */
    @Override
    public boolean matches(SingleRecipeInput input, Level level) {
        return ingredient.test(input.item());
    }

    @Override
    public ItemStack assemble(SingleRecipeInput input, HolderLookup.Provider registries) {
        return result.copy();
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return true;
    }

    @Override
    public ItemStack getResultItem(HolderLookup.Provider registries) {
        return result;
    }

    @Override
    public NonNullList<Ingredient> getIngredients() {
        return NonNullList.of(Ingredient.EMPTY, ingredient.ingredient());
    }

    @Override
    public boolean isSpecial() {
        return true; // non compare nel libro delle ricette di Minecraft
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return kind.serializer();
    }

    @Override
    public RecipeType<?> getType() {
        return kind.type();
    }

    /** Legge/scrive le ricette dai file JSON e le invia ai giocatori connessi */
    public static class Serializer implements RecipeSerializer<ProcessingRecipe> {
        private final MapCodec<ProcessingRecipe> codec;
        private final StreamCodec<RegistryFriendlyByteBuf, ProcessingRecipe> streamCodec;

        public Serializer(ProcessingKind kind) {
            this.codec = RecordCodecBuilder.mapCodec(i -> i.group(
                    SizedIngredient.FLAT_CODEC.fieldOf("ingredient").forGetter(ProcessingRecipe::getIngredient),
                    ItemStack.CODEC.fieldOf("result").forGetter(ProcessingRecipe::getResult),
                    ChanceOutput.CODEC.optionalFieldOf("secondary").forGetter(ProcessingRecipe::getSecondary),
                    Codec.INT.optionalFieldOf("time", 0).forGetter(ProcessingRecipe::getTime)
            ).apply(i, (ing, res, sec, time) -> new ProcessingRecipe(kind, ing, res, sec, time)));
            this.streamCodec = StreamCodec.composite(
                    SizedIngredient.STREAM_CODEC, ProcessingRecipe::getIngredient,
                    ItemStack.STREAM_CODEC, ProcessingRecipe::getResult,
                    ByteBufCodecs.optional(ChanceOutput.STREAM_CODEC), ProcessingRecipe::getSecondary,
                    ByteBufCodecs.VAR_INT, ProcessingRecipe::getTime,
                    (ing, res, sec, time) -> new ProcessingRecipe(kind, ing, res, sec, time));
        }

        @Override
        public MapCodec<ProcessingRecipe> codec() {
            return codec;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, ProcessingRecipe> streamCodec() {
            return streamCodec;
        }
    }
}
