package me.TreeOfSelf.PandaColors;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public final class PandaColorsConfig {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path PATH = FabricLoader.getInstance().getConfigDir().resolve("PandaColors.json");

    private static volatile PandaColorsConfig instance = defaults();

    public boolean chat = true;
    public boolean commandBroadcast = true;
    public boolean sign = true;
    public boolean anvil = true;
    public boolean book = true;
    public boolean markdown = true;
    // [item] / [inv] / [echest] in chat. Each list holds every way to type that tag (case-insensitive).
    public boolean chatItems = true;
    public List<String> itemTags = new ArrayList<>(List.of("[item]", "[i]", "[hand]"));
    public List<String> inventoryTags = new ArrayList<>(List.of("[inv]", "[inventory]"));
    public List<String> enderChestTags = new ArrayList<>(List.of("[echest]", "[ec]", "[enderchest]", "[ender]"));
    // /namecolor
    public boolean nameColor = true;
    // Replaces "joined the game" the first time a player joins. %player% is their name. Yellow like vanilla; tags/&-codes work. Empty = off.
    public String firstJoinMessage = "";

    private static PandaColorsConfig defaults() {
        return new PandaColorsConfig();
    }

    public static PandaColorsConfig get() {
        return instance;
    }

    public static void load() {
        PandaColorsConfig cfg = defaults();
        if (Files.isRegularFile(PATH)) {
            try (BufferedReader reader = Files.newBufferedReader(PATH)) {
                PandaColorsConfig read = GSON.fromJson(reader, PandaColorsConfig.class);
                if (read != null) cfg = read;
            } catch (Exception e) {
                PandaColors.LOGGER.warn("Could not read {}, using defaults", PATH, e);
                instance = cfg;
                return;
            }
        }
        // Writes back so new options show up in old files
        try {
            Files.createDirectories(PATH.getParent());
            save(cfg);
        } catch (IOException e) {
            PandaColors.LOGGER.warn("Could not write {}", PATH, e);
        }
        instance = cfg;
    }

    public static void save(PandaColorsConfig cfg) throws IOException {
        try (BufferedWriter writer = Files.newBufferedWriter(PATH)) {
            GSON.toJson(cfg, writer);
        }
    }
}
