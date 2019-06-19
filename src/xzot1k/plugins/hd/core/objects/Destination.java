package xzot1k.plugins.hd.core.objects;

import org.bukkit.Location;
import xzot1k.plugins.hd.HyperDrive;
import xzot1k.plugins.hd.api.objects.SerializableLocation;
import xzot1k.plugins.hd.api.objects.Warp;

public class Destination {

    private HyperDrive pluginInstance;
    private SerializableLocation location;
    private Warp warp;

    public Destination(HyperDrive pluginInstance, Location location) {
        setPluginInstance(pluginInstance);
        setLocation(location);
        setWarp(null);
    }

    public Destination(HyperDrive pluginInstance, SerializableLocation location) {
        setPluginInstance(pluginInstance);
        setLocation(location);
        setWarp(null);
    }

    private HyperDrive getPluginInstance() {
        return pluginInstance;
    }

    private void setPluginInstance(HyperDrive pluginInstance) {
        this.pluginInstance = pluginInstance;
    }

    public SerializableLocation getLocation() {
        return location;
    }

    public void setLocation(Location location) {
        this.location = new SerializableLocation(location);
    }

    public void setLocation(SerializableLocation location) {
        this.location = location;
    }

    public Warp getWarp() {
        return warp;
    }

    public void setWarp(Warp warp) {
        this.warp = warp;
    }
}
