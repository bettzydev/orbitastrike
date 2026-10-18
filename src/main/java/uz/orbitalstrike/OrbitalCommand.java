package uz.orbitalstrike;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class OrbitalCommand implements CommandExecutor, TabCompleter {
    private static final List<String> SUBS = Arrays.asList(
            "help", "give", "strike", "craft", "recipes", "set", "reload", "resetdeaths");
    private static final List<String> ITEM_TYPES = Arrays.asList("stab", "nuke", "wolf", "stasis", "railgun");
    private static final List<String> STRIKE_TYPES = Arrays.asList("stab", "nuke", "wolf");
    private static final List<String> CRAFT_TYPES = Arrays.asList("nuke", "stab");
    private static final List<String> SET_KEYS = Arrays.asList(
            "recipe", "rod-delay", "railgun-delay", "arrow-velocity", "proximity", "proximity-range",
            "deathban", "ban-duration", "death-sound", "deathsound-range");

    private final OrbitalStrikePlugin plugin;

    public OrbitalCommand(OrbitalStrikePlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (args.length == 0) { help(sender); return true; }
        String sub = args[0].toLowerCase(Locale.ROOT);
        switch (sub) {
            case "help": help(sender); return true;
            case "recipes": recipes(sender); return true;
            case "craft": craft(sender, args); return true;
            default: break;
        }
        if (!sender.hasPermission("orbitalstrike.admin")) {
            sender.sendMessage(Msg.err("You do not have permission."));
            return true;
        }
        switch (sub) {
            case "give": give(sender, args); break;
            case "strike": strike(sender, args); break;
            case "set": set(sender, args); break;
            case "reload":
                plugin.reloadConfig();
                sender.sendMessage(Msg.ok("Config reloaded."));
                break;
            case "resetdeaths": resetDeaths(sender, args); break;
            default: help(sender); break;
        }
        return true;
    }

    // ------------------------------------------------------------------ help
    private void help(CommandSender s) {
        s.sendMessage(Msg.info("=== OrbitalStrike ==="));
        s.sendMessage(Msg.plain("/os recipes  -  show crafting costs"));
        s.sendMessage(Msg.plain("/os craft <nuke|stab>  -  craft an item"));
        if (s.hasPermission("orbitalstrike.admin")) {
            s.sendMessage(Msg.plain("/os give stab|nuke|wolf|stasis|railgun [amount] [param]"));
            s.sendMessage(Msg.plain("   nuke param = rings, wolf param = wolf count, railgun param = radius"));
            s.sendMessage(Msg.plain("   stasis: /os give stasis [amount] [x y z]  (no coords = block you look at)"));
            s.sendMessage(Msg.plain("/os strike stab [x y z [world]]"));
            s.sendMessage(Msg.plain("/os strike nuke <rings> [x y z [world]]"));
            s.sendMessage(Msg.plain("/os strike wolf <amount> [x y z [world]]"));
            s.sendMessage(Msg.plain("/os set <key> <value>  keys: " + String.join(", ", SET_KEYS)));
            s.sendMessage(Msg.plain("/os reload   /os resetdeaths <player>"));
        }
    }

    // ------------------------------------------------------------------ give
    private void give(CommandSender sender, String[] args) {
        if (!(sender instanceof Player p)) {
            sender.sendMessage(Msg.err("Only players can use /os give."));
            return;
        }
        if (args.length < 2) { sender.sendMessage(Msg.err("Usage: /os give <stab|nuke|wolf|stasis|railgun> [amount] ...")); return; }
        FileConfiguration c = plugin.getConfig();
        OrbitalItems items = plugin.items();
        String type = args[1].toLowerCase(Locale.ROOT);

        int amount = 1;
        if (args.length > 2) {
            Integer a = num(args[2]);
            if (a == null || a < 1 || a > 64) { sender.sendMessage(Msg.err("Amount must be between 1 and 64!")); return; }
            amount = a;
        }

        ItemStack item;
        switch (type) {
            case "stab":
                item = items.stab();
                break;
            case "nuke": {
                int rings = c.getInt("nuke.default-rings", 4);
                if (args.length > 3) {
                    Integer r = num(args[3]);
                    if (r == null) { sender.sendMessage(Msg.err("Invalid ring count!")); return; }
                    rings = r;
                }
                int max = c.getInt("nuke.max-rings", 32);
                if (rings < 1 || rings > max) { sender.sendMessage(Msg.err("Ring size must be between 1 and " + max + "!")); return; }
                item = items.nuke(rings);
                break;
            }
            case "wolf": {
                int n = c.getInt("wolf.default-amount", 20);
                if (args.length > 3) {
                    Integer r = num(args[3]);
                    if (r == null) { sender.sendMessage(Msg.err("Invalid wolf amount!")); return; }
                    n = r;
                }
                int max = c.getInt("wolf.max-amount", 500);
                if (n < 1 || n > max) { sender.sendMessage(Msg.err("Wolf amount must be between 1 and " + max + " (server safety)!")); return; }
                item = items.wolf(n);
                break;
            }
            case "stasis": {
                int x, y, z;
                String world = p.getWorld().getName();
                if (args.length >= 6) {
                    Integer ix = num(args[3]), iy = num(args[4]), iz = num(args[5]);
                    if (ix == null || iy == null || iz == null) { sender.sendMessage(Msg.err("Invalid coordinates!")); return; }
                    x = ix; y = iy; z = iz;
                } else {
                    Location t = plugin.strikes().targetBlock(p);
                    if (t == null) { sender.sendMessage(Msg.err("No block in your crosshair!")); return; }
                    x = t.getBlockX(); y = t.getBlockY() + 1; z = t.getBlockZ();
                }
                item = items.stasis(world, x, y, z);
                break;
            }
            case "railgun": {
                double radius = 0;
                if (args.length > 3) {
                    try { radius = Double.parseDouble(args[3]); }
                    catch (NumberFormatException ex) { sender.sendMessage(Msg.err("Invalid radius!")); return; }
                }
                if (radius < 0 || radius > 50) { sender.sendMessage(Msg.err("Arrow radius must be between 0 and 50!")); return; }
                item = items.railgun(radius);
                break;
            }
            default:
                sender.sendMessage(Msg.err("Unknown item type. Use: " + String.join(", ", ITEM_TYPES)));
                return;
        }
        giveCopies(p, item, amount);
        sender.sendMessage(Msg.ok("Given " + amount + " x " + type + "."));
    }

    private void giveCopies(Player p, ItemStack item, int amount) {
        for (int i = 0; i < amount; i++) {
            Map<Integer, ItemStack> left = p.getInventory().addItem(item.clone());
            for (ItemStack rest : left.values()) {
                p.getWorld().dropItemNaturally(p.getLocation(), rest);
            }
        }
    }

    // ------------------------------------------------------------------ strike
    private void strike(CommandSender sender, String[] args) {
        if (args.length < 2) { sender.sendMessage(Msg.err("Usage: /os strike <stab|nuke|wolf> ...")); return; }
        FileConfiguration c = plugin.getConfig();
        String type = args[1].toLowerCase(Locale.ROOT);
        if (!STRIKE_TYPES.contains(type)) {
            sender.sendMessage(Msg.err("Unknown strike. Use: " + String.join(", ", STRIKE_TYPES)));
            return;
        }

        int param = 0;
        int coordIdx = 2;
        if (!type.equals("stab")) {
            param = type.equals("nuke") ? c.getInt("nuke.default-rings", 4) : c.getInt("wolf.default-amount", 20);
            if (args.length > 2) {
                Integer v = num(args[2]);
                if (v == null) { sender.sendMessage(Msg.err("Invalid number: " + args[2])); return; }
                param = v;
            }
            coordIdx = 3;
            int max = type.equals("nuke") ? c.getInt("nuke.max-rings", 32) : c.getInt("wolf.max-amount", 500);
            if (param < 1 || param > max) { sender.sendMessage(Msg.err("Value must be between 1 and " + max + "!")); return; }
        }

        World w;
        int x, y, z;
        if (args.length >= coordIdx + 3) {
            Integer ix = num(args[coordIdx]), iy = num(args[coordIdx + 1]), iz = num(args[coordIdx + 2]);
            if (ix == null || iy == null || iz == null) { sender.sendMessage(Msg.err("Invalid coordinates!")); return; }
            x = ix; y = iy; z = iz;
            if (args.length > coordIdx + 3) {
                w = Bukkit.getWorld(args[coordIdx + 3]);
            } else if (sender instanceof Player pl) {
                w = pl.getWorld();
            } else {
                w = null;
            }
            if (w == null) { sender.sendMessage(Msg.err("World not found (add the world name at the end).")); return; }
        } else if (sender instanceof Player pl) {
            Location t = plugin.strikes().targetBlock(pl);
            if (t == null) { sender.sendMessage(Msg.err("No block in your crosshair!")); return; }
            w = t.getWorld();
            x = t.getBlockX(); y = t.getBlockY(); z = t.getBlockZ();
        } else {
            sender.sendMessage(Msg.err("From console use coordinates and a world name."));
            return;
        }

        switch (type) {
            case "stab": plugin.strikes().stab(w, x, y, z); break;
            case "nuke": plugin.strikes().nuke(w, x, y, z, param); break;
            default: plugin.strikes().wolves(w, x, y, z, param); break;
        }
        sender.sendMessage(Msg.ok("Strike executed at " + w.getName() + " " + x + " " + y + " " + z));
    }

    // ------------------------------------------------------------------ recipes / craft
    private void recipes(CommandSender s) {
        s.sendMessage(Msg.info("=== Orbital Strike Crafting Recipes ==="));
        for (String t : CRAFT_TYPES) {
            Map<Material, Integer> ing = plugin.ingredients(t);
            if (ing == null) continue;
            StringBuilder sb = new StringBuilder();
            for (Map.Entry<Material, Integer> en : ing.entrySet()) {
                if (sb.length() > 0) sb.append(", ");
                sb.append(en.getValue()).append("x ").append(pretty(en.getKey()));
            }
            s.sendMessage(Msg.info(cap(t) + " Shot:"));
            s.sendMessage(Msg.plain("  " + sb));
            s.sendMessage(Msg.plain("  Command: /os craft " + t));
        }
        if (!plugin.getConfig().getBoolean("recipe-enabled", true)) {
            s.sendMessage(Msg.err("Crafting is currently disabled."));
        }
    }

    private void craft(CommandSender sender, String[] args) {
        if (!(sender instanceof Player p)) { sender.sendMessage(Msg.err("Only players can craft.")); return; }
        if (!p.hasPermission("orbitalstrike.craft")) { p.sendMessage(Msg.err("You do not have permission.")); return; }
        if (!plugin.getConfig().getBoolean("recipe-enabled", true)) { p.sendMessage(Msg.err("Crafting is disabled.")); return; }
        if (args.length < 2 || !CRAFT_TYPES.contains(args[1].toLowerCase(Locale.ROOT))) {
            p.sendMessage(Msg.err("Usage: /os craft <nuke|stab>"));
            return;
        }
        String type = args[1].toLowerCase(Locale.ROOT);
        Map<Material, Integer> ing = plugin.ingredients(type);
        if (ing == null || ing.isEmpty()) { p.sendMessage(Msg.err("No recipe configured for " + type + ".")); return; }

        for (Map.Entry<Material, Integer> en : ing.entrySet()) {
            if (!p.getInventory().containsAtLeast(new ItemStack(en.getKey()), en.getValue())) {
                p.sendMessage(Msg.err("You need " + en.getValue() + "x " + pretty(en.getKey()) + "."));
                return;
            }
        }
        for (Map.Entry<Material, Integer> en : ing.entrySet()) {
            p.getInventory().removeItem(new ItemStack(en.getKey(), en.getValue()));
        }
        ItemStack out = type.equals("nuke")
                ? plugin.items().nuke(plugin.getConfig().getInt("nuke.default-rings", 4))
                : plugin.items().stab();
        giveCopies(p, out, 1);
        p.sendMessage(Msg.ok("Crafted " + cap(type) + " Shot."));
    }

    // ------------------------------------------------------------------ set
    private void set(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sender.sendMessage(Msg.err("Usage: /os set <key> <value>. Keys: " + String.join(", ", SET_KEYS)));
            return;
        }
        String key = args[1].toLowerCase(Locale.ROOT);
        String val = args[2];
        FileConfiguration c = plugin.getConfig();
        boolean ok;
        switch (key) {
            case "recipe": ok = setBool(sender, "recipe-enabled", val); break;
            case "rod-delay": ok = setNum(sender, "rod-delay-seconds", val, 0, 60); break;
            case "railgun-delay": ok = setNum(sender, "railgun-delay-seconds", val, 0, 60); break;
            case "arrow-velocity": ok = setNum(sender, "arrow-velocity", val, 0, 100); break;
            case "proximity": ok = setBool(sender, "proximity.enabled", val); break;
            case "proximity-range": ok = setNum(sender, "proximity.range", val, 0, 10000); break;
            case "deathban": ok = setBool(sender, "death-ban.enabled", val); break;
            case "deathsound-range": ok = setNum(sender, "death-ban.sound-range", val, 0, 10000); break;
            case "death-sound":
                c.set("death-ban.sound", val);
                ok = true;
                break;
            case "ban-duration":
                try {
                    Durations.parse(val);
                    c.set("death-ban.duration", val);
                    ok = true;
                } catch (IllegalArgumentException ex) {
                    sender.sendMessage(Msg.err("Invalid format! Use -2 (kick), -1 (permanent), or 30m, 1h, 1d, 7d, 2w"));
                    ok = false;
                }
                break;
            default:
                sender.sendMessage(Msg.err("Unknown setting! Keys: " + String.join(", ", SET_KEYS)));
                return;
        }
        if (ok) {
            plugin.saveConfig();
            sender.sendMessage(Msg.ok(key + " set to: " + val));
        }
    }

    private boolean setBool(CommandSender s, String path, String val) {
        if (!val.equalsIgnoreCase("true") && !val.equalsIgnoreCase("false")) {
            s.sendMessage(Msg.err("Value must be true or false."));
            return false;
        }
        plugin.getConfig().set(path, Boolean.parseBoolean(val));
        return true;
    }

    private boolean setNum(CommandSender s, String path, String val, double min, double max) {
        double d;
        try { d = Double.parseDouble(val); }
        catch (NumberFormatException ex) { s.sendMessage(Msg.err("Invalid number!")); return false; }
        if (d < min || d > max) {
            s.sendMessage(Msg.err("Value must be between " + (long) min + " and " + (long) max + "!"));
            return false;
        }
        plugin.getConfig().set(path, d == Math.floor(d) ? (Object) (long) d : (Object) d);
        return true;
    }

    private void resetDeaths(CommandSender sender, String[] args) {
        if (args.length < 2) { sender.sendMessage(Msg.err("Usage: /os resetdeaths <player>")); return; }
        OfflinePlayer op = Bukkit.getOfflinePlayerIfCached(args[1]);
        if (op == null) { sender.sendMessage(Msg.err("Player not found (they must have joined before).")); return; }
        plugin.deathBan().reset(op.getUniqueId());
        sender.sendMessage(Msg.ok("Death counter reset for " + args[1] + "."));
    }

    // ------------------------------------------------------------------ tab complete
    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String alias, String[] args) {
        boolean admin = sender.hasPermission("orbitalstrike.admin");
        if (args.length == 1) {
            List<String> subs = new ArrayList<>();
            for (String s : SUBS) {
                if (admin || s.equals("help") || s.equals("craft") || s.equals("recipes")) subs.add(s);
            }
            return filter(subs, args[0]);
        }
        if (args.length == 2) {
            switch (args[0].toLowerCase(Locale.ROOT)) {
                case "give": return admin ? filter(ITEM_TYPES, args[1]) : List.of();
                case "strike": return admin ? filter(STRIKE_TYPES, args[1]) : List.of();
                case "craft": return filter(CRAFT_TYPES, args[1]);
                case "set": return admin ? filter(SET_KEYS, args[1]) : List.of();
                case "resetdeaths": return admin ? null : List.of(); // null = online player names
                default: return List.of();
            }
        }
        if (args.length == 3 && args[0].equalsIgnoreCase("set") && admin) {
            String k = args[1].toLowerCase(Locale.ROOT);
            if (k.equals("recipe") || k.equals("proximity") || k.equals("deathban")) {
                return filter(Arrays.asList("true", "false"), args[2]);
            }
            if (k.equals("ban-duration")) {
                return filter(Arrays.asList("-2", "-1", "1h", "1d", "7d"), args[2]);
            }
        }
        return List.of();
    }

    private static List<String> filter(List<String> options, String prefix) {
        String p = prefix.toLowerCase(Locale.ROOT);
        List<String> out = new ArrayList<>();
        for (String o : options) if (o.startsWith(p)) out.add(o);
        return out;
    }

    // ------------------------------------------------------------------ utils
    private static Integer num(String s) {
        try { return Integer.parseInt(s); }
        catch (NumberFormatException ex) { return null; }
    }

    private static String cap(String s) {
        return s.isEmpty() ? s : Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }

    private static String pretty(Material m) {
        String[] words = m.name().toLowerCase(Locale.ROOT).split("_");
        StringBuilder sb = new StringBuilder();
        for (String w : words) {
            if (sb.length() > 0) sb.append(' ');
            sb.append(cap(w));
        }
        return sb.toString();
    }
}
