/*
 * Copyright (c) 2021. All rights reserved.
 */

package xzot1k.plugins.hd.api.objects;

import net.md_5.bungee.chat.ComponentSerializer;
import org.bukkit.entity.Player;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import xzot1k.plugins.hd.HyperDrive;

public class JSONMessage {

    private final HyperDrive pluginInstance;
    private final JSONObject jsonObject;

    @SuppressWarnings("unchecked")
    public JSONMessage(String text) {
        pluginInstance = HyperDrive.getPluginInstance();
        jsonObject = new JSONObject();
        if (text != null) getObject().put("text", getPluginInstance().getManager().colorText(text));
    }

    @SuppressWarnings("unchecked")
    public JSONMessage extend(JSONMessage jsonMessage) {
        if (!getObject().containsKey("extra")) getObject().put("extra", new JSONArray());
        JSONArray extra = (JSONArray) getObject().get("extra");
        extra.add(jsonMessage.getObject());
        getObject().put("extra", extra);
        return this;
    }

    public void send(Player player) {
        player.spigot().sendMessage(ComponentSerializer.parse(getObject().toJSONString()));
    }

    @SuppressWarnings("unchecked")
    public void setClickEvent(ClickAction action, String value) {
        JSONObject clickEvent = new JSONObject();
        clickEvent.put("action", action.name().toLowerCase());
        clickEvent.put("value", getPluginInstance().getManager().colorText(value));
        getObject().put("clickEvent", clickEvent);
    }

    @SuppressWarnings("unchecked")
    public void setHoverEvent(HoverAction action, String value) {
        JSONObject hoverEvent = new JSONObject();
        hoverEvent.put("action", action.name().toLowerCase());
        hoverEvent.put("value", getPluginInstance().getManager().colorText(value));
        getObject().put("hoverEvent", hoverEvent);
    }

    public JSONObject getObject() {
        return jsonObject;
    }

    private HyperDrive getPluginInstance() {
        return pluginInstance;
    }

    public enum Action {
        CLICK_EVENT, HOVER_EVENT
    }

    public enum ClickAction {
        OPEN_URL, OPEN_FILE, RUN_COMMAND, SUGGEST_COMMAND, CHANGE_PAGE
    }

    public enum HoverAction {
        SHOW_TEXT, SHOW_ACHIEVEMENT, SHOW_ITEM, SHOW_ENTITY
    }

}
