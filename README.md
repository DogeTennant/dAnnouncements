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
| `/da tp <world> <x> <y> <z> [yaw] [pitch]` | `dannouncements.tp` | Self-teleport, used by `tp:` links |
| `/da reload` | `dannouncements.admin.reload` | Reload config + announcements |

## Permissions

| Node | Default |
|---|---|
| `dannouncements.command` | true |
| `dannouncements.tp` | true |
| `dannouncements.admin.*` | op |

`/da tp` is self-only and coordinates-only - no entity selectors, can't move other players. That's why it's safe to leave on by default.

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

`SPECIFIC` announcements always disable themselves after firing. `once: true` does the same for the other types; for `DAILY` with multiple `times`, it waits until the last slot of the day.

## Formatting and links

MiniMessage tags, plus legacy `&` color codes. In any line:

```
[click here](https://example.com)          opens a URL
[teleport to spawn](tp:world,0.5,65,0.5)   teleports the clicker (yaw/pitch optional)
[read the rules](cmd:/rules)               runs a command as the clicker
```

Bare URLs get linkified automatically. Lines containing a manual `<click:...>` tag are left untouched. PlaceholderAPI placeholders resolve per-player when the plugin is installed.

## Warning

In-game edits (`/da create`, `line`, `toggle`, `remove`, `join`) rewrite `announcements.yml` from memory, which strips comments from that file. Hand-editing plus `/da reload` preserves them.

## Building

```
mvn clean package
```

Output lands in `target/`.
