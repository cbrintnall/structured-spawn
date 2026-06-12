package com.odder.structuredspawn;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.neoforge.common.ModConfigSpec;

// An example config class. This is not required, but it's a good idea to have one to keep your config organized.
// Demonstrates how to use Neo's config APIs
public class Config {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    public static final ModConfigSpec.ConfigValue<Boolean> ON_JOIN = BUILDER.comment("Should randomize run on first join").define("on_join", true);
    public static final ModConfigSpec.ConfigValue<Boolean> ON_SPAWN = BUILDER.comment("Should randomize run on each spawn").define("on_spawn", false);
    public static final ModConfigSpec.ConfigValue<Boolean> RESPECT_BED = BUILDER.comment("If on spawn is enabled, should it prioritize bed spawn over randomized").define("respectBed", true);
    public static final ModConfigSpec.ConfigValue<Boolean> SEARCH_FROM_SPAWN = BUILDER.comment("Should the search originate from the player's respawn point? Otherwise it picks a random block.").define("searchFromSpawn", false);
    public static final ModConfigSpec.ConfigValue<Integer> SEARCH_RADIUS = BUILDER.comment("How far we should search for a structure before giving up").define("searchRadius", 100);

    static final ModConfigSpec SPEC = BUILDER.build();
}
