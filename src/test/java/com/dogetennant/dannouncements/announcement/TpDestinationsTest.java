package com.dogetennant.dannouncements.announcement;

import com.dogetennant.dannouncements.Fixture;
import com.dogetennant.dannouncements.dispatch.AnnouncementDispatcher;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/** /da tp: only where an announcement a player could have been shown sends them. */
class TpDestinationsTest {

    @TempDir
    Path folder;
    private AnnouncementConfigLoader loader;
    private final AnnouncementDispatcher dispatcher = mock(AnnouncementDispatcher.class);
    private final Player player = mock(Player.class);

    @BeforeEach
    void setUp() throws Exception {
        Fixture fixture = new Fixture(folder);
        loader = new AnnouncementConfigLoader(fixture.plugin);
        when(fixture.plugin.getAnnouncementConfigLoader()).thenReturn(loader);
        when(fixture.plugin.getDispatcher()).thenReturn(dispatcher);
    }

    private void add(String id, boolean enabled, String permission) throws Exception {
        loader.getAll().put(id, Fixture.announcement(id, """
                enabled: %s
                permission: "%s"
                lines:
                  - "[Spawn](tp:world,0.5,65,0.5,0,0)"
                """.formatted(enabled, permission)));
    }

    @Test
    void anEnabledAnnouncementsDestination() throws Exception {
        add("welcome", true, "");

        assertThat(TpDestinations.check(player, "world", 0.5, 65, 0.5)).isEqualTo(TpDestinations.Result.ALLOWED);
        assertThat(TpDestinations.check(player, "world", 100, 65, 0.5)).isEqualTo(TpDestinations.Result.NOT_PUBLISHED);
    }

    @Test
    void aPermissionTheyLack() throws Exception {
        add("vip", true, "server.vip");

        assertThat(TpDestinations.check(player, "world", 0.5, 65, 0.5)).isEqualTo(TpDestinations.Result.NO_PERMISSION);
    }

    @Test
    void aSwitchedOffAnnouncementOffersNoTeleport() throws Exception {
        // like the shipped example welcome_message: disabled, never shown to anyone
        add("welcome", false, "");

        assertThat(TpDestinations.check(player, "world", 0.5, 65, 0.5)).isEqualTo(TpDestinations.Result.NOT_PUBLISHED);
    }

    @Test
    void aOnceAnnouncementJustSentKeepsItsLinks() throws Exception {
        // it switched itself off right after sending - players click its link now
        add("event", false, "");
        when(dispatcher.sentWithin("event", TpDestinations.LINK_LIFETIME)).thenReturn(true);

        assertThat(TpDestinations.check(player, "world", 0.5, 65, 0.5)).isEqualTo(TpDestinations.Result.ALLOWED);
    }
}
