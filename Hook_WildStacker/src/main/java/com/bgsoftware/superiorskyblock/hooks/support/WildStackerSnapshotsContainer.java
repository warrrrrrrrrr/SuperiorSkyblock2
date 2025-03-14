package com.bgsoftware.superiorskyblock.hooks.support;

import com.bgsoftware.superiorskyblock.utils.debug.PluginDebugger;
import com.bgsoftware.superiorskyblock.world.chunks.ChunkPosition;
import com.bgsoftware.wildstacker.api.WildStackerAPI;
import com.bgsoftware.wildstacker.api.objects.StackedSnapshot;
import org.bukkit.Chunk;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class WildStackerSnapshotsContainer {

    private static final Map<ChunkPosition, StackedSnapshot> cachedSnapshots = new ConcurrentHashMap<>();

    private WildStackerSnapshotsContainer() {

    }

    public static void takeSnapshot(Chunk chunk) {
        ChunkPosition chunkPosition = ChunkPosition.of(chunk);

        if (cachedSnapshots.containsKey(chunkPosition))
            return;

        try {
            StackedSnapshot stackedSnapshot;

            try {
                stackedSnapshot = WildStackerAPI.getWildStacker().getSystemManager().getStackedSnapshot(chunk);
            } catch (Throwable ex) {
                //noinspection deprecation
                stackedSnapshot = WildStackerAPI.getWildStacker().getSystemManager().getStackedSnapshot(chunk, false);
            }

            if (stackedSnapshot != null) {
                cachedSnapshots.put(chunkPosition, stackedSnapshot);
            }
        } catch (Throwable error) {
            PluginDebugger.debug(error);
        }
    }

    public static void releaseSnapshot(ChunkPosition chunkPosition) {
        cachedSnapshots.remove(chunkPosition);
    }

    public static StackedSnapshot getSnapshot(ChunkPosition chunkPosition) {
        StackedSnapshot stackedSnapshot = cachedSnapshots.get(chunkPosition);

        if (stackedSnapshot == null) {
            throw new RuntimeException("Chunk " + chunkPosition + " is not cached.");
        }

        return stackedSnapshot;
    }

}
