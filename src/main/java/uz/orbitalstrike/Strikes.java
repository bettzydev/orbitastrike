package uz.orbitalstrike;

import org.bukkit.Bukkit;
import org.bukkit.FluidCollisionMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.AbstractArrow;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Player;
import org.bukkit.entity.TNTPrimed;
import org.bukkit.entity.Wolf;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public final class Strikes {
    private final OrbitalStrikePlugin plugin;

    public Strikes(OrbitalStrikePlugin plugin) {
        this.plugin = plugin;
    }

    /** Block the player is looking at (up to 512 blocks), or null. */
    public Location targetBlock(Player p) {
        RayTraceResult r = p.getWorld().rayTraceBlocks(
                p.getEyeLocation(), p.getEyeLocation().getDirection(), 512,
                FluidCollisionMode.NEVER, true);
        if (r == null || r.getHitBlock() == null) return null;
        return r.getHitBlock().getLocation();
    }

    // ---------------------------------------------------------------- NUKE
    public void nuke(World w, double cx, double cy, double cz, int rings) {
        FileConfiguration c = plugin.getConfig();
        int maxTnt = c.getInt("nuke.max-tnt", 4000);
        int fuse = c.getInt("nuke.fuse-ticks", 80);
        double height = c.getDouble("nuke.drop-height", 30);
        double spacing = c.getDouble("nuke.ring-spacing", 1.5);
        double y = Math.min(cy + height, w.getMaxHeight() - 2);
        double x0 = cx + 0.5;
        double z0 = cz + 0.5;

        int spawned = 0;
        spawnTnt(w, x0, y, z0, fuse);
        spawned++;
        for (int ring = 1; ring <= rings && spawned < maxTnt; ring++) {
            double radius = ring * spacing;
            int count = ring * 6;
            for (int i = 0; i < count && spawned < maxTnt; i++) {
                double ang = 2 * Math.PI * i / count;
                spawnTnt(w, x0 + Math.cos(ang) * radius, y, z0 + Math.sin(ang) * radius, fuse);
                spawned++;
            }
        }
        w.playSound(new Location(w, x0, cy, z0), "minecraft:entity.lightning_bolt.thunder", 4f, 0.8f);
    }

    private void spawnTnt(World w, double x, double y, double z, int fuse) {
        w.spawn(new Location(w, x, y, z), TNTPrimed.class, t -> t.setFuseTicks(fuse));
    }

    // ---------------------------------------------------------------- STAB
    public void stab(World w, int x, int y, int z) {
        FileConfiguration c = plugin.getConfig();
        int r = Math.max(0, c.getInt("stab.radius", 1));
        boolean bedrock = c.getBoolean("stab.break-bedrock", false);
        int minY = w.getMinHeight();
        for (int dx = -r; dx <= r; dx++) {
            for (int dz = -r; dz <= r; dz++) {
                if (dx * dx + dz * dz > r * r) continue;
                for (int by = y; by >= minY; by--) {
                    Block b = w.getBlockAt(x + dx, by, z + dz);
                    Material t = b.getType();
                    if (t.isAir()) continue;
                    if (t == Material.BEDROCK && !bedrock) continue;
                    b.setType(Material.AIR, false);
                }
            }
        }
        Location l = new Location(w, x + 0.5, y, z + 0.5);
        w.strikeLightningEffect(l);
        w.playSound(l, "minecraft:entity.generic.explode", 4f, 0.7f);
    }

    // ---------------------------------------------------------------- WOLVES
    public void wolves(World w, double cx, double cy, double cz, int amount) {
        FileConfiguration c = plugin.getConfig();
        double height = c.getDouble("wolf.drop-height", 40);
        double spread = c.getDouble("wolf.spread", 4.0);
        int lifetime = c.getInt("wolf.lifetime-seconds", 30);
        boolean armor = c.getBoolean("wolf.armor", true);
        ThreadLocalRandom rnd = ThreadLocalRandom.current();
        double y = Math.min(cy + height, w.getMaxHeight() - 2);

        List<Wolf> list = new ArrayList<>();
        for (int i = 0; i < amount; i++) {
            double ang = rnd.nextDouble() * 2 * Math.PI;
            double dist = Math.sqrt(rnd.nextDouble()) * spread;
            Location l = new Location(w, cx + 0.5 + Math.cos(ang) * dist, y + rnd.nextDouble() * 5,
                    cz + 0.5 + Math.sin(ang) * dist);
            Wolf wolf = w.spawn(l, Wolf.class, wf -> {
                wf.setAngry(true);
                wf.setRemoveWhenFarAway(true);
                if (armor) {
                    wf.getEquipment().setItem(EquipmentSlot.BODY, new ItemStack(Material.WOLF_ARMOR));
                    wf.getEquipment().setDropChance(EquipmentSlot.BODY, 0f);
                }
            });
            list.add(wolf);
        }
        if (lifetime > 0) {
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                for (Wolf wf : list) if (wf.isValid()) wf.remove();
            }, lifetime * 20L);
        }
    }

    // ---------------------------------------------------------------- STASIS
    public void stasis(Player p, String worldName, int x, int y, int z) {
        World w = worldName == null ? null : Bukkit.getWorld(worldName);
        if (w == null) w = p.getWorld();
        Location cur = p.getLocation();
        p.getWorld().playSound(cur, "minecraft:entity.enderman.teleport", 1f, 1f);
        p.teleport(new Location(w, x + 0.5, y, z + 0.5, cur.getYaw(), cur.getPitch()));
        w.playSound(p.getLocation(), "minecraft:entity.enderman.teleport", 1f, 1f);
    }

    // ---------------------------------------------------------------- RAILGUN
    public void railgun(Player p, double radius) {
        FileConfiguration c = plugin.getConfig();
        double vel = c.getDouble("arrow-velocity", 8.0);
        Location eye = p.getEyeLocation();
        Vector dir = eye.getDirection().normalize();
        Vector ref = Math.abs(dir.getY()) > 0.99 ? new Vector(1, 0, 0) : new Vector(0, 1, 0);
        Vector right = dir.clone().crossProduct(ref).normalize();
        Vector up = right.clone().crossProduct(dir).normalize();

        List<double[]> offsets = new ArrayList<>();
        offsets.add(new double[]{0, 0});
        if (radius > 0) {
            double step = 1.5;
            for (double r = Math.min(step, radius); r <= radius + 1e-9 && offsets.size() < 300; r += step) {
                int n = Math.max(6, (int) Math.round(r * 4));
                for (int i = 0; i < n && offsets.size() < 300; i++) {
                    double ang = 2 * Math.PI * i / n;
                    offsets.add(new double[]{Math.cos(ang) * r, Math.sin(ang) * r});
                }
            }
        }

        World w = p.getWorld();
        Location origin = eye.clone().add(dir.clone().multiply(1.5));
        Vector velocity = dir.clone().multiply(vel);
        List<Arrow> arrows = new ArrayList<>();
        for (double[] o : offsets) {
            Location l = origin.clone()
                    .add(right.clone().multiply(o[0]))
                    .add(up.clone().multiply(o[1]));
            Arrow a = w.spawn(l, Arrow.class, ar -> {
                ar.setShooter(p);
                ar.setGravity(false);
                ar.setCritical(true);
                ar.setPickupStatus(AbstractArrow.PickupStatus.DISALLOWED);
                ar.setVelocity(velocity);
            });
            arrows.add(a);
        }
        w.playSound(eye, "minecraft:entity.generic.explode", 2f, 1.6f);
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            for (Arrow a : arrows) if (a.isValid()) a.remove();
        }, 200L);
    }
}
