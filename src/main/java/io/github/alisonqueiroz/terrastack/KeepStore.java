package io.github.alisonqueiroz.terrastack;

import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.EnumMap;
import java.util.Map;
import java.util.UUID;

/** Per-player minimum amounts that /qs leaves in the inventory (keep.yml: uuid -> material -> amount). */
final class KeepStore {

    /** Stored amount meaning "never store this item". */
    static final int ALL = 1_000_000;

    private final JavaPlugin plugin;
    private final File file;
    private YamlConfiguration data;

    KeepStore(JavaPlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "keep.yml");
        File legacy = new File(plugin.getDataFolder(), "manter.yml"); // pre-1.5 name
        if (!file.exists() && legacy.exists() && !legacy.renameTo(file)) {
            plugin.getLogger().warning("Could not rename manter.yml to keep.yml");
        }
        data = YamlConfiguration.loadConfiguration(file);
    }

    Map<Material, Integer> get(UUID player) {
        Map<Material, Integer> m = new EnumMap<>(Material.class);
        ConfigurationSection sec = data.getConfigurationSection(player.toString());
        if (sec == null) return m;
        for (String k : sec.getKeys(false)) {
            Material mat = Material.matchMaterial(k);
            int n = sec.getInt(k, 0);
            if (mat != null && n > 0) m.put(mat, n);
        }
        return m;
    }

    void set(UUID player, Material mat, int amount) {
        data.set(player + "." + mat.getKey().getKey(), amount <= 0 ? null : amount);
        try {
            data.save(file);
        } catch (IOException e) {
            plugin.getLogger().warning("Could not save keep.yml: " + e.getMessage());
        }
    }
}
