package com.bgsoftware.superiorskyblock.database.bridge;

import com.bgsoftware.superiorskyblock.api.data.DatabaseBridge;
import com.bgsoftware.superiorskyblock.api.data.DatabaseBridgeMode;
import com.bgsoftware.superiorskyblock.api.data.DatabaseFilter;
import com.bgsoftware.superiorskyblock.api.objects.Pair;

import javax.annotation.Nullable;
import java.util.Map;
import java.util.function.Consumer;

public final class EmptyDatabaseBridge implements DatabaseBridge {

    private static final EmptyDatabaseBridge instance = new EmptyDatabaseBridge();

    private EmptyDatabaseBridge() {

    }

    public static EmptyDatabaseBridge getInstance() {
        return instance;
    }

    @Override
    public void loadAllObjects(String table, Consumer<Map<String, Object>> resultConsumer) {
        // Do nothing.
    }

    @Override
    public void batchOperations(boolean batchOperations) {
        // Do nothing.
    }

    @Override
    public void updateObject(String table, @Nullable DatabaseFilter filter, Pair<String, Object>... columns) {
        // Do nothing.
    }

    @Override
    public void insertObject(String table, Pair<String, Object>... columns) {
        // Do nothing.
    }

    @Override
    public void deleteObject(String table, @Nullable DatabaseFilter filter) {
        // Do nothing.
    }

    @Override
    public void loadObject(String table, @Nullable DatabaseFilter filter, Consumer<Map<String, Object>> resultConsumer) {
        // Do nothing.
    }

    @Override
    public void setDatabaseBridgeMode(DatabaseBridgeMode databaseBridgeMode) {
        // Do nothing.
    }

}
