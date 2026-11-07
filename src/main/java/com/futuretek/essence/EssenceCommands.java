package com.futuretek.essence;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import com.futuretek.FutureTek;
import com.futuretek.energy.EnergyUtil;
import com.mojang.brigadier.arguments.LongArgumentType;
import com.mojang.brigadier.context.CommandContext;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

/**
 * Comandi dell'Essenza (scrivili nella chat):
 *   /futuretek essenza valore            -> quanto vale l'oggetto che hai in mano
 *   /futuretek essenza mancanti          -> scrive in config/futuretek_essenza_mancanti.txt gli oggetti senza valore
 *                                          e in config/futuretek_essenza_valori.txt tutti i valori
 *   /futuretek essenza aggiungi <numero> -> (operatori) ti da' Essenza, utile per provare
 *   /futuretek essenza impara_tutto      -> (operatori) impari tutti gli oggetti con un valore
 */
public final class EssenceCommands {
    private EssenceCommands() {
    }

    public static void register(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("futuretek")
                .then(Commands.literal("essenza")
                        .then(Commands.literal("valore").executes(EssenceCommands::value))
                        .then(Commands.literal("mancanti").requires(s -> s.hasPermission(2))
                                .executes(EssenceCommands::missing))
                        .then(Commands.literal("aggiungi").requires(s -> s.hasPermission(2))
                                .then(Commands.argument("quantita", LongArgumentType.longArg(1))
                                        .executes(ctx -> add(ctx, LongArgumentType.getLong(ctx, "quantita")))))
                        .then(Commands.literal("impara_tutto").requires(s -> s.hasPermission(2))
                                .executes(EssenceCommands::learnAll))));
    }

    private static int value(CommandContext<CommandSourceStack> ctx) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        ItemStack stack = player.getMainHandItem();
        long each = EssenceValues.get(false, stack.getItem());
        long total = EssenceValues.stackValue(false, stack);
        ctx.getSource().sendSuccess(() -> Component.translatable("message.futuretek.essence_value_cmd",
                stack.getHoverName(), EnergyUtil.format(each), EnergyUtil.format(total)), false);
        return (int) Math.min(Integer.MAX_VALUE, total);
    }

    private static int missing(CommandContext<CommandSourceStack> ctx) {
        List<ResourceLocation> list = EssenceValues.missing();
        Path file = FMLPaths.CONFIGDIR.get().resolve("futuretek_essenza_mancanti.txt");
        try {
            StringBuilder text = new StringBuilder();
            for (ResourceLocation id : list) {
                text.append(id).append('\n');
            }
            Files.writeString(file, text.toString(), StandardCharsets.UTF_8);
            // e la tabella completa dei valori (per controllare o per modificare i JSON)
            StringBuilder all = new StringBuilder();
            EssenceValues.serverValues().entrySet().stream()
                    .map(e -> net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(e.getKey()) + " = " + e.getValue())
                    .sorted().forEach(line -> all.append(line).append('\n'));
            Files.writeString(FMLPaths.CONFIGDIR.get().resolve("futuretek_essenza_valori.txt"), all.toString(),
                    StandardCharsets.UTF_8);
        } catch (Exception e) {
            FutureTek.LOGGER.error("FutureTek: impossibile scrivere {}", file, e);
        }
        int total = EssenceValues.serverValues().size();
        ctx.getSource().sendSuccess(() -> Component.translatable("message.futuretek.essence_missing_cmd",
                list.size(), total, file.toString()), false);
        FutureTek.LOGGER.info("FutureTek: oggetti senza valore dell'Essenza: {} (con valore: {})", list.size(), total);
        return list.size();
    }

    private static int add(CommandContext<CommandSourceStack> ctx, long amount) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        EssenceManager.addEssence(player, amount);
        ctx.getSource().sendSuccess(() -> Component.translatable("message.futuretek.crystal_emptied",
                EnergyUtil.format(amount)), false);
        return 1;
    }

    private static int learnAll(CommandContext<CommandSourceStack> ctx) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        int added = EssenceManager.learnAll(player);
        ctx.getSource().sendSuccess(() -> Component.translatable("message.futuretek.learned_all", added), false);
        return added;
    }
}
