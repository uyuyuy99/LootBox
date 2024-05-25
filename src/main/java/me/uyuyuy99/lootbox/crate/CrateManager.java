package me.uyuyuy99.lootbox.crate;

import dev.jorel.commandapi.arguments.Argument;
import dev.jorel.commandapi.arguments.CustomArgument;
import dev.jorel.commandapi.arguments.StringArgument;
import me.uyuyuy99.lootbox.LootBox;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.stream.Collectors;

public class CrateManager {

    private LootBox plugin;

    private File cratesConfigFile;
    private File typesConfigFile;

    private List<Crate> crateList = new ArrayList<>();
    private List<CrateType> typeList = new ArrayList<>();

    private EnvoyRunnable envoyTask;

    public CrateManager(LootBox plugin) {
        this.plugin = plugin;
        cratesConfigFile = new File(plugin.getDataFolder(), "crates.yml");
        typesConfigFile = new File(plugin.getDataFolder(), "types.yml");
        plugin.getServer().getPluginManager().registerEvents(new Listeners(), plugin);
    }

    public void addCrate(Crate crate) {
        crateList.add(crate);
        save();
    }

    // Returns the CrateType with the specified ID, null if none
    public CrateType getType(String id) {
        for (CrateType type : typeList) {
            if (type.getId().equals(id)) {
                return type;
            }
        }
        return null;
    }

    // Returns true if successful, false if crate type w/ that ID already exists
    public boolean addType(CrateType crateType) {
        if (getType(crateType.getId()) != null) {
            return false;
        }

        typeList.add(crateType);
        save();

        return true;
    }

    // Gets all crates of a certain type
    public List<Crate> getCrates(CrateType crateType) {
        return crateList.stream()
                .filter(c -> c.getType().equals(crateType))
                .collect(Collectors.toList());
    }

    public List<Crate> getActiveCrates() {
        return crateList.stream()
                .filter(Crate::isActive)
                .collect(Collectors.toList());
    }

    // Gets all active crates of a certain type
    public List<Crate> getActiveCrates(CrateType crateType) {
        return getCrates(crateType).stream()
                .filter(Crate::isActive)
                .collect(Collectors.toList());
    }

    public boolean isEnvoyRunning() {
        return envoyTask != null;
    }

    public int getEnvoyTimeLeft() {
        return (envoyTask != null) ? envoyTask.getTicksLeft() : 0;
    }

    public void startEnvoy() {
        if (envoyTask != null) {
            envoyTask.cancel();
        }
        envoyTask = new EnvoyRunnable(20 * plugin.getConfig().getInt("options.timer"));
        envoyTask.runTaskTimer(plugin, 0, 1);

        for (Crate crate : crateList) {
            crate.activate();
        }
    }

    public void stopEnvoy() {
        if (envoyTask != null) {
            envoyTask.cancel();
            envoyTask = null;
        }
        for (Crate crate : crateList) {
            crate.deactivate();
        }
    }

    // Load crate types & crates from config files
    public void load() {
        loadTypes();
        loadCrates();
    }

    private void loadTypes() {
        typeList.clear();

        // Save default yml file
        if (!typesConfigFile.exists()) {
            plugin.saveResource(typesConfigFile.getName(), false);
        }

        // Reload config
        FileConfiguration typesConfig = YamlConfiguration.loadConfiguration(typesConfigFile);

        // Find defaults in the jar
        Reader defConfigStream1 = new InputStreamReader(plugin.getResource(typesConfigFile.getName()), StandardCharsets.UTF_8);
        if (defConfigStream1 != null) {
            YamlConfiguration defConfig = YamlConfiguration.loadConfiguration(defConfigStream1);
            typesConfig.setDefaults(defConfig);
        }

        // Load the crate types
        for (String key : typesConfig.getKeys(false)) {
            ConfigurationSection section = typesConfig.getConfigurationSection(key);
            CrateType crateType = new CrateType(key);

            List<CrateItem> items = new ArrayList<>();
            for (Object o : section.getList("items")) {
                items.add((CrateItem) o);
            }
            crateType.setItems(items);
            crateType.setHeadUrl(section.getString("head-url"));

            typeList.add(crateType);
        }
    }

    private void loadCrates() {
        crateList.clear();

        // Save default yml file
        if (!cratesConfigFile.exists()) {
            plugin.saveResource(cratesConfigFile.getName(), false);
        }

        // Reload config
        FileConfiguration cratesConfig = YamlConfiguration.loadConfiguration(cratesConfigFile);

        // Looks for defaults in the jar
        Reader defConfigStream2 = new InputStreamReader(plugin.getResource(cratesConfigFile.getName()), StandardCharsets.UTF_8);
        if (defConfigStream2 != null) {
            YamlConfiguration defConfig = YamlConfiguration.loadConfiguration(defConfigStream2);
            cratesConfig.setDefaults(defConfig);
        }

        // Load the crates
        if (cratesConfig.isSet("crates")) {
            for (Object o : cratesConfig.getList("crates")) {
                crateList.add((Crate) o);
            }
        }
        if (cratesConfig.isSet("next-entity-id")) {
            Crate.NEXT_ENTITY_ID = cratesConfig.getInt("next-entity-id");
        }
    }

    // Saves all crate & type data to config files
    public void save() {
        FileConfiguration typesConfig = new YamlConfiguration();
        FileConfiguration cratesConfig = new YamlConfiguration();

        for (CrateType type : typeList) {
            typesConfig.set(type.getId() + ".items", type.getItems());
            typesConfig.set(type.getId() + ".head-url", type.getHeadUrl());
        }

        cratesConfig.set("crates", crateList);
        cratesConfig.set("next-entity-id", Crate.NEXT_ENTITY_ID);

        try {
            typesConfig.save(typesConfigFile);
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Could not save config to " + typesConfigFile.getName(), e);
        }

        try {
            cratesConfig.save(cratesConfigFile);
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Could not save config to " + cratesConfigFile.getName(), e);
        }
    }

    private class Listeners implements Listener {

        private void refreshCrates(Player player) {
            if (player != null && player.isOnline()) {
                for (Crate crate : getActiveCrates()) {
                    if (crate.getLocation().getWorld().equals(player.getWorld())) {
                        crate.sendPackets(player);
                    }
                }
            }
        }

        @EventHandler
        public void onJoin(PlayerJoinEvent event) {
            final Player player = event.getPlayer();
            plugin.getServer().getScheduler().scheduleSyncDelayedTask(plugin, () -> refreshCrates(player), 1);
        }

        @EventHandler
        public void onWorldChange(PlayerChangedWorldEvent event) {
            final Player player = event.getPlayer();
            plugin.getServer().getScheduler().scheduleSyncDelayedTask(plugin, () -> refreshCrates(player), 1);
        }

        @EventHandler
        public void onRespawn(PlayerRespawnEvent event) {
            final Player player = event.getPlayer();
            plugin.getServer().getScheduler().scheduleSyncDelayedTask(plugin, () -> refreshCrates(player), 1);
        }

    }

    private class EnvoyRunnable extends BukkitRunnable {

        int ticks;

        public EnvoyRunnable(int ticks) {
            this.ticks = ticks;
        }

        public int getTicksLeft() {
            return ticks;
        }

        @Override
        public void run() {
            if (ticks-- == 0) {
                stopEnvoy();
                cancel();
            }
        }

    }

    // Autofill suggestions for commands (lists the available crate types)
    public Argument cmdArgType(String nodeName) {
        return new CustomArgument<CrateType>(nodeName, info -> {
            CrateType crateType = getType(info);

            if (crateType == null) {
                throw new CustomArgument.CustomArgumentException("Unknown crate type: " + info);
            } else {
                return crateType;
            }
        });
    }

}
