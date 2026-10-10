package com.dogetennant.dannouncements.schedule;

import com.dogetennant.dannouncements.Fixture;
import com.dogetennant.dannouncements.TestClock;
import com.dogetennant.dannouncements.announcement.AnnouncementConfigLoader;
import com.dogetennant.dannouncements.dispatch.AnnouncementDispatcher;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/** The scheduler: one announcement's mistake must not stop the others, and a countdown keeps going. */
class AnnouncementSchedulerTest {

    @TempDir
    Path folder;
    private Fixture fixture;
    private AnnouncementConfigLoader loader;
    private final AnnouncementDispatcher dispatcher = mock(AnnouncementDispatcher.class);
    private final TestClock clock = new TestClock("2026-10-10T09:59:30Z");
    private AnnouncementScheduler scheduler;

    @BeforeEach
    void setUp() throws Exception {
        fixture = new Fixture(folder);
        loader = new AnnouncementConfigLoader(fixture.plugin);
        scheduler = new AnnouncementScheduler(fixture.plugin, loader, dispatcher, Fixture.config(), clock);
        loader.setOnChange(scheduler::refresh);         // as the plugin wires it
    }

    private static String scheduled(String id, String type, String extra) {
        return """
                %s:
                  enabled: true
                  schedule:
                    enabled: true
                    type: %s
                %s
                  lines: ["x"]
                """.formatted(id, type, extra.indent(4).stripTrailing());
    }

    private void start(String... announcements) throws Exception {
        fixture.announcements(String.join("", announcements));
        loader.load();
        scheduler.load();
    }

    private void poll() {
        fixture.timers.getLast().run();
    }

    private static long at(String instant) {
        return Instant.parse(instant).toEpochMilli();
    }

    private boolean enabledInFile(String id) {
        return YamlConfiguration.loadConfiguration(folder.resolve("announcements.yml").toFile())
                .getBoolean("announcements." + id + ".enabled");
    }

    private void verifySent(String id, int count) {
        verify(dispatcher, times(count)).dispatch(argThat(a -> a.id.equals(id)), isNull());
    }

    @Test
    void aTimeOrDayThatDoesNotExistStopsOnlyThatAnnouncement() throws Exception {
        assertThatCode(() -> start(
                scheduled("good", "DAILY", "time: \"10:00\""),
                scheduled("hour25", "DAILY", "time: \"25:00\""),
                scheduled("minute60", "WEEKLY", "time: \"10:60\"\nday: FRIDAY"),
                scheduled("typo_day", "WEEKLY", "time: \"10:00\"\nday: FRIDY"),
                scheduled("day0", "MONTHLY", "time: \"10:00\"\nday-of-month: 0"),
                scheduled("no_date", "SPECIFIC", "time: \"10:00\""),
                scheduled("bad_slot", "DAILY", "times: [\"08:00\", \"8 pm\"]")))
                .doesNotThrowAnyException();

        assertThat(scheduler.getNextRun("good")).contains(at("2026-10-10T10:00:00Z"));
        for (String id : new String[] {"hour25", "minute60", "typo_day", "day0", "no_date", "bad_slot"}) {
            assertThat(scheduler.getNextRun(id)).as(id).isEmpty();
        }
        assertThat(fixture.timers).hasSize(1);           // and the scheduler runs
    }

    @Test
    void aDailyAnnouncementIsSentOnceAtItsTime() throws Exception {
        start(scheduled("morning", "DAILY", "time: \"10:00\""));
        verify(dispatcher, never()).dispatch(any(), any());

        clock.advance(Duration.ofSeconds(31));
        poll();
        poll();

        verifySent("morning", 1);
        assertThat(scheduler.getNextRun("morning")).contains(at("2026-10-11T10:00:00Z"));
    }

    @Test
    void anotherAnnouncementsChangeKeepsAnIntervalsCountdown() throws Exception {
        start(scheduled("every_hour", "INTERVAL", "interval-minutes: 60"),
                scheduled("other", "DAILY", "time: \"10:00\""));
        long due = at("2026-10-10T10:59:00Z");
        assertThat(scheduler.getNextRun("every_hour")).contains(due);

        clock.advance(Duration.ofMinutes(30));
        loader.update("other", a -> {                   // /da toggle other
            a.enabled = false;
            return true;
        });
        assertThat(scheduler.getNextRun("every_hour")).contains(due);
        assertThat(scheduler.getNextRun("other")).isEmpty();

        loader.load();                                  // and /da reload
        scheduler.reload(Fixture.config());
        assertThat(scheduler.getNextRun("every_hour")).contains(due);
    }

    @Test
    void aChangedScheduleIsWorkedOutAgain() throws Exception {
        start(scheduled("every_hour", "INTERVAL", "interval-minutes: 60"));
        clock.advance(Duration.ofMinutes(30));

        fixture.announcements(scheduled("every_hour", "INTERVAL", "interval-minutes: 10"));   // edited by hand
        loader.load();
        scheduler.reload(Fixture.config());

        assertThat(scheduler.getNextRun("every_hour")).contains(at("2026-10-10T10:39:00Z"));
    }

    @Test
    void aSpecificDateAFewMinutesLateIsStillSent() throws Exception {
        // the server restarted right at 09:55
        start(scheduled("event", "SPECIFIC", "date: \"2026-10-10\"\ntime: \"09:55\""));

        verifySent("event", 1);
        assertThat(enabledInFile("event")).isFalse();    // sent once, then off
        assertThat(scheduler.getNextRun("event")).isEmpty();
    }

    @Test
    void aSpecificDateLongPastIsSwitchedOffWithoutSending() throws Exception {
        start(scheduled("event", "SPECIFIC", "date: \"2026-10-08\"\ntime: \"18:00\""));
        poll();

        verify(dispatcher, never()).dispatch(any(), any());
        assertThat(enabledInFile("event")).isFalse();
        assertThat(scheduler.getNextRun("event")).isEmpty();
    }

    @Test
    void aSpecificDateAheadWaitsForItsTime() throws Exception {
        start(scheduled("event", "SPECIFIC", "date: \"2026-10-12\"\ntime: \"18:00\""));

        assertThat(scheduler.getNextRun("event")).contains(at("2026-10-12T18:00:00Z"));
        verify(dispatcher, never()).dispatch(any(), any());
        assertThat(enabledInFile("event")).isTrue();
    }
}
