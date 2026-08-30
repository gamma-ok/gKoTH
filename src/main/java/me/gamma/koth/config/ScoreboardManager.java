package me.gamma.koth.config;

import me.gamma.koth.KoTHPlugin;
import me.gamma.koth.scoreboard.AnimatedText;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class ScoreboardManager {

    private final KoTHPlugin plugin;
    private FileConfiguration scoreboardConfig;
    private File scoreboardFile;

    private Map<String, AnimatedText> lines = new LinkedHashMap<>();

    public ScoreboardManager(KoTHPlugin plugin) {
        this.plugin = plugin;
    }

    public void loadScoreboard() {
        scoreboardFile = new File(plugin.getDataFolder(), "scoreboard.yml");
        if (!scoreboardFile.exists()) {
            plugin.saveResource("scoreboard.yml", false);
        }

        scoreboardConfig = YamlConfiguration.loadConfiguration(scoreboardFile);

        loadLines();
    }

    public void reloadScoreboard() {
        loadScoreboard();
    }

    private void loadLines() {
        lines.clear();

        // Cargar título
        loadAnimatedLine("display.title");

        // Cargar líneas
        for (int i = 1; i <= 15; i++) {
            loadAnimatedLine("display.line-" + i);
        }
    }

    private void loadAnimatedLine(String path) {
        if (!scoreboardConfig.contains(path)) return;

        List<String> texts = scoreboardConfig.getStringList(path + ".text");
        boolean random = scoreboardConfig.getBoolean(path + ".random", false);
        int interval = scoreboardConfig.getInt(path + ".interval", 60);
        String score = scoreboardConfig.getString(path + ".score", "");

        if (!texts.isEmpty()) {
            AnimatedText animatedText = new AnimatedText(texts, random, interval);
            lines.put(path, animatedText);
            if (!score.isEmpty()) {
                animatedText.setScore(score);
            }
        }
    }

    public Map<String, AnimatedText> getLines() {
        return lines;
    }

    public AnimatedText getLine(String path) {
        return lines.get(path);
    }

    public FileConfiguration getScoreboardConfig() {
        return scoreboardConfig;
    }
}