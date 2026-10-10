# dAnnouncements

Scheduled, eye-catching announcements for the important stuff.

**Requires:** Paper 1.21+ (or a fork), Java 21. PlaceholderAPI optional.

## Install

Drop the jar in `/plugins` and restart. Four disabled example announcements ship in `announcements.yml` - edit one, `/da reload`, `/da toggle <id>`.

## Commands

Base command `/dannouncements`, alias `/da`. Line indexes are 1-based.

| Command | Permission | Does |
|---|---|---|
| `/da help` | `dannouncements.command` | Lists commands you have access to |
| `/da list` | `dannouncements.admin.list` | All announcements, state, schedule, line count |
| `/da info <id>` | `dannouncements.admin.info` | Full detail + next scheduled run |
| `/da create <id>` | `dannouncements.admin.create` | New announcement from config defaults, disabled |
| `/da remove <id>` | `dannouncements.admin.remove` | Delete |
| `/da toggle <id>` | `dannouncements.admin.toggle` | Enable / disable |
| `/da force <id> [player]` | `dannouncements.admin.force` | Send now, to everyone or one player |
| `/da line list <id>` | `dannouncements.admin.line` | Show lines |
| `/da line add <id> <text...>` | `dannouncements.admin.line` | Append |
| `/da line set <id> <index> <text...>` | `dannouncements.admin.line` | Replace |
| `/da line insert <id> <index> <text...>` | `dannouncements.admin.line` | Insert at position |
| `/da line remove <id> <index>` | `dannouncements.admin.line` | Delete |
| `/da join <id> <on\|off\|delay> [seconds]` | `dannouncements.admin.join` | Configure on-join trigger |
| `/da tp <world> <x> <y> <z> [yaw] [pitch]` | `dannouncements.tp` | Self-teleport to a destination a `tp:` link publishes - see below |
| `/da reload` | `dannouncements.admin.reload` | Reload config + announcements |

## Permissions

| Node | Default |
|---|---|
| `dannouncements.command` | true |
| `dannouncements.tp` | true |
| `dannouncements.admin.*` | op |
| `dannouncements.admin.tp` | op |

### Why `dannouncements.tp` can be on by default

A click link runs as the clicking player, so `tp:` links only work if ordinary players can run `/da tp`. That does **not** make it a free teleport:

- A destination only works if some announcement publishes it through a `tp:` link (or a hand-written `/da tp` in a line). Anything else - a coordinate a player invented, or one left over from a line you since edited - is refused.
- If that announcement has a `permission` set, the player needs it to follow the link, not just to see the announcement. A staff-only announcement's `tp:` link stays staff-only.
- `/da tp` is hidden from `/da help` and tab-completion, and every refusal reads the same, so it can't be used to fish for world names.

`dannouncements.admin.tp` (op) lifts all of that and turns `/da tp` back into an unrestricted self-teleport to any coordinates. Keep it to staff.

Destinations are read live, so `/da reload` and `/da line` edits apply immediately. A switched-off announcement publishes nothing, except for 30 minutes after it was sent - so the links of a `once` announcement (which switches itself off right after sending) and of a draft sent with `/da force` still work while players click them. A `tp:` link assembled by a PlaceholderAPI placeholder can't be matched - write the coordinates literally in the line.

## config.yml

| Key | Default | Notes |
|---|---|---|
| `timezone` | `system` | `system` or a Java ZoneId, e.g. `Europe/Prague` |
| `poll-interval-ticks` | `20` | How often the scheduler checks for due announcements |
| `defaults.*` | - | Applied to announcements made with `/da create` |
| `messages.*` | - | Command feedback strings, MiniMessage supported |

New top-level sections added in an update get appended to your existing file automatically; new keys inside sections you already have are logged as a warning instead of spliced in.

## announcements.yml

```yaml
announcements:
  my_announcement:
    enabled: false
    once: false            # fire once, then auto-disable
    permission: ""         # blank = everyone
    schedule:
      enabled: true
      type: DAILY          # INTERVAL, DAILY, WEEKLY, MONTHLY, SPECIFIC
      time: "09:00"
      times: ["16:00", "20:00"]   # DAILY only; overrides "time" when present
      interval-minutes: 60        # INTERVAL only
      day: FRIDAY                 # WEEKLY only
      day-of-month: 1             # MONTHLY only, clamped to short months
      date: "2026-08-01"          # SPECIFIC only, yyyy-MM-dd
    join:
      enabled: false
      delay-seconds: 0
      exclude-ops: false
      excluded-permissions: []
    lines:
      - "<gold><bold>Hello</bold></gold>"
    delivery:
      chat:
        enabled: true
        border: true
        border-char: "▬"
        border-length: 46
        border-color: "<dark_gray>"
        center: true
      title:
        enabled: false
        text: ""           # blank = first line
        subtitle: ""       # blank = second line
        fade-in: 10        # ticks
        stay: 60
        fade-out: 20
      actionbar:
        enabled: false
        text: ""           # blank = first line
      bossbar:
        enabled: false
        text: ""           # blank = first line
        color: YELLOW      # PINK, BLUE, RED, GREEN, YELLOW, PURPLE, WHITE
        overlay: PROGRESS  # PROGRESS, NOTCHED_6/10/12/20
        seconds: 8
      sound:
        enabled: true
        name: ENTITY_PLAYER_LEVELUP
        volume: 1.0
        pitch: 1.0
```

`join` is independent of `schedule` - an announcement can fire only on join, only on a timer, or both.

`SPECIFIC` announcements always disable themselves after firing. `once: true` does the same for the other types; for `DAILY` with multiple `times`, it waits until the last slot of the day. A `SPECIFIC` date missed while the server was off (or set in the past) is still sent if at most 10 minutes late; later than that it is switched off without sending, and the console says so.

A schedule that cannot be followed - a time that isn't `00:00` to `23:59`, a `day` that isn't a weekday, a `day-of-month` outside 1 to 31, a missing or wrong `date` - is reported in the console and by `/da info`, and only that announcement is left unscheduled. Changing or switching one announcement doesn't restart the others' countdowns.

## Formatting and links

MiniMessage tags, plus legacy `&` color codes. In any line:

```
[click here](https://example.com)          opens a URL
[teleport to spawn](tp:world,0.5,65,0.5)   teleports the clicker (yaw/pitch optional)
[read the rules](cmd:/rules)               runs a command as the clicker
```

Bare URLs get linkified automatically. Lines containing a manual `<click:...>` tag are left untouched. PlaceholderAPI placeholders resolve per-player when the plugin is installed.

Writing a `tp:` link is what makes that destination reachable by `/da tp` at all, so keep links to places you're happy for that announcement's audience to stand in - see [Permissions](#permissions).

## Editing by hand and in game

Both work together. An in-game edit (`/da create`, `line`, `toggle`, `remove`, `join`) reads `announcements.yml` again first, so what you changed by hand since the last `/da reload` is kept (and starts being used), and it changes only that announcement, in place - comments stay. If the file has a YAML error, nothing is saved over it: `/da reload` keeps the announcements loaded before, in-game edits are refused, and the console shows the error until you fix it.

## Building

```
mvn clean package
```

Output lands in `target/`.
