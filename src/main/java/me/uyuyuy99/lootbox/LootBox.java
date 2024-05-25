package me.uyuyuy99.lootbox;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.wrappers.EnumWrappers;
import dev.jorel.commandapi.CommandAPI;
import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.CommandAPIConfig;
import dev.jorel.commandapi.arguments.IntegerArgument;
import dev.jorel.commandapi.arguments.PotionEffectArgument;
import dev.jorel.commandapi.arguments.StringArgument;
import dev.jorel.commandapi.arguments.TimeArgument;
import me.uyuyuy99.lootbox.crate.Crate;
import me.uyuyuy99.lootbox.crate.CrateItem;
import me.uyuyuy99.lootbox.crate.CrateManager;
import me.uyuyuy99.lootbox.crate.CrateType;
import me.uyuyuy99.lootbox.util.CC;
import me.uyuyuy99.lootbox.util.Util;
import net.minecraft.server.v1_15_R1.EntityTypes;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.serialization.ConfigurationSerialization;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.Optional;
import java.util.UUID;

public final class LootBox extends JavaPlugin {

    private static LootBox PLUGIN;

    private CrateManager crates;

    static {
        ConfigurationSerialization.registerClass(CrateItem.class);
        ConfigurationSerialization.registerClass(Crate.class);
    }

    @Override
    public void onLoad() {
        CommandAPI.onLoad(new CommandAPIConfig());
    }

    @Override
    public void onEnable() {
        PLUGIN = this;

        crates = new CrateManager(this);
        crates.load();

        saveDefaultConfig();

        new CommandAPICommand("lb")
                .withSubcommand(new CommandAPICommand("reload")
                        .withPermission("lootbox.admin.reload")
                        .executes((sender, args) -> {
                            crates.stopEnvoy();
                            crates.load();
                            reloadConfig();

                            sender.sendMessage(CC.GREEN + "LootBox config files were reloaded!");
                        })
                )
                .withSubcommand(new CommandAPICommand("addtype")
                        .withArguments(new StringArgument("type"))
                        .withPermission("lootbox.admin.addtype")
                        .executes(this::runAddTypeCommand)
                )
                .withSubcommand(new CommandAPICommand("addtype")
                        .withArguments(new StringArgument("type"))
                        .withArguments(new StringArgument("headUrl"))
                        .withPermission("lootbox.admin.addtype")
                        .executes(this::runAddTypeCommand)
                )
                .withSubcommand(new CommandAPICommand("sethead")
                        .withArguments(crates.cmdArgType("type"))
                        .withArguments(new StringArgument("headUrl"))
                        .withPermission("lootbox.admin.sethead")
                        .executes((sender, args) -> {
                            CrateType type = (CrateType) args[0];
                            String headUrl = (String) args[1];

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
                            CrateType type = (CrateType) args[0];
                            int rarity = (int) args[1];
                            ItemStack item = player.getInventory().getItemInMainHand();

                            type.addItem(item, rarity);
                            crates.save();

                            player.sendMessage(CC.GREEN + "Added " + CC.DARK_GRAY + item.getAmount() + "x "
                                    + CC.GRAY + Util.getItemName(item) + CC.GREEN + " w/ rarity " + CC.GRAY + rarity
                                    + CC.GREEN + " to crate type " + CC.GRAY + type.getId() + CC.GREEN + "!");
                        })
                )
                .withSubcommand(new CommandAPICommand("addeffect")
                        .withArguments(crates.cmdArgType("type"))
                        .withArguments(new IntegerArgument("rarity", 1, 1000))
                        .withArguments(new PotionEffectArgument("potion"))
                        .withArguments(new TimeArgument("duration"))
                        .withArguments(new IntegerArgument("amplifier"))
                        .withPermission("lootbox.admin.addeffect")
                        .executesPlayer((player, args) -> {
                            CrateType type = (CrateType) args[0];
                            int rarity = (int) args[1];
                            PotionEffectType effect = (PotionEffectType) args[2];
                            int duration = (int) args[3];
                            int amplifier = (int) args[4];

                            type.addEffect(new PotionEffect(effect, duration, amplifier), rarity);
                            crates.save();

                            player.sendMessage(CC.GREEN + "Added potion effect " + CC.GRAY + effect.getName()
                                    + CC.GREEN + " w/ duration " + CC.GRAY + duration + CC.GREEN + " and amplifer "
                                    + CC.GRAY + amplifier + CC.GREEN + " to crate type " + CC.GRAY + type.getId()
                                    + CC.GREEN + "!");
                        })
                )
                .withSubcommand(new CommandAPICommand("addlocation")
                        .withArguments(crates.cmdArgType("type"))
                        .withPermission("lootbox.admin.addlocation")
                        .executesPlayer((player, args) -> {
                            CrateType type = (CrateType) args[0];
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
                            sender.sendMessage(CC.format(getConfig().getString("messages.envoy-start")));
                        })
                )
                .withSubcommand(new CommandAPICommand("stop")
                        .withPermission("lootbox.envoy.stop")
                        .executes((sender, args) -> {
                            crates.stopEnvoy();
                            sender.sendMessage(CC.format(getConfig().getString("messages.envoy-stop")));
                        })
                )
                .withSubcommand(new CommandAPICommand("timer")
                        .withPermission("lootbox.envoy.timer")
                        .executes((sender, args) -> {
                            if (crates.isEnvoyRunning()) {
                                sender.sendMessage(CC.format(getConfig().getString("messages.envoy-time-left")
                                        .replace("%time%", Util.getReadableTimeFromTicks(crates.getEnvoyTimeLeft()))));
                            } else {
                                sender.sendMessage(CC.format(getConfig().getString("messages.no-envoy")));
                            }
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

    private void runAddTypeCommand(CommandSender sender, Object[] args) {
        String typeId = (String) args[0];

        if (!typeId.matches("^[a-zA-Z0-9\\-]*$")) {
            sender.sendMessage(CC.RED + "The type identifier must use only letters, numbers, or hypens (-)");
            return;
        }

        CrateType crateType = new CrateType(typeId);

        if (args.length > 1) {
            String headUrl = (String) args[1];
            crateType.setHeadUrl(headUrl);
        }

        if (crates.addType(crateType)) {
            sender.sendMessage(CC.GREEN + "Added crate type " + CC.GRAY + typeId + CC.GREEN + "!");
        } else {
            sender.sendMessage(CC.RED + "Crate type " + CC.GRAY + typeId + CC.RED + " already exists!");
        }
    }

}
