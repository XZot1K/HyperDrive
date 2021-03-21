/*
 * Copyright (c) 2021. All rights reserved.
 */

package xzot1k.plugins.hd.core.internals;

import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import org.bukkit.entity.Player;
import org.bukkit.plugin.messaging.PluginMessageListener;
import xzot1k.plugins.hd.HyperDrive;
import xzot1k.plugins.hd.api.objects.SerializableLocation;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

public class BungeeListener implements PluginMessageListener {
    private HyperDrive pluginInstance;
    private String myServer;
    private HashMap<String, String> serverAddressMap;
    private HashMap<UUID, SerializableLocation> transferMap;

    public BungeeListener(HyperDrive pluginInstance) {
        setPluginInstance(pluginInstance);
        setServerAddressMap(new HashMap<>());
        setTransferMap(new HashMap<>());

        updateServerList();
    }

    public void updateServerList() {
        final Player firstPlayer = getFirstPlayer();
        if (firstPlayer != null) {
            requestValue(firstPlayer, "GetServer");
            requestValue(firstPlayer, "GetServers");
        }
    }

    @Override
    public void onPluginMessageReceived(String channel, Player player, byte[] message) {
        if (!channel.equals("BungeeCord")) return;

        ByteArrayDataInput in = ByteStreams.newDataInput(message);
        String subChannel = in.readUTF();

        switch (subChannel) {
            case "HyperDrive":
                final String[] lineArgs = in.readUTF().split(";");
                final UUID uuid = UUID.fromString(lineArgs[0]);

                final String[] locationArgs = lineArgs[1].split(":"), coordArgs = locationArgs[1].split(",");
                SerializableLocation location = new SerializableLocation(locationArgs[0], Double.parseDouble(coordArgs[0]),
                        Double.parseDouble(coordArgs[1]), Double.parseDouble(coordArgs[2]), Float.parseFloat(coordArgs[3]),
                        Float.parseFloat(coordArgs[4]));
                getTransferMap().put(uuid, location);
                break;
            case "GetServer":
                setMyServer(in.readUTF());
                break;
            case "GetServers":
                String[] serverList = in.readUTF().split(", ");
                for (int i = -1; ++i < serverList.length; )
                    requestValue(player, "ServerIP", serverList[i]);
                break;
            case "ServerIP":
                getServerAddressMap().put(in.readUTF(), (in.readUTF() + ":" + in.readShort()));
                break;
            default:
                break;
        }
    }

    private void requestValue(Player player, String... values) {
        ByteArrayDataOutput output = ByteStreams.newDataOutput();
        for (int i = -1; ++i < values.length; )
            output.writeUTF(values[i]);
        player.sendPluginMessage(getPluginInstance(), "BungeeCord", output.toByteArray());
    }

    public String getServerName(String ipAddress) {
        ipAddress = ipAddress.replace("localhost", "127.0.0.1");
        List<String> serverNames = new ArrayList<>(getServerAddressMap().keySet());
        for (int i = -1; ++i < serverNames.size(); ) {
            String serverName = serverNames.get(i), ip = getServerAddressMap().get(serverName);
            System.out.println(serverName + " - " + ip);
            if (!ipAddress.equalsIgnoreCase(ip)) continue;
            return serverName;
        }

        return null;
    }

    public Player getFirstPlayer() {
        return !getPluginInstance().getServer().getOnlinePlayers().isEmpty() ? getPluginInstance().getServer().getOnlinePlayers().iterator().next() : null;
    }

    // getters & setters
    private HyperDrive getPluginInstance() {
        return pluginInstance;
    }

    private void setPluginInstance(HyperDrive pluginInstance) {
        this.pluginInstance = pluginInstance;
    }

    public String getMyServer() {
        return myServer;
    }

    private void setMyServer(String myServer) {
        this.myServer = myServer;
    }

    public HashMap<String, String> getServerAddressMap() {
        return serverAddressMap;
    }

    private void setServerAddressMap(HashMap<String, String> serverAddressMap) {
        this.serverAddressMap = serverAddressMap;
    }

    public HashMap<UUID, SerializableLocation> getTransferMap() {
        return transferMap;
    }

    private void setTransferMap(HashMap<UUID, SerializableLocation> transferMap) {
        this.transferMap = transferMap;
    }
}
