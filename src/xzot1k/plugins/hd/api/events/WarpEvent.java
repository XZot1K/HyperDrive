/*
 * Copyright (c) 2019. All rights reserved.
 */

package xzot1k.plugins.hd.api.events;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public class WarpEvent extends Event implements Cancellable {
    private static HandlerList handlers;
    private boolean cancelled;
    private Location location;
    private Player player;

    public WarpEvent(Location location, Player player) {
        handlers = new HandlerList();
        setCancelled(false);
        setLocation(location);
        setPlayer(player);
    }

    @Override
    public boolean isCancelled() {
        return cancelled;
    }

    @Override
    public void setCancelled(boolean cancelled) {
        this.cancelled = cancelled;
    }

    public Location getLocation() {
        return location;
    }

    public void setLocation(Location location) {
        this.location = location;
    }

    public Player getPlayer() {
        return player;
    }

    private void setPlayer(Player player) {
        this.player = player;
    }

    @Override
    public HandlerList getHandlers() {
        return handlers;
    }

    public static HandlerList getHandlerList() {
        return handlers;
    }
}
