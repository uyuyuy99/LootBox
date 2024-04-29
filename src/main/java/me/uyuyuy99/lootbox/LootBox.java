package me.uyuyuy99.lootbox;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.wrappers.*;
import dev.jorel.commandapi.CommandAPI;
import dev.jorel.commandapi.CommandAPIBukkitConfig;
import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.arguments.IntegerArgument;
import dev.jorel.commandapi.arguments.StringArgument;
import me.uyuyuy99.lootbox.crate.Crate;
import me.uyuyuy99.lootbox.crate.CrateItem;
import me.uyuyuy99.lootbox.crate.CrateManager;
import me.uyuyuy99.lootbox.crate.CrateType;
import me.uyuyuy99.lootbox.util.CC;
import me.uyuyuy99.lootbox.util.Util;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.configuration.serialization.ConfigurationSerialization;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;

public final class LootBox extends JavaPlugin {

    private static LootBox PLUGIN;

    private CrateManager crates;

    static {
        ConfigurationSerialization.registerClass(CrateItem.class);
        ConfigurationSerialization.registerClass(Crate.class);
    }

    @Override
    public void onLoad() {
        CommandAPI.onLoad(new CommandAPIBukkitConfig(this));
    }

    @Override
    public void onEnable() {
        PLUGIN = this;

        crates = new CrateManager(this);
        crates.load();

        final UUID testUUID = UUID.randomUUID();

        new CommandAPICommand("lootbox")
                .withSubcommand(new CommandAPICommand("addtype")
                        .withArguments(new StringArgument("type"))
                        .withOptionalArguments(new StringArgument("headUrl"))
                        .withPermission("lootbox.admin.addtype")
                        .executes((sender, args) -> {
                            String typeId = (String) args.get("type");
                            CrateType crateType = new CrateType(typeId);
                            Optional<Object> optHeadUrl = args.getOptional("headUrl");

                            if (optHeadUrl.isPresent()) {
                                String headUrl = (String) optHeadUrl.get();
                                crateType.setHeadUrl(headUrl);
                            }

                            if (crates.addType(crateType)) {
                                sender.sendMessage(CC.GREEN + "Added crate type " + CC.GRAY + typeId + CC.GREEN + "!");
                            } else {
                                sender.sendMessage(CC.RED + "Crate type " + CC.GRAY + typeId + CC.RED + " already exists!");
                            }
                        })
                )
                .withSubcommand(new CommandAPICommand("sethead")
                        .withArguments(crates.cmdArgType("type"))
                        .withArguments(new StringArgument("headUrl"))
                        .withPermission("lootbox.admin.sethead")
                        .executes((sender, args) -> {
                            CrateType type = (CrateType) args.get("type");
                            String headUrl = (String) args.get("headUrl");

                            type.setHeadUrl(headUrl);
                            crates.save();

                            // Resend the packets to change the loot box entitys' heads
                            for (Crate crate : crates.getActiveCrates(type)) {
                                for (Player p : crate.getLocation().getWorld().getPlayers()) {
                                    crate.sendEquipPacket(p);
                                }
                            }
                        })
                )
                .withSubcommand(new CommandAPICommand("additem")
                        .withArguments(crates.cmdArgType("type"))
                        .withArguments(new IntegerArgument("rarity", 1, 1000))
                        .withPermission("lootbox.admin.additem")
                        .executesPlayer((player, args) -> {
                            CrateType type = (CrateType) args.get("type");
                            Integer rarity = (Integer) args.get("rarity");
                            ItemStack item = player.getInventory().getItemInMainHand();

                            type.addItem(item, rarity);
                            crates.save();

                            player.sendMessage(CC.GREEN + "Added " + CC.DARK_GRAY + item.getAmount() + "x "
                                    + CC.GRAY + Util.getItemName(item) + CC.GREEN + " w/ rarity " + CC.GRAY + rarity
                                    + CC.GREEN + " to crate type " + CC.GRAY + type.getId() + CC.GREEN + "!");
                        })
                )
                .withSubcommand(new CommandAPICommand("addlocation")
                        .withArguments(crates.cmdArgType("type"))
                        .withPermission("lootbox.admin.addlocation")
                        .executesPlayer((player, args) -> {
                            CrateType type = (CrateType) args.get("type");
                            Location loc = player.getLocation();

                            crates.addCrate(new Crate(type, loc));

                            player.sendMessage(CC.GREEN + "Added a " + CC.GRAY + type.getId() + CC.GREEN + " crate at "
                                    + CC.GRAY + "(" + loc.getBlockX() + ", " + loc.getBlockY() + ", " + loc.getBlockZ() + ")"
                                    + CC.GREEN + " in world " + CC.GRAY + loc.getWorld().getName());
                        })
                )
                .register();

        new CommandAPICommand("envoy")
                .withSubcommand(new CommandAPICommand("start")
                        .withPermission("lootbox.envoy.start")
                        .executes((sender, args) -> {
                            crates.startEnvoy();
                            sender.sendMessage(CC.GREEN + "LootBox envoy started!");
                        })
                )
                .withSubcommand(new CommandAPICommand("stop")
                        .withPermission("lootbox.envoy.stop")
                        .executes((sender, args) -> {
                            crates.stopEnvoy();
                            sender.sendMessage(CC.RED + "LootBox envoy stopped.");
                        })
                )
                .register();
    }

    @Override
    public void onDisable() {
//        crates.save();
    }

    public static LootBox plugin() {
        return PLUGIN;
    }

    public CrateManager crates() {
        return crates;
    }

}
