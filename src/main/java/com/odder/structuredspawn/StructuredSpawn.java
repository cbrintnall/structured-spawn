package com.odder.structuredspawn;

import com.mojang.datafixers.util.Pair;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.Registry;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.BuiltinStructures;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import org.slf4j.Logger;

import com.mojang.logging.LogUtils;

import net.minecraft.core.registries.Registries;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.ModContainer;
import net.neoforged.neoforge.common.NeoForge;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Mod(StructuredSpawn.MODID)
public class StructuredSpawn {
    public static final String MODID = "structuredspawn";
    public static final Logger LOGGER = LogUtils.getLogger();

    public static final TagKey<Structure> ON_JOIN = TagKey.create(Registries.STRUCTURE, ResourceLocation.fromNamespaceAndPath(MODID, "on_join"));

    public static final TagKey<Structure> ON_SPAWN = TagKey.create(Registries.STRUCTURE, ResourceLocation.fromNamespaceAndPath(MODID, "on_spawn"));

    public StructuredSpawn(IEventBus modEventBus, ModContainer modContainer) {
        modContainer.registerConfig(ModConfig.Type.COMMON, Config.SPEC);

        NeoForge.EVENT_BUS.addListener(StructuredSpawn::onPlayerJoined);
        NeoForge.EVENT_BUS.addListener(StructuredSpawn::onPlayerSpawned);

        if (FMLEnvironment.dist == Dist.CLIENT) {
            modContainer.registerExtensionPoint(
                IConfigScreenFactory.class,
                ConfigurationScreen::new
            );
        }
    }

    private static void onPlayerSpawned(PlayerEvent.PlayerRespawnEvent event) {
        if (!Config.ON_SPAWN.get()) return;
        if (!(event.getEntity() instanceof ServerPlayer player)) return;

        if (Config.RESPECT_BED.get() && player.getRespawnPosition() != null) {
            LOGGER.info("Player {} has a bed and respect bed is on, skipping randomize spawn.", player.getName());
            return;
        }

        StructuredSpawn.LOGGER.info("Running structured spawn for ON_SPAWN, for player {}", player.getName());

        teleportPlayerToStructure(player, ON_SPAWN);
    }

    private static void onPlayerJoined(PlayerEvent.PlayerLoggedInEvent event) {
        if (!Config.ON_JOIN.get()) return;
        if (!(event.getEntity() instanceof ServerPlayer player)) return;

        CompoundTag persistentData = player.getPersistentData();
        if (persistentData.getBoolean("hasRandomSpawned")) return;
        persistentData.putBoolean("hasRandomSpawned", true);

        StructuredSpawn.LOGGER.info("Running structured spawn for ON_JOIN, for player {}", player.getName());

        teleportPlayerToStructure(player, ON_JOIN);
    }

    private static void teleportPlayerToStructure(ServerPlayer player, TagKey<Structure> key) {
        ServerLevel level = player.serverLevel();

        List<Holder<Structure>> pool = StructuredSpawn.resolveStructurePool(level, key);
        Holder<Structure> chosen = pool.get(level.random.nextInt(pool.size()));

        BlockPos searchCenter = new BlockPos((level.random.nextInt(20000)) - 10000, 64, (level.random.nextInt(20000)) - 10000);

        Pair<BlockPos, Holder<Structure>> found = level.getChunkSource().getGenerator().findNearestMapStructure(level, HolderSet.direct(chosen), searchCenter, Config.SEARCH_RADIUS.get(), false);

        if (found != null) {
            StructuredSpawn.LOGGER.info("Found structure -- teleporting player!");
            BlockPos pos = found.getFirst();
            level.getChunk(pos.getX() >> 4, pos.getZ() >> 4);
            int safeY = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, pos.getX(), pos.getZ());
            player.teleportTo(level, pos.getX() + 0.5, safeY, pos.getZ() + 0.5, player.getYRot(), player.getXRot());
        } else {
            StructuredSpawn.LOGGER.info("Failed to find structure, can't teleport!!");
        }
    }

    private static List<Holder<Structure>> resolveStructurePool(ServerLevel level, TagKey<Structure> key) {
        Registry<Structure> registry = level.registryAccess().registryOrThrow(Registries.STRUCTURE);

        LOGGER.info("Known structure tags: {}", registry.getTags().map(pair -> pair.getFirst().location().toString()).collect(Collectors.joining(", ")));

        List<Holder<Structure>> resolved = new ArrayList<>();
        registry.getTagOrEmpty(key).forEach(resolved::add);

        if (resolved.isEmpty()) {
            LOGGER.warn("Tag {} is empty — no structures to spawn at. " + "Add structures to this tag via a datapack.", key.location());
        }

        return resolved;
    }
}
