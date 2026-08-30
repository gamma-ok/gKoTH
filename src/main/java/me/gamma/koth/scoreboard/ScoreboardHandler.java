package me.gamma.koth.scoreboard;

import me.gamma.koth.KoTHPlugin;
import me.gamma.koth.config.MessagesManager;
import me.gamma.koth.config.ScoreboardManager;
import me.gamma.koth.koth.KoTH;
import me.gamma.koth.koth.KoTHManager;
import me.gamma.koth.utils.TimeUtils;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.*;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class ScoreboardHandler {

    private final KoTHPlugin plugin;
    private final ScoreboardManager scoreboardManager;

    private final Map<UUID, Scoreboard> playerScoreboards = new ConcurrentHashMap<>();
    private final Map<UUID, String[]> lastTexts = new ConcurrentHashMap<>();
    private final Map<UUID, String> lastTitles = new ConcurrentHashMap<>();
    private final Map<UUID, String[]> currentEntries = new ConcurrentHashMap<>();
    private final Map<UUID, Integer> boardLineCounts = new ConcurrentHashMap<>();

    private long tick = 0;

    private static final int MAX_LINES = 16;
    private static final String[] TEAM_NAMES = new String[MAX_LINES];
    private static final String[] MARKERS = new String[MAX_LINES];

    private static final int PREFIX_MAX = 16;
    private static final int SUFFIX_MAX = 16;
    private static final int ENTRY_MAX = 40;

    static {
        for (int i = 0; i < MAX_LINES; i++) {
            TEAM_NAMES[i] = "koth" + i;
            MARKERS[i] = "\u00a7" + Integer.toHexString(i) + "\u00a7r";
        }
    }

    public ScoreboardHandler(KoTHPlugin plugin) {
        this.plugin = plugin;
        this.scoreboardManager = plugin.getScoreboardManager();
    }

    public void updateScoreboards() {
        tick++;

        if (!plugin.getConfigManager().isScoreboardEnabled()) {
            clearAllScoreboards();
            return;
        }

        KoTHManager kothManager = plugin.getKothManager();

        if (!kothManager.hasActiveKoTHs()) {
            clearAllScoreboards();
            return;
        }

        KoTH activeKoTH = kothManager.getMostRecentActiveKoTH();
        if (activeKoTH == null) {
            clearAllScoreboards();
            return;
        }

        List<String> lineKeys = getLineKeys();
        int lineCount = Math.min(lineKeys.size(), MAX_LINES);

        for (Player player : Bukkit.getOnlinePlayers()) {
            updatePlayerScoreboard(player, activeKoTH, lineKeys, lineCount);
        }
    }

    private void updatePlayerScoreboard(Player player, KoTH activeKoTH, List<String> lineKeys, int lineCount) {
        UUID uuid = player.getUniqueId();

        Scoreboard board = playerScoreboards.get(uuid);
        Integer existingLineCount = boardLineCounts.get(uuid);

        if (board == null || existingLineCount == null || existingLineCount != lineCount) {
            board = rebuildScoreboard(player, lineKeys, lineCount);
            if (board == null) return;
        }

        Objective obj = board.getObjective("koth");
        if (obj == null) return;

        Map<String, String> placeholders = buildPlaceholders(activeKoTH);

        // Actualizar título (límite real de 32, es el nombre del objective, no cambia)
        AnimatedText titleText = scoreboardManager.getLine("display.title");
        if (titleText != null) {
            String title = titleText.getFrame(tick);
            title = applyPlaceholders(title, placeholders);
            title = MessagesManager.colorize(title);
            title = safeTruncate(title, 32);

            String lastTitle = lastTitles.getOrDefault(uuid, "");
            if (!title.equals(lastTitle)) {
                obj.setDisplayName(title);
                lastTitles.put(uuid, title);
            }
        }

        // Actualizar líneas
        String[] last = lastTexts.get(uuid);
        String[] entries = currentEntries.get(uuid);
        if (last == null || entries == null) return;

        for (int i = 0; i < lineCount; i++) {
            AnimatedText line = scoreboardManager.getLine("display." + lineKeys.get(i));
            if (line == null) continue;

            String text = line.getFrame(tick);
            text = applyPlaceholders(text, placeholders);
            text = MessagesManager.colorize(text);

            if (!text.equals(last[i])) {
                last[i] = text;
                applyLineText(board, obj, i, text, entries, getLineScore(line, lineCount - i));
            }
        }
    }

    private void applyLineText(Scoreboard board, Objective obj, int lineIndex, String text,
                                String[] entries, int score) {
        Team team = board.getTeam(TEAM_NAMES[lineIndex]);
        if (team == null) return;

        String marker = MARKERS[lineIndex];
        String[] split = splitTextForLine(text, marker);
        String newPrefix = split[0];
        String newEntry = split[1];
        String newSuffix = split[2];

        String oldEntry = entries[lineIndex];

        if (oldEntry == null) {
            // Primera vez que se escribe esta línea para este jugador.
            team.addEntry(newEntry);
        } else if (!oldEntry.equals(newEntry)) {
            // El contenido visible cambió lo suficiente como para necesitar un entry distinto.
            team.removeEntry(oldEntry);
            board.resetScores(oldEntry);
            team.addEntry(newEntry);
        }

        team.setPrefix(newPrefix);
        team.setSuffix(newSuffix);

        if (oldEntry == null || !oldEntry.equals(newEntry)) {
            obj.getScore(newEntry).setScore(score);
        }

        entries[lineIndex] = newEntry;
    }

    private String[] splitTextForLine(String text, String marker) {
        if (text == null) text = "";

        int entryVisibleBudget = ENTRY_MAX - marker.length(); // normalmente 36

        // Caso 1: el texto completo cabe dentro del entry (con marcador). No hace falta
        // usar prefix ni suffix -> menos llamadas a la API, menos overhead.
        if (text.length() <= entryVisibleBudget) {
            return new String[] { "", marker + text, "" };
        }

        // Caso 2: hace falta usar el prefix para la primera parte del texto.
        int prefixSplit = Math.min(PREFIX_MAX, text.length());
        if (prefixSplit > 0 && text.charAt(prefixSplit - 1) == '\u00a7') {
            prefixSplit--; // evita dejar un '§' suelto al final del prefix
        }

        String prefix = text.substring(0, prefixSplit);
        String afterPrefix = text.substring(prefixSplit);

        String carryToEntry = afterPrefix.startsWith("\u00a7") ? "" : getLastColorCode(prefix);
        String entryContent = carryToEntry + afterPrefix;

        if (entryContent.length() <= entryVisibleBudget) {
            // Cabe todo en prefix + entry, no hace falta suffix.
            return new String[] { prefix, marker + entryContent, "" };
        }

        // Caso 3: hace falta el suffix también (texto muy largo, cerca del máximo real).
        String entryVisiblePart = safeTruncate(entryContent, entryVisibleBudget);
        String afterEntry = entryContent.substring(entryVisiblePart.length());

        String carryToSuffix = afterEntry.startsWith("\u00a7") ? "" : getLastColorCode(entryVisiblePart);
        String suffix = safeTruncate(carryToSuffix + afterEntry, SUFFIX_MAX);

        return new String[] { prefix, marker + entryVisiblePart, suffix };
    }

    private String safeTruncate(String text, int maxLength) {
        if (text == null || text.isEmpty() || maxLength <= 0) return "";
        if (text.length() <= maxLength) return text;

        String truncated = text.substring(0, maxLength);

        // Si termina con un código de color incompleto, cortar un carácter más
        if (truncated.endsWith("\u00a7")) {
            truncated = truncated.substring(0, maxLength - 1);
        }

        return truncated;
    }

    private String getLastColorCode(String text) {
        String lastColor = "";
        for (int i = text.length() - 2; i >= 0; i--) {
            if (text.charAt(i) == '\u00a7' && i + 1 < text.length()) {
                char code = text.charAt(i + 1);
                // Solo códigos de color (0-9, a-f)
                if ((code >= '0' && code <= '9') || (code >= 'a' && code <= 'f') ||
                    (code >= 'A' && code <= 'F')) {
                    lastColor = "\u00a7" + code;
                    break;
                }
            }
        }
        return lastColor;
    }

    private int getLineScore(AnimatedText line, int fallback) {
        String scoreStr = line.getScore();
        try {
            return Integer.parseInt(scoreStr);
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    private Scoreboard rebuildScoreboard(Player player, List<String> lineKeys, int lineCount) {
        UUID uuid = player.getUniqueId();

        // Si ya había un board viejo, lo desmontamos por completo primero.
        Scoreboard oldBoard = playerScoreboards.get(uuid);
        if (oldBoard != null) {
            for (Team team : new ArrayList<>(oldBoard.getTeams())) {
                for (String entry : new ArrayList<>(team.getEntries())) {
                    oldBoard.resetScores(entry);
                }
                team.unregister();
            }
            Objective oldObj = oldBoard.getObjective("koth");
            if (oldObj != null) {
                oldObj.unregister();
            }
        }

        playerScoreboards.remove(uuid);
        lastTexts.remove(uuid);
        lastTitles.remove(uuid);
        currentEntries.remove(uuid);
        boardLineCounts.remove(uuid);

        return createScoreboard(player, lineKeys, lineCount);
    }

    private Scoreboard createScoreboard(Player player, List<String> lineKeys, int lineCount) {
        UUID uuid = player.getUniqueId();
        Scoreboard board = Bukkit.getScoreboardManager().getNewScoreboard();

        Objective obj = board.registerNewObjective("koth", "dummy");
        obj.setDisplaySlot(DisplaySlot.SIDEBAR);

        String[] entries = new String[lineCount];

        for (int i = 0; i < lineCount; i++) {
            String teamName = TEAM_NAMES[i];
            Team team = board.registerNewTeam(teamName);

            String initialEntry = MARKERS[i];
            team.addEntry(initialEntry);
            entries[i] = initialEntry;

            AnimatedText line = scoreboardManager.getLine("display." + lineKeys.get(i));
            int score = line != null ? getLineScore(line, lineCount - i) : (lineCount - i);
            obj.getScore(initialEntry).setScore(score);
        }

        playerScoreboards.put(uuid, board);
        lastTexts.put(uuid, new String[lineCount]);
        lastTitles.put(uuid, "");
        currentEntries.put(uuid, entries);
        boardLineCounts.put(uuid, lineCount);
        player.setScoreboard(board);

        return board;
    }

    private List<String> getLineKeys() {
        List<String> keys = new ArrayList<>();
        for (int i = 1; i <= 15; i++) {
            if (scoreboardManager.getLine("display.line-" + i) != null) {
                keys.add("line-" + i);
            }
        }
        return keys;
    }

    private Map<String, String> buildPlaceholders(KoTH activeKoTH) {
        Map<String, String> placeholders = new HashMap<>();

        placeholders.put("%koth%", activeKoTH.getName());

        if (activeKoTH.getCenter() != null) {
            placeholders.put("%x%", String.valueOf((int) activeKoTH.getCenter().getX()));
            placeholders.put("%y%", String.valueOf((int) activeKoTH.getCenter().getY()));
            placeholders.put("%z%", String.valueOf((int) activeKoTH.getCenter().getZ()));
        } else {
            placeholders.put("%x%", "N/A");
            placeholders.put("%y%", "N/A");
            placeholders.put("%z%", "N/A");
        }

        if (activeKoTH.isBeingCaptured()) {
            Player capturer = activeKoTH.getCurrentCapturer();
            if (capturer != null && capturer.isOnline()) {
                placeholders.put("%player%", capturer.getName());

                if (plugin.getClanHook().isEnabled()) {
                    placeholders.putAll(plugin.getClanHook().getClanPlaceholders(capturer));
                }
            } else {
                placeholders.put("%player%", "N/A");
                placeholders.put("%gclan_name%", "N/A");
                placeholders.put("%gclan_display%", "N/A");
                placeholders.put("%gclan_name_raw%", "N/A");
                placeholders.put("%gclan_tag%", "N/A");
            }

            placeholders.put("%koth_time%", TimeUtils.formatTime(activeKoTH.getCaptureTimeLeft()));
            placeholders.put("%koth_max_time%", TimeUtils.formatTime(activeKoTH.getMaxTimeLeft()));
        } else {
            placeholders.put("%player%", "N/A");
            placeholders.put("%gclan_name%", "N/A");
            placeholders.put("%gclan_display%", "N/A");
            placeholders.put("%gclan_name_raw%", "N/A");
            placeholders.put("%gclan_tag%", "N/A");
            placeholders.put("%koth_time%", TimeUtils.formatTime(activeKoTH.getCaptureTime()));
            placeholders.put("%koth_max_time%", TimeUtils.formatTime(activeKoTH.getMaxTimeLeft()));
        }

        return placeholders;
    }

    private String applyPlaceholders(String text, Map<String, String> placeholders) {
        for (Map.Entry<String, String> entry : placeholders.entrySet()) {
            text = text.replace(entry.getKey(), entry.getValue());
        }
        return text;
    }

    public void clearAllScoreboards() {
        for (UUID uuid : playerScoreboards.keySet()) {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null && player.isOnline()) {
                player.setScoreboard(Bukkit.getScoreboardManager().getMainScoreboard());
            }
        }
        playerScoreboards.clear();
        lastTexts.clear();
        lastTitles.clear();
        currentEntries.clear();
        boardLineCounts.clear();
    }

    public void clearPlayerScoreboard(Player player) {
        UUID uuid = player.getUniqueId();
        playerScoreboards.remove(uuid);
        lastTexts.remove(uuid);
        lastTitles.remove(uuid);
        currentEntries.remove(uuid);
        boardLineCounts.remove(uuid);
        player.setScoreboard(Bukkit.getScoreboardManager().getMainScoreboard());
    }

    public void cleanup() {
        clearAllScoreboards();
    }
}