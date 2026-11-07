package com.futuretek.essence;

import java.util.Map;

import com.google.gson.Gson;
import com.google.gson.JsonElement;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;

/** Legge i file data/<mod>/essence_values/*.json all'avvio e con /reload. */
public class EssenceValueLoader extends SimpleJsonResourceReloadListener {
    public EssenceValueLoader() {
        super(new Gson(), "essence_values");
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> files, ResourceManager manager, ProfilerFiller profiler) {
        EssenceValues.load(files);
    }
}
