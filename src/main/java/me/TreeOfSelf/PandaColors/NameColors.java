package me.TreeOfSelf.PandaColors;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.storage.LevelResource;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

// /namecolor: a format (like <gr:red:gold> or &c) applied to the player's name in chat and the tab list.
// Saved in <world>/pandacolors_names.json as the format the player typed.
public final class NameColors {

    private static final Gson GSON = new Gson();
    private static final LevelResource FILE = new LevelResource("pandacolors_names.json");

    private static final Map<UUID, String> FORMATS = new ConcurrentHashMap<>();
    private static final Map<UUID, Component> NAMES = new ConcurrentHashMap<>();
    private static Path path;

    public static void load(MinecraftServer server) {
        path = server.getWorldPath(FILE);
        FORMATS.clear();
        NAMES.clear();
        if (!Files.isRegularFile(path)) return;
        try {
            Map<UUID, String> read = GSON.fromJson(Files.readString(path), new TypeToken<Map<UUID, String>>() {}.getType());
            if (read != null) FORMATS.putAll(read);
        } catch (Exception e) {
            PandaColors.LOGGER.error("Could not read {}", path, e);
        }
    }

    // The colored name, or null if the player has none (or the format no longer fits their name)
    public static Component get(UUID uuid, String name) {
        String format = FORMATS.get(uuid);
        if (format == null) return null;
        Component cached = NAMES.get(uuid);
        if (cached != null && cached.getString().equals(name)) return cached;
        Component built = build(format, name);
        if (built != null) NAMES.put(uuid, built);
        return built;
    }

    public static void set(UUID uuid, String format) {
        NAMES.remove(uuid);
        if (format == null) FORMATS.remove(uuid);
        else FORMATS.put(uuid, format);
        save();
    }

    // Colors and bold/italic/underline/strikethrough only, and the text must still read exactly as the name
    public static Component build(String format, String name) {
        // Underscores escaped so markdown doesn't eat them
        Component parsed = TextFormattingHelper.formatStyledInput(format + name.replace("_", "\\_"));
        MutableComponent out = Component.empty();
        StringBuilder plain = new StringBuilder();
        parsed.visit((style, text) -> {
            plain.append(text);
            out.append(Component.literal(text).withStyle(Style.EMPTY
                    .withColor(style.getColor())
                    .withBold(style.isBold() ? true : null)
                    .withItalic(style.isItalic() ? true : null)
                    .withUnderlined(style.isUnderlined() ? true : null)
                    .withStrikethrough(style.isStrikethrough() ? true : null)));
            return Optional.empty();
        }, Style.EMPTY);
        return plain.toString().equals(name) ? out : null;
    }

    private static void save() {
        if (path == null) return;
        try {
            Files.writeString(path, GSON.toJson(FORMATS));
        } catch (IOException e) {
            PandaColors.LOGGER.error("Could not save {}", path, e);
        }
    }
}
