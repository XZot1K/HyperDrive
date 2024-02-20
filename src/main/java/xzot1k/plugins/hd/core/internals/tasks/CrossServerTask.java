package xzot1k.plugins.hd.core.internals.tasks;

import org.jetbrains.annotations.NotNull;
import xzot1k.plugins.hd.HyperDrive;

public class CrossServerTask implements Runnable {

    private final HyperDrive INSTANCE;

    public CrossServerTask(@NotNull HyperDrive instance) {
        INSTANCE = instance;

    }

    @Override
    public void run() {

    }
}