package com.bgsoftware.superiorskyblock.world;

import com.bgsoftware.superiorskyblock.SuperiorSkyblockPlugin;
import com.bgsoftware.superiorskyblock.api.data.DatabaseBridge;
import com.bgsoftware.superiorskyblock.api.data.DatabaseBridgeMode;
import com.bgsoftware.superiorskyblock.api.handlers.GridManager;
import com.bgsoftware.superiorskyblock.api.island.Island;
import com.bgsoftware.superiorskyblock.api.island.IslandPreview;
import com.bgsoftware.superiorskyblock.api.island.SortingType;
import com.bgsoftware.superiorskyblock.api.island.container.IslandsContainer;
import com.bgsoftware.superiorskyblock.api.schematic.Schematic;
import com.bgsoftware.superiorskyblock.api.world.algorithm.IslandCreationAlgorithm;
import com.bgsoftware.superiorskyblock.api.wrappers.SuperiorPlayer;
import com.bgsoftware.superiorskyblock.database.DatabaseResult;
import com.bgsoftware.superiorskyblock.database.bridge.GridDatabaseBridge;
import com.bgsoftware.superiorskyblock.database.bridge.IslandsDatabaseBridge;
import com.bgsoftware.superiorskyblock.database.cache.CachedIslandInfo;
import com.bgsoftware.superiorskyblock.database.cache.DatabaseCache;
import com.bgsoftware.superiorskyblock.handler.AbstractHandler;
import com.bgsoftware.superiorskyblock.island.SIslandPreview;
import com.bgsoftware.superiorskyblock.island.spawn.SpawnIsland;
import com.bgsoftware.superiorskyblock.lang.Message;
import com.bgsoftware.superiorskyblock.menu.SuperiorMenu;
import com.bgsoftware.superiorskyblock.player.chat.PlayerChat;
import com.bgsoftware.superiorskyblock.schematic.BaseSchematic;
import com.bgsoftware.superiorskyblock.threads.Executor;
import com.bgsoftware.superiorskyblock.utils.StringUtils;
import com.bgsoftware.superiorskyblock.utils.debug.PluginDebugger;
import com.bgsoftware.superiorskyblock.utils.events.EventsCaller;
import com.bgsoftware.superiorskyblock.utils.islands.IslandUtils;
import com.bgsoftware.superiorskyblock.world.algorithm.DefaultIslandCreationAlgorithm;
import com.bgsoftware.superiorskyblock.world.chunks.ChunkPosition;
import com.bgsoftware.superiorskyblock.world.chunks.ChunksTracker;
import com.bgsoftware.superiorskyblock.world.preview.IslandPreviews;
import com.bgsoftware.superiorskyblock.world.purge.IslandsPurger;
import com.bgsoftware.superiorskyblock.wrappers.SBlockPosition;
import com.google.common.base.Preconditions;
import com.google.common.collect.Sets;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Biome;
import org.bukkit.block.Block;

import javax.annotation.Nullable;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@SuppressWarnings({"WeakerAccess", "unused"})
public final class GridHandler extends AbstractHandler implements GridManager {

    private final Set<UUID> pendingCreationTasks = Sets.newHashSet();
    private final Set<UUID> customWorlds = Sets.newHashSet();

    private final IslandsPurger islandsPurger;
    private final IslandPreviews islandPreviews;
    private final IslandsContainer islandsContainer;
    private DatabaseBridge databaseBridge;
    private IslandCreationAlgorithm islandCreationAlgorithm;

    private Island spawnIsland;
    private SBlockPosition lastIsland;

    private BigDecimal totalWorth = BigDecimal.ZERO;
    private long lastTimeWorthUpdate = 0;
    private BigDecimal totalLevel = BigDecimal.ZERO;
    private long lastTimeLevelUpdate = 0;

    private boolean pluginDisable = false;

    public GridHandler(SuperiorSkyblockPlugin plugin, IslandsPurger islandsPurger, IslandPreviews islandPreviews,
                       IslandsContainer islandsContainer) {
        super(plugin);
        this.islandsPurger = islandsPurger;
        this.islandPreviews = islandPreviews;
        this.islandsContainer = islandsContainer;
    }

    @Override
    public void loadData() {
        initializeDatabaseBridge();
        this.islandCreationAlgorithm = DefaultIslandCreationAlgorithm.getInstance();

        this.lastIsland = SBlockPosition.of(plugin.getSettings().getWorlds().getDefaultWorldName(), 0, 100, 0);
        Executor.sync(this::updateSpawn);
        Executor.timer(plugin.getNMSDragonFight()::tickBattles, 1L);
    }

    public void updateSpawn() {
        spawnIsland = new SpawnIsland(plugin);
    }

    public void syncUpgrades() {
        getIslands().forEach(Island::updateUpgrades);
    }

    public void createIsland(DatabaseCache<CachedIslandInfo> cache, DatabaseResult resultSet) {
        Optional<Island> island = plugin.getFactory().createIsland(cache, resultSet);
        island.ifPresent(this.islandsContainer::addIsland);
    }

    @Override
    public void createIsland(SuperiorPlayer superiorPlayer, String schemName, BigDecimal bonus, Biome biome, String islandName) {
        Preconditions.checkNotNull(superiorPlayer, "superiorPlayer parameter cannot be null.");
        Preconditions.checkNotNull(schemName, "schemName parameter cannot be null.");
        Preconditions.checkNotNull(bonus, "bonus parameter cannot be null.");
        Preconditions.checkNotNull(biome, "biome parameter cannot be null.");
        Preconditions.checkNotNull(islandName, "islandName parameter cannot be null.");
        createIsland(superiorPlayer, schemName, bonus, biome, islandName, false);
    }

    @Override
    public void createIsland(SuperiorPlayer superiorPlayer, String schemName, BigDecimal bonus, Biome biome, String islandName, boolean offset) {
        Preconditions.checkNotNull(superiorPlayer, "superiorPlayer parameter cannot be null.");
        Preconditions.checkNotNull(schemName, "schemName parameter cannot be null.");
        Preconditions.checkNotNull(bonus, "bonus parameter cannot be null.");
        Preconditions.checkNotNull(biome, "biome parameter cannot be null.");
        Preconditions.checkNotNull(islandName, "islandName parameter cannot be null.");
        createIsland(superiorPlayer, schemName, bonus, BigDecimal.ZERO, biome, islandName, false);
    }

    @Override
    public void createIsland(SuperiorPlayer superiorPlayer, String schemName, BigDecimal bonusWorth,
                             BigDecimal bonusLevel, Biome biome, String islandName, boolean offset) {
        Preconditions.checkNotNull(superiorPlayer, "superiorPlayer parameter cannot be null.");
        Preconditions.checkNotNull(schemName, "schemName parameter cannot be null.");
        Preconditions.checkNotNull(bonusWorth, "bonusWorth parameter cannot be null.");
        Preconditions.checkNotNull(bonusLevel, "bonusLevel parameter cannot be null.");
        Preconditions.checkNotNull(biome, "biome parameter cannot be null.");
        Preconditions.checkNotNull(islandName, "islandName parameter cannot be null.");

        if (!Bukkit.isPrimaryThread()) {
            Executor.sync(() -> createIsland(superiorPlayer, schemName, bonusWorth, bonusLevel, biome, islandName, offset));
            return;
        }

        PluginDebugger.debug("Action: Create Island, Target: " + superiorPlayer.getName() + ", Schematic: " +
                schemName + ", Bonus Worth: " + bonusWorth + ", Bonus Level: " + bonusLevel + ", Biome: " + biome +
                ", Name: " + islandName + ", Offset: " + offset);

        Schematic schematic = plugin.getSchematics().getSchematic(schemName);

        if (schematic == null)
            throw new IllegalArgumentException("Cannot create an island with an invalid schematic.");

        // Removing any active previews for the player.
        boolean updateGamemode = this.islandPreviews.endIslandPreview(superiorPlayer) != null;

        if (!EventsCaller.callPreIslandCreateEvent(superiorPlayer, islandName))
            return;

        UUID islandUUID = generateIslandUUID();

        long startTime = System.currentTimeMillis();

        pendingCreationTasks.add(superiorPlayer.getUniqueId());

        this.islandCreationAlgorithm.createIsland(islandUUID, superiorPlayer, this.lastIsland, islandName, schematic)
                .whenComplete((islandCreationResult, error) -> {
                    if (error == null) {
                        Island island = islandCreationResult.getIsland();
                        Location islandLocation = islandCreationResult.getIslandLocation();
                        boolean teleportPlayer = islandCreationResult.shouldTeleportPlayer();

                        Set<ChunkPosition> loadedChunks = ((BaseSchematic) schematic).getLoadedChunks();

                        this.islandsContainer.addIsland(island);
                        setLastIsland(SBlockPosition.of(islandLocation));

                        pendingCreationTasks.remove(superiorPlayer.getUniqueId());

                        island.setBonusWorth(offset ? island.getRawWorth().negate() : bonusWorth);
                        island.setBonusLevel(offset ? island.getRawLevel().negate() : bonusLevel);
                        island.setBiome(biome);
                        island.setIslandHome(schematic.adjustRotation(islandLocation));

                        IslandsDatabaseBridge.insertIsland(island);

                        superiorPlayer.runIfOnline(player -> {
                            Message.CREATE_ISLAND.send(superiorPlayer, SBlockPosition.of(islandLocation),
                                    System.currentTimeMillis() - startTime);
                            if (teleportPlayer) {
                                if (updateGamemode)
                                    player.setGameMode(GameMode.SURVIVAL);
                                superiorPlayer.teleport(island, result -> {
                                    if (result) {
                                        Executor.sync(() -> IslandUtils.resetChunksExcludedFromList(island, loadedChunks), 10L);
                                        if (plugin.getSettings().getWorlds().getDefaultWorld() == World.Environment.THE_END) {
                                            plugin.getNMSDragonFight().awardTheEndAchievement(player);
                                            if (plugin.getSettings().getWorlds().getEnd().isDragonFight())
                                                plugin.getNMSDragonFight().startDragonBattle(island, island.getCenter(World.Environment.THE_END));
                                        }
                                    }
                                });
                            }
                        });
                    } else {
                        error.printStackTrace();
                        PluginDebugger.debug(error);

                        pendingCreationTasks.remove(superiorPlayer.getUniqueId());

                        superiorPlayer.setIsland(null);

                        Message.CREATE_ISLAND_FAILURE.send(superiorPlayer);
                    }
                });
    }

    @Override
    public void setIslandCreationAlgorithm(@Nullable IslandCreationAlgorithm islandCreationAlgorithm) {
        this.islandCreationAlgorithm = islandCreationAlgorithm != null ? islandCreationAlgorithm :
                DefaultIslandCreationAlgorithm.getInstance();
    }

    @Override
    public boolean hasActiveCreateRequest(SuperiorPlayer superiorPlayer) {
        Preconditions.checkNotNull(superiorPlayer, "superiorPlayer parameter cannot be null.");
        return pendingCreationTasks.contains(superiorPlayer.getUniqueId());
    }

    @Override
    public void startIslandPreview(SuperiorPlayer superiorPlayer, String schemName, String islandName) {
        Preconditions.checkNotNull(superiorPlayer, "superiorPlayer parameter cannot be null.");
        Preconditions.checkNotNull(schemName, "schemName parameter cannot be null.");
        Preconditions.checkNotNull(islandName, "islandName parameter cannot be null.");

        Location previewLocation = plugin.getSettings().getPreviewIslands().get(schemName.toLowerCase());
        if (previewLocation != null && previewLocation.getWorld() != null) {
            superiorPlayer.teleport(previewLocation, result -> {
                if (result) {
                    this.islandPreviews.startIslandPreview(new SIslandPreview(superiorPlayer, previewLocation, schemName, islandName));
                    Executor.ensureMain(() -> superiorPlayer.runIfOnline(player -> player.setGameMode(GameMode.SPECTATOR)));
                    Message.ISLAND_PREVIEW_START.send(superiorPlayer, schemName);
                }
            });
        }
    }

    @Override
    public void cancelIslandPreview(SuperiorPlayer superiorPlayer) {
        Preconditions.checkNotNull(superiorPlayer, "superiorPlayer parameter cannot be null.");

        IslandPreview islandPreview = this.islandPreviews.endIslandPreview(superiorPlayer);
        if (islandPreview != null) {
            superiorPlayer.runIfOnline(player -> {
                Executor.ensureMain(() -> superiorPlayer.teleport(plugin.getGrid().getSpawnIsland(), teleportResult -> {
                    if (teleportResult && superiorPlayer.isOnline())
                        player.setGameMode(GameMode.SURVIVAL);
                }));
                PlayerChat.remove(player);
            });
        }
    }

    @Override
    public void cancelAllIslandPreviews() {
        if (!Bukkit.isPrimaryThread()) {
            Executor.sync(this::cancelAllIslandPreviews);
            return;
        }

        this.islandPreviews.getActivePreviews().forEach(islandPreview -> {
            SuperiorPlayer superiorPlayer = islandPreview.getPlayer();
            superiorPlayer.runIfOnline(player -> {
                superiorPlayer.teleport(plugin.getGrid().getSpawnIsland());
                // We don't wait for the teleport to happen, as this method is called when the server is disabled.
                // Therefore, we can't wait for the async task to occur.
                player.setGameMode(GameMode.SURVIVAL);
                PlayerChat.remove(player);
            });
        });
    }

    @Override
    public IslandPreview getIslandPreview(SuperiorPlayer superiorPlayer) {
        Preconditions.checkNotNull(superiorPlayer, "superiorPlayer parameter cannot be null.");
        return this.islandPreviews.getIslandPreview(superiorPlayer);
    }

    @Override
    public void deleteIsland(Island island) {
        Preconditions.checkNotNull(island, "island parameter cannot be null.");
        PluginDebugger.debug("Action: Disband Island, Island: " + island.getOwner().getName());

        island.getAllPlayersInside().forEach(superiorPlayer -> {
            SuperiorMenu.killMenu(superiorPlayer);
            superiorPlayer.teleport(plugin.getGrid().getSpawnIsland());
            Message.ISLAND_GOT_DELETED_WHILE_INSIDE.send(superiorPlayer);
        });

        this.islandsContainer.removeIsland(island);

        // Delete island from database
        if (pluginDisable) {
            IslandsDatabaseBridge.deleteIsland(island);
        } else {
            Executor.data(() -> IslandsDatabaseBridge.deleteIsland(island));
        }

        plugin.getNMSDragonFight().removeDragonBattle(island);

        ChunksTracker.removeIsland(island);
    }

    @Override
    public Island getIsland(SuperiorPlayer superiorPlayer) {
        Preconditions.checkNotNull(superiorPlayer, "superiorPlayer parameter cannot be null.");
        return getIsland(superiorPlayer.getIslandLeader().getUniqueId());
    }

    @Override
    public Island getIsland(int index, SortingType sortingType) {
        Preconditions.checkNotNull(sortingType, "sortingType parameter cannot be null.");
        return this.islandsContainer.getIslandAtPosition(index, sortingType);
    }

    @Override
    public int getIslandPosition(Island island, SortingType sortingType) {
        Preconditions.checkNotNull(island, "island parameter cannot be null.");
        Preconditions.checkNotNull(sortingType, "sortingType parameter cannot be null.");
        return this.islandsContainer.getIslandPosition(island, sortingType);
    }

    @Override
    public Island getIsland(UUID uuid) {
        Preconditions.checkNotNull(uuid, "uuid parameter cannot be null.");
        return this.islandsContainer.getIslandByLeader(uuid);
    }

    @Override
    public Island getIslandByUUID(UUID uuid) {
        Preconditions.checkNotNull(uuid, "uuid parameter cannot be null.");
        return this.islandsContainer.getIslandByUUID(uuid);
    }

    @Override
    public Island getIsland(String islandName) {
        Preconditions.checkNotNull(islandName, "islandName parameter cannot be null.");
        String inputName = StringUtils.stripColors(islandName);
        return getIslands().stream().filter(island -> island.getRawName().equalsIgnoreCase(inputName)).findFirst().orElse(null);
    }

    @Override
    public Island getIslandAt(Location location) {
        if (location == null)
            return null;

        if (spawnIsland != null && spawnIsland.isInside(location))
            return spawnIsland;

        return this.islandsContainer.getIslandAt(location);
    }

    @Override
    public Island getIslandAt(Chunk chunk) {
        if (chunk == null)
            return null;

        Island island;

        Location corner = chunk.getBlock(0, 100, 0).getLocation();
        if ((island = getIslandAt(corner)) != null)
            return island;

        corner = chunk.getBlock(15, 100, 0).getLocation();
        if ((island = getIslandAt(corner)) != null)
            return island;

        corner = chunk.getBlock(0, 100, 15).getLocation();
        if ((island = getIslandAt(corner)) != null)
            return island;

        corner = chunk.getBlock(15, 100, 15).getLocation();
        if ((island = getIslandAt(corner)) != null)
            return island;

        return null;
    }

    @Override
    public void transferIsland(UUID oldOwner, UUID newOwner) {
        Preconditions.checkNotNull(oldOwner, "oldOwner parameter cannot be null.");
        Preconditions.checkNotNull(newOwner, "newOwner parameter cannot be null.");
        this.islandsContainer.transferIsland(oldOwner, newOwner);
    }

    @Override
    public int getSize() {
        return this.islandsContainer.getIslandsAmount();
    }

    @Override
    public void sortIslands(SortingType sortingType) {
        Preconditions.checkNotNull(sortingType, "sortingType parameter cannot be null.");
        sortIslands(sortingType, null);
    }

    @Override
    public void sortIslands(SortingType sortingType, Runnable onFinish) {
        Preconditions.checkNotNull(sortingType, "sortingType parameter cannot be null.");

        PluginDebugger.debug("Action: Sort Islands, Sorting Type: " + sortingType.getName());

        this.islandsContainer.sortIslands(sortingType, () -> {
            plugin.getMenus().refreshTopIslands(sortingType);
            if (onFinish != null)
                onFinish.run();
        });
    }

    @Override
    public Island getSpawnIsland() {
        if (spawnIsland == null)
            updateSpawn();

        return spawnIsland;
    }

    @Override
    public World getIslandsWorld(Island island, World.Environment environment) {
        Preconditions.checkNotNull(island, "island parameter cannot be null.");
        Preconditions.checkNotNull(environment, "environment parameter cannot be null.");
        return plugin.getProviders().getWorldsProvider().getIslandsWorld(island, environment);
    }

    @Override
    public boolean isIslandsWorld(World world) {
        Preconditions.checkNotNull(world, "world parameter cannot be null.");
        return customWorlds.contains(world.getUID()) || plugin.getProviders().getWorldsProvider().isIslandsWorld(world);
    }

    @Override
    public void registerIslandWorld(World world) {
        Preconditions.checkNotNull(world, "world parameter cannot be null.");
        customWorlds.add(world.getUID());
    }

    @Override
    public List<World> getRegisteredWorlds() {
        return customWorlds.stream().map(Bukkit::getWorld).collect(Collectors.toList());
    }

    @Override
    public List<UUID> getAllIslands(SortingType sortingType) {
        Preconditions.checkNotNull(sortingType, "sortingType parameter cannot be null.");
        return Collections.unmodifiableList(this.islandsContainer.getIslandsUnsorted().stream()
                .map(island -> island.getOwner().getUniqueId())
                .collect(Collectors.toList()));
    }

    @Override
    public List<Island> getIslands() {
        return this.islandsContainer.getIslandsUnsorted();
    }

    @Override
    public List<Island> getIslands(SortingType sortingType) {
        Preconditions.checkNotNull(sortingType, "sortingType parameter cannot be null.");
        return this.islandsContainer.getSortedIslands(sortingType);
    }

    @Override
    @Deprecated
    public int getBlockAmount(Block block) {
        Preconditions.checkNotNull(block, "block parameter cannot be null.");
        return getBlockAmount(block.getLocation());
    }

    @Override
    @Deprecated
    public int getBlockAmount(Location location) {
        return plugin.getStackedBlocks().getStackedBlockAmount(location);
    }

    @Override
    @Deprecated
    public void setBlockAmount(Block block, int amount) {
        plugin.getStackedBlocks().setStackedBlock(block, amount);
    }

    @Override
    public List<Location> getStackedBlocks() {
        return Collections.unmodifiableList(new ArrayList<>(plugin.getStackedBlocks().getStackedBlocks().keySet()));
    }

    @Override
    public void calcAllIslands() {
        calcAllIslands(null);
    }

    @Override
    public void calcAllIslands(Runnable callback) {
        PluginDebugger.debug("Action: Calculate All Islands");
        List<Island> islands = new ArrayList<>();

        {
            for (Island island : this.islandsContainer.getIslandsUnsorted()) {
                if (!island.isBeingRecalculated())
                    islands.add(island);
            }
        }

        for (int i = 0; i < islands.size(); i++) {
            islands.get(i).calcIslandWorth(null, i + 1 < islands.size() ? null : callback);
        }
    }

    @Override
    public void addIslandToPurge(Island island) {
        Preconditions.checkNotNull(island, "island parameter cannot be null.");
        Preconditions.checkNotNull(island.getOwner(), "island's owner cannot be null.");
        PluginDebugger.debug("Action: Purge Island, Island: " + island.getOwner().getName());
        this.islandsPurger.scheduleIslandPurge(island);
    }

    @Override
    public void removeIslandFromPurge(Island island) {
        Preconditions.checkNotNull(island, "island parameter cannot be null.");
        Preconditions.checkNotNull(island.getOwner(), "island's owner cannot be null.");
        PluginDebugger.debug("Action: Remove From Purge, Island: " + island.getOwner().getName());
        this.islandsPurger.unscheduleIslandPurge(island);
    }

    @Override
    public boolean isIslandPurge(Island island) {
        Preconditions.checkNotNull(island, "island parameter cannot be null.");
        Preconditions.checkNotNull(island.getOwner(), "island's owner cannot be null.");
        return this.islandsPurger.isIslandPurgeScheduled(island);
    }

    @Override
    public List<Island> getIslandsToPurge() {
        return this.islandsPurger.getScheduledPurgedIslands();
    }

    @Override
    public void registerSortingType(SortingType sortingType) {
        Preconditions.checkNotNull(sortingType, "sortingType parameter cannot be null.");
        PluginDebugger.debug("Action: Register Sorting Type, Sorting Type: " + sortingType.getName());
        this.islandsContainer.addSortingType(sortingType, true);
    }

    @Override
    public BigDecimal getTotalWorth() {
        long currentTime = System.currentTimeMillis();

        if (currentTime - lastTimeWorthUpdate > 60000) {
            lastTimeWorthUpdate = currentTime;
            totalWorth = BigDecimal.ZERO;
            for (Island island : getIslands())
                totalWorth = totalWorth.add(island.getWorth());
        }

        return totalWorth;
    }

    @Override
    public BigDecimal getTotalLevel() {
        long currentTime = System.currentTimeMillis();

        if (currentTime - lastTimeLevelUpdate > 60000) {
            lastTimeLevelUpdate = currentTime;
            totalLevel = BigDecimal.ZERO;
            for (Island island : getIslands())
                totalLevel = totalLevel.add(island.getIslandLevel());
        }

        return totalLevel;
    }

    @Override
    public Location getLastIslandLocation() {
        return lastIsland.parse();
    }

    @Override
    public void setLastIslandLocation(Location location) {
        this.setLastIsland(SBlockPosition.of(location));
    }

    @Override
    public IslandsContainer getIslandsContainer() {
        return this.islandsContainer;
    }

    @Override
    public DatabaseBridge getDatabaseBridge() {
        return databaseBridge;
    }

    public UUID generateIslandUUID() {
        UUID uuid;

        do {
            uuid = UUID.randomUUID();
        } while (getIslandByUUID(uuid) != null || getIsland(uuid) != null);

        return uuid;
    }

    public void disablePlugin() {
        this.pluginDisable = true;
        cancelAllIslandPreviews();
    }

    public boolean wasPluginDisabled() {
        return this.pluginDisable;
    }

    public void loadGrid(DatabaseResult resultSet) {
        resultSet.getString("last_island").map(SBlockPosition::of)
                .ifPresent(lastIsland -> this.lastIsland = lastIsland);

        if (!lastIsland.getWorldName().equalsIgnoreCase(plugin.getSettings().getWorlds().getDefaultWorldName())) {
            lastIsland = SBlockPosition.of(plugin.getSettings().getWorlds().getDefaultWorldName(),
                    lastIsland.getX(), lastIsland.getY(), lastIsland.getZ());
        }

        int maxIslandSize = resultSet.getInt("max_island_size").orElse(plugin.getSettings().getMaxIslandSize());
        String world = resultSet.getString("world").orElse(plugin.getSettings().getWorlds().getDefaultWorldName());

        try {
            if (plugin.getSettings().getMaxIslandSize() != maxIslandSize) {
                SuperiorSkyblockPlugin.log("&cYou have changed the max-island-size value without deleting database.");
                SuperiorSkyblockPlugin.log("&cRestoring it to the old value...");
                plugin.getSettings().updateValue("max-island-size", maxIslandSize);
            }

            if (!plugin.getSettings().getWorlds().getDefaultWorldName().equals(world)) {
                SuperiorSkyblockPlugin.log("&cYou have changed the island-world value without deleting database.");
                SuperiorSkyblockPlugin.log("&cRestoring it to the old value...");
                plugin.getSettings().updateValue("worlds.normal-world", world);
            }
        } catch (IOException ex) {
            ex.printStackTrace();
            PluginDebugger.debug(ex);
            Bukkit.shutdown();
        }
    }

    public void saveIslands() {
        List<Island> onlineIslands = Bukkit.getOnlinePlayers().stream()
                .map(player -> plugin.getPlayers().getSuperiorPlayer(player).getIsland())
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        List<Island> modifiedIslands = getIslands().stream()
                .filter(IslandsDatabaseBridge::isModified)
                .collect(Collectors.toList());

        if (!onlineIslands.isEmpty())
            onlineIslands.forEach(Island::updateLastTime);

        if (!modifiedIslands.isEmpty())
            modifiedIslands.forEach(IslandsDatabaseBridge::executeFutureSaves);

        getIslands().forEach(Island::removeEffects);
    }

    private void setLastIsland(SBlockPosition lastIsland) {
        PluginDebugger.debug("Action: Set Last Island, Location: " + lastIsland);
        this.lastIsland = lastIsland;
        GridDatabaseBridge.saveLastIsland(this, lastIsland);
    }

    private void initializeDatabaseBridge() {
        databaseBridge = plugin.getFactory().createDatabaseBridge(this);
        databaseBridge.setDatabaseBridgeMode(DatabaseBridgeMode.SAVE_DATA);
    }

}
