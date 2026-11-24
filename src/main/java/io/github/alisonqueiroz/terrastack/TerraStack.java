package io.github.alisonqueiroz.terrastack;

import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * TerraStack - Terraria-style quick stack.
 * <ul>
 *   <li>/qs [radius] - deposit main-inventory items (never hotbar, armor or off hand) into nearby
 *       containers that already hold a similar item</li>
 *   <li>/qs keep &lt;item|hand&gt; [amount|all] - per-player minimum that always stays in the inventory</li>
 *   <li>/qs reload</li>
 * </ul>
 */
public final class TerraStack extends JavaPlugin {

    private Messages messages;
    private KeepStore keep;
    private QuickStacker stacker;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        messages = new Messages(this);
        reloadSettings();
        keep = new KeepStore(this);
        stacker = new QuickStacker(this);
    }

    private void reloadSettings() {
        reloadConfig();
        messages.reload(this);
    }

    private int maxRadius() {
        return Math.max(1, getConfig().getInt("max-radius", 32));
    }

    private int defaultRadius() {
        return Math.max(1, Math.min(maxRadius(), getConfig().getInt("radius", 16)));
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length > 0 && args[0].equalsIgnoreCase("reload")) {
            if (!sender.hasPermission("terrastack.admin")) {
                messages.send(sender, "no-permission");
                return true;
            }
            reloadSettings();
            messages.send(sender, "reloaded");
            return true;
        }
        if (!(sender instanceof Player player)) {
            messages.sendPlain(sender, "players-only");
            return true;
        }
        if (!player.hasPermission("terrastack.use")) {
            messages.send(player, "no-permission");
            return true;
        }
        if (args.length > 0 && isKeep(args[0])) {
            keepCommand(player, args);
            return true;
        }
        // /qs [radius] - non-numeric arguments (e.g. "/qs stack") are ignored
        int radius = defaultRadius();
        for (String a : args) {
            try {
                radius = Math.max(1, Math.min(maxRadius(), Integer.parseInt(a)));
                break;
            } catch (NumberFormatException ignored) {
                // not a number
            }
        }
        QuickStacker.Result r = stacker.run(player, radius, keep.get(player.getUniqueId()));
        if (!r.anyContainer()) messages.send(player, "no-chests", "radius", radius);
        else if (r.items() == 0) messages.send(player, "nothing", "radius", radius);
        else messages.send(player, "stored", "items", r.items(), "chests", r.chests(), "radius", radius);
        return true;
    }

    private static boolean isKeep(String a) {
        return a.equalsIgnoreCase("keep") || a.equalsIgnoreCase("manter"); // "manter": pre-1.5 alias
    }

    private static boolean isHand(String a) {
        return a.equalsIgnoreCase("hand") || a.equalsIgnoreCase("mao") || a.equalsIgnoreCase("mão");
    }

    private static String pretty(Material m) {
        return m.getKey().getKey().replace('_', ' ');
    }

    private void keepCommand(Player p, String[] args) {
        if (args.length == 1) {
            Map<Material, Integer> cur = keep.get(p.getUniqueId());
            if (cur.isEmpty()) {
                messages.send(p, "keep-none");
                return;
            }
            messages.send(p, "keep-header");
            cur.forEach((m, n) -> messages.sendPlain(p, "keep-line",
                    "amount", n >= KeepStore.ALL ? messages.raw("keep-all-word") : n + "x", "item", pretty(m)));
            messages.sendPlain(p, "keep-footer");
            return;
        }
        Material mat;
        if (isHand(args[1])) {
            ItemStack inHand = p.getInventory().getItemInMainHand();
            if (inHand.getType().isAir()) {
                messages.send(p, "keep-hold-item");
                return;
            }
            mat = inHand.getType();
        } else {
            mat = Material.matchMaterial(args[1]);
            if (mat == null || !mat.isItem()) {
                messages.send(p, "keep-unknown-item", "input", args[1]);
                return;
            }
        }
        int n = KeepStore.ALL; // no amount, "all" or "todos" = never store this item
        if (args.length >= 3 && !args[2].equalsIgnoreCase("all") && !args[2].equalsIgnoreCase("todos")) {
            try {
                n = Math.max(0, Math.min(KeepStore.ALL - 1, Integer.parseInt(args[2])));
            } catch (NumberFormatException e) {
                messages.send(p, "keep-bad-amount", "input", args[2]);
                return;
            }
        }
        keep.set(p.getUniqueId(), mat, n);
        if (n == 0) messages.send(p, "keep-removed", "item", pretty(mat));
        else if (n >= KeepStore.ALL) messages.send(p, "keep-set-all", "item", pretty(mat));
        else messages.send(p, "keep-set", "amount", n, "item", pretty(mat));
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> out = new ArrayList<>();
        if (args.length == 1) {
            List<String> opts = new ArrayList<>(List.of("keep", "8", "16", "24", "32"));
            if (sender.hasPermission("terrastack.admin")) opts.add("reload");
            for (String s : opts) if (s.startsWith(args[0].toLowerCase(Locale.ROOT))) out.add(s);
        } else if (args.length == 2 && isKeep(args[0])) {
            String pre = args[1].toLowerCase(Locale.ROOT);
            if ("hand".startsWith(pre)) out.add("hand");
            for (Material m : Material.values()) {
                if (out.size() > 60) break;
                if (m.isItem() && !m.isLegacy() && m.getKey().getKey().startsWith(pre)) out.add(m.getKey().getKey());
            }
        } else if (args.length == 3 && isKeep(args[0])) {
            for (String s : List.of("all", "1", "16", "64", "0")) if (s.startsWith(args[2].toLowerCase(Locale.ROOT))) out.add(s);
        }
        return out;
    }
}
