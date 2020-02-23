package ru.csm.bukkit.protocol.hologram;

import ru.csm.wrappers.WrapperPlayServerEntityDestroy;
import ru.csm.wrappers.WrapperPlayServerSpawnEntityLiving;
import com.comphenix.protocol.utility.MinecraftReflection;
import com.comphenix.protocol.wrappers.WrappedDataWatcher;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.Optional;
import java.util.Random;
import java.util.UUID;

public class Holo_1_14 implements Hologram {

    private int id;
    private Location location;
    private String text;

    private WrapperPlayServerSpawnEntityLiving entity;
    private WrappedDataWatcher watcher;

    public int getEntityID(){
        return id;
    }

    public Location getLocation(){
        return location;
    }

    public String getText() {
        return text;
    }

    public Holo_1_14(Location location){
        this.location = location;
        this.id = new Random().nextInt(Integer.MAX_VALUE);

        entity = new WrapperPlayServerSpawnEntityLiving();
        entity.setEntityID(id);
        entity.setUniqueId(UUID.randomUUID());

        entity.setType(1);

        entity.setX(location.getX());
        entity.setY(location.getY()-2.2);
        entity.setZ(location.getZ());
        entity.setYaw(0);
        entity.setPitch(0);

        watcher = new WrappedDataWatcher();

        WrappedDataWatcher.Serializer byteSerializer = WrappedDataWatcher.Registry.get(Byte.class);
        WrappedDataWatcher.Serializer booleanSerializer = WrappedDataWatcher.Registry.get(Boolean.class);

        watcher.setObject(new WrappedDataWatcher.WrappedDataWatcherObject(0, byteSerializer), (byte) 0x20);
        watcher.setObject(new WrappedDataWatcher.WrappedDataWatcherObject(3, booleanSerializer), true);
    }

    public void setText(String text){
        this.text = text;

        try{
            WrappedDataWatcher.Serializer chatSerializer = WrappedDataWatcher.Registry.getChatComponentSerializer(true);
            Class<?> chatComponentClass = MinecraftReflection.getChatComponentTextClass();
            Object chatComponent = chatComponentClass.getConstructor(String.class).newInstance(text);

            watcher.setObject(new WrappedDataWatcher.WrappedDataWatcherObject(2, chatSerializer), Optional.of(chatComponent));
        } catch (ReflectiveOperationException e){
            e.printStackTrace();
        }
    }

    public void show(Player player){
        entity.setMetadata(watcher);
        entity.sendPacket(player);
    }

    public void hide(Player player){
        WrapperPlayServerEntityDestroy destroy = new WrapperPlayServerEntityDestroy();
        destroy.setEntityIds(new int[]{id});
        destroy.sendPacket(player);
    }

    public void destroy(){
        WrapperPlayServerEntityDestroy destroy = new WrapperPlayServerEntityDestroy();
        destroy.setEntityIds(new int[]{id});

        for(Player player : Bukkit.getOnlinePlayers()){
            destroy.sendPacket(player);
        }
    }
}
