package me.gamma.koth.scoreboard;

import org.bukkit.ChatColor;

import java.util.List;
import java.util.Random;

public class AnimatedText {

    private final List<String> texts;
    private final boolean random;
    private final int interval;
    private String score;
    
    private final Random randomGenerator = new Random();

    public AnimatedText(List<String> texts, boolean random, int interval) {
        this.texts = texts;
        this.random = random;
        this.interval = Math.max(1, interval);
        this.score = "";
    }

    public String getFrame(long currentTick) {
        if (texts.isEmpty()) return "";
        if (texts.size() == 1) return colorize(texts.get(0));
        
        if (random) {
            int idx = randomGenerator.nextInt(texts.size());
            return colorize(texts.get(idx));
        } else {
            int frameIdx = (int) ((currentTick / interval) % texts.size());
            return colorize(texts.get(frameIdx));
        }
    }

    public String getCurrentText() {
        if (texts.isEmpty()) return "";
        return colorize(texts.get(0));
    }

    public String getCurrentTextWithPlaceholders(java.util.Map<String, String> placeholders) {
        String text = getCurrentText();
        for (java.util.Map.Entry<String, String> entry : placeholders.entrySet()) {
            text = text.replace(entry.getKey(), entry.getValue());
        }
        return text;
    }

    private static String colorize(String text) {
        if (text == null) return "";
        return ChatColor.translateAlternateColorCodes('&', text);
    }

    public List<String> getTexts() {
        return texts;
    }

    public boolean isRandom() {
        return random;
    }

    public int getInterval() {
        return interval;
    }

    public String getScore() {
        return score;
    }

    public void setScore(String score) {
        this.score = score;
    }
}