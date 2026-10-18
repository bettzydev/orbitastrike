package uz.orbitalstrike;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class ItemListener implements Listener {
    private final OrbitalStrikePlugin plugin;
    private final Map<UUID, Long> creativeCooldown = new HashMap<>();
    private final Set<UUID> railgunBusy = new HashSet<>();

    public ItemListener(OrbitalStrikePlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent e) {
        Action a = e.getAction();
        if (a != Action.RIGHT_CLICK_AIR && a != Action.RIGHT_CLICK_BLOCK) return;
        EquipmentSlot hand = e.getHand();
        if (hand == null) return;
        ItemStack item = e.getItem();
        OrbitalItems items = plugin.items();
        String type = items.typeOf(item);
        if (type == null) return;

        e.setCancelled(true);
        Player p = e.getPlayer();
        FileConfiguration c = plugin.getConfig();

        if (!p.hasPermission("orbitalstrike.use")) {
            p.sendMessage(Msg.err("You are not allowed to use this item."));
            return;
        }
        if (c.getStringList("disabled-worlds").contains(p.getWorld().getName())) {
            p.sendMessage(Msg.err("Orbital items are disabled in this world."));
            return;
        }

        Strikes s = plugin.strikes();
        double delay = c.getDouble("rod-delay-seconds", 3);

        switch (type) {
            case "stab": {
                Location t = s.targetBlock(p);
                if (t == null) { noBlock(p); return; }
                if (!ready(p)) return;
                final World w = t.getWorld();
                final int x = t.getBlockX(), y = t.getBlockY(), z = t.getBlockZ();
                consume(p, hand);
                p.sendMessage(Msg.ok("Stab Shot launched."));
                later(delay, () -> s.stab(w, x, y, z));
                break;
            }
            case "nuke": {
                Location t = s.targetBlock(p);
                if (t == null) { noBlock(p); return; }
                if (!ready(p)) return;
                final World w = t.getWorld();
                final int x = t.getBlockX(), y = t.getBlockY(), z = t.getBlockZ();
                final int rings = items.getInt(item, items.keyRings, c.getInt("nuke.default-rings", 4));
                consume(p, hand);
                p.sendMessage(Msg.ok("Nuke Shot launched."));
                later(delay, () -> s.nuke(w, x, y, z, rings));
                break;
            }
            case "wolf": {
                Location t = s.targetBlock(p);
                if (t == null) { noBlock(p); return; }
                if (!ready(p)) return;
                final World w = t.getWorld();
                final int x = t.getBlockX(), y = t.getBlockY(), z = t.getBlockZ();
                final int n = items.getInt(item, items.keyWolves, c.getInt("wolf.default-amount", 20));
                consume(p, hand);
                p.sendMessage(Msg.ok("Orbital Wolf launched."));
                later(delay, () -> s.wolves(w, x, y, z, n));
                break;
            }
            case "stasis": {
                int sx = items.getInt(item, items.keyX, OrbitalItems.NONE);
                int sy = items.getInt(item, items.keyY, OrbitalItems.NONE);
                int sz = items.getInt(item, items.keyZ, OrbitalItems.NONE);
                if (sx == OrbitalItems.NONE || sy == OrbitalItems.NONE || sz == OrbitalItems.NONE) {
                    p.sendMessage(Msg.err("This Stasis item has no stored coordinates."));
                    return;
                }
                if (!ready(p)) return;
                final String world = items.getString(item, items.keyWorld);
                consume(p, hand);
                p.sendMessage(Msg.ok("Stasis activated."));
                later(delay, () -> {
                    if (p.isOnline()) s.stasis(p, world, sx, sy, sz);
                });
                break;
            }
            case "railgun": {
                if (railgunBusy.contains(p.getUniqueId())) return;
                final double radius = items.getDouble(item, items.keyRadius, 0);
                double rd = c.getDouble("railgun-delay-seconds", 0);
                railgunBusy.add(p.getUniqueId());
                later(rd, () -> {
                    railgunBusy.remove(p.getUniqueId());
                    if (p.isOnline()) s.railgun(p, radius);
                });
                break;
            }
            default:
                break;
        }
    }

    private void noBlock(Player p) {
        p.sendMessage(Msg.err("No block in your crosshair!"));
    }

    /** Creative players keep their items, so they get a cooldown instead. */
    private boolean ready(Player p) {
        if (p.getGameMode() != GameMode.CREATIVE) return true;
        long now = System.currentTimeMillis();
        long until = creativeCooldown.getOrDefault(p.getUniqueId(), 0L);
        if (now < until) {
            p.sendMessage(Msg.err("Cooldown: " + ((until - now) / 1000 + 1) + "s"));
            return false;
        }
        long cd = (long) (plugin.getConfig().getDouble("creative-cooldown-seconds", 5) * 1000);
        creativeCooldown.put(p.getUniqueId(), now + cd);
        return true;
    }

    private void consume(Player p, EquipmentSlot hand) {
        if (p.getGameMode() == GameMode.CREATIVE) return;
        ItemStack cur = p.getInventory().getItem(hand);
        if (cur == null) return;
        if (cur.getAmount() <= 1) p.getInventory().setItem(hand, null);
        else cur.setAmount(cur.getAmount() - 1);
    }

    private void later(double seconds, Runnable r) {
        long ticks = Math.max(0, Math.round(seconds * 20));
        if (ticks == 0) Bukkit.getScheduler().runTask(plugin, r);
        else Bukkit.getScheduler().runTaskLater(plugin, r, ticks);
    }
}
