package me.gamma.koth.config;

import me.gamma.koth.KoTHPlugin;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class MessagesManager {

    private final KoTHPlugin plugin;
    private FileConfiguration messages;
    private File messagesFile;
    
    private boolean usePrefix;
    private String prefix;

    public MessagesManager(KoTHPlugin plugin) {
        this.plugin = plugin;
    }

    public void loadMessages() {
        messagesFile = new File(plugin.getDataFolder(), "messages.yml");
        if (!messagesFile.exists()) {
            plugin.saveResource("messages.yml", false);
        }
        
        messages = YamlConfiguration.loadConfiguration(messagesFile);
        
        try (InputStreamReader reader = new InputStreamReader(
                plugin.getResource("messages.yml"), StandardCharsets.UTF_8)) {
            YamlConfiguration defaultMessages = YamlConfiguration.loadConfiguration(reader);
            messages.setDefaults(defaultMessages);
            messages.options().copyDefaults(true);
            messages.save(messagesFile);
        } catch (Exception e) {
            plugin.getLogger().warning("Could not load default messages: " + e.getMessage());
        }
        
        loadValues();
    }

    public void reloadMessages() {
        loadMessages();
    }

    private void loadValues() {
        usePrefix = messages.getBoolean("USE_PREFIX", true);
        prefix = colorize(messages.getString("PREFIX", "&3&lKoTH &8» "));
    }

    public String getMessage(String path) {
        String message = messages.getString(path, "&cMessage not found: " + path);
        return applyPrefix(message);
    }

    public String getMessageNoPrefix(String path) {
        String message = messages.getString(path, "&cMessage not found: " + path);
        return colorize(message);
    }

    public List<String> getMessageList(String path) {
        List<String> messageList = messages.getStringList(path);
        List<String> result = new ArrayList<>();
        for (String message : messageList) {
            result.add(applyPrefix(message));
        }
        return result;
    }

    public String getMessage(String path, String... replacements) {
        String message = getMessage(path);
        return applyReplacements(message, replacements);
    }

    public String getMessageNoPrefix(String path, String... replacements) {
        String message = getMessageNoPrefix(path);
        return applyReplacements(message, replacements);
    }

    public List<String> getMessageList(String path, String... replacements) {
        List<String> messageList = getMessageList(path);
        List<String> result = new ArrayList<>();
        for (String message : messageList) {
            String processed = applyReplacements(message, replacements);
            result.add(processed);
        }
        return result;
    }
    
    private String applyPrefix(String message) {
        String coloredMessage = colorize(message);
        if (coloredMessage.contains("%prefix%")) {
            coloredMessage = coloredMessage.replace("%prefix%", prefix);
        }
        return coloredMessage;
    }

    private String applyReplacements(String text, String[] replacements) {
        if (replacements.length % 2 != 0) return text;
        for (int i = 0; i < replacements.length; i += 2) {
            text = text.replace(replacements[i], replacements[i + 1]);
        }
        return text;
    }

    public static String colorize(String text) {
        if (text == null) return "";
        return ChatColor.translateAlternateColorCodes('&', text);
    }

    public String getPrefix() {
        return prefix;
    }

    public boolean isUsePrefix() {
        return usePrefix;
    }
}