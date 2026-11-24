package io.github.alisonqueiroz.terrastack;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

/** Built-in language files (lang/*.yml) with optional per-key overrides from config.yml "messages". */
final class Messages {

    private final YamlConfiguration fallback;
    private YamlConfiguration lang;
    private ConfigurationSection overrides;

    Messages(JavaPlugin plugin) {
        fallback = load(plugin, "en");
    }

    void reload(JavaPlugin plugin) {
        String code = plugin.getConfig().getString("language", "en");
        YamlConfiguration l = load(plugin, code);
        if (l == null) {
            plugin.getLogger().warning("Unknown language '" + code + "', using en");
            l = fallback;
        }
        lang = l;
        overrides = plugin.getConfig().getConfigurationSection("messages");
    }

    private static YamlConfiguration load(JavaPlugin plugin, String code) {
        InputStream in = plugin.getResource("lang/" + code + ".yml");
        if (in == null) return null;
        return YamlConfiguration.loadConfiguration(new InputStreamReader(in, StandardCharsets.UTF_8));
    }

    String raw(String key) {
        String s = overrides != null ? overrides.getString(key) : null;
        if (s == null && lang != null) s = lang.getString(key);
        if (s == null && fallback != null) s = fallback.getString(key);
        return s == null ? key : s;
    }

    /** Message text with colors and {placeholder} values (pairs: name, value, name, value...). */
    String get(String key, Object... kv) {
        String s = raw(key);
        for (int i = 0; i + 1 < kv.length; i += 2) {
            s = s.replace("{" + kv[i] + "}", String.valueOf(kv[i + 1]));
        }
        return ChatColor.translateAlternateColorCodes('&', s);
    }

    void send(CommandSender to, String key, Object... kv) {
        to.sendMessage(get("prefix") + get(key, kv));
    }

    void sendPlain(CommandSender to, String key, Object... kv) {
        to.sendMessage(get(key, kv));
    }
}
