package xzot1k.plugins.hd.core.objects.json;

import org.bukkit.inventory.ItemStack;
import org.json.simple.JSONObject;
import xzot1k.plugins.hd.HyperDrive;
import xzot1k.plugins.hd.api.EnumContainer;

public class JSONExtra
{
    private HyperDrive pluginInstance;
    private JSONObject extraObject;

    @SuppressWarnings("unchecked")
    public JSONExtra(HyperDrive pluginInstance, String text)
    {
        setPluginInstance(pluginInstance);
        extraObject = new JSONObject();
        if (text != null) getExtraObject().put("text", getPluginInstance().getManager().colorText(text));
    }

    @SuppressWarnings("unchecked")
    public void setClickEvent(EnumContainer.JSONClickAction action, String value)
    {
        JSONObject clickEvent = new JSONObject();
        clickEvent.put("action", action.name().toLowerCase());
        clickEvent.put("value", getPluginInstance().getManager().colorText(value));
        getExtraObject().put("clickEvent", clickEvent);
    }

    @SuppressWarnings("unchecked")
    public void setHoverEvent(EnumContainer.JSONHoverAction action, String value)
    {
        JSONObject hoverEvent = new JSONObject();
        hoverEvent.put("action", action.name().toLowerCase());
        hoverEvent.put("value", getPluginInstance().getManager().colorText(value));
        getExtraObject().put("hoverEvent", hoverEvent);
    }

    public String itemToJSON(ItemStack itemStack)
    {
        return getPluginInstance().getManager().getJsonHandler().getJSONItem(itemStack);
    }

    public JSONObject getExtraObject()
    {
        return extraObject;
    }

    private HyperDrive getPluginInstance()
    {
        return pluginInstance;
    }

    private void setPluginInstance(HyperDrive pluginInstance)
    {
        this.pluginInstance = pluginInstance;
    }
}
