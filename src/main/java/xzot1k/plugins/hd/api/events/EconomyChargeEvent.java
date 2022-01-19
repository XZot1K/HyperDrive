/*
 * Copyright (c) 2020. All rights reserved.
 */

package xzot1k.plugins.hd.api.events;

import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public class EconomyChargeEvent extends Event implements Cancellable {
    final private static HandlerList handlers = new HandlerList();
    private boolean cancelled;
    private double amount;
    private Player player;

    public EconomyChargeEvent(Player player, double price) {
        setCancelled(false);
        setPlayer(player);
        setAmount(price);
    }

    public static HandlerList getHandlerList() {
        return handlers;
    }

    @Override
    public boolean isCancelled() {
        return cancelled;
    }

    @Override
    public void setCancelled(boolean cancelled) {
        this.cancelled = cancelled;
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

    public double getAmount() {
        return amount;
    }

    public void setAmount(double amount) {
        this.amount = amount;
    }
}
