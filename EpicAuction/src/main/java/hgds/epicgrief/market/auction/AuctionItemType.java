package hgds.epicgrief.market.auction;

import java.util.ArrayList;
import java.util.Arrays;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import hgds.epicgrief.market.messaging.Lang;

public enum AuctionItemType {
    ARMOR(new ItemStack(Material.IRON_CHESTPLATE), getArmor()),
    WEAPON(new ItemStack(Material.DIAMOND_SWORD), getWeapons()),
    TOOLS(new ItemStack(Material.IRON_PICKAXE), getTools()),
    FOOD(new ItemStack(Material.APPLE), getFood()),
    POTION(new ItemStack(Material.POTION), getPotions()),
    BLOCKS(new ItemStack(Material.BRICKS), getBlocks()),
    ENCHANTING(new ItemStack(Material.ENCHANTING_TABLE), getEnchanting()),
    RESOURCES(new ItemStack(Material.EMERALD), getResources()),
    FARMING(new ItemStack(Material.OAK_SAPLING), getFarming()),
    OTHER(new ItemStack(Material.CHEST), getOthers()),
    ALL(new ItemStack(Material.BARRIER), new ArrayList<>());

    AuctionItemType(ItemStack item, ArrayList<Material> items) {
        this.item = item;
        this.items = items;
    }

    private final ItemStack item;

    private final ArrayList<Material> items;

    public ItemStack getItem() {
        return this.item.clone();
    }

    public String getName() {
        return Lang.getMessage("AUCTION_TYPE_" + name() + "_NAME");
    }

    public ArrayList<Material> getItems() {
        return this.items;
    }

    public static AuctionItemType getType(ItemStack item) {
        Material material = item.getType();
        return Arrays.stream(values())
                .filter(auctionItemType -> auctionItemType.items.contains(material))
                .findFirst()
                .orElse(OTHER);
    }

    private static ArrayList<Material> getFarming() {
        ArrayList<Material> ma = new ArrayList<>();
        ma.add(Material.WHEAT_SEEDS);
        ma.add(Material.PUMPKIN_SEEDS);
        ma.add(Material.MELON_SEEDS);
        ma.add(Material.BEETROOT_SEEDS);
        ma.add(Material.WHEAT);
        ma.add(Material.SUGAR_CANE);
        ma.add(Material.CACTUS);
        return ma;
    }

    private static ArrayList<Material> getResources() {
        ArrayList<Material> ma = new ArrayList<>();
        ma.add(Material.DIAMOND);
        ma.add(Material.IRON_INGOT);
        ma.add(Material.GOLD_INGOT);
        ma.add(Material.REDSTONE);
        ma.add(Material.COAL);
        ma.add(Material.GOLD_NUGGET);
        ma.add(Material.EMERALD);
        return ma;
    }

    private static ArrayList<Material> getArmor() {
        ArrayList<Material> ma = new ArrayList<>();
        ma.add(Material.DIAMOND_HELMET);
        ma.add(Material.DIAMOND_CHESTPLATE);
        ma.add(Material.DIAMOND_LEGGINGS);
        ma.add(Material.DIAMOND_BOOTS);
        ma.add(Material.CHAINMAIL_HELMET);
        ma.add(Material.CHAINMAIL_CHESTPLATE);
        ma.add(Material.CHAINMAIL_LEGGINGS);
        ma.add(Material.CHAINMAIL_BOOTS);
        ma.add(Material.GOLDEN_HELMET);
        ma.add(Material.GOLDEN_CHESTPLATE);
        ma.add(Material.GOLDEN_LEGGINGS);
        ma.add(Material.GOLDEN_BOOTS);
        ma.add(Material.IRON_HELMET);
        ma.add(Material.IRON_CHESTPLATE);
        ma.add(Material.IRON_LEGGINGS);
        ma.add(Material.IRON_BOOTS);
        ma.add(Material.LEATHER_HELMET);
        ma.add(Material.LEATHER_CHESTPLATE);
        ma.add(Material.LEATHER_LEGGINGS);
        ma.add(Material.LEATHER_BOOTS);
        return ma;
    }

    private static ArrayList<Material> getTools() {
        ArrayList<Material> ma = new ArrayList<>();
        ma.add(Material.WOODEN_PICKAXE);
        ma.add(Material.STONE_PICKAXE);
        ma.add(Material.IRON_PICKAXE);
        ma.add(Material.DIAMOND_PICKAXE);
        ma.add(Material.NETHERITE_PICKAXE);
        ma.add(Material.WOODEN_AXE);
        ma.add(Material.STONE_AXE);
        ma.add(Material.IRON_AXE);
        ma.add(Material.DIAMOND_AXE);
        ma.add(Material.NETHERITE_AXE);
        ma.add(Material.WOODEN_SHOVEL);
        ma.add(Material.STONE_SHOVEL);
        ma.add(Material.IRON_SHOVEL);
        ma.add(Material.DIAMOND_SHOVEL);
        ma.add(Material.NETHERITE_SHOVEL);
        ma.add(Material.WOODEN_HOE);
        ma.add(Material.STONE_HOE);
        ma.add(Material.IRON_HOE);
        ma.add(Material.DIAMOND_HOE);
        ma.add(Material.NETHERITE_HOE);
        ma.add(Material.FISHING_ROD);
        return ma;
    }

    private static ArrayList<Material> getWeapons() {
        ArrayList<Material> ma = new ArrayList<>();
        ma.add(Material.WOODEN_SWORD);
        ma.add(Material.STONE_SWORD);
        ma.add(Material.IRON_SWORD);
        ma.add(Material.DIAMOND_SWORD);
        ma.add(Material.NETHERITE_SWORD);
        ma.add(Material.WOODEN_AXE);
        ma.add(Material.STONE_AXE);
        ma.add(Material.IRON_AXE);
        ma.add(Material.DIAMOND_AXE);
        ma.add(Material.NETHERITE_AXE);
        ma.add(Material.BOW);
        ma.add(Material.CROSSBOW);
        return ma;
    }

    private static ArrayList<Material> getFood() {
        ArrayList<Material> ma = new ArrayList<>();
        for (Material m : Material.values()) {
            if (m.isEdible() && m != Material.POTION)
                ma.add(m);
        }
        return ma;
    }

    private static ArrayList<Material> getPotions() {
        ArrayList<Material> ma = new ArrayList<>();
        ma.add(Material.GLOWSTONE_DUST);
        ma.add(Material.NETHER_WART);
        ma.add(Material.BLAZE_POWDER);
        ma.add(Material.BLAZE_ROD);
        ma.add(Material.SUGAR);
        ma.add(Material.GHAST_TEAR);
        ma.add(Material.SPIDER_EYE);
        ma.add(Material.FERMENTED_SPIDER_EYE);
        ma.add(Material.MAGMA_CREAM);
        ma.add(Material.GLISTERING_MELON_SLICE);
        ma.add(Material.GOLDEN_CARROT);
        ma.add(Material.RABBIT_FOOT);
        ma.add(Material.GLASS_BOTTLE);
        ma.add(Material.POTION);
        ma.add(Material.SPLASH_POTION);
        ma.add(Material.LINGERING_POTION);
        ma.add(Material.SLIME_BALL);
        return ma;
    }

    private static ArrayList<Material> getEnchanting() {
        ArrayList<Material> ma = new ArrayList<>();
        ma.add(Material.ENCHANTED_BOOK);
        ma.add(Material.EXPERIENCE_BOTTLE);
        ma.add(Material.BOOKSHELF);
        ma.add(Material.ENCHANTING_TABLE);
        return ma;
    }

    private static ArrayList<Material> getBlocks() {
        ArrayList<Material> ma = new ArrayList<>();
        for (Material m : Material.values()) {
            if (m.isBlock() && m.isItem())
                ma.add(m);
        }
        return ma;
    }

    private static ArrayList<Material> getOthers() {
        ArrayList<Material> ma = new ArrayList<>();
        for (Material m : Material.values()) {
            if (!m.isItem() || m == Material.AIR)
                continue;
            if (!getArmor().contains(m) &&
                    !getTools().contains(m) &&
                    !getWeapons().contains(m) &&
                    !getFood().contains(m) &&
                    !getPotions().contains(m) &&
                    !getEnchanting().contains(m) &&
                    !getBlocks().contains(m))
                ma.add(m);
        }
        return ma;
    }
}

