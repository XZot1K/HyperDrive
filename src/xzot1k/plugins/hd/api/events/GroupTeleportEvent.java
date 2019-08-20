package xzot1k.plugins.hd.api.events;

import java.util.List;
import java.util.UUID;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public class GroupTeleportEvent extends Event implements Cancellable {
	private static HandlerList handlers;
	private boolean cancelled;
	private Location destination;
	private Player groupLeader;
	private List<UUID> groupMemberIds;

	public GroupTeleportEvent(Location destination, Player groupLeader, List<UUID> groupMemberIds) {
		handlers = new HandlerList();
		setCancelled(false);
		setDestination(destination);
		setGroupLeader(groupLeader);
		setGroupMemberIds(groupMemberIds);
	}

	@Override
	public HandlerList getHandlers() {
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

	public List<UUID> getGroupMemberIds() {
		return groupMemberIds;
	}

	private void setGroupMemberIds(List<UUID> groupMemberIds) {
		this.groupMemberIds = groupMemberIds;
	}

	public Player getGroupLeader() {
		return groupLeader;
	}

	private void setGroupLeader(Player groupLeader) {
		this.groupLeader = groupLeader;
	}

	public Location getDestination() {
		return destination;
	}

	public void setDestination(Location destination) {
		this.destination = destination;
	}

	public static HandlerList getHandlerList() {
		return handlers;
	}
}
