package me.TreeOfSelf.PandaColors;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.fabricmc.loader.api.FabricLoader;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public final class PandaColorsConfig {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path PATH = FabricLoader.getInstance().getConfigDir().resolve("PandaColors.json");

    private static volatile PandaColorsConfig instance = defaults();

    public boolean chat = true;
    public boolean commandBroadcast = true;
    public boolean sign = true;
    public boolean anvil = true;
    public boolean book = true;

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
                JsonObject o = JsonParser.parseReader(reader).getAsJsonObject();
                if (o.has("chat")) cfg.chat = o.get("chat").getAsBoolean();
                if (o.has("commandBroadcast")) cfg.commandBroadcast = o.get("commandBroadcast").getAsBoolean();
                if (o.has("sign")) cfg.sign = o.get("sign").getAsBoolean();
                if (o.has("anvil")) cfg.anvil = o.get("anvil").getAsBoolean();
                if (o.has("book")) cfg.book = o.get("book").getAsBoolean();
            } catch (Exception e) {
                PandaColors.LOGGER.warn("Could not read {}, using defaults", PATH, e);
            }
        } else {
            try {
                Files.createDirectories(PATH.getParent());
                save(cfg);
            } catch (IOException e) {
                PandaColors.LOGGER.warn("Could not write default {}", PATH, e);
            }
        }
        instance = cfg;
    }

    public static void save(PandaColorsConfig cfg) throws IOException {
        try (BufferedWriter writer = Files.newBufferedWriter(PATH)) {
            GSON.toJson(cfg, writer);
        }
    }
}
