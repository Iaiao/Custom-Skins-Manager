/*
 * Custom Skins Manager
 * Copyright (C) 2020  Nanit
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package ru.csm.bukkit.nms.handler;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import com.mojang.authlib.properties.PropertyMap;
import net.minecraft.server.v1_10_R1.*;
import org.bukkit.Bukkit;
import org.bukkit.craftbukkit.v1_10_R1.entity.CraftPlayer;
import org.bukkit.entity.Player;
import ru.csm.api.player.Skin;
import ru.csm.bukkit.util.BukkitTasks;

import java.util.Collections;
import java.util.Iterator;

public final class Handler_v1_10_R1 implements SkinHandler {

    @Override
    public Skin getSkin(Player player) {
        GameProfile profile = ((CraftPlayer)player).getProfile();
        Iterator<Property> iterator = profile.getProperties().get("textures").iterator();

        if (iterator.hasNext()){
            Property property = iterator.next();
            return new Skin(property.getValue(), property.getSignature());
        }

        return null;
    }

    @Override
    public void applySkin(Player player, Skin skin) {
        PropertyMap propertyMap = ((CraftPlayer)player).getProfile().getProperties();
        propertyMap.removeAll("textures");
        propertyMap.put("textures", new Property("textures", skin.getValue(), skin.getSignature()));
    }

    @Override
    public void updateSkin(Player player) {
        CraftPlayer cp = (CraftPlayer) player;
        EntityPlayer ep = cp.getHandle();

        PacketPlayOutPlayerInfo removeInfo = new PacketPlayOutPlayerInfo(
                PacketPlayOutPlayerInfo.EnumPlayerInfoAction.REMOVE_PLAYER, ep);
        PacketPlayOutPlayerInfo addInfo = new PacketPlayOutPlayerInfo(
                PacketPlayOutPlayerInfo.EnumPlayerInfoAction.ADD_PLAYER, ep);

        WorldServer worldServer = (WorldServer)ep.getWorld();
        int dimension = worldServer.worldProvider.getDimensionManager().getDimensionID();
        EnumDifficulty difficulty = EnumDifficulty.getById(cp.getWorld().getDifficulty().getValue());
        WorldType worldType = worldServer.getWorldData().getType();

        PacketPlayOutRespawn respawn = new PacketPlayOutRespawn(dimension, difficulty, worldType, ep.playerInteractManager.getGameMode());
        PacketPlayOutPosition position = new PacketPlayOutPosition(
                player.getLocation().getX(),
                player.getLocation().getY(),
                player.getLocation().getZ(),
                player.getLocation().getYaw(),
                player.getLocation().getPitch(),
                Collections.emptySet(),
                0
        );
        PacketPlayOutHeldItemSlot slot = new PacketPlayOutHeldItemSlot(player.getInventory().getHeldItemSlot());

        BukkitTasks.runTask(()->{
            for (Player p : Bukkit.getOnlinePlayers()) {
                p.hidePlayer(player);
                p.showPlayer(player);
            }

            ep.playerConnection.sendPacket(removeInfo);
            ep.playerConnection.sendPacket(addInfo);
            ep.playerConnection.sendPacket(respawn);
            ep.playerConnection.sendPacket(position);
            ep.playerConnection.sendPacket(slot);

            ep.updateAbilities();
            cp.updateScaledHealth();
            ep.triggerHealthUpdate();
            player.updateInventory();
            player.recalculatePermissions();
            player.setFlying(player.isFlying());
        });
    }
}
