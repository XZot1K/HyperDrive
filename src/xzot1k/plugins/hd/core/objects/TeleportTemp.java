package xzot1k.plugins.hd.core.objects;

import xzot1k.plugins.hd.HyperDrive;

public class TeleportTemp
{
    private HyperDrive pluginInstance;
    private String teleportTypeId, teleportValue;
    private int seconds;

    public TeleportTemp(HyperDrive pluginInstance, String teleportTypeId, String teleportValue, int seconds)
    {
        setPluginInstance(pluginInstance);
        setTeleportTypeId(teleportTypeId);
        setTeleportValue(teleportValue);
        setSeconds(seconds);
    }

    // getters & setters
    public HyperDrive getPluginInstance()
    {
        return pluginInstance;
    }

    private void setPluginInstance(HyperDrive pluginInstance)
    {
        this.pluginInstance = pluginInstance;
    }

    public int getSeconds()
    {
        return seconds;
    }

    public void setSeconds(int seconds)
    {
        this.seconds = seconds;
    }

    public String getTeleportTypeId()
    {
        return teleportTypeId;
    }

    private void setTeleportTypeId(String teleportTypeId)
    {
        this.teleportTypeId = teleportTypeId;
    }

	public String getTeleportValue()
	{
		return teleportValue;
	}

	private void setTeleportValue(String teleportValue)
	{
		this.teleportValue = teleportValue;
	}
}
