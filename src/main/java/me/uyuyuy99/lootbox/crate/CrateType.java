package me.uyuyuy99.lootbox.crate;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class CrateType {

    private String id;
    private List<CrateItem> items = new ArrayList<>();
    private String headUrl;

    public CrateType(String id, String headUrl) {
        this.id = id;
        this.headUrl = headUrl;
    }
    public CrateType(String id) {
        this(id, "9d3d250e25bbca3a62be5b3ef02cfcab6dcdc424884c9a7d5cc95c9d0");
    }

    // Gets a random item from this crate, based on the rarities of the items
    public ItemStack getRandomItem() {
        List<Integer> chances = new ArrayList<>();
        int sum = 0;

        for (CrateItem item : items) {
            sum += item.getRarity();
            chances.add(sum);
        }

        int choice = new Random().nextInt(sum);
        for (int i=0; i<chances.size(); i++) {
            if (choice < chances.get(i)) {
                return items.get(i).getItem();
            }
        }
        return new ItemStack(Material.AIR); // this shouldn't ever happen
    }

    public String getId() {
        return id;
    }

    public List<CrateItem> getItems() {
        return items;
    }

    public String getHeadUrl() {
        return headUrl;
    }

    public void setItems(List<CrateItem> items) {
        this.items = items;
    }

    public void setHeadUrl(String headUrl) {
        this.headUrl = headUrl;
    }

    public void addItem(ItemStack item, int rarity) {
        CrateItem crateItem = new CrateItem();
        crateItem.setItem(item);
        crateItem.setRarity(rarity);
        items.add(crateItem);
    }

}
