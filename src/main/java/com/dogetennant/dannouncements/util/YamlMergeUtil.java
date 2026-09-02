package com.dogetennant.dannouncements.util;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Keeps an on-disk YAML file (config.yml) in sync with new keys shipped in a plugin update,
 * without ever touching a line the admin already has. Whole new top-level sections are appended
 * verbatim (comments included); a new key added inside a section the admin already has on disk
 * is too risky to splice in position-correctly, so it's only logged.
 */
public final class YamlMergeUtil {

    private YamlMergeUtil() {}

    public static void mergeMissingKeys(File diskFile, String resourcePath, Plugin plugin, String logLabel) {
        List<String> bundledLines = readResourceLines(plugin, resourcePath);
        if (bundledLines == null) return;
        mergeMissingKeys(diskFile, bundledLines, logLabel);
    }

    static void mergeMissingKeys(File diskFile, List<String> bundledLines, String logLabel) {
        if (!diskFile.exists()) return;

        YamlConfiguration bundled = YamlConfiguration.loadConfiguration(
                new StringReader(String.join("\n", bundledLines)));
        YamlConfiguration disk = YamlConfiguration.loadConfiguration(diskFile);

        Set<String> diskTop = disk.getKeys(false);
        List<String> newSections = new ArrayList<>();
        for (String key : bundled.getKeys(false)) {
            if (!diskTop.contains(key)) newSections.add(key);
        }

        warnAboutNestedGaps(bundled, disk, newSections, logLabel);
        appendNewSections(diskFile, bundledLines, newSections, logLabel);
    }

    private static void warnAboutNestedGaps(YamlConfiguration bundled, YamlConfiguration disk,
                                             List<String> newSections, String logLabel) {
        Set<String> diskDeep = disk.getKeys(true);
        for (String key : bundled.getKeys(true)) {
            if (diskDeep.contains(key)) continue;
            if (bundled.get(key) instanceof ConfigurationSection) continue;
            String top = key.contains(".") ? key.substring(0, key.indexOf('.')) : key;
            if (newSections.contains(top)) continue;

            LogUtil.warn("[" + logLabel + "] Missing option '" + key + "' (default: "
                    + bundled.get(key) + ") - add it manually, see the bundled default for the full comment.");
        }
    }

    private static void appendNewSections(File diskFile, List<String> bundledLines,
                                           List<String> newSections, String logLabel) {
        if (newSections.isEmpty()) return;

        StringBuilder appended = new StringBuilder();
        appended.append("\n# ---- Added by update - review/adjust as needed ----\n");
        boolean any = false;
        for (String key : newSections) {
            String block = extractTopLevelBlock(bundledLines, key);
            if (block == null) continue;
            appended.append(block).append("\n\n");
            any = true;
        }
        if (!any) return;

        try {
            Files.write(diskFile.toPath(), appended.toString().getBytes(StandardCharsets.UTF_8),
                    StandardOpenOption.APPEND);
            LogUtil.info("[" + logLabel + "] Added new option(s) not present on disk: "
                    + String.join(", ", newSections) + " - review the appended block(s) at the end of the file.");
        } catch (IOException e) {
            LogUtil.severe("Failed to append new options to " + logLabel + ": " + e.getMessage());
        }
    }

    private static String extractTopLevelBlock(List<String> lines, String key) {
        int start = -1;
        for (int i = 0; i < lines.size(); i++) {
            if (lines.get(i).startsWith(key + ":")) { start = i; break; }
        }
        if (start == -1) return null;

        int leadStart = start;
        while (leadStart > 0 && lines.get(leadStart - 1).trim().startsWith("#")) {
            leadStart--;
        }

        int end = lines.size();
        for (int i = start + 1; i < lines.size(); i++) {
            String line = lines.get(i);
            if (line.isBlank()) continue;
            if (!Character.isWhitespace(line.charAt(0))) { end = i; break; }
        }

        return String.join("\n", lines.subList(leadStart, end)).stripTrailing();
    }

    private static List<String> readResourceLines(Plugin plugin, String resourcePath) {
        try (InputStream in = plugin.getResource(resourcePath)) {
            if (in == null) return null;
            return new String(in.readAllBytes(), StandardCharsets.UTF_8).lines().toList();
        } catch (IOException e) {
            return null;
        }
    }
}
