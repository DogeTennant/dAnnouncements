package com.dogetennant.dannouncements.command.subcommand;

import com.dogetennant.dannouncements.DAnnouncements;
import com.dogetennant.dannouncements.announcement.Announcement;
import com.dogetennant.dannouncements.command.CommandUtil;
import com.dogetennant.dannouncements.config.Messages;
import com.dogetennant.dannouncements.util.ColorUtil;
import org.bukkit.command.CommandSender;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.OptionalInt;
import java.util.function.Predicate;

public class LineSubCommand implements SubCommand {

    @Override public String getName() { return "line"; }
    @Override public String getPermission() { return "dannouncements.admin.line"; }
    @Override public String getUsage() { return "/da line <list|add|set|insert|remove> <id> [index] [text...]"; }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (args.length < 3) { CommandUtil.sendUsage(sender, getUsage()); return; }
        String action = args[1].toLowerCase();
        String id = args[2];

        var found = CommandUtil.requireAnnouncement(sender, id);
        if (found.isEmpty()) return;
        Announcement a = found.get();

        switch (action) {
            case "list" -> list(sender, a);
            case "add" -> add(sender, a, args);
            case "set" -> set(sender, a, args);
            case "insert" -> insert(sender, a, args);
            case "remove" -> remove(sender, a, args);
            default -> CommandUtil.sendUsage(sender, getUsage());
        }
    }

    private void list(CommandSender sender, Announcement a) {
        if (a.lines.isEmpty()) {
            sender.sendMessage(ColorUtil.parse("<gray>" + a.id + " has no lines yet."));
            return;
        }
        sender.sendMessage(ColorUtil.parse("<gold>" + a.id + " <gray>- " + a.lines.size() + " line(s)"));
        for (int i = 0; i < a.lines.size(); i++) {
            sender.sendMessage(ColorUtil.parse("<dark_gray>[" + (i + 1) + "] ").append(ColorUtil.parse(a.lines.get(i))));
        }
    }

    // Each change is made to the announcement as it is in the file now (see AnnouncementConfigLoader#update),
    // so the line numbers are checked against that.

    private void add(CommandSender sender, Announcement a, String[] args) {
        if (args.length < 4) { CommandUtil.sendUsage(sender, "/da line add <id> <text...>"); return; }
        String text = joinFrom(args, 3);
        if (!save(sender, a.id, now -> now.lines.add(text))) return;
        int count = DAnnouncements.getInstance().getAnnouncementConfigLoader().get(a.id).map(now -> now.lines.size()).orElse(0);
        sender.sendMessage(ColorUtil.parse(Messages.get("line-added",
                Map.of("index", String.valueOf(count), "id", a.id))));
    }

    private void set(CommandSender sender, Announcement a, String[] args) {
        if (args.length < 5) { CommandUtil.sendUsage(sender, "/da line set <id> <index> <text...>"); return; }
        String text = joinFrom(args, 4);
        if (!save(sender, a.id, now -> {
            OptionalInt idx = CommandUtil.parseOneBasedIndex(sender, args[3], now.lines.size(), now.id);
            if (idx.isEmpty()) return false;
            now.lines.set(idx.getAsInt() - 1, text);
            return true;
        })) return;
        sender.sendMessage(ColorUtil.parse(Messages.get("line-set", Map.of("index", args[3], "id", a.id))));
    }

    private void insert(CommandSender sender, Announcement a, String[] args) {
        if (args.length < 5) { CommandUtil.sendUsage(sender, "/da line insert <id> <index> <text...>"); return; }
        String text = joinFrom(args, 4);
        if (!save(sender, a.id, now -> {
            OptionalInt idx = CommandUtil.parseOneBasedIndex(sender, args[3], now.lines.size() + 1, now.id);
            if (idx.isEmpty()) return false;
            now.lines.add(idx.getAsInt() - 1, text);
            return true;
        })) return;
        sender.sendMessage(ColorUtil.parse(Messages.get("line-inserted", Map.of("index", args[3], "id", a.id))));
    }

    private void remove(CommandSender sender, Announcement a, String[] args) {
        if (args.length < 4) { CommandUtil.sendUsage(sender, "/da line remove <id> <index>"); return; }
        if (!save(sender, a.id, now -> {
            OptionalInt idx = CommandUtil.parseOneBasedIndex(sender, args[3], now.lines.size(), now.id);
            if (idx.isEmpty()) return false;
            now.lines.remove(idx.getAsInt() - 1);
            return true;
        })) return;
        sender.sendMessage(ColorUtil.parse(Messages.get("line-removed", Map.of("index", args[3], "id", a.id))));
    }

    private boolean save(CommandSender sender, String id, Predicate<Announcement> edit) {
        return CommandUtil.saved(sender, DAnnouncements.getInstance().getAnnouncementConfigLoader().update(id, edit), id);
    }

    private String joinFrom(String[] args, int startIndex) {
        return String.join(" ", Arrays.copyOfRange(args, startIndex, args.length));
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        if (args.length == 2) {
            return new ArrayList<>(List.of("list", "add", "set", "insert", "remove"));
        }
        if (args.length == 3) {
            return new ArrayList<>(DAnnouncements.getInstance().getAnnouncementConfigLoader().getAll().keySet());
        }
        return List.of();
    }
}
