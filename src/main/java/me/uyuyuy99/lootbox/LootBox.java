package me.uyuyuy99.lootbox;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.wrappers.*;
import dev.jorel.commandapi.CommandAPI;
import dev.jorel.commandapi.CommandAPIBukkitConfig;
import dev.jorel.commandapi.CommandAPICommand;
import me.uyuyuy99.lootbox.packetwrapper.WrapperPlayServerEntityEffect;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.EntityType;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.File;
import java.util.*;

public final class LootBox extends JavaPlugin {

    private static LootBox PLUGIN;
    public static File PLAYER_FOLDER;

    @Override
    public void onLoad() {
        CommandAPI.onLoad(new CommandAPIBukkitConfig(this));
    }

    @Override
    public void onEnable() {
//        NPCLib.getInstance().registerPlugin(this);

        final UUID testUUID = UUID.randomUUID();

        new CommandAPICommand("lootbox")
                .executesPlayer((player, args) -> {
//                    NPC npc = CitizensAPI.getNPCRegistry().createNPC(EntityType.ZOMBIE, "");
//                    npc.spawn(player.getLocation());
//
//                    if (npc.getEntity().getType() == EntityType.ZOMBIE) {
//                        Zombie zombie = (Zombie) npc.getEntity();
//                        ItemStack headItem = new ItemStack(Material.PLAYER_HEAD, 1);
//                        Util.setHeadTexture(headItem, "9d3d250e25bbca3a62be5b3ef02cfcab6dcdc424884c9a7d5cc95c9d0");
//                        zombie.getEquipment().setHelmet(headItem);
//                    }

                    PacketContainer packetSpawn = ProtocolLibrary.getProtocolManager().createPacket(PacketType.Play.Server.SPAWN_ENTITY);
                    PacketContainer packetEquip = ProtocolLibrary.getProtocolManager().createPacket(PacketType.Play.Server.ENTITY_EQUIPMENT);
                    PacketContainer packetInvis = ProtocolLibrary.getProtocolManager().createPacket(PacketType.Play.Server.ENTITY_METADATA);

                    packetSpawn.getIntegers().write(0, 123456);
                    packetSpawn.getUUIDs().write(0, testUUID);
                    packetSpawn.getEntityTypeModifier().write(0, EntityType.ZOMBIE);
                    packetSpawn.getDoubles().write(0, player.getLocation().getX());
                    packetSpawn.getDoubles().write(1, player.getLocation().getY());
                    packetSpawn.getDoubles().write(2, player.getLocation().getZ());

                    ItemStack headItem = new ItemStack(Material.PLAYER_HEAD, 1);
                    Util.setHeadTexture(headItem, "9d3d250e25bbca3a62be5b3ef02cfcab6dcdc424884c9a7d5cc95c9d0");

                    packetEquip.getIntegers().write(0, 123456);
                    packetEquip.getSlotStackPairLists().write(0, Collections.singletonList(new Pair<>(EnumWrappers.ItemSlot.HEAD, headItem)));

                    List<WrappedDataValue> dataVals = new ArrayList<>();
                    dataVals.add(new WrappedDataValue(0, WrappedDataWatcher.Registry.get(Byte.class), (byte) 0x20));
                    dataVals.add(new WrappedDataValue(2,
                            WrappedDataWatcher.Registry.getChatComponentSerializer(true),
                            Optional.of(WrappedChatComponent.fromText("Loot Box").getHandle())));
                    dataVals.add(new WrappedDataValue(10, WrappedDataWatcher.Registry.get(Boolean.class), false));
                    packetInvis.getIntegers().write(0, 123456);
                    packetInvis.getDataValueCollectionModifier().write(0, dataVals);

//                    packetInvis.getIntegers().write(0, 123456);
//                    packetInvis.getEffectTypes().write(0, PotionEffectType.INVISIBILITY);
//                    packetInvis.getBytes().write(0, (byte) 1);
//                    packetInvis.getIntegers().write(1, 2000);
//                    packetInvis.getBytes().write(1, (byte) 0);

                    ProtocolLibrary.getProtocolManager().sendServerPacket(player, packetSpawn);
                    ProtocolLibrary.getProtocolManager().sendServerPacket(player, packetEquip);
                    ProtocolLibrary.getProtocolManager().sendServerPacket(player, packetInvis);

                    new BukkitRunnable() {
                        int angle = 0;

                        @Override
                        public void run() {
                            PacketContainer packetRotate = ProtocolLibrary.getProtocolManager().createPacket(PacketType.Play.Server.ENTITY_HEAD_ROTATION);

                            packetRotate.getIntegers().write(0, 123456);
                            packetRotate.getBytes().write(0, (byte) (angle++ % 256));

                            ProtocolLibrary.getProtocolManager().sendServerPacket(player, packetRotate);
                        }
                    }.runTaskTimer(this, 0, 1);
                })
                .register();
    }

    @Override
    public void onDisable() {
    }

    public static LootBox plugin() {
        return PLUGIN;
    }

}
