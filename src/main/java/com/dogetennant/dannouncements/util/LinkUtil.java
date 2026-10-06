package com.dogetennant.dannouncements.util;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Turns URLs and command shortcuts typed into an announcement line into clickable MiniMessage tags. */
public final class LinkUtil {

    // [label](target) - the label is what's shown/clickable, target decides what clicking does:
    //   tp:world,x,y,z[,yaw,pitch]  -> teleports the clicker via /da tp (self-only, and only to
    //                                  destinations published here - see TpDestinations)
    //   cmd:/some command           -> runs the command as the clicking player (their own permissions apply)
    //   anything else               -> treated as a URL (open_url), with https:// assumed if no scheme given
    private static final Pattern MARKDOWN_LINK = Pattern.compile("\\[([^\\]]+)]\\(([^)]+)\\)");

    // A literal /da tp written straight into a line. Lines carrying a hand-authored <click:...>
    // tag skip autoLinkify() entirely, so those destinations have to be read out of the raw text.
    private static final Pattern LITERAL_TP_COMMAND =
            Pattern.compile("(?i)/(?:da|dannouncements)\\s+tp\\s+([^'\"<>)]+)");

    private static final Pattern URL_PATTERN =
            Pattern.compile("(?i)\\b((?:https?://|www\\.)[\\w\\-._~:/?#\\[\\]@!$&'()*+,;=%]+)");
    private static final String TRAILING_PUNCTUATION = ".,!?;:)]}>'\"";

    private LinkUtil() {}

    /**
     * Resolves links/commands in an announcement line, trying two forms in order:
     *  1. Markdown-style [label](target) - see the tp:/cmd:/URL forms above.
     *  2. Bare URLs (http://, https://, www.) - auto-wrapped as clickable text as-is.
     * Skipped entirely if the line already contains a manual <click:...> tag, so hand-authored
     * MiniMessage tags are never double-wrapped.
     */
    public static String autoLinkify(String line) {
        if (line == null || line.isEmpty()) return line;
        if (containsClickTag(line)) return line;

        String withMarkdownLinks = linkifyMarkdown(line);
        if (containsClickTag(withMarkdownLinks)) return withMarkdownLinks;

        return linkifyBareUrls(withMarkdownLinks);
    }

    /**
     * Every teleport destination this line offers, in the same "world x y z [yaw pitch]" form
     * autoLinkify() hands to /da tp. Covers both tp: links and hand-written /da tp commands, so
     * that whatever a player can click, TpDestinations can recognise - and nothing else.
     */
    public static List<String> extractTpTargets(String line) {
        if (line == null || line.isEmpty()) return List.of();

        List<String> targets = new ArrayList<>();

        Matcher markdown = MARKDOWN_LINK.matcher(line);
        while (markdown.find()) {
            String target = markdown.group(2).trim();
            if (target.regionMatches(true, 0, "tp:", 0, 3)) {
                targets.add(normalizeCoords(target.substring(3)));
            }
        }

        Matcher literal = LITERAL_TP_COMMAND.matcher(line);
        while (literal.find()) {
            targets.add(normalizeCoords(literal.group(1)));
        }

        return targets;
    }

    private static boolean containsClickTag(String line) {
        return line.toLowerCase(Locale.ROOT).contains("<click:");
    }

    private static String linkifyMarkdown(String line) {
        Matcher m = MARKDOWN_LINK.matcher(line);
        if (!m.find()) return line;
        m.reset();

        StringBuilder sb = new StringBuilder();
        int last = 0;
        while (m.find()) {
            String label = m.group(1);
            String target = m.group(2).trim();

            sb.append(line, last, m.start());
            appendMarkdownTarget(sb, target, label);
            last = m.end();
        }
        sb.append(line.substring(last));
        return sb.toString();
    }

    private static void appendMarkdownTarget(StringBuilder sb, String target, String label) {
        if (target.regionMatches(true, 0, "tp:", 0, 3)) {
            String coords = normalizeCoords(target.substring(3));
            appendRunCommandTag(sb, "/da tp " + coords, "<gray>Click to teleport", label);
        } else if (target.regionMatches(true, 0, "cmd:", 0, 4)) {
            String command = target.substring(4).trim();
            if (!command.startsWith("/")) command = "/" + command;
            appendRunCommandTag(sb, command, "<gray>Click to run command", label);
        } else {
            appendOpenUrlTag(sb, normalizeUrl(target), label);
        }
    }

    /** "world, 0.5, 65, 0.5" and "world 0.5 65 0.5" have to end up as the same destination. */
    private static String normalizeCoords(String raw) {
        return raw.trim().replace(",", " ").replaceAll("\\s+", " ");
    }

    private static String linkifyBareUrls(String line) {
        Matcher m = URL_PATTERN.matcher(line);
        if (!m.find()) return line;
        m.reset();

        StringBuilder sb = new StringBuilder();
        int last = 0;
        while (m.find()) {
            String raw = m.group(1);
            String trimmed = raw;
            StringBuilder trailing = new StringBuilder();
            while (!trimmed.isEmpty()
                    && TRAILING_PUNCTUATION.indexOf(trimmed.charAt(trimmed.length() - 1)) >= 0) {
                trailing.insert(0, trimmed.charAt(trimmed.length() - 1));
                trimmed = trimmed.substring(0, trimmed.length() - 1);
            }
            if (trimmed.isEmpty()) continue;

            sb.append(line, last, m.start());
            appendOpenUrlTag(sb, normalizeUrl(trimmed), trimmed);
            sb.append(trailing);
            last = m.end();
        }
        sb.append(line.substring(last));
        return sb.toString();
    }

    private static void appendOpenUrlTag(StringBuilder sb, String url, String label) {
        sb.append("<click:open_url:'").append(url).append("'>")
          .append("<hover:show_text:'<gray>Click to open link'>")
          .append("<u>").append(label).append("</u>")
          .append("</hover></click>");
    }

    private static void appendRunCommandTag(StringBuilder sb, String command, String hoverText, String label) {
        sb.append("<click:run_command:'").append(escapeQuoted(command)).append("'>")
          .append("<hover:show_text:'").append(hoverText).append("'>")
          .append("<u>").append(label).append("</u>")
          .append("</hover></click>");
    }

    private static String normalizeUrl(String url) {
        boolean hasScheme = url.matches("(?i)^[a-z][a-z0-9+.\\-]*://.*");
        String withScheme = hasScheme ? url : "https://" + url;
        return escapeQuoted(withScheme);
    }

    private static String escapeQuoted(String s) {
        return s.replace("\\", "\\\\").replace("'", "\\'");
    }
}
