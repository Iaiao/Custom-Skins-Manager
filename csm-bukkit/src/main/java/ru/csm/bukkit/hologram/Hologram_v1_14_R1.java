package ru.csm.bukkit.hologram;

import net.minecraft.server.v1_14_R1.*;
import org.bukkit.craftbukkit.v1_14_R1.CraftWorld;
import org.bukkit.craftbukkit.v1_14_R1.entity.CraftPlayer;
import org.bukkit.entity.Player;

public class Hologram_v1_14_R1 extends AbstractHologram {

    @Override
    public void spawn(Player p) {
        EntityPlayer player = ((CraftPlayer)p).getHandle();
        World world = ((CraftWorld)getLocation().getWorld()).getHandle();

        for (HoloLine line : getLines()){
            EntityLiving armorStand = new EntityArmorStand(world,
                    line.getLocation().getX(),
                    line.getLocation().getY(),
                    line.getLocation().getZ());

            armorStand.setCustomName(new ChatComponentText(line.getText()));
            armorStand.setCustomNameVisible(true);
            armorStand.setInvisible(true);
            armorStand.setInvulnerable(true);

            player.playerConnection.sendPacket(new PacketPlayOutSpawnEntityLiving(armorStand));
            line.setId(armorStand.getId());
        }
    }

    @Override
    public void destroy(Player player) {
        int[] ids = new int[getLines().size()];
        int index = 0;

        for (HoloLine line : getLines()){
            ids[index] = line.getId();
            index++;
        }

        PlayerConnection connection = ((CraftPlayer)player).getHandle().playerConnection;

        connection.sendPacket(new PacketPlayOutEntityDestroy(ids));
    }
}
