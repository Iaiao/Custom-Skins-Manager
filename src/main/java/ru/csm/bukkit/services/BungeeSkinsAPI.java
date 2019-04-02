package ru.csm.bukkit.services;

import com.google.gson.JsonObject;
import ninja.leaping.configurate.objectmapping.ObjectMappingException;
import org.bukkit.entity.Player;
import ru.csm.api.network.Channels;
import ru.csm.api.player.Skin;
import ru.csm.api.player.SkinModel;
import ru.csm.api.player.SkinPlayer;
import ru.csm.api.services.SkinsAPI;
import ru.csm.api.storage.Configuration;
import ru.csm.api.storage.Language;
import ru.csm.api.storage.database.Database;
import ru.csm.bukkit.network.PluginMessageService;
import ru.csm.bukkit.player.CitizensSkinPlayer;

public class BungeeSkinsAPI extends SkinsAPI {

    public BungeeSkinsAPI(Database database, Configuration conf, Language lang) throws ObjectMappingException {
        super(database, conf, lang);
    }

    @Override
    public void setCustomSkin(SkinPlayer player, Skin skin){
        JsonObject json = new JsonObject();
        json.addProperty("player", player.getUUID().toString());

        JsonObject skinJson = new JsonObject();
        skinJson.addProperty("value", skin.getValue());
        skinJson.addProperty("signature", skin.getSignature());

        json.add("skin", skinJson);

        sendJsonMessage(player, Channels.SKINS_APPLY, json);
    }

    @Override
    public void setSkinFromImage(SkinPlayer player, String imageUrl, SkinModel model) {
        JsonObject json = new JsonObject();

        if(player instanceof CitizensSkinPlayer){
            CitizensSkinPlayer npc = (CitizensSkinPlayer) player;

            json.addProperty("sender", npc.getPlayer().getUniqueId().toString());
            json.addProperty("npc", npc.getUUID().toString());
            json.addProperty("url", imageUrl);
            json.addProperty("model", model.getName());
            sendJsonMessage(player, Channels.SKINS_CITIZENS, json);
            return;
        }

        json.addProperty("player", player.getUUID().toString());
        json.addProperty("url", imageUrl);
        json.addProperty("model", model.getName());

        sendJsonMessage(player, Channels.SKINS_URL, json);
    }

    @Override
    public void setSkinFromName(SkinPlayer player, String name) {
        JsonObject json = new JsonObject();
        json.addProperty("player", player.getUUID().toString());
        json.addProperty("name", name);

        sendJsonMessage(player, Channels.SKINS_PLAYER, json);
    }

    @Override
    public void resetSkin(SkinPlayer player) {
        JsonObject json = new JsonObject();
        json.addProperty("player", player.getUUID().toString());

        sendJsonMessage(player, Channels.SKINS_RESET, json);
    }

    private void sendJsonMessage(SkinPlayer player, String channel, JsonObject json){
        Player bukkitPlayer = (Player) player.getPlayer();
        PluginMessageService.sendMessage(bukkitPlayer, channel, json);
    }
}
