package me.uyuyuy99.lootbox.crate;

import lombok.Data;
import org.bukkit.configuration.serialization.ConfigurationSerializable;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;

import java.util.HashMap;
import java.util.Map;

@Data
public class CrateItem implements ConfigurationSerializable {

    // 1 of these should be null, the other 1 should be non-null
    private ItemStack item;
    private PotionEffect effect;

    private int rarity;

    public boolean isItem() {
        return item != null;
    }

    @Override
    public Map<String, Object> serialize() {
        Map<String, Object> data = new HashMap<>();

        if (item != null) {
            data.put("item", item);
        } else {
            data.put("effect", effect);
        }
        data.put("rarity", rarity);

        return data;
    }

    public static CrateItem deserialize(Map<String, Object> data) {
        CrateItem crateItem = new CrateItem();

        if (data.containsKey("item")) {
            crateItem.setItem((ItemStack) data.get("item"));
        } else {
            crateItem.setEffect((PotionEffect) data.get("effect"));
        }
        crateItem.setRarity((int) data.get("rarity"));

        return crateItem;
    }

}
