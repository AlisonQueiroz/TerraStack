package io.github.alisonqueiroz.terrastack;

import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.block.Container;
import org.bukkit.entity.Display;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.util.Transformation;
import org.joml.AxisAngle4f;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** The quick stack itself: find nearby containers, move matching items, animate. */
final class QuickStacker {

    private static final int[] KEPT_SLOTS = {0, 1, 2, 3, 4, 5, 6, 7, 8, 40}; // hotbar + off hand
    private static final int MAIN_FIRST = 9, MAIN_LAST = 35;                 // main inventory

    private final TerraStack plugin;

    QuickStacker(TerraStack plugin) {
        this.plugin = plugin;
    }

    record Result(int items, int chests, boolean anyContainer) { }

    private record Flight(ItemStack item, Location target) { }

    Result run(Player player, int radius, Map<Material, Integer> keep) {
        List<Inventory> containers = nearbyContainers(player, radius);
        if (containers.isEmpty()) return new Result(0, 0, false);

        PlayerInventory pinv = player.getInventory();
        // what is already in the hotbar / off hand counts toward the minimums
        for (int s : KEPT_SLOTS) {
            ItemStack it = pinv.getItem(s);
            if (it != null && keep.containsKey(it.getType())) {
                keep.put(it.getType(), Math.max(0, keep.get(it.getType()) - it.getAmount()));
            }
        }

        int moved = 0;
        Map<Inventory, Boolean> touched = new IdentityHashMap<>();
        List<Flight> flights = new ArrayList<>();
        int maxFlights = plugin.getConfig().getBoolean("animation.enabled", true)
                ? plugin.getConfig().getInt("animation.max-items", 24) : 0;

        for (int slot = MAIN_FIRST; slot <= MAIN_LAST; slot++) {
            ItemStack item = pinv.getItem(slot);
            if (item == null || item.getType().isAir()) continue;
            int reserve = Math.min(item.getAmount(), keep.getOrDefault(item.getType(), 0));
            if (reserve > 0) keep.put(item.getType(), keep.get(item.getType()) - reserve);
            if (reserve >= item.getAmount()) continue;

            ItemStack kept = null;
            if (reserve > 0) {
                kept = item.clone();
                kept.setAmount(reserve);
                item = item.clone();
                item.setAmount(item.getAmount() - reserve);
            }
            final int movable = item.getAmount();
            ItemStack remaining = item;
            for (Inventory inv : containers) {
                if (!containsSimilar(inv, remaining)) continue;
                Map<Integer, ItemStack> left = inv.addItem(remaining.clone());
                int notStored = left.values().stream().mapToInt(ItemStack::getAmount).sum();
                if (notStored < remaining.getAmount()) {
                    touched.put(inv, true);
                    if (inv.getLocation() != null && flights.size() < maxFlights) {
                        ItemStack shown = remaining.clone();
                        shown.setAmount(1);
                        flights.add(new Flight(shown, inv.getLocation()));
                    }
                }
                if (notStored == 0) { remaining = null; break; }
                remaining = remaining.clone();
                remaining.setAmount(notStored);
            }
            int left = remaining == null ? 0 : remaining.getAmount();
            int stored = movable - left;
            if (stored > 0) {
                int stay = left + (kept == null ? 0 : kept.getAmount());
                if (stay == 0) {
                    pinv.setItem(slot, null);
                } else {
                    ItemStack st = (kept != null ? kept : remaining).clone();
                    st.setAmount(stay);
                    pinv.setItem(slot, st);
                }
                moved += stored;
            }
        }

        if (moved > 0) {
            int delay = 0;
            for (Flight f : flights) {
                Bukkit.getScheduler().runTaskLater(plugin, () -> fly(player, f), delay);
                delay += 2;
            }
            player.playSound(player.getLocation(), Sound.ENTITY_ITEM_PICKUP, 0.6f, 1.2f);
        }
        return new Result(moved, touched.size(), true);
    }

    private List<Inventory> nearbyContainers(Player player, int radius) {
        Location loc = player.getLocation();
        World world = loc.getWorld();
        double r2 = (double) radius * radius;
        boolean shulkers = plugin.getConfig().getBoolean("include-shulker-boxes", false);
        boolean protection = plugin.getConfig().getBoolean("respect-protection", true)
                && !player.hasPermission("terrastack.bypass-protection");
        Map<Inventory, Boolean> seen = new IdentityHashMap<>();
        Set<String> seenLoc = new HashSet<>();
        List<Inventory> result = new ArrayList<>();
        for (int cx = (loc.getBlockX() - radius) >> 4; cx <= (loc.getBlockX() + radius) >> 4; cx++) {
            for (int cz = (loc.getBlockZ() - radius) >> 4; cz <= (loc.getBlockZ() + radius) >> 4; cz++) {
                if (!world.isChunkLoaded(cx, cz)) continue;
                Chunk chunk = world.getChunkAt(cx, cz);
                for (BlockState state : chunk.getTileEntities(false)) {
                    if (!(state instanceof Container container)) continue;
                    if (state.getLocation().add(0.5, 0.5, 0.5).distanceSquared(loc) > r2) continue;
                    if (!isStorage(state.getType(), shulkers)) continue;
                    Inventory inv = container.getInventory();
                    if (seen.containsKey(inv)) continue;
                    Location il = inv.getLocation();
                    String key = il == null ? null
                            : il.getBlockX() + "," + il.getBlockY() + "," + il.getBlockZ() + "," + inv.getSize();
                    if (key != null && seenLoc.contains(key)) continue;
                    if (protection && !canOpen(player, state.getBlock())) continue;
                    seen.put(inv, true);
                    if (key != null) seenLoc.add(key);
                    result.add(inv);
                }
            }
        }
        return result;
    }

    private static boolean isStorage(Material type, boolean shulkers) {
        String n = type.name();
        if (n.endsWith("SHULKER_BOX")) return shulkers;
        return n.endsWith("CHEST") || type == Material.BARREL; // chest, trapped chest, copper chests, barrel
    }

    /** Asks protection plugins whether the player could open this block (simulated right-click). */
    private static boolean canOpen(Player player, Block block) {
        PlayerInteractEvent e = new PlayerInteractEvent(player, Action.RIGHT_CLICK_BLOCK, null, block,
                BlockFace.UP, EquipmentSlot.HAND);
        Bukkit.getPluginManager().callEvent(e);
        return e.useInteractedBlock() != Event.Result.DENY;
    }

    private static boolean containsSimilar(Inventory inv, ItemStack item) {
        for (ItemStack s : inv.getStorageContents()) {
            if (s != null && s.isSimilar(item)) return true;
        }
        return false;
    }

    /** One item icon flies from the player to the chest (client-side interpolated teleport). */
    private void fly(Player player, Flight f) {
        if (!player.isOnline()) return;
        World w = player.getWorld();
        Location target = f.target().clone().add(0.5, 0.6, 0.5);
        if (target.getWorld() != w) return;
        int ticks = Math.max(2, plugin.getConfig().getInt("animation.flight-ticks", 8));
        ItemDisplay d = w.spawn(player.getLocation().add(0, 1.1, 0), ItemDisplay.class, e -> {
            e.setItemStack(f.item());
            e.setPersistent(false);
            e.setBillboard(Display.Billboard.CENTER);
            e.setTransformation(new Transformation(new Vector3f(), new AxisAngle4f(),
                    new Vector3f(0.45f, 0.45f, 0.45f), new AxisAngle4f()));
            e.setTeleportDuration(Math.min(59, ticks));
        });
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (d.isValid()) d.teleport(target);
        }, 2);
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (d.isValid()) d.remove();
            w.spawnParticle(Particle.HAPPY_VILLAGER, target.clone().add(0, 0.4, 0), 6, 0.25, 0.15, 0.25, 0);
            w.playSound(target, Sound.ENTITY_ITEM_PICKUP, 0.35f, 1.6f);
        }, ticks + 3L);
    }
}
