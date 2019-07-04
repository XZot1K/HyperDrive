package xzot1k.plugins.hd.api.events;

import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import xzot1k.plugins.hd.HyperDrive;

public class EconomyChargeEvent extends Event implements Cancellable {
    private HyperDrive pluginInstance;
    private static HandlerList handlers;
    private boolean cancelled;
    private double amount;
    private Player player;

    public EconomyChargeEvent(HyperDrive pluginInstance, Player player, double price) {
        setPluginInstance(pluginInstance);
        handlers = new HandlerList();
        setCancelled(false);
        setPlayer(player);
        setAmount(price);
    }

    private HyperDrive getPluginInstance() {
        return pluginInstance;
    }

    private void setPluginInstance(HyperDrive pluginInstance) {
        this.pluginInstance = pluginInstance;
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

    public static HandlerList getHandlerList() {
        return handlers;
    }

    public double getAmount() {
        return amount;
    }

    public void setAmount(double amount) {
        this.amount = amount;
    }
}
