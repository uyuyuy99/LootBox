package me.uyuyuy99.lootbox.crate;

import lombok.Data;
import org.bukkit.configuration.serialization.ConfigurationSerializable;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;

@Data
public class CrateItem implements ConfigurationSerializable {

    private ItemStack item;
    private int rarity;

    @NotNull
    @Override
    public Map<String, Object> serialize() {
        Map<String, Object> data = new HashMap<>();

        data.put("item", item);
        data.put("rarity", rarity);

        return data;
    }

    public static CrateItem deserialize(Map<String, Object> data) {
        CrateItem crateItem = new CrateItem();

        crateItem.setItem((ItemStack) data.get("item"));
        crateItem.setRarity((int) data.get("rarity"));

        return crateItem;
    }

}
