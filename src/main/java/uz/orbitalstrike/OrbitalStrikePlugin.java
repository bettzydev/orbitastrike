package uz.orbitalstrike;

import org.bukkit.Material;
import org.bukkit.command.PluginCommand;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.LinkedHashMap;
import java.util.Map;

public final class OrbitalStrikePlugin extends JavaPlugin {
    private OrbitalItems items;
    private Strikes strikes;
    private DeathBanListener deathBan;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        items = new OrbitalItems(this);
        strikes = new Strikes(this);
        deathBan = new DeathBanListener(this);

        getServer().getPluginManager().registerEvents(new ItemListener(this), this);
        getServer().getPluginManager().registerEvents(deathBan, this);
        getServer().getPluginManager().registerEvents(new ProximityListener(this), this);

        OrbitalCommand cmd = new OrbitalCommand(this);
        PluginCommand pc = getCommand("orbitalstrike");
        if (pc != null) {
            pc.setExecutor(cmd);
            pc.setTabCompleter(cmd);
        }
        getLogger().info("OrbitalStrike enabled.");
    }

    @Override
    public void onDisable() {
        if (deathBan != null) deathBan.save();
    }

    public OrbitalItems items() { return items; }
    public Strikes strikes() { return strikes; }
    public DeathBanListener deathBan() { return deathBan; }

    /** Crafting cost of "nuke" or "stab" from config; null if not defined. */
    public Map<Material, Integer> ingredients(String type) {
        ConfigurationSection sec = getConfig().getConfigurationSection("recipes." + type);
        if (sec == null) return null;
        Map<Material, Integer> out = new LinkedHashMap<>();
        for (String key : sec.getKeys(false)) {
            Material m = Material.matchMaterial(key);
            int amount = sec.getInt(key, 0);
            if (m == null || amount <= 0) {
                getLogger().warning("Bad recipe entry recipes." + type + "." + key);
                continue;
            }
            out.put(m, amount);
        }
        return out;
    }
}
