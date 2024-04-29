package me.uyuyuy99.lootbox.crate;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.wrappers.*;
import me.uyuyuy99.lootbox.LootBox;
import me.uyuyuy99.lootbox.util.Util;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.configuration.serialization.ConfigurationSerializable;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public class Crate implements ConfigurationSerializable {

    public static int NEXT_ENTITY_ID = Integer.MAX_VALUE / 2;
    private ProtocolManager protocol = ProtocolLibrary.getProtocolManager();

    private CrateType type;
    private Location location;
    private boolean active = false;

    // For the fake entity
    private int entityId;
    private UUID entityUuid;

    // Use this to load from config
    public Crate() {
        // Head rotation on fake entities
        new BukkitRunnable() {
            int angle = 0;

            @Override
            public void run() {
                if (active) {
                    PacketContainer packetRotate = protocol.createPacket(PacketType.Play.Server.ENTITY_HEAD_ROTATION);

                    packetRotate.getIntegers().write(0, entityId);
                    packetRotate.getBytes().write(0, (byte) (angle++ % 256));

                    for (Player p : location.getWorld().getPlayers()) {
                        protocol.sendServerPacket(p, packetRotate);
                    }
                }
            }
        }.runTaskTimer(LootBox.plugin(), 1, 1);

        // If player interacts w/ fake entity, give him loot
        protocol.addPacketListener(new PacketAdapter(LootBox.plugin(),
                ListenerPriority.HIGH,
                PacketType.Play.Client.USE_ENTITY) {
            @Override
            public void onPacketReceiving(PacketEvent event) {
                if (!active) return;

                PacketContainer packet = event.getPacket();

                if (packet.getIntegers().read(0) == entityId) {
                    plugin.getServer().getScheduler().scheduleSyncDelayedTask(plugin, () -> {
                        location.getWorld().dropItem(location.clone().add(0, 1.5, 0), type.getRandomItem());
                    });
                    deactivate();
                }
            }
        });
    }

    // Use this to create an entirely new Crate
    public Crate(CrateType type, Location location) {
        this();
        this.type = type;
        this.location = location;
        this.entityId = NEXT_ENTITY_ID++;
        this.entityUuid = UUID.randomUUID();
    }

    public void activate() {
        this.active = true;

        for (Player p : location.getWorld().getPlayers()) {
            sendPackets(p);
        }
    }

    public void deactivate() {
        this.active = false;

        for (Player p : location.getWorld().getPlayers()) {
            PacketContainer packetDespawn = protocol.createPacket(PacketType.Play.Server.ENTITY_DESTROY);
            packetDespawn.getIntLists().write(0, Collections.singletonList(entityId));
            protocol.sendServerPacket(p, packetDespawn);
        }
    }

    // Sends all the necessary packets to initialize the fake entity
    public void sendPackets(Player player) {
        sendSpawnPacket(player);
        sendEquipPacket(player);
        sendInvisPacket(player);
    }

    // Sends the pcaket to spawn the fake entity
    public void sendSpawnPacket(Player player) {
        PacketContainer packetSpawn = protocol.createPacket(PacketType.Play.Server.SPAWN_ENTITY);

        packetSpawn.getIntegers().write(0, entityId);
        packetSpawn.getUUIDs().write(0, entityUuid);
        packetSpawn.getEntityTypeModifier().write(0, EntityType.ZOMBIE);
        packetSpawn.getDoubles().write(0, location.getX());
        packetSpawn.getDoubles().write(1, location.getY());
        packetSpawn.getDoubles().write(2, location.getZ());

        protocol.sendServerPacket(player, packetSpawn);
    }

    // Sends the packet to have the fake entity equip a player head
    public void sendEquipPacket(Player player) {
        PacketContainer packetEquip = protocol.createPacket(PacketType.Play.Server.ENTITY_EQUIPMENT);

        ItemStack headItem = new ItemStack(Material.PLAYER_HEAD, 1);
        Util.setHeadTexture(headItem, type.getHeadUrl());

        packetEquip.getIntegers().write(0, entityId);
        packetEquip.getSlotStackPairLists().write(0, Collections.singletonList(new Pair<>(EnumWrappers.ItemSlot.HEAD, headItem)));

        protocol.sendServerPacket(player, packetEquip);
    }

    // Sends the packet to make the fake entity invisible, and to name it
    public void sendInvisPacket(Player player) {
        PacketContainer packetInvis = protocol.createPacket(PacketType.Play.Server.ENTITY_METADATA);

        List<WrappedDataValue> dataVals = new ArrayList<>();
        dataVals.add(new WrappedDataValue(0, WrappedDataWatcher.Registry.get(Byte.class), (byte) 0x20));
        dataVals.add(new WrappedDataValue(2,
                WrappedDataWatcher.Registry.getChatComponentSerializer(true),
                Optional.of(WrappedChatComponent.fromText("Loot Box").getHandle())));
        dataVals.add(new WrappedDataValue(10, WrappedDataWatcher.Registry.get(Boolean.class), false));
        packetInvis.getIntegers().write(0, entityId);
        packetInvis.getDataValueCollectionModifier().write(0, dataVals);

        protocol.sendServerPacket(player, packetInvis);
    }

    public CrateType getType() {
        return type;
    }

    public Location getLocation() {
        return location;
    }

    public boolean isActive() {
        return active;
    }

    public void setType(CrateType type) {
        this.type = type;
    }

    public void setLocation(Location location) {
        this.location = location;
    }

    public void setEntityId(int entityId) {
        this.entityId = entityId;
    }

    public void setEntityUuid(UUID entityUuid) {
        this.entityUuid = entityUuid;
    }

    @NotNull
    @Override
    public Map<String, Object> serialize() {
        Map<String, Object> data = new HashMap<>();

        data.put("type", type.getId());
        data.put("location", location);
        data.put("entityId", entityId);
        data.put("entityUuid", entityUuid.toString());

        return data;
    }

    public static Crate deserialize(Map<String, Object> data) {
        Crate crate = new Crate();

        crate.setType(LootBox.plugin().crates().getType((String) data.get("type")));
        crate.setLocation((Location) data.get("location"));
        crate.setEntityId((int) data.get("entityId"));
        crate.setEntityUuid(UUID.fromString((String) data.get("entityUuid")));

        return crate;
    }

}
