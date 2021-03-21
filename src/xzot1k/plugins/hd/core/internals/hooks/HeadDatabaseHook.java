/*
 * Copyright (c) 2021. All rights reserved.
 */

package xzot1k.plugins.hd.core.internals.hooks;

import me.arcaniax.hdb.api.HeadDatabaseAPI;

public class HeadDatabaseHook {

    private final HeadDatabaseAPI headDatabaseAPI;

    public HeadDatabaseHook() {
        headDatabaseAPI = new HeadDatabaseAPI();
    }

    public boolean isHeadDatabaseAvailable() {
        return getHeadDatabaseAPI() != null;
    }

    public HeadDatabaseAPI getHeadDatabaseAPI() {
        return headDatabaseAPI;
    }

}