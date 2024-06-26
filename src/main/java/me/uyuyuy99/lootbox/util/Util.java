package me.uyuyuy99.lootbox.util;

import me.uyuyuy99.lootbox.LootBox;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.profile.PlayerProfile;

import java.net.MalformedURLException;
import java.net.URI;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

public class Util {

    public static void addGlow(ItemStack item) {
        item.addUnsafeEnchantment(Enchantment.SILK_TOUCH, 69);
        ItemMeta meta = item.getItemMeta();
        meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        item.setItemMeta(meta);
    }

    public static void setHeadTexture(ItemStack head, String url) {
        if (url.isEmpty()) {
            return;
        }

        SkullMeta headMeta = (SkullMeta) head.getItemMeta();
        PlayerProfile profile = Bukkit.createPlayerProfile(UUID.randomUUID());

        try {
            profile.getTextures().setSkin(URI.create("http://textures.minecraft.net/texture/" + url).toURL());
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }

        headMeta.setOwnerProfile(profile);
        head.setItemMeta(headMeta);
    }

    public static ItemStack getIconFromConfig(ConfigurationSection section, String key) {
        ItemStack item = new ItemStack(
                Material.valueOf(section.getString(key).toUpperCase()),
                section.getInt(key + "-amount", 1)
        );

        if (section.getBoolean(key + "-glow", false)) {
            addGlow(item);
        }

        if (item.getType() == Material.PLAYER_HEAD && section.isSet(key + "-head-url")) {
            setHeadTexture(item, section.getString(key + "-head-url"));
        }

        return item;
    }
    public static ItemStack getIconFromConfig(FileConfiguration config, String key) {
        return getIconFromConfig(config.getRoot(), key);
    }
    public static ItemStack getIconFromConfig(String key) {
        return getIconFromConfig(LootBox.plugin().getConfig(), key);
    }

    public static String getItemName(ItemStack item) {
        if (item.hasItemMeta()) {
            ItemMeta meta = item.getItemMeta();

            if (meta.hasDisplayName()) {
                return meta.getDisplayName();
            }
        }
        return item.getType().name();
    }

    public static String getReadableTimeFromTicks(int ticks) {
        long millis = ticks * 50L;

        if (millis >= TimeUnit.DAYS.toMillis(1)) {
            long days = (millis / TimeUnit.DAYS.toMillis(1));
            return days + "d";
        }
        if (millis >= TimeUnit.HOURS.toMillis(1)) {
            long hours = (millis / TimeUnit.HOURS.toMillis(1));
            return hours + "h";
        }
        else if (millis >= TimeUnit.MINUTES.toMillis(1)) {
            long minutes = (millis / TimeUnit.MINUTES.toMillis(1));
            return minutes + "m";
        }
        else {
            long seconds = (millis / TimeUnit.SECONDS.toMillis(1));
            return seconds + "s";
        }
    }

}
