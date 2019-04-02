package ru.csm.bungee.network.executors;

import com.google.gson.JsonObject;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import ru.csm.api.network.Channels;
import ru.csm.api.player.SkinPlayer;
import ru.csm.api.services.SkinsAPI;
import ru.csm.bungee.network.JsonMessage;
import ru.csm.bungee.network.MessageExecutor;

import java.util.UUID;

public class ExecutorCommandPlayer implements MessageExecutor {

    private SkinsAPI api;

    public ExecutorCommandPlayer(SkinsAPI api){
        this.api = api;
    }

    @Override
    public JsonMessage execute(JsonObject json) {
        UUID uuid = UUID.fromString(json.get("player").getAsString());
        String name = json.get("name").getAsString();
        SkinPlayer<ProxiedPlayer> player = api.getPlayer(uuid);

        if(player != null){
            api.setSkinFromName(player, name);
            return null;
        }

        return null;
    }

}
