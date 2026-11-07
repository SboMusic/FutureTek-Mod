package com.futuretek.registry;

import com.futuretek.FutureTek;
import com.futuretek.essence.EssenceData;

import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

/** Dati "attaccati" ai giocatori (salvati col mondo e inviati al client). */
public final class ModAttachments {
    public static final DeferredRegister<AttachmentType<?>> ATTACHMENTS =
            DeferredRegister.create(NeoForgeRegistries.Keys.ATTACHMENT_TYPES, FutureTek.MODID);

    /** Essenza e oggetti conosciuti: si conservano anche dopo la morte */
    public static final DeferredHolder<AttachmentType<?>, AttachmentType<EssenceData>> ESSENCE =
            ATTACHMENTS.register("essence", () -> AttachmentType.builder(() -> EssenceData.EMPTY)
                    .serialize(EssenceData.CODEC)
                    .sync(EssenceData.STREAM_CODEC)
                    .copyOnDeath()
                    .build());

    /** Contenuto della Bisaccia Dimensionale (54 slot), resta dopo la morte */
    public static final DeferredHolder<AttachmentType<?>, AttachmentType<com.futuretek.alchemy.SatchelInventory>> SATCHEL =
            ATTACHMENTS.register("satchel", () -> AttachmentType.serializable(com.futuretek.alchemy.SatchelInventory::new)
                    .copyOnDeath()
                    .build());

    private ModAttachments() {
    }
}
