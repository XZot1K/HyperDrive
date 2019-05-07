package xzot1k.plugins.hd.api.events;

import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.bukkit.inventory.Inventory;
import xzot1k.plugins.hd.HyperDrive;
import xzot1k.plugins.hd.api.EnumContainer;

public class MenuOpenEvent extends Event implements Cancellable
{
    private HyperDrive pluginInstance;
    private HandlerList handlers;
    private boolean cancelled;
    private Inventory openedMenu;
    private EnumContainer.MenuType menuType;
    private String customMenuId;
    private Player player;

    public MenuOpenEvent(HyperDrive pluginInstance, EnumContainer.MenuType menuType, Inventory openedMenu, Player player)
    {
        setPluginInstance(pluginInstance);
        setHandlers(new HandlerList());
        setCancelled(false);
        setOpenedMenu(openedMenu);
        setPlayer(player);
        setMenuType(menuType);
        setCustomMenuId(null);
    }

    public HandlerList getHandlers()
    {
        return handlers;
    }

    private HyperDrive getPluginInstance()
    {
        return pluginInstance;
    }

    private void setPluginInstance(HyperDrive pluginInstance)
    {
        this.pluginInstance = pluginInstance;
    }

    private void setHandlers(HandlerList handlers)
    {
        this.handlers = handlers;
    }

    @Override
    public boolean isCancelled()
    {
        return cancelled;
    }

    @Override
    public void setCancelled(boolean cancelled)
    {
        this.cancelled = cancelled;
    }

    public Player getPlayer()
    {
        return player;
    }

    private void setPlayer(Player player)
    {
        this.player = player;
    }

    public Inventory getOpenedMenu()
    {
        return openedMenu;
    }

    private void setOpenedMenu(Inventory openedMenu)
    {
        this.openedMenu = openedMenu;
    }

    public EnumContainer.MenuType getMenuType()
    {
        return menuType;
    }

    private void setMenuType(EnumContainer.MenuType menuType)
    {
        this.menuType = menuType;
    }

    public String getCustomMenuId()
    {
        return customMenuId;
    }

    public void setCustomMenuId(String customMenuId)
    {
        this.customMenuId = customMenuId;
    }
}
