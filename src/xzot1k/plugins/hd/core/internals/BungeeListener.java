package xzot1k.plugins.hd.core.internals;

import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import org.bukkit.entity.Player;
import org.bukkit.plugin.messaging.PluginMessageListener;
import xzot1k.plugins.hd.HyperDrive;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class BungeeListener implements PluginMessageListener
{
    private HyperDrive pluginInstance;
    private HashMap<String, String> serverAddressMap;

    public BungeeListener(HyperDrive pluginInstance)
    {
        setPluginInstance(pluginInstance);
        setServerAddressMap(new HashMap<>());

        Player firstPlayer = getFirstPlayer();
        if (firstPlayer != null)
        {
            requestServer(firstPlayer);
            requestServers(firstPlayer);
        }
    }

    public String getServerName(String ipAddress)
    {
        ipAddress = ipAddress.replace("localhost", "127.0.0.1");
        List<String> serverNames = new ArrayList<>(getServerAddressMap().keySet());
        for (int i = -1; ++i < serverNames.size(); )
        {
            String serverName = serverNames.get(i), ip = getServerAddressMap().get(serverName);
            if (ipAddress == null || !ipAddress.equalsIgnoreCase(ip)) continue;
            return serverName;
        }

        return null;
    }

    @Override
    public void onPluginMessageReceived(String channel, Player player, byte[] message)
    {
        if (!channel.equals("BungeeCord")) return;

        ByteArrayDataInput in = ByteStreams.newDataInput(message);
        String subChannel = in.readUTF(), serverName;
        switch (subChannel)
        {
            case "GetServers":
                String[] serverList = in.readUTF().split(", ");
                for (int i = -1; ++i < serverList.length; )
                    requestServerIP(serverList[i]);
                break;

            case "ServerIP":
                serverName = in.readUTF();
                String ip = in.readUTF();
                short port = in.readShort();
                getServerAddressMap().put(serverName, ip + ":" + port);
                break;

            default:
                break;
        }
    }

    public void requestServers(Player player)
    {
        ByteArrayDataOutput output = ByteStreams.newDataOutput();
        output.writeUTF("GetServers");
        player.sendPluginMessage(getPluginInstance(), "BungeeCord", output.toByteArray());
    }

    private void requestServer(Player player)
    {
        ByteArrayDataOutput output = ByteStreams.newDataOutput();
        output.writeUTF("GetServer");
        player.sendPluginMessage(getPluginInstance(), "BungeeCord", output.toByteArray());
    }

    private void requestServerIP(String server)
    {
        ByteArrayDataOutput out = ByteStreams.newDataOutput();
        out.writeUTF("ServerIP");
        out.writeUTF(server);
        Player firstPlayer = getFirstPlayer();
        if (firstPlayer != null) firstPlayer.sendPluginMessage(getPluginInstance(), "BungeeCord", out.toByteArray());
    }

    public Player getFirstPlayer()
    {
        List<Player> playerList = new ArrayList<>(getPluginInstance().getServer().getOnlinePlayers());
        for (int i = -1; ++i < playerList.size(); )
            return playerList.get(i);
        return null;
    }

    public String getIPFromMap(String serverName)
    {
        if (!getServerAddressMap().isEmpty() && getServerAddressMap().containsKey(serverName))
            return getServerAddressMap().get(serverName);
        return null;
    }

    // getters & setters
    private HyperDrive getPluginInstance()
    {
        return pluginInstance;
    }

    private void setPluginInstance(HyperDrive pluginInstance)
    {
        this.pluginInstance = pluginInstance;
    }

    private HashMap<String, String> getServerAddressMap()
    {
        return serverAddressMap;
    }

    private void setServerAddressMap(HashMap<String, String> serverAddressMap)
    {
        this.serverAddressMap = serverAddressMap;
    }

}
