package xzot1k.plugins.hd.core.objects.json;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import xzot1k.plugins.hd.HyperDrive;
import xzot1k.plugins.hd.api.EnumContainer;

public class JSONMessage
{

    private HyperDrive pluginInstance;
    private JSONObject chatObject;

    @SuppressWarnings("unchecked")
    public JSONMessage(HyperDrive pluginInstance, String text)
    {
        setPluginInstance(pluginInstance);
        chatObject = new JSONObject();
        if (text != null) getChatObject().put("text", getPluginInstance().getManager().colorText(text));
    }

    @SuppressWarnings("unchecked")
    public void addExtra(JSONExtra extraObject)
    {
        if (!chatObject.containsKey("extra")) chatObject.put("extra", new JSONArray());
        JSONArray extra = (JSONArray) chatObject.get("extra");
        extra.add(extraObject.getExtraObject());
        getChatObject().put("extra", extra);
    }

    public void sendJSONToPlayer(Player player)
    {
        getPluginInstance().getManager().getJsonHandler().sendJSONMessage(player, getChatObject().toJSONString());
    }

    @SuppressWarnings("unchecked")
    public void setClickEvent(EnumContainer.JSONClickAction action, String value)
    {
        JSONObject clickEvent = new JSONObject();
        clickEvent.put("action", action.name().toLowerCase());
        clickEvent.put("value", getPluginInstance().getManager().colorText(value));
        getChatObject().put("clickEvent", clickEvent);
    }

    @SuppressWarnings("unchecked")
    public void setHoverEvent(EnumContainer.JSONHoverAction action, String value)
    {
        JSONObject hoverEvent = new JSONObject();
        hoverEvent.put("action", action.name().toLowerCase());
        hoverEvent.put("value", getPluginInstance().getManager().colorText(value));
        getChatObject().put("hoverEvent", hoverEvent);
    }

    public String itemToJSON(ItemStack itemStack)
    {
        return getPluginInstance().getManager().getJsonHandler().getJSONItem(itemStack);
    }

    private JSONObject getChatObject()
    {
        return chatObject;
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
