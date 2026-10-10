package com.dogetennant.dannouncements;

import com.dogetennant.dannouncements.announcement.Announcement;
import com.dogetennant.dannouncements.announcement.AnnouncementConfigLoader;
import com.dogetennant.dannouncements.command.subcommand.JoinSubCommand;
import com.dogetennant.dannouncements.command.subcommand.ToggleSubCommand;
import com.dogetennant.dannouncements.dispatch.AnnouncementDispatcher;
import com.dogetennant.dannouncements.listener.JoinAnnouncementListener;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerJoinEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** The join trigger and the commands around it. */
class SmallFindingsTest {

    @TempDir
    Path folder;
    private Fixture fixture;
    private AnnouncementConfigLoader loader;
    private Path file;

    @BeforeEach
    void setUp() throws Exception {
        fixture = new Fixture(folder);
        loader = new AnnouncementConfigLoader(fixture.plugin);
        when(fixture.plugin.getAnnouncementConfigLoader()).thenReturn(loader);
        file = fixture.announcements("""
                welcome:
                  enabled: true
                  join:
                    enabled: false
                    delay-seconds: 3
                  lines: ["Welcome!"]
                """);
        loader.load();
    }

    private YamlConfiguration saved() {
        return YamlConfiguration.loadConfiguration(file.toFile());
    }

    private void join(boolean on) {
        loader.update("welcome", a -> {
            a.join.enabled = on;
            return true;
        });
    }

    @Test
    void aRefusedJoinDelayChangesNothing() {
        new JoinSubCommand().execute(mock(CommandSender.class), new String[] {"join", "welcome", "on", "soon"});

        assertThat(loader.get("welcome").orElseThrow().join.enabled).isFalse();
        assertThat(saved().getBoolean("announcements.welcome.join.enabled")).isFalse();
    }

    @Test
    void aJoinDelayIsSaved() {
        new JoinSubCommand().execute(mock(CommandSender.class), new String[] {"join", "welcome", "on", "5"});

        assertThat(saved().getBoolean("announcements.welcome.join.enabled")).isTrue();
        assertThat(saved().getInt("announcements.welcome.join.delay-seconds")).isEqualTo(5);
    }

    @Test
    void aDelayedJoinAnnouncementIsSentAsItIsWhenTheDelayEnds() {
        join(true);
        AnnouncementDispatcher dispatcher = mock(AnnouncementDispatcher.class);
        Player player = mock(Player.class);
        when(player.isOnline()).thenReturn(true);
        PlayerJoinEvent event = mock(PlayerJoinEvent.class);
        when(event.getPlayer()).thenReturn(player);
        JoinAnnouncementListener listener = new JoinAnnouncementListener(fixture.plugin, loader, dispatcher);

        listener.onJoin(event);
        fixture.later.forEach(Runnable::run);
        verify(dispatcher, times(1)).dispatch(argThat(a -> a.id.equals("welcome")), eq(player));

        fixture.later.clear();
        listener.onJoin(event);
        loader.update("welcome", a -> {                 // /da toggle welcome during the 3 seconds
            a.enabled = false;
            return true;
        });
        fixture.later.forEach(Runnable::run);
        verify(dispatcher, times(1)).dispatch(any(), any());   // still just the first one
    }

    @Test
    void aCommandOnAFileWithAnErrorChangesNothingAndSaysWhy() throws Exception {
        String broken = Files.readString(file) + "  oops: [unclosed\n";
        Files.writeString(file, broken, StandardCharsets.UTF_8);
        CommandSender sender = mock(CommandSender.class);

        new ToggleSubCommand().execute(sender, new String[] {"toggle", "welcome"});

        assertThat(Files.readString(file)).isEqualTo(broken);
        ArgumentCaptor<Component> message = ArgumentCaptor.forClass(Component.class);
        verify(sender).sendMessage(message.capture());
        assertThat(PlainTextComponentSerializer.plainText().serialize(message.getValue()))
                .contains("announcements.yml has an error");
    }

    @Test
    void theDispatcherKnowsWhatWasJustSent() {
        Announcement welcome = loader.get("welcome").orElseThrow();
        welcome.sound.enabled = false;
        AnnouncementDispatcher dispatcher = new AnnouncementDispatcher(fixture.plugin);

        assertThat(dispatcher.dispatch(welcome, mock(Player.class))).isEqualTo(1);

        assertThat(dispatcher.sentWithin("welcome", Duration.ofMinutes(30))).isTrue();
        assertThat(dispatcher.sentWithin("other", Duration.ofMinutes(30))).isFalse();
    }
}
