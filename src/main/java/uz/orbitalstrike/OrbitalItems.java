package uz.orbitalstrike;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public final class OrbitalItems {
    public static final int NONE = Integer.MIN_VALUE;

    public final NamespacedKey keyType;
    public final NamespacedKey keyRings;
    public final NamespacedKey keyWolves;
    public final NamespacedKey keyX;
    public final NamespacedKey keyY;
    public final NamespacedKey keyZ;
    public final NamespacedKey keyWorld;
    public final NamespacedKey keyRadius;

    public OrbitalItems(OrbitalStrikePlugin plugin) {
        keyType = new NamespacedKey(plugin, "type");
        keyRings = new NamespacedKey(plugin, "rings");
        keyWolves = new NamespacedKey(plugin, "wolves");
        keyX = new NamespacedKey(plugin, "sx");
        keyY = new NamespacedKey(plugin, "sy");
        keyZ = new NamespacedKey(plugin, "sz");
        keyWorld = new NamespacedKey(plugin, "sworld");
        keyRadius = new NamespacedKey(plugin, "radius");
    }

    private ItemStack make(Material mat, String name, NamedTextColor color, String type,
                           List<String> lore, Consumer<PersistentDataContainer> extra) {
        ItemStack it = new ItemStack(mat);
        ItemMeta meta = it.getItemMeta();
        meta.displayName(Component.text(name, color).decoration(TextDecoration.ITALIC, false));
        List<Component> l = new ArrayList<>();
        for (String s : lore) {
            l.add(Component.text(s, NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false));
        }
        meta.lore(l);
        meta.setUnbreakable(true);
        meta.setEnchantmentGlintOverride(true);
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        pdc.set(keyType, PersistentDataType.STRING, type);
        extra.accept(pdc);
        it.setItemMeta(meta);
        return it;
    }

    public ItemStack stab() {
        return make(Material.FISHING_ROD, "Stab Shot", NamedTextColor.RED, "stab",
                List.of("Right-click: digs a shaft down", "from the block you look at."),
                p -> { });
    }

    public ItemStack nuke(int rings) {
        return make(Material.FISHING_ROD, "Nuke Shot", NamedTextColor.DARK_RED, "nuke",
                List.of("Right-click: rains TNT rings", "on the block you look at.", "Rings: " + rings),
                p -> p.set(keyRings, PersistentDataType.INTEGER, rings));
    }

    public ItemStack wolf(int amount) {
        return make(Material.FISHING_ROD, "Orbital Wolf", NamedTextColor.GOLD, "wolf",
                List.of("Right-click: drops wolves", "on the block you look at.", "Wolves: " + amount),
                p -> p.set(keyWolves, PersistentDataType.INTEGER, amount));
    }

    public ItemStack stasis(String world, int x, int y, int z) {
        return make(Material.FISHING_ROD, "Stasis", NamedTextColor.AQUA, "stasis",
                List.of("Right-click: teleports you to", world + " " + x + " " + y + " " + z),
                p -> {
                    p.set(keyWorld, PersistentDataType.STRING, world);
                    p.set(keyX, PersistentDataType.INTEGER, x);
                    p.set(keyY, PersistentDataType.INTEGER, y);
                    p.set(keyZ, PersistentDataType.INTEGER, z);
                });
    }

    public ItemStack railgun(double radius) {
        return make(Material.CROSSBOW, "Orbital Railgun", NamedTextColor.LIGHT_PURPLE, "railgun",
                List.of("Right-click: fires fast straight arrows.", "Spread radius: " + radius),
                p -> p.set(keyRadius, PersistentDataType.DOUBLE, radius));
    }

    public String typeOf(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return null;
        return item.getItemMeta().getPersistentDataContainer().get(keyType, PersistentDataType.STRING);
    }

    public int getInt(ItemStack item, NamespacedKey key, int def) {
        Integer v = item.getItemMeta().getPersistentDataContainer().get(key, PersistentDataType.INTEGER);
        return v == null ? def : v;
    }

    public double getDouble(ItemStack item, NamespacedKey key, double def) {
        Double v = item.getItemMeta().getPersistentDataContainer().get(key, PersistentDataType.DOUBLE);
        return v == null ? def : v;
    }

    public String getString(ItemStack item, NamespacedKey key) {
        return item.getItemMeta().getPersistentDataContainer().get(key, PersistentDataType.STRING);
    }
}
