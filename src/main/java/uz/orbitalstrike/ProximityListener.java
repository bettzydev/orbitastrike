package uz.orbitalstrike;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public final class ProximityListener implements Listener {
    private final OrbitalStrikePlugin plugin;

    public ProximityListener(OrbitalStrikePlugin plugin) {
        this.plugin = plugin;
    }

    private boolean enabled() {
        return plugin.getConfig().getBoolean("proximity.enabled", false);
    }

    private void sendNear(Location center, Component msg) {
        double r = plugin.getConfig().getDouble("proximity.range", 100);
        double r2 = r * r;
        for (Player o : center.getWorld().getPlayers()) {
            if (o.getLocation().distanceSquared(center) <= r2) {
                o.sendMessage(msg);
            }
        }
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        if (!enabled()) return;
        e.joinMessage(null);
        Player p = e.getPlayer();
        sendNear(p.getLocation(), Component.text(p.getName() + " joined the game", NamedTextColor.YELLOW));
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        if (!enabled()) return;
        e.quitMessage(null);
        Player p = e.getPlayer();
        sendNear(p.getLocation(), Component.text(p.getName() + " left the game", NamedTextColor.YELLOW));
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent e) {
        if (!enabled()) return;
        Component dm = e.deathMessage();
        e.deathMessage(null);
        if (dm == null) return;
        Player p = e.getEntity();
        sendNear(p.getLocation(), dm);
    }
}
