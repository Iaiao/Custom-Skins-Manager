package ru.csm.bukkit.services;

import com.google.common.reflect.TypeToken;
import ninja.leaping.modded.configurate.objectmapping.ObjectMappingException;
import org.bukkit.entity.Player;
import ru.csm.api.player.Head;
import ru.csm.api.player.Skin;
import ru.csm.api.player.SkinModel;
import ru.csm.api.player.SkinPlayer;
import ru.csm.api.services.SkinHash;
import ru.csm.api.services.SkinsAPI;
import ru.csm.api.storage.Configuration;
import ru.csm.api.storage.Language;
import ru.csm.api.storage.database.Database;
import ru.csm.api.upload.*;
import ru.csm.api.utils.Logger;
import ru.csm.api.utils.Validator;
import ru.csm.bukkit.player.BukkitSkinPlayer;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

public class BukkitSkinsAPI implements SkinsAPI<Player> {

    private final Configuration conf;
    private final Database database;
    private final Language lang;

    private final Map<UUID, SkinPlayer<Player>> playersByUUID = new TreeMap<>();
    private final Map<String, SkinPlayer<Player>> playersByName = new TreeMap<>();

    private Map<String, String> blacklist;
    private Map<String, String> whitelist;

    private Skin[] defaultSkins;

    private NameQueue nameQueue;
    private ImageQueue imageQueue;

    public BukkitSkinsAPI(Database database, Configuration conf, Language lang) {
        this.database = database;
        this.conf = conf;
        this.lang = lang;

        loadDefaultSkins();
        loadBlacklist();
        loadWhitelist();
        loadQueues();
    }

    @Override
    public Configuration getConfiguration(){
        return conf;
    }

    @Override
    public Language getLang(){
        return lang;
    }

    @Override
    public Database getDatabase() {
        return database;
    }

    @Override
    public NameQueue getNameQueue() {
        return nameQueue;
    }

    @Override
    public ImageQueue getImageQueue() {
        return imageQueue;
    }

    @Override
    public boolean isBlackList(String nickname, SkinPlayer<Player> player){
        if (blacklist == null) return false;

        if (blacklist.containsKey(nickname.toLowerCase())){
            String perm = blacklist.get(nickname.toLowerCase());
            return perm == null || player.hasPermission(perm);
        }

        return false;
    }

    @Override
    public boolean isWhitelist(String nickname, SkinPlayer<Player> player){
        if (whitelist == null) return true;

        if (whitelist.containsKey(nickname.toLowerCase())){
            String perm = whitelist.get(nickname.toLowerCase());
            return perm == null || player.hasPermission(perm);
        }

        return false;
    }

    @Override
    public Skin getDefaultSkin(){
        return defaultSkins[ThreadLocalRandom.current().nextInt(defaultSkins.length)];
    }

    @Override
    public SkinPlayer<Player> getPlayer(UUID uuid){
        return playersByUUID.get(uuid);
    }

    @Override
    public SkinPlayer<Player> getPlayer(String name){
        return playersByName.get(name.toLowerCase());
    }

    @Override
    public void setCustomSkin(SkinPlayer<Player> player, Skin skin){
        player.setCustomSkin(skin);
        player.applySkin();
        player.refreshSkin();
        savePlayer(player);
        player.sendMessage(lang.of("skin.success"));
    }

    @Override
    public void setCustomSkin(Player p, Skin skin) {
        SkinPlayer<Player> player = getPlayer(p.getUniqueId());
        if (player != null) setCustomSkin(player, skin);
    }

    @Override
    public void setSkinFromImage(SkinPlayer<Player> player, String link, SkinModel model) {
        if(!Validator.validateURL(link)){
            player.sendMessage(lang.of("skin.image.invalid"));
            return;
        }

        Optional<Skin> hashed = SkinHash.get(link);

        if (hashed.isPresent()){
            setCustomSkin(player, hashed.get());
            return;
        }

        imageQueue.push(player, link, model);
        long seconds = imageQueue.getWaitSeconds();
        player.sendMessage(String.format(lang.of("skin.process"), seconds));
    }

    @Override
    public void setSkinFromName(SkinPlayer<Player> player, String name) {
        if (!Validator.validateName(name)){
            player.sendMessage(lang.of("skin.name.invalid"));
            return;
        }

        if (isBlackList(name, player) || !isWhitelist(name, player)){
            player.sendMessage(lang.of("skin.unallowed"));
            return;
        }

        Optional<Skin> hashed = SkinHash.get(name);

        if (hashed.isPresent()){
            setCustomSkin(player, hashed.get());
            return;
        }

        nameQueue.push(player, name);
        long seconds = nameQueue.getWaitSeconds();
        player.sendMessage(String.format(lang.of("skin.process"), seconds));
    }

    @Override
    public void resetSkin(SkinPlayer<Player> player) {
        if(!player.hasCustomSkin()){
            player.sendMessage(lang.of("skin.reset.empty"));
            return;
        }

        player.resetSkin();
        player.applySkin();
        player.refreshSkin();
        savePlayer(player);
        player.sendMessage(lang.of("skin.reset.success"));
    }

    @Override
    public Head getPlayerHead(SkinPlayer<Player> player){
        if(player != null) {
            Skin skin = player.getDefaultSkin();
            if(player.hasCustomSkin()){
                skin = player.getCustomSkin();
            }
            return new Head(player.getUUID(), player.getName(), skin);
        }
        return null;
    }

    @Override
    public SkinPlayer<Player> buildPlayer(Player player) {
        return new BukkitSkinPlayer(player);
    }

    @Override
    public void addPlayer(SkinPlayer<Player> player) {
        playersByName.put(player.getName().toLowerCase(), player);
        playersByUUID.put(player.getUUID(), player);
    }

    @Override
    public void removePlayer(UUID uuid) {
        SkinPlayer<Player> player = getPlayer(uuid);
        if (player != null){
            playersByName.remove(player.getName().toLowerCase());
            playersByUUID.remove(uuid);
        }
    }

    private void loadDefaultSkins(){
        try{
            List<Skin> skins = conf.get().getNode("defaultSkins").getList(TypeToken.of(Skin.class));
            defaultSkins = skins.toArray(new Skin[0]);
        } catch (ObjectMappingException e){
            Logger.severe("Cannot load default skins: %s", e.getMessage());
        }
    }

    private void loadBlacklist(){
        boolean enable = conf.get().getNode("enableBlacklist").getBoolean();

        if (enable){
            blacklist = new HashMap<>();
            try{
                List<String> list = conf.get().getNode("blacklist").getList(TypeToken.of(String.class));

                for (String elem : list){
                    String[] arr = elem.split(":");
                    blacklist.put(arr[0].toLowerCase(), (arr.length == 2) ? arr[1] : null);
                }
            } catch (ObjectMappingException e){
                Logger.severe("Cannot load skins blacklist: %s", e.getMessage());
            }
        }
    }

    private void loadWhitelist(){
        boolean enable = conf.get().getNode("enableWhitelist").getBoolean();

        if (enable){
            blacklist = null;
            whitelist = new HashMap<>();
            try{
                List<String> list = conf.get().getNode("whitelist").getList(TypeToken.of(String.class));

                for (String elem : list){
                    String[] arr = elem.split(":");
                    whitelist.put(arr[0].toLowerCase(), (arr.length == 2) ? arr[1] : null);
                }
            } catch (ObjectMappingException e){
                Logger.severe("Cannot load skins whitelist: %s", e.getMessage());
            }
        }
    }

    private void loadQueues() {
        try{
            boolean enableMojang = conf.get().getNode("mojang", "enable").getBoolean();
            int imagePeriod = 6;

            if (enableMojang){
                imagePeriod = conf.get().getNode("mojang", "period").getInt();
                List<Profile> profiles = conf.get().getNode("mojang", "accounts").getList(TypeToken.of(Profile.class));
                imageQueue = new MojangQueue(this, profiles);
            } else {
                imageQueue = new MineskinQueue(this);
            }

            nameQueue = new NameQueue(this);
            nameQueue.start(1);
            imageQueue.start(imagePeriod);
        } catch (ObjectMappingException e){
            Logger.severe("Cannot load skin queue service: %s" + e.getMessage());
        }
    }

}