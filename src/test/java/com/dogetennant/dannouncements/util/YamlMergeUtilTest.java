package com.dogetennant.dannouncements.util;

import com.dogetennant.dannouncements.Fixture;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/** config.yml gets sections added in an update - and is left alone when it cannot be read. */
class YamlMergeUtilTest {

    private static final List<String> BUNDLED = """
            # Timezone
            timezone: system

            # Messages
            messages:
              no-permission: "nope"
            """.lines().toList();

    @TempDir
    Path folder;
    private Path config;

    @BeforeEach
    void setUp() throws Exception {
        new Fixture(folder);
        config = folder.resolve("config.yml");
    }

    @Test
    void aNewSectionIsAddedWithItsComment() throws Exception {
        Files.writeString(config, "timezone: Europe/Prague\n", StandardCharsets.UTF_8);

        YamlMergeUtil.mergeMissingKeys(config.toFile(), BUNDLED, "config.yml");

        String text = Files.readString(config);
        assertThat(text).startsWith("timezone: Europe/Prague\n").contains("# Messages\nmessages:");
        assertThat(YamlConfiguration.loadConfiguration(config.toFile()).getString("timezone")).isEqualTo("Europe/Prague");
    }

    @Test
    void aConfigWithATypoIsLeftAlone() throws Exception {
        String broken = "timezone: Europe/Prague\nmessages: [unclosed\n";
        Files.writeString(config, broken, StandardCharsets.UTF_8);

        YamlMergeUtil.mergeMissingKeys(config.toFile(), BUNDLED, "config.yml");
        YamlMergeUtil.mergeMissingKeys(config.toFile(), BUNDLED, "config.yml");   // a second start

        assertThat(Files.readString(config)).isEqualTo(broken);
    }
}
