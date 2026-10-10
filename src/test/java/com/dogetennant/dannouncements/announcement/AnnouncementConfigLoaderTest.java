package com.dogetennant.dannouncements.announcement;

import com.dogetennant.dannouncements.Fixture;
import com.dogetennant.dannouncements.announcement.AnnouncementConfigLoader.Change;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

import static org.assertj.core.api.Assertions.assertThat;

/** announcements.yml: an in-game change must never lose what is in the file. */
class AnnouncementConfigLoaderTest {

    private static final String FILE = """
            # Our announcements - keep me
            announcements:
              alpha:
                enabled: false
                schedule:
                  enabled: true
                  type: DAILY
                  time: "9:00"   # before the morning rush
                lines:
                  - "Hello"
            """;
    private static final String BETA_BY_HAND = """
              beta:
                enabled: true
                lines:
                  - "Added by hand"
            """;

    @TempDir
    Path folder;
    private Path file;
    private AnnouncementConfigLoader loader;
    private int changes;

    @BeforeEach
    void setUp() throws Exception {
        file = folder.resolve("announcements.yml");
        loader = new AnnouncementConfigLoader(new Fixture(folder).plugin);
        loader.setOnChange(() -> changes++);
    }

    private void write(String text) throws IOException {
        Files.writeString(file, text, StandardCharsets.UTF_8);
    }

    private void append(String text) throws IOException {
        Files.writeString(file, text, StandardCharsets.UTF_8, StandardOpenOption.APPEND);
    }

    private String read() throws IOException {
        return Files.readString(file, StandardCharsets.UTF_8);
    }

    private YamlConfiguration saved() {
        return YamlConfiguration.loadConfiguration(file.toFile());
    }

    private static boolean switchOn(Announcement a) {
        a.enabled = true;
        return true;
    }

    @Test
    void aFileThatCannotBeReadIsNeverSavedOver() throws Exception {
        String broken = FILE + "  beta: [unclosed\n";
        write(broken);
        assertThat(loader.load()).isFalse();            // the typo: nothing could be read

        assertThat(loader.add(Announcement.createDefault("gamma", Fixture.config()))).isEqualTo(Change.UNREADABLE);
        assertThat(loader.update("alpha", AnnouncementConfigLoaderTest::switchOn)).isEqualTo(Change.UNREADABLE);
        assertThat(loader.remove("alpha")).isEqualTo(Change.UNREADABLE);

        assertThat(read()).isEqualTo(broken);           // alpha (and the half-written beta) still there
        assertThat(changes).isZero();
    }

    @Test
    void aReloadOfABrokenFileKeepsTheAnnouncementsLoadedBefore() throws Exception {
        write(FILE);
        loader.load();
        append("  beta: [unclosed\n");

        assertThat(loader.load()).isFalse();

        assertThat(loader.getAll()).containsOnlyKeys("alpha");
    }

    @Test
    void anInGameChangeKeepsWhatWasEditedByHandSinceTheLastLoad() throws Exception {
        write(FILE);
        loader.load();
        append(BETA_BY_HAND);

        assertThat(loader.update("alpha", AnnouncementConfigLoaderTest::switchOn)).isEqualTo(Change.SAVED);   // /da toggle alpha

        assertThat(saved().getBoolean("announcements.alpha.enabled")).isTrue();
        assertThat(saved().getStringList("announcements.beta.lines")).containsExactly("Added by hand");
        assertThat(loader.get("beta")).isPresent();     // and it is in use now
        assertThat(changes).isEqualTo(1);
    }

    @Test
    void aHandEditToTheSameAnnouncementIsKeptToo() throws Exception {
        write(FILE);
        loader.load();
        write(FILE.replace("\"Hello\"", "\"Hello, edited\""));

        loader.update("alpha", AnnouncementConfigLoaderTest::switchOn);

        assertThat(saved().getStringList("announcements.alpha.lines")).containsExactly("Hello, edited");
        assertThat(saved().getBoolean("announcements.alpha.enabled")).isTrue();
    }

    @Test
    void anInGameChangeKeepsTheCommentsAndTheAdminsSpelling() throws Exception {
        write(FILE);
        loader.load();

        loader.update("alpha", AnnouncementConfigLoaderTest::switchOn);

        assertThat(read()).contains("# Our announcements - keep me").contains("# before the morning rush");
        assertThat(saved().getString("announcements.alpha.schedule.time")).isEqualTo("9:00");
    }

    @Test
    void anIdAddedByHandMeanwhileIsNotOverwritten() throws Exception {
        write(FILE);
        loader.load();
        append(BETA_BY_HAND);

        assertThat(loader.add(Announcement.createDefault("beta", Fixture.config()))).isEqualTo(Change.EXISTS);

        assertThat(saved().getStringList("announcements.beta.lines")).containsExactly("Added by hand");
    }

    @Test
    void addAndRemoveTouchOnlyTheirOwn() throws Exception {
        write(FILE);
        loader.load();
        append(BETA_BY_HAND);

        assertThat(loader.add(Announcement.createDefault("gamma", Fixture.config()))).isEqualTo(Change.SAVED);
        assertThat(loader.remove("alpha")).isEqualTo(Change.SAVED);
        assertThat(loader.remove("alpha")).isEqualTo(Change.NOT_FOUND);

        assertThat(saved().getConfigurationSection("announcements").getKeys(false)).containsExactly("beta", "gamma");
        assertThat(loader.getAll()).containsOnlyKeys("beta", "gamma");
        assertThat(folder.resolve("announcements.yml.tmp")).doesNotExist();
    }

    @Test
    void anEditThatSaysNoChangesNothing() throws Exception {
        write(FILE);
        loader.load();

        assertThat(loader.update("alpha", a -> false)).isEqualTo(Change.UNCHANGED);
        assertThat(loader.update("nope", AnnouncementConfigLoaderTest::switchOn)).isEqualTo(Change.NOT_FOUND);

        assertThat(read()).isEqualTo(FILE);
        assertThat(changes).isZero();
    }
}
