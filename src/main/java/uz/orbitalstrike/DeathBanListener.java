package uz.orbitalstrike;

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;

import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.UUID;

public final class DeathBanListener implements Listener {
    private final OrbitalStrikePlugin plugin;
    private final File file;
    private final YamlConfiguration data;

    public DeathBanListener(OrbitalStrikePlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "deaths.yml");
        this.data = file.exists() ? YamlConfiguration.loadConfiguration(file) : new YamlConfiguration();
    }

    public void save() {
        try {
            plugin.getDataFolder().mkdirs();
            data.save(file);
        } catch (IOException ex) {
            plugin.getLogger().warning("Could not save deaths.yml: " + ex.getMessage());
        }
    }

    public void reset(UUID id) {
        data.set("deaths." + id, null);
        save();
    }

    public int count(UUID id) {
        return data.getInt("deaths." + id, 0);
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent e) {
        FileConfiguration c = plugin.getConfig();
        if (!c.getBoolean("death-ban.enabled", false)) return;
        Player p = e.getEntity();

        // death sound for nearby players
        String sound = c.getString("death-ban.sound", "");
        double range = c.getDouble("death-ban.sound-range", 100);
        Location loc = p.getLocation();
        if (sound != null && !sound.isEmpty() && range > 0) {
            double r2 = range * range;
            for (Player o : loc.getWorld().getPlayers()) {
                if (o.getLocation().distanceSquared(loc) <= r2) {
                    o.playSound(loc, sound, 1f, 1f);
                }
            }
        }

        int need = Math.max(1, c.getInt("death-ban.deaths-until-ban", 3));
        UUID id = p.getUniqueId();
        int n = count(id) + 1;

        if (n >= need) {
            data.set("deaths." + id, 0);
            save();
            long delayTicks = Math.max(0, Math.round(c.getDouble("death-ban.delay-seconds", 5) * 20));
            Bukkit.getScheduler().runTaskLater(plugin, () -> punish(p), delayTicks);
            plugin.getLogger().info("Player " + p.getName() + " reached " + need + " deaths and will be punished.");
        } else {
            data.set("deaths." + id, n);
            save();
            p.sendMessage(Msg.err("You have died " + n + " time(s). " + (need - n) + " more death(s) until ban!"));
        }
    }

    private void punish(Player p) {
        FileConfiguration c = plugin.getConfig();
        long ms;
        try {
            ms = Durations.parse(c.getString("death-ban.duration", "1d"));
        } catch (IllegalArgumentException ex) {
            plugin.getLogger().warning("Bad death-ban.duration in config, using 1d.");
            ms = 86_400_000L;
        }
        if (ms == -2) {
            if (p.isOnline()) {
                p.kick(Component.text(c.getString("death-ban.kick-message", "You died too many times!")));
            }
            return;
        }
        String reason = c.getString("death-ban.message", "You died too many times.")
                .replace("%death.ban.duration%", Durations.format(ms));
        Date expires = ms == -1 ? null : new Date(System.currentTimeMillis() + ms);
        p.ban(reason, expires, "OrbitalStrike");
    }
}
